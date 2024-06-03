package org.example.chatkopring.common.dto

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomUser(
    val userId: Long,
    username: String,
    password: String,
    authorities: Collection<GrantedAuthority>,
    private val attributes: MutableMap<String, Any> = mutableMapOf()
) : User(username, password, authorities), OAuth2User {

    override fun getName(): String = username

    override fun getAttributes(): MutableMap<String, Any> = attributes

}
