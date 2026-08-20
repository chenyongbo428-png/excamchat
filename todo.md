# AI 改卷 / 试卷讲解 Web 项目任务拆解

## 1. 项目目标

构建一个基于 Java 21、Spring Boot 4、Maven 的 Web 应用，支持：

- 用户上传题目图片
- 将题目图片发送给可选的大模型进行讲解
- 大模型以"引导型老师"身份回答，不直接一次性给出完整答案
- Web 端提供画布能力，可在题目图上标注、绘图、辅助讲解
- 根据大模型返回结果，在前端对题目图片进行高亮、标注、画图
- 提供登录、会话历史、对话重放等基础能力

---

## 2. 建议开发阶段

### Phase 0 - 需求收敛与技术方案确认

开发与评审需优先遵循：

- [ ] `docs/requirements.md`
- [ ] `docs/ai-prompt-spec.md`
- [ ] `docs/database-design.md`
- [ ] `docs/api-design.md`
- [ ] `docs/canvas-protocol.md`
- [ ] `docs/model-provider-matrix.md`

- [ ] 明确产品角色
  - [ ] 定义用户类型：学生、老师、管理员
  - [ ] 明确当前第一版是否只做学生端
- [ ] 明确 AI 交互边界
  - [ ] 定义"引导型老师"提示词规范
  - [ ] 约束模型不能直接输出完整答案的场景和例外场景
  - [ ] 明确模型输出是否需要结构化格式
- [ ] 明确题目类型范围
  - [ ] 只支持拍照题目，还是也支持 PDF/整页试卷
  - [ ] 是否支持数学公式、几何图、英语阅读、作文等题型
- [ ] 明确前端画布交互范围
  - [ ] 支持自由画笔、矩形、圆形、箭头、文字、橡皮擦
  - [ ] 支持撤销/重做
  - [ ] 支持缩放/拖拽/适配移动端
- [ ] 明确对话回放能力
  - [ ] 回放消息内容
  - [ ] 回放图片和画布操作
  - [ ] 回放 AI 标注步骤
- [ ] 明确存储策略
  - [ ] 图片文件落本地、对象存储还是云存储
  - [ ] 画布操作存快照还是存操作日志
- [ ] 明确模型接入范围
  - [ ] 第一版接哪些模型供应商
  - [ ] 每个模型是否支持图像输入
  - [ ] 是否需要统一模型适配层

交付物：

- [ ] `docs/requirements.md`
- [ ] `docs/ai-prompt-spec.md`
- [ ] `docs/model-provider-matrix.md`
- [ ] `docs/api-design.md`
- [ ] `docs/database-design.md`
- [ ] `docs/canvas-protocol.md`

---

### Phase 1 - 项目基础脚手架

实现时需遵循：

- [ ] 接口定义遵循 `docs/api-design.md`
- [ ] 数据实体遵循 `docs/database-design.md`

- [x] 初始化 Maven 单模块结构
- [x] 确定基础包结构
  - [x] `web`
  - [x] `service`
  - [x] `domain`
  - [x] `repository`
  - [x] `config`
  - [ ] `integration`
- [x] 集成基础依赖
  - [x] Spring Web
  - [x] Spring Validation
  - [x] Spring Security
  - [x] Spring Data JPA
  - [ ] Lombok（当前未接入）
  - [ ] OpenAPI / Swagger
  - [x] 文件上传支持
- [ ] 配置多环境
  - [x] 当前先落基础 `application.yml`
  - [x] 服务端口已外部化为 `SERVER_PORT`，默认 `8080`；本地私有端口可放入被 `.gitignore` 忽略的 `config/application.yml`
  - [ ] `dev`
  - [ ] `test`
  - [ ] `prod`
- [x] 建立统一返回体和异常处理
- [ ] 建立基础日志、traceId、请求日志规范
- [x] 建立数据库迁移方案
  - [x] Flyway
- [ ] 建立代码规范与提交规范

交付物：

- [x] 可启动的 Spring Boot 工程
- [ ] 基础健康检查接口
- [ ] OpenAPI 文档页

当前实现记录：

- [x] 已使用 Spring Initializr 生成 Spring Boot 4.1.0 + Maven Wrapper 工程
- [x] 当前本地构建命令使用 `mvnw.cmd`
- [x] 当前默认运行数据库已切换为 MySQL，连接配置见 `src/main/resources/application.yml`
- [x] 当前测试环境单独使用 H2 内存库，避免构建依赖本地 MySQL 实例
- [x] 当前默认文件存储目录为 `storage/uploads`
- [x] 当前 Maven 已接入 `mysql-connector-j`，测试依赖保留 H2
- [x] 当前 Maven 已接入 `flyway-mysql`，MySQL 8.0.36 环境下可正常执行 Flyway 迁移
- [x] 2026-07-06 已将 `server.port` 调整为 `${SERVER_PORT:8080}`，遵循 Spring Boot 外部化配置规范；后续部署或本地联调改端口优先使用环境变量或忽略的 `config/application.yml`，不要写死到代码中

---

### Phase 2 - 账户与权限系统

实现时需遵循：

- [ ] 认证接口遵循 `docs/api-design.md`
- [ ] 用户表与鉴权相关字段遵循 `docs/database-design.md`

- [x] 用户表设计
  - [x] 用户 ID
  - [x] 账号
  - [x] 密码摘要
  - [x] 昵称
  - [x] 头像
  - [x] 状态
  - [x] 创建时间/更新时间
- [x] 登录注册功能
  - [x] 用户注册
  - [x] 用户登录
  - [x] 退出登录
  - [ ] Token 刷新
- [ ] 权限模型设计
  - [x] 普通用户
  - [x] 管理员
- [x] Spring Security 鉴权接入
- [x] 密码加密与安全策略
- [ ] 登录态失效、封禁、异常处理
- [x] 用户基础信息接口

交付物：

- [x] 登录/注册接口
- [x] 鉴权中间件
- [x] 用户信息接口

当前实现记录：

- [x] 当前采用简易 Bearer Token 方案，适合作为 MVP，后续可替换为 JWT 或更完整会话方案
- [x] 当前已开放接口：`/api/auth/register`、`/api/auth/login`、`/api/auth/logout`、`/api/auth/me`
- [x] 已在本地 MySQL 8.0.36 环境下验证注册、登录、`/api/auth/me` 正常

---

### Phase 3 - 题目图片上传与资源管理

实现时需遵循：

- [ ] 图片资源对象与字段遵循 `docs/database-design.md`
- [ ] 上传接口和返回结构遵循 `docs/api-design.md`

- [x] 文件上传模块
  - [x] 单图上传
  - [x] 图片大小校验
  - [x] 图片格式校验
  - [ ] 恶意文件校验
- [x] 资源存储设计
  - [x] 原图存储
  - [ ] 缩略图生成
  - [x] 访问 URL 设计
- [x] 图片元数据表设计
  - [x] 文件 ID
  - [x] 用户 ID
  - [x] 文件名
  - [x] MIME 类型
  - [x] 宽高
  - [x] 大小
  - [x] 存储路径
- [ ] 题目图片与会话绑定关系设计
- [ ] 图片预处理
  - [ ] 自动旋转纠正
  - [ ] 压缩
  - [ ] 清晰度检查
- [ ] 为后续 OCR / 模型识别预留扩展点

交付物：

- [x] 图片上传接口
- [x] 图片访问接口
- [x] 资源元数据表

当前实现记录：

- [x] 当前已开放接口：`POST /api/images/upload`、`GET /api/images/{id}`、`GET /api/images/{id}/content`
- [x] 当前支持 `image/jpeg`、`image/png`、`image/webp`
- [x] 当前删除接口已占位，但尚未真正实现逻辑删除
- [x] 已在本地 MySQL 8.0.36 环境下验证图片上传成功

