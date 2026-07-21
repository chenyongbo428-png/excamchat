update model_config
set supports_stream = false,
    updated_at = current_timestamp
where model_code = 'doubao-seed-2-1-turbo-260628';
