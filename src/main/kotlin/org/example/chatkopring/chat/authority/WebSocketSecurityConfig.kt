package org.example.chatkopring.chat.authority

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer

@Configuration
class WebSocketSecurityConfig: AbstractSecurityWebSocketMessageBrokerConfigurer() {
    override fun configureInbound(messages: MessageSecurityMetadataSourceRegistry) {
        messages.nullDestMatcher().permitAll()      // Connect 연결 허용
            .simpDestMatchers("/pub/**").authenticated()
            .simpSubscribeDestMatchers("/sub/**").authenticated()
            .anyMessage().denyAll()
    }

    override fun sameOriginDisabled(): Boolean {
        return true
    }
}