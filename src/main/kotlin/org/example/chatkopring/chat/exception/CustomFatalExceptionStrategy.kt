package org.example.chatkopring.chat.exception

import org.example.chatkopring.common.exception.InvalidInputException
import org.example.chatkopring.common.exception.UnAuthorizationException
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler

class CustomFatalExceptionStrategy: ConditionalRejectingErrorHandler.DefaultExceptionStrategy() {
    override fun isFatal(t: Throwable): Boolean {
        return super.isFatal(t) || t.cause is InvalidInputException || t.cause is UnAuthorizationException
    }
}