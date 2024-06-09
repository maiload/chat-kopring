package org.example.chatkopring.chat.repository

import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.chat.entity.ChatImage
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.entity.ChatRoom
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRoomRepository: JpaRepository<ChatRoom, String> {
    fun existsByReceiverAndCreatorAndJoinNumberGreaterThanEqual(receiver: String, creator: String, joinNumber: Long): Boolean
}

interface ChatMessageRepository: JpaRepository<ChatMessage, Long> {
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId AND cm.type = :type And cm.sender = :sender ORDER BY cm.id DESC")
    fun findFirstByRoomIdAndTypeOrderByIdDesc(@Param("roomId") roomId: String, @Param("type") type: MessageType, @Param("sender") sender: String, pageable: Pageable): List<ChatMessage>?

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId And cm.sender = :sender ORDER BY cm.id DESC")
    fun findFirstByRoomIdOrderByIdDesc(@Param("roomId") roomId: String, @Param("sender") sender: String, pageable: Pageable): List<ChatMessage>?

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId AND cm.id >= :id ORDER BY cm.id DESC")
    fun findByRoomIdAndMessageIdGreaterThanEqual(@Param("roomId") roomId: String, @Param("id") id: Long, pageable: Pageable): List<ChatMessage>
}

interface ChatImageRepository: JpaRepository<ChatImage, Long> {
    fun findByChatMessage(chatMessage: ChatMessage): ChatImage
}

