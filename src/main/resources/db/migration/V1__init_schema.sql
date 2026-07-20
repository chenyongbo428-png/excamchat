create table sys_user (
    id bigint auto_increment primary key,
    username varchar(64) not null,
    password_hash varchar(255) not null,
    nickname varchar(64),
    avatar_url varchar(512),
    role_code varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index uk_sys_user_username on sys_user (username);

create table image_resource (
    id bigint auto_increment primary key,
    user_id bigint not null,
    origin_file_name varchar(255),
    storage_key varchar(512) not null,
    access_url varchar(1024),
    mime_type varchar(64) not null,
    file_size bigint not null,
    width int,
    height int,
    sha256 varchar(128),
    storage_type varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_image_resource_user foreign key (user_id) references sys_user (id)
);

create index idx_image_user_id on image_resource (user_id);
create index idx_image_sha256 on image_resource (sha256);

create table model_config (
    id bigint auto_increment primary key,
    model_code varchar(64) not null,
    display_name varchar(128) not null,
    provider_code varchar(64) not null,
    supports_vision boolean not null,
    supports_stream boolean not null,
    enabled boolean not null,
    sort_order int,
    config_json text,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index uk_model_config_code on model_config (model_code);
create index idx_model_config_enabled on model_config (enabled);

create table chat_session (
    id bigint auto_increment primary key,
    user_id bigint not null,
    title varchar(255),
    model_code varchar(64) not null,
    image_id bigint not null,
    subject_code varchar(32),
    grade_level varchar(32),
    status varchar(32) not null,
    last_message_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_chat_session_user foreign key (user_id) references sys_user (id),
    constraint fk_chat_session_image foreign key (image_id) references image_resource (id)
);

create index idx_session_user_id on chat_session (user_id);
create index idx_session_user_status on chat_session (user_id, status);
create index idx_session_last_message_at on chat_session (last_message_at);

create table chat_message (
    id bigint auto_increment primary key,
    session_id bigint not null,
    role_code varchar(32) not null,
    content_type varchar(32) not null,
    content_text text,
    raw_payload_json text,
    annotation_json text,
    hint_level int,
    guidance_stage varchar(32),
    message_status varchar(32) not null,
    token_usage_prompt int,
    token_usage_completion int,
    provider_request_id varchar(128),
    created_at timestamp not null,
    constraint fk_chat_message_session foreign key (session_id) references chat_session (id)
);

create index idx_message_session_id on chat_message (session_id);
create index idx_message_session_created_at on chat_message (session_id, created_at);
create index idx_message_role_code on chat_message (role_code);

insert into model_config (
    model_code, display_name, provider_code, supports_vision, supports_stream, enabled, sort_order, config_json, created_at, updated_at
) values
    ('openai-default', 'OpenAI Default', 'OPENAI', true, true, true, 1, '{}', current_timestamp, current_timestamp),
    ('anthropic-default', 'Anthropic Default', 'ANTHROPIC', true, true, true, 2, '{}', current_timestamp, current_timestamp),
    ('gemini-default', 'Gemini Default', 'GOOGLE', true, true, true, 3, '{}', current_timestamp, current_timestamp);
