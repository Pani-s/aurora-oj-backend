package com.pani.oj.mq.rabbitmq;

import com.pani.oj.constant.MqConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Pani
 * @date Created in 2024/3/17 14:16
 * @description
 */
@Component
public class QuestionMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param message question submit id
     */
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(MqConstant.QUESTION_EXCHANGE_NAME, MqConstant.QUESTION_ROUTING_KEY, message);
    }

}
