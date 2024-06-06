package org.example.chatkopring.chat.dto

data class ErrorMessage(
    val message: String?,
    val sender: String,
    val receiver: String?,
    val roomId: String,
) {
    val type: MessageType = MessageType.ERROR
}