---

### Phase 4 - AI 模型接入层

实现时需遵循：

- [ ] 模型接入范围与差异说明遵循 `docs/model-provider-matrix.md`
- [ ] AI 输入输出约束遵循 `docs/ai-prompt-spec.md`
- [ ] 调用接口与响应包装遵循 `docs/api-design.md`
- [ ] 接入前先复核 `docs/model-provider-matrix.md` 的"检查日期"和官方链接，避免模型能力过期

- [x] 定义统一模型接口
  - [x] 文本输入
  - [x] 图片输入
  - [x] 流式输出
  - [x] 错误码映射
- [x] 设计模型配置中心
  - [x] 模型名称
  - [x] 供应商
  - [x] 是否支持视觉
  - [ ] 超时时间
  - [ ] 最大 token
  - [ ] 温度参数
- [ ] 接入第一批模型供应商
  - [ ] 优先接入 OpenAI
  - [ ] 优先接入 Anthropic
  - [ ] 优先接入 Google Gemini
  - [x] 国内供应商优先接入阿里云百炼 / 通义千问，并已更新 `docs/model-provider-matrix.md`
- [ ] 实现模型切换能力
  - [ ] 前端可选模型
  - [x] 后端按模型路由
- [ ] 统一封装请求日志
  - [ ] 请求参数摘要
  - [ ] 响应摘要
  - [ ] 耗时
  - [ ] 失败原因
- [ ] 增加限流与重试机制
- [ ] 设计模型健康状态检查

交付物：

- [x] `ModelClient` 抽象
- [ ] 至少 2 个模型实现
- [x] 模型切换接口
- [ ] 模型能力开关与降级策略遵循 `docs/model-provider-matrix.md`

当前实现记录：

- [x] 已落 `model_config` 表与初始化种子数据
- [x] 当前已开放接口：`GET /api/models/enabled`
- [x] 已新增统一模型抽象：`ModelClient`、`ModelChatRequest`、`ModelChatResponse`、`ModelClientSelection`、`ModelImageInput`、`ModelMessageInput`
- [x] 已新增 `ModelClientRouter`，业务层可按 `model_config.provider_code` 路由到对应模型客户端；当前无真实供应商客户端时回退到 `StubModelClient`
- [x] 已将原 `SessionService` 内部 stub 回复迁移到 `StubModelClient`，消息发送链路统一走 `ModelClient` 抽象
- [x] 已在 `ModelChatRequest` 中携带会话 ID、模型编码、供应商编码、学科、年级、系统提示词、当前用户问题、历史消息和题图信息
- [x] 当前仍未开始真实模型 SDK / HTTP 接入，下一步应优先实现第一家视觉模型适配器
- [x] 已在本地 MySQL 8.0.36 环境下验证模型列表查询成功
- [x] 已通过 `mvnw.cmd test` 覆盖模型路由、会话消息、AI 标注解析、画布保存和回放链路
- [x] 2026-07-05 已验证阿里云百炼 / 通义千问 workspace 专属域名可用：OpenAI-compatible 路径为 `https://ws-rjhou3pw5punr881.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/chat/completions`
- [x] 2026-07-05 已验证 `qwen-plus` 文本调用返回 HTTP 200，`qwen-vl-plus` 图片输入调用返回 HTTP 200，并能识别官方示例图片；后续实现真实适配器时需将 API Key 通过环境变量外部化，严禁写入代码仓库
- [x] 已实现 `BailianModelClient`，通过 OpenAI-compatible `/chat/completions` 调用阿里云百炼 / Qwen 模型
- [x] 已新增 `BAILIAN_API_KEY`、`BAILIAN_BASE_URL`、`BAILIAN_TIMEOUT_SECONDS`、`BAILIAN_MAX_TOKENS`、`BAILIAN_TEMPERATURE` 外部化配置；真实密钥不得写入仓库
- [x] 已将本地上传题图在适配器内转为 data URL 传给 Qwen-VL，避免外部模型无法访问本地 `/api/images/{id}/content`
- [x] 已新增 Flyway 迁移 `V4__add_bailian_qwen_model.sql`，写入 `qwen-vl-plus` / `BAILIAN` 模型配置
- [x] 已新增 `BailianModelClientTests`，覆盖供应商支持判断、配置缺失错误、图片 data URL 请求和结构化响应解析
- [x] 已通过 `mvnw.cmd test`，当前 7 个测试全部通过
- [x] 已将正式百炼 API 配置写入本地外部配置 `config/application.yml`，并通过 `.gitignore` 忽略 `config/application*.yml`，避免密钥进入仓库
- [x] 已修复 Qwen-VL 真实联调中暴露的问题：兼容接口使用 `max_tokens` 限制输出，响应按 UTF-8 读取，JSON 解析失败时抽取首个 `replyText` 兜底，不再因模型输出截断返回 502
- [x] 2026-07-06 已用真实 `BAILIAN_API_KEY` / `BAILIAN_BASE_URL` 启动应用并完成端到端联调：注册、登录、模型列表、图片上传、创建 `qwen-vl-plus` 会话、发送消息、保存助手回复、读取画布均成功
- [x] 已通过 `mvnw.cmd test`，当前 8 个测试全部通过
- [x] 2026-07-06 根据真实效果调整为"直答识图模式"：百炼 / Qwen 暂不强制 JSON、不要求引导、不生成标注，直接展示模型原文答案，用于优先验证图片识别和解题正确性
- [x] 2026-07-06 真实验证简单题图 `2 + 3 = ?`，`qwen-vl-plus` 成功识别题目并返回正确答案 `5` 与简短解题过程
- [x] 已通过 `mvnw.cmd test`，当前 9 个测试全部通过
- [x] 2026-07-06 排查到"仍返回引导式固定话术"的原因：前端默认选中了仍启用的占位模型 `openai-default` 等，后端路由找不到真实客户端后兜底到了 `StubModelClient`
- [x] 已新增 Flyway 迁移 `V5__prefer_qwen_disable_placeholder_models.sql`，禁用未真实接入的 `openai-default`、`anthropic-default`、`gemini-default`，并将 `qwen-vl-plus` 设为启用且排序第一
- [x] 已调整 `ModelClientRouter`：除非模型供应商显式为 `STUB`，否则不允许静默兜底到 `StubModelClient`；以后未接入模型会直接报 `MODEL_NOT_AVAILABLE`
- [x] 已通过 `mvnw.cmd test`，当前 9 个测试全部通过
- [x] 2026-07-06 已新增模型流式输出抽象：`ModelClient.stream(...)`；百炼 / Qwen 适配器已通过 OpenAI-compatible `stream=true` 读取 SSE delta，并在不支持真实流式的模型上保留分段降级输出
- [x] 已通过 `mvnw.cmd test`，当前 9 个测试全部通过
- [x] 2026-07-06 修复前端请求流式接口失败时只显示"流式响应建立失败"的问题：现在会读取真实 HTTP 错误，并在 `/messages/stream` 不可用时自动降级到原 `/messages` 非流式接口，避免旧后端进程未重启时无法提问
- [x] 已通过 `mvnw.cmd test` 和 Node `--check src/main/resources/static/app.js`
- [x] 2026-07-06 已优化前端流式展示：发送后立即显示用户消息与助手草稿，SSE `delta` 到达时逐帧刷新草稿内容，`done` 后替换为持久化消息；流式连接失败会清理草稿并降级或提示，避免界面卡在半生成状态
- [x] 已通过 `mvnw.cmd test` 和 Codex bundled Node `--check src/main/resources/static/app.js`
- [x] 2026-07-06 使用浏览器插件排查"前端看起来不是流式"：根因是流式接口手动序列化 `Instant` 失败，前端收到非 2xx 后自动降级到普通 `/messages`，导致答案一次性出现
- [x] 2026-07-06 已修复流式链路：`POST /api/sessions/{id}/messages/stream` 改为同步写 `HttpServletResponse` SSE，补齐 `X-Accel-Buffering: no`，`ObjectMapper` 显式注册 `JavaTimeModule` 并禁用时间戳日期输出
- [x] 2026-07-06 已通过 `curl -N` 验证原始响应为 `200 text/event-stream` 且持续输出多个 `event: delta`；已通过浏览器插件验证前端助手消息长度从 91、116、147 逐步增长到完成状态
- [x] 2026-07-07 已排查"长答案只显示一半"：根因不是前端 CSS，而是百炼/Qwen 非结构化兜底解析把用户可见 `replyText` 截断到 600 字符，且本地 `max_tokens` 仅 500；已移除用户可见答案截断，并将默认/本地输出上限提高到 3000 tokens
- [x] 2026-07-07 已新增长文本回归测试，确保模型返回超过 600 字符的纯文本答案时，最终持久化助手消息不会再被替换成半截内容
- [x] 2026-07-09 已将百炼/Qwen 从临时"直答识图模式"切换回"引导式讲解模式"，同时保留 3000 tokens 输出预算和流式输出能力
- [x] 2026-07-21 已新增火山方舟 / 豆包 `ArkModelClient`，按 OpenAI-compatible `/api/v3/chat/completions` 接入 `doubao-seed-2-1-turbo-260628` 视觉模型，支持图片 data URL 和普通回复；SSE 流式解析代码已保留，但模型配置默认禁用流式直到真实联调确认
- [x] 2026-07-21 已新增 `ARK_API_KEY`、`ARK_BASE_URL`、`ARK_TIMEOUT_SECONDS`、`ARK_MAX_TOKENS`、`ARK_TEMPERATURE` 外部化配置；真实密钥不得写入仓库
- [x] 2026-07-21 已新增 Flyway 迁移 `V7__add_ark_doubao_model.sql`，写入 `doubao-seed-2-1-turbo-260628` / `ARK` 模型配置
- [x] 2026-07-21 已修复豆包调用失败时错误信息过少的问题：Ark 非 2xx 响应会透传供应商错误摘要；前端会根据模型 `supportsStream` 决定是否直接走普通消息接口
- [x] 2026-07-21 已新增 Flyway 迁移 `V8__disable_ark_stream_until_verified.sql`，先关闭豆包流式能力开关，避免未确认 Ark SSE 行为前前端误走流式链路
- [x] 2026-07-21 已新增 Flyway 迁移 `V9__update_ark_doubao_endpoint_model.sql`，将豆包模型编码切换为火山方舟 Endpoint ID `ep-20260721164323-qjbgk`
- [x] 2026-07-21 已在前端模型选择处补充提示：模型选择只对新建会话生效，旧会话继续使用创建时绑定的模型
- [x] 2026-07-21 已新增 Flyway 迁移 `V10__enable_qwen_stream_support.sql`，修复前端按 `supportsStream` 决策后 Qwen 被误判为非流式的问题
- [x] 2026-07-21 已验证豆包 Ark Endpoint `ep-20260721164323-qjbgk` 支持 `text/event-stream`，新增 `V11__enable_ark_stream_support.sql` 开启豆包流式；Ark 流式解析兼容 `delta.content` 和 `delta.reasoning_content`
- [x] 2026-07-21 已给 `ArkModelClient` 增加安全调用日志：记录模型、会话、模式、是否带图、HTTP 状态、响应长度、providerRequestId 和错误摘要，不输出 API Key 或图片 base64
- [x] 2026-07-21 已给 `SessionService` 消息发送链路增加日志：记录普通/流式入口、模型返回、回复长度和消息落库 ID，用于排查"模型返回但前端不显示"问题
- [x] 2026-07-21 已新增 `ArkModelClientTests`，覆盖 Ark 配置缺失、图片 data URL 普通请求，以及 `delta.reasoning_content` / `delta.content` 流式增量解析
- [x] 2026-07-21 已新增豆包 `reasoning_effort` 可配置能力：通过 `ARK_REASONING_EFFORT` 外部化配置传入，支持 `minimal/low/medium/high`，默认 `medium`
- [x] 2026-08-05 已用真实题图验证豆包 Seed 2.1 Turbo / ARK 在引导模式下的表现（详见下方验证记录）
- [x] 2026-08-05 已修复 ARK 默认超时 60s→180s、默认 reasoning_effort medium→low（推理模型+图片场景 60s 不够）
- [x] 2026-08-05 已修复 AnswerEvaluator 与模型适配器的 JSON 输出冲突：新增 `evaluate` 模式透传，避免 `direct`/`guided` 模式的"不要输出 JSON"指令覆盖评估器的 JSON 需求
- [x] 2026-08-05 已修复数据库 utf8 编码不支持 emoji 问题：新增 V12 迁移将所有表转为 utf8mb4，JDBC 连接改为 `characterEncoding=UTF-8`

