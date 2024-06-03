package org.example.chatkopring.common.service

import org.example.chatkopring.common.dto.CustomUser
import org.example.chatkopring.common.status.Gender
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.repository.MemberRepository
import org.example.chatkopring.member.service.MemberService
import org.example.chatkopring.util.logger
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOath2UserService(
    private val memberRepository: MemberRepository,
    private val memberService: MemberService,
): DefaultOAuth2UserService() {
    val log = logger()
    val passwordEncoder: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {

        val attributes = super.loadUser(userRequest).attributes
        val customUser = memberRepository.findByEmail(attributes["email"] as String)?.let { createOAuth2User(it.toDto(), attributes) }
            ?: createMember(attributes)

        return customUser
    }

    private fun createMember(attributes: MutableMap<String, Any>): OAuth2User {
        val memberDto = MemberDto(
            null,
            attributes["email"] as String,
            attributes["sub"] as String,
            attributes["name"] as String,
            "1000-01-01",           // birthDate 와 성별은 google 데이터에 없어 추가 입력을 받아야 한다
            Gender.MAN.name,
            attributes["email"] as String
        )
        memberService.signUp(memberDto, Role.OAUTH_MEMBER)

        return createOAuth2User(memberDto, attributes)
    }

    private fun createOAuth2User(memberDto: MemberDto, attributes: MutableMap<String, Any>): OAuth2User {
        val id = memberRepository.findByEmail(memberDto.email)?.id
        return CustomUser(
            id!!,
            memberDto.loginId,
            passwordEncoder.encode(memberDto.password),
            mutableListOf(SimpleGrantedAuthority("ROLE_${Role.OAUTH_MEMBER.name}")),
            attributes
        )
    }
}