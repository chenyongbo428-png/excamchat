# 模型接入矩阵文档（初稿）

## 1. 文档目的

本文档用于定义“AI 改卷 / 试卷讲解 Web”项目第一版的模型供应商选型原则、接入优先级、能力对比、降级策略和开发注意事项，作为以下工作的依据：

- 后端模型接入层设计
- 前端模型选择器设计
- Prompt 编排与结构化输出适配
- 流式输出与画布标注联动
- 后续模型扩展与替换

配套文档：

- `docs/requirements.md`
- `docs/ai-prompt-spec.md`
- `docs/api-design.md`
- `docs/canvas-protocol.md`
- `docs/database-design.md`

---

## 2. 时效性说明

模型能力、模型命名、接口形态和可用特性变化很快。

本文件基于 **2026-06-22** 查阅的官方文档进行初步整理，适合作为当前项目的开发基线。
阿里云百炼 / 通义千问接入路径已于 **2026-07-05** 按官方文档与本地接口联调重新核验。

在以下动作前必须再次复核官方文档：

- 第一次真实接入某个供应商前
- 上线生产前
- 升级模型版本前
- 新增结构化输出或流式能力前

如果本文件的检查日期距离当前超过 30 天，建议先重新核验。

---

## 3. 本项目对模型的核心要求

本项目不是普通聊天应用，因此模型至少要尽量满足以下能力：

### 3.1 必需能力

- 支持图片输入
- 支持文本输出
- 支持较稳定的多轮对话
- 支持通过 Prompt 进行“引导型老师”角色约束

### 3.2 强烈建议能力

- 支持流式输出
- 支持结构化 JSON 输出
- 支持低温度下较稳定的格式遵循
- 支持较低延迟的首轮响应

### 3.3 最好具备的能力

- 支持工具调用或函数调用
- 支持较强的视觉理解
- 支持较长上下文
- 支持 Java 友好的 SDK 或稳定 REST API

---

## 4. 第一版选型原则

第一版建议优先采用“国际主流、官方能力清晰、视觉与结构化支持较成熟”的供应商，先把架构跑通，再考虑更多厂商。

### 4.1 优先模型供应商

- 阿里云百炼 / 通义千问（当前已验证 workspace OpenAI-compatible 接入）
- OpenAI
- Anthropic
- Google Gemini

### 4.2 暂不纳入第一版主实现的供应商

以下供应商可作为后续扩展候选，但本文件当前不对其能力做最终定论：

- DeepSeek
- 智谱
- 月之暗面
- 其他国内模型平台

说明：

- 如果后续要接入以上厂商，建议补一版对应官方能力矩阵，或直接扩充本文档
- 不建议在第一版就同时接入过多供应商，否则结构化输出和视觉差异会明显增加联调成本

---

## 5. 候选供应商能力矩阵

说明：

- 以下内容基于 **2026-06-22** 官方文档做了高层归纳
- “支持”不代表在本项目场景下一定足够稳定
- “推荐程度”是本项目视角，不是通用榜单

| 供应商 | 候选模型族 | 图片输入 | 流式输出 | 结构化输出 / JSON Schema | 多轮对话适配 | 本项目推荐度 | 备注 |
|---|---|---|---|---|---|---|---|
| OpenAI | GPT 系列（最新通用模型） | 支持 | 支持 | 支持，官方 Structured Outputs 能力明确 | 强 | 高 | 适合作为首个标准接入实现 |
| Anthropic | Claude 系列（支持 vision 的最新模型） | 支持 | 支持 SSE | 支持，但需关注模型与功能开关差异 | 强 | 高 | 文字引导风格通常较稳定 |
| Google Gemini | Gemini 系列（支持视觉的最新模型） | 支持 | 支持 | 支持 JSON Schema 结构化输出 | 强 | 高 | 多模态和文件输入路线清晰 |
| 阿里云百炼 / 通义千问 | Qwen / Qwen-VL 系列 | 支持 | 支持（后续接） | 支持 JSON mode；JSON Schema 能力需按模型复核 | 强 | 高 | 2026-07-05 已验证 `qwen-plus` 文本与 `qwen-vl-plus` 图片输入调用 HTTP 200 |

---

## 6. 逐家分析

### 6.1 OpenAI

适合作为：

- 第一优先接入供应商
- 统一抽象层的基准实现
- 结构化输出和图片输入的标准参考

