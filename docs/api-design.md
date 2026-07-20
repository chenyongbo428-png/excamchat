# API 设计文档（初稿）

## 1. 文档目的

本文档用于定义“AI 改卷 / 试卷讲解 Web”项目第一版的后端接口设计，统一前后端请求与响应格式，并为 Spring Boot 4 开发、接口联调和 OpenAPI 文档生成提供依据。

本文档与以下文档配套使用：

- `docs/requirements.md`
- `docs/database-design.md`
- `docs/ai-prompt-spec.md`
- `docs/canvas-protocol.md`

---

## 2. 设计原则

### 2.1 面向 MVP

接口优先支撑最小业务闭环，不提前引入复杂后台和多角色业务。

### 2.2 统一响应格式

除流式接口外，所有接口统一返回标准 JSON 包装结构。

### 2.3 资源与会话分层

- 图片资源独立管理
- 会话负责组织问答上下文
- 画布负责组织题图上的可视化标注

### 2.4 兼容 AI 结构化输出

接口既要支持用户可读文本，也要支持 AI 标注和画布操作数据。

---

## 3. 基础约定

### 3.1 Base URL

建议统一前缀：

```text
/api
```

### 3.2 认证方式

MVP 建议采用 Bearer Token：

```text
Authorization: Bearer <token>
```

### 3.3 数据格式

- 请求体：`application/json`
- 文件上传：`multipart/form-data`
- 流式回复：`text/event-stream`

### 3.4 时间格式

统一使用 ISO 8601 字符串，例如：

```text
2026-06-22T10:30:00Z
```

### 3.5 ID 规则

第一版接口直接返回数据库主键 ID 即可，类型建议前后端统一按字符串或 long 使用，避免 JavaScript 精度问题时可序列化为字符串。

---

## 4. 统一响应结构

### 4.1 普通成功响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

### 4.2 分页响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 100
  }
}
```

### 4.3 错误响应

```json
{
  "code": "SESSION_NOT_FOUND",
  "message": "session does not exist",
  "data": null
}
```

### 4.4 通用错误码建议

- `OK`
- `BAD_REQUEST`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `NOT_FOUND`
- `VALIDATION_ERROR`
- `FILE_TOO_LARGE`
- `UNSUPPORTED_FILE_TYPE`
- `MODEL_NOT_AVAILABLE`
- `MODEL_RESPONSE_INVALID`
- `SESSION_NOT_FOUND`
- `IMAGE_NOT_FOUND`
- `CANVAS_NOT_FOUND`
- `INTERNAL_ERROR`

---

## 5. 鉴权接口

### 5.1 用户注册

`POST /api/auth/register`

请求体：

```json
{
  "username": "student001",
  "password": "12345678",
  "nickname": "小明"
}
```

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "userId": "1",
    "username": "student001",
    "nickname": "小明"
  }
}
```

校验规则：

- `username` 必填，长度 4-64
- `password` 必填，长度至少 8
- `nickname` 可选

### 5.2 用户登录

`POST /api/auth/login`

请求体：

```json
{
  "username": "student001",
  "password": "12345678"
}
```

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "accessToken": "token_value",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "userId": "1",
      "username": "student001",
      "nickname": "小明",
      "avatarUrl": null
    }
  }
}
```

### 5.3 退出登录

`POST /api/auth/logout`

请求体：

```json
{}
```

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": true
}
```

### 5.4 获取当前用户

`GET /api/auth/me`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "userId": "1",
    "username": "student001",
    "nickname": "小明",
    "avatarUrl": null,
    "roleCode": "USER"
  }
}
```

---

## 6. 模型接口

### 6.1 获取可用模型列表

`GET /api/models/enabled`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "modelCode": "gpt-vision-1",
      "displayName": "GPT Vision 1",
      "providerCode": "OPENAI",
      "supportsVision": true,
      "supportsStream": true
    }
  ]
}
```

说明：

- 前端仅展示 `enabled=true` 的模型
- 若模型不支持图片输入，不应出现在当前业务场景可选项中

---

## 7. 图片接口

### 7.1 上传题目图片

`POST /api/images/upload`

请求类型：

- `multipart/form-data`

表单字段：

- `file`: 图片文件，必填

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "imageId": "101",
    "fileName": "math-question.png",
    "mimeType": "image/png",
    "fileSize": 238122,
    "width": 1280,
    "height": 960,
    "accessUrl": "/api/images/101/content"
  }
}
```

### 7.2 获取图片元数据

`GET /api/images/{imageId}`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "imageId": "101",
    "fileName": "math-question.png",
    "mimeType": "image/png",
    "fileSize": 238122,
    "width": 1280,
    "height": 960,
    "accessUrl": "/api/images/101/content",
    "createdAt": "2026-06-22T10:30:00Z"
  }
}
```

### 7.3 获取图片内容

`GET /api/images/{imageId}/content`

返回：

- 图片二进制流

说明：

- 需要校验该图片归属当前用户

