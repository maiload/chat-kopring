package org.example.chatkopring.member.entity

import jakarta.persistence.*

@Entity
@Table
class BlackList(
    @Column(nullable = false)
    val invalidRefreshToken: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}