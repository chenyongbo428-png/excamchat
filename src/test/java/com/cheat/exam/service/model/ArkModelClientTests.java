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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArkModelClientTests {

    @TempDir
    Path tempDir;

    @Test
    void supportsArkProviderSelection() {
        ArkModelClient client = new ArkModelClient(config("https://ark.cn-beijing.volces.com/api/v3", "test-key"), new ObjectMapper());

        assertThat(client.supports(new ModelClientSelection(
            "doubao-seed-2-1-turbo-260628",
            "ARK",
            true,
            true,
            "{}"
        ))).isTrue();
    }

    @Test
    void throwsClearErrorWhenApiKeyMissing() {
        ArkModelClient client = new ArkModelClient(config("https://ark.cn-beijing.volces.com/api/v3", ""), new ObjectMapper());

        assertThatThrownBy(() -> client.chat(sampleRequest("doubao-seed-2-1-turbo-260628", null)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("ARK_API_KEY");
    }

    @Test
    void sendsImageAsDataUrlAndParsesPlainTextResponse() throws Exception {
        Path imagePath = tempDir.resolve("question.png");
        Files.write(imagePath, new byte[] {1, 2, 3, 4});
        HttpServer server = startMockServer();
        try {
            int port = server.getAddress().getPort();
            ArkModelClient client = new ArkModelClient(
                config("http://127.0.0.1:" + port + "/api/v3", "test-key"),
                new ObjectMapper()
            );

            ModelChatResponse response = client.chat(sampleRequest("doubao-seed-2-1-turbo-260628", imagePath));

            assertThat(response.providerCode()).isEqualTo("ARK");
            assertThat(response.modelCode()).isEqualTo("doubao-seed-2-1-turbo-260628");
            assertThat(response.replyText()).contains("先观察题目");
            assertThat(response.guidanceStage()).isEqualTo("observe");
            assertThat(response.teacherIntent()).isEqualTo("guide_next_step");
            assertThat(response.annotations()).isEmpty();
            assertThat(response.providerRequestId()).isEqualTo("ark-request-1");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void streamsReasoningContentDeltas() throws Exception {
        HttpServer server = startMockStreamServer();
        try {
            int port = server.getAddress().getPort();
            ArkModelClient client = new ArkModelClient(
                config("http://127.0.0.1:" + port + "/api/v3", "test-key"),
                new ObjectMapper()
            );
            List<String> chunks = new ArrayList<>();

            ModelChatResponse response = client.stream(sampleRequest("ep-20260721164323-qjbgk", null), chunks::add);

            assertThat(chunks).containsExactly("先想", "条件");
            assertThat(response.replyText()).isEqualTo("先想条件");
            assertThat(response.providerRequestId()).isEqualTo("ark-stream-1");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer startMockServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v3/chat/completions", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
            assertThat(requestBody).contains("\"model\":\"doubao-seed-2-1-turbo-260628\"");
            assertThat(requestBody).contains("data:image/png;base64,");
            assertThat(requestBody).contains("\"max_tokens\"");
            assertThat(requestBody).contains("引导式讲解模式");
            assertThat(requestBody).contains("不要在第一轮直接给最终答案");
            assertThat(requestBody).contains("先判断学生");

            String responseBody = """
                {
                  "id": "ark-request-1",
                  "object": "chat.completion",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "我们先观察题目给了哪些条件，然后你试着说出第一步想用什么关系。"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private HttpServer startMockStreamServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v3/chat/completions", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
            assertThat(requestBody).contains("\"stream\":true");
            assertThat(requestBody).contains("\"model\":\"ep-20260721164323-qjbgk\"");

            String responseBody = """
                data: {"id":"ark-stream-1","choices":[{"index":0,"delta":{"reasoning_content":"先想","role":"assistant"}}]}

                data: {"id":"ark-stream-1","choices":[{"index":0,"delta":{"content":"条件","role":"assistant"}}]}

                data: [DONE]

                """;
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
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
            "ARK",
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
        properties.getModel().getArk().setBaseUrl(baseUrl);
        properties.getModel().getArk().setApiKey(apiKey);
        properties.getModel().getArk().setTimeoutSeconds(5);
        properties.getModel().getArk().setMaxTokens(200);
        properties.getModel().getArk().setTemperature(0.1);
        return properties;
    }
}
