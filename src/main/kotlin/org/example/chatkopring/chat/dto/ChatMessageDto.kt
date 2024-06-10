package org.example.chatkopring.chat.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.common.annotation.ValidEnum
import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.common.status.RoomType

data class ChatMessageDto(
    @field:NotBlank
    val roomId: String,

    @field:ValidEnum(enumClass = MessageType::class, message = "CHAT 과 IMAGE 중 하나를 선택해주세요.")
    val type: MessageType,

    @field:NotBlank
    val sender: String,

    @field:NotBlank
    val content: String,

    val image: String?,
){
    fun toErrorMessage(message: String) = ErrorMessage(message, sender, roomId)
    fun toChatRoomDto(roomType: RoomType) = ChatRoomDto(roomId, sender, roomType)
}


