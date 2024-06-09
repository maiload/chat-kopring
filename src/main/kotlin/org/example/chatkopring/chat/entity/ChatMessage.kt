package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import org.example.chatkopring.chat.dto.ChatMessageResponse
import org.example.chatkopring.common.status.MessageType

@Entity
@Table
class ChatMessage(
    @Column(nullable = false, length = 30)
    val sender: String,

    @Column(nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    val type: MessageType,

    @Column(nullable = true, length = 300)
    val content: String?,       // 이미지 -> 파일명

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = ForeignKey(name = "fk_chatRoom_role_chatRoom_id"))
    val chatRoom: ChatRoom,

    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @JsonManagedReference
    @Column(nullable = true)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "chatMessage", cascade = [CascadeType.ALL])
    val images: List<ChatImage>? = null

    fun toChatMessageResponse(base64Image: String?): ChatMessageResponse =
        ChatMessageResponse(id!!, type.name, sender, content, base64Image)
}