package com.pani.oj.config;


import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Pani
 * @date Created in 2023/11/19 23:03
 * @description 从application.yml文件中读取前缀为"spring.redis"的配置项
 */
//todo: 记得去pom里取消 Redisson 注释，还有下面的
//@Configuration
//@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private Integer database;

    private String host;

    private Integer port;

    // 如果redis默认没有密码，则不用写
    private String password;


    @Bean
    /**
     * spring启动时，会自动创建一个RedissonClient对象
     */
    public RedissonClient getRedissonClient() {
        // 1.创建配置对象
        Config config = new Config();
        // 添加单机Redisson配置
        config.useSingleServer()
                // 设置数据库
                .setDatabase(database)
                // 设置redis的地址
                .setAddress("redis://" + host + ":" + port)
                // 设置redis的密码(redis有密码才设置)
        //todo pwd
                .setPassword(password);

        // 2.创建Redisson实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
