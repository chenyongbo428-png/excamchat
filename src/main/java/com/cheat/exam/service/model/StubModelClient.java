package com.cheat.exam.service.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 开发期占位模型客户端。
 *
 * 作用：
 * 1. 在没有真实 API Key 时保持“上传题图 -> 建会话 -> 发消息 -> 标注画布”的完整闭环可用。
 * 2. 为真实模型适配器提供统一响应格式样例。
 * 3. 让测试环境不依赖外部网络和第三方模型服务。
 */
@Component
public class StubModelClient implements ModelClient {

    @Override
    public String providerCode() {
        return "STUB";
    }

    @Override
    public boolean supports(ModelClientSelection selection) {
        // 当前 stub 作为兜底客户端，可以响应任何已启用模型。
        return true;
    }

    @Override
    public ModelChatResponse chat(ModelChatRequest request) {
        String normalized = StringUtils.defaultString(request.currentUserMessage()).trim();
        int hintLevel = 1;
        String guidanceStage = "observe";
        String teacherIntent = "guide_next_step";
        String replyText = "我们先不急着求结果，先一起圈出题目里已经给出的条件，再想它们之间能建立什么关系。";
        boolean shouldRevealFinalAnswer = false;

        if ("direct".equalsIgnoreCase(StringUtils.trimToEmpty(request.guidanceMode()))) {
            hintLevel = 3;
            guidanceStage = "review";
            teacherIntent = "answer_question";
            shouldRevealFinalAnswer = true;
            replyText = "这是直答模式。当前使用开发期占位模型，已准备直接输出题目识别结果、正确答案和完整解题过程。";
        }

        if (!shouldRevealFinalAnswer && containsAny(normalized, "不会", "不懂", "看不懂", "没思路", "不知道")) {
            hintLevel = 2;
            guidanceStage = "analyze";
            replyText = "我们先只做一件事：把题目里明确写出来的已知条件列出来，然后判断哪个条件最适合当第一步。";
        } else if (!shouldRevealFinalAnswer && containsAny(normalized, "继续", "下一步", "然后")) {
            hintLevel = 2;
            guidanceStage = "solve_step";
            teacherIntent = "advance_step";
            replyText = "可以继续往前推一步，但先别直接跳到结论。你先试着写出下一行应该用到的公式或关系式。";
        } else if (!shouldRevealFinalAnswer && containsAny(normalized, "答案", "直接", "结果")) {
            teacherIntent = "withhold_final_answer";
            replyText = "我先不直接给最终答案，我们先抓住最关键的条件。只要把第一步想清楚，后面会顺很多。";
        }

        List<Map<String, Object>> annotations = buildStubAnnotations(request.image(), guidanceStage);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("replyText", replyText);
        payload.put("guidanceStage", guidanceStage);
        payload.put("hintLevel", hintLevel);
        payload.put("shouldRevealFinalAnswer", shouldRevealFinalAnswer);
        payload.put("annotations", annotations);
        payload.put("teacherIntent", teacherIntent);
        payload.put("providerCode", providerCode());
        payload.put("modelCode", request.modelCode());

        return new ModelChatResponse(
            providerCode(),
            request.modelCode(),
            replyText,
            hintLevel,
            guidanceStage,
            teacherIntent,
            shouldRevealFinalAnswer,
            annotations,
            payload,
            "stub-%d".formatted(System.currentTimeMillis())
        );
    }

    private List<Map<String, Object>> buildStubAnnotations(ModelImageInput image, String guidanceStage) {
        Integer width = image == null ? null : image.width();
        Integer height = image == null ? null : image.height();
        int anchorX = width != null && width > 0 ? Math.max(40, width / 10) : 120;
        int anchorY = height != null && height > 0 ? Math.max(40, height / 8) : 90;
        int boxWidth = width != null && width > 0 ? Math.max(120, width / 4) : 240;
        int boxHeight = height != null && height > 0 ? Math.max(60, height / 10) : 100;

        List<Map<String, Object>> annotations = new ArrayList<>();
        if ("solve_step".equals(guidanceStage)) {
            Map<String, Object> arrow = new LinkedHashMap<>();
            arrow.put("id", "stub-arrow-1");
            arrow.put("type", "arrow");
            arrow.put("x", anchorX + boxWidth);
            arrow.put("y", anchorY);
            arrow.put("toX", anchorX + (boxWidth / 2));
            arrow.put("toY", anchorY + (boxHeight / 2));
            arrow.put("color", "#1f6feb");
            arrow.put("label", "先从这里继续推");
            annotations.add(arrow);
            return annotations;
        }

        Map<String, Object> rect = new LinkedHashMap<>();
        rect.put("id", "stub-rect-1");
        rect.put("type", "rect");
        rect.put("x", anchorX);
        rect.put("y", anchorY);
        rect.put("width", boxWidth);
        rect.put("height", boxHeight);
        rect.put("color", "#ff6b6b");
        rect.put("label", "先看这部分已知条件");
        annotations.add(rect);
        return annotations;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
