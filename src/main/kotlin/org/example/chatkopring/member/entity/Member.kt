package org.example.chatkopring.member.entity

import jakarta.persistence.*
import org.example.chatkopring.common.status.Gender
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.common.status.State
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.dto.MemberResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(name = "uk_member_login_id", columnNames = ["loginId"])]
)
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 30, updatable = false)
    val loginId: String,

    @Column(nullable = false, length = 100)
    val password: String,

    @Column(nullable = false, length = 10)
    val name: String,

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    val birthDate: LocalDate,

    @Column(nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    val gender: Gender,

    @Column(nullable = false, length = 30)
    val email: String,

    @Column(nullable = true, length = 20)
    val companyCode: String? = null,

    @Column(nullable = true, length = 10)
    val ceoName: String? = null,
    @Column(nullable = true, length = 30)
    val companyName: String? = null,
    @Column(nullable = true, length = 10)
    val businessId: String? = null,
    @Column(nullable = true, length = 14)
    val companyCertificateNumber: String? = null,

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var state: State = State.PENDING,
    ) {
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    val memberRole: List<MemberRole>? = null

    private fun LocalDate.formatDate(): String =
        this.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    fun toResponseDto(): MemberResponse =
        MemberResponse(id!!, loginId, name, birthDate.formatDate(), gender.desc, email, memberRole!!.first().role.name, companyCode, state.name)

    fun toDto(): MemberDto =
        MemberDto(id!!, loginId, password, name, birthDate.formatDate(), gender.desc, email)

    override fun toString(): String {
        return "Member(id=$id, loginId='$loginId', password='$password', name='$name', birthDate=$birthDate, gender=$gender, email='$email', " +
                "ceoName=$ceoName, companyName=$companyName, businessId=$businessId, companyCertificateNumber=$companyCertificateNumber, memberRole=$memberRole)"
    }

}

@Entity
class MemberRole(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    val role: Role,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = ForeignKey(name = "fk_member_role_member_id"))
    val member: Member

) {
    override fun toString(): String {
        return "MemberRole(id=$id, role=$role, member=$member)"
    }
}