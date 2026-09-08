CREATE TABLE commerce_shop_settings (
    singleton_id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton_id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    shop_name VARCHAR(120) NOT NULL,
    shop_name_kn VARCHAR(120),
    hero_title VARCHAR(180) NOT NULL,
    hero_title_kn VARCHAR(180),
    hero_subtitle VARCHAR(300),
    hero_subtitle_kn VARCHAR(300),
    logo_url VARCHAR(1000),
    primary_color VARCHAR(7) NOT NULL,
    secondary_color VARCHAR(7) NOT NULL,
    accent_color VARCHAR(7) NOT NULL,
    background_color VARCHAR(7) NOT NULL,
    text_color VARCHAR(7) NOT NULL,
    loyalty_max_percent NUMERIC(5,2) NOT NULL DEFAULT 30,
    loyalty_point_value_paise BIGINT NOT NULL DEFAULT 100,
    delivery_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_fee_paise BIGINT NOT NULL DEFAULT 0,
    pay_at_facility_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    contact_phone VARCHAR(20),
    contact_email VARCHAR(180),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT commerce_shop_loyalty_percent CHECK (loyalty_max_percent BETWEEN 0 AND 100),
    CONSTRAINT commerce_shop_point_value CHECK (loyalty_point_value_paise > 0),
    CONSTRAINT commerce_shop_delivery_fee CHECK (delivery_fee_paise >= 0)
);

INSERT INTO commerce_shop_settings (
    singleton_id, enabled, shop_name, shop_name_kn, hero_title, hero_title_kn,
    hero_subtitle, hero_subtitle_kn, logo_url, primary_color, secondary_color,
    accent_color, background_color, text_color, loyalty_max_percent,
    loyalty_point_value_paise, delivery_enabled, delivery_fee_paise,
    pay_at_facility_enabled, contact_phone, contact_email
) VALUES (
    TRUE, TRUE, 'Jnapanam Sports Shop', 'ಜ್ಞಾನಪನಂ ಕ್ರೀಡಾ ಅಂಗಡಿ',
    'Sports shop', 'ಕ್ರೀಡಾ ಅಂಗಡಿ',
    'Sportswear, accessories and equipment', 'ಕ್ರೀಡಾ ಉಡುಪು, ಪರಿಕರಗಳು ಮತ್ತು ಉಪಕರಣಗಳು',
    '/images/jnapanam-logo.png', '#20360F', '#6C765F', '#D83A0B', '#FBF7EF', '#172014',
    30, 100, FALSE, 0, TRUE, '+91 82960 22107', 'info@jnapanam.com'
);

ALTER TABLE commerce_categories
    ADD COLUMN loyalty_max_percent NUMERIC(5,2),
    ADD CONSTRAINT commerce_category_loyalty_percent
        CHECK (loyalty_max_percent IS NULL OR loyalty_max_percent BETWEEN 0 AND 100);

ALTER TABLE commerce_products
    ADD COLUMN loyalty_max_percent NUMERIC(5,2),
    ADD CONSTRAINT commerce_product_loyalty_percent
        CHECK (loyalty_max_percent IS NULL OR loyalty_max_percent BETWEEN 0 AND 100);

ALTER TABLE commerce_orders
    ADD COLUMN original_total_paise BIGINT,
    ADD COLUMN loyalty_points_used BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN loyalty_discount_paise BIGINT NOT NULL DEFAULT 0;

UPDATE commerce_orders SET original_total_paise = total_paise WHERE original_total_paise IS NULL;
ALTER TABLE commerce_orders ALTER COLUMN original_total_paise SET NOT NULL;

