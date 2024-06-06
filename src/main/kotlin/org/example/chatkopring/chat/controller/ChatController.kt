package org.example.chatkopring.chat.controller

import org.example.chatkopring.chat.config.UserSessionRegistry
import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.dto.ChatMessageResponse
import org.example.chatkopring.chat.dto.ChatRoomDto
import org.example.chatkopring.chat.dto.MessageType
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.service.ChatService
import org.example.chatkopring.common.exception.UnAuthorizationException
import org.example.chatkopring.util.logger
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Controller
class ChatController(
    private val userSessionRegistry: UserSessionRegistry,
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
) {
    private val log = logger()

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
        val sessionId = StompHeaderAccessor.wrap(event.message).sessionId!!
        log.info("New Connection : $sessionId")
        userSessionRegistry.registerSession(sessionId)
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val sessionId = StompHeaderAccessor.wrap(event.message).sessionId!!
        log.info("User Disconnected : $sessionId")
        userSessionRegistry.removeSession(sessionId)
    }

    @MessageMapping("/chat/sendMessage")
    fun sendMessage(@Payload chatMessageDto: ChatMessageDto) {
        chatService.sendMessage(chatMessageDto)
        messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
        log.info("${chatMessageDto.sender} sent message to room (${chatMessageDto.roomId})")
    }

    @MessageMapping("/chat/enterRoom")
    fun enterRoom(@Payload chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        require(chatService.validateChatRoom(chatRoomDto)) { throw UnAuthorizationException(chatRoomDto.roomId, "개인 채팅방에 입장할 수 없습니다.") }
        log.info("${chatRoomDto.sender} joined the room (${chatRoomDto.roomId})")
        val chatMessageDto = ChatMessageDto(MessageType.JOIN, null, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
        chatService.enterRoom(chatMessageDto)
        messagingTemplate.convertAndSend("/sub/chat/${chatRoomDto.roomId}", chatMessageDto)
    }

    @MessageMapping("/chat/leaveRoom")
    fun leaveRoom(@Payload chatRoomDto: ChatRoomDto) {
        require(chatService.validateChatRoom(chatRoomDto)) { throw UnAuthorizationException(chatRoomDto.roomId, "개인 채팅방에 접근할 수 없습니다.") }
        log.info("${chatRoomDto.sender} leaved the room (${chatRoomDto.roomId})")
        val chatMessageDto = ChatMessageDto(MessageType.LEAVE, null, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
        chatService.leaveRoom(chatMessageDto)
        messagingTemplate.convertAndSend("/sub/chat/${chatRoomDto.roomId}", chatMessageDto)
    }

    @ResponseBody
    @GetMapping("/chat/history")
    fun requestHistory(@RequestParam roomId: String, @RequestParam loginId: String): List<ChatMessageResponse> =
        chatService.activeRoom(roomId, loginId)

    @MessageMapping("/chat/createRoom")
    fun createRoom(@Payload chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val (roomId, sender, receiver) = chatRoomDto
        val messageType = when (receiver) {
            "ALL" -> {
                log.info("$sender created a public chat room.")
                chatService.createRoom(ChatMessageDto(MessageType.CREATE, null, null, sender, receiver, roomId))
                MessageType.CREATE
            }
            else -> determineMessageType(sender, receiver, roomId)
        }
        val chatMessageDto = ChatMessageDto(messageType, null, null, sender, receiver, roomId)
        messagingTemplate.convertAndSend("/sub/chat/$receiver", chatMessageDto)
    }

    private fun determineMessageType(sender: String, receiver: String, roomId: String): MessageType {
        return if (chatService.isPrivateRoomExist(receiver, sender)) {
            log.info("Already private room existed ($sender & $receiver)")
            MessageType.ACTIVE
        } else {
            log.info("$sender invited $receiver to the chat room.")
            chatService.createRoom(ChatMessageDto(MessageType.CREATE, null, null, sender, receiver, roomId))
            MessageType.CREATE
        }
    }
//    fun createRoom(@Payload chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
//        val receiver = chatRoomDto.receiver
//        if (receiver == "ALL") {
//            log.info("${chatRoomDto.sender} created a public chat room.")
//        } else{
//            val messageType = if (chatService.isPrivateRoomExist(chatRoomDto.receiver, chatRoomDto.sender)) {
//                log.info("Already private room existed (${chatRoomDto.sender} & ${chatRoomDto.receiver})")
//                MessageType.ACTIVE
//            } else{
//                log.info("${chatRoomDto.sender} invited ${chatRoomDto.receiver} to the chat room.")
//                chatService.createRoom(ChatMessageDto(MessageType.CREATE, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId))
//                MessageType.CREATE
//            }
//            val chatMessageDto = ChatMessageDto(messageType, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
//            messagingTemplate.convertAndSend("/sub/chat/${chatRoomDto.roomId}", chatMessageDto)
//        }
//    }
}