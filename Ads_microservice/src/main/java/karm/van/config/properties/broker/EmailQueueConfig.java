package karm.van.config.properties.broker;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EmailQueueConfig {

    @Value("${rabbitmq.queue.email.name}")
    public String emailQueue;

    @Value("${rabbitmq.routing-key.email.name}")
    public String emailRoutingKey;

    private final TopicExchange finderExchange;

    @Bean
    public Queue emailQueue() {
        return new Queue(emailQueue);
    }

    @Bean
    public Binding emailMessageBinding() {
        return BindingBuilder.bind(emailQueue()).to(finderExchange).with(emailRoutingKey);
    }
}

