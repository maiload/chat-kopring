package org.example.chatkopring.chat.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.Valid
import org.example.chatkopring.chat.config.RabbitmqConfig
import org.example.chatkopring.chat.config.UserSessionRegistry
import org.example.chatkopring.chat.config.UserState
import org.example.chatkopring.chat.dto.*
import org.example.chatkopring.chat.service.ChatService
import org.example.chatkopring.common.dto.BaseResponse
import org.example.chatkopring.common.dto.CustomUser
import org.example.chatkopring.common.exception.InvalidInputException
import org.example.chatkopring.common.exception.UnAuthorizationException
import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.common.status.State
import org.example.chatkopring.member.service.MemberService
import org.example.chatkopring.util.logger
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.util.StringUtils
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.session.WebSessionManager
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

@Controller
class ChatController2(
    private val userSessionRegistry: UserSessionRegistry,
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
    private val memberService: MemberService,
    private val rabbitTemplate: RabbitTemplate,
    private val rabbitmqConfig: RabbitmqConfig,
) {
    private val log = logger()

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
        val user = StompHeaderAccessor.wrap(event.message).user
        if(user != null) {
            val customUser = (user as UsernamePasswordAuthenticationToken).principal as CustomUser
            val state = userSessionRegistry.registerUser(customUser.username)
            if(state == UserState.CONNECT){
                messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.CONNECT, user.name))
                log.info("New Connection : ${customUser.username} ${user.authorities}")
            } else{
                log.warn("DUP - Connection : [${customUser.username}]")
            }
        }
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val user = StompHeaderAccessor.wrap(event.message).user
        if(user != null) {
            val customUser = (user as UsernamePasswordAuthenticationToken).principal as CustomUser
            val state = userSessionRegistry.removeUser(customUser.username)
            if(state == UserState.DISCONNECT) {
                messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.DISCONNECT, user.name))
                log.info("User Disconnected : ${customUser.username} ${user.authorities}")
                // INACTIVE 가 아닌 모든 방 INACTIVE
                val allParticipatedRooms = chatService.loadAllParticipatedRooms(user.name)
                allParticipatedRooms?.forEach {
                    if(!chatService.isInactivatedRoom(it.chatRoom, user.name)) {
                        chatService.inactiveRoom(it.toChatRoomDto())
                    }
                }
            } else{
                log.warn("DUP - Disconnection : [${customUser.username}]")
            }
        }
    }

    @EventListener
    fun handleWebSocketUnsubscribeListener(event: SessionUnsubscribeEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val username = headerAccessor.user?.name
        val subId = headerAccessor.subscriptionId
        if(StringUtils.hasText(username) && StringUtils.hasText(subId)) {
            val state = userSessionRegistry.removeSession(username!!, subId!!)
            when (state) {
                UserState.UNSUBSCRIBE -> log.info("SubId : $subId, [$username] Unsubscribed")
                UserState.DUP_UNSUBSCRIBE -> log.warn("[$username] DUP - Unsubscription(SubId: $subId)")
                else -> log.error("[$username] Unsubscribe - NOT_CONNECT")
            }
        }else{
            log.error("Value cannot be NULL (username: $username, subId: $subId)")
        }
    }

    @EventListener
    fun handleWebSocketSubscribeListener(event: SessionSubscribeEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val destination = headerAccessor.destination
        val subId = headerAccessor.subscriptionId
        val username = headerAccessor.user?.name
        if(StringUtils.hasText(username) && StringUtils.hasText(subId) && StringUtils.hasText(destination)) {
            val state = userSessionRegistry.addSession(username!!, subId!!, destination!!)
            when (state) {
                UserState.SUBSCRIBE -> log.info("Destination : $destination, SubId : $subId, [$username] Subscribed")
                UserState.DUP_SUBSCRIBE -> log.warn("[$username] DUP - Subscription(SubId: $subId), Destination : $destination")
                else -> log.error("[$username] Subscribe - NOT_CONNECT")
            }
        }else{
            log.error("Value cannot be NULL (username: $username, subId: $subId), destination: $destination")
        }
    }

    /**
     * 채팅방 열기 (ACTIVE)
     * @param roomId
     */
    @ResponseBody
    @GetMapping("/api/chat/history")
    fun getChatHistory(@RequestParam roomId: String,
                       @AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<ChatMessageResponse>> {
        val loginId = customUser.username
        val state = memberService.searchMyInfo(loginId).state
        require(state == State.APPROVED.name) { throw UnAuthorizationException(loginId, "[$state] 승인되지 않은 사용자입니다.") }
        val chatRoom = chatService.getChatRoomById(roomId) ?: throw InvalidInputException(roomId, "유효하지 않은 roomId 입니다")
        val response = chatService.getChatHistory(chatRoom, loginId)
        log.info("[$loginId] Request ChatHistory : ${response.size}")
        return BaseResponse(data = response)
    }

    @ResponseBody
    @GetMapping("/api/chat/rooms")
    fun loadAllParticipatedRooms(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<ParticipantResponse>> {
        val loginId = customUser.username
        val state = memberService.searchMyInfo(loginId).state
        require(state == State.APPROVED.name) { throw UnAuthorizationException(loginId, "[$state] 승인되지 않은 사용자입니다.") }
        val allParticipatedRooms = chatService.loadAllParticipatedRooms(loginId)
        val response = allParticipatedRooms?.map { it.toResponse() }
        log.info("[$loginId] Request Participated Rooms : ${response?.size ?: 0}")
        return BaseResponse(data = response,
            message = if (response.isNullOrEmpty()) "현재 참여중인 방이 없습니다." else "${response.size}개의 채팅방을 불러왔습니다.")
    }

    private fun messageToJSON(dto: Any): String = jacksonObjectMapper().writeValueAsString(dto)

    /**
     * 채팅방 끄기 (INACTIVE)
     * @param chatRoomDto(creator, roomType, roomId)
     */
    @MessageMapping("/chat/outRoom")
    fun outRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val loginId = headerAccessor.user!!.name
        validateLoginId(loginId, chatRoomDto, "outRoom")
        val message = MessageDto(loginId, MessageType.INACTIVE.name, chatRoomDto)
        rabbitTemplate.convertAndSend(rabbitmqConfig.directExchange().name, rabbitmqConfig.outQueue().name, messageToJSON(message))
    }

    /**
     * 메세지 전송
     * @param chatMessageDto(type, content, sender, roomId)
     */
    @MessageMapping("/chat/sendMessage")
    fun sendMessage(@Payload @Valid chatMessageDto: ChatMessageDto, headerAccessor: SimpMessageHeaderAccessor) {
        val loginId = headerAccessor.user!!.name
        validateLoginId(loginId, chatMessageDto)
        val message = MessageDto(loginId, chatMessageDto.type.name, chatMessageDto)
        rabbitTemplate.convertAndSend(rabbitmqConfig.directExchange().name, rabbitmqConfig.sendQueue().name, messageToJSON(message))
    }

    /**
     * 방 퇴장
     * @param chatRoomDto(creator, roomType, roomId)
     */
    @MessageMapping("/chat/leaveRoom")
    fun leaveRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val loginId = headerAccessor.user!!.name
        validateLoginId(loginId, chatRoomDto, "leaveRoom")
        val message = MessageDto(loginId, MessageType.LEAVE.name, chatRoomDto)
        rabbitTemplate.convertAndSend(rabbitmqConfig.directExchange().name, rabbitmqConfig.leaveQueue().name, messageToJSON(message))
    }


    /**
     * 방 초대
     * @param chatRoomDto(creator, roomType, participant)
     */
    @MessageMapping("/chat/inviteRoom")
    fun inviteRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val loginId = headerAccessor.user!!.name
        validateLoginId(loginId, chatRoomDto, "inviteRoom")
        log.info("[${chatRoomDto.creator}] invited users to room (${chatRoomDto.roomId})")
        val message = MessageDto(loginId, MessageType.JOIN.name, chatRoomDto)
        rabbitTemplate.convertAndSend(rabbitmqConfig.directExchange().name, rabbitmqConfig.inviteQueue().name, messageToJSON(message))
    }

    /**
     * 방 생성 - ALL
     * @param chatRoomDto(creator, roomType)
     */
    @Validated
    @MessageMapping("/chat/createRoom/all")
    fun createAllRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val loginId = headerAccessor.user!!.name
        validateLoginId(loginId, chatRoomDto, "createAllRoom")
        val message = MessageDto(loginId, chatRoomDto.roomType.name, chatRoomDto)
        rabbitTemplate.convertAndSend(rabbitmqConfig.directExchange().name, rabbitmqConfig.createQueue().name, messageToJSON(message))
    }

    /**
     * 방 생성 - GROUP
     * @param chatRoomDto(creator, roomType, participant)
     */
    @MessageMapping("/chat/createRoom/group")
    fun createGroupRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val loginId = headerAccessor.user!!.name
        validateLoginId(loginId, chatRoomDto, "createGroupRoom")
        val message = MessageDto(loginId, chatRoomDto.roomType.name, chatRoomDto)
        rabbitTemplate.convertAndSend(rabbitmqConfig.directExchange().name, rabbitmqConfig.createQueue().name, messageToJSON(message))
    }


    /**
     * 방 생성 - PRIVATE
     * @param chatRoomDto(creator, roomType, participant)
     */
    @MessageMapping("/chat/createRoom/private")
    fun createPrivateRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val loginId = headerAccessor.user!!.name
        validateLoginId(loginId, chatRoomDto, "createPrivateRoom")
        val message = MessageDto(loginId, chatRoomDto.roomType.name, chatRoomDto)
        rabbitTemplate.convertAndSend(rabbitmqConfig.directExchange().name, rabbitmqConfig.createQueue().name, messageToJSON(message))
    }


    private fun validateLoginId(loginId: String, chatRoomDto: ChatRoomDto, funcName: String){
        if(loginId != chatRoomDto.creator){
            chatService.sendErrorMessage(chatRoomDto.toErrorMessage("$funcName() : JWT 토큰의 loginId 와 메세지의 creator 가 일치하지 않습니다."))
            throw InvalidInputException(message = "Token [$loginId] and Creator [${chatRoomDto.creator}] is not accord")
        }
    }

    private fun validateLoginId(loginId: String, chatMessageDto: ChatMessageDto){
        if(loginId != chatMessageDto.sender){
            chatService.sendErrorMessage(chatMessageDto.toErrorMessage("sendMessage() : JWT 토큰의 loginId 와 메세지의 creator 가 일치하지 않습니다."))
            throw InvalidInputException(message = "Token [$loginId] and Creator [${chatMessageDto.sender}] is not accord")
        }
    }

    //    @MessageMapping("/chat/enterRoom")
