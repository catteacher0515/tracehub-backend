package com.pingyu.tracehub.infrastructure.config;

import com.alibaba.dashscope.utils.Constants;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Data
public class AliYunAiConfig {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    @PostConstruct
    public void init() {
        if (apiKey != null) {
            Constants.apiKey = apiKey;
        }
    }
}
