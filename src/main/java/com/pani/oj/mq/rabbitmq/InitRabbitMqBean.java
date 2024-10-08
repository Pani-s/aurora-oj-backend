package com.pani.oj.mq.rabbitmq;

import com.pani.oj.constant.MqConstant;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pani
 * @date Created in 2024/3/17 14:21
 * @description
 */
@Slf4j
@Component
public class InitRabbitMqBean {
    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.pwd:guest}")
    private String pwd;

//    public static void main(String[] args) {
//        new InitRabbitMqBean().init();
//    }

    @PostConstruct
    public void init() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(pwd);
        //部署的时候还需要账号密码 > .<

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明死信队列
            channel.exchangeDeclare(MqConstant.JUDGE_DLX_EXCHANGE, "direct");
            channel.queueDeclare(MqConstant.JUDGE_DLX_QUEUE, true, false, false, null);
            channel.queueBind(MqConstant.JUDGE_DLX_QUEUE, MqConstant.JUDGE_DLX_EXCHANGE,
                    MqConstant.JUDGE_DLX_ROUTING);
            //判题队列
            channel.exchangeDeclare(MqConstant.JUDGE_EXCHANGE_NAME, "direct");
            Map<String, Object> arg = new HashMap<>();
            arg.put("x-dead-letter-exchange", MqConstant.JUDGE_DLX_EXCHANGE);
            arg.put("x-dead-letter-routing-key", MqConstant.JUDGE_DLX_ROUTING);
            // 声明队列，设置队列持久化、非独占、非自动删除，并传入额外的参数为 map 的arg!!!!
            channel.queueDeclare(MqConstant.JUDGE_QUEUE_NAME, true, false, false, arg);
            channel.queueBind(MqConstant.JUDGE_QUEUE_NAME, MqConstant.JUDGE_EXCHANGE_NAME,
                    MqConstant.JUDGE_ROUTING_KEY);

            //question的
            channel.exchangeDeclare(MqConstant.QUESTION_EXCHANGE_NAME, "direct");
            channel.queueDeclare(MqConstant.QUESTION_QUEUE_NAME, true, false, false, null);
            channel.queueBind(MqConstant.QUESTION_QUEUE_NAME, MqConstant.QUESTION_EXCHANGE_NAME,
                    MqConstant.QUESTION_ROUTING_KEY);

            log.info("消息队列启动成功");
        } catch (Exception e) {
            log.error("消息队列启动失败", e);
        }
    }

}
