create table notification_template_channels (
    template_id uuid not null references notification_templates(id) on delete cascade,
    channel varchar(20) not null,
    primary key (template_id, channel)
);

insert into notification_template_channels(template_id, channel)
select id, channel from notification_templates
on conflict do nothing;
