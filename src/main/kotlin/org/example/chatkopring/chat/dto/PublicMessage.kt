package org.example.chatkopring.chat.dto

data class PublicMessage(
    val messageType: MessageType,
    val username: String,
    val roomId: String?,
) {
}