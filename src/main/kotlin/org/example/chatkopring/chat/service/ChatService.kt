package org.example.chatkopring.chat.service

import jakarta.transaction.Transactional
import org.example.chatkopring.chat.dto.*
import org.example.chatkopring.chat.entity.ChatImage
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.chat.repository.ChatImageRepository
import org.example.chatkopring.chat.repository.ChatMessageRepository
import org.example.chatkopring.chat.repository.ChatRoomRepository
import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.util.logger
import org.springframework.data.domain.PageRequest
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.jvm.optionals.getOrNull

const val OUTPUT_PATH: String = "src/main/resources/images/"

@Transactional
@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatImageRepository: ChatImageRepository,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    val log = logger()

    fun createRoom(chatMessageDto: ChatMessageDto) {
        val chatRoom = ChatRoom(chatMessageDto.roomId, chatMessageDto.sender, 1, chatMessageDto.receiver!!)
        chatRoomRepository.save(chatRoom)
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        chatMessageRepository.save(chatMessage)
        messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
        if (chatMessageDto.receiver != "ALL") {
            // 개인 채팅 -> 강제 JOIN
            enterRoom(ChatMessageDto(MessageType.JOIN, null, null, chatMessageDto.receiver, chatMessageDto.sender, chatMessageDto.roomId))
        }
    }

    fun isPrivateRoomExist(receiver: String, creator: String): Boolean =
        chatRoomRepository.existsByReceiverAndCreatorAndJoinNumberGreaterThanEqual(receiver, creator, 1)
                || chatRoomRepository.existsByReceiverAndCreatorAndJoinNumberGreaterThanEqual(creator, receiver, 1)

    fun getLastChatMessage(roomId: String, sender: String) =
        chatMessageRepository.findFirstByRoomIdOrderByIdDesc(roomId, sender, PageRequest.of(0,1))?.firstOrNull()

    fun sendMessage(chatMessageDto: ChatMessageDto, chatRoom: ChatRoom) {
        if(!validateEnterHistory(chatMessageDto)){
            sendErrorMessage(
                ErrorMessage(
                    "입장중인 방이 아닙니다.",
                    chatMessageDto.sender,
                    chatRoom.receiver,
                    chatMessageDto.roomId
                )
            )
        }else{
            val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
            // 이미지 처리
            if (chatMessageDto.type == MessageType.IMAGE){
                val originFilename = chatMessageDto.content ?: "unnamed.jpg"
                val storageFilename = originFilename.generateStorageFileName()
                val chatImage = ChatImage(originFilename, storageFilename, chatMessage)
                saveBase64Image(chatMessageDto.image!!, OUTPUT_PATH + storageFilename)
                chatImageRepository.save(chatImage)
            }

            chatMessageRepository.save(chatMessage)
            messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)

            // 개인 채팅방 상대 나가면 public 알림 X
            if(!isReceiverLeaveTheRoom(chatMessageDto.roomId)){
                messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(chatMessageDto.type, chatMessageDto.sender, chatMessageDto.roomId))
            }
            log.info("${chatMessageDto.sender} sent message to room (${chatMessageDto.roomId})")
        }
    }

    fun isReceiverLeaveTheRoom(roomId: String): Boolean {
        // 오픈 채팅방은 public 알림 제외
        val chatRoom = getChatRoomById(roomId)
        return chatRoom?.receiver == "ALL" || chatRoom?.joinNumber == 1L
    }

    fun saveBase64Image(base64Image: String, outputPath: String) {
        val imageBytes = Base64.getDecoder().decode(base64Image)
        FileOutputStream(outputPath).use { outputStream ->
            outputStream.write(imageBytes)
            outputStream.flush()
        }
    }

    fun validateChatRoom(chatRoomDto: ChatRoomDto): Boolean {
        val chatRoom = chatRoomRepository.findById(chatRoomDto.roomId).getOrNull()
        if (chatRoom == null) {
            sendErrorMessage(ErrorMessage("존재하지 않는 방입니다.", chatRoomDto.sender, chatRoomDto.receiver, chatRoomDto.roomId))
            throw RuntimeException()
        }
        val receiver = chatRoom.receiver
        val creator = chatRoom.creator
//        log.info("validate : $receiver, $creator, ${chatRoomDto.sender}")
        return receiver == "ALL" || receiver == chatRoomDto.sender || creator == chatRoomDto.sender
    }

    fun enterRoom(chatMessageDto: ChatMessageDto) {
        val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
        if(chatMessageDto.sender in listOf(chatRoom.receiver, chatRoom.creator)){
            if(chatRoom.joinNumber == 2L){
                // 개인 -> 활성화
                activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
            }else{
                val lastChatMessage = getLastChatMessage(chatMessageDto.roomId, chatMessageDto.sender)
                if(chatRoom.joinNumber == 1L && chatMessageDto.sender == chatRoom.creator){
                    if(lastChatMessage?.type == MessageType.LEAVE) {
                        // 본인방 퇴장 후 재입장 -> 입장
                        canEnterRoom(chatMessageDto, chatRoom)
                        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
                        messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
                    }else{
                        // 본인방 재입장 -> 활성화
                        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
                    }
                }else{
                    // 개인 -> receiver 퇴장 후 재입장
                    if (isReceiverLeaveTheRoom(chatMessageDto.roomId)) {
                        activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
                    }

                    // 개인 -> receiver 강제 입장
                    canEnterRoom(chatMessageDto, chatRoom)
                    messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
                }
            }
        }else{
            if(validateEnterHistory(chatMessageDto)){
                // 전체 -> 활성화
                activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
            }else{
                // 전체 -> 입장
                canEnterRoom(chatMessageDto, chatRoom)
                activeRoom(chatMessageDto.sender, chatMessageDto.roomId)
                messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
            }
        }
    }

    fun activeRoom(sender: String, roomId: String) {
        messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.ACTIVE, sender, roomId))
