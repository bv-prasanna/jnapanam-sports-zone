create table commerce_categories (
 id uuid primary key default gen_random_uuid(), code varchar(50) not null unique, name varchar(120) not null,
 name_kn varchar(120), description varchar(500), image_url varchar(1000), display_order integer not null default 0,
 status varchar(20) not null default 'ACTIVE', created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table commerce_products (
 id uuid primary key default gen_random_uuid(), category_id uuid not null references commerce_categories(id),
 sku varchar(80) not null unique, name varchar(180) not null, name_kn varchar(180), description varchar(1000),
 description_kn varchar(1000), image_url varchar(1000), tax_percent numeric(5,2) not null default 0,
 status varchar(20) not null default 'DRAFT', created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table commerce_variants (
 id uuid primary key default gen_random_uuid(), product_id uuid not null references commerce_products(id) on delete cascade,
 sku varchar(80) not null unique, label varchar(120) not null, price_paise bigint not null,
 stock_quantity integer not null default 0, low_stock_threshold integer not null default 5,
 status varchar(20) not null default 'ACTIVE', constraint commerce_price_valid check(price_paise>=0),
 constraint commerce_stock_valid check(stock_quantity>=0)
);
create table commerce_cart_items (
 id uuid primary key default gen_random_uuid(), user_id uuid not null references users(id),
 variant_id uuid not null references commerce_variants(id), quantity integer not null,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 unique(user_id,variant_id), constraint commerce_cart_quantity_valid check(quantity between 1 and 99)
);
create table commerce_orders (
 id uuid primary key default gen_random_uuid(), order_number varchar(30) not null unique, user_id uuid not null references users(id),
 status varchar(30) not null default 'PLACED', fulfilment_type varchar(20) not null, payment_method varchar(30) not null,
 payment_status varchar(20) not null default 'PENDING', subtotal_paise bigint not null, tax_paise bigint not null,
 delivery_fee_paise bigint not null default 0, total_paise bigint not null, recipient_name varchar(150) not null,
 mobile varchar(20) not null, address_line varchar(250), city varchar(100), state varchar(100), postal_code varchar(12),
 cancellation_reason varchar(300), created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table commerce_order_items (
 id uuid primary key default gen_random_uuid(), order_id uuid not null references commerce_orders(id) on delete cascade,
 product_id uuid not null, variant_id uuid not null, sku varchar(80) not null, product_name varchar(180) not null,
 variant_label varchar(120) not null, unit_price_paise bigint not null, tax_paise bigint not null, quantity integer not null,
 line_total_paise bigint not null
);
create table commerce_stock_movements (
 id uuid primary key default gen_random_uuid(), variant_id uuid not null references commerce_variants(id),
 quantity_delta integer not null, reason varchar(40) not null, reference_id uuid, actor_id uuid references users(id),
 created_at timestamptz not null default now()
);
create index commerce_product_catalog_idx on commerce_products(category_id,status);
create index commerce_order_user_idx on commerce_orders(user_id,created_at desc);
create index commerce_order_status_idx on commerce_orders(status,created_at);
