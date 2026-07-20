# AI 改卷 / 试卷讲解 Web 项目任务拆解

## 1. 项目目标

构建一个基于 Java 21、Spring Boot 4、Maven 的 Web 应用，支持：

- 用户上传题目图片
- 将题目图片发送给可选的大模型进行讲解
- 大模型以“引导型老师”身份回答，不直接一次性给出完整答案
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
  - [ ] 定义“引导型老师”提示词规范
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
- [ ] 接入前先复核 `docs/model-provider-matrix.md` 的“检查日期”和官方链接，避免模型能力过期

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
- [x] 2026-07-06 根据真实效果调整为“直答识图模式”：百炼 / Qwen 暂不强制 JSON、不要求引导、不生成标注，直接展示模型原文答案，用于优先验证图片识别和解题正确性
- [x] 2026-07-06 真实验证简单题图 `2 + 3 = ?`，`qwen-vl-plus` 成功识别题目并返回正确答案 `5` 与简短解题过程
- [x] 已通过 `mvnw.cmd test`，当前 9 个测试全部通过
- [x] 2026-07-06 排查到“仍返回引导式固定话术”的原因：前端默认选中了仍启用的占位模型 `openai-default` 等，后端路由找不到真实客户端后兜底到了 `StubModelClient`
- [x] 已新增 Flyway 迁移 `V5__prefer_qwen_disable_placeholder_models.sql`，禁用未真实接入的 `openai-default`、`anthropic-default`、`gemini-default`，并将 `qwen-vl-plus` 设为启用且排序第一
- [x] 已调整 `ModelClientRouter`：除非模型供应商显式为 `STUB`，否则不允许静默兜底到 `StubModelClient`；以后未接入模型会直接报 `MODEL_NOT_AVAILABLE`
- [x] 已通过 `mvnw.cmd test`，当前 9 个测试全部通过
- [x] 2026-07-06 已新增模型流式输出抽象：`ModelClient.stream(...)`；百炼 / Qwen 适配器已通过 OpenAI-compatible `stream=true` 读取 SSE delta，并在不支持真实流式的模型上保留分段降级输出
- [x] 已通过 `mvnw.cmd test`，当前 9 个测试全部通过
- [x] 2026-07-06 修复前端请求流式接口失败时只显示“流式响应建立失败”的问题：现在会读取真实 HTTP 错误，并在 `/messages/stream` 不可用时自动降级到原 `/messages` 非流式接口，避免旧后端进程未重启时无法提问
- [x] 已通过 `mvnw.cmd test` 和 Node `--check src/main/resources/static/app.js`
- [x] 2026-07-06 已优化前端流式展示：发送后立即显示用户消息与助手草稿，SSE `delta` 到达时逐帧刷新草稿内容，`done` 后替换为持久化消息；流式连接失败会清理草稿并降级或提示，避免界面卡在半生成状态
- [x] 已通过 `mvnw.cmd test` 和 Codex bundled Node `--check src/main/resources/static/app.js`
- [x] 2026-07-06 使用浏览器插件排查“前端看起来不是流式”：根因是流式接口手动序列化 `Instant` 失败，前端收到非 2xx 后自动降级到普通 `/messages`，导致答案一次性出现
- [x] 2026-07-06 已修复流式链路：`POST /api/sessions/{id}/messages/stream` 改为同步写 `HttpServletResponse` SSE，补齐 `X-Accel-Buffering: no`，`ObjectMapper` 显式注册 `JavaTimeModule` 并禁用时间戳日期输出
- [x] 2026-07-06 已通过 `curl -N` 验证原始响应为 `200 text/event-stream` 且持续输出多个 `event: delta`；已通过浏览器插件验证前端助手消息长度从 91、116、147 逐步增长到完成状态
- [x] 2026-07-07 已排查“长答案只显示一半”：根因不是前端 CSS，而是百炼/Qwen 非结构化兜底解析把用户可见 `replyText` 截断到 600 字符，且本地 `max_tokens` 仅 500；已移除用户可见答案截断，并将默认/本地输出上限提高到 3000 tokens
- [x] 2026-07-07 已新增长文本回归测试，确保模型返回超过 600 字符的纯文本答案时，最终持久化助手消息不会再被替换成半截内容
- [x] 2026-07-09 已将百炼/Qwen 从临时“直答识图模式”切换回“引导式讲解模式”，同时保留 3000 tokens 输出预算和流式输出能力
- [ ] 下一步最优先：用真实题目图片验证 Qwen-VL 在引导模式下是否能先提出有效问题、再根据学生回答逐步推进

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
- [ ] 设计 AI 输出格式
  - [x] 纯文本讲解
  - [ ] 结构化提示点
  - [ ] 画布标注指令
