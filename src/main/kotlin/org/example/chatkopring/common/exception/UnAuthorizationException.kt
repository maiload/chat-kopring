package org.example.chatkopring.common.exception

class UnAuthorizationException(
    val loginId: String = "",
    override val message: String = "UnAuthorization",
): RuntimeException(message) {
}