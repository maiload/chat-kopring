package org.example.chatkopring.chat.config

import org.springframework.stereotype.Component
import java.util.*

@Component
class UserSessionRegistry {
    private val sessions: MutableSet<String> = Collections.synchronizedSet(HashSet())

    fun registerSession(loginId: String) = sessions.add(loginId)

    fun removeSession(loginId: String) = sessions.remove(loginId)

    fun isExist(loginId: String): Boolean = sessions.contains(loginId)

    fun getUsers(): MutableSet<String> = sessions
}