**2026-08-05 ARK 豆包引导模式真实验证记录**

验证环境：本地 MySQL + Spring Boot + curl HTTP 驱动，模型 `ep-20260721164323-qjbgk`（豆包 Seed 2.1 Turbo 视觉），reasoning_effort=low，timeout=180s

| 题图 | 难度 | 直答模式 | 引导 T1 | 引导 T2 | 引导 T3 | 看答案 |
| --- | --- | --- | --- | --- | --- | --- |
| cz1.png | 初中几何 | ✅ 完整5步证明 | ✅ 提问"哪两个角相等" | ✅ 评估PARTIAL(0.7)+推进 | ✅ 评估CORRECT(1.0)+推进 | ✅ 完整证明 |
| cz2.png | 初中进阶 | ✅ 完整8步证明 | ✅ 提问"平行四边形对边关系" | — | — | — |
| gz1.png | 高中立体几何 | ✅ 答案ACD+4选项验证 | ✅ 提问"设AB=1求长宽高" | — | — | — |

结论：
- 引导模式首轮均能正确识别题目、提出具体引导问题，不直接给完整答案 ✅
- AnswerEvaluator 在多轮对话中正确返回 PARTIAL/CORRECT 评估结果，模型据此推进 ✅
- 直答模式与引导模式行为有明显差异 ✅
- 豆包 Seed 2.1 Turbo 视觉模型可稳定用于引导式讲解场景 ✅

---

### Phase 5 - 引导型老师 Prompt 与对话编排

实现时需严格遵循：

- [ ] `docs/ai-prompt-spec.md`
- [ ] `docs/api-design.md`
- [ ] 各模型的结构化输出实现差异遵循 `docs/model-provider-matrix.md`

- [ ] 设计系统提示词
  - [x] 引导式提问
  - [x] 分步骤启发
  - [x] 避免直接给终答案
  - [x] 根据年级/学科调整语气
- [ ] 设计消息结构
  - [x] 系统消息
  - [x] 用户消息
  - [ ] 图片消息
  - [ ] 助手消息
- [x] 设计 AI 输出格式
  - [x] 纯文本讲解
  - [x] 结构化提示点（2026-08-06 已启用"文本 + 分隔符 + 标注 JSON"输出格式，`ModelOutputParser` 统一解析）
  - [x] 画布标注指令（引导模式 Prompt 要求输出 rect/arrow/text/highlight 标注 JSON）