适配判断：

- 官方文档明确说明最新模型支持图像输入
- Responses API 支持文本与图像输入
- 官方提供 Structured Outputs，可要求模型按 JSON Schema 输出
- 官方提供流式响应能力

对本项目的价值：

- 比较适合作为“AI 文本讲解 + 结构化标注输出”的首个落地实现
- 适合先把 `replyText + annotations` 的协议跑通
- 对后端统一抽象设计帮助很大

接入建议：

- 先做一个 OpenAI 适配器，作为 `ModelClient` 标准实现
- 先打通非流式 JSON 输出
- 再补流式回复和标注事件拆分

风险点：

- 模型族升级较快，命名和推荐模型可能变化
- 需要把业务协议固定到自定义 JSON Schema 上，而不是仅依赖自然语言 Prompt

### 6.2 Anthropic

适合作为：

- 第二优先接入供应商
- 侧重引导式讲解质量的对照模型

适配判断：

- 官方文档支持 vision
- 官方文档支持通过 SSE 进行流式输出
- 官方当前提供 Structured Outputs 能力，但不同模型和功能开关需额外注意

对本项目的价值：

- 在“老师式表达、分步引导、自然语言解释”方面适合作为对照接入
- 可用于比较不同供应商在“讲解感”上的差异

接入建议：

- 先实现非流式调用
- 结构化输出采用更保守的 schema
- 如果实际联调发现格式稳定性不足，先降级为“文本 + 轻量标注抽取”

风险点：

- 结构化输出能力需要关注模型支持范围和开关要求
- 某些高级能力与工具能力组合时，需要留意兼容性

### 6.3 Google Gemini

适合作为：

- 第三优先接入供应商
- 多模态与文件输入能力的对照模型

适配判断：

- 官方文档明确支持图片输入
- 支持将图片以内联数据或 File API 方式传入
- 官方文档支持 Structured Outputs，可按 JSON Schema 约束输出
- API 文档明确包含标准和流式交互能力

对本项目的价值：

- 适合题图输入场景
- 对未来扩展到 PDF、文档类题目有一定延展空间

接入建议：

- 第一版可以先按“图片 + 文本 + JSON 输出”模式接入
- 先不急着扩展到更复杂的实时或语音能力

风险点：

- 不同模型档位在速度、成本、结构化稳定性上会有差异
- 需要严格测试在当前 schema 下的输出一致性

### 6.4 阿里云百炼 / 通义千问

适合作为：

- 当前第一家真实视觉模型供应商
- 国内网络与成本更可控的 MVP 默认候选
- OpenAI-compatible 适配器路径的本地落地样板

适配判断：

- 官方文档提供 OpenAI 兼容的 Chat Completions 接口
- workspace 专属域名可使用 `/compatible-mode/v1/chat/completions`
- Qwen-VL 可通过 `image_url` 传入图片；本项目本地上传图片会在适配器中转成 data URL，避免公网访问依赖
- 官方兼容接口支持 `response_format: {"type":"json_object"}`，但仍需要后端做宽松 JSON 提取与纯文本兜底

当前联调结论：

- 2026-07-05 已验证 `qwen-plus` 文本调用 HTTP 200
- 2026-07-05 已验证 `qwen-vl-plus` 图片输入调用 HTTP 200，并可识别官方示例图片
- 当前后端已实现 `BailianModelClient`，供应商编码为 `BAILIAN`

接入要求：

- API Key 必须通过 `BAILIAN_API_KEY` 环境变量传入
- Base URL 必须通过 `BAILIAN_BASE_URL` 环境变量传入，例如 `https://{workspace}.cn-beijing.maas.aliyuncs.com`
- 不允许把真实 API Key 写入代码、配置文件或迁移脚本

---

## 7. 本项目推荐接入顺序

### 7.1 推荐顺序

1. 阿里云百炼 / 通义千问
2. OpenAI
3. Anthropic
4. Google Gemini

### 7.2 排序原因

阿里云百炼 / 通义千问放第一：

- 当前用户已提供并验证可用 workspace API
- 对国内访问、成本和联调稳定性更友好
- OpenAI-compatible 模式可复用统一模型抽象和多模态消息结构

OpenAI 放第二：

- 当前官方文档对图像输入、Responses API、Structured Outputs、流式输出的路径都较清晰
- 适合作为统一模型抽象的首个标准样板

