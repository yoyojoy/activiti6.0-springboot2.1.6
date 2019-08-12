package com.shengyecapital.process.config;

import com.shengyecapital.process.constant.ProcessConstant;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {

    @Bean
    public Queue invoiceCheckCallbackQueue() {
        return new Queue(ProcessConstant.PROCESS_TASK_COMPLETE_AFTER);
    }

}
