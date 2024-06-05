package org.example.chatkopring.chat.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import net.minidev.json.annotate.JsonIgnore
import org.example.chatkopring.chat.dto.MessageType

@Entity
@Table
class ChatMessage(
    @Column(nullable = false, length = 30)
    val sender: String,

    @Column(nullable = false, length = 5)
    val type: MessageType,

    @Column(nullable = true, length = 300)
    val content: String?,

    @JsonBackReference @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(foreignKey = ForeignKey(name = "fk_chatRoom_role_chatRoom_id"))
    val chatRoom: ChatRoom,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}