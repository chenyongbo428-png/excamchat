# 画布协议文档（初稿）

## 1. 文档目的

本文档用于定义题目图片画布的前后端数据协议，统一 AI 标注、用户手工标注、画布快照保存、操作日志记录和对话重放时的结构格式。

本文档是以下模块的直接实现依据：

- 前端画布组件
- AI 标注渲染
- 画布保存/加载接口
- 画布操作日志
- 对话重放

配套文档：

- `docs/requirements.md`
- `docs/api-design.md`
- `docs/database-design.md`
- `docs/ai-prompt-spec.md`

---

## 2. 设计目标

### 2.1 统一坐标系统

所有标注和绘图对象都基于原题图尺寸存储，避免因前端缩放导致数据失真。

### 2.2 AI 与用户共用协议

AI 标注对象与用户标注对象尽量共用同一套对象结构，降低渲染复杂度。

### 2.3 快照与操作并存

- 快照用于快速回显和恢复当前状态
- 操作日志用于回放和审计

### 2.4 易扩展

第一版先支持少量基础对象类型，后续可追加几何辅助线、自由画笔路径等复杂对象。

---

## 3. 坐标与尺寸规则

### 3.1 基准坐标系

- 原点：题图左上角
- `x`：向右增长
- `y`：向下增长
- 所有坐标和尺寸均相对于原图宽高

### 3.2 存储规则

- 存储值使用数字
- 可使用整数或小数
- 不允许使用前端缩放后的屏幕坐标

### 3.3 边界处理

- 坐标小于 `0` 时，渲染前应裁剪到 `0`
- 超出图片宽高时，应裁剪到边界内
- 宽高小于等于 `0` 的对象视为无效对象

---

## 4. 画布快照结构

### 4.1 顶层结构

```json
{
  "schemaVersion": "1.0",
  "background": {
    "imageId": "101",
    "width": 1280,
    "height": 960
  },
  "viewport": {
    "zoom": 1,
    "panX": 0,
    "panY": 0
  },
  "layers": [
    {
      "layerId": "ai-layer",
      "layerType": "AI",
      "visible": true,
      "locked": false,
      "objects": []
    },
    {
      "layerId": "user-layer",
      "layerType": "USER",
      "visible": true,
      "locked": false,
      "objects": []
    }
  ]
}
```

### 4.2 顶层字段说明

- `schemaVersion`
  - 画布协议版本
- `background`
  - 背景题图信息
- `viewport`
  - 当前视口信息，可选
- `layers`
  - 图层列表

---

## 5. 背景对象结构

```json
{
  "imageId": "101",
  "width": 1280,
  "height": 960
}
```

字段说明：

- `imageId`
  - 对应题图资源 ID
- `width`
  - 原图宽度
- `height`
  - 原图高度

---

## 6. 图层结构

### 6.1 图层对象

```json
{
  "layerId": "ai-layer",
  "layerType": "AI",
  "visible": true,
  "locked": false,
  "objects": []
}
```

### 6.2 图层类型

MVP 建议至少支持：

- `AI`
- `USER`

扩展预留：

- `SYSTEM`
- `REPLAY_TEMP`

### 6.3 图层约束

- `AI` 图层主要承载模型生成标注
- `USER` 图层主要承载手工编辑对象
- 回放时可以临时构建只读图层

---

## 7. 通用对象结构

### 7.1 基础对象

所有图形对象建议继承以下基础字段：

```json
{
  "objectId": "obj_001",
  "type": "rect",
  "source": "AI",
  "x": 120,
  "y": 80,
  "rotation": 0,
  "style": {
    "strokeColor": "#ff6b6b",
    "fillColor": "transparent",
    "strokeWidth": 2,
    "opacity": 1
  },
  "meta": {
    "messageId": "9002",
    "annotationId": "ann_001"
  }
}
```

### 7.2 基础字段说明

- `objectId`
  - 当前画布对象唯一 ID
- `type`
  - 对象类型
- `source`
  - 对象来源，建议值：`AI`、`USER`、`SYSTEM`
- `x`、`y`
  - 基准位置
- `rotation`
  - 旋转角度，MVP 可默认为 `0`
- `style`
  - 统一样式定义
- `meta`
  - 扩展信息，例如来源消息 ID、标注 ID

---

## 8. 样式结构

```json
{
  "strokeColor": "#ff6b6b",
  "fillColor": "transparent",
  "strokeWidth": 2,
  "opacity": 1,
  "fontSize": 18,
  "fontFamily": "sans-serif"
}
```

字段说明：

- `strokeColor`
  - 线条颜色
- `fillColor`
  - 填充颜色
- `strokeWidth`
  - 线条粗细
- `opacity`
  - 透明度，范围 0-1
- `fontSize`
  - 文本字号
- `fontFamily`
  - 字体

---

## 9. MVP 对象类型定义

### 9.1 矩形 `rect`

```json
{
  "objectId": "obj_rect_001",
  "type": "rect",
  "source": "AI",
  "x": 120,
  "y": 80,
  "width": 220,
  "height": 90,
  "style": {
    "strokeColor": "#ff6b6b",
    "fillColor": "transparent",
    "strokeWidth": 2,
    "opacity": 1
  },
  "label": "先看这个条件"
}
```

必备字段：

- `width`
- `height`

### 9.2 箭头 `arrow`

```json
{
  "objectId": "obj_arrow_001",
  "type": "arrow",
  "source": "AI",
  "x": 400,
  "y": 100,
  "toX": 270,
  "toY": 120,
  "style": {
    "strokeColor": "#1f6feb",
    "strokeWidth": 2,
    "opacity": 1
  },
  "label": "先从这里看"
}
```

