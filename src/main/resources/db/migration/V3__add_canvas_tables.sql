create table canvas_document (
    id bigint auto_increment primary key,
    session_id bigint not null,
    background_image_id bigint not null,
    snapshot_json longtext not null,
    version_no int not null,
    updated_by_type varchar(32) not null,
    updated_by_id bigint,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_canvas_document_session foreign key (session_id) references chat_session (id),
    constraint fk_canvas_document_image foreign key (background_image_id) references image_resource (id)
);

create unique index uk_canvas_document_session on canvas_document (session_id);

create table canvas_operation (
    id bigint auto_increment primary key,
    session_id bigint not null,
    message_id bigint,
    operator_type varchar(32) not null,
    operator_id bigint,
    operation_type varchar(64) not null,
    layer_type varchar(32) not null,
    payload_json longtext not null,
    sequence_no bigint not null,
    created_at timestamp not null,
    constraint fk_canvas_operation_session foreign key (session_id) references chat_session (id),
    constraint fk_canvas_operation_message foreign key (message_id) references chat_message (id)
);

create index idx_canvas_op_session_id on canvas_operation (session_id);
create index idx_canvas_op_session_seq on canvas_operation (session_id, sequence_no);
create index idx_canvas_op_message_id on canvas_operation (message_id);
