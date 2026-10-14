package com.jnapanam.sportszone.commerce.service;

import static com.jnapanam.sportszone.commerce.dto.CommerceDtos.*;
import com.jnapanam.sportszone.notification.service.NotificationService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommerceAdminService {
    private final JdbcTemplate jdbc;
    private final CommerceService commerce;
    private final NotificationService notifications;

    public CommerceAdminService(JdbcTemplate jdbc, CommerceService commerce, NotificationService notifications) {
        this.jdbc = jdbc;
        this.commerce = commerce;
        this.notifications = notifications;
    }

    public ShopSettings settings() {
        return commerce.settings();
    }

    public void updateSettings(ShopSettingsRequest r) {
        jdbc.update("update commerce_shop_settings set enabled=?,shop_name=?,shop_name_kn=?,hero_title=?,"
                        + "hero_title_kn=?,hero_subtitle=?,hero_subtitle_kn=?,logo_url=?,primary_color=?,"
                        + "secondary_color=?,accent_color=?,background_color=?,text_color=?,"
                        + "loyalty_max_percent=?,loyalty_point_value_paise=?,delivery_enabled=?,"
                        + "delivery_fee_paise=?,pay_at_facility_enabled=?,contact_phone=?,contact_email=?,"
                        + "updated_at=now() where singleton_id=true",
                r.enabled(), r.shopName(), r.shopNameKn(), r.heroTitle(), r.heroTitleKn(),
                r.heroSubtitle(), r.heroSubtitleKn(), r.logoUrl(), r.primaryColor(),
                r.secondaryColor(), r.accentColor(), r.backgroundColor(), r.textColor(),
                r.loyaltyMaxPercent(), r.loyaltyPointValuePaise(), r.deliveryEnabled(),
                r.deliveryFeePaise(), r.payAtFacilityEnabled(), r.contactPhone(), r.contactEmail());
    }

    public List<Category> categories() {
        return jdbc.query("select * from commerce_categories order by display_order,name", (r, n) ->
                new Category(r.getObject("id", UUID.class), r.getString("code"), r.getString("name"),
                        r.getString("name_kn"), r.getString("description"), r.getString("image_url"),
                        r.getInt("display_order"), r.getString("status"),
                        nullableDouble(r, "loyalty_max_percent")));
    }

    public List<Product> products() {
        return jdbc.query("select p.*,c.code category_code from commerce_products p "
                        + "join commerce_categories c on c.id=p.category_id order by p.name",
                (r, n) -> adminProduct(r));
    }

    public UUID category(CategoryRequest r) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into commerce_categories(id,code,name,name_kn,description,image_url,"
                        + "display_order,status,loyalty_max_percent) values(?,?,?,?,?,?,?,?,?)",
                id, r.code().toUpperCase(), r.name(), r.nameKn(), r.description(), r.imageUrl(),
                r.displayOrder(), status(r.status(), "ACTIVE"), r.loyaltyMaxPercent());
        return id;
    }

    public void updateCategory(UUID id, CategoryRequest r) {
        requireChanged(jdbc.update("update commerce_categories set code=?,name=?,name_kn=?,description=?,"
                        + "image_url=?,display_order=?,status=?,loyalty_max_percent=?,updated_at=now() where id=?",
                r.code().toUpperCase(), r.name(), r.nameKn(), r.description(), r.imageUrl(),
                r.displayOrder(), status(r.status(), "ACTIVE"), r.loyaltyMaxPercent(), id), "Category");
    }

    public UUID product(ProductRequest r) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into commerce_products(id,category_id,sku,name,name_kn,description,description_kn,"
                        + "image_url,tax_percent,status,loyalty_max_percent) values(?,?,?,?,?,?,?,?,?,?,?)",
                id, r.categoryId(), r.sku().toUpperCase(), r.name(), r.nameKn(), r.description(),
                r.descriptionKn(), r.imageUrl(), r.taxPercent(), status(r.status(), "DRAFT"),
                r.loyaltyMaxPercent());
        return id;
    }

    public void updateProduct(UUID id, ProductRequest r) {
        requireChanged(jdbc.update("update commerce_products set category_id=?,sku=?,name=?,name_kn=?,"
                        + "description=?,description_kn=?,image_url=?,tax_percent=?,status=?,"
                        + "loyalty_max_percent=?,updated_at=now() where id=?",
                r.categoryId(), r.sku().toUpperCase(), r.name(), r.nameKn(), r.description(),
                r.descriptionKn(), r.imageUrl(), r.taxPercent(), status(r.status(), "DRAFT"),
                r.loyaltyMaxPercent(), id), "Product");
    }

    public UUID variant(UUID product, VariantRequest r) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into commerce_variants(id,product_id,sku,label,price_paise,stock_quantity,"
                        + "low_stock_threshold,status) values(?,?,?,?,?,?,?,?)",
                id, product, r.sku().toUpperCase(), r.label(), r.pricePaise(), r.stockQuantity(),
                r.lowStockThreshold(), status(r.status(), "ACTIVE"));
        return id;
    }

    public void updateVariant(UUID id, VariantRequest r) {
        requireChanged(jdbc.update("update commerce_variants set sku=?,label=?,price_paise=?,"
                        + "low_stock_threshold=?,status=? where id=?",
                r.sku().toUpperCase(), r.label(), r.pricePaise(), r.lowStockThreshold(),
                status(r.status(), "ACTIVE"), id), "Variant");
    }

    @Transactional
    public void stock(UUID actor, UUID variant, StockRequest r) {
        Integer changed = jdbc.queryForObject("update commerce_variants set stock_quantity=stock_quantity+? "
                        + "where id=? and stock_quantity+?>=0 returning stock_quantity",
                Integer.class, r.quantityDelta(), variant, r.quantityDelta());
        if (changed == null) {
            throw new IllegalArgumentException("Stock adjustment would make inventory negative");
        }
        jdbc.update("insert into commerce_stock_movements(variant_id,quantity_delta,reason,actor_id) "
                + "values(?,?,?,?)", variant, r.quantityDelta(), r.reason(), actor);
    }

    @Transactional
    public void orderStatus(UUID id, String status) {
        String next = status.toUpperCase();
        if (!Set.of("PLACED", "CONFIRMED", "PACKED", "READY_FOR_PICKUP", "SHIPPED",
                "DELIVERED", "CANCELLED").contains(next)) {
            throw new IllegalArgumentException("Unsupported order status");
        }
        requireChanged(jdbc.update("update commerce_orders set status=?,updated_at=now() where id=?",
                next, id), "Order");
        var order = jdbc.queryForMap("select user_id,order_number from commerce_orders where id=?", id);
        if (order.get("user_id") instanceof UUID userId) {
            notifications.lifecycle(userId, "COMMERCE", "Shop order " + next.replace('_', ' ').toLowerCase(),
                    "Order " + order.get("order_number") + " is now " + next.replace('_', ' ').toLowerCase() + ".");
        }
    }

    public List<Map<String, Object>> orders() {
        return jdbc.queryForList("select id,order_number,status,fulfilment_type,payment_method,"
                + "payment_status,original_total_paise,loyalty_points_used,loyalty_discount_paise,"
                + "total_paise,recipient_name,mobile,created_at from commerce_orders order by created_at desc");
    }

    private Product adminProduct(ResultSet r) throws SQLException {
        UUID id = r.getObject("id", UUID.class);
        List<Variant> variants = jdbc.query(
                "select * from commerce_variants where product_id=? order by price_paise",
                (v, n) -> new Variant(v.getObject("id", UUID.class), v.getString("sku"),
                        v.getString("label"), v.getLong("price_paise"), v.getInt("stock_quantity"),
                        v.getInt("low_stock_threshold"), v.getString("status")), id);
        return new Product(id, r.getObject("category_id", UUID.class), r.getString("category_code"),
                r.getString("sku"), r.getString("name"), r.getString("name_kn"),
                r.getString("description"), r.getString("description_kn"), r.getString("image_url"),
                r.getDouble("tax_percent"), r.getString("status"),
                nullableDouble(r, "loyalty_max_percent"), variants);
    }

    private Double nullableDouble(ResultSet r, String column) throws SQLException {
        double value = r.getDouble(column);
        return r.wasNull() ? null : value;
    }

    private String status(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.toUpperCase();
    }

    private void requireChanged(int changed, String entity) {
        if (changed == 0) throw new IllegalArgumentException(entity + " not found");
    }
}

