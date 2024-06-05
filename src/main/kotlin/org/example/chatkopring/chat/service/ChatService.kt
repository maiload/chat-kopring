package org.example.chatkopring.chat.service

import jakarta.transaction.Transactional
import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.dto.ChatMessageResponse
import org.example.chatkopring.chat.dto.ChatRoomDto
import org.example.chatkopring.chat.dto.MessageType
import org.example.chatkopring.chat.entity.ChatImage
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.chat.repository.ChatImageRepository
import org.example.chatkopring.chat.repository.ChatMessageRepository
import org.example.chatkopring.chat.repository.ChatRoomRepository
import org.example.chatkopring.common.exception.UnAuthorizationException
import org.example.chatkopring.util.logger
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.imageio.ImageIO

const val OUTPUT_PATH: String = "src/main/resources/images/"

@Transactional
@Service
class ChatService(
    val chatRoomRepository: ChatRoomRepository,
    val chatMessageRepository: ChatMessageRepository,
    val chatImageRepository: ChatImageRepository,
) {
    val log = logger()

    fun createRoom(chatMessageDto: ChatMessageDto) {
        val chatRoom = ChatRoom(chatMessageDto.roomId, chatMessageDto.sender, 1, chatMessageDto.receiver!!)
        chatRoomRepository.save(chatRoom)
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        chatMessageRepository.save(chatMessage)
    }

    fun isPrivateRoomExist(receiver: String, sender: String): Boolean =
        chatRoomRepository.existsByReceiverAndCreatorAndJoinNumberEquals(receiver, sender, 2)
                || chatRoomRepository.existsByReceiverAndCreatorAndJoinNumberEquals(sender, receiver, 2)

    fun sendMessage(chatMessageDto: ChatMessageDto) {
        val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
        val chatRoomDto = ChatRoomDto(chatRoom.id, chatMessageDto.sender, chatRoom.receiver)
        require(validateChatRoom(chatRoomDto)) { throw UnAuthorizationException(message = "개인 채팅방에 메세지를 전송할 수 없습니다.") }
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        if (chatMessageDto.type == MessageType.IMAGE){
            val originFilename = chatMessageDto.content ?: "unnamed.jpg"
            val storageFilename = originFilename.generateStorageFileName()
            val chatImage = ChatImage(originFilename, storageFilename, chatMessage)
            saveBase64Image(chatMessageDto.image!!, OUTPUT_PATH + storageFilename)
            chatImageRepository.save(chatImage)
        }
        chatMessageRepository.save(chatMessage)
    }

    fun saveBase64Image(base64Image: String, outputPath: String) {
        val imageBytes = Base64.getDecoder().decode(base64Image)
        FileOutputStream(outputPath).use { outputStream ->
            outputStream.write(imageBytes)
            outputStream.flush()
        }
    }

    fun validateChatRoom(chatRoomDto: ChatRoomDto): Boolean {
        val chatRoom = chatRoomRepository.findById(chatRoomDto.roomId).get()
        val receiver = chatRoom.receiver
        val creator = chatRoom.creator
//        log.info("validate : $receiver, $creator, ${chatRoomDto.sender}")
        return receiver == "ALL" || receiver == chatRoomDto.sender || creator == chatRoomDto.sender
    }

    // 처음 입장 or 퇴장했던 방 입장
    fun enterRoom(chatMessageDto: ChatMessageDto) {
        val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
        if (chatRoom.receiver != "ALL" && chatRoom.joinNumber == 2L) throw UnAuthorizationException(message = "private room 의 정원이 가득찼습니다.")
        chatRoom.joinNumber += 1
        chatRoomRepository.save(chatRoom)
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        chatMessageRepository.save(chatMessage)
    }

    // 처음 입장 or 퇴장했던 방 입장
    fun leaveRoom(chatMessageDto: ChatMessageDto) {
        val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
        chatRoom.joinNumber -= 1
        chatRoomRepository.save(chatRoom)
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        chatMessageRepository.save(chatMessage)
    }

    // 입장 중인 방 다시 입장
    fun activeRoom(roomId: String, sender: String): List<ChatMessageResponse> {
        // 마지막 입장 기록이 없다면 방 생성자
        val lastJoinHistoryId = chatMessageRepository.findFirstByRoomIdAndTypeOrderByIdDesc(roomId, MessageType.JOIN, sender)?.id ?: 1
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

}