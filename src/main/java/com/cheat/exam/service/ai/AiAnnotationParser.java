package com.cheat.exam.service.ai;

import com.cheat.exam.domain.session.ChatSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AiAnnotationParser {

    private static final String DEFAULT_STROKE_COLOR = "#ff6b6b";
    private static final String DEFAULT_ARROW_COLOR = "#1f6feb";
    private static final String DEFAULT_HIGHLIGHT_COLOR = "#fde047";
    private static final double DEFAULT_STROKE_WIDTH = 2;

    public List<Map<String, Object>> normalizeAnnotations(
        ChatSession session,
        Long messageId,
        String teacherIntent,
        List<Map<String, Object>> annotations
    ) {
        if (annotations == null || annotations.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (int index = 0; index < annotations.size(); index++) {
            Map<String, Object> annotation = annotations.get(index);
            normalizeAnnotation(session, messageId, teacherIntent, annotation, index)
                .ifPresent(normalized::add);
        }
        return normalized;
    }

    private java.util.Optional<Map<String, Object>> normalizeAnnotation(
        ChatSession session,
        Long messageId,
        String teacherIntent,
        Map<String, Object> annotation,
        int index
    ) {
        if (annotation == null) {
            return java.util.Optional.empty();
        }
        String type = StringUtils.defaultString(asString(annotation.get("type"))).toLowerCase(Locale.ROOT);
        if (!List.of("rect", "arrow", "text", "highlight").contains(type)) {
            return java.util.Optional.empty();
        }

        int imageWidth = safeDimension(session.getImage().getWidth());
        int imageHeight = safeDimension(session.getImage().getHeight());
        String annotationId = StringUtils.firstNonBlank(
            asString(annotation.get("annotationId")),
            asString(annotation.get("id")),
            asString(annotation.get("objectId"))
        );
        String objectId = StringUtils.firstNonBlank(
            asString(annotation.get("objectId")),
            messageId == null ? null : "ai-" + messageId + "-" + index,
            annotationId == null ? null : "ai-" + annotationId,
            "ai-temp-" + index
        );

        Map<String, Object> object = new LinkedHashMap<>();
        object.put("objectId", objectId);
        object.put("type", type);
        object.put("source", "AI");
        object.put("rotation", 0);

        if ("rect".equals(type) || "highlight".equals(type)) {
            if (!putBoxFields(object, annotation, imageWidth, imageHeight)) {
                return java.util.Optional.empty();
            }
            if (StringUtils.isNotBlank(asString(annotation.get("label")))) {
                object.put("label", asString(annotation.get("label")));
            }
        } else if ("arrow".equals(type)) {
            if (!putArrowFields(object, annotation, imageWidth, imageHeight)) {
                return java.util.Optional.empty();
            }
            if (StringUtils.isNotBlank(asString(annotation.get("label")))) {
                object.put("label", asString(annotation.get("label")));
            }
        } else if ("text".equals(type)) {
            if (!putTextFields(object, annotation, imageWidth, imageHeight)) {
                return java.util.Optional.empty();
            }
        }

        Map<String, Object> style = buildStyle(type, annotation);
        object.put("style", style);
        object.put("color", style.getOrDefault("strokeColor", style.getOrDefault("fillColor", DEFAULT_STROKE_COLOR)));
        object.put("meta", buildMeta(messageId, annotationId, teacherIntent));
        return java.util.Optional.of(object);
    }

    private boolean putBoxFields(Map<String, Object> object, Map<String, Object> annotation, int imageWidth, int imageHeight) {
        double x = clampCoordinate(asDouble(annotation.get("x")), imageWidth);
        double y = clampCoordinate(asDouble(annotation.get("y")), imageHeight);
        double width = asDouble(annotation.get("width"));
        double height = asDouble(annotation.get("height"));
        if (width <= 0 || height <= 0) {
            return false;
        }
        if (imageWidth > 0) {
            width = Math.min(width, Math.max(0, imageWidth - x));
        }
        if (imageHeight > 0) {
            height = Math.min(height, Math.max(0, imageHeight - y));
        }
        if (width <= 0 || height <= 0) {
            return false;
        }
        object.put("x", x);
        object.put("y", y);
        object.put("width", width);
        object.put("height", height);
        return true;
    }

    private boolean putArrowFields(Map<String, Object> object, Map<String, Object> annotation, int imageWidth, int imageHeight) {
        if (!annotation.containsKey("toX") || !annotation.containsKey("toY")) {
            return false;
        }
        object.put("x", clampCoordinate(asDouble(annotation.get("x")), imageWidth));
        object.put("y", clampCoordinate(asDouble(annotation.get("y")), imageHeight));
        object.put("toX", clampCoordinate(asDouble(annotation.get("toX")), imageWidth));
        object.put("toY", clampCoordinate(asDouble(annotation.get("toY")), imageHeight));
        return true;
    }

    private boolean putTextFields(Map<String, Object> object, Map<String, Object> annotation, int imageWidth, int imageHeight) {
        String text = StringUtils.firstNonBlank(asString(annotation.get("text")), asString(annotation.get("label")));
        if (StringUtils.isBlank(text)) {
            return false;
        }
        object.put("x", clampCoordinate(asDouble(annotation.get("x")), imageWidth));
        object.put("y", clampCoordinate(asDouble(annotation.get("y")), imageHeight));
        object.put("text", text);
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildStyle(String type, Map<String, Object> annotation) {
        Map<String, Object> incomingStyle = annotation.get("style") instanceof Map<?, ?> map
            ? (Map<String, Object>) map
            : Map.of();
        Map<String, Object> style = new LinkedHashMap<>();

        String color = StringUtils.firstNonBlank(
            asString(annotation.get("color")),
            asString(incomingStyle.get("strokeColor")),
            asString(incomingStyle.get("fillColor")),
            defaultColor(type)
        );
        double strokeWidth = positiveOrDefault(
            annotation.containsKey("strokeWidth") ? asDouble(annotation.get("strokeWidth")) : asDouble(incomingStyle.get("strokeWidth")),
            DEFAULT_STROKE_WIDTH
        );
        double opacity = clampOpacity(
            annotation.containsKey("opacity") ? asDouble(annotation.get("opacity")) : asDouble(incomingStyle.get("opacity")),
            "highlight".equals(type) ? 0.35 : 1
        );

        if ("highlight".equals(type)) {
            style.put("fillColor", color);
            style.put("opacity", Math.min(opacity, 0.6));
        } else if ("text".equals(type)) {
            style.put("fillColor", color);
            style.put("fontSize", positiveOrDefault(asDouble(annotation.get("fontSize")), 18));
            style.put("fontFamily", StringUtils.defaultIfBlank(asString(incomingStyle.get("fontFamily")), "sans-serif"));
            style.put("opacity", opacity);
        } else {
            style.put("strokeColor", color);
            style.put("fillColor", "rect".equals(type) ? "transparent" : color);
            style.put("strokeWidth", strokeWidth);
            style.put("opacity", opacity);
        }
        return style;
    }

    private Map<String, Object> buildMeta(Long messageId, String annotationId, String teacherIntent) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (messageId != null) {
            meta.put("messageId", messageId);
        }
        if (StringUtils.isNotBlank(annotationId)) {
            meta.put("annotationId", annotationId);
        }
        if (StringUtils.isNotBlank(teacherIntent)) {
            meta.put("teacherIntent", teacherIntent);
        }
        return meta;
    }

    private String defaultColor(String type) {
        if ("arrow".equals(type) || "text".equals(type)) {
            return DEFAULT_ARROW_COLOR;
        }
        if ("highlight".equals(type)) {
            return DEFAULT_HIGHLIGHT_COLOR;
        }
        return DEFAULT_STROKE_COLOR;
    }

    private int safeDimension(Integer value) {
        return value == null || value < 1 ? 0 : value;
    }

    private double clampCoordinate(double value, int max) {
        double safeValue = Math.max(0, value);
        if (max > 0) {
            return Math.min(safeValue, max);
        }
        return safeValue;
    }

    private double clampOpacity(double value, double defaultValue) {
        double safeValue = value <= 0 ? defaultValue : value;
        return Math.max(0, Math.min(1, safeValue));
    }

    private double positiveOrDefault(double value, double defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
