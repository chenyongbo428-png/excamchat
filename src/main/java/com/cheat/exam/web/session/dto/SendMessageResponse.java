package com.cheat.exam.web.session.dto;

public record SendMessageResponse(
    MessageItemResponse userMessage,
    MessageItemResponse assistantMessage
) {
}
