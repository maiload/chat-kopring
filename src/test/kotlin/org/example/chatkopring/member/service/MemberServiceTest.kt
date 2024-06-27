package org.example.chatkopring.member.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.example.chatkopring.common.exception.InvalidInputException
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.entity.Member
import org.example.chatkopring.member.repository.MemberRepository
import org.example.chatkopring.member.repository.MemberRoleRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class MemberServiceTest{
    @Autowired private lateinit var memberService: MemberService
    @MockBean private lateinit var memberRepository: MemberRepository
    @MockBean private lateinit var memberRoleRepository: MemberRoleRepository

    @Test
    fun signUpWithDuplicateId() {
        val memberDto = MemberDto(
            id = null,
            _loginId = "john_doe",
            _password = "Password123!",
            _name = "John Doe",
            _birthDate = "1990-01-01",
            _gender = "MAN",
            _email = "john.doe@example.com"
        )

        assertThat(memberService.signUp(memberDto))
            .isEqualTo("회원가입이 완료되었습니다.")

        val existingMember = Member(
            id = 1L,
            loginId = memberDto.loginId,
            password = memberDto.password,
            name = memberDto.name,
            birthDate = memberDto.birthDate,
            gender = memberDto.gender,
            email = memberDto.email,
            profile = "안녕하세요"
        )
        Mockito.`when`(memberRepository.findByLoginId("john_doe")).thenReturn(existingMember)

        assertThatCode { memberService.signUp(memberDto) }
            .isInstanceOf(InvalidInputException::class.java)
            .hasMessage("이미 등록된 ID 입니다.")
    }
}
