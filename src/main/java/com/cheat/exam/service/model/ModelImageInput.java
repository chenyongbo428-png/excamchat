package com.cheat.exam.service.model;

/**
 * 传给模型的题图信息。
 *
 * 当前先使用站内图片访问路径，真实供应商适配器可以在内部转换为公网 URL、
 * base64，或供应商文件 ID。
 */
public record ModelImageInput(
    Long imageId,
    String accessUrl,
    String storageKey,
    Integer width,
    Integer height,
    String mimeType
) {
}
