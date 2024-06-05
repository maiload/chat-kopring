package org.example.chatkopring.chat.controller

import org.example.chatkopring.chat.config.UserSessionRegistry
import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.dto.ChatRoomDto
import org.example.chatkopring.chat.dto.MessageType
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.service.ChatService
import org.example.chatkopring.util.logger
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
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
        log.info("${chatMessageDto.sender} sent message to room (${chatMessageDto.roomId})")
        messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
        chatService.sendMessage(chatMessageDto)
    }

    @MessageMapping("/chat/enterRoom")
    fun enterRoom(@Payload chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        log.info("${chatRoomDto.sender} joined the room (${chatRoomDto.roomId})")
        val sessionId = headerAccessor.sessionId!!
        userSessionRegistry.registerSession(sessionId, chatRoomDto.sender)
        val chatMessageDto = ChatMessageDto(MessageType.JOIN, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
        messagingTemplate.convertAndSend("/sub/chat/${chatRoomDto.roomId}", chatMessageDto)
        chatService.enterRoom(chatMessageDto)
    }

    @MessageMapping("/chat/leaveRoom")
    fun leaveRoom(@Payload chatRoomDto: ChatRoomDto) {
        log.info("${chatRoomDto.sender} leaved the room (${chatRoomDto.roomId})")
        val chatMessageDto = ChatMessageDto(MessageType.LEAVE, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
        messagingTemplate.convertAndSend("/sub/chat/${chatRoomDto.roomId}", chatMessageDto)
        chatService.leaveRoom(chatMessageDto)
    }

    @ResponseBody
    @GetMapping("/chat/history")
    fun requestHistory(@RequestParam roomId: String, @RequestParam loginId: String): List<ChatMessage> =
        chatService.activeRoom(roomId, loginId)

    @MessageMapping("/chat/createRoom")
    fun createRoom(@Payload chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        val receiver = chatRoomDto.receiver
        if (receiver == "ALL") {
            log.info("${chatRoomDto.sender} created a public chat room.")
        } else{
            log.info("${chatRoomDto.sender} invited ${chatRoomDto.receiver} to the chat room.")
        }
        val sessionId = headerAccessor.sessionId!!
        userSessionRegistry.registerSession(sessionId, chatRoomDto.sender)
        val chatMessageDto = ChatMessageDto(MessageType.CREATE, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
        messagingTemplate.convertAndSend("/sub/chat/${chatRoomDto.roomId}", chatMessageDto)
        chatService.createRoom(chatMessageDto)
    }
}