//        chatMessageDto.type = MessageType.ACTIVE
//        messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
    }

    fun canEnterRoom(chatMessageDto: ChatMessageDto, chatRoom: ChatRoom) {
        chatRoom.joinNumber += 1
        chatRoomRepository.save(chatRoom)
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        chatMessageRepository.save(chatMessage)
        log.info("${chatMessageDto.sender} joined the room (${chatMessageDto.roomId})")
    }


    // 퇴장
    fun leaveRoom(chatMessageDto: ChatMessageDto) {
        if(!validateEnterHistory(chatMessageDto)){
            sendErrorMessage(
                ErrorMessage(
                    "입장중인 방이 아닙니다.",
                    chatMessageDto.sender,
                    chatMessageDto.receiver,
                    chatMessageDto.roomId
                )
            )
        }else{
            val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
            chatRoom.joinNumber -= 1
            chatRoomRepository.save(chatRoom)
            val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
            chatMessageRepository.save(chatMessage)
            log.info("${chatMessageDto.sender} leaved the room (${chatMessageDto.roomId})")
            messagingTemplate.convertAndSend("/sub/chat/${chatMessageDto.roomId}", chatMessageDto)
            messagingTemplate.convertAndSend("/sub/chat/public", PublicMessage(MessageType.INACTIVE, chatMessageDto.sender, chatMessageDto.roomId))
        }
    }

    // 입장 중인 방 다시 입장
    fun getRoomHistory(roomId: String, sender: String): List<ChatMessageResponse> {
        val lastJoinChatMessage = chatMessageRepository.findFirstByRoomIdAndTypeOrderByIdDesc(roomId, MessageType.JOIN, sender, PageRequest.of(0, 1))?.firstOrNull()
        // 마지막 입장 기록이 없다면 방 생성자
        val lastJoinHistoryId = lastJoinChatMessage?.id ?: 1
        // 최근 히스토리 기준으로 100개까지만 로드
        return chatMessageRepository.findByRoomIdAndMessageIdGreaterThanEqual(roomId, lastJoinHistoryId, PageRequest.of(0, 100))
            .map {
                var base64Image: String? = null
                if (it.type == MessageType.IMAGE) {
                    val storageFilename = chatImageRepository.findByChatMessage(it).storageFileName
                    base64Image = encodeImageToBase64(storageFilename)
                }
                it.toChatMessageResponse(base64Image)
            }
    }

    fun validateEnterHistory(chatMessageDto: ChatMessageDto): Boolean {
        val lastChatMessage = getLastChatMessage(chatMessageDto.roomId, chatMessageDto.sender) ?: return false
        return lastChatMessage.type !in listOf(MessageType.LEAVE)
//        return when (chatMessageDto.type) {
//            MessageType.JOIN, -> lastChatMessage.type != MessageType.LEAVE
//            MessageType.LEAVE -> lastChatMessage.type in listOf(MessageType.LEAVE, MessageType.CREATE)
//            MessageType.CHAT, MessageType.IMAGE -> lastChatMessage.type !in listOf(MessageType.LEAVE, MessageType.CREATE)
//            else -> false
//        }
    }

    fun validateEnterHistory(roomId: String, sender: String): Boolean {
        val lastChatMessage = getLastChatMessage(roomId, sender) ?: return false
        return lastChatMessage.type !in listOf(MessageType.LEAVE, MessageType.CREATE)
    }

    fun getChatRoomById(roomId: String) = chatRoomRepository.findById(roomId).getOrNull()

    fun String.generateStorageFileName(): String {
        val currentTimeMillisString = System.currentTimeMillis().toString()
        val parts = this.split('.')
        val fileName = parts.firstOrNull() ?: ""
        val extension = parts.lastOrNull() ?: ""
        return "$fileName-${currentTimeMillisString}.$extension"
    }

    fun encodeImageToBase64(storageFileName: String): String? {
        val file = File(OUTPUT_PATH + storageFileName)
        file.inputStream().use { inputStream ->
            val bytes = inputStream.readBytes()

            return Base64.getEncoder().encodeToString(bytes)
        }
    }

    fun sendErrorMessage(errorMessage: ErrorMessage?) {
        log.error("WebSocketError : $errorMessage")
        if (errorMessage != null) {
            messagingTemplate.convertAndSend("/sub/chat/${errorMessage.roomId}", errorMessage)
        }
    }

}