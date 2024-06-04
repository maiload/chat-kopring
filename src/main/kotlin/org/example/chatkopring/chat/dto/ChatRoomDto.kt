package org.example.chatkopring.chat.dto

import org.example.chatkopring.chat.entity.ChatRoom

data class ChatRoomDto(
//    val roomId: String = UUID.randomUUID().toString(),
    val roomId: String = "1",
    val sender: String,
    val receiver: String = "ALL",
) {

}
