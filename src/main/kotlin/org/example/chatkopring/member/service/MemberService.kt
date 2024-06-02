package org.example.chatkopring.member.service

import jakarta.transaction.Transactional
import org.example.chatkopring.common.authority.JwtTokenProvider
import org.example.chatkopring.common.authority.TokenInfo
import org.example.chatkopring.common.exception.InvalidInputException
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.member.dto.LoginDto
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.dto.MemberResponse
import org.example.chatkopring.member.entity.Member
import org.example.chatkopring.member.entity.MemberRole
import org.example.chatkopring.member.repository.MemberRepository
import org.example.chatkopring.member.repository.MemberRoleRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Transactional
@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberRoleRepository: MemberRoleRepository,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    val passwordEncoder: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
    /**
     * 회원가입
     */
    fun signUp(memberDto: MemberDto, role: Role = Role.MEMBER): String {
        // ID 중복 검사
        var member: Member? = memberRepository.findByLoginId(memberDto.loginId)
        if (member != null) {
            throw InvalidInputException("loginId", "이미 등록된 ID 입니다.")
        }

        val originPw = memberDto.password
        val encodedPw = passwordEncoder.encode(originPw)
        member = memberDto.toEntity(encodedPw)
        memberRepository.save(member)

        val memberRole: MemberRole = MemberRole(null, role, member)
        memberRoleRepository.save(memberRole)

        return "회원가입이 완료되었습니다."
    }

    /**
     * 로그인 -> 토큰 발행
     */
    fun login(loginDto: LoginDto): TokenInfo {
        val savedPw = memberRepository.findByLoginId(loginDto.loginId)?.password
            ?: throw InvalidInputException(message = "아이디 혹은 비밀번호를 다시 확인하세요.")
        require(passwordEncoder.matches(loginDto.password, savedPw)) { "아이디 혹은 비밀번호를 다시 확인하세요." }
        val authenticationToken = UsernamePasswordAuthenticationToken(loginDto.loginId, savedPw)
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)

        return jwtTokenProvider.createToken(authentication)
    }

    /**
     * 내 정보 조회
     */
    fun searchMyInfo(id: Long): MemberResponse {
        val member: Member = memberRepository.findByIdOrNull(id)
            ?: throw InvalidInputException("id", "회원번호(${id})가 존재하지 않는 유저입니다.")
        return member.toResponseDto()
    }

    /**
     * 내 정보 수정
     */
    fun saveMyInfo(memberDto: MemberDto): String {
        val savedPw = memberRepository.findByLoginId(memberDto.loginId)?.password
            ?: throw InvalidInputException("loginId", "존재하지 않는 ID(${memberDto.loginId}) 입니다")
        require(passwordEncoder.matches(memberDto.password, savedPw)) { "비밀번호를 다시 확인하세요." }
        val member: Member = memberDto.toEntity(savedPw)
        memberRepository.save(member)
        return "수정 완료되었습니다."
    }
}