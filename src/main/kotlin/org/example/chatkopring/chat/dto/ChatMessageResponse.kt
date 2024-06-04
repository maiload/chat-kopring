package org.example.chatkopring.chat.dto

import org.example.chatkopring.chat.entity.ChatRoom

class ChatMessageResponse(
    val id: Long,
    val sender: String,
    val type: MessageType,
    val content: String?,
    val chatRoom: ChatRoom,
){
}