- [ ] 定义结构化协议
  - [ ] 高亮区域
  - [ ] 矩形框
  - [ ] 箭头
  - [ ] 文本说明
  - [ ] 几何辅助线
- [ ] 设计防越权策略
  - [ ] 禁止输出不合适内容
  - [ ] 禁止提示泄露
  - [ ] 禁止直接整题抄答案
- [ ] 设计多轮对话状态管理
  - [ ] 追问
  - [ ] 用户回答后的二次引导
  - [ ] 用户要求“直接答案”时的兜底策略
  - [x] 持久化当前阶段、当前目标、提示次数和连续未推进次数
  - [x] 无法可靠判断时停止编造提示
  - [ ] 独立回答评估器：`CORRECT` / `PARTIAL` / `WRONG` / `UNCLEAR`
  - [ ] 数学题步骤验证与解题路径约束
  - [ ] 提示次数达到上限后的“查看下一步/查看答案”交互

交付物：

- [x] Prompt 模板
- [ ] AI 输出 JSON 协议文档
- [x] 对话编排服务

当前实现记录：

- [x] 已在 `SessionService` 中加入基础系统提示词编排，包含引导型老师、分步讲解、不直接给完整答案、学科和年级上下文
- [x] 已通过 `ModelChatRequest.messages` 向模型客户端传递会话历史消息
- [x] 当前 Prompt 仍是代码内基础模板，后续可升级为 `prompt_template` 表或配置化模板
- [x] 2026-07-06 百炼 / Qwen 适配器已临时切换为直答识图 Prompt，不再套用引导型老师和 JSON 标注约束；目的是先验证视觉识别与题解能力
- [x] 2026-07-09 已恢复并落地“引导式讲解模式”：`SessionService` 负责生成引导型老师业务 Prompt，`BailianModelClient` 不再覆盖为直答 Prompt，而是要求首轮识别题意/关键条件/提出一个小问题，多轮时先判断学生回答再给下一步提示
- [x] 2026-07-09 已将百炼/Qwen 纯文本兜底元数据调整为 `guidanceStage=observe`、`teacherIntent=guide_next_step`、`shouldRevealFinalAnswer=false`，避免非 JSON 输出被误标记为直答
- [x] 2026-07-09 已通过 `BailianModelClientTests` 校验真实模型请求体包含“引导式讲解模式”“不要在第一轮直接给最终答案”“先判断学生”等关键约束，并通过 `mvnw.cmd test`
- [x] 2026-07-20 已在前端工作台增加“引导模式 / 直答模式”切换，选择保存在浏览器本地，并通过普通/流式消息请求的 `mode` 字段传到后端 Prompt 编排
- [x] 2026-07-20 已约定 `mode=guided` 为默认值、`mode=direct` 为直答模式；后端未传值时自动按引导模式处理，并更新 `docs/api-design.md`
- [x] 2026-07-20 已为会话增加 `guidance_state_json`，持久化当前阶段、当前目标、累计提示次数、连续未推进次数、上一轮回答判断和置信度
- [x] 2026-07-20 已将引导状态注入下一轮 Prompt，并增加“每轮只围绕一个目标”“无法判断时要求具体过程”“连续未推进达到 2 次时停止猜测”的安全规则
- [x] 2026-07-20 已新增 Flyway 迁移 `V6__add_guidance_state_to_chat_session.sql`，并通过 H2 测试验证迁移可执行
- [ ] 下一步最优先：使用同一道真实题目分别验证两种模式的回答差异，检查引导模式是否能根据学生回答逐步推进

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
  - [ ] 删除会话
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

