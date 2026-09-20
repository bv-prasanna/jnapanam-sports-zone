package com.jnapanam.sportszone.commerce.service;

import static com.jnapanam.sportszone.commerce.dto.CommerceDtos.*;

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
public class CommerceService {
    private final JdbcTemplate jdbc;

    public CommerceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Category> categories() {
        return jdbc.query(
                "select * from commerce_categories where status='ACTIVE' order by display_order,name",
                (r, n) -> new Category(
                        r.getObject("id", UUID.class), r.getString("code"), r.getString("name"),
                        r.getString("name_kn"), r.getString("description"), r.getString("image_url"),
                        r.getInt("display_order"), r.getString("status"),
                        nullableDouble(r, "loyalty_max_percent")));
    }

    public ShopSettings settings() {
        List<ShopSettings> rows = jdbc.query("select * from commerce_shop_settings where singleton_id=true",
                (r, n) -> new ShopSettings(
                        r.getBoolean("enabled"), r.getString("shop_name"), r.getString("shop_name_kn"),
                        r.getString("hero_title"), r.getString("hero_title_kn"),
                        r.getString("hero_subtitle"), r.getString("hero_subtitle_kn"),
                        r.getString("logo_url"), r.getString("primary_color"),
                        r.getString("secondary_color"), r.getString("accent_color"),
                        r.getString("background_color"), r.getString("text_color"),
                        r.getDouble("loyalty_max_percent"), r.getLong("loyalty_point_value_paise"),
                        r.getBoolean("delivery_enabled"), r.getLong("delivery_fee_paise"),
                        r.getBoolean("pay_at_facility_enabled"), r.getString("contact_phone"),
                        r.getString("contact_email")));
        if (!rows.isEmpty()) return rows.get(0);
        jdbc.update("insert into commerce_shop_settings(singleton_id,enabled,shop_name,shop_name_kn,hero_title,hero_title_kn,hero_subtitle,hero_subtitle_kn,logo_url,primary_color,secondary_color,accent_color,background_color,text_color,loyalty_max_percent,loyalty_point_value_paise,delivery_enabled,delivery_fee_paise,pay_at_facility_enabled,contact_phone,contact_email) values(true,true,'Jnapanam Sports Shop','ಜ್ಞಾನಪನಂ ಕ್ರೀಡಾ ಅಂಗಡಿ','Sports shop','ಕ್ರೀಡಾ ಅಂಗಡಿ','Sportswear, accessories and equipment','ಕ್ರೀಡಾ ಉಡುಪು, ಪರಿಕರಗಳು ಮತ್ತು ಉಪಕರಣಗಳು','/images/jnapanam-logo.png','#20360F','#6C765F','#D83A0B','#FBF7EF','#172014',30,100,false,0,true,'+91 82960 22107','info@jnapanam.com') on conflict(singleton_id) do nothing");
        return settings();
    }

    public List<Product> products(String category) {
        String sql = "select p.*,c.code category_code from commerce_products p "
                + "join commerce_categories c on c.id=p.category_id "
                + "where p.status='ACTIVE' and c.status='ACTIVE' "
                + "and (? is null or upper(c.code)=upper(?)) order by p.name";
        return jdbc.query(sql, (r, n) -> product(r), category, category);
    }

    public Product product(UUID id) {
        return jdbc.queryForObject(
                "select p.*,c.code category_code from commerce_products p "
                        + "join commerce_categories c on c.id=p.category_id "
                        + "where p.id=? and p.status='ACTIVE'",
                (r, n) -> product(r), id);
    }

    private Product product(ResultSet r) throws SQLException {
        UUID id = r.getObject("id", UUID.class);
        List<Variant> variants = jdbc.query(
                "select * from commerce_variants where product_id=? and status='ACTIVE' order by price_paise",
                (v, n) -> new Variant(v.getObject("id", UUID.class), v.getString("sku"),
                        v.getString("label"), v.getLong("price_paise"), v.getInt("stock_quantity"),
                        v.getInt("low_stock_threshold"),
                        v.getString("status")), id);
        return new Product(id, r.getObject("category_id", UUID.class), r.getString("category_code"),
                r.getString("sku"), r.getString("name"),
                r.getString("name_kn"), r.getString("description"), r.getString("description_kn"),
                r.getString("image_url"), r.getDouble("tax_percent"), r.getString("status"),
                nullableDouble(r, "loyalty_max_percent"), variants);
    }

