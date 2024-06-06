package org.example.chatkopring.member.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

class MemberDtoTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @DisplayName("입력 값이 하나라도 비어 있으면 에러 발생한다")
    @ParameterizedTest
    @MethodSource("provideInvalidMembers")
    fun notBlankTest(
        loginId: String?,
        password: String?,
        name: String?,
        birthDate: String?,
        gender: String?,
        email: String?
    ) {
        val memberDto = MemberDto(null, loginId, password, name, birthDate, gender, email)
        val violations = validator.validate(memberDto)

        Assertions.assertThat(violations).isNotEmpty()
    }

    companion object {
        @JvmStatic
        fun provideInvalidMembers(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, "Password123!", "John Doe", "1990-01-01", "MAN", "john@example.com"),
                Arguments.of("loginId", null, "John Doe", "1990-01-01", "MAN", "john@example.com"),
                Arguments.of("loginId", "Password123!", null, "1990-01-01", "MAN", "john@example.com"),
                Arguments.of("loginId", "Password123!", "John Doe", null, "MAN", "john@example.com"),
                Arguments.of("loginId", "Password123!", "John Doe", "1990-01-01", null, "john@example.com"),
                Arguments.of("loginId", "Password123!", "John Doe", "1990-01-01", "MAN", null)
            )
        }
    }

    @DisplayName("영문, 숫자, 특수문자를 포함한 8~20자리가 아니면 에러가 발생한다")
    @ParameterizedTest
    @ValueSource(strings = ["12345678", "abcdefgh", "123456789asd!345678901", "qwer1234"])
    fun passwordTest(password: String) {
        val memberDto = MemberDto(
            id = null,
            _loginId = "john_doe",
            _password = password,
            _name = "John Doe",
            _birthDate = "1990-01-01",
            _gender = "MAN",
            _email = "john.doe@example.com")

        val violations = validator.validate(memberDto)

        Assertions.assertThat(violations).isNotEmpty()
    }

    @DisplayName("날짜 잘못입력되면 에러가 발생한다")
    @ParameterizedTest
    @ValueSource(strings = ["19990101", "1999-00-01", "1999-01-00"])
    fun birthDateTest(birthDate: String) {
        val memberDto = MemberDto(
            id = null,
            _loginId = "john_doe",
            _password = "qwer1234!",
            _name = "John Doe",
            _birthDate = birthDate,
            _gender = "MAN",
            _email = "john.doe@example.com")

        val violations = validator.validate(memberDto)

        Assertions.assertThat(violations).isNotEmpty()
    }

    @DisplayName("모든 값이 올바르게 입력되면 멤버로 등록된다")
    @Test
    fun correctMemberTest() {
        val memberDto = MemberDto(
            id = null,
            _loginId = "john_doe",
            _password = "qwer1234!",
            _name = "John Doe",
            _birthDate = "1999-01-01",
            _gender = "MAN",
            _email = "john.doe@example.com")

        val violations = validator.validate(memberDto)

        Assertions.assertThat(violations).isEmpty()
    }
}