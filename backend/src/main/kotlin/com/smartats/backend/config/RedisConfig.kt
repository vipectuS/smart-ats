package com.smartats.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartats.backend.queue.ResumeParseMessage
import com.smartats.backend.queue.ResumeQueueConsumer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(
    private val resumeQueueProperties: ResumeQueueProperties,
) {

    @Bean
    fun resumeParseTopic(): ChannelTopic = ChannelTopic(resumeQueueProperties.channel)

    @Bean
    fun resumeParseMessageSerializer(objectMapper: ObjectMapper): Jackson2JsonRedisSerializer<ResumeParseMessage> {
        val serializer = Jackson2JsonRedisSerializer(ResumeParseMessage::class.java)
        serializer.setObjectMapper(objectMapper)
        return serializer
    }

    @Bean
    fun resumeParseRedisTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisTemplate<String, ResumeParseMessage> {
        val template = RedisTemplate<String, ResumeParseMessage>()
        val keySerializer = StringRedisSerializer()
        val valueSerializer = Jackson2JsonRedisSerializer(ResumeParseMessage::class.java)
        valueSerializer.setObjectMapper(objectMapper)

        template.connectionFactory = connectionFactory
        template.keySerializer = keySerializer
        template.hashKeySerializer = keySerializer
        template.valueSerializer = valueSerializer
        template.hashValueSerializer = valueSerializer
        template.afterPropertiesSet()
        return template
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resume-queue", name = ["listener-enabled"], havingValue = "true")
    fun resumeParseListenerAdapter(
        resumeQueueConsumer: ResumeQueueConsumer,
        resumeParseMessageSerializer: Jackson2JsonRedisSerializer<ResumeParseMessage>,
    ): MessageListenerAdapter {
        val adapter = MessageListenerAdapter(resumeQueueConsumer, "handleMessage")
        adapter.setSerializer(resumeParseMessageSerializer)
        return adapter
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resume-queue", name = ["listener-enabled"], havingValue = "true")
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        resumeParseListenerAdapter: MessageListenerAdapter,
        resumeParseTopic: ChannelTopic,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(resumeParseListenerAdapter, resumeParseTopic)
        return container
    }
}