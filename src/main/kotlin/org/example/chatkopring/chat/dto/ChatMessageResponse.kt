package org.example.chatkopring.chat.dto

import java.time.LocalDateTime

class ChatMessageResponse(
    val id: Long,
    val type: String,
    val sender: String,
    val content: String?,
    val image: String?,
    val createdDate: LocalDateTime
) {
}