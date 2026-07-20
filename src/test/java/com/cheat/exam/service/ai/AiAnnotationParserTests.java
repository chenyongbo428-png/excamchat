package com.cheat.exam.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.cheat.exam.domain.image.ImageResource;
import com.cheat.exam.domain.session.ChatSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiAnnotationParserTests {

    private final AiAnnotationParser parser = new AiAnnotationParser();

    @Test
    @SuppressWarnings("unchecked")
    void normalizesAndClipsSupportedAnnotations() {
        ChatSession session = sessionWithImageSize(100, 80);
        Map<String, Object> rect = new LinkedHashMap<>();
        rect.put("id", "ann-1");
        rect.put("type", "rect");
        rect.put("x", -20);
        rect.put("y", 10);
        rect.put("width", 140);
        rect.put("height", 20);
        rect.put("color", "#123456");
        rect.put("label", "关键条件");

        Map<String, Object> unknown = new LinkedHashMap<>();
        unknown.put("type", "circle");
        unknown.put("x", 1);
        unknown.put("y", 1);

        Map<String, Object> blankText = new LinkedHashMap<>();
        blankText.put("type", "text");
        blankText.put("x", 1);
        blankText.put("y", 1);
        blankText.put("text", " ");

        List<Map<String, Object>> normalized = parser.normalizeAnnotations(
            session,
            42L,
            "guide_next_step",
            List.of(rect, unknown, blankText)
        );

        assertThat(normalized).hasSize(1);
        Map<String, Object> object = normalized.getFirst();
        assertThat(object)
            .containsEntry("objectId", "ai-42-0")
            .containsEntry("type", "rect")
            .containsEntry("source", "AI")
            .containsEntry("x", 0.0)
            .containsEntry("y", 10.0)
            .containsEntry("width", 100.0)
            .containsEntry("height", 20.0)
            .containsEntry("label", "关键条件");
        assertThat((Map<String, Object>) object.get("style"))
            .containsEntry("strokeColor", "#123456")
            .containsEntry("fillColor", "transparent");
        assertThat((Map<String, Object>) object.get("meta"))
            .containsEntry("messageId", 42L)
            .containsEntry("annotationId", "ann-1")
            .containsEntry("teacherIntent", "guide_next_step");
    }

    private ChatSession sessionWithImageSize(int width, int height) {
        ImageResource image = new ImageResource();
        image.setWidth(width);
        image.setHeight(height);
        ChatSession session = new ChatSession();
        session.setImage(image);
        return session;
    }
}
