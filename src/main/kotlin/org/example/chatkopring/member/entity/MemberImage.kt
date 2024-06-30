package org.example.chatkopring.member.entity

import jakarta.persistence.*

@Entity
class MemberImage(
    @Column(nullable = false, length = 250)
    val originFileName: String,

    @Column(nullable = false, length = 250)
    val storageFileName: String,

    @Column(nullable = false)
    val fileSize: Long,

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToOne(mappedBy = "memberImage")
    val member: Member? = null
}