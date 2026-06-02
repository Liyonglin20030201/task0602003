package com.reviewbot.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_QUEUE = "review.requests";
    public static final String REVIEW_ROUTING_KEY = "review.new";

    public static final String DLX_EXCHANGE = "review.dlx";
    public static final String DLQ_QUEUE = "review.requests.dlq";

    @Bean
    public DirectExchange reviewExchange() {
        return new DirectExchange(REVIEW_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue reviewQueue() {
        return QueueBuilder.durable(REVIEW_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "review.dead")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding reviewBinding(Queue reviewQueue, DirectExchange reviewExchange) {
        return BindingBuilder.bind(reviewQueue).to(reviewExchange).with(REVIEW_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("review.dead");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
