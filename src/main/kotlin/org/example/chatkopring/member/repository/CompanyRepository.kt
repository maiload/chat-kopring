package org.example.chatkopring.member.repository

import org.example.chatkopring.member.entity.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository: JpaRepository<Company, Long> {
    fun existsByCompanyCode(companyCode: String): Boolean
}