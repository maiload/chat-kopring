package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import net.minidev.json.annotate.JsonIgnore
import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.dto.ChatRoomDto

@Entity
@Table
class ChatRoom(
    @Id
    @Column(nullable = false, length = 36)
    val id: String,

    @Column(nullable = false, length = 30)
    val creator: String,

    @Column(nullable = false)
    var joinNumber: Long,

    @Column(nullable = false, length = 30)
    val receiver: String,
) {
    @JsonManagedReference
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "chatRoom")
    val chatMessages: List<ChatMessage>? = null
}