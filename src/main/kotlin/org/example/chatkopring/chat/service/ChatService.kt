package org.example.chatkopring.chat.service

import jakarta.transaction.Transactional
import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.dto.ChatMessageResponse
import org.example.chatkopring.chat.dto.ChatRoomDto
import org.example.chatkopring.chat.dto.MessageType
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.chat.repository.ChatMessageRepository
import org.example.chatkopring.chat.repository.ChatRoomRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Transactional
@Service
class ChatService(
    val chatRoomRepository: ChatRoomRepository,
    val chatMessageRepository: ChatMessageRepository,
) {
    fun createRoom(chatMessageDto: ChatMessageDto) {
        val chatRoom = ChatRoom(chatMessageDto.roomId, chatMessageDto.sender, 1, chatMessageDto.receiver!!)
        chatRoomRepository.save(chatRoom)
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        chatMessageRepository.save(chatMessage)
    }

    fun sendMessage(chatMessageDto: ChatMessageDto) {
        val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
        val chatMessage = ChatMessage(chatMessageDto.sender, chatMessageDto.type, chatMessageDto.content, chatRoom)
        chatMessageRepository.save(chatMessage)
    }

    // 처음 입장 or 퇴장했던 방 입장
    fun enterRoom(chatMessageDto: ChatMessageDto) {
        val chatRoom = chatRoomRepository.findById(chatMessageDto.roomId).get()
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
    fun activeRoom(roomId: String, sender: String): List<ChatMessage> {
        // 마지막 입장 기록이 없다면 방 생성자
        val lastJoinHistoryId = chatMessageRepository.findFirstByRoomIdAndTypeOrderByIdDesc(roomId, MessageType.JOIN, sender)?.id ?: 1
        // 최근 히스토리 기준으로 100개까지만 로드
        return chatMessageRepository.findByRoomIdAndMessageIdGreaterThanEqual(roomId, lastJoinHistoryId, PageRequest.of(0, 100))
    }

}