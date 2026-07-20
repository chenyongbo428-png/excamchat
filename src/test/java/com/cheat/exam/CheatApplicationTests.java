package com.cheat.exam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheatApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sessionAndMessageFlowWorks() throws Exception {
        jdbcTemplate.update("""
            insert into model_config (
                model_code, display_name, provider_code, supports_vision, supports_stream,
                enabled, sort_order, config_json, created_at, updated_at
            ) values (
                'stub-test', 'Stub Test Model', 'STUB', true, false,
                true, 99, '{}', current_timestamp, current_timestamp
            )
            """);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "student_flow",
                      "password": "12345678",
                      "nickname": "student"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "student_flow",
                      "password": "12345678"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andReturn();

        String token = JsonTestUtils.readJson(loginResult.getResponse().getContentAsString(), "$.data.accessToken");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "question.png",
            MediaType.IMAGE_PNG_VALUE,
            new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        );

        MvcResult uploadResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/images/upload")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andReturn();

        String imageId = JsonTestUtils.readJson(uploadResult.getResponse().getContentAsString(), "$.data.imageId");

        MvcResult createSessionResult = mockMvc.perform(post("/api/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "imageId": %s,
                      "modelCode": "stub-test",
                      "title": "测试会话",
                      "subjectCode": "MATH",
                      "gradeLevel": "JUNIOR"
                    }
                    """.formatted(imageId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andReturn();

        String sessionId = JsonTestUtils.readJson(createSessionResult.getResponse().getContentAsString(), "$.data.sessionId");

        MvcResult canvasResult = mockMvc.perform(get("/api/canvas/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.sessionId").value(Long.parseLong(sessionId)))
            .andExpect(jsonPath("$.data.version").value(1))
            .andExpect(jsonPath("$.data.backgroundImage.imageId").value(Long.parseLong(imageId)))
            .andExpect(jsonPath("$.data.snapshot.schemaVersion").value("1.0"))
            .andReturn();

        String canvasVersion = JsonTestUtils.readJson(canvasResult.getResponse().getContentAsString(), "$.data.version");

        mockMvc.perform(put("/api/canvas/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": %s,
                      "snapshot": {
                        "schemaVersion": "1.0",
                        "background": {
                          "imageId": "%s",
                          "width": 0,
                          "height": 0
                        },
                        "layers": [
                          {
                            "layerId": "ai-layer",
                            "layerType": "AI",
                            "visible": true,
                            "locked": false,
                            "objects": []
                          },
                          {
                            "layerId": "user-layer",
                            "layerType": "USER",
                            "visible": true,
                            "locked": false,
                            "objects": [
                              {
                                "objectId": "u1",
                                "type": "text",
                                "source": "USER",
                                "x": 20,
                                "y": 30,
                                "text": "第一步"
                              }
                            ]
                          }
                        ]
                      }
                    }
                    """.formatted(canvasVersion, imageId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.version").value(2));

        mockMvc.perform(post("/api/canvas/{sessionId}/operations", sessionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operations": [
                        {
                          "operationType": "ADD_OBJECT",
                          "layerType": "USER",
                          "payload": {
                            "objectId": "u2",
                            "type": "arrow",
                            "x": 10,
                            "y": 20,
                            "toX": 80,
                            "toY": 90
                          }
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.accepted").value(1))
            .andExpect(jsonPath("$.data.version").value(3));

        mockMvc.perform(get("/api/sessions")
                .header("Authorization", "Bearer " + token)
                .param("page", "1")
                .param("pageSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.items[0].sessionId").value(Long.parseLong(sessionId)));

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.sessionId").value(Long.parseLong(sessionId)))
            .andExpect(jsonPath("$.data.image.imageId").value(Long.parseLong(imageId)));

        mockMvc.perform(post("/api/sessions/{sessionId}/messages", sessionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "我不会这道题，下一步该看哪里？",
                      "useStream": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.userMessage.roleCode").value("USER"))
            .andExpect(jsonPath("$.data.assistantMessage.roleCode").value("ASSISTANT"))
            .andExpect(jsonPath("$.data.assistantMessage.hintLevel").value(2))
            .andExpect(jsonPath("$.data.assistantMessage.guidanceStage").value("analyze"))
            .andExpect(jsonPath("$.data.assistantMessage.annotationSummary[0].objectId").exists())
            .andExpect(jsonPath("$.data.assistantMessage.annotationSummary[0].type").value("rect"))
            .andExpect(jsonPath("$.data.assistantMessage.annotationSummary[0].source").value("AI"))
            .andExpect(jsonPath("$.data.assistantMessage.annotationSummary[0].style.strokeColor").value("#ff6b6b"))
            .andExpect(jsonPath("$.data.assistantMessage.annotationSummary[0].meta.messageId").exists());

        mockMvc.perform(get("/api/canvas/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.snapshot.layers[0].layerType").value("AI"))
            .andExpect(jsonPath("$.data.snapshot.layers[0].objects[0].type").value("rect"))
            .andExpect(jsonPath("$.data.snapshot.layers[0].objects[0].source").value("AI"));

        mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].roleCode").value("USER"))
            .andExpect(jsonPath("$.data[1].roleCode").value("ASSISTANT"));

        mockMvc.perform(get("/api/replay/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.session.sessionId").value(Long.parseLong(sessionId)))
            .andExpect(jsonPath("$.data.backgroundImage.imageId").value(Long.parseLong(imageId)))
            .andExpect(jsonPath("$.data.timeline.length()").value(4))
            .andExpect(jsonPath("$.data.timeline[0].stepNo").value(1))
            .andExpect(jsonPath("$.data.timeline[0].stepType").value("CANVAS_OPERATION"))
            .andExpect(jsonPath("$.data.timeline[0].operation.operationType").value("ADD_OBJECT"))
            .andExpect(jsonPath("$.data.timeline[1].stepType").value("MESSAGE"))
            .andExpect(jsonPath("$.data.timeline[1].roleCode").value("USER"))
            .andExpect(jsonPath("$.data.timeline[2].stepType").value("MESSAGE"))
            .andExpect(jsonPath("$.data.timeline[2].roleCode").value("ASSISTANT"))
            .andExpect(jsonPath("$.data.timeline[3].stepType").value("AI_ANNOTATION"))
            .andExpect(jsonPath("$.data.timeline[3].annotation.type").value("rect"))
            .andExpect(jsonPath("$.data.timeline[3].annotation.style.strokeColor").value("#ff6b6b"));

        mockMvc.perform(post("/api/sessions/{sessionId}/messages/stream", sessionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content("""
                    {
                      "content": "请继续用流式方式提示下一步。",
                      "useStream": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                org.assertj.core.api.Assertions.assertThat(body).contains("event: user");
                org.assertj.core.api.Assertions.assertThat(body).contains("event: delta");
                org.assertj.core.api.Assertions.assertThat(body).contains("event: done");
            });
    }
}
