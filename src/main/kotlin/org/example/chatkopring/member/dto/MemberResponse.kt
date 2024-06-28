package org.example.chatkopring.member.dto

import org.springframework.core.io.Resource

data class MemberResponse(
    val id: Long,
    val loginId: String,
    val name: String,
    val birthDate: String,
    val gender: String,
    val email: String,
    val role: String,
    val companyCode: String?,
    val state: String,
    val profile: String,
)
