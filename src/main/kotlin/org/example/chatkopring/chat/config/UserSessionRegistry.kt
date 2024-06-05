package org.example.chatkopring.chat.config

import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class UserSessionRegistry {
    private val sessions = Collections.synchronizedSet(mutableSetOf<String>())

    fun registerSession(sessionId: String) = sessions.add(sessionId)

    fun removeSession(sessionId: String) = sessions.remove(sessionId)
}