package org.example.chatkopring.chat.controller

import org.example.chatkopring.chat.config.UserSessionRegistry
import org.example.chatkopring.chat.dto.*
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.chat.service.ChatService
import org.example.chatkopring.common.dto.BaseResponse
import org.example.chatkopring.common.status.ResultCode
import org.example.chatkopring.util.logger
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

@Controller
class ChatController(
    private val userSessionRegistry: UserSessionRegistry,
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
) {
    private val log = logger()

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
        val user = StompHeaderAccessor.wrap(event.message).user
        log.info("New Connection : $user")
        if(user != null) {
            userSessionRegistry.registerSession(user.name)
            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.CONNECT, user.name, null))
        }
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val user = StompHeaderAccessor.wrap(event.message).user
        log.info("User Disconnected : $user")
        if(user != null) {
            userSessionRegistry.removeSession(user.name)
            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.DISCONNECT, user.name, null))
        }
    }

    @EventListener
    fun handleWebSocketUnsubscribeListener(event: SessionUnsubscribeEvent) {
        val user = StompHeaderAccessor.wrap(event.message).user
        log.info("User Unsubscribed : $user")
        if(user != null) {
            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.INACTIVE, user.name, null))
        }
    }


    @MessageMapping("/chat/sendMessage")
    fun sendMessage(@Payload chatMessageDto: ChatMessageDto) {
        require(messageValueValidate(chatMessageDto, MessageType.CHAT))
        val chatRoom = chatService.getChatRoomById(chatMessageDto.roomId)
        requireNotNull(chatRoom)
        val chatRoomDto = ChatRoomDto(chatRoom.id, chatMessageDto.sender, chatRoom.receiver)
        if(!chatService.validateChatRoom(chatRoomDto)) {
            sendErrorMessage(
                ErrorMessage(
                    "다른 유저의 개인 채팅방에 메세지를 전송할 수 없습니다.",
                    chatRoomDto.sender,
                    chatRoomDto.receiver,
                    chatRoomDto.roomId
                )
            )
        } else{
            chatService.sendMessage(chatMessageDto, chatRoom)
        }
    }

    @MessageMapping("/chat/enterRoom")
    fun enterRoom(@Payload chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        require(messageValueValidate(chatRoomDto, MessageType.JOIN))
        if(!chatService.validateChatRoom(chatRoomDto)) {
            sendErrorMessage(
                ErrorMessage(
                    "다른 유저의 개인 채팅방에 입장할 수 없습니다.",
                    chatRoomDto.sender,
                    chatRoomDto.receiver,
                    chatRoomDto.roomId
                )
            )
        } else{
            val chatMessageDto = ChatMessageDto(MessageType.JOIN, null, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
            chatService.enterRoom(chatMessageDto)
        }
    }

    @MessageMapping("/chat/leaveRoom")
    fun leaveRoom(@Payload chatRoomDto: ChatRoomDto) {
        require(messageValueValidate(chatRoomDto, MessageType.LEAVE))
        if(!chatService.validateChatRoom(chatRoomDto)) {
            sendErrorMessage(
                ErrorMessage(
                    "다른 유저의 개인 채팅방에서 퇴장할 수 없습니다.",
                    chatRoomDto.sender,
                    chatRoomDto.receiver,
                    chatRoomDto.roomId
                )
            )
        } else{
            val chatMessageDto = ChatMessageDto(MessageType.LEAVE, null, null, chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId)
            chatService.leaveRoom(chatMessageDto)
        }
    }

    @ResponseBody
    @GetMapping("/chat/history")
    fun requestHistory(@RequestParam roomId: String, @RequestParam loginId: String) =
        if(chatService.validateEnterHistory(roomId, loginId)) {
            ResponseEntity(BaseResponse(data = chatService.getRoomHistory(roomId, loginId)), HttpStatus.OK)
        } else {
            ResponseEntity(BaseResponse(ResultCode.ERROR.name, "입장중인 방이 아닙니다.", ResultCode.ERROR.msg), HttpStatus.BAD_REQUEST)
        }

    @MessageMapping("/chat/createRoom")
    fun createRoom(@Payload chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
        require(messageValueValidate(chatRoomDto, MessageType.CREATE))
        val (roomId, creator, receiver) = chatRoomDto
        if(creator == receiver) {
            sendErrorMessage(
                ErrorMessage(
                    "creator 와 receiver 가 동일합니다.",
                    chatRoomDto.sender,
                    chatRoomDto.receiver,
                    chatRoomDto.roomId
                )
            )
        }else{
            when (receiver) {
                "ALL" -> {
                    log.info("$creator created a public chat room.")
                    chatService.createRoom(ChatMessageDto(MessageType.CREATE, null, null, creator, receiver, roomId))
                }
                else -> determineMessageType(creator, receiver, roomId)
            }
        }
    }

    private fun determineMessageType(creator: String, receiver: String, roomId: String) {
        if (chatService.isPrivateRoomExist(receiver, creator)) {
            log.info("Already private room existed ($creator & $receiver)")
            val lastChatMessage = chatService.getLastChatMessage(roomId, creator)

            if(lastChatMessage?.type == null || lastChatMessage.type == MessageType.LEAVE){
                chatService.canEnterRoom(ChatMessageDto(MessageType.JOIN, null, null, creator, receiver, roomId),
                    ChatRoom(roomId, creator, 1, receiver))
            }
            chatService.activeRoom(creator, roomId)
//            else{
//                val chatMessageDto = ChatMessageDto(MessageType.ACTIVE, null, null, creator, receiver, roomId)
//                messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
//            }
        } else {
            log.info("$creator invited $receiver to the chat room.")
            chatService.createRoom(ChatMessageDto(MessageType.CREATE, null, null, creator, receiver, roomId))
        }
    }

    fun sendErrorMessage(errorMessage: ErrorMessage?) {
        log.error("WebSocketError : $errorMessage")
        if (errorMessage != null) {
            messagingTemplate.convertAndSend("/sub/chat/${errorMessage.roomId}", errorMessage)
        }
    }

    fun messageValueValidate(dto: Any, messageType: MessageType): Boolean {
        val message = when (messageType) {
            MessageType.CREATE -> if ((dto as ChatRoomDto).sender.isEmpty()) "Sender is empty!" else null
            MessageType.JOIN, MessageType.LEAVE -> if ((dto as ChatRoomDto).sender.isEmpty() || dto.roomId.isEmpty()) "value is empty" else null
            MessageType.CHAT -> if((dto as ChatMessageDto).sender.isEmpty() || dto.roomId.isEmpty() || dto.content.isNullOrEmpty()) "Value is empty" else null
            else -> null
        }

        message?.let {
            when (dto) {
                is ChatRoomDto -> dto.toErrorMessage(it)
                is ChatMessageDto -> dto.toErrorMessage(it)
                else -> null
            }.apply { sendErrorMessage(this) }
        }

        return message == null
    }

}