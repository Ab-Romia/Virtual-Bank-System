create table outbox (
    id          varchar(64)  primary key,
    topic       varchar(128) not null,
    message_key varchar(128) not null,
    payload     text         not null,
    type        varchar(128) not null,
    created_at  timestamptz  not null,
    sent_at     timestamptz,
    attempts    integer      not null default 0
);

create index idx_outbox_unsent on outbox (created_at) where sent_at is null;
