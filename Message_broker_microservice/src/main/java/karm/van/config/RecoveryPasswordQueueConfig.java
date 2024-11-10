package karm.van.config;

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
public class RecoveryPasswordQueueConfig {

    @Value("${rabbitmq.queue.recovery.name}")
    public String recoveryQueue;

    @Value("${rabbitmq.routing-key.recovery.name}")
    public String recoveryRoutingKey;

    private final TopicExchange finderExchange;

    @Bean
    public Queue recoveryQueue() {
        return new Queue(recoveryQueue);
    }

    @Bean
    public Binding recoveryMessageBinding() {
        return BindingBuilder.bind(recoveryQueue()).to(finderExchange).with(recoveryRoutingKey);
    }
}

