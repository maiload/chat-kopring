package org.example.chatkopring.chat.repository

import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.chat.entity.ChatImage
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.chat.entity.Participant
import org.example.chatkopring.common.status.RoomType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRoomRepository: JpaRepository<ChatRoom, String> {
    fun findByRoomTypeAndCreatorAndValid(roomType: RoomType, creator: String, valid: Boolean): List<ChatRoom>?
}

interface ChatMessageRepository: JpaRepository<ChatMessage, Long> {

    fun deleteByChatRoomAndTypeAndSender(chatRoom: ChatRoom, type: MessageType, sender: String): Unit

    fun existsByChatRoomAndSenderAndType(chatRoom: ChatRoom, sender: String, type: MessageType): Boolean

    fun findFirstByChatRoomAndTypeAndSenderOrderByIdDesc(chatRoom: ChatRoom, type: MessageType, sender: String): ChatMessage

    fun findByChatRoomAndIdGreaterThanEqual(chatRoom: ChatRoom, chatMessageId: Long, pageable: Pageable): List<ChatMessage>

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId And cm.sender = :sender ORDER BY cm.id DESC")
    fun findFirstByRoomIdOrderByIdDesc(@Param("roomId") roomId: String, @Param("sender") sender: String, pageable: Pageable): List<ChatMessage>

//    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId AND cm.id >= :id ORDER BY cm.id DESC")
//    fun findByRoomIdAndMessageIdGreaterThanEqual(@Param("roomId") roomId: String, @Param("id") id: Long, pageable: Pageable): List<ChatMessage>
//
//    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId AND cm.type = :type And cm.sender = :sender ORDER BY cm.id DESC")
//    fun findFirstByRoomIdAndTypeOrderByIdDesc(@Param("roomId") roomId: String, @Param("type") type: MessageType, @Param("sender") sender: String, pageable: Pageable): List<ChatMessage>?


}

interface ChatImageRepository: JpaRepository<ChatImage, Long> {
    fun findByChatMessage(chatMessage: ChatMessage): ChatImage
}

interface ParticipantRepository: JpaRepository<Participant, Long> {
    @EntityGraph(attributePaths = ["chatRoom"])
    fun findByLoginIdOrderByIdDesc(loginId: String): List<Participant>?
    fun findByLoginIdAndChatRoom(loginId: String, chatRoom: ChatRoom): Participant
    fun existsByChatRoomAndLoginId(chatRoom: ChatRoom, loginId: String): Boolean
    fun deleteByChatRoomAndLoginId(chatRoom: ChatRoom, loginId: String): Unit
    fun existsByChatRoom(chatRoom: ChatRoom): Boolean
}

