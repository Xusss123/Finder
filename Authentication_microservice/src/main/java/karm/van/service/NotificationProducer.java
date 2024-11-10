package karm.van.service;

import karm.van.dto.response.RecoveryMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {
    @Value("${rabbitmq.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.recovery.name}")
    private String recoveryRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public void sendRecoveryMessage(RecoveryMessageDto recoveryMessageDto){
        rabbitTemplate.convertAndSend(exchangeName,recoveryRoutingKey,recoveryMessageDto);
    }

}
