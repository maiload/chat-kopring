package org.example.chatkopring.chat.dto

import jakarta.validation.constraints.NotBlank
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.entity.ChatRoom
import org.example.chatkopring.chat.entity.Participant
import org.example.chatkopring.common.annotation.ValidEnum
import org.example.chatkopring.common.status.MessageType
import org.example.chatkopring.common.status.RoomType
import java.util.UUID

data class ChatRoomDto(
//    val roomId: String = UUID.randomUUID().toString(),
    @field:NotBlank
    val roomId: String,

    @field:NotBlank
    val creator: String,

    @field:ValidEnum(enumClass = RoomType::class, message = "PRIVATE, ALL, GROUP 중 하나를 선택해주세요.")
    val roomType: RoomType,

    val title: String = "UnTitled Room",
    val participant: List<String>? = null,
) {
    fun toErrorMessage(message: String) = ErrorMessage(message, creator, roomId)
    fun toEntity() = ChatRoom(roomId, title, creator, roomType)
    fun makeChatMessage(type: MessageType) = ChatMessage(creator, type, this.toEntity())
}