- [x] 当前已开放接口：`POST /api/sessions`、`GET /api/sessions`、`GET /api/sessions/{id}`、`POST /api/sessions/{id}/messages`、`GET /api/sessions/{id}/messages`
- [x] 当前消息发送已实现 MVP stub AI 回复，返回 `hintLevel`、`guidanceStage`、`teacherIntent` 和 `annotationSummary`，行为遵循 `docs/ai-prompt-spec.md` 的“引导型老师”约束
- [x] 当前消息发送链路会持久化用户消息与助手消息，助手结构化结果落库到 `raw_payload_json` / `annotation_json`
- [x] 已通过 `mvnw.cmd test` 验证会话与消息最小闭环
- [x] 已在本地 MySQL 8.0.36 环境下验证注册、登录、模型列表、图片上传、会话创建、会话列表、会话详情、消息发送、消息历史查询成功
- [x] 已完成 Phase 7/8 前置工作：已查阅并落实 `docs/canvas-protocol.md`，补齐 `canvas_document` / `canvas_operation` 实体、迁移与 `GET/PUT /api/canvas/{sessionId}` 最小接口
- [x] 2026-07-20 已新增会话引导状态字段和 V6 迁移；每轮模型调用会读取状态并在助手回复后更新状态
- [ ] 下一步：把模型回答评估拆成独立结构化步骤，避免老师 Prompt 同时承担判题和提示生成

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
  - [ ] 直线
  - [x] 箭头
  - [x] 矩形
  - [ ] 圆形
  - [x] 文本框
  - [ ] 橡皮擦
  - [x] 撤销
  - [ ] 重做
  - [x] 清空当前图层
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
- [x] 已支持将后端 `annotationSummary` 渲染为 AI 标注图层，形成“消息 + 题图 + AI 标注”联动 MVP
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
- [x] 2026-07-06 已用浏览器插件实测前端流式展示，确认助手消息在 `streaming` 状态下持续增量变长，完成后替换为持久化消息；同时修复流式接口失败时“发送成功”覆盖降级提示的问题
- [x] 2026-07-07 已确认长答案显示不全并非 `.message-list` 高度或 `overflow` 导致：消息区域可滚动，主消息渲染没有 `slice/substring`；前端会在 `done` 后使用后端持久化消息，因此后端 `replyText` 被截断会表现为流式结束后答案变短
- [x] 2026-07-09 已将前端流式助手草稿的临时 `teacherIntent` 从 `answer_question` 调整为 `guide_next_step`，与后端引导式讲解模式保持一致

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
  - [ ] 几何辅助图
- [x] 定义 AI 标注指令解析器
- [ ] 处理模型输出不规范情况
  - [x] JSON 解析失败恢复
  - [x] 坐标缺失兜底
  - [x] 超出图片边界纠正
- [ ] 支持 AI 分步骤标注
  - [ ] 第一步提示
  - [ ] 第二步补充
  - [ ] 最终总结
- [ ] 支持用户手动继续编辑 AI 标注
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
- [ ] 下一步最优先：待直答识题准确率可接受后，再恢复并微调 `rect/highlight/text/arrow` 标注输出

---

### Phase 9 - 对话重放

实现时需遵循：

- [ ] 回放相关数据结构遵循 `docs/database-design.md`
- [ ] 回放接口遵循 `docs/api-design.md`
- [ ] 画布回放与标注渲染遵循 `docs/canvas-protocol.md`

- [x] 定义“回放”范围
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
- [ ] 后台展示模型状态时标注“是否支持视觉 / 流式 / 结构化输出”

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
- [ ] `DELETE /api/sessions/{id}`

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
