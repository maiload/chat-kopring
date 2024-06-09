package org.example.chatkopring.chat.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.example.chatkopring.common.status.MessageType

data class ChatMessageDto(
    @field:NotNull
    var type: MessageType,
    val content: String?,
    val image: String?,

    @field:NotBlank
    val sender: String,
    val receiver: String?,

    @field:NotBlank
    val roomId: String,
){
    fun toErrorMessage(message: String) = ErrorMessage(message, sender, receiver, roomId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatMessageDto

        if (type != other.type) return false
        if (content != other.content) return false
        if (sender != other.sender) return false
        if (receiver != other.receiver) return false
        if (roomId != other.roomId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + sender.hashCode()
        result = 31 * result + (receiver?.hashCode() ?: 0)
        result = 31 * result + roomId.hashCode()
        return result
    }
}


