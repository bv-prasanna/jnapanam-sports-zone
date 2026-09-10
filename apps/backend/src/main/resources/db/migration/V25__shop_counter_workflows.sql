alter table commerce_products
    add column if not exists return_window_days integer not null default 7;

create table shop_counter_sales (
    id uuid primary key,
    sale_number varchar(32) not null unique,
    customer_user_id uuid references users(id),
    customer_name varchar(150),
    mobile varchar(20),
    payment_method varchar(20) not null,
    payment_reference varchar(120),
    payment_status varchar(20) not null default 'PAID',
    subtotal_paise bigint not null,
    tax_paise bigint not null default 0,
    total_paise bigint not null,
    created_by uuid not null references users(id),
    created_at timestamptz not null default now()
);

create table shop_counter_sale_items (
    id uuid primary key,
    sale_id uuid not null references shop_counter_sales(id) on delete cascade,
    product_id uuid not null references commerce_products(id),
    variant_id uuid not null references commerce_variants(id),
    sku varchar(80) not null,
    product_name varchar(180) not null,
    variant_label varchar(120) not null,
    unit_price_paise bigint not null,
    tax_paise bigint not null,
    quantity integer not null,
    line_total_paise bigint not null
);

create table shop_return_requests (
    id uuid primary key,
    source_type varchar(20) not null,
    source_id uuid not null,
    variant_id uuid not null references commerce_variants(id),
    quantity integer not null,
    resolution varchar(30) not null,
    replacement_variant_id uuid references commerce_variants(id),
    reason varchar(300) not null,
    amount_paise bigint not null default 0,
    status varchar(30) not null,
    requested_by uuid not null references users(id),
    approved_by uuid references users(id),
    requested_at timestamptz not null default now(),
    reviewed_at timestamptz,
    completed_at timestamptz
);

create table commerce_store_credits (
    id uuid primary key,
    customer_user_id uuid references users(id),
    mobile varchar(20),
    amount_paise bigint not null,
    reference_id uuid not null references shop_return_requests(id),
    created_by uuid not null references users(id),
    created_at timestamptz not null default now()
);

create index idx_shop_counter_sales_created_at on shop_counter_sales(created_at desc);
create index idx_shop_return_requests_status on shop_return_requests(status, requested_at desc);
