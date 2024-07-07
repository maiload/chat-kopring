package org.example.chatkopring.chat.config

import org.springframework.stereotype.Component
import java.util.*

@Component
class UserSessionRegistry {
    private val sessions: MutableSet<String> = Collections.synchronizedSet(HashSet())

    fun registerSession(loginId: String): Boolean {
        synchronized(sessions) {
            return if(!isExist(loginId)){
                sessions.add(loginId)
                true
            }else false
        }
    }

    fun removeSession(loginId: String): Boolean {
        synchronized(sessions) {
            return if(isExist(loginId)){
                sessions.remove(loginId)
                true
            }else false
        }
    }

    fun isExist(loginId: String): Boolean = sessions.contains(loginId)

    fun getUsers(): MutableSet<String> = sessions
}