# 数据库设计文档（初稿）

## 1. 文档目的

本文档用于定义“AI 改卷 / 试卷讲解 Web”项目第一版的核心数据模型，为后续数据库建表、ORM 映射、接口开发和测试提供统一基础。

当前设计以 MVP 为目标，优先支持以下核心能力：

- 用户登录
- 题目图片上传
- 模型选择与会话创建
- 消息存储
- AI 标注存储
- 画布内容保存
- 对话重放

---

## 2. 设计原则

### 2.1 面向 MVP

优先支撑最小业务闭环，暂不提前引入复杂教学、班级、计费、多租户结构。

### 2.2 可扩展

表结构预留模型扩展、标注扩展和回放扩展能力。

### 2.3 审计友好

关键业务对象保留创建时间、更新时间、状态字段，便于排查和追溯。

### 2.4 前后端协议对齐

画布和 AI 标注相关数据尽量保留 JSON 字段，减少第一版频繁改表成本。

---

## 3. 数据库选型建议

当前项目数据库方案已确定为 MySQL，第一版建议优先使用 MySQL 8.4 LTS。

建议：

- 生产和长期维护优先使用 MySQL 8.4 LTS
- 本地开发建议与生产保持同主版本
- 自动化测试可继续使用 H2 的 MySQL 兼容模式以降低环境依赖

本文档中的字段类型以通用关系型数据库表达为主。

---

## 4. 表清单

第一版核心表建议如下：

- `sys_user`
- `image_resource`
- `model_config`
- `chat_session`
- `chat_message`
- `canvas_document`
- `canvas_operation`
- `ai_annotation_record`

可选扩展表：

- `prompt_template`
- `user_login_log`

---

## 5. 核心实体关系

关系说明：

- 一个用户可以拥有多个会话
- 一个用户可以上传多张图片
- 一个会话关联一张主题目图
- 一个会话包含多条消息
- 一个会话对应一个当前画布文档
- 一个会话可以有多条画布操作记录
- 一条 AI 消息可以产生一组标注记录

简化关系：

- `sys_user 1 - n chat_session`
- `sys_user 1 - n image_resource`
- `chat_session 1 - n chat_message`
- `chat_session 1 - 1 canvas_document`
- `chat_session 1 - n canvas_operation`
- `chat_message 1 - n ai_annotation_record`

---

## 6. 表设计

### 6.1 用户表 `sys_user`

用途：

- 存储系统用户基础信息和登录信息

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `username` | varchar(64) | 是 | 登录账号，唯一 |
| `password_hash` | varchar(255) | 是 | 密码摘要 |
| `nickname` | varchar(64) | 否 | 昵称 |
| `avatar_url` | varchar(512) | 否 | 头像地址 |
| `role_code` | varchar(32) | 是 | 角色编码，默认 `USER` |
| `status` | varchar(32) | 是 | 状态，`ACTIVE`/`DISABLED` |
| `created_at` | datetime/timestamp | 是 | 创建时间 |
| `updated_at` | datetime/timestamp | 是 | 更新时间 |

约束建议：

- `username` 唯一索引
- `status` 普通索引

说明：

- 第一版角色可只用 `USER` 和 `ADMIN`
- 如果后续做手机号登录，可再增加手机号字段

### 6.2 图片资源表 `image_resource`

用途：

- 存储题目图片及其元数据

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `user_id` | bigint | 是 | 上传用户 ID |
| `origin_file_name` | varchar(255) | 否 | 原始文件名 |
| `storage_key` | varchar(512) | 是 | 存储路径或对象 Key |
| `access_url` | varchar(1024) | 否 | 访问地址 |
| `mime_type` | varchar(64) | 是 | MIME 类型 |
| `file_size` | bigint | 是 | 文件大小，字节 |
| `width` | int | 否 | 图片宽度 |
| `height` | int | 否 | 图片高度 |
| `sha256` | varchar(128) | 否 | 文件摘要，可用于去重 |
| `storage_type` | varchar(32) | 是 | `LOCAL`/`MINIO`/`OSS` |
| `status` | varchar(32) | 是 | `ACTIVE`/`DELETED` |
| `created_at` | datetime/timestamp | 是 | 创建时间 |
| `updated_at` | datetime/timestamp | 是 | 更新时间 |

索引建议：

- `idx_image_user_id`
- `idx_image_sha256`

说明：

- 第一版即使先落本地，也建议保留 `storage_type`
- `access_url` 可由服务动态拼接，不一定强依赖持久化

### 6.3 模型配置表 `model_config`

用途：

- 存储可供前端选择的模型配置

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `model_code` | varchar(64) | 是 | 模型编码，唯一 |
| `display_name` | varchar(128) | 是 | 前端显示名称 |
| `provider_code` | varchar(64) | 是 | 供应商编码 |
| `supports_vision` | boolean | 是 | 是否支持图像输入 |
| `supports_stream` | boolean | 是 | 是否支持流式输出 |
| `enabled` | boolean | 是 | 是否启用 |
| `sort_order` | int | 否 | 排序值 |
| `config_json` | text/json | 否 | 额外配置 |
| `created_at` | datetime/timestamp | 是 | 创建时间 |
| `updated_at` | datetime/timestamp | 是 | 更新时间 |