- [x] 定义结构化协议
  - [x] 高亮区域（highlight）
  - [x] 矩形框（rect）
  - [x] 箭头（arrow）
  - [x] 文本说明（text）
  - [ ] 几何辅助线（待实现）
- [x] 设计防越权策略
  - [x] 禁止输出不合适内容（`buildSafetyGuardPrompt` 第 1/5 条）
  - [x] 禁止提示泄露（`buildSafetyGuardPrompt` 第 2/6 条，含注入指令防御）
  - [x] 禁止直接整题抄答案（`buildSafetyGuardPrompt` 第 4 条）
- [ ] 设计多轮对话状态管理
  - [x] 追问（数学约束第 5 条：跳步时追问中间步骤）
  - [x] 用户回答后的二次引导（AnswerEvaluator 评估后注入 Prompt，CORRECT→推进/PARTIAL→补缺/WRONG→纠错/UNCLEAR→要求重述）
  - [x] 用户要求"直接答案"时的兜底策略（buildHintLimitPrompt + isRevealRequest，学生说"看答案"触发完整证明）
  - [x] 持久化当前阶段、当前目标、提示次数和连续未推进次数
  - [x] 无法可靠判断时停止编造提示
  - [x] 独立回答评估器：`CORRECT` / `PARTIAL` / `WRONG` / `UNCLEAR`
  - [x] 数学题步骤验证与解题路径约束
  - [x] 提示次数达到上限后的"查看下一步/查看答案"交互

交付物：

- [x] Prompt 模板
- [ ] AI 输出 JSON 协议文档
- [x] 对话编排服务

当前实现记录：

- [x] 已在 `SessionService` 中加入基础系统提示词编排，包含引导型老师、分步讲解、不直接给完整答案、学科和年级上下文
- [x] 已通过 `ModelChatRequest.messages` 向模型客户端传递会话历史消息
- [x] 当前 Prompt 仍是代码内基础模板，后续可升级为 `prompt_template` 表或配置化模板
- [x] 2026-07-06 百炼 / Qwen 适配器已临时切换为直答识图 Prompt，不再套用引导型老师和 JSON 标注约束；目的是先验证视觉识别与题解能力
- [x] 2026-07-09 已恢复并落地"引导式讲解模式"：`SessionService` 负责生成引导型老师业务 Prompt，`BailianModelClient` 不再覆盖为直答 Prompt，而是要求首轮识别题意/关键条件/提出一个小问题，多轮时先判断学生回答再给下一步提示
- [x] 2026-07-09 已将百炼/Qwen 纯文本兜底元数据调整为 `guidanceStage=observe`、`teacherIntent=guide_next_step`、`shouldRevealFinalAnswer=false`，避免非 JSON 输出被误标记为直答
- [x] 2026-07-09 已通过 `BailianModelClientTests` 校验真实模型请求体包含"引导式讲解模式""不要在第一轮直接给最终答案""先判断学生"等关键约束，并通过 `mvnw.cmd test`
- [x] 2026-07-20 已在前端工作台增加"引导模式 / 直答模式"切换，选择保存在浏览器本地，并通过普通/流式消息请求的 `mode` 字段传到后端 Prompt 编排
- [x] 2026-07-20 已约定 `mode=guided` 为默认值、`mode=direct` 为直答模式；后端未传值时自动按引导模式处理，并更新 `docs/api-design.md`
- [x] 2026-07-20 已为会话增加 `guidance_state_json`，持久化当前阶段、当前目标、累计提示次数、连续未推进次数、上一轮回答判断和置信度
- [x] 2026-07-20 已将引导状态注入下一轮 Prompt，并增加"每轮只围绕一个目标""无法判断时要求具体过程""连续未推进达到 2 次时停止猜测"的安全规则
- [x] 2026-07-20 已新增 Flyway 迁移 `V6__add_guidance_state_to_chat_session.sql`，并通过 H2 测试验证迁移可执行
- [x] 2026-08-05 已新增独立回答评估器 `AnswerEvaluator`，通过独立模型调用评估学生回答为 CORRECT/PARTIAL/WRONG/UNCLEAR，评估结果注入老师 Prompt 避免模型自判自答
- [x] 2026-08-05 已新增数学题步骤验证与解题路径约束：数学学科自动注入不跳步、每步一个运算动作、要求学生写具体过程等 Prompt 约束
- [x] 2026-08-05 已实现提示次数上限（默认 5 次）和连续卡住上限（2 次）策略：达标后老师 Prompt 注入"查看下一步/查看答案"引导语，前端在最后一条助手消息下方显示快捷按钮
- [x] 2026-08-05 已新增 `AnswerEvaluatorTests`（8 个用例），覆盖 JSON 解析、code-block 包裹、regex 兜底、空输入和置信度裁剪
- [x] 2026-08-05 已通过 `mvn test`，当前 22 个测试全部通过
- [x] 2026-08-05 已用同一道真实题图（cz1.png）分别验证引导模式/直答模式的回答差异：直答模式给出完整5步证明，引导模式首轮只提引导问题、多轮逐步推进、AnswerEvaluator 正确评估学生回答、提示上限后"看答案"触发完整证明
- [x] 2026-08-05 已用三张难度递增题图（cz1初中/cz2初中进阶/gz1高中）完成验证，引导模式和直答模式行为差异明显，引导模式能根据学生回答逐步推进
- [x] 2026-08-06 已实现防越权策略：`SessionService.buildSafetyGuardPrompt` 追加安全约束（只答学习相关内容、禁止泄露系统提示/评估器逻辑、拒绝角色扮演/注入指令、引导模式禁止抄整题答案、不输出有害内容）
- [x] 2026-08-06 已恢复画布标注输出链路：新增 `ModelOutputParser` 统一解析"讲解文本 + `---ANNOTATIONS_JSON---` + 标注 JSON"格式；`ArkModelClient` 从"纯文本兜底"升级为标注解析（原 `toResponse` 硬编码空标注已修复）；`BailianModelClient` 优先解析分隔符格式再回退整体 JSON；两个适配器的流式 delta 在进入标注区后停止向前端推送 JSON 元数据；`AiAnnotationParser` 复用既有 rect/arrow/text/highlight 标准化逻辑，标注经 `SessionService.saveAssistantMessage` 落库并同步到画布 AI 图层

---

### Phase 6 - 会话与消息系统

实现时需遵循：

- [ ] 会话与消息表结构遵循 `docs/database-design.md`
- [ ] 会话与消息接口遵循 `docs/api-design.md`
- [ ] AI 消息结构遵循 `docs/ai-prompt-spec.md`

- [x] 会话表设计
  - [x] 会话 ID
  - [x] 用户 ID
  - [x] 标题
  - [x] 选中的模型
  - [x] 关联题目图片
  - [x] 创建时间/更新时间
- [x] 消息表设计
  - [x] 消息 ID
  - [x] 会话 ID
  - [x] 角色
  - [x] 内容
  - [x] 内容类型
  - [x] 结构化标注数据
  - [x] 时间戳
- [ ] 会话接口
  - [x] 创建会话
  - [x] 会话列表
  - [x] 会话详情
  - [x] 删除会话
- [ ] 消息接口
  - [x] 发送消息
  - [x] 拉取历史消息
  - [x] 流式接收 AI 回复
- [ ] 会话标题自动生成
- [ ] 会话搜索能力

交付物：

- [x] 会话/消息数据库模型
- [x] 会话管理 API
- [x] 流式对话 API

当前实现记录：

