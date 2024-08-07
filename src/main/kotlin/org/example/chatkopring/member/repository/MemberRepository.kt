package org.example.chatkopring.member.repository

import ch.qos.logback.core.util.FileSize
import org.example.chatkopring.common.status.State
import org.example.chatkopring.member.entity.BlackList
import org.example.chatkopring.member.entity.Member
import org.example.chatkopring.member.entity.MemberImage
import org.example.chatkopring.member.entity.MemberRole
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository: JpaRepository<Member, Long> {
    fun findByLoginId(loginId: String): Member?
    fun findByEmail(email: String): Member?
    fun existsByBusinessId(businessId: String): Boolean
    fun findByCompanyCodeAndStateNot(companyCode: String, state: State): List<Member>?
    @EntityGraph(attributePaths = ["memberImage"])
    fun findByCompanyCodeAndState(companyCode: String, state: State): List<Member>
}

interface MemberRoleRepository: JpaRepository<MemberRole, Long>

interface BlackListRepository: JpaRepository<BlackList, Long> {
    fun existsByInvalidRefreshToken(refreshToken: String): Boolean
}

interface MemberImageRepository: JpaRepository<MemberImage, Long> {
    fun findByMember(member: Member): MemberImage?
}