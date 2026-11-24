-- Harvesta schema (PostgreSQL 16+). Runs on every startup; every statement is idempotent.

create table if not exists users (
    id          bigserial primary key,
    name        varchar(255) not null,
    email       varchar(255) not null unique,
    password    varchar(255) not null,
    role        varchar(20)  not null default 'staff',
    created_at  timestamp    not null default now(),
    updated_at  timestamp    not null default now()
);

create table if not exists categories (
    id          bigserial primary key,
    name        varchar(100) not null unique,
    description text,
    created_at  timestamp not null default now(),
    updated_at  timestamp not null default now()
);

create table if not exists tree_types (
    id              bigserial primary key,
    category_id     bigint not null references categories (id) on delete restrict,
    name            varchar(100) not null,
    scientific_name varchar(150),
    description     text,
    created_at      timestamp not null default now(),
    updated_at      timestamp not null default now(),
    unique (category_id, name)
);

create table if not exists blocks (
    id            bigserial primary key,
    name          varchar(100) not null unique,
    area_sqm      numeric(12, 2),
    location_note varchar(255),
    description   text,
    created_at    timestamp not null default now(),
    updated_at    timestamp not null default now()
);

create table if not exists trees (
    id           bigserial primary key,
    code         varchar(30) not null unique,
    tree_type_id bigint not null references tree_types (id) on delete restrict,
    block_id     bigint not null references blocks (id) on delete restrict,
    created_by   bigint references users (id) on delete set null,
    planted_on   date not null,
    status       varchar(20) not null default 'seedling',
    height_cm    numeric(8, 2),
    notes        text,
    photo        bytea,
    photo_type   varchar(50),
    created_at   timestamp not null default now(),
    updated_at   timestamp not null default now(),
    deleted_at   timestamp
);

create table if not exists care_logs (
    id            bigserial primary key,
    tree_id       bigint not null references trees (id) on delete cascade,
    user_id       bigint references users (id) on delete set null,
    activity_type varchar(30) not null,
    performed_on  date not null,
    next_due_on   date,
    notes         text,
    created_at    timestamp not null default now(),
    updated_at    timestamp not null default now()
);

create table if not exists harvests (
    id           bigserial primary key,
    tree_id      bigint not null references trees (id) on delete cascade,
    user_id      bigint references users (id) on delete set null,
    harvested_on date not null,
    quantity     numeric(10, 2) not null check (quantity > 0),
    unit         varchar(10) not null,
    grade        varchar(20) not null,
    notes        text,
    created_at   timestamp not null default now(),
    updated_at   timestamp not null default now()
);

create table if not exists expenses (
    id          bigserial primary key,
    block_id    bigint references blocks (id) on delete set null,
    user_id     bigint references users (id) on delete set null,
    category    varchar(30) not null,
    amount      numeric(14, 2) not null check (amount > 0),
    spent_on    date not null,
    description varchar(255),
    created_at  timestamp not null default now(),
    updated_at  timestamp not null default now()
);

create table if not exists sales (
    id           bigserial primary key,
    harvest_id   bigint not null references harvests (id) on delete restrict,
    user_id      bigint references users (id) on delete set null,
    buyer_name   varchar(150) not null,
    sold_on      date not null,
    quantity     numeric(10, 2) not null check (quantity > 0),
    unit_price   numeric(14, 2) not null check (unit_price > 0),
    total_amount numeric(14, 2) not null,
    notes        text,
    created_at   timestamp not null default now(),
    updated_at   timestamp not null default now()
);

create index if not exists trees_block_id_index on trees (block_id);
create index if not exists trees_tree_type_id_index on trees (tree_type_id);
create index if not exists trees_status_index on trees (status);
create index if not exists care_logs_tree_performed_index on care_logs (tree_id, performed_on);
create index if not exists care_logs_next_due_on_index on care_logs (next_due_on);
create index if not exists harvests_tree_harvested_index on harvests (tree_id, harvested_on);
create index if not exists expenses_spent_on_index on expenses (spent_on);
create index if not exists sales_sold_on_index on sales (sold_on);
