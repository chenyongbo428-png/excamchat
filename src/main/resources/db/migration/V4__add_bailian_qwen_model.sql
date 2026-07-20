insert into model_config (
    model_code,
    display_name,
    provider_code,
    supports_vision,
    supports_stream,
    enabled,
    sort_order,
    config_json,
    created_at,
    updated_at
)
select
    'qwen-vl-plus',
    '通义千问 Qwen-VL Plus',
    'BAILIAN',
    true,
    false,
    true,
    4,
    '{"provider":"bailian","endpointMode":"openai-compatible","default":true}',
    current_timestamp,
    current_timestamp
where not exists (
    select 1 from model_config where model_code = 'qwen-vl-plus'
);
