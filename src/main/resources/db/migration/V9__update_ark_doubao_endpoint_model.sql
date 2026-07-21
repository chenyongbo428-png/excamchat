update model_config
set model_code = 'ep-20260721164323-qjbgk',
    display_name = '豆包视觉模型',
    updated_at = current_timestamp
where model_code = 'doubao-seed-2-1-turbo-260628'
  and provider_code = 'ARK';

update chat_session
set model_code = 'ep-20260721164323-qjbgk',
    updated_at = current_timestamp
where model_code = 'doubao-seed-2-1-turbo-260628';
