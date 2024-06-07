package org.example.chatkopring.chat.dto

import jakarta.validation.constraints.NotBlank
import org.example.chatkopring.chat.entity.ChatRoom

data class ChatRoomDto(
    // TODO : roomId 설정
//    val roomId: String = UUID.randomUUID().toString(),
    val roomId: String = "1",

    @field:NotBlank
    val sender: String,

    val receiver: String = "ALL",
) {
    fun toErrorMessage(message: String) = ErrorMessage(message, sender, receiver, roomId)
}
