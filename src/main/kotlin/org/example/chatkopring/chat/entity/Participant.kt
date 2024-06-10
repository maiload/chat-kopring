package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*
import org.example.chatkopring.chat.dto.ChatRoomDto
import org.example.chatkopring.chat.dto.ParticipantResponse

@Entity
@Table
data class Participant(
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = ForeignKey(name = "fk_participant_chatRoom_id"))
    val chatRoom: ChatRoom,

    @Column(nullable = false, length = 30, updatable = false)
    val loginId: String,

    @Column(nullable = false, length = 3)
    var unreadMsgNumber: Int = 0,

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    fun toChatRoomDto() = ChatRoomDto(chatRoom.id, loginId, chatRoom.roomType)
    fun toResponse() = ParticipantResponse(chatRoom.id, chatRoom.title, chatRoom.roomType, loginId, unreadMsgNumber)
    fun resetUnreadNumber() = apply { unreadMsgNumber = 0 }
}