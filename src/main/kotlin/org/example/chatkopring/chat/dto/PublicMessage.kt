package org.example.chatkopring.chat.dto

import org.example.chatkopring.common.status.MessageType

data class PublicMessage(
    val messageType: MessageType,
    val username: String,
    val roomId: String? = null,
) {
}