    public Cart cart(UUID user) {
        String sql = "select ci.id,ci.variant_id,p.name,v.label,p.image_url,v.price_paise,"
                + "ci.quantity,v.stock_quantity from commerce_cart_items ci "
                + "join commerce_variants v on v.id=ci.variant_id "
                + "join commerce_products p on p.id=v.product_id "
                + "where ci.user_id=? order by ci.created_at";
        List<CartItem> items = jdbc.query(sql,
                (r, n) -> new CartItem(r.getObject("id", UUID.class), r.getObject("variant_id", UUID.class),
                        r.getString("name"), r.getString("label"), r.getString("image_url"),
                        r.getLong("price_paise"), r.getInt("quantity"),
                        r.getLong("price_paise") * r.getInt("quantity"), r.getInt("stock_quantity")), user);
        return new Cart(items, items.stream().mapToLong(CartItem::lineTotalPaise).sum());
    }

    public Cart putCart(UUID user, CartRequest request) {
        Integer stock = jdbc.queryForObject(
                "select stock_quantity from commerce_variants where id=? and status='ACTIVE'",
                Integer.class, request.variantId());
        if (stock == null || request.quantity() > stock) {
            throw new IllegalArgumentException("Only " + (stock == null ? 0 : stock) + " item(s) available");
        }
        String sql = "insert into commerce_cart_items(user_id,variant_id,quantity) values(?,?,?) "
                + "on conflict(user_id,variant_id) do update set "
                + "quantity=excluded.quantity,updated_at=now()";
        jdbc.update(sql, user, request.variantId(), request.quantity());
        return cart(user);
    }

    public Cart removeCart(UUID user, UUID variant) {
        jdbc.update("delete from commerce_cart_items where user_id=? and variant_id=?", user, variant);
        return cart(user);
    }

    public LoyaltyQuote loyaltyQuote(UUID user) {
        List<Map<String, Object>> rows = checkoutRows(user, false);
        ShopSettings settings = settings();
        long maximumDiscount = maximumLoyaltyDiscount(rows, settings.loyaltyMaxPercent());
        long balance = loyaltyBalance(user);
        long maximumPoints = Math.min(balance, maximumDiscount / settings.loyaltyPointValuePaise());
        return new LoyaltyQuote(balance, maximumPoints, settings.loyaltyPointValuePaise(),
                maximumPoints * settings.loyaltyPointValuePaise());
    }

