package org.example.chatkopring.member.entity

import jakarta.persistence.*

@Entity
@Table
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 30)
    val companyName: String,

    @Column(nullable = false, length = 20)
    val companyCode: String,
)
