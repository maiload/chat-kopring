package org.example.chatkopring.common.dto

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomUser(
    val userId: Long,
    userName: String,
    password: String,
    authorities: Collection<GrantedAuthority>
//) : User(userName, password, authorities), OAuth2User {
) : User(userName, password, authorities), OAuth2User {

    override fun getName(): String = username

    override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()

}
