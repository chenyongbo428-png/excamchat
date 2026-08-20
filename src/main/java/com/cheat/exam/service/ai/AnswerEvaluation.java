package com.cheat.exam.service.ai;

/**
 * 独立回答评估器的评估结果。
 *
 * 评估器不负责生成下一步提示，只负责判定学生回答的正确性。
 */
public record AnswerEvaluation(
    /** CORRECT, PARTIAL, WRONG, UNCLEAR */
    Status status,
    /** 0.0~1.0，评估器对自身判断的置信度 */
    double confidence,
    /** 简短诊断说明（内部使用，不直接展示给学生） */
    String diagnosis,
    /** 学生回答中正确的部分摘要，可能为 null */
    String correctPart,
    /** 学生回答中错误或缺失的关键点，可能为 null */
    String missingOrWrong
) {

    public enum Status {
        CORRECT,
        PARTIAL,
        WRONG,
        UNCLEAR;

        public static Status fromString(String value) {
            if (value == null) {
                return UNCLEAR;
            }
            return switch (value.trim().toUpperCase()) {
                case "CORRECT" -> CORRECT;
                case "PARTIAL" -> PARTIAL;
                case "WRONG" -> WRONG;
                default -> UNCLEAR;
            };
        }
    }
}
