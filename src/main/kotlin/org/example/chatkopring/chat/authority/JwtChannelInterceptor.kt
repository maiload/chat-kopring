package org.example.chatkopring.chat.authority

import org.example.chatkopring.common.authority.JwtTokenProvider
import org.example.chatkopring.common.exception.UnAuthorizationException
import org.example.chatkopring.util.logger
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

const val AUTHORIZATION: String = "Authorization"
const val BEARER_: String = "Bearer "

@Component
class JwtChannelInterceptor(
    val jwtTokenProvider: JwtTokenProvider
): ChannelInterceptor{
    val log = logger()

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor!!.command != StompCommand.CONNECT) return message

        val accessToken = accessor.getFirstNativeHeader(AUTHORIZATION)
        requireNotNull(accessToken) {unAuthorized("Authorization 헤더가 없습니다.")}
        require(accessToken.startsWith(BEARER_)) {unAuthorized("토큰이 Bearer_ 로 시작하지 않습니다.")}
        val token = accessToken.substring(BEARER_.length)
        require(jwtTokenProvider.validateToken(token)) {unAuthorized("토큰이 유효하지 않습니다.")}
        val authentication = jwtTokenProvider.getAuthentication(token)
        accessor.user = authentication      // SecurityContextHolder 에 저장

        return message
    }

    private fun unAuthorized(message: String) {
        log.error(message)
        throw RuntimeException()
    }
}