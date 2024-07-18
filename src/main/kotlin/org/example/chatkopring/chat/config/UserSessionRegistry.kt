package org.example.chatkopring.chat.config

import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class UserSessionRegistry {
    private val sessions: ConcurrentHashMap<String, MutableMap<String, String>> = ConcurrentHashMap()

    fun registerUser(loginId: String): UserState {
        synchronized(sessions) {
            return if(!isExistUser(loginId)){
                sessions[loginId] = mutableMapOf()
                UserState.CONNECT
            }else UserState.DUP_CONNECT
        }
    }

    fun removeUser(loginId: String): UserState {
        synchronized(sessions) {
            return if(isExistUser(loginId)){
                sessions.remove(loginId)
                UserState.DISCONNECT
            }else UserState.DUP_DISCONNECT
        }
    }

    fun addSession(loginId: String, subId: String, destination: String): UserState {
        synchronized(sessions) {
            return if(isExistUser(loginId)) {
                if(!isExistDestination(loginId, destination)){
                    val innerMap = sessions[loginId]!!
                    innerMap[subId] = destination
                    UserState.SUBSCRIBE
                } else UserState.DUP_SUBSCRIBE
            }else UserState.NOT_CONNECT
        }
    }

    fun removeSession(loginId: String, subId: String): UserState {
        synchronized(sessions) {
            return if(isExistUser(loginId)){
                if(isExistSubId(loginId, subId)) {
                    sessions[loginId]!!.remove(subId)
                    UserState.UNSUBSCRIBE
                } else UserState.DUP_UNSUBSCRIBE
            } else UserState.NOT_CONNECT
        }
    }

    fun isExistUser(loginId: String): Boolean = sessions.containsKey(loginId)
    fun isExistDestination(loginId: String, destination: String): Boolean = sessions[loginId]!!.contains(destination)
    fun isExistSubId(loginId: String, subId: String): Boolean = sessions[loginId]!!.containsKey(subId)

    fun getSessions() = this.sessions
}