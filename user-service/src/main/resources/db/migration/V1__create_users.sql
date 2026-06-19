create table users (
    id            varchar(64)  primary key,
    username      varchar(128) not null unique,
    email         varchar(256),
    password_hash varchar(256) not null,
    full_name     varchar(256),
    created_at    timestamptz  not null
);
