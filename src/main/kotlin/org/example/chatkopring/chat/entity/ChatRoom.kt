package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import net.minidev.json.annotate.JsonIgnore
import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.dto.ChatRoomDto
import org.example.chatkopring.common.status.RoomType

@Entity
@Table
class ChatRoom(
    @Id
    @Column(nullable = false, length = 36)
    val id: String,

    @Column(nullable = false, length = 50)
    val title: String,

    @Column(nullable = false, length = 30)
    val creator: String,

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    val roomType: RoomType,

    @Column(nullable = false, length = 1)
    var valid: Boolean = true,
) {
    @JsonManagedReference
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "chatRoom", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chatMessages: List<ChatMessage>? = null

    @JsonManagedReference
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "chatRoom", cascade = [CascadeType.ALL], orphanRemoval = true)
    val participants: List<Participant>? = null

    fun toChatRoomDto(creator: String) = ChatRoomDto(id, creator, roomType, title)
}