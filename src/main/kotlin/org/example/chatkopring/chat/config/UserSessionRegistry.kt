package org.example.chatkopring.chat.config

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class UserSessionRegistry {
    private val sessions = ConcurrentHashMap<String, String>()

    fun registerSession(sessionId: String, username: String = "") {
        sessions[sessionId] = username
    }

    fun getUsername(sessionId: String): String? = sessions[sessionId]

    fun removeSession(sessionId: String) = sessions.remove(sessionId)
}