约束建议：

- `model_code` 唯一索引
- `enabled` 普通索引

### 6.4 会话表 `chat_session`

用途：

- 存储一次题目讲解会话的主信息

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `user_id` | bigint | 是 | 所属用户 ID |
| `title` | varchar(255) | 否 | 会话标题 |
| `model_code` | varchar(64) | 是 | 使用的模型编码 |
| `image_id` | bigint | 是 | 关联题目图片 ID |
| `subject_code` | varchar(32) | 否 | 学科编码 |
| `grade_level` | varchar(32) | 否 | 年级编码 |
| `status` | varchar(32) | 是 | `ACTIVE`/`ARCHIVED`/`DELETED` |
| `last_message_at` | datetime/timestamp | 否 | 最后消息时间 |
| `guidance_state_json` | longtext/json | 否 | 当前引导阶段、目标、提示次数和回答判断状态 |
| `created_at` | datetime/timestamp | 是 | 创建时间 |
| `updated_at` | datetime/timestamp | 是 | 更新时间 |

索引建议：

- `idx_session_user_id`
- `idx_session_user_status`
- `idx_session_last_message_at`

说明：

- `title` 可由 AI 或后端自动生成
- 第一版一个会话默认绑定一张主图，后续如支持多图可增加关联表
- `guidance_state_json` 用于保存多轮引导状态；无法解析的模型判断必须按低置信度处理，不得作为最终答案依据

### 6.5 消息表 `chat_message`

用途：

- 存储会话中的消息内容

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `session_id` | bigint | 是 | 所属会话 ID |
| `role_code` | varchar(32) | 是 | `SYSTEM`/`USER`/`ASSISTANT` |
| `content_type` | varchar(32) | 是 | `TEXT`/`JSON`/`MIXED` |
| `content_text` | text | 否 | 面向用户展示的文本 |
| `raw_payload_json` | longtext/json | 否 | 模型原始返回或结构化消息 |
| `annotation_json` | longtext/json | 否 | 本条消息的标注摘要 |
| `hint_level` | int | 否 | 提示强度 |
| `guidance_stage` | varchar(32) | 否 | 引导阶段 |
| `message_status` | varchar(32) | 是 | `SUCCESS`/`FAILED`/`PARTIAL` |
| `token_usage_prompt` | int | 否 | 输入 token 用量 |
| `token_usage_completion` | int | 否 | 输出 token 用量 |
| `provider_request_id` | varchar(128) | 否 | 模型供应商请求 ID |
| `created_at` | datetime/timestamp | 是 | 创建时间 |

索引建议：

- `idx_message_session_id`
- `idx_message_session_created_at`
- `idx_message_role_code`

说明：

- `raw_payload_json` 用于后续调试和重放
- `annotation_json` 可用于快速回显，详细标注还可拆到单独表

### 6.6 画布文档表 `canvas_document`

用途：

- 存储某个会话当前画布的最新状态

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `session_id` | bigint | 是 | 会话 ID，唯一 |
| `background_image_id` | bigint | 是 | 背景题图 ID |
| `snapshot_json` | longtext/json | 是 | 当前画布快照 |
| `version_no` | int | 是 | 版本号 |
| `updated_by_type` | varchar(32) | 是 | `USER`/`AI`/`SYSTEM` |
| `updated_by_id` | bigint | 否 | 操作者 ID，可为空 |
| `created_at` | datetime/timestamp | 是 | 创建时间 |
| `updated_at` | datetime/timestamp | 是 | 更新时间 |

约束建议：

- `session_id` 唯一索引

说明：

- 第一版使用快照存储最省实现成本
- 快照结构与前端画布 JSON 协议保持一致

### 6.7 画布操作表 `canvas_operation`

用途：

- 存储画布的操作日志，用于重放与审计

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `session_id` | bigint | 是 | 所属会话 ID |
| `message_id` | bigint | 否 | 来源消息 ID，如来自 AI 回复 |
| `operator_type` | varchar(32) | 是 | `USER`/`AI`/`SYSTEM` |
| `operator_id` | bigint | 否 | 用户 ID，AI 时可为空 |
| `operation_type` | varchar(64) | 是 | `ADD_SHAPE`/`UPDATE_SHAPE`/`DELETE_SHAPE`/`CLEAR_LAYER` |
| `layer_type` | varchar(32) | 是 | `AI`/`USER` |
| `payload_json` | longtext/json | 是 | 操作内容 |
| `sequence_no` | bigint | 是 | 会话内顺序号 |
| `created_at` | datetime/timestamp | 是 | 创建时间 |

索引建议：

- `idx_canvas_op_session_id`
- `idx_canvas_op_session_seq`
- `idx_canvas_op_message_id`

说明：

