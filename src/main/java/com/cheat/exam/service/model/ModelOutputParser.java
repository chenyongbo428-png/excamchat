package com.cheat.exam.service.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 统一解析模型输出中的"讲解文本 + 标注 JSON"混合格式。
 *
 * 模型在引导模式下输出格式约定为：
 * <pre>
 * &lt;面向学生的自然语言讲解&gt;
 *
 * ---ANNOTATIONS_JSON---
 * {"annotations":[{"type":"rect","x":120,"y":80,"width":200,"height":60,"label":"关键条件"}]}
 * </pre>
 *
 * 本解析器负责：
 * <ul>
 *   <li>按分隔符拆分出 replyText 与 annotations JSON</li>
 *   <li>解析 annotations 数组，失败时安全回退为空标注</li>
 *   <li>找不到分隔符时，整段内容作为 replyText，标注为空</li>
 * </ul>
 *
 * 流式场景下，{@link #ANNOTATION_SEPARATOR} 暴露为公共常量，
 * 供流式适配器检测并停止向前端推送标注元数据片段。
 */
@Component
public class ModelOutputParser {

    private static final Logger log = LoggerFactory.getLogger(ModelOutputParser.class);

    /** 讲解文本与标注 JSON 之间的分隔符。模型需原样输出此分隔符。 */
    public static final String ANNOTATION_SEPARATOR = "\n---ANNOTATIONS_JSON---\n";

    private final ObjectMapper objectMapper;

    public ModelOutputParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 判断累积文本是否已进入标注区（用于流式过滤）。
     * 一旦返回 true，后续 delta 不应再推送给前端。
     */
    public boolean enteredAnnotationSection(String accumulatedText) {
        return accumulatedText != null && accumulatedText.contains(ANNOTATION_SEPARATOR);
    }

    /**
     * 解析模型输出，拆分为 replyText 与 annotations。
     *
     * @param content 模型原始输出
     * @return 解析结果，content 为空时返回空文本 + 空标注
     */
    public ParseResult parse(String content) {
        if (StringUtils.isBlank(content)) {
            return new ParseResult("", List.of(), false);
        }

        int separatorIndex = content.indexOf(ANNOTATION_SEPARATOR);
        if (separatorIndex < 0) {
            // 没有分隔符：整段作为讲解文本，无标注
            return new ParseResult(content.trim(), List.of(), false);
        }

        String replyText = content.substring(0, separatorIndex).trim();
        String jsonPart = content.substring(separatorIndex + ANNOTATION_SEPARATOR.length()).trim();
        List<Map<String, Object>> annotations = extractAnnotations(jsonPart);

        if (StringUtils.isBlank(replyText)) {
            replyText = "我暂时没有生成有效讲解，请换个问法或稍后重试。";
        }
        return new ParseResult(replyText, annotations, true);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractAnnotations(String jsonPart) {
        if (StringUtils.isBlank(jsonPart)) {
            return List.of();
        }
        String json = extractJson(jsonPart);
        if (json == null) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode annotationsNode = node.path("annotations");
            if (!annotationsNode.isArray() || annotationsNode.isEmpty()) {
                return List.of();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : annotationsNode) {
                if (item.isObject()) {
                    result.add(objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}));
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to parse annotations JSON: {}", ex.getMessage());
            return List.of();
        }
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

    /**
     * 解析结果。
     *
     * @param replyText 面向学生的讲解文本（已去除标注 JSON）
     * @param annotations 标注列表（可能为空）
     * @param hasAnnotationSection 模型输出是否包含分隔符
     */
    public record ParseResult(String replyText, List<Map<String, Object>> annotations, boolean hasAnnotationSection) {
    }
}
