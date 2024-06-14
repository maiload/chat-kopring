package org.example.chatkopring.chat.dto

import org.example.chatkopring.common.status.MessageType

data class MessageDto(
    val loginId: String,
    val type: String,
    val payload: Any,
)
