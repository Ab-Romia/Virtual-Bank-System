create table audit_entries (
    id              varchar(64)    primary key,
    transfer_id     varchar(64)    not null,
    event_type      varchar(16)    not null,
    initiator_id    varchar(64),
    from_account_id varchar(64),
    to_account_id   varchar(64),
    amount          numeric(19, 2),
    currency        varchar(8),
    reason          varchar(256),
    occurred_at     timestamptz    not null,
    recorded_at     timestamptz    not null,
    constraint uq_audit_transfer_event unique (transfer_id, event_type)
);

create index idx_audit_transfer on audit_entries (transfer_id);