### 7.4 删除图片

`DELETE /api/images/{imageId}`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": true
}
```

---

## 8. 会话接口

### 8.1 创建会话

`POST /api/sessions`

请求体：

```json
{
  "imageId": "101",
  "modelCode": "gpt-vision-1",
  "title": "二次函数题讲解",
  "subjectCode": "MATH",
  "gradeLevel": "JUNIOR"
}
```

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "sessionId": "5001",
    "title": "二次函数题讲解",
    "modelCode": "gpt-vision-1",
    "imageId": "101",
    "subjectCode": "MATH",
    "gradeLevel": "JUNIOR",
    "createdAt": "2026-06-22T10:35:00Z"
  }
}
```

说明：

- 创建会话时建议同步初始化 `canvas_document`

### 8.2 获取会话列表

`GET /api/sessions?page=1&pageSize=20`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "items": [
      {
        "sessionId": "5001",
        "title": "二次函数题讲解",
        "modelCode": "gpt-vision-1",
        "imageId": "101",
        "lastMessageAt": "2026-06-22T10:40:00Z",
        "createdAt": "2026-06-22T10:35:00Z"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1
  }
}
```

### 8.3 获取会话详情

`GET /api/sessions/{sessionId}`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "sessionId": "5001",
    "title": "二次函数题讲解",
    "modelCode": "gpt-vision-1",
    "image": {
      "imageId": "101",
      "accessUrl": "/api/images/101/content",
      "width": 1280,
      "height": 960
    },
    "subjectCode": "MATH",
    "gradeLevel": "JUNIOR",
    "createdAt": "2026-06-22T10:35:00Z",
    "updatedAt": "2026-06-22T10:40:00Z"
  }
}
```

### 8.4 删除会话

`DELETE /api/sessions/{sessionId}`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": true
}
```

---

## 9. 消息接口

### 9.1 获取消息列表

`GET /api/sessions/{sessionId}/messages`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": [
    {
      "messageId": "9001",
      "roleCode": "USER",
      "contentText": "请讲一下这道题",
      "annotationSummary": [],
      "createdAt": "2026-06-22T10:36:00Z"
    },
    {
      "messageId": "9002",
      "roleCode": "ASSISTANT",
      "contentText": "我们先一起看题目给了哪些条件。",
      "hintLevel": 1,
      "guidanceStage": "observe",
      "annotationSummary": [
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
      "createdAt": "2026-06-22T10:36:03Z"
    }
  ]
}
```

### 9.2 发送消息并获得普通回复

`POST /api/sessions/{sessionId}/messages`

请求体：

```json
{
  "content": "我还是不知道第一步该做什么",
  "useStream": false,
  "mode": "guided"
}
```

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "userMessage": {
      "messageId": "9003",
      "roleCode": "USER",
      "contentText": "我还是不知道第一步该做什么",
      "createdAt": "2026-06-22T10:37:00Z"
    },
    "assistantMessage": {
      "messageId": "9004",
      "roleCode": "ASSISTANT",
      "contentText": "那我们先只做一件事：把题目中的已知量列出来。",
      "hintLevel": 2,
      "guidanceStage": "analyze",
      "teacherIntent": "guide_next_step",
      "annotationSummary": [
        {
          "type": "arrow",
          "x": 400,
          "y": 100,
          "toX": 270,
          "toY": 120,
          "color": "#1f6feb",
          "label": "先从这里看"
        }
      ],
      "createdAt": "2026-06-22T10:37:03Z"
    }
  }
}
```

说明：

- 后端需按 `docs/ai-prompt-spec.md` 解析 AI 回复
- 标注结构需按 `docs/canvas-protocol.md` 进行校验和转换

### 9.3 发送消息并获得流式回复

`POST /api/sessions/{sessionId}/messages/stream`

请求体：

```json
{
  "content": "继续讲",
  "useStream": true,
  "mode": "guided"
}
```

`mode` 可选值：

- `guided`：引导模式。默认值。首轮识别题意、提取条件并提出一个小问题，后续根据学生回答逐步提示。
- `direct`：直答模式。直接返回题目识别结果、正确答案和完整解题过程。

未传 `mode` 时，后端按 `guided` 处理；该字段同时适用于普通消息接口和流式消息接口。

响应类型：

- `text/event-stream`

事件建议：

- `user-message-created`
- `assistant-text-delta`
- `assistant-annotation`
- `assistant-complete`
- `assistant-error`

SSE 示例：

```text
event: assistant-text-delta
data: {"delta":"我们先"}

event: assistant-text-delta
data: {"delta":"看已知条件。"}

event: assistant-annotation
data: {"type":"rect","x":120,"y":80,"width":220,"height":90,"color":"#ff6b6b","label":"关键条件"}

