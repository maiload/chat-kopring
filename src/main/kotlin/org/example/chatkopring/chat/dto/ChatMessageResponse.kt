package org.example.chatkopring.chat.dto

class ChatMessageResponse(
    val id: Long,
    val type: String,
    val sender: String,
    val content: String?,
    val image: String?,
) {
}