Anthropic 放第二：

- 很适合做“引导型老师”体验对照
- 可验证不同供应商在讲解风格上的差异

Gemini 放第三：

- 多模态能力强，适合后续扩展
- 但第一版先保证基础主链路稳定更重要

---

## 8. 推荐的模型能力抽象

后端不应把供应商特性直接泄露到业务层，建议抽象为统一能力描述。

### 8.1 建议的能力字段

- `providerCode`
- `modelCode`
- `displayName`
- `supportsVision`
- `supportsStream`
- `supportsStructuredOutput`
- `supportsToolCalling`
- `recommendedForMvp`
- `status`

### 8.2 建议的 Java 抽象

```java
public interface ModelClient {
    ModelChatResponse chat(ModelChatRequest request);
    StreamingHandle streamChat(ModelChatRequest request);
    ModelCapabilities capabilities();
}
```

说明：

- 不同供应商都要实现同一抽象
- 业务层只依赖统一的 `ModelClient`

---

## 9. 本项目推荐的统一请求协议

无论接哪个供应商，业务层都建议先统一成内部请求对象：

```json
{
  "sessionId": "5001",
  "modelCode": "openai-default",
  "subjectCode": "MATH",
  "gradeLevel": "JUNIOR",
  "systemPrompt": "...",
  "messages": [
    {
      "role": "user",
      "content": "请讲解这道题"
    }
  ],
  "images": [
    {
      "imageId": "101",
      "url": "/api/images/101/content"
    }
  ],
  "outputMode": "JSON"
}
```

这样做的好处：

- 业务层不关心供应商具体接口字段
- 便于做 A/B 测试和模型切换
- 便于统一记录日志和异常

---

## 10. 本项目推荐的统一响应协议

建议所有供应商最终都转换为统一内部响应：

```json
{
  "success": true,
  "providerCode": "OPENAI",
  "modelCode": "openai-default",
  "replyText": "我们先看已知条件。",
  "guidanceStage": "observe",
  "hintLevel": 1,
  "annotations": [
    {
      "type": "rect",
      "x": 120,
      "y": 80,
      "width": 220,
      "height": 90,
      "color": "#ff6b6b",
      "label": "先看这个条件"
    }
  ],
  "rawPayload": {}
}
```

说明：

- `replyText`、`guidanceStage`、`hintLevel`、`annotations` 的业务语义以 `docs/ai-prompt-spec.md` 为准
- `annotations` 的渲染协议以 `docs/canvas-protocol.md` 为准

---

## 11. 推荐的降级策略

模型接入不要假设所有能力都永远稳定存在，必须设计降级路径。

### 11.1 结构化输出失败

降级顺序建议：

1. 优先尝试严格 JSON Schema 输出
2. 失败时尝试宽松 JSON 提取
3. 再失败时仅保留纯文本 `replyText`
4. 标注失败不应阻断文字讲解

### 11.2 视觉输入失败

降级建议：

- 给用户明确提示“当前模型图片解析失败，请重试或切换模型”
- 如果支持模型切换，前端可提示切换到另一家供应商

### 11.3 流式失败

降级建议：

- 从流式接口回退到普通请求接口
- 前端仍显示完整回答

### 11.4 超时或限流

降级建议：

- 同模型短重试
- 失败后切换备用模型
- 记录告警和失败原因

---

## 12. Prompt 与模型差异适配建议

虽然业务上统一叫“引导型老师”，但不同供应商对 Prompt 的遵循方式不同，建议做适配层。

### 12.1 OpenAI

建议：

- 优先配合 Structured Outputs 使用
- 用 schema 限定字段，比只靠文本提示更稳

### 12.2 Anthropic

建议：

- 对系统提示词写得更清楚、更直接
- 结构化输出 schema 保持尽量简洁
- 强化“不要首轮直接给最终答案”的规则

### 12.3 Gemini

建议：

- 明确区分自然语言回复与结构化字段
- 对标注字段做更严格的后端校验

---

## 13. 建议的模型评估维度

在真正选定默认模型前，建议针对同一批题图做离线评测。

### 13.1 评估维度

- 是否能正确理解题图内容
- 是否首轮直接给出完整答案
- 是否能保持“老师式引导”
- 结构化 JSON 是否稳定
- 标注坐标是否基本合理
- 流式输出体验是否顺畅
- 平均响应时长是否可接受
- 成本是否在可控范围

