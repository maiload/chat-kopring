package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import org.example.chatkopring.chat.dto.ChatMessageResponse
import org.example.chatkopring.common.status.MessageType

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

    @JsonManagedReference
    @Column(nullable = true)
    @OneToMany(mappedBy = "chatMessage")
    val images: List<ChatImage>? = null

    fun toChatMessageResponse(base64Image: String?): ChatMessageResponse =
        ChatMessageResponse(id!!, type.name, sender, content, base64Image)
}