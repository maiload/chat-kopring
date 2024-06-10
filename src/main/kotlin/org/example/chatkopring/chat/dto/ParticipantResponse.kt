package org.example.chatkopring.chat.dto

import org.example.chatkopring.common.status.RoomType

data class ParticipantResponse(
    val roomId: String,
    val title: String,
    val roomType: RoomType,
    val loginId: String,
    val unreadMsgNumber: Int,
)