    @Transactional
    public Order checkout(UUID user, CheckoutRequest r) {
        String fulfil = r.fulfilmentType().toUpperCase();
        if (!Set.of("PICKUP", "DELIVERY").contains(fulfil)) {
            throw new IllegalArgumentException("Fulfilment type must be PICKUP or DELIVERY");
        }
        if (fulfil.equals("DELIVERY")
                && (blank(r.addressLine()) || blank(r.city()) || blank(r.state()) || blank(r.postalCode()))) {
            throw new IllegalArgumentException("Delivery address, city, state and postal code are required");
        }

        List<Map<String, Object>> rows = checkoutRows(user, true);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        long subtotal = 0;
        long tax = 0;
        for (Map<String, Object> row : rows) {
            int quantity = (Integer) row.get("quantity");
            int stock = (Integer) row.get("stock_quantity");
            if (quantity > stock) {
                throw new IllegalArgumentException(row.get("name") + " has only " + stock + " item(s) available");
            }
            long line = ((Number) row.get("price_paise")).longValue() * quantity;
            subtotal += line;
            tax += Math.round(line * ((Number) row.get("tax_percent")).doubleValue() / 100d);
        }

        ShopSettings settings = settings();
        if (!settings.enabled()) {
            throw new IllegalArgumentException("The shop is currently unavailable");
        }
        if (fulfil.equals("DELIVERY") && !settings.deliveryEnabled()) {
            throw new IllegalArgumentException("Delivery is currently unavailable");
        }
        if (r.paymentMethod().equalsIgnoreCase("PAY_AT_PICKUP") && !settings.payAtFacilityEnabled()) {
            throw new IllegalArgumentException("Pay at facility is currently unavailable");
        }
        long delivery = fulfil.equals("DELIVERY") ? settings.deliveryFeePaise() : 0;
        long originalTotal = subtotal + tax + delivery;
        long requestedPoints = r.loyaltyPoints() == null ? 0 : r.loyaltyPoints();
        long balance = loyaltyBalance(user);
        long maximumDiscount = maximumLoyaltyDiscount(rows, settings.loyaltyMaxPercent());
        long maximumPoints = Math.min(balance, maximumDiscount / settings.loyaltyPointValuePaise());
        if (requestedPoints > maximumPoints) {
            throw new IllegalArgumentException("You can use a maximum of " + maximumPoints + " loyalty points");
        }
        long loyaltyDiscount = requestedPoints * settings.loyaltyPointValuePaise();
        long total = originalTotal - loyaltyDiscount;
        UUID id = UUID.randomUUID();
        String number = "JNZ-SHOP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String method = r.paymentMethod().toUpperCase();
        String paymentStatus = "PENDING";
        String orderSql = "insert into commerce_orders(id,order_number,user_id,status,fulfilment_type,"
                + "payment_method,payment_status,subtotal_paise,tax_paise,delivery_fee_paise,"
                + "original_total_paise,loyalty_points_used,loyalty_discount_paise,total_paise,"
                + "recipient_name,mobile,address_line,city,state,postal_code) "
                + "values(?,?,?,'PLACED',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbc.update(orderSql, id, number, user, fulfil, method, paymentStatus, subtotal, tax, delivery,
                originalTotal, requestedPoints, loyaltyDiscount, total,
                r.recipientName(), r.mobile(), r.addressLine(), r.city(), r.state(), r.postalCode());

        if (requestedPoints > 0) {
            int changed = jdbc.update("update loyalty_wallets set points_balance=points_balance-?,"
                            + "lifetime_redeemed=lifetime_redeemed+?,updated_at=now(),version=version+1 "
                            + "where user_id=? and points_balance>=?",
                    requestedPoints, requestedPoints, user, requestedPoints);
            if (changed != 1) {
                throw new IllegalArgumentException("Insufficient loyalty points");
            }
            jdbc.update("insert into loyalty_transactions(id,user_id,transaction_type,points,description,created_at) "
                            + "values(?,?,'REDEEM',?,?,now())",
                    UUID.randomUUID(), user, requestedPoints, "Redeemed on shop order " + number);
        }

