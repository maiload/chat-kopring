package org.example.chatkopring.common.authority

import com.nimbusds.jose.util.StandardCharset
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.chatkopring.common.dto.CustomUser
import org.example.chatkopring.member.repository.MemberRepository
import org.example.chatkopring.util.logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

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
        log.info("authentication : $authentication")
        val tokenInfo = jwtTokenProvider.createToken(authentication)
        log.info("tokenInfo : $tokenInfo")
        val type = if(memberRepository.findByLoginId(authentication.name)!!.birthDate.year == 1000) "NEW" else "MEMBER"

        response.apply {
            status = HttpStatus.OK.value()
            contentType = MediaType.APPLICATION_JSON_VALUE
            characterEncoding = StandardCharset.UTF_8.name()
            writer.write("{'tokenInfo': '$tokenInfo', 'type': $type}")
        }
    }
}