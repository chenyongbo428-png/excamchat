package com.cheat.exam.service.model;

/**
 * 模型路由时使用的最小模型配置快照。
 *
 * 这里不直接暴露 JPA Entity，避免模型适配层依赖数据库实体细节。
 */
public record ModelClientSelection(
    String modelCode,
    String providerCode,
    boolean supportsVision,
    boolean supportsStream,
    String configJson
) {
}
