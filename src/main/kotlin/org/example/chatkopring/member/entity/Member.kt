package org.example.chatkopring.member.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*
import org.example.chatkopring.common.status.Gender
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.common.status.State
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.dto.MemberResponse
import org.springframework.core.io.Resource
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
    var name: String,

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    var birthDate: LocalDate,

    @Column(nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    var gender: Gender,

    @Column(nullable = false, length = 30)
    var email: String,

    @Column(nullable = false, length = 300)
    var profile: String,

    @Column(nullable = true, length = 20)
    var companyCode: String? = null,

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
    @OneToMany(mappedBy = "member")
    val memberRole: List<MemberRole>? = null

    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(foreignKey = ForeignKey(name = "fk_member_image_memberImage_id"))
    var memberImage: MemberImage? = null

    private fun LocalDate.formatDate(): String =
        this.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    fun toResponseDto(): MemberResponse =
        MemberResponse(id!!, loginId, name, birthDate.formatDate(), gender.name, email, memberRole!!.first().role.name, state.name, profile,
            companyCode, ceoName, companyName, businessId, companyCertificateNumber)

    fun toDto(): MemberDto =
        MemberDto(id!!, loginId, password, name, birthDate.formatDate(), gender.name, email)

    override fun toString(): String {
        return "Member(id=$id, loginId='$loginId', password='$password', name='$name', birthDate=$birthDate, gender=$gender, email='$email', " +
                "companyCode=$companyCode, ceoName=$ceoName, companyName=$companyName, businessId=$businessId, companyCertificateNumber=$companyCertificateNumber, " +
                "state=$state, profile='$profile', memberRole=$memberRole)"
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