//    fun enterRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
//        if(!chatService.validateChatRoom(chatRoomDto)) {
//            sendErrorMessage(
//                ErrorMessage(
//                    "다른 유저의 개인 채팅방에 입장할 수 없습니다.",
//                    chatRoomDto.creator,
//                    chatRoomDto.roomId
//                )
//            )
//        } else{
//            val chatMessageDto = ChatMessageDto(MessageType.JOIN, null, null, chatRoomDto.creator, chatRoomDto.privateReceiver, chatRoomDto.roomId)
//            chatService.enterRoom(chatMessageDto)
//        }
//    }

//    private fun determineMessageType(creator: String, receiver: String, roomId: String) {
//        if (chatService.isPrivateRoomExist(receiver, creator)) {
//            val lastChatMessage = chatService.getLastChatMessage(roomId, creator)
//
//            if(lastChatMessage?.type == null || lastChatMessage.type == MessageType.LEAVE){
//                chatService.canEnterRoom(ChatMessageDto(MessageType.JOIN, null, null, creator, receiver, roomId),
//                    ChatRoom(roomId, creator, 1, receiver))
//            }
//            chatService.activeRoom(creator, roomId)
////            else{
////                val chatMessageDto = ChatMessageDto(MessageType.ACTIVE, null, null, creator, receiver, roomId)
////                messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
////            }
//        } else {
//            log.info("$creator invited $receiver to the chat room.")
//            chatService.createRoom(ChatMessageDto(MessageType.CREATE, null, null, creator, receiver, roomId))
//        }
//    }



