update model_config
set supports_stream = true,
    updated_at = current_timestamp
where model_code = 'qwen-vl-plus'
  and provider_code = 'BAILIAN';