- [x] 当前已开放接口：`POST /api/sessions`、`GET /api/sessions`、`GET /api/sessions/{id}`、`DELETE /api/sessions/{id}`、`POST /api/sessions/{id}/messages`、`GET /api/sessions/{id}/messages`
- [x] 当前消息发送已实现 MVP stub AI 回复，返回 `hintLevel`、`guidanceStage`、`teacherIntent` 和 `annotationSummary`，行为遵循 `docs/ai-prompt-spec.md` 的"引导型老师"约束
- [x] 当前消息发送链路会持久化用户消息与助手消息，助手结构化结果落库到 `raw_payload_json` / `annotation_json`
- [x] 已通过 `mvnw.cmd test` 验证会话与消息最小闭环
- [x] 已在本地 MySQL 8.0.36 环境下验证注册、登录、模型列表、图片上传、会话创建、会话列表、会话详情、消息发送、消息历史查询成功
- [x] 已完成 Phase 7/8 前置工作：已查阅并落实 `docs/canvas-protocol.md`，补齐 `canvas_document` / `canvas_operation` 实体、迁移与 `GET/PUT /api/canvas/{sessionId}` 最小接口
- [x] 2026-07-20 已新增会话引导状态字段和 V6 迁移；每轮模型调用会读取状态并在助手回复后更新状态
- [x] 2026-07-21 已新增历史对话删除能力：`DELETE /api/sessions/{id}` 软删除会话，前端会话列表提供删除按钮，删除当前会话后自动清空工作区
- [x] 下一步：把模型回答评估拆成独立结构化步骤，避免老师 Prompt 同时承担判题和提示生成（已通过 `AnswerEvaluator` 独立模型调用实现，8/5 验证通过）

---

### Phase 7 - 画布能力设计与前端交互

实现时需严格遵循：

- [ ] `docs/canvas-protocol.md`
- [ ] `docs/api-design.md`
- [ ] `docs/database-design.md`

- [ ] 确定前端技术方案
  - [x] MVP 暂定不做前后端分离，先复用 Spring Boot 静态资源方案承载前端页面
  - [x] MVP 暂定采用 Vanilla JavaScript + 原生 HTML/CSS，避免在后端闭环未稳定前引入额外构建链路
  - [x] MVP 画布实现先采用原生 Canvas，后续如交互复杂度上升再评估 Fabric.js / Konva.js
- [ ] 画布基础能力
  - [x] 加载题目原图
  - [x] 自由画笔
  - [x] 直线（2026-08-06 新增 line 工具）
  - [x] 箭头
  - [x] 矩形
  - [x] 圆形（2026-08-06 新增 circle 工具）
  - [x] 文本框
  - [ ] 橡皮擦（2026-08-06 可用"选择"工具选中后 Delete 删除替代，独立橡皮擦待做）
  - [x] 撤销
  - [ ] 重做
  - [x] 清空当前图层
  - [x] 选择/移动对象（2026-08-06 新增 select 工具，支持拖动移动与 Delete 删除）
- [ ] 视图交互
  - [ ] 缩放
  - [ ] 平移
  - [x] 自适应窗口
  - [ ] 移动端触控支持
- [ ] 图层管理
  - [x] 原题图层
  - [x] 用户标注图层
  - [x] AI 标注图层
- [ ] 标注对象结构定义
  - [x] 坐标体系
  - [x] 颜色
  - [x] 线宽
  - [x] 文字内容
  - [ ] 透明度
- [ ] 前后端数据同步协议
  - [x] 当前前端已从本地草稿存储切换到服务端画布快照保存/加载
  - [x] 画布快照
  - [x] 画布操作日志

交付物：

- [x] 可操作画布页面原型
- [ ] 标注对象 JSON 协议
- [x] 画布保存/加载接口

当前实现记录：

- [x] 开发前已查阅 `todo.md`、`docs/requirements.md`、`docs/canvas-protocol.md`
- [x] 已落地前端工作台页面：登录/注册、模型选择、题图上传、会话列表、消息区、画布区
- [x] 已打通当前后端最小 API：登录、模型列表、图片上传、会话创建、会话详情、消息发送与历史查询
- [x] 已支持本地画布标注：画笔、矩形、箭头、文字、撤销、清空、导出 JSON
- [x] 已支持将后端 `annotationSummary` 渲染为 AI 标注图层，形成"消息 + 题图 + AI 标注"联动 MVP
- [x] 当前前端进度备忘已落 `src/main/resources/static/memo.md`
- [x] 已完成工作台首轮视觉整理：修复左侧表单挤压、会话区布局混乱与右侧工具栏/画布信息区层次不清的问题，并补齐基础响应式样式
- [x] 已将认证入口与工作台拆分为独立页面：`index.html` 负责登录/注册，`workspace.html` 负责登录后的会话、回放与画布工作流
- [x] 已新增 `canvas_document` / `canvas_operation` 表、实体、Repository 与 `GET/PUT /api/canvas/{sessionId}`、`POST /api/canvas/{sessionId}/operations` 接口
- [x] 创建会话时已同步初始化 `canvas_document`，初始快照遵循 `docs/canvas-protocol.md`
- [x] 已通过 `mvnw.cmd test` 覆盖画布读取、快照保存、操作追加最小闭环
- [x] 前端已接入服务端画布快照：选中会话时读取 `GET /api/canvas/{sessionId}`，绘制/清空/撤销后保存 `PUT /api/canvas/{sessionId}` 并追加 `canvas_operation`
- [x] 前端导出的画布 JSON 已调整为 `docs/canvas-protocol.md` 的 `schemaVersion/background/layers` 快照结构
- [x] 已完成 Phase 9 回放后端最小闭环：基于 `chat_message.annotation_json` + `canvas_operation` 生成 `GET /api/replay/{sessionId}` 时间线接口
- [x] 2026-07-06 已接入前端流式问答：`POST /api/sessions/{sessionId}/messages/stream` 会逐段渲染助手答案，完成后保存用户消息、助手消息并刷新会话列表
- [x] 已放大对话区域并优化长答案展示，支持 `###` 标题、编号步骤、粗体和 `\( ... \)` 公式片段的基础格式化
- [x] 2026-07-06 已补强前端真正流式体验：提交后先渲染临时用户消息和助手流式草稿，随后按 SSE `delta` 增量更新内容，并用 `requestAnimationFrame` 合并高频刷新；最终 `done` 事件替换为后端持久化消息
- [x] 2026-07-06 已用浏览器插件实测前端流式展示，确认助手消息在 `streaming` 状态下持续增量变长，完成后替换为持久化消息；同时修复流式接口失败时"发送成功"覆盖降级提示的问题
- [x] 2026-07-07 已确认长答案显示不全并非 `.message-list` 高度或 `overflow` 导致：消息区域可滚动，主消息渲染没有 `slice/substring`；前端会在 `done` 后使用后端持久化消息，因此后端 `replyText` 被截断会表现为流式结束后答案变短
- [x] 2026-07-09 已将前端流式助手草稿的临时 `teacherIntent` 从 `answer_question` 调整为 `guide_next_step`，与后端引导式讲解模式保持一致
- [x] 2026-07-21 前端发送消息时已按当前会话绑定模型的 `supportsStream` 决定是否调用 `/messages/stream`；Qwen 和豆包开启流式后会走 SSE，未开启流式的模型自动走普通接口
- [x] 2026-07-21 前端会话列表已增加历史对话删除按钮，并在删除当前会话后自动退出回放、清空消息区和画布工作区
- [x] 2026-07-21 前端模型选择区已补充"只对新建会话生效"的提示，避免误以为切换下拉框会改变已创建会话的模型
- [x] 2026-07-21 前端发送消息时会在浏览器控制台输出当前会话模型、`supportsStream` 和回答模式，辅助排查流式链路是否生效

