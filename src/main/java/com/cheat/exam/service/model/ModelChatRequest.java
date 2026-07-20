package com.cheat.exam.service.model;

import java.util.List;

/**
 * 业务层传给模型适配器的统一请求对象。
 *
 * 这里包含会话、学科年级、历史消息、当前问题、题图和系统提示词，
 * 足够支撑第一版“题图 + 引导式讲解 + 结构化标注”闭环。
 */
public record ModelChatRequest(
    Long sessionId,
    String modelCode,
    String providerCode,
    String subjectCode,
    String gradeLevel,
    String systemPrompt,
    String currentUserMessage,
    ModelImageInput image,
    List<ModelMessageInput> messages,
    String guidanceMode,
    boolean streamRequested
) {
}