event: assistant-complete
data: {"messageId":"9005"}
```

---

## 10. 画布接口

### 10.1 获取画布文档

`GET /api/canvas/{sessionId}`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "sessionId": "5001",
    "backgroundImage": {
      "imageId": "101",
      "accessUrl": "/api/images/101/content",
      "width": 1280,
      "height": 960
    },
    "version": 3,
    "snapshot": {
      "schemaVersion": "1.0",
      "background": {
        "imageId": "101",
        "width": 1280,
        "height": 960
      },
      "layers": [
        {
          "layerId": "ai-layer",
          "layerType": "AI",
          "objects": []
        },
        {
          "layerId": "user-layer",
          "layerType": "USER",
          "objects": []
        }
      ]
    },
    "updatedAt": "2026-06-22T10:40:00Z"
  }
}
```

### 10.2 覆盖保存画布快照

`PUT /api/canvas/{sessionId}`

请求体：

```json
{
  "version": 3,
  "snapshot": {
    "schemaVersion": "1.0",
    "background": {
      "imageId": "101",
      "width": 1280,
      "height": 960
    },
    "layers": [
      {
        "layerId": "ai-layer",
        "layerType": "AI",
        "objects": []
      },
      {
        "layerId": "user-layer",
        "layerType": "USER",
        "objects": [
          {
            "objectId": "u1",
            "type": "arrow",
            "x": 100,
            "y": 120,
            "toX": 180,
            "toY": 170,
            "style": {
              "strokeColor": "#f59e0b",
              "strokeWidth": 2
            }
          }
        ]
      }
    ]
  }
}
```

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "sessionId": "5001",
    "version": 4,
    "updatedAt": "2026-06-22T10:41:00Z"
  }
}
```

说明：

- `snapshot` 结构必须遵循 `docs/canvas-protocol.md`
- 建议使用乐观锁版本号避免覆盖冲突

### 10.3 追加画布操作

`POST /api/canvas/{sessionId}/operations`

请求体：

```json
{
  "operations": [
    {
      "operationType": "ADD_OBJECT",
      "layerType": "USER",
      "payload": {
        "objectId": "u2",
        "type": "text",
        "x": 300,
        "y": 220,
        "text": "这里是关键步骤",
        "style": {
          "fontSize": 20,
          "fillColor": "#ef4444"
        }
      }
    }
  ]
}
```

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "accepted": 1,
    "version": 5
  }
}
```

---

## 11. 回放接口

### 11.1 获取回放数据

`GET /api/replay/{sessionId}`

响应体：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "session": {
      "sessionId": "5001",
      "title": "二次函数题讲解",
      "modelCode": "gpt-vision-1"
    },
    "backgroundImage": {
      "imageId": "101",
      "accessUrl": "/api/images/101/content",
      "width": 1280,
      "height": 960
    },
    "timeline": [
      {
        "stepNo": 1,
        "stepType": "MESSAGE",
        "messageId": "9001",
        "roleCode": "USER",
        "contentText": "请讲一下这道题",
        "createdAt": "2026-06-22T10:36:00Z"
      },
      {
        "stepNo": 2,
        "stepType": "AI_ANNOTATION",
        "messageId": "9002",
        "annotation": {
          "type": "rect",
          "x": 120,
          "y": 80,
          "width": 220,
          "height": 90,
          "color": "#ff6b6b",
          "label": "先看这个条件"
        },
        "createdAt": "2026-06-22T10:36:03Z"
      }
    ]
  }
}
```

说明：

- 回放时间线建议把聊天和标注统一为顺序事件流

---

## 12. DTO 建议

建议按业务域拆分 DTO：

- `auth.dto`
- `image.dto`
- `session.dto`
- `message.dto`
- `canvas.dto`
- `replay.dto`
- `model.dto`

返回对象建议分为：

- `Request`
- `Response`
- `Item`
- `Summary`

---

## 13. 校验建议

### 13.1 用户输入

- 文本消息长度限制
- 会话标题长度限制
- 用户昵称长度限制

### 13.2 图片上传

- 文件类型白名单
- 文件大小上限
- 图片尺寸上限

### 13.3 画布快照

- 必须带 `schemaVersion`
- 坐标必须非负
- 图层类型必须合法
- 对象类型必须在支持范围内

### 13.4 模型调用

- 模型编码不能为空
- 所选模型必须启用
- 当前业务必须选择支持视觉的模型

---

## 14. 权限要求

所有业务接口默认需要登录。

当前用户只能访问：

- 自己上传的图片
- 自己的会话
- 自己的消息
- 自己的画布
- 自己的回放数据

管理员接口后续单独规划。

---

## 15. 与其他文档的对应关系

- 会话、消息、图片字段定义以 `docs/database-design.md` 为准
- AI 回复行为、提示等级和结构化字段以 `docs/ai-prompt-spec.md` 为准
- 画布快照、对象结构和操作日志格式以 `docs/canvas-protocol.md` 为准

---

## 16. 后续建议

下一步建议继续补：

- OpenAPI 字段级说明
- 错误码枚举类
- SSE 事件对象定义
- 接口示例测试数据
