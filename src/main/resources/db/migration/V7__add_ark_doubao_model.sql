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
    'doubao-seed-2-1-turbo-260628',
    '豆包 Seed 2.1 Turbo 视觉',
    'ARK',
    true,
    true,
    true,
    2,
    '{"provider":"volcengine-ark","endpointMode":"openai-compatible","vision":true}',
    current_timestamp,
    current_timestamp
where not exists (
    select 1 from model_config where model_code = 'doubao-seed-2-1-turbo-260628'
);
