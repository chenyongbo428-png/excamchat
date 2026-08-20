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
        unknown.put("type", "ellipse");
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
            .containsEntry("width", 140.0)
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

    @Test
    void normalizesLineAndCircleGeometricAids() {
        ChatSession session = sessionWithImageSize(200, 120);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("id", "line-1");
        line.put("type", "line");
        line.put("x", 10);
        line.put("y", 20);
        line.put("toX", 150);
        line.put("toY", 100);
        line.put("dashed", true);
        line.put("label", "辅助线");
        line.put("color", "#22c55e");

        Map<String, Object> circle = new LinkedHashMap<>();
        circle.put("id", "circle-1");
        circle.put("type", "circle");
        circle.put("x", 60);
        circle.put("y", 50);
        circle.put("radius", 30);
        circle.put("label", "关键点");

        List<Map<String, Object>> normalized = parser.normalizeAnnotations(
            session,
            43L,
            "guide_next_step",
            List.of(line, circle)
        );

        assertThat(normalized).hasSize(2);
        Map<String, Object> lineObject = normalized.get(0);
        assertThat(lineObject)
            .containsEntry("objectId", "ai-43-0")
            .containsEntry("type", "line")
            .containsEntry("x", 10.0)
            .containsEntry("y", 20.0)
            .containsEntry("toX", 150.0)
            .containsEntry("toY", 100.0)
            .containsEntry("dashed", true)
            .containsEntry("label", "辅助线");
        assertThat((Map<String, Object>) lineObject.get("style"))
            .containsEntry("strokeColor", "#22c55e")
            .containsEntry("fillColor", "transparent");

        Map<String, Object> circleObject = normalized.get(1);
        assertThat(circleObject)
            .containsEntry("objectId", "ai-43-1")
            .containsEntry("type", "circle")
            .containsEntry("x", 60.0)
            .containsEntry("y", 50.0)
            .containsEntry("radius", 30.0)
            .containsEntry("label", "关键点");
        assertThat((Map<String, Object>) circleObject.get("style"))
            .containsEntry("strokeColor", "#ff6b6b")
            .containsEntry("fillColor", "transparent");
    }

    @Test
    void clipsLineAndCircleToImageBounds() {
        ChatSession session = sessionWithImageSize(100, 100);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("type", "line");
        line.put("x", 200);
        line.put("y", -30);
        line.put("toX", 9999);
        line.put("toY", 9999);

        Map<String, Object> circle = new LinkedHashMap<>();
        circle.put("type", "circle");
        circle.put("x", 90);
        circle.put("y", 90);
        circle.put("radius", 50);

        List<Map<String, Object>> normalized = parser.normalizeAnnotations(
            session,
            44L,
            "guide_next_step",
            List.of(line, circle)
        );

        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0))
            .containsEntry("x", 200.0)
            .containsEntry("y", 0.0)
            .containsEntry("toX", 1000.0)
            .containsEntry("toY", 1000.0);
        assertThat(normalized.get(1))
            .containsEntry("x", 90.0)
            .containsEntry("y", 90.0)
            .containsEntry("radius", 50.0);
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
