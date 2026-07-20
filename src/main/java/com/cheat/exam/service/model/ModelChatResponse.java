package com.cheat.exam.service.model;

import java.util.List;
import java.util.Map;

/**
 * 所有模型供应商统一返回到业务层的响应结构。
 *
 * annotations 保持“模型原始结构化标注”语义，进入消息和画布前还会经过
 * AiAnnotationParser 做协议标准化、裁剪和兜底。
 */
public record ModelChatResponse(
    String providerCode,
    String modelCode,
    String replyText,
    int hintLevel,
    String guidanceStage,
    String teacherIntent,
    boolean shouldRevealFinalAnswer,
    List<Map<String, Object>> annotations,
    Map<String, Object> rawPayload,
    String providerRequestId
) {
}
