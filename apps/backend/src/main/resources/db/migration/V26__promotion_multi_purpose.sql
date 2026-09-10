create table promotion_purposes (
    promotion_id uuid not null references promotions(id) on delete cascade,
    purpose varchar(20) not null,
    primary key (promotion_id, purpose)
);

insert into promotion_purposes(promotion_id,purpose)
select id,purpose from promotions where purpose is not null
on conflict do nothing;
