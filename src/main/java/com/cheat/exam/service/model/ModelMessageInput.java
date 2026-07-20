package com.cheat.exam.service.model;

/**
 * 模型上下文中的一条历史消息。
 */
public record ModelMessageInput(
    Long messageId,
    String roleCode,
    String contentText
) {
}
