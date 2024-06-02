package org.example.chatkopring.member.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.service.MemberService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var memberService: MemberService

    @Test
    fun signUp() {
        val memberDto = MemberDto(
            id = null,
            _loginId = "john_doe",
            _password = "Password123!",
            _name = "John Doe",
            _birthDate = "1990-01-01",
            _gender = "MAN",
            _email = "john.doe@example.com"
        )

        val msg: String = "회원가입이 완료되었습니다."
        Mockito.`when`(memberService.signUp(memberDto)).thenReturn(msg)

        mockMvc.perform(MockMvcRequestBuilders.post("/api/member/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(memberDto)))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(msg))
    }
}