//    fun messageValueValidate(dto: Any, messageType: MessageType): Boolean {
//        val message = when (messageType) {
//            MessageType.CREATE -> if ((dto as ChatRoomDto).creator.isEmpty()) "Sender is empty!" else null
//            MessageType.JOIN, MessageType.LEAVE -> if ((dto as ChatRoomDto).creator.isEmpty() || dto.roomId.isEmpty()) "value is empty" else null
//            MessageType.CHAT -> if((dto as ChatMessageDto).sender.isEmpty() || dto.roomId.isEmpty() || dto.content.isNullOrEmpty()) "Value is empty" else null
//            else -> null
//        }
//
//        message?.let {
//            when (dto) {
//                is ChatRoomDto -> dto.toErrorMessage(it)
//                is ChatMessageDto -> dto.toErrorMessage(it)
//                else -> null
//            }.apply { sendErrorMessage(this) }
//        }
//
//        return message == null
//    }

//    private fun validateCreateRoom(chatRoomDto: ChatRoomDto, role: String): Boolean {
//        val errorMessage = when {
//            chatRoomDto.roomType == RoomType.PRIVATE && chatRoomDto.privateReceiver == null -> "PRIVATE 채팅방은 receiver 가 null 일 수 없습니다."
//            chatRoomDto.roomType == RoomType.PRIVATE && chatRoomDto.creator == chatRoomDto.privateReceiver -> "PRIVATE 채팅방의 creator 와 receiver 가 동일합니다."
//            chatRoomDto.roomType == RoomType.ALL && role != Role.ADMIN.name -> "Admin 사용자만 전체 채팅방을 만들 수 있습니다."
//            else -> null
//        }
//
//        return if (errorMessage != null) {
//            sendErrorMessage(ErrorMessage(errorMessage, chatRoomDto.creator, chatRoomDto.roomId))
//            false
//        } else {
//            true
//        }
//    }

}