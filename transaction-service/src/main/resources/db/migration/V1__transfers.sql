create table transfers (
    id              varchar(64)    primary key,
    initiator_id    varchar(64)    not null,
    from_account_id varchar(64)    not null,
    to_account_id   varchar(64)    not null,
    amount          numeric(19, 2) not null,
    currency        varchar(8)     not null,
    status          varchar(16)    not null,
    failure_reason  varchar(256),
    idempotency_key varchar(128)   not null unique,
    created_at      timestamptz    not null,
    updated_at      timestamptz    not null,
    version         bigint         not null
);

create index idx_transfers_initiator on transfers (initiator_id, created_at desc);

-- Idempotency inbox for the transfer.events consumer: one terminal outcome per transfer.
create table processed_events (
    transfer_id  varchar(64) primary key,
    processed_at timestamptz not null
);
