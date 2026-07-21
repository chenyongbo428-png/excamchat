update model_config
set supports_stream = true,
    updated_at = current_timestamp
where model_code = 'ep-20260721164323-qjbgk'
  and provider_code = 'ARK';