### 13.2 建议结果分级

- A：可直接作为默认模型
- B：可作为备选模型
- C：仅实验接入，不用于默认路径

---

## 14. 推荐的第一版配置策略

### 14.1 前端展示

第一版前端建议只展示 2 到 3 个模型选项，不要把所有可能供应商都直接暴露给用户。

建议：

- 默认模型：阿里云百炼 / 通义千问
- 备选模型：OpenAI
- 备选模型：Anthropic 或 Gemini

### 14.2 后端配置

建议在 `model_config` 中维护以下字段：

- `model_code`
- `display_name`
- `provider_code`
- `supports_vision`
- `supports_stream`
- `supports_structured_output`
- `enabled`
- `sort_order`
- `config_json`

说明：

- 表结构字段如未建全，可按 `docs/database-design.md` 补齐

---

## 15. 与 todo.md 的对应关系

以下阶段开发前，应先阅读本文件：

- Phase 0：需求收敛与技术方案确认
- Phase 4：AI 模型接入层
- Phase 5：Prompt 与对话编排
- Phase 8：AI 标注与画图联动
- Phase 10：管理与配置能力

开发要求：

- 模型相关代码评审时，需确认实现是否符合本文件中的能力假设和降级策略
- 如果新增供应商，需先更新本文件再开工

---

## 16. 官方参考链接

以下链接为本文件整理时参考的官方文档，检查日期为 **2026-06-22**：

- OpenAI Models: [developers.openai.com/api/docs/models](https://developers.openai.com/api/docs/models)
- OpenAI Images and Vision: [developers.openai.com/api/docs/guides/images-vision](https://developers.openai.com/api/docs/guides/images-vision)
- OpenAI Structured Outputs: [developers.openai.com/api/docs/guides/structured-outputs](https://developers.openai.com/api/docs/guides/structured-outputs)
- OpenAI Responses API: [developers.openai.com/api/reference/responses/overview](https://developers.openai.com/api/reference/responses/overview)
- OpenAI Streaming: [developers.openai.com/api/docs/guides/streaming-responses](https://developers.openai.com/api/docs/guides/streaming-responses)
- Anthropic Vision: [platform.claude.com/docs/en/build-with-claude/vision](https://platform.claude.com/docs/en/build-with-claude/vision)
- Anthropic Streaming: [platform.claude.com/docs/en/build-with-claude/streaming](https://platform.claude.com/docs/en/build-with-claude/streaming)
- Anthropic Structured Outputs / consistency guidance: [docs.anthropic.com/en/docs/test-and-evaluate/strengthen-guardrails/increase-consistency](https://docs.anthropic.com/en/docs/test-and-evaluate/strengthen-guardrails/increase-consistency)
- Anthropic Models list: [docs.anthropic.com/en/api/models-list](https://docs.anthropic.com/en/api/models-list)
- Gemini API docs: [ai.google.dev/gemini-api/docs](https://ai.google.dev/gemini-api/docs)
- Gemini Image Understanding: [ai.google.dev/gemini-api/docs/image-understanding](https://ai.google.dev/gemini-api/docs/image-understanding)
- Gemini Structured Outputs: [ai.google.dev/gemini-api/docs/structured-output](https://ai.google.dev/gemini-api/docs/structured-output)
- Gemini API reference: [ai.google.dev/api](https://ai.google.dev/api)
- 阿里云百炼 OpenAI 兼容 Chat Completions: [help.aliyun.com/zh/model-studio/qwen-api-via-openai-chat-completions](https://help.aliyun.com/zh/model-studio/qwen-api-via-openai-chat-completions)
- 阿里云百炼 Qwen-VL OpenAI 兼容: [help.aliyun.com/zh/model-studio/qwen-vl-compatible-with-openai](https://help.aliyun.com/zh/model-studio/qwen-vl-compatible-with-openai)

---

## 17. 后续建议

完成本文档后，建议下一步进入以下开发准备：

1. 在代码中定义 `ModelClient`、`ModelCapabilities`、`ModelChatRequest`、`ModelChatResponse`
2. 先用一个供应商跑通完整链路
3. 再接第二家供应商做统一抽象验证
4. 最后再开启前端模型切换功能
