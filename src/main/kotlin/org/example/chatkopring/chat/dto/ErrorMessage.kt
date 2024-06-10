package org.example.chatkopring.chat.dto

import org.example.chatkopring.common.status.MessageType

data class ErrorMessage(
    val message: String?,
    val sender: String,
    val roomId: String,
) {
    val type: MessageType = MessageType.ERROR
}