        for (Map<String, Object> row : rows) {
            int quantity = (Integer) row.get("quantity");
            long price = ((Number) row.get("price_paise")).longValue();
            long line = price * quantity;
            long itemTax = Math.round(line * ((Number) row.get("tax_percent")).doubleValue() / 100d);
            UUID variant = (UUID) row.get("variant_id");
            jdbc.update("insert into commerce_order_items(order_id,product_id,variant_id,sku,product_name,"
                            + "variant_label,unit_price_paise,tax_paise,quantity,line_total_paise) "
                            + "values(?,?,?,?,?,?,?,?,?,?)",
                    id, row.get("product_id"), variant, row.get("sku"), row.get("name"), row.get("label"),
                    price, itemTax, quantity, line + itemTax);
            jdbc.update("update commerce_variants set stock_quantity=stock_quantity-? where id=?", quantity, variant);
            jdbc.update("insert into commerce_stock_movements(variant_id,quantity_delta,reason,reference_id,actor_id) "
                            + "values(?,?,'ORDER',?,?)",
                    variant, -quantity, id, user);
        }
        jdbc.update("delete from commerce_cart_items where user_id=?", user);
        return order(user, id);
    }

    public List<Order> orders(UUID user) {
        return jdbc.query("select id from commerce_orders where user_id=? order by created_at desc",
                (r, n) -> order(user, r.getObject(1, UUID.class)), user);
    }

    public Order order(UUID user, UUID id) {
        return jdbc.queryForObject("select * from commerce_orders where id=? and user_id=?",
                (r, n) -> mapOrder(r), id, user);
    }

    private Order mapOrder(ResultSet r) throws SQLException {
        UUID id = r.getObject("id", UUID.class);
        List<OrderItem> items = jdbc.query("select * from commerce_order_items where order_id=?",
                (x, n) -> new OrderItem(x.getString("sku"), x.getString("product_name"),
                        x.getString("variant_label"), x.getLong("unit_price_paise"), x.getInt("quantity"),
                        x.getLong("tax_paise"), x.getLong("line_total_paise")), id);
        return new Order(id, r.getString("order_number"), r.getString("status"),
                r.getString("fulfilment_type"), r.getString("payment_method"), r.getString("payment_status"),
                r.getLong("subtotal_paise"), r.getLong("tax_paise"), r.getLong("delivery_fee_paise"),
                r.getLong("original_total_paise"), r.getLong("loyalty_points_used"),
                r.getLong("loyalty_discount_paise"), r.getLong("total_paise"),
                r.getString("recipient_name"), r.getString("mobile"),
                r.getString("address_line"), r.getString("city"), r.getString("state"),
                r.getString("postal_code"), r.getTimestamp("created_at").toInstant(), items);
    }

    @Transactional
    public Order cancel(UUID user, UUID id, String reason) {
        Order order = order(user, id);
        if (!Set.of("PLACED", "CONFIRMED").contains(order.status())) {
            throw new IllegalArgumentException("This order cannot be cancelled");
        }
        jdbc.update("update commerce_orders set status='CANCELLED',cancellation_reason=?,updated_at=now() where id=?",
                reason, id);
        jdbc.queryForList("select variant_id,quantity from commerce_order_items where order_id=?", id)
                .forEach(row -> {
                    UUID variant = (UUID) row.get("variant_id");
                    int quantity = (Integer) row.get("quantity");
                    jdbc.update("update commerce_variants set stock_quantity=stock_quantity+? where id=?",
                            quantity, variant);
                    jdbc.update("insert into commerce_stock_movements(variant_id,quantity_delta,reason,reference_id,actor_id) "
                                    + "values(?,?,'CANCELLATION',?,?)",
                            variant, quantity, id, user);
                });
        return order(user, id);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private List<Map<String, Object>> checkoutRows(UUID user, boolean lock) {
        String sql = "select ci.variant_id,ci.quantity,v.stock_quantity,v.price_paise,v.sku,v.label,"
                + "p.id product_id,p.name,p.tax_percent,p.loyalty_max_percent product_loyalty_percent,"
                + "c.loyalty_max_percent category_loyalty_percent from commerce_cart_items ci "
                + "join commerce_variants v on v.id=ci.variant_id "
                + "join commerce_products p on p.id=v.product_id "
                + "join commerce_categories c on c.id=p.category_id "
                + "where ci.user_id=? order by ci.created_at" + (lock ? " for update of v" : "");
        return jdbc.queryForList(sql, user);
    }

    private long loyaltyBalance(UUID user) {
        Long value = jdbc.queryForObject(
                "select coalesce((select points_balance from loyalty_wallets where user_id=?),0)",
                Long.class, user);
        return value == null ? 0 : value;
    }

    private long maximumLoyaltyDiscount(List<Map<String, Object>> rows, double shopPercent) {
        long maximum = 0;
        for (Map<String, Object> row : rows) {
            Number productPercent = (Number) row.get("product_loyalty_percent");
            Number categoryPercent = (Number) row.get("category_loyalty_percent");
            double percent = productPercent != null ? productPercent.doubleValue()
                    : categoryPercent != null ? categoryPercent.doubleValue() : shopPercent;
            long line = ((Number) row.get("price_paise")).longValue() * (Integer) row.get("quantity");
            maximum += (long) Math.floor(line * percent / 100d);
        }
        return maximum;
    }

    private Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }
}
