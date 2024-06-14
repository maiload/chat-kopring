package org.example.chatkopring.chat.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.dto.ChatRoomDto
import org.example.chatkopring.chat.dto.ErrorMessage
import org.example.chatkopring.chat.dto.MessageDto
import org.example.chatkopring.chat.service.ChatService
import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.common.status.RoomType
import org.example.chatkopring.member.service.MemberService
import org.example.chatkopring.util.logger
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class MessageConsumer(
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
    private val memberService: MemberService,
) {
    private val log = logger()

    @RabbitListener(queues = ["\${spring.rabbitmq.cq}"])
    fun createMessage(message: String) {
        val messageDto = jsonToMessage(message)
        when (messageDto.type) {
            RoomType.ALL.name -> createAllRoom(messageDto)
            RoomType.GROUP.name -> createGroupRoom(messageDto)
            RoomType.PRIVATE.name -> createPrivateRoom(messageDto)
        }
    }

    @RabbitListener(queues = ["\${spring.rabbitmq.iq}"])
    fun inviteMessage(message: String) {
        val (loginId, type, payload) = jsonToMessage(message)
        chatService.inviteRoom(payload as ChatRoomDto, loginId)
    }

    @RabbitListener(queues = ["\${spring.rabbitmq.lq}"])
    fun leaveMessage(message: String) {
        val (loginId, type, payload) = jsonToMessage(message)
        chatService.leaveRoom(payload as ChatRoomDto)
    }

    @RabbitListener(queues = ["\${spring.rabbitmq.sq}"])
    fun sendMessage(message: String) {
        val (loginId, type, payload) = jsonToMessage(message)
        val chatMessageDto = mapToChatMessageDto(payload)
        val chatRoom = chatService.getChatRoomById(chatMessageDto.roomId)
        requireNotNull(chatRoom)
        chatService.sendMessage(chatMessageDto, chatRoom)
    }

    fun createAllRoom(messageDto: MessageDto) {
        val (loginId, type, payload) = messageDto
        val chatRoomDto = mapToChatRoomDto(payload)
        val member = memberService.searchMyInfo(loginId)
        val (roomId, creator, roomType, title) = chatRoomDto
        if (member.role != Role.ADMIN.name || chatRoomDto.creator != loginId) {
            sendErrorMessage(ErrorMessage("Admin 사용자만 전체 채팅방을 만들 수 있습니다.", creator, roomId))
        }else{
            chatService.createRoom(chatRoomDto)
            val members = memberService.findColleague(member.id)
            // ADMIN -> 전체 참여(생성자 포함)
            members.forEach {
                val roomDto = ChatRoomDto(roomId, it.loginId, roomType)
                chatService.joinRoom(roomDto)
                if (it.loginId != loginId) chatService.inactiveRoom(roomDto)
                else chatService.activeRoom(chatRoomDto)
            }
        }
    }

    fun createGroupRoom(messageDto: MessageDto) {
        val (loginId, type, payload) = messageDto
        val chatRoomDto = mapToChatRoomDto(payload)
        val (roomId, creator, roomType, title) = chatRoomDto
        if (chatRoomDto.participant.isNullOrEmpty()) {
            sendErrorMessage(ErrorMessage("Group 채팅방은 creator 를 제외한 참여자가 1명 이상이어야 합니다.", creator, roomId))
        }else{
            chatService.createRoom(chatRoomDto)
            // 방 생성자 JOIN
            chatService.joinRoom(chatRoomDto)
            chatService.activeRoom(chatRoomDto)
            // 참여자 강제 JOIN
            chatRoomDto.participant.forEach {
                val roomDto = ChatRoomDto(roomId, it, roomType)
                chatService.joinRoom(roomDto)
                chatService.inactiveRoom(roomDto)
            }
        }
    }

    fun createPrivateRoom(messageDto: MessageDto) {
        val (loginId, type, payload) = messageDto
        val chatRoomDto = mapToChatRoomDto(payload)
        val (roomId, creator, roomType, title) = chatRoomDto
        val receiver = chatRoomDto.participant?.firstOrNull()
        if(receiver == null || receiver == creator) {
            sendErrorMessage(ErrorMessage("Private 채팅방은 자신을 제외한 참여자가 있어야 합니다.", creator, roomId))
        }else{
            val alreadyCreatedRoomId = chatService.isPrivateRoomExist(creator, receiver, chatRoomDto)
            if(alreadyCreatedRoomId != null){
                log.info("Already private room existed (${creator} & ${receiver})")
                val lastChatMessage = chatService.getLastChatMessage(alreadyCreatedRoomId, creator)
                if(lastChatMessage.type == MessageType.LEAVE) {
                    // 재입장
                    chatService.joinRoom(chatRoomDto)
                }
                // 재입장 활성화 or 활성화
                chatService.activeRoom(chatRoomDto)
            }else{
                chatService.createRoom(chatRoomDto)
                // 생성자 JOIN
                chatService.joinRoom(chatRoomDto)
                chatService.activeRoom(chatRoomDto)
                // 상대방 강제 JOIN
                val roomDto = ChatRoomDto(roomId, receiver, roomType, title)
                chatService.joinRoom(roomDto)
                chatService.inactiveRoom(roomDto)
            }
        }
    }

    fun jsonToMessage(message: String): MessageDto = jacksonObjectMapper().readValue(message, MessageDto::class.java)
    fun mapToChatRoomDto(payload: Any): ChatRoomDto = jacksonObjectMapper().convertValue(payload, ChatRoomDto::class.java)
    fun mapToChatMessageDto(payload: Any): ChatMessageDto = jacksonObjectMapper().convertValue(payload, ChatMessageDto::class.java)

    fun sendErrorMessage(errorMessage: ErrorMessage?) {
        log.error("WebSocketError : $errorMessage")
        if (errorMessage != null) {
            messagingTemplate.convertAndSend("/sub/chat/${errorMessage.roomId}", errorMessage)
        }
    }
}