//package org.example.chatkopring.chat.controller
//
//import jakarta.validation.Valid
//import org.example.chatkopring.chat.config.UserSessionRegistry
//import org.example.chatkopring.chat.dto.*
//import org.example.chatkopring.chat.service.ChatService
//import org.example.chatkopring.common.dto.BaseResponse
//import org.example.chatkopring.common.dto.CustomUser
//import org.example.chatkopring.common.exception.InvalidInputException
//import org.example.chatkopring.common.exception.UnAuthorizationException
//import org.example.chatkopring.common.status.MessageType
//import org.example.chatkopring.common.status.Role
//import org.example.chatkopring.common.status.State
//import org.example.chatkopring.member.service.MemberService
//import org.example.chatkopring.util.logger
//import org.springframework.context.event.EventListener
//import org.springframework.messaging.handler.annotation.MessageMapping
//import org.springframework.messaging.handler.annotation.Payload
//import org.springframework.messaging.simp.SimpMessageHeaderAccessor
//import org.springframework.messaging.simp.SimpMessagingTemplate
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor
//import org.springframework.security.core.annotation.AuthenticationPrincipal
//import org.springframework.stereotype.Controller
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.RequestParam
//import org.springframework.web.bind.annotation.ResponseBody
//import org.springframework.web.socket.messaging.SessionConnectEvent
//import org.springframework.web.socket.messaging.SessionDisconnectEvent
//import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
//
//@Controller
//class ChatController(
//    private val userSessionRegistry: UserSessionRegistry,
//    private val messagingTemplate: SimpMessagingTemplate,
//    private val chatService: ChatService,
//    private val memberService: MemberService,
//) {
//    private val log = logger()
//
//    @EventListener
//    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
//        val user = StompHeaderAccessor.wrap(event.message).user
//        log.info("New Connection : $user")
//        if(user != null) {
//            userSessionRegistry.registerSession(user.name)
//            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.CONNECT, user.name))
//        }
//    }
//
//    @EventListener
//    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
//        val user = StompHeaderAccessor.wrap(event.message).user
//        log.info("User Disconnected : $user")
//        if(user != null) {
//            userSessionRegistry.removeSession(user.name)
//            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.DISCONNECT, user.name))
//        }
//    }
//
//    @EventListener
//    fun handleWebSocketUnsubscribeListener(event: SessionUnsubscribeEvent) {
//        val user = StompHeaderAccessor.wrap(event.message).user
//        log.info("User Unsubscribed : $user")
//        if(user != null) {
//            // 모든 방 INACTIVE
//            val allParticipatedRooms = chatService.loadAllParticipatedRooms(user.name)
//            allParticipatedRooms?.forEach { chatService.inactiveRoom(it.toChatRoomDto()) }
//
//        }
//    }
//
//    @ResponseBody
//    @GetMapping("/chat/history")
//    fun getChatHistory(@RequestParam roomId: String,
//                       @AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<ChatMessageResponse>> {
//        val loginId = customUser.username
//        val state = memberService.searchMyInfo(loginId).state
//        require(state == State.APPROVED.name) { throw UnAuthorizationException(loginId, "[$state] 승인되지 않은 사용자입니다.") }
//        val chatRoom = chatService.getChatRoomById(roomId) ?: throw InvalidInputException(roomId, "유효하지 않은 roomId 입니다")
//        val response = chatService.getChatHistory(chatRoom, loginId)
//        return BaseResponse(data = response)
//    }
//
//    @ResponseBody
//    @GetMapping("/chat/rooms")
//    fun loadAllParticipatedRooms(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<ParticipantResponse>> {
//        val loginId = customUser.username
//        val state = memberService.searchMyInfo(loginId).state
//        require(state == State.APPROVED.name) { throw UnAuthorizationException(loginId, "[$state] 승인되지 않은 사용자입니다.") }
//        val allParticipatedRooms = chatService.loadAllParticipatedRooms(loginId)
//        val response = allParticipatedRooms?.map { it.toResponse() }
//        return BaseResponse(data = response,
//            message = if (response.isNullOrEmpty()) "현재 참여중인 방이 없습니다." else "${response.size}개의 채팅방을 불러왔습니다.")
//    }
//
//    /**
//     * 채팅방 끄기 (INACTIVE)
//     * @param chatRoomDto(creator, roomType, roomId)
//     */
//    @MessageMapping("/chat/outRoom")
//    fun outRoom(@Payload @Valid chatRoomDto: ChatRoomDto) {
//        chatService.inactiveRoom(chatRoomDto)
//    }
//
//    /**
//     * 메세지 전송
//     * @param chatMessageDto(type, content, sender, roomId)
//     */
//    @MessageMapping("/chat/sendMessage")
//    fun sendMessage(@Payload @Valid chatMessageDto: ChatMessageDto) {
//        val chatRoom = chatService.getChatRoomById(chatMessageDto.roomId)
//        requireNotNull(chatRoom)
//        chatService.sendMessage(chatMessageDto, chatRoom)
//    }
//
//    /**
//     * 방 퇴장
//     * @param chatRoomDto(creator, roomType, roomId)
//     */
//    @MessageMapping("/chat/leaveRoom")
//    fun leaveRoom(@Payload @Valid chatRoomDto: ChatRoomDto) =
//        chatService.leaveRoom(chatRoomDto)
//
//
//    /**
//     * 방 초대
//     * @param chatRoomDto(creator, roomType, participant)
//     */
//    @MessageMapping("/chat/inviteRoom")
//    fun inviteRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
//        val loginId = headerAccessor.user!!.name
//        chatService.inviteRoom(chatRoomDto, loginId)
//    }
//
//    /**
//     * 방 생성 - ALL
//     * @param chatRoomDto(creator, roomType)
//     */
//    @MessageMapping("/chat/createRoom/all")
//    fun createAllRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
//        val loginId = headerAccessor.user!!.name
//        val member = memberService.searchMyInfo(loginId)
//        val (roomId, creator, roomType, title) = chatRoomDto
//        if (member.role != Role.ADMIN.name || chatRoomDto.creator != loginId) {
//            sendErrorMessage(ErrorMessage("Admin 사용자만 전체 채팅방을 만들 수 있습니다.", creator, roomId))
//        }else{
//            chatService.createRoom(chatRoomDto)
//            val members = memberService.findColleague(member.id)
//            // ADMIN -> 전체 참여(생성자 포함)
//            members.forEach {
//                val roomDto = ChatRoomDto(roomId, it.loginId, roomType)
//                chatService.joinRoom(roomDto)
//                if (it.loginId != loginId) chatService.inactiveRoom(roomDto)
//                else chatService.activeRoom(chatRoomDto)
//            }
//        }
//    }
//
//    /**
//     * 방 생성 - GROUP
//     * @param chatRoomDto(creator, roomType, participant)
//     */
//    @MessageMapping("/chat/createRoom/group")
//    fun createGroupRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
//        val (roomId, creator, roomType, title) = chatRoomDto
//        if (chatRoomDto.participant.isNullOrEmpty()) {
//            sendErrorMessage(ErrorMessage("Group 채팅방은 creator 를 제외한 참여자가 1명 이상이어야 합니다.", creator, roomId))
//        }else{
//            chatService.createRoom(chatRoomDto)
//            // 방 생성자 JOIN
//            chatService.joinRoom(chatRoomDto)
//            chatService.activeRoom(chatRoomDto)
//            // 참여자 강제 JOIN
//            chatRoomDto.participant.forEach {
//                val roomDto = ChatRoomDto(roomId, it, roomType)
//                chatService.joinRoom(roomDto)
//                chatService.inactiveRoom(roomDto)
//            }
//        }
//    }
//
//
//    /**
//     * 방 생성 - PRIVATE
//     * @param chatRoomDto(creator, roomType, participant)
//     */
//    @MessageMapping("/chat/createRoom/private")
//    fun createPrivateRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
//        val (roomId, creator, roomType, title) = chatRoomDto
//        val receiver = chatRoomDto.participant?.firstOrNull()
//        if(receiver == null || receiver == creator) {
//            sendErrorMessage(ErrorMessage("Private 채팅방은 자신을 제외한 참여자가 있어야 합니다.", creator, roomId))
//        }else{
//            val alreadyCreatedRoomId = chatService.isPrivateRoomExist(creator, receiver, chatRoomDto)
//            if(alreadyCreatedRoomId != null){
//                log.info("Already private room existed (${creator} & ${receiver})")
//                val lastChatMessage = chatService.getLastChatMessage(alreadyCreatedRoomId, creator)
//                if(lastChatMessage.type == MessageType.LEAVE) {
//                    // 재입장
//                    chatService.joinRoom(chatRoomDto)
//                }
//                // 재입장 활성화 or 활성화
//                chatService.activeRoom(chatRoomDto)
//            }else{
//                chatService.createRoom(chatRoomDto)
//                // 생성자 JOIN
//                chatService.joinRoom(chatRoomDto)
//                chatService.activeRoom(chatRoomDto)
//                // 상대방 강제 JOIN
//                val roomDto = ChatRoomDto(roomId, receiver, roomType, title)
//                chatService.joinRoom(roomDto)
//                chatService.inactiveRoom(roomDto)
//            }
//        }
//    }
//
//    fun sendErrorMessage(errorMessage: ErrorMessage?) {
//        log.error("WebSocketError : $errorMessage")
//        if (errorMessage != null) {
//            messagingTemplate.convertAndSend("/sub/chat/${errorMessage.roomId}", errorMessage)
//        }
//    }
//
//    //    @MessageMapping("/chat/enterRoom")
////    fun enterRoom(@Payload @Valid chatRoomDto: ChatRoomDto, headerAccessor: SimpMessageHeaderAccessor) {
////        if(!chatService.validateChatRoom(chatRoomDto)) {
////            sendErrorMessage(
////                ErrorMessage(
////                    "다른 유저의 개인 채팅방에 입장할 수 없습니다.",
////                    chatRoomDto.creator,
////                    chatRoomDto.roomId
////                )
////            )
////        } else{
////            val chatMessageDto = ChatMessageDto(MessageType.JOIN, null, null, chatRoomDto.creator, chatRoomDto.privateReceiver, chatRoomDto.roomId)
////            chatService.enterRoom(chatMessageDto)
////        }
////    }
//
////    private fun determineMessageType(creator: String, receiver: String, roomId: String) {
////        if (chatService.isPrivateRoomExist(receiver, creator)) {
////            val lastChatMessage = chatService.getLastChatMessage(roomId, creator)
////
////            if(lastChatMessage?.type == null || lastChatMessage.type == MessageType.LEAVE){
////                chatService.canEnterRoom(ChatMessageDto(MessageType.JOIN, null, null, creator, receiver, roomId),
////                    ChatRoom(roomId, creator, 1, receiver))
////            }
////            chatService.activeRoom(creator, roomId)
//////            else{
//////                val chatMessageDto = ChatMessageDto(MessageType.ACTIVE, null, null, creator, receiver, roomId)
//////                messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
//////            }
////        } else {
////            log.info("$creator invited $receiver to the chat room.")
////            chatService.createRoom(ChatMessageDto(MessageType.CREATE, null, null, creator, receiver, roomId))
////        }
////    }
//
//
//
////    fun messageValueValidate(dto: Any, messageType: MessageType): Boolean {
////        val message = when (messageType) {
////            MessageType.CREATE -> if ((dto as ChatRoomDto).creator.isEmpty()) "Sender is empty!" else null
////            MessageType.JOIN, MessageType.LEAVE -> if ((dto as ChatRoomDto).creator.isEmpty() || dto.roomId.isEmpty()) "value is empty" else null
////            MessageType.CHAT -> if((dto as ChatMessageDto).sender.isEmpty() || dto.roomId.isEmpty() || dto.content.isNullOrEmpty()) "Value is empty" else null
////            else -> null
////        }
////
////        message?.let {
////            when (dto) {
////                is ChatRoomDto -> dto.toErrorMessage(it)
////                is ChatMessageDto -> dto.toErrorMessage(it)
////                else -> null
////            }.apply { sendErrorMessage(this) }
////        }
////
////        return message == null
////    }
//
////    private fun validateCreateRoom(chatRoomDto: ChatRoomDto, role: String): Boolean {
////        val errorMessage = when {
////            chatRoomDto.roomType == RoomType.PRIVATE && chatRoomDto.privateReceiver == null -> "PRIVATE 채팅방은 receiver 가 null 일 수 없습니다."
////            chatRoomDto.roomType == RoomType.PRIVATE && chatRoomDto.creator == chatRoomDto.privateReceiver -> "PRIVATE 채팅방의 creator 와 receiver 가 동일합니다."
////            chatRoomDto.roomType == RoomType.ALL && role != Role.ADMIN.name -> "Admin 사용자만 전체 채팅방을 만들 수 있습니다."
////            else -> null
////        }
////
////        return if (errorMessage != null) {
////            sendErrorMessage(ErrorMessage(errorMessage, chatRoomDto.creator, chatRoomDto.roomId))
////            false
////        } else {
////            true
////        }
////    }
//
//}