---

### Phase 8 - AI 标注与画图联动

实现时需严格遵循：

- [ ] `docs/ai-prompt-spec.md`
- [ ] `docs/canvas-protocol.md`
- [ ] `docs/api-design.md`
- [ ] 不同模型的结构化标注稳定性和降级策略遵循 `docs/model-provider-matrix.md`

- [ ] 将 AI 输出映射为画布动作
  - [x] 高亮区域
  - [x] 圈重点
  - [x] 箭头指向
  - [x] 文本批注
  - [x] 几何辅助图（2026-08-06 新增 line 虚线辅助线 + circle 辅助圆，已真实验证）
- [x] 定义 AI 标注指令解析器
- [ ] 处理模型输出不规范情况
  - [x] JSON 解析失败恢复
  - [x] 坐标缺失兜底
  - [x] 超出图片边界纠正
- [x] 支持 AI 分步骤标注（多轮标注按消息逐步累积到 AI 图层，2026-08-06 已真实验证）
  - [x] 第一步提示（首轮 line/rect 标注圈出待证线段/图形）
  - [x] 第二步补充（后续轮次 highlight/circle/arrow 在既有标注上叠加）
  - [ ] 最终总结（待观察 guidanceStage=summary 轮次的标注行为，可后续微调）
- [x] 支持用户手动继续编辑 AI 标注（2026-08-06 已实现：select 工具选中/拖动移动 + Delete 删除 + AI 图层解锁 + 快照与操作日志保存）
- [x] 支持保存 AI 标注结果

交付物：

- [x] AI 标注指令解析模块
- [x] 前端渲染 AI 标注能力
- [x] 联调样例数据

当前实现记录：

- [x] 已新增 `AiAnnotationParser`，将 stub / 未来模型适配层输出的结构化 `annotations` 标准化为 `docs/canvas-protocol.md` 画布对象
- [x] 解析器当前支持 `rect`、`highlight`、`arrow`、`text`，会忽略未知类型与无效对象
- [x] 解析器会补齐 `objectId`、`source=AI`、`style`、`meta.messageId`、`meta.annotationId`、`meta.teacherIntent`
- [x] 已完成坐标非负兜底、越界裁剪、无效宽高过滤、空文本过滤和高亮透明度限制
- [x] 消息发送链路已接入解析器，`chat_message.annotation_json` 保存标准化后的协议对象
- [x] AI 标注已同步保存到 `canvas_document` 的 AI 图层，前端刷新会话后仍可从画布快照恢复
- [x] 前端 AI 标注渲染已兼容协议对象中的 `style.strokeColor` / `style.fillColor` / `style.strokeWidth`
- [x] 已通过 `mvnw.cmd test` 覆盖 AI 标注解析、消息返回、画布快照保存和回放链路；已通过 Node `--check` 验证前端脚本
- [x] Phase 4/5 交界的 `ModelClient` 抽象与模型响应结构已完成，当前消息发送链路已统一通过模型客户端路由
- [x] 已接入第一家真实视觉模型适配器：阿里云百炼 / Qwen，输出仍走 `AiAnnotationParser` 标准化
- [x] 已完成真实 Qwen-VL 回复端到端联调，并补齐 JSON 解析失败恢复；当前真实模型可稳定返回文字讲解，标注可能为空
- [x] 当前真实模型联调策略已从标注优先调整为直答优先，AI 图层可暂时为空，不阻断问答主流程
- [x] 下一步最优先：待直答识题准确率可接受后，再恢复并微调 `rect/highlight/text/arrow` 标注输出（2026-08-06 已实现并真实验证：引导模式 Prompt 输出标注 JSON + `ModelOutputParser` 统一解析 + Ark/Bailian 两个适配器接入 + 流式标注区过滤，见下方验证记录）

**2026-08-06 画布标注输出真实验证记录（cz1.png / ARK 豆包）**

验证环境：本地 MySQL + Spring Boot + ARK `ep-20260721164323-qjbgk`，引导模式非流式

| 轮次 | 模型输出 | 标注输出 | 评估器 |
| --- | --- | --- | --- |
| T1 首轮 | 正确识别题目+提问"证哪两个角相等"，不直接给答案 | ✅ 1 个 rect（框 △CEF，x=570,y=380,100×200） | 不触发 |
| T2 学生答"等角对等边" | 肯定思路+推进"求∠ACD 度数" | ✅ highlight 高亮 ∠ACD + rect 框等腰△ACE | CORRECT, 0.9 |

结论：
- 模型完整遵守标注输出协议：讲解文本 + `---ANNOTATIONS_JSON---` + 标注 JSON，`ModelOutputParser` 正确拆分（331 字符原文 → 195 字符讲解）
- `AiAnnotationParser` 正确标准化 rect/highlight 两种类型，标注落库 `annotation_json` 并同步画布 AI 图层
- AnswerEvaluator 与标注输出共存正常，引导模式多轮逐步推进保持
- emoji（👍✅）可正常存储（utf8mb4）
- 遗留问题：上传接口未提取图片宽高（`background.width/height` 为 0），标注边界裁剪未生效，属 Phase 3 图片元数据提取范围

---

### Phase 9 - 对话重放

实现时需遵循：

- [ ] 回放相关数据结构遵循 `docs/database-design.md`
- [ ] 回放接口遵循 `docs/api-design.md`
- [ ] 画布回放与标注渲染遵循 `docs/canvas-protocol.md`

- [x] 定义"回放"范围
  - [x] 聊天消息逐条回放
  - [x] AI 标注逐步回放
  - [x] 用户画布操作回放
- [x] 设计回放数据结构
- [x] 实现回放时间线
  - [x] 播放
  - [x] 暂停
  - [x] 跳转步骤
  - [x] 调整速度
- [x] 回放页面设计
  - [x] 左侧聊天
  - [x] 右侧题图与画布
  - [x] 当前步骤提示
- [ ] 处理回放兼容问题
  - [ ] 老数据缺少操作日志
  - [ ] 标注版本差异

交付物：

- [x] 对话重放接口
- [x] 回放页面
- [x] 回放时间线组件

当前实现记录：

- [x] 已开放接口：`GET /api/replay/{sessionId}`，按 `docs/api-design.md` 和 `docs/canvas-protocol.md` 合并消息事件、AI 标注事件和用户画布操作事件
- [x] 已通过 `mvnw.cmd test` 覆盖回放接口最小闭环，验证返回 `MESSAGE`、`AI_ANNOTATION`、`CANVAS_OPERATION` 三类事件
- [x] 已完成前端回放页面与时间线控件，读取 `GET /api/replay/{sessionId}` 并按步骤渲染聊天、AI 标注和用户画布操作
- [x] 回放控件支持载入、播放/暂停、上一步、下一步、重置、退出回放和速度切换；回放模式下会阻止直接编辑画布，避免误写真实快照
- [x] 已通过 Node `--check` 验证 `src/main/resources/static/app.js` 语法，并通过 `mvnw.cmd test`
- [x] Phase 8 的 AI 标注解析器已完成，后续真实模型输出应先经过该解析层再进入消息、画布和回放链路

---

### Phase 10 - 管理与配置能力

实现时需遵循：

- [ ] 模型管理字段遵循 `docs/database-design.md`
- [ ] 模型展示和启停逻辑遵循 `docs/model-provider-matrix.md`
- [ ] 管理接口遵循 `docs/api-design.md`
- [ ] 后台展示模型状态时标注"是否支持视觉 / 流式 / 结构化输出"

- [ ] 模型配置管理
  - [ ] 模型上下线
  - [ ] 模型展示名称
  - [ ] 默认模型
