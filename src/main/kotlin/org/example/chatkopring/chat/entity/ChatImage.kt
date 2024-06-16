package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*

@Entity
class ChatImage(
    @Column(nullable = false, length = 100)
    val originFileName: String,

    @Column(nullable = false, length = 120)
    val storageFileName: String,

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = ForeignKey(name = "fk_chat_image_chatMessage_id"))
    val chatMessage: ChatMessage,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}