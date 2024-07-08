package org.example.chatkopring.chat.service

import jakarta.transaction.Transactional
import org.example.chatkopring.chat.dto.*
import org.example.chatkopring.chat.entity.ChatImage
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.chat.entity.Participant
import org.example.chatkopring.chat.repository.ChatImageRepository
import org.example.chatkopring.chat.repository.ChatMessageRepository
import org.example.chatkopring.chat.repository.ChatRoomRepository
import org.example.chatkopring.chat.repository.ParticipantRepository
import org.example.chatkopring.common.exception.InvalidInputException
import org.example.chatkopring.common.service.ImageService
import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.common.status.RoomType
import org.example.chatkopring.util.logger
import org.springframework.data.domain.PageRequest
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.jvm.optionals.getOrNull

//const val CHAT_IMAGE_OUTPUT_PATH: String = "src/main/resources/images/chat/"

@Transactional
@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatImageRepository: ChatImageRepository,
    private val participantRepository: ParticipantRepository,
    private val messagingTemplate: SimpMessagingTemplate,
    private val imageService: ImageService,
) {
    val log = logger()

    fun createRoom(chatRoomDto: ChatRoomDto): Boolean {
        if (roomIdValidate(chatRoomDto.roomId)) {
            sendErrorMessage(chatRoomDto.toErrorMessage("이미 존재하는 roomId 입니다."))
            return false;
        }else{
            chatRoomRepository.save(chatRoomDto.toEntity())
            val createChatMessage = chatRoomDto.makeChatMessage(MessageType.CREATE)
            chatMessageRepository.save(createChatMessage)
            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(createChatMessage.type, createChatMessage.sender, chatRoomDto.roomId))
            messagingTemplate.convertAndSend("/sub/chat/${chatRoomDto.roomId}", createChatMessage)
            log.info("[${chatRoomDto.creator}] create [${chatRoomDto.roomType}] room (${chatRoomDto.roomId})")
            return true;
        }
    }

    fun roomIdValidate(roomId: String): Boolean = chatRoomRepository.existsById(roomId)


    fun isPrivateRoomExist(creator: String, receiver: String): String? {
        val creatorChatRoomList = chatRoomRepository.findByRoomTypeAndCreatorAndValid(RoomType.PRIVATE, creator, true)
        val receiverRoomId = creatorChatRoomList
            ?.filter { chatRoom -> chatRoom.participants!!.any { it.loginId == receiver } }
            ?.map { it.id }
            ?.firstOrNull()

        val receiverChatRoomList = chatRoomRepository.findByRoomTypeAndCreatorAndValid(RoomType.PRIVATE, receiver, true)
        val creatorRoomId = receiverChatRoomList
            ?.filter { chatRoom -> chatRoom.participants!!.any{ it.loginId == creator } }
            ?.map { it.id }
            ?.firstOrNull()

        return when {
            receiverRoomId != null -> receiverRoomId
            creatorRoomId != null -> creatorRoomId
            else -> null
        }
    }

    fun getLastChatMessage(roomId: String, sender: String) =
        chatMessageRepository.findFirstByRoomIdOrderByIdDesc(roomId, sender, PageRequest.of(0,1)).first()

    fun isInactivatedRoom(chatRoom: ChatRoom, loginId: String): Boolean =
        chatMessageRepository.existsByChatRoomAndSenderAndType(chatRoom, loginId, MessageType.INACTIVE)

    fun sendMessage(chatMessageDto: ChatMessageDto, chatRoom: ChatRoom) {
        val chatRoomDto = chatMessageDto.toChatRoomDto(chatRoom.roomType)
        val (roomId, creator, roomType, title) = chatRoomDto
        val isJoinedRoom = participantRepository.existsByChatRoomAndLoginId(chatRoom, creator)
        val isInActive = chatMessageRepository.existsByChatRoomAndSenderAndType(chatRoom, creator, MessageType.INACTIVE)
        if(isJoinedRoom && !isInActive){
            val chatMessage = chatMessageDto.makeChatMessage(chatRoom)
            // 이미지 처리
            if (chatMessageDto.type == MessageType.IMAGE){
                val chatImage = imageService.uploadChatImage(chatMessageDto, chatMessage)
                chatImageRepository.save(chatImage)
            }

            chatMessageRepository.save(chatMessage)
            messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto.apply { this.createdDate = chatMessage.createdDate })

            val participants = chatRoomRepository.findById(chatMessageDto.roomId).get().participants
            participants?.forEach {
                // 참여자가 Inactive 면 unreadMsgNumber 증가
                if(chatMessageRepository.existsByChatRoomAndSenderAndType(chatRoom, it.loginId, MessageType.INACTIVE)) {
                    it.unreadMsgNumber++
                }
            }
            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(chatMessageDto.type, chatMessageDto.sender, chatMessageDto.roomId))
            log.info("[${chatMessageDto.sender}] sent message to room (${chatMessageDto.roomId})")
        }else{
            sendErrorMessage(chatRoomDto.toErrorMessage("sendMessage() : 입장중인 방이 아니거나 Active 되지 않았습니다."))
        }
    }

    fun activeRoom(chatRoomDto: ChatRoomDto) {
        // INACTIVE ChatMessage 삭제
        chatMessageRepository.deleteByChatRoomAndTypeAndSender(chatRoomDto.toEntity(), MessageType.INACTIVE, chatRoomDto.creator)
        // UnreadMsgNumber 초기화
        val participant = participantRepository.findByLoginIdAndChatRoom(chatRoomDto.creator, chatRoomDto.toEntity())
        if (participant.unreadMsgNumber != 0) participantRepository.save(participant.resetUnreadNumber())
        messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.ACTIVE, chatRoomDto.creator, chatRoomDto.roomId))
        log.info("[${chatRoomDto.creator}] activate the room (${chatRoomDto.roomId})")
    }

    fun inactiveRoom(chatRoomDto: ChatRoomDto) {
        val chatRoom = chatRoomDto.toEntity()
        val (roomId, creator, roomType, title) = chatRoomDto
        val isJoinedRoom = participantRepository.existsByChatRoomAndLoginId(chatRoom, creator)
        val isInActive = chatMessageRepository.existsByChatRoomAndSenderAndType(chatRoom, creator, MessageType.INACTIVE)
        if(isJoinedRoom && !isInActive) {
            chatMessageRepository.save(chatRoomDto.makeChatMessage(MessageType.INACTIVE))
            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.INACTIVE, creator, roomId))
            log.info("[${chatRoomDto.creator}] inactivate the room (${chatRoomDto.roomId})")
        }else{
            sendErrorMessage(chatRoomDto.toErrorMessage("inactiveRoom() : 입장중인 방이 아니거나 이미 INACTIVE 상태 입니다."))
        }
    }

    fun loadAllParticipatedRooms(loginId: String) = participantRepository.findByLoginIdOrderByIdDesc(loginId)

    fun joinRoom(chatRoomDto: ChatRoomDto) {
        val (roomId, creator, roomType, title) = chatRoomDto
        participantRepository.save(Participant(chatRoomDto.toEntity(), creator))
        val joinChatMessage = chatRoomDto.makeChatMessage(MessageType.JOIN)
        chatMessageRepository.save(joinChatMessage)
        messagingTemplate.convertAndSend("/sub/chat/${roomId}", joinChatMessage)
        log.info("[$creator] joined the room ($roomId)")
    }

    fun inviteRoom(chatRoomDto: ChatRoomDto, loginId: String) {
        val (roomId, creator, roomType, title) = chatRoomDto
        val chatRoom = chatRoomDto.toEntity()
        val isJoinedRoom = participantRepository.existsByChatRoomAndLoginId(chatRoom, loginId)
        val participants = chatRoomDto.participant
        if(isJoinedRoom && !participants.isNullOrEmpty()){
            participants.forEach {
                // 참가해 있지 않을 때에만 참가
                if(it != creator && !participantRepository.existsByChatRoomAndLoginId(chatRoom, it)) {
                    val roomDto = ChatRoomDto(roomId, it, roomType)
                    joinRoom(roomDto)
                    inactiveRoom(roomDto)
                }else{
                    sendErrorMessage(ErrorMessage("이미 참가중인 사용자입니다.", it, roomId))
                }
            }
        }else{
            sendErrorMessage(ErrorMessage("inviteRoom() : 입장중인 방이 아니거나 참여자 리스트가 없습니다.", loginId, roomId))
        }
    }


    // 퇴장
    fun leaveRoom(chatRoomDto: ChatRoomDto) {
        val (roomId, creator, roomType, title) = chatRoomDto
        val chatRoom = chatRoomDto.toEntity()
        val isJoinedRoom = participantRepository.existsByChatRoomAndLoginId(chatRoom, creator)
        if(isJoinedRoom){
            participantRepository.deleteByChatRoomAndLoginId(chatRoomDto.toEntity(), creator)
            val leaveChatMessage = chatRoomDto.makeChatMessage(MessageType.LEAVE)
            chatMessageRepository.save(leaveChatMessage)
            
            if (!participantRepository.existsByChatRoom(chatRoom)) {
                // 채팅방에 남은 참여자가 없으면 INVALID 로 변경
                chatRoom.valid = false
                chatRoomRepository.save(chatRoom)
                log.info("[$creator] leaved the room (${roomId}) and the room changed INVALID")
            } else{
                log.info("[$creator] leaved the room (${roomId})")
            }
            messagingTemplate.convertAndSend("/sub/chat/${roomId}", leaveChatMessage)
        }else{
            sendErrorMessage(chatRoomDto.toErrorMessage("입장중인 방이 아닙니다."))
        }
    }


    fun getChatHistory(chatRoom: ChatRoom, loginId: String): List<ChatMessageResponse> {
        // 참여 중인 방인지 확인
        require(participantRepository.existsByChatRoomAndLoginId(chatRoom, loginId)) { throw InvalidInputException(message = "참여중인 채팅방이 아닙니다.") }
        // 활성화
        activeRoom(chatRoom.toChatRoomDto(loginId))
        // 마지막 JOIN 기록
//        val lastJoinChatMessage = chatMessageRepository.findFirstByRoomIdAndTypeOrderByIdDesc(roomId, MessageType.JOIN, sender, PageRequest.of(0, 1))?.firstOrNull()
        val lastJoinChatMessage = chatMessageRepository.findFirstByChatRoomAndTypeAndSenderOrderByIdDesc(chatRoom, MessageType.JOIN, loginId)

        // JOIN 기록이 없다면 방 생성자
//        val lastJoinHistoryId = lastJoinChatMessage?.id ?: 1
        // 최근 채팅 히스토리 기준으로 100개까지만 로드
//        return chatMessageRepository.findByRoomIdAndMessageIdGreaterThanEqual(roomId, lastJoinHistoryId, PageRequest.of(0, 100))
        return chatMessageRepository.findByChatRoomAndIdGreaterThanEqual(chatRoom, lastJoinChatMessage.id!!, PageRequest.of(0, 100))
            .map {
                var base64Image: String? = null
                if (it.type == MessageType.IMAGE) {
                    val storageFilename = chatImageRepository.findByChatMessage(it).storageFileName
                    base64Image = imageService.encodeImageToBase64(storageFilename)
                }
                it.toChatMessageResponse(base64Image)
            }
    }

    fun getChatRoomById(roomId: String) = chatRoomRepository.findById(roomId).getOrNull()

    fun sendErrorMessage(errorMessage: ErrorMessage?) {
        log.error("WebSocketError : $errorMessage")
        if (errorMessage != null) {
            messagingTemplate.convertAndSend("/sub/chat/${errorMessage.roomId}", errorMessage)
        }
    }

    //    fun isReceiverLeaveTheRoom(roomId: String): Boolean {
