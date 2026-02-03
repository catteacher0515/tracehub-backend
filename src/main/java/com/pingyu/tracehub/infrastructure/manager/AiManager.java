package com.pingyu.tracehub.infrastructure.manager;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AiManager {

    /**
     * 发送请求并获取 AI 的回复文本
     *
     * @param systemMessage 系统预设
     * @param userMessage   用户输入
     * @return AI 回复
     */
    public String doChat(String systemMessage, String userMessage) {
        Generation gen = new Generation();
        List<Message> messages = new ArrayList<>();

        // 构造消息
        Message sysMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemMessage)
                .build();
        messages.add(sysMsg);

        Message usrMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userMessage)
                .build();
        messages.add(usrMsg);

        GenerationParam param = GenerationParam.builder()
                .model(Generation.Models.QWEN_PLUS)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        try {
            // 【日志埋点】记录请求内容（便于调试 Prompt 效果）
            log.info("【AI 请求】发送消息给 Qwen-Plus, UserMessage: {}", userMessage);

            GenerationResult result = gen.call(param);
            String content = result.getOutput().getChoices().get(0).getMessage().getContent();

            // 【日志埋点】记录响应内容（便于排查 JSON 格式问题）
            log.info("【AI 响应】调用成功, 响应内容: {}", content);

            return content;
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务异常");
        }
    }
}