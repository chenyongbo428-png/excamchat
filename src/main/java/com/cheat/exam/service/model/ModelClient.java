package com.cheat.exam.service.model;

import java.util.function.Consumer;

/**
 * 统一模型客户端抽象。
 *
 * 后续无论接 OpenAI、Anthropic、Gemini 还是国内模型，业务层都只依赖这个接口。
 * 这样可以避免供应商 SDK / HTTP 细节污染会话、消息、画布等核心业务代码。
 */
public interface ModelClient {

    /**
     * 当前客户端声明的供应商编码，例如 OPENAI、ANTHROPIC、GEMINI、STUB。
     */
    String providerCode();

    /**
     * 判断当前客户端是否能处理指定模型配置。
     */
    boolean supports(ModelClientSelection selection);

    /**
     * 非流式对话调用。
     *
     * 第一版先稳定普通请求链路；SSE 流式输出后续再单独扩展。
     */
    ModelChatResponse chat(ModelChatRequest request);

    default ModelChatResponse stream(ModelChatRequest request, Consumer<String> chunkConsumer) {
        ModelChatResponse response = chat(request);
        String text = response.replyText() == null ? "" : response.replyText();
        int cursor = 0;
        int chunkSize = 24;
        while (cursor < text.length()) {
            int next = Math.min(text.length(), cursor + chunkSize);
            chunkConsumer.accept(text.substring(cursor, next));
            cursor = next;
        }
        return response;
    }
}
