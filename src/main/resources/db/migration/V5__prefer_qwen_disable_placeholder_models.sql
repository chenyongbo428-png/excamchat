update model_config
set enabled = false,
    updated_at = current_timestamp
where model_code in ('openai-default', 'anthropic-default', 'gemini-default');

update model_config
set enabled = true,
    sort_order = 1,
    updated_at = current_timestamp
where model_code = 'qwen-vl-plus';
