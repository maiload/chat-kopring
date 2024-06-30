package org.example.chatkopring.common.authority

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.chatkopring.member.repository.MemberRepository
import org.example.chatkopring.util.logger
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

// TODO : 소셜 로그인 성공 처리
const val URI_STRING: String = "http://localhost:3000/loading"

@Component
class OAuth2SuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val memberRepository: MemberRepository,
): AuthenticationSuccessHandler {
    val log = logger()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        log.info("OAuth2SuccessHandler.authentication : $authentication")
        val tokenInfo = jwtTokenProvider.createToken(authentication)
        val type = if(memberRepository.findByLoginId(authentication.name)!!.birthDate.year == 1000) "SIGNUP" else "LOGIN"

//        response.apply {
//            status = HttpStatus.OK.value()
//            contentType = MediaType.APPLICATION_JSON_VALUE
//            characterEncoding = StandardCharset.UTF_8.name()
//            val responseDto = BaseResponse(data = tokenInfo, message = type)
//            writer.write("{'response': '$responseDto'")
//        }

        val redirectUri = UriComponentsBuilder.fromUriString(URI_STRING)
            .queryParam("accessToken", tokenInfo.accessToken)
            .queryParam("refresh", tokenInfo.refreshToken)
            .queryParam("type", type)
            .build().toUriString()

        response.sendRedirect(redirectUri)
    }
}