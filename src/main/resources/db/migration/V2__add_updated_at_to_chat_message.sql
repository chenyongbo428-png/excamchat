alter table chat_message
    add column updated_at timestamp;

update chat_message
set updated_at = created_at
where updated_at is null;

alter table chat_message
    modify column updated_at timestamp not null;
