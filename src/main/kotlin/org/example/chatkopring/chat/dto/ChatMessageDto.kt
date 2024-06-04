package org.example.chatkopring.chat.dto

import org.example.chatkopring.chat.entity.ChatRoom

data class ChatMessageDto(
    val type: MessageType,
    val content: String?,
    val sender: String,
    val receiver: String?,
    val roomId: String,
){

}


