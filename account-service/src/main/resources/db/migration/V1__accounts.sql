create table accounts (
    id             varchar(64)    primary key,
    owner_id       varchar(64)    not null,
    account_number varchar(32)    not null unique,
    type           varchar(16)    not null,
    balance        numeric(19, 2) not null check (balance >= 0),
    currency       varchar(8)     not null,
    status         varchar(16)    not null,
    created_at     timestamptz    not null,
    updated_at     timestamptz    not null,
    version        bigint         not null
);

create index idx_accounts_owner_id on accounts (owner_id);

create table processed_events (
    transfer_id  varchar(64) primary key,
    processed_at timestamptz not null
);