//        // 오픈 채팅방은 public 알림 제외
//        val chatRoom = getChatRoomById(roomId)
//        return chatRoom?.receiver == "ALL" || chatRoom?.joinNumber == 1L
//    }


    //    fun validateChatRoom(chatRoomDto: ChatRoomDto): Boolean {
//        val chatRoom = chatRoomRepository.findById(chatRoomDto.roomId).getOrNull()
//        if (chatRoom == null) {
//            sendErrorMessage(ErrorMessage("존재하지 않는 방입니다.", chatRoomDto.creator, chatRoomDto.roomId))
//            throw RuntimeException()
//        }
//        chatRoom.participants.
//        val creator = chatRoom.creator
////        log.info("validate : $receiver, $creator, ${chatRoomDto.sender}")
//        return receiver == "ALL" || receiver == chatRoomDto.creator || creator == chatRoomDto.creator
//    }

//    fun enterRoom(chatRoomDto: ChatRoomDto) {
//        val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
//        if(chatMessageDto.sender in listOf(chatRoom.receiver, chatRoom.creator)){
//            if(chatRoom.joinNumber == 2L){
//                // 개인 -> 활성화
//                activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
//            }else{
//                val lastChatMessage = getLastChatMessage(chatMessageDto.roomId, chatMessageDto.sender)
//                if(chatRoom.joinNumber == 1L && chatMessageDto.sender == chatRoom.creator){
//                    if(lastChatMessage?.type == MessageType.LEAVE) {
//                        // 본인방 퇴장 후 재입장 -> 입장
//                        canEnterRoom(chatMessageDto, chatRoom)
//                        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
//                        messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
//                    }else{
//                        // 본인방 재입장 -> 활성화
//                        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
//                    }
//                }else{
//                    // 개인 -> receiver 퇴장 후 재입장
//                    if (isReceiverLeaveTheRoom(chatMessageDto.roomId)) {
//                        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
//                    }
//
//                    // 개인 -> receiver 강제 입장
//                    canEnterRoom(chatMessageDto, chatRoom)
//                    messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
//                }
//            }
//        }else{
//            if(validateEnterHistory(chatMessageDto)){
//                // 전체 -> 활성화
//                activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
//            }else{
//                // 전체 -> 입장
//                canEnterRoom(chatMessageDto, chatRoom)
//                activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
//                messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
//            }
//        }
//    }

    //    fun canEnterRoom(chatMessageDto: ChatMessageDto, chatRoom: ChatRoom) {
