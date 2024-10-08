package com.pani.oj.mq.rabbitmq;

import com.pani.oj.constant.MqConstant;
import com.pani.oj.judge.JudgeService;
import com.pani.oj.service.QuestionSubmitService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Pani
 * @date Created in 2024/3/17 14:19
 * @description
 */
@Component
@Slf4j
public class JudgeMessageConsumer {
    @Resource
    private JudgeService judgeService;
    @Resource
    private QuestionSubmitService questionSubmitService;

    /**
     *     指定程序监听的消息队列和确认机制
      */
    @SneakyThrows
    @RabbitListener(queues = {MqConstant.JUDGE_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("消息队列receiveMessage message = {}", message);
        long questionSubmitId = Long.parseLong(message);
        try {
            judgeService.doJudge(questionSubmitId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消息队列，捕捉到异常：{}",e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 死信消费异常消息
     *
     */
    @SneakyThrows
    @RabbitListener(queues = {MqConstant.JUDGE_DLX_QUEUE}, ackMode = "MANUAL")
    public void receiveErrorMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        long questionSubmitId = Long.parseLong(message);
        log.info("---------死信消费异常消息--------questionSubmit id : {}",questionSubmitId);
        boolean b = questionSubmitService.setQuestionSubmitFailure(questionSubmitId);
        if(!b){
            log.error("题目状态数据库更改失败！");
        }
        log.info("---------死信消费异常消息成功--------");
        channel.basicAck(deliveryTag,false);
    }

}