- [ ] Prompt 模板管理
  - [ ] 按学科模板
  - [ ] 按年级模板
- [ ] 用户会话管理
  - [ ] 会话检索
  - [ ] 敏感内容审查
- [ ] 图片资源管理
- [ ] 系统运行日志查看
- [ ] 基础运营统计
  - [ ] 调用次数
  - [ ] 成功率
  - [ ] 平均响应时长

交付物：

- [ ] 简易后台管理页或管理接口

---

### Phase 11 - 测试与质量保障

测试设计需对齐：

- [ ] `docs/requirements.md`
- [ ] `docs/ai-prompt-spec.md`
- [ ] `docs/api-design.md`
- [ ] `docs/canvas-protocol.md`
- [ ] `docs/database-design.md`

- [ ] 单元测试
  - [ ] 鉴权
  - [ ] 上传
  - [ ] 会话服务
  - [ ] 模型路由
  - [ ] 标注解析器
- [ ] 集成测试
  - [ ] 图片上传到 AI 返回完整链路
  - [ ] 会话保存与回放链路
- [ ] 前端交互测试
  - [ ] 画布核心操作
  - [ ] 标注渲染
  - [ ] 回放流程
- [ ] 异常场景测试
  - [ ] 模型超时
  - [ ] 图片过大
  - [ ] 模型返回非法 JSON
  - [ ] 网络中断
- [ ] 性能测试
  - [ ] 大图加载性能
  - [ ] 多轮会话性能
  - [ ] 并发调用模型性能

交付物：

- [ ] 最低可接受测试覆盖率标准
- [ ] 联调测试清单
- [ ] 发布前检查清单

---

### Phase 12 - 部署与运维

部署后联调需覆盖：

- [ ] `docs/api-design.md` 中核心接口
- [ ] `docs/ai-prompt-spec.md` 中结构化输出约束
- [ ] `docs/canvas-protocol.md` 中画布保存/加载协议

- [ ] 明确部署方式
  - [ ] 单机部署
  - [ ] Docker 部署
  - [ ] 云服务器部署
- [ ] 配置外部化
  - [ ] 数据库连接
  - [ ] 模型 API Key
  - [ ] 文件存储配置
- [ ] 日志与监控
  - [ ] 应用日志
  - [ ] 调用错误报警
  - [ ] 资源占用监控
- [ ] 数据备份策略
  - [ ] 数据库备份
  - [ ] 图片资源备份
- [ ] 发布回滚策略

交付物：

- [ ] `Dockerfile`
- [ ] `docker-compose.yml` 或部署文档
- [ ] `docs/deploy.md`

---

## 3. 核心数据实体清单

- [ ] User
- [ ] Session
- [ ] Message
- [ ] ImageResource
- [ ] CanvasDocument
- [ ] CanvasOperation
- [ ] AiAnnotation
- [ ] ModelConfig
- [ ] PromptTemplate

---

## 4. 接口任务清单

### 4.1 认证接口

- [ ] `POST /api/auth/register`
- [ ] `POST /api/auth/login`
- [ ] `POST /api/auth/logout`
- [ ] `GET /api/auth/me`

### 4.2 图片接口

- [ ] `POST /api/images/upload`
- [ ] `GET /api/images/{id}`
- [ ] `DELETE /api/images/{id}`

### 4.3 会话接口

- [x] `POST /api/sessions`
- [x] `GET /api/sessions`
- [x] `GET /api/sessions/{id}`
- [x] `DELETE /api/sessions/{id}`

### 4.4 消息接口

- [x] `POST /api/sessions/{id}/messages`
- [x] `GET /api/sessions/{id}/messages`
- [x] `POST /api/sessions/{id}/messages/stream`

### 4.5 画布接口

- [x] `GET /api/canvas/{sessionId}`
- [x] `PUT /api/canvas/{sessionId}`
- [x] `POST /api/canvas/{sessionId}/operations`

### 4.6 回放接口

- [x] `GET /api/replay/{sessionId}`

### 4.7 模型配置接口

- [ ] `GET /api/models`
- [ ] `GET /api/models/enabled`

---

## 5. 推荐开发顺序

- [ ] 第 1 周：完成需求收敛、表结构草案、模型协议草案
- [ ] 第 2 周：完成项目脚手架、登录、图片上传
- [ ] 第 3 周：完成会话系统、模型接入、基础对话
- [ ] 第 4 周：完成前端画布基础能力
- [ ] 第 5 周：完成 AI 标注协议与联动
- [ ] 第 6 周：完成对话重放、测试、发布准备

---

## 6. 第一版 MVP 范围建议

优先做最小闭环，避免第一版过重：

- [ ] 仅支持用户登录，不做复杂角色体系
- [ ] 仅支持单张题目图片上传
- [ ] 仅接入 2 个支持图片输入的大模型
- [ ] 仅支持基础画布能力：画笔、矩形、箭头、文字、撤销重做
- [ ] 仅支持 AI 文本讲解 + 基础标注，不做复杂自动几何作图
- [ ] 仅支持会话历史和简单回放，不做高级搜索和运营后台

---

## 7. 当前需要优先澄清的问题

- [x] 前端 MVP 当前采用 Spring Boot 静态资源 + Vanilla JavaScript + 原生 Canvas
- [x] 数据库使用 MySQL
- [x] 推荐版本：MySQL 8.4 LTS
- [ ] 文件存储先落本地还是对象存储
- [x] 第一批要接入哪些大模型
- [x] 2026-07-05 已确认第一家真实模型供应商优先接入阿里云百炼 / 通义千问，候选模型为 `qwen-vl-plus` 或后续按官方文档确认的新版 Qwen-VL 模型
- [ ] 是否先只接入 `docs/model-provider-matrix.md` 中的三家优先模型
- [ ] AI 输出是否强制 JSON 结构化
- [x] 是否需要流式输出打字机效果（2026-07-06 已实现后端 SSE + 前端 ReadableStream 增量渲染 MVP）
- [ ] 是否需要移动端优先适配
- [ ] 是否需要后台管理页面

---

## 8. 后续开发建议

- [ ] 先完成 `docs/requirements.md` 和数据库 ER 草图
- [ ] 再实现后端最小闭环：登录 + 上传 + 建会话 + 调模型
- [ ] 然后补前端画布和 AI 标注联动
- [ ] 最后实现对话回放和管理能力
- [ ] 开发任何模块前，先检查对应阶段标注的 `.md` 规范，避免接口、表结构、画布协议各自漂移
- [ ] 涉及模型能力的功能开发前，先核对 `docs/model-provider-matrix.md` 的检查日期；若距当前超过 30 天，先按官方文档复核
- [x] 每完成一批功能后，必须回写 `todo.md` 对应阶段的完成状态和当前实现说明
- [x] 数据库方案已定为 MySQL，后续 SQL、索引和部署文档优先按 MySQL 编写

---

## 9. 变更记录

### 2026-08-05 前端布局与交互优化

**改动文件：**
- `src/main/resources/static/workspace.html`
- `src/main/resources/static/app.css`
- `src/main/resources/static/app.js`

**变更内容：**

1. **题图与标注模块移至页面顶部**
   - 原布局为左右两栏（侧栏 + 主区域），题图画布在主区域最下方
   - 新布局改为上下结构：顶部全宽放置题图画布，下方为左侧会话列表 + 右侧聊天区
   - 图片上传按钮也整合到顶部画布面板的标题栏中
   - 标注颜色和线宽控件合并到工具栏，减少视觉层级