//        chatRoom.joinNumber += 1
//        chatRoomRepository.save(chatRoom)
//        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
//        chatMessageRepository.save(chatMessage)
////        log.info("${chatMessageDto.sender} joined the room (${chatMessageDto.roomId})")
//    }

    //    fun validateEnterHistory(chatMessageDto: ChatMessageDto): Boolean {
//        val lastChatMessage = getLastChatMessage(chatMessageDto.roomId, chatMessageDto.sender) ?: return false
//        return lastChatMessage.type !in listOf(MessageType.LEAVE)
////        return when (chatMessageDto.type) {
////            MessageType.JOIN, -> lastChatMessage.type != MessageType.LEAVE
////            MessageType.LEAVE -> lastChatMessage.type in listOf(MessageType.LEAVE, MessageType.CREATE)
////            MessageType.CHAT, MessageType.IMAGE -> lastChatMessage.type !in listOf(MessageType.LEAVE, MessageType.CREATE)
////            else -> false
////        }
//    }

//    fun validateEnterHistory(roomId: String, sender: String): Boolean {
//        val lastChatMessage = getLastChatMessage(roomId, sender) ?: return false
//        return lastChatMessage.type !in listOf(MessageType.LEAVE, MessageType.CREATE)
//    }


//    fun createRoom(chatMessageDto: ChatMessageDto) {
//        val chatRoom = ChatRoom(chatMessageDto.roomId, chatMessageDto.sender, 1, chatMessageDto.receiver!!)
//        chatRoomRepository.save(chatRoom)
//        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
//        chatMessageRepository.save(chatMessage)
//        messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
//        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
//        if (chatMessageDto.receiver != "ALL") {
//            // 개인 채팅 -> 강제 JOIN
//            enterRoom(ChatMessageDto(MessageType.JOIN, null, null, chatMessageDto.receiver, chatMessageDto.sender, chatMessageDto.roomId))
//        }
//    }
}