package org.example.chatkopring.member.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.example.chatkopring.common.annotation.ValidEnum
import org.example.chatkopring.common.status.Gender
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.common.status.State
import org.example.chatkopring.member.entity.Member
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MemberDto(
    val id: Long?,

    @field:NotBlank
    @JsonProperty("loginId")
    private val _loginId: String?,

    @field:NotBlank
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#\$%^&*])[a-zA-Z0-9!@#\$%^&*]{8,20}\$",
        message = "영문, 숫자, 특수문자를 포함한 8~20자리로 입력해주세요"
    )
    @JsonProperty("password")
    private val _password: String?,

    @field:NotBlank
    @JsonProperty("name")
    private val _name: String?,

    @field:NotBlank
    @JsonProperty("birthDate")
    @field:Pattern(
        regexp = "^([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))$",
        message = "날짜형식(YYYY-MM-DD)을 확인해주세요"
    )
    private val _birthDate: String?,

//    @field:NotNull
//    @field:Past(message = "생일은 과거의 날짜만 가능합니다.")
//    @JsonProperty("birthDate")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
//    private val _birthDate: LocalDate?,

    @field:NotBlank
    @field:ValidEnum(enumClass = Gender::class, message = "MAN 이나 WOMAN 중 하나를 선택해주세요.")
    @JsonProperty("gender")
    private val _gender: String?,

    @field:NotBlank
    @field:Email
    @JsonProperty("email")
    private val _email: String?,

    @JsonProperty("profile")
    private val _profile: String? = null,

    @JsonProperty("companyCode")
    private val _companyCode: String? = null,

    @JsonProperty("ceoName")
    private val _ceoName: String? = null,

    @JsonProperty("companyName")
    private val _companyName: String? = null,

    @field:Pattern(regexp = "^\\d{10}$", message = "사업자등록번호(10자리)를 확인해주세요")
    @JsonProperty("businessId")
    private val _businessId: String? = null,

    @field:Pattern(regexp = "^\\d{14}$", message = "기업인증 번호(14자리)를 확인해주세요")
    @JsonProperty("companyCertificateNumber")
    private val _companyCertificateNumber: String? = null,

    @field:ValidEnum(enumClass = State::class, message = "APPROVED 나 DENIED 중 하나를 선택해주세요.")
    @JsonProperty("state")
    private val _state: String? = State.PENDING.name,
) {

    val loginId: String
        get() = _loginId!!
    val password: String
        get() = _password!!
    val name: String
        get() = _name!!
    val birthDate: LocalDate
        get() = _birthDate!!.toLocalDate()
    val gender: Gender
        get() = Gender.valueOf(_gender!!)
    val email: String
        get() = _email!!
    val companyCode: String?
        get() = _companyCode
    val ceoName: String?
        get() = _ceoName
    val companyName: String?
        get() = _companyName
    val businessId: String?
        get() = _businessId
    val companyCertificateNumber: String?
        get() = _companyCertificateNumber
    val state: State
        get() = State.valueOf(_state!!)
    val profile: String
        get() = _profile ?: "안녕하세요!"


    fun toEntity(password: String, role: String): Member {
        return if (role == Role.ADMIN.name) Member(id, loginId, password, name, birthDate, gender, email, profile,
            companyCode, ceoName, companyName, businessId, companyCertificateNumber)
        else Member(id, loginId, password, name, birthDate, gender, email, profile, companyCode)
    }

    private fun String.toLocalDate(): LocalDate =
        LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}

