package org.example.chatkopring.common.authority

data class TokenInfo(
    val grantType: String,
    val accessToken: String,
    val refreshToken: String,
)