- 如果第一版不立即实现精细回放，也建议先保留此表
- 第一版可只记录 AI 标注和关键用户操作

### 6.8 AI 标注记录表 `ai_annotation_record`

用途：

- 将 AI 标注拆成更细粒度记录，便于回放、调试和统计

字段设计：

| 字段名 | 类型 | 非空 | 说明 |
|---|---|---|---|
| `id` | bigint | 是 | 主键 ID |
| `session_id` | bigint | 是 | 会话 ID |
| `message_id` | bigint | 是 | 来源 AI 消息 ID |
| `annotation_id` | varchar(64) | 否 | 模型输出中的标注 ID |
| `annotation_type` | varchar(32) | 是 | `RECT`/`ARROW`/`TEXT`/`HIGHLIGHT` |
| `payload_json` | longtext/json | 是 | 标注详情 |
| `render_status` | varchar(32) | 是 | `READY`/`IGNORED`/`FAILED` |
| `created_at` | datetime/timestamp | 是 | 创建时间 |

索引建议：

- `idx_ai_ann_session_id`
- `idx_ai_ann_message_id`

说明：

- 如果嫌实现重，第一版也可只保存在 `chat_message.annotation_json`
- 但独立表更利于回放和问题排查

---

## 7. 可选扩展表

### 7.1 Prompt 模板表 `prompt_template`

用途：

- 存储不同学科、年级、场景下的 Prompt 模板

字段建议：

- `id`
- `template_code`
- `template_name`
- `subject_code`
- `grade_level`
- `content_text`
- `enabled`
- `created_at`
- `updated_at`

### 7.2 登录日志表 `user_login_log`

用途：

- 记录用户登录审计信息

字段建议：

- `id`
- `user_id`
- `login_ip`
- `user_agent`
- `login_result`
- `created_at`

---

## 8. 枚举建议

### 8.1 用户状态

- `ACTIVE`
- `DISABLED`

### 8.2 会话状态

- `ACTIVE`
- `ARCHIVED`
- `DELETED`

### 8.3 消息角色

- `SYSTEM`
- `USER`
- `ASSISTANT`

### 8.4 消息状态

- `SUCCESS`
- `FAILED`
- `PARTIAL`

### 8.5 标注类型

- `RECT`
- `ARROW`
- `TEXT`
- `HIGHLIGHT`

### 8.6 操作者类型

- `USER`
- `AI`
- `SYSTEM`

---

## 9. 索引建议汇总

高频查询场景：

- 按用户查会话列表
- 按会话查消息列表
- 按会话查画布快照与操作日志
- 按用户查图片

建议重点索引：

- `sys_user.username`
- `image_resource.user_id`
- `chat_session.user_id, status, last_message_at`
- `chat_message.session_id, created_at`
- `canvas_operation.session_id, sequence_no`
- `ai_annotation_record.message_id`

---

## 10. 删除策略

第一版建议采用逻辑删除为主：

- 用户对象通常不物理删除
- 会话可逻辑删除
- 图片资源可逻辑删除

说明：

- 如果文件已删除但数据库还保留，可通过 `status` 标记异常
- 真正清理物理文件可以由后台任务处理

---

## 11. 一致性建议

### 11.1 会话创建

创建会话时建议同时落：

- `chat_session`
- 首张题图关联
- 初始 `canvas_document`

### 11.2 AI 回复保存

当 AI 返回一轮结果时建议在一个事务里尽量完成：

- 写入 `chat_message`
- 写入 `ai_annotation_record`
- 更新 `canvas_document`
- 写入 `canvas_operation`
- 更新 `chat_session.last_message_at`

说明：

- 如果涉及外部模型调用，调用本身不在事务内
- 数据落库阶段应确保核心信息一致

---

## 12. 首版建表优先级

P0 必须：

- `sys_user`
- `image_resource`
- `model_config`
- `chat_session`
- `chat_message`

P1 建议：

- `canvas_document`
- `canvas_operation`

P1.5 可选：

- `ai_annotation_record`

说明：

- 如果要更快启动开发，第一版也可先把标注只存到 `chat_message.annotation_json`
- 但回放能力会受限

---

## 13. 与接口的映射关系

### 13.1 登录接口

主要涉及：

- `sys_user`

### 13.2 图片上传接口

主要涉及：

- `image_resource`

### 13.3 会话创建接口

主要涉及：

- `chat_session`
- `canvas_document`

### 13.4 消息发送接口

主要涉及：

- `chat_message`
- `ai_annotation_record`
- `canvas_operation`
- `canvas_document`

### 13.5 回放接口

主要涉及：

- `chat_message`
- `canvas_operation`
- `ai_annotation_record`

---

## 14. 后续工作建议

建议下一步继续补充：

- `docs/api-design.md`
- `docs/canvas-protocol.md`
- `docs/model-provider-matrix.md`

落地开发时建议优先做：

1. ER 图草图
2. Flyway 或 Liquibase 初始脚本
3. Java 实体类与枚举定义
4. 基础 Repository 层