必备字段：

- `toX`
- `toY`

### 9.3 文本 `text`

```json
{
  "objectId": "obj_text_001",
  "type": "text",
  "source": "AI",
  "x": 360,
  "y": 120,
  "text": "这是关键已知",
  "style": {
    "fillColor": "#ef4444",
    "fontSize": 18,
    "opacity": 1
  }
}
```

必备字段：

- `text`

### 9.4 高亮 `highlight`

```json
{
  "objectId": "obj_hl_001",
  "type": "highlight",
  "source": "AI",
  "x": 120,
  "y": 80,
  "width": 220,
  "height": 90,
  "style": {
    "fillColor": "#fde047",
    "opacity": 0.35
  }
}
```

必备字段：

- `width`
- `height`

### 9.5 自由画笔 `path`

第一版如实现自由画笔，建议结构如下：

```json
{
  "objectId": "obj_path_001",
  "type": "path",
  "source": "USER",
  "points": [
    [100, 120],
    [102, 123],
    [105, 128]
  ],
  "style": {
    "strokeColor": "#f59e0b",
    "strokeWidth": 3,
    "opacity": 1
  }
}
```

说明：

- `path` 属于建议支持项
- 如果第一版实现压力较大，可仅在前端内部处理后存为统一对象结构

---

## 10. AI 标注到画布对象的映射规则

### 10.1 映射原则

AI 输出的 `annotations` 不一定等于最终画布对象，后端或前端需做一次协议转换。

### 10.2 推荐映射

`rect` 标注：

- 映射为 `type=rect`

`arrow` 标注：

- 映射为 `type=arrow`

`text` 标注：

- 映射为 `type=text`

`highlight` 标注：

- 映射为 `type=highlight`

### 10.3 来源追踪

AI 生成对象应尽量在 `meta` 中保留：

- `messageId`
- `annotationId`
- `teacherIntent`

---

## 11. 画布操作日志结构

### 11.1 顶层结构

```json
{
  "operationType": "ADD_OBJECT",
  "layerType": "USER",
  "payload": {
    "objectId": "u1",
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
```

### 11.2 支持的操作类型

MVP 建议支持：

- `ADD_OBJECT`
- `UPDATE_OBJECT`
- `DELETE_OBJECT`
- `CLEAR_LAYER`
- `SET_LAYER_VISIBLE`

### 11.3 操作说明

`ADD_OBJECT`

- 新增一个对象到指定图层

`UPDATE_OBJECT`

- 更新某个已有对象

`DELETE_OBJECT`

- 删除某个对象

`CLEAR_LAYER`

- 清空某图层所有对象

`SET_LAYER_VISIBLE`

- 设置图层显示隐藏

---

## 12. 回放协议建议

### 12.1 回放最小事件单元

建议按事件流记录：

- 聊天消息事件
- AI 标注事件
- 用户画布操作事件

### 12.2 回放事件结构

```json
{
  "stepNo": 2,
  "stepType": "AI_ANNOTATION",
  "sessionId": "5001",
  "messageId": "9002",
  "timestamp": "2026-06-22T10:36:03Z",
  "payload": {
    "operationType": "ADD_OBJECT",
    "layerType": "AI",
    "payload": {
      "objectId": "obj_rect_001",
      "type": "rect",
      "x": 120,
      "y": 80,
      "width": 220,
      "height": 90,
      "style": {
        "strokeColor": "#ff6b6b",
        "strokeWidth": 2
      },
      "label": "先看这个条件"
    }
  }
}
```

### 12.3 回放实现建议

- 聊天消息和画布操作按统一 `stepNo` 排序
- 前端按时间线逐条应用事件
- 需要快进时，可从最近快照恢复再补后续事件

---

## 13. 前端渲染约束

### 13.1 不可信字段处理

前端不能直接执行任何非白名单字段。

### 13.2 未知对象类型

- 记录日志
- 忽略渲染
- 不影响其他对象显示

### 13.3 坐标异常对象

- 能裁剪则裁剪
- 无法修复则丢弃并记录

---

## 14. 数据校验规则

### 14.1 快照校验

- `schemaVersion` 必填
- `background.imageId` 必填
- `layers` 不能为空

### 14.2 对象校验

- `objectId` 必填
- `type` 必填
- `source` 必填
- 坐标必须为数字

### 14.3 类型校验

`rect`

- `width`、`height` 必须大于 0

`arrow`

- `toX`、`toY` 必填

`text`

- `text` 不可为空字符串

`highlight`

- `opacity` 建议不大于 0.6

---

## 15. 版本策略

### 15.1 当前版本

当前协议版本：

```text
1.0
```

### 15.2 升级原则

- 新增兼容字段时，保持次版本兼容
- 破坏性调整时，提升主版本

### 15.3 后端兼容建议

- 后端读取快照时识别 `schemaVersion`
- 对旧版本数据提供兼容转换层

---

## 16. 与其他文档的对应关系

- AI 输出中的 `annotations` 来源规范以 `docs/ai-prompt-spec.md` 为准
- 接口请求和响应包装以 `docs/api-design.md` 为准
- 持久化字段与表结构以 `docs/database-design.md` 为准

---

## 17. 开发落地建议

后续开发时建议按以下顺序实现：

1. 先定义 Java DTO 和前端 TypeScript 类型
2. 再实现 AI 标注到画布对象的转换器
3. 再实现画布快照保存与加载
4. 最后接入操作日志与回放

