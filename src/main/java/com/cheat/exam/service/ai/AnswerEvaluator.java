package com.cheat.exam.service.ai;

import com.cheat.exam.config.AppProperties;
import com.cheat.exam.service.model.ModelClientRouter;
import com.cheat.exam.service.model.ModelClientSelection;
import com.cheat.exam.service.model.ModelChatRequest;
import com.cheat.exam.service.model.ModelChatResponse;
import com.cheat.exam.service.model.ModelClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 独立回答评估器：使用一次独立的模型调用来评估学生回答的正确性。
 *
 * 与引导老师 Prompt 分离，避免让同一次调用既判题又生成提示，
 * 降低模型"自己骗自己"的概率。
 *
 * 评估器返回 {@link AnswerEvaluation}，由 SessionService 注入到
 * 引导状态后再传给老师 Prompt 生成下一步提示。
 */
@Service
public class AnswerEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AnswerEvaluator.class);

    private static final Pattern STATUS_PATTERN = Pattern.compile(
        "\"status\"\\s*:\\s*\"(CORRECT|PARTIAL|WRONG|UNCLEAR)\"",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile(
        "\"confidence\"\\s*:\\s*(\\d+\\.?\\d*)"
    );
    private static final Pattern DIAGNOSIS_PATTERN = Pattern.compile(
        "\"diagnosis\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
    );
    private static final Pattern CORRECT_PART_PATTERN = Pattern.compile(
        "\"correctPart\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
    );
    private static final Pattern MISSING_PATTERN = Pattern.compile(
        "\"missingOrWrong\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
    );

    private final ModelClientRouter modelClientRouter;
    private final ObjectMapper objectMapper;

    public AnswerEvaluator(ModelClientRouter modelClientRouter, ObjectMapper objectMapper) {
        this.modelClientRouter = modelClientRouter;
        this.objectMapper = objectMapper;
    }

    /**
     * 评估学生回答。
     *
     * @param selection       模型路由信息
     * @param questionContext 题目描述/当前目标（老师侧上下文）
     * @param currentGoal     当前引导目标
     * @param studentAnswer   学生本轮回答
     * @param previousHints   之前老师给的提示摘要
     * @return 评估结果，如果评估失败返回 UNCLEAR + 低置信度
     */
    public AnswerEvaluation evaluate(
        ModelClientSelection selection,
        String questionContext,
        String currentGoal,
        String studentAnswer,
        String previousHints
    ) {
        try {
            ModelClient client = modelClientRouter.route(selection);
            String systemPrompt = buildEvaluatorSystemPrompt();
            String userPrompt = buildEvaluatorUserPrompt(questionContext, currentGoal, studentAnswer, previousHints);

            ModelChatRequest request = new ModelChatRequest(
                null, // sessionId not needed for evaluation
                selection.modelCode(),
                selection.providerCode(),
                null, // subjectCode
                null, // gradeLevel
                systemPrompt,
                userPrompt,
                null, // no image needed for evaluation
                List.of(),
                "evaluate", // evaluator uses evaluate mode to avoid JSON suppression
                false     // no streaming
            );

            ModelChatResponse response = client.chat(request);
            return parseEvaluation(response.replyText());
        } catch (Exception ex) {
            log.warn("Answer evaluation failed, returning UNCLEAR: {}", ex.getMessage());
            return new AnswerEvaluation(
                AnswerEvaluation.Status.UNCLEAR,
                0.0,
                "评估调用失败: " + ex.getMessage(),
                null,
                null
            );
        }
    }

    private String buildEvaluatorSystemPrompt() {
        return """
            你是一个专业的学生回答评估器。你的唯一任务是判断学生的回答是否正确。
            
            请严格按照以下 JSON 格式输出，不要输出任何其他内容：
            {
              "status": "CORRECT|PARTIAL|WRONG|UNCLEAR",
              "confidence": 0.0到1.0之间的数字,
              "diagnosis": "简短诊断说明",
              "correctPart": "学生回答中正确的部分，如果全错则为null",
              "missingOrWrong": "学生回答中错误或缺失的关键点，如果全对则为null"
            }
            
            判断标准：
            - CORRECT：学生回答完全正确或实质性正确（允许表述差异）
            - PARTIAL：学生回答部分正确，抓住了一些关键点但遗漏或错误了其他关键点
            - WRONG：学生回答的核心逻辑或结论错误
            - UNCLEAR：学生回答过于模糊、无法判断、或与当前问题无关
            
            置信度说明：
            - 0.9-1.0：非常确定
            - 0.7-0.9：比较确定
            - 0.5-0.7：有一定把握但不完全确定
            - 0.0-0.5：不确定，建议要求学生补充
            
            重要规则：
            - 不要编造或推测学生没有说的内容
            - 如果当前目标不够清晰导致无法判断，返回 UNCLEAR
            - 只输出 JSON，不要有任何前导或后缀文字
            """;
    }

    private String buildEvaluatorUserPrompt(
        String questionContext,
        String currentGoal,
        String studentAnswer,
        String previousHints
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("题目/上下文：").append(StringUtils.defaultIfBlank(questionContext, "未提供")).append("\n\n");
        sb.append("当前引导目标（老师期望学生回答的方向）：").append(StringUtils.defaultIfBlank(currentGoal, "未明确")).append("\n\n");
        if (StringUtils.isNotBlank(previousHints)) {
            sb.append("老师之前给的提示：").append(previousHints).append("\n\n");
        }
        sb.append("学生本轮回答：").append(StringUtils.defaultIfBlank(studentAnswer, "(空)")).append("\n\n");
        sb.append("请评估学生回答相对于「当前引导目标」的正确性。");
        return sb.toString();
    }

    AnswerEvaluation parseEvaluation(String modelOutput) {
        if (StringUtils.isBlank(modelOutput)) {
            return fallbackEvaluation("模型未返回内容");
        }

        // Try JSON parsing first
        String json = extractJson(modelOutput);
        if (json != null) {
            try {
                JsonNode node = objectMapper.readTree(json);
                return new AnswerEvaluation(
                    AnswerEvaluation.Status.fromString(node.path("status").asText(null)),
                    clampConfidence(node.path("confidence").asDouble(0.0)),
                    node.path("diagnosis").asText(null),
                    nullIfBlank(node.path("correctPart").asText(null)),
                    nullIfBlank(node.path("missingOrWrong").asText(null))
                );
            } catch (JsonProcessingException ex) {
                // Fall through to regex
            }
        }

        // Regex fallback
        AnswerEvaluation.Status status = extractStatus(modelOutput);
        double confidence = extractConfidence(modelOutput);
        String diagnosis = extractField(DIAGNOSIS_PATTERN, modelOutput);
        String correctPart = extractField(CORRECT_PART_PATTERN, modelOutput);
        String missingOrWrong = extractField(MISSING_PATTERN, modelOutput);

        if (status == AnswerEvaluation.Status.UNCLEAR && confidence == 0.0 && diagnosis == null) {
            return fallbackEvaluation("无法解析评估输出");
        }

        return new AnswerEvaluation(status, confidence, diagnosis, correctPart, missingOrWrong);
    }

    private AnswerEvaluation fallbackEvaluation(String reason) {
        return new AnswerEvaluation(
            AnswerEvaluation.Status.UNCLEAR,
            0.0,
            reason,
            null,
            null
        );
    }

    private String extractJson(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return null;
    }

    private AnswerEvaluation.Status extractStatus(String content) {
        Matcher m = STATUS_PATTERN.matcher(content);
        return m.find() ? AnswerEvaluation.Status.fromString(m.group(1)) : AnswerEvaluation.Status.UNCLEAR;
    }

    private double extractConfidence(String content) {
        Matcher m = CONFIDENCE_PATTERN.matcher(content);
        return m.find() ? clampConfidence(Double.parseDouble(m.group(1))) : 0.0;
    }

    private String extractField(Pattern pattern, String content) {
        Matcher m = pattern.matcher(content);
        return m.find() ? nullIfBlank(m.group(1)) : null;
    }

    private double clampConfidence(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String nullIfBlank(String value) {
        return StringUtils.isBlank(value) || "null".equalsIgnoreCase(value) ? null : value;
    }
}
