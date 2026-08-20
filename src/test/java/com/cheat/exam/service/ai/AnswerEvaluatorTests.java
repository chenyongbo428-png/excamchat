package com.cheat.exam.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnswerEvaluatorTests {

    private AnswerEvaluator evaluator;

    @BeforeEach
    void setUp() {
        // Router not needed for parsing tests - only the ObjectMapper is needed
        evaluator = new AnswerEvaluator(null, new ObjectMapper());
    }

    @Test
    void parseValidJsonEvaluation() {
        String json = """
            {
              "status": "CORRECT",
              "confidence": 0.92,
              "diagnosis": "学生正确识别了方程的根",
              "correctPart": "x = 3 是正确的",
              "missingOrWrong": null
            }
            """;
        AnswerEvaluation result = evaluator.parseEvaluation(json);
        assertEquals(AnswerEvaluation.Status.CORRECT, result.status());
        assertEquals(0.92, result.confidence(), 0.001);
        assertEquals("学生正确识别了方程的根", result.diagnosis());
        assertEquals("x = 3 是正确的", result.correctPart());
        assertEquals(null, result.missingOrWrong());
    }

    @Test
    void parsePartialAnswer() {
        String json = """
            ```json
            {
              "status": "PARTIAL",
              "confidence": 0.78,
              "diagnosis": "学生找到了一个根但漏了另一个",
              "correctPart": "x = 2 是方程的一个根",
              "missingOrWrong": "还有一个根 x = -3 被遗漏了"
            }
            ```
            """;
        AnswerEvaluation result = evaluator.parseEvaluation(json);
        assertEquals(AnswerEvaluation.Status.PARTIAL, result.status());
        assertEquals(0.78, result.confidence(), 0.001);
        assertNotNull(result.correctPart());
        assertNotNull(result.missingOrWrong());
    }

    @Test
    void parseWrongAnswer() {
        String json = """
            {"status": "WRONG", "confidence": 0.95, "diagnosis": "计算错误", "correctPart": null, "missingOrWrong": "2+3不等于6"}
            """;
        AnswerEvaluation result = evaluator.parseEvaluation(json);
        assertEquals(AnswerEvaluation.Status.WRONG, result.status());
        assertEquals(0.95, result.confidence(), 0.001);
    }

    @Test
    void parseFallsBackToRegexWhenJsonInvalid() {
        String messy = """
            好的，我来评估一下
            "status": "PARTIAL"
            "confidence": 0.6
            "diagnosis": "部分正确"
            学生回答还可以
            """;
        AnswerEvaluation result = evaluator.parseEvaluation(messy);
        assertEquals(AnswerEvaluation.Status.PARTIAL, result.status());
        assertEquals(0.6, result.confidence(), 0.001);
    }

    @Test
    void parseEmptyInputReturnsUnclear() {
        AnswerEvaluation result = evaluator.parseEvaluation("");
        assertEquals(AnswerEvaluation.Status.UNCLEAR, result.status());
        assertEquals(0.0, result.confidence(), 0.001);
    }

    @Test
    void parseNullInputReturnsUnclear() {
        AnswerEvaluation result = evaluator.parseEvaluation(null);
        assertEquals(AnswerEvaluation.Status.UNCLEAR, result.status());
    }

    @Test
    void confidenceClampedTo0And1() {
        String json = """
            {"status": "CORRECT", "confidence": 1.5, "diagnosis": "test", "correctPart": null, "missingOrWrong": null}
            """;
        AnswerEvaluation result = evaluator.parseEvaluation(json);
        assertEquals(1.0, result.confidence(), 0.001);
    }

    @Test
    void statusFromStringHandlesVariants() {
        assertEquals(AnswerEvaluation.Status.CORRECT, AnswerEvaluation.Status.fromString("correct"));
        assertEquals(AnswerEvaluation.Status.CORRECT, AnswerEvaluation.Status.fromString("CORRECT"));
        assertEquals(AnswerEvaluation.Status.PARTIAL, AnswerEvaluation.Status.fromString(" Partial "));
        assertEquals(AnswerEvaluation.Status.WRONG, AnswerEvaluation.Status.fromString("WRONG"));
        assertEquals(AnswerEvaluation.Status.UNCLEAR, AnswerEvaluation.Status.fromString("something else"));
        assertEquals(AnswerEvaluation.Status.UNCLEAR, AnswerEvaluation.Status.fromString(null));
    }
}
