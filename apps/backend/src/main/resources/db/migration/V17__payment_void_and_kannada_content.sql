ALTER TABLE payment_orders
    ADD COLUMN voided_at TIMESTAMPTZ,
    ADD COLUMN void_reason VARCHAR(300);

ALTER TABLE public_events
    ADD COLUMN title_kn VARCHAR(180),
    ADD COLUMN description_kn TEXT;

ALTER TABLE announcements
    ADD COLUMN title_kn VARCHAR(180),
    ADD COLUMN body_kn TEXT;

ALTER TABLE content_pages
    ADD COLUMN title_kn VARCHAR(180),
    ADD COLUMN summary_kn VARCHAR(500),
    ADD COLUMN body_kn TEXT;

ALTER TABLE gallery_items
    ADD COLUMN title_kn VARCHAR(180),
    ADD COLUMN alt_text_kn VARCHAR(250);
