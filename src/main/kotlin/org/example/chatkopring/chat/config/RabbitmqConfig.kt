package org.example.chatkopring.chat.config

import org.example.chatkopring.chat.exception.CustomFatalExceptionStrategy
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ErrorHandler
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties(prefix = "spring.rabbitmq")
class RabbitmqConfig {
    lateinit var host: String
    var port by Delegates.notNull<Int>()
    lateinit var username: String
    lateinit var password: String
    lateinit var ex: String
    lateinit var dex: String
    lateinit var sq: String
    lateinit var lq: String
    lateinit var iq: String
    lateinit var cq: String
    lateinit var dq: String

    @Bean
    fun directExchange() = DirectExchange(ex)

    @Bean
    fun deadLetterExchange() = DirectExchange(dex)

    @Bean
    fun createQueue(): Queue =
        QueueBuilder.durable(cq)
            .withArgument("x-queue-type", "quorum")
            .withArgument("x-dead-letter-exchange", deadLetterExchange().name)
            .withArgument("x-dead-letter-routing-key", deadLetterQueue().name)
            .build()

    @Bean
    fun leaveQueue(): Queue =
        QueueBuilder.durable(lq)
            .withArgument("x-queue-type", "quorum")
            .withArgument("x-dead-letter-exchange", deadLetterExchange().name)
            .withArgument("x-dead-letter-routing-key", deadLetterQueue().name)
            .build()

    @Bean
    fun inviteQueue(): Queue =
        QueueBuilder.durable(iq)
            .withArgument("x-queue-type", "quorum")
            .withArgument("x-dead-letter-exchange", deadLetterExchange().name)
            .withArgument("x-dead-letter-routing-key", deadLetterQueue().name)
            .build()

    @Bean
    fun sendQueue(): Queue =
        QueueBuilder.durable(sq)
            .withArgument("x-queue-type", "quorum")
            .withArgument("x-dead-letter-exchange", deadLetterExchange().name)
            .withArgument("x-dead-letter-routing-key", deadLetterQueue().name)
            .build()

    @Bean
    fun deadLetterQueue(): Queue = Queue(dq, true)

    @Bean
    fun createBinding(): Binding = BindingBuilder.bind(createQueue()).to(directExchange()).with(createQueue().name)


    @Bean
    fun leaveBinding(): Binding = BindingBuilder.bind(leaveQueue()).to(directExchange()).with(leaveQueue().name)


    @Bean
    fun inviteBinding(): Binding = BindingBuilder.bind(inviteQueue()).to(directExchange()).with(inviteQueue().name)


    @Bean
    fun sendBinding(): Binding = BindingBuilder.bind(sendQueue()).to(directExchange()).with(sendQueue().name)

    @Bean
    fun deadLetterBinding(): Binding = BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(deadLetterQueue().name)


    @Bean
    fun connectionFactory(): ConnectionFactory =
        CachingConnectionFactory()
            .apply {
                setHost(this@RabbitmqConfig.host)
                port = this@RabbitmqConfig.port
                username = this@RabbitmqConfig.username
                setPassword(this@RabbitmqConfig.password)
            }

    @Bean
    fun rabbitListenerContainerFactory(connectionFactory: ConnectionFactory,
                                       configurer: SimpleRabbitListenerContainerFactoryConfigurer): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        configurer.configure(factory, connectionFactory);
        factory.setErrorHandler(errorhandler())
        return factory
    }

    @Bean
    fun errorhandler(): ErrorHandler = ConditionalRejectingErrorHandler(customExceptionStrategy())

    @Bean
    fun customExceptionStrategy(): FatalExceptionStrategy  = CustomFatalExceptionStrategy()

    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(): RabbitTemplate = RabbitTemplate(connectionFactory()).apply { this.messageConverter = messageConverter() }

}