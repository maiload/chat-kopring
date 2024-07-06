package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import org.example.chatkopring.chat.dto.ChatMessageResponse
import org.example.chatkopring.common.status.MessageType
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
class ChatMessage(
    @Column(nullable = false, length = 30)
    val sender: String,

    @Column(nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    val type: MessageType,

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = ForeignKey(name = "fk_chat_message_chatRoom_id"))
    val chatRoom: ChatRoom,

    @Column(nullable = true, length = 300)
    val content: String? = null,       // 이미지 -> 파일명

    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(updatable = false)
    val createdDate: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

    @JsonManagedReference
    @Column(nullable = true)
    @OneToMany(mappedBy = "chatMessage", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: List<ChatImage>? = null

    fun toChatMessageResponse(base64Image: String?): ChatMessageResponse =
        ChatMessageResponse(id!!, type.name, sender, content, base64Image, createdDate)
}