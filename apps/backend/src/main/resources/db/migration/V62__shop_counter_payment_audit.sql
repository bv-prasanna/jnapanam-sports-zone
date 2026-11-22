alter table commerce_orders
    add column counter_payment_method varchar(20),
    add column payment_reference varchar(120),
    add column payment_collected_by uuid references users(id),
    add column payment_collected_at timestamptz;

alter table commerce_orders
    add constraint commerce_counter_payment_method_valid
    check (counter_payment_method is null or counter_payment_method in ('CASH', 'UPI', 'CARD'));

create index commerce_orders_payment_collected_idx
    on commerce_orders(payment_collected_at desc)
    where payment_collected_at is not null;
