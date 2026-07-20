package com.cheat.exam.service.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cheat.exam.common.api.ApiException;
import com.cheat.exam.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BailianModelClientTests {

    @TempDir
    Path tempDir;

    @Test
    void supportsBailianProviderSelection() {
        BailianModelClient client = new BailianModelClient(config("https://example.com", "test-key"), new ObjectMapper());

        assertThat(client.supports(new ModelClientSelection(
            "qwen-vl-plus",
            "BAILIAN",
            true,
            false,
            "{}"
        ))).isTrue();
    }

    @Test
    void throwsClearErrorWhenApiKeyMissing() {
        BailianModelClient client = new BailianModelClient(config("https://example.com", ""), new ObjectMapper());

        assertThatThrownBy(() -> client.chat(sampleRequest("missing-key", null)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("BAILIAN_API_KEY");
    }

    @Test
    void sendsImageAsDataUrlAndParsesStructuredResponse() throws Exception {
        Path imagePath = tempDir.resolve("question.png");
        Files.write(imagePath, new byte[] {1, 2, 3, 4});
        HttpServer server = startMockServer();
        try {
            int port = server.getAddress().getPort();
            BailianModelClient client = new BailianModelClient(
                config("http://127.0.0.1:" + port, "test-key"),
                new ObjectMapper()
            );

            ModelChatResponse response = client.chat(sampleRequest("qwen-vl-plus", imagePath));

            assertThat(response.providerCode()).isEqualTo("BAILIAN");
            assertThat(response.modelCode()).isEqualTo("qwen-vl-plus");
            assertThat(response.replyText()).contains("先观察题干");
            assertThat(response.guidanceStage()).isEqualTo("observe");
            assertThat(response.hintLevel()).isEqualTo(1);
            assertThat(response.annotations()).hasSize(1);
            assertThat(response.providerRequestId()).isEqualTo("mock-request-1");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fallsBackToTextWhenModelReturnsInvalidJson() throws Exception {
        Path imagePath = tempDir.resolve("question-invalid.png");
        Files.write(imagePath, new byte[] {5, 6, 7, 8});
        HttpServer server = startMockServer("not a valid json {\"replyText\":\"先观察题目\"");
        try {
            int port = server.getAddress().getPort();
            BailianModelClient client = new BailianModelClient(
                config("http://127.0.0.1:" + port, "test-key"),
                new ObjectMapper()
            );

            ModelChatResponse response = client.chat(sampleRequest("qwen-vl-plus", imagePath));

            assertThat(response.replyText()).contains("先观察题目");
            assertThat(response.annotations()).isEmpty();
            assertThat(response.rawPayload()).containsEntry("parseFallback", true);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void displaysPlainTextAnswerWithoutJsonRequirement() throws Exception {
        Path imagePath = tempDir.resolve("question-plain.png");
        Files.write(imagePath, new byte[] {9, 10, 11, 12});
        HttpServer server = startMockServer("题目是 2 + 3 = ?。答案是 5，因为 2 加 3 等于 5。");
        try {
            int port = server.getAddress().getPort();
            BailianModelClient client = new BailianModelClient(
                config("http://127.0.0.1:" + port, "test-key"),
                new ObjectMapper()
            );

            ModelChatResponse response = client.chat(sampleRequest("qwen-vl-plus", imagePath));

            assertThat(response.replyText()).contains("答案是 5");
            assertThat(response.guidanceStage()).isEqualTo("observe");
            assertThat(response.teacherIntent()).isEqualTo("guide_next_step");
            assertThat(response.shouldRevealFinalAnswer()).isFalse();
            assertThat(response.annotations()).isEmpty();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void keepsLongPlainTextAnswerWhenFallingBackFromUnstructuredContent() throws Exception {
        Path imagePath = tempDir.resolve("question-long.png");
        Files.write(imagePath, new byte[] {13, 14, 15, 16});
        String longAnswer = "题目内容：".repeat(90) + "最终结论：PA = PF。";
        HttpServer server = startMockServer(longAnswer);
        try {
            int port = server.getAddress().getPort();
            BailianModelClient client = new BailianModelClient(
                config("http://127.0.0.1:" + port, "test-key"),
                new ObjectMapper()
            );

            ModelChatResponse response = client.chat(sampleRequest("qwen-vl-plus", imagePath));

            assertThat(response.replyText()).isEqualTo(longAnswer);
            assertThat(response.replyText()).endsWith("最终结论：PA = PF。");
            assertThat(response.replyText()).doesNotEndWith("...");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startMockServer() throws IOException {
        return startMockServer(null);
    }

    private HttpServer startMockServer(String overrideContent) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/compatible-mode/v1/chat/completions", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
            assertThat(requestBody).contains("\"model\":\"qwen-vl-plus\"");
            assertThat(requestBody).contains("data:image/png;base64,");
            assertThat(requestBody).contains("\"max_tokens\"");
            assertThat(requestBody).contains("引导式讲解模式");
            assertThat(requestBody).contains("不要在第一轮直接给最终答案");
            assertThat(requestBody).contains("先判断学生");
            assertThat(requestBody).doesNotContain("response_format");

            String businessJson = """
                {
                  "replyText": "我们先观察题干里给出的已知条件。",
                  "guidanceStage": "observe",
                  "hintLevel": 1,
                  "shouldRevealFinalAnswer": false,
                  "teacherIntent": "guide_next_step",
                  "annotations": [
                    {"type":"rect","x":10,"y":20,"width":100,"height":40,"color":"#ff6b6b","label":"已知条件"}
                  ]
                }
                """.replace("\n", "");
            String content = overrideContent == null ? businessJson : overrideContent;
            String responseBody = """
                {
                  "id": "mock-request-1",
                  "object": "chat.completion",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": %s
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """.formatted(new ObjectMapper().writeValueAsString(content));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private ModelChatRequest sampleRequest(String modelCode, Path imagePath) {
        ModelImageInput image = imagePath == null
            ? null
            : new ModelImageInput(1L, "/api/images/1/content", imagePath.toString(), null, null, "image/png");
        return new ModelChatRequest(
            100L,
            modelCode,
            "BAILIAN",
            "MATH",
            "JUNIOR",
            "你是一位引导型老师。",
            "请讲解这道题",
            image,
            List.of(new ModelMessageInput(1L, "USER", "上一轮问题")),
            "guided",
            false
        );
    }

    private AppProperties config(String baseUrl, String apiKey) {
        AppProperties properties = new AppProperties();
        properties.getModel().getBailian().setBaseUrl(baseUrl);
        properties.getModel().getBailian().setApiKey(apiKey);
        properties.getModel().getBailian().setTimeoutSeconds(5);
        properties.getModel().getBailian().setMaxTokens(200);
        properties.getModel().getBailian().setTemperature(0.1);
        return properties;
    }
}