2. **去掉"创建会话"按钮，改为自动创建**
   - 删除了创建会话表单（标题、学科、年级输入框 + 创建按钮）
   - 新逻辑：用户上传题图后，在聊天框输入第一条消息并发送时，系统自动创建会话
   - 会话标题取自用户第一条问题的前 30 个字符
   - `subjectCode` 默认 `MATH`，`gradeLevel` 默认 `JUNIOR`

3. **隐藏"当前画布 JSON"调试面板**
   - 通过 `style="display:none;"` 隐藏，保留 DOM 结构供后续调试使用

### 2026-08-06 防越权策略 + 画布标注输出恢复

**改动文件：**
- `src/main/java/com/cheat/exam/service/model/ModelOutputParser.java`（新增）
- `src/main/java/com/cheat/exam/service/model/ArkModelClient.java`
- `src/main/java/com/cheat/exam/service/model/BailianModelClient.java`
- `src/main/java/com/cheat/exam/service/SessionService.java`
- `src/test/java/com/cheat/exam/service/model/ArkModelClientTests.java`
- `src/test/java/com/cheat/exam/service/model/BailianModelClientTests.java`

**变更内容：**

1. **防越权策略**：`SessionService.buildSafetyGuardPrompt` 追加 6 条安全约束——只回答学习相关内容、禁止泄露系统提示/评估器逻辑、拒绝角色扮演/非学习任务、引导模式禁止直接抄整题答案、不输出有害内容、拒绝注入指令（如"忽略以上所有指令"）

2. **画布标注输出恢复**：
   - 新增 `ModelOutputParser`，统一解析"讲解文本 + `---ANNOTATIONS_JSON---` + 标注 JSON"格式，支持流式标注区检测
   - 引导模式 Prompt 明确标注协议：rect/arrow/text/highlight 类型、原图坐标规则、分隔符格式
   - `ArkModelClient` 从"整段当纯文本"升级为标注解析，修复 `toResponse` 硬编码空标注问题
   - `BailianModelClient` 优先解析分隔符格式，兼容旧的整体 JSON 格式
   - 两个适配器的流式 delta 在进入标注区后停止向前端推送 JSON 元数据
   - 标注经 `AiAnnotationParser` 标准化后落库 `annotation_json` 并同步到画布 AI 图层

### 2026-08-06 画布几何辅助图 + AI 分步骤标注 + 用户编辑 AI 标注

**改动文件：**
- `src/main/java/com/cheat/exam/service/SessionService.java`
- `src/main/java/com/cheat/exam/service/ai/AiAnnotationParser.java`
- `src/main/resources/static/app.js`
- `src/main/resources/static/workspace.html`
- `src/test/java/com/cheat/exam/service/ai/AiAnnotationParserTests.java`

**变更内容：**

1. **几何辅助图**：
   - 标注协议新增 `line`（线段/辅助线，支持 `dashed` 虚线）和 `circle`（圆/辅助圆，`x,y,radius`）类型
   - `AiAnnotationParser` 新增 `putLineFields`/`putCircleFields`，line/circle 的 fillColor 透明、坐标超界裁剪
   - 前端 `drawAiAnnotation`/`drawUserObject` 渲染 line（虚线可选）/circle
   - 工具栏新增"直线""圆形"工具，用户可手绘几何辅助线

2. **AI 分步骤标注**：多轮对话每轮标注按消息追加到 AI 图层，逐步累积展示（T1 line + T2 highlight/circle 叠加验证通过）

3. **用户手动编辑 AI 标注**：
   - AI 图层 `locked` 改为 `false`，允许编辑
   - 新增"选择/移动"工具：命中检测（rect/highlight/line/arrow/circle/text/pen）+ 拖动移动 + Delete 删除
   - 新增"清空AI标注"按钮；编辑操作经 `UPDATE_OBJECT`/`DELETE_OBJECT` 落操作日志并保存快照

**2026-08-06 真实验证（cz1.png / ARK 豆包，session 30）**
- T1：模型输出 2 条 line 辅助线（红色虚线标出待证线段 CE、CF）+ 引导提问；标注正确解析并同步画布 AI 图层
- T2 学生答"等角对等边"：AnswerEvaluator CORRECT(0.95)；模型输出 highlight 高亮 △CEF + circle 圈住 ∠CAD，与 T1 标注按步骤累积
- 24 个测试全部通过（AiAnnotationParserTests 新增 line/circle 2 个用例）

### 2026-08-06 画布标注坐标错位修复（图片尺寸基准对齐）

**问题**：AI 标注（如"待证线段CE/CF"辅助线）显示在画面右下角，未覆盖题图几何图形。

**根因**：
1. `ImageService.upload` 上传时 `setWidth(null); setHeight(null)` 不提取图片尺寸，`image_resource` 宽高为空
2. 引导模式 Prompt 未告知模型图片实际尺寸，模型内部对图片缩放/pad 后按自己的尺寸基准输出坐标（cz1.png 实际 638×461，模型却输出 y=590 等越界坐标）
3. 前端 `toCanvasX/Y = rawX/naturalWidth * canvas.width` 用真实图片尺寸归一化，与模型坐标基准不一致 → 错位

**修复**：
1. `ImageService.resolveImageDimension`：上传时用 `ImageIO` 解析 PNG/JPEG 宽高存库（WebP 暂不支持返回 null）
2. `SessionService.buildSystemPrompt`：从 `session.image.width/height` 读取实际尺寸，注入标注协议"题目图片实际尺寸：宽 X 像素，高 Y 像素。所有标注坐标必须基于该尺寸输出"

**验证**（cz1.png 638×461，session 32）：
- 修复后模型坐标全部落在 638×461 内：rect 正方形ABCD (398,148)184×234；line CE (580,380)→(638,250)；line CF (580,380)→(580,225)
- 标注正确覆盖几何图形区域，画布 AI 图层同步正常
- 遗留：历史图片（上传于修复前）宽高仍为 null，前端会用图片自然尺寸兜底归一化，坐标基准仍可能偏差；建议重新上传题图或后续提供批量补数据脚本

### 2026-08-06 标注坐标改为 0~1000 归一化 + 定位引导

**背景**：注入图片尺寸后标注仍不在正确位置。诊断发现根因是**视觉模型绝对像素定位不可靠**：
- 豆包模型以为图片是 1784×1264（实际 638×461），输出坐标基于错误坐标系
- 同一张图多次调用，模型对同一元素（如 C 点）输出位置差异巨大（0-1000 下 x 从 331 到 625）
- Qwen-VL 更差，直接无视坐标指令只讲题

**修复**：
1. **0~1000 归一化坐标系**：Prompt 要求模型把图片看作 1000×1000 画布，按几何元素的相对位置输出坐标；`AiAnnotationParser` 裁剪边界改为 1000；前端 `toCanvasX/Y`、`toImageX/Y` 改为 `坐标/1000×画布尺寸`
2. **定位引导 Prompt**：要求模型先判断元素在图片中的相对位置（左上/右上/中部等）再输出坐标；无法可靠定位时省略标注，禁止瞎猜；label 必须说明指向元素

**验证**（session 41，cz1.png / ARK）：
- 模型输出 3 个自洽标注：rect 正方形ABCD (400,325)200×255；line 对角线AC (405,335)→(595,575)；line 线段DE (595,335)→(665,420)
- 几何关系正确（正方形 + 对角线方向 + 右边界外延线段），标注不再越界
- 测试 24 个全部通过；待用户在浏览器实测确认视觉位置

**经验**：视觉模型（豆包/Qwen-VL）对图片精确像素坐标的感知不可靠且不稳定，标注坐标应使用归一化相对坐标 + 定位引导，并接受位置为近似值，配合前端"选择/移动"工具供用户微调
