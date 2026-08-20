-- 将数据库和所有表转换为 utf8mb4，支持 emoji 等 4 字节 UTF-8 字符
-- ARK/豆包模型返回内容可能包含 emoji（如 👍），utf8 编码无法存储

ALTER DATABASE cheat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE image_resource CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE model_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE chat_session CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE chat_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE canvas_document CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE canvas_operation CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
