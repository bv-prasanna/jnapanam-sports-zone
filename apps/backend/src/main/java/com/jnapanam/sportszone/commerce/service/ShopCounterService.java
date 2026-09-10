package com.jnapanam.sportszone.commerce.service;

import static com.jnapanam.sportszone.commerce.dto.ShopCounterDtos.*;

import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopCounterService {
    private final JdbcTemplate jdbc;

    public ShopCounterService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public WalkInSaleResponse sale(UUID actor, WalkInSaleRequest request) {
        String method = request.paymentMethod().trim().toUpperCase();
        if (!Set.of("CASH", "UPI", "CARD").contains(method)) {
            throw new ConflictException("Payment method must be CASH, UPI or CARD");
        }
        long subtotal = 0;
        long tax = 0;
        for (SaleItemRequest item : request.items()) {
            Map<String, Object> row = variant(item.variantId(), true);
            int stock = ((Number) row.get("stock_quantity")).intValue();
            if (item.quantity() > stock) throw new ConflictException(row.get("product_name") + " has only " + stock + " item(s) available");
            long line = ((Number) row.get("price_paise")).longValue() * item.quantity();
            subtotal += line;
            tax += Math.round(line * ((Number) row.get("tax_percent")).doubleValue() / 100d);
        }
        UUID id = UUID.randomUUID();
        String number = "JNZ-POS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long total = subtotal + tax;
        jdbc.update("insert into shop_counter_sales(id,sale_number,customer_user_id,customer_name,mobile,payment_method,payment_reference,payment_status,subtotal_paise,tax_paise,total_paise,created_by) values(?,?,?,?,?,?,?,'PAID',?,?,?,?)",
                id, number, request.customerUserId(), clean(request.customerName()), clean(request.mobile()), method,
                clean(request.paymentReference()), subtotal, tax, total, actor);

        for (SaleItemRequest item : request.items()) {
            Map<String, Object> row = variant(item.variantId(), false);
            long price = ((Number) row.get("price_paise")).longValue();
            long line = price * item.quantity();
            long itemTax = Math.round(line * ((Number) row.get("tax_percent")).doubleValue() / 100d);
            jdbc.update("insert into shop_counter_sale_items(id,sale_id,product_id,variant_id,sku,product_name,variant_label,unit_price_paise,tax_paise,quantity,line_total_paise) values(?,?,?,?,?,?,?,?,?,?,?)",
                    UUID.randomUUID(), id, row.get("product_id"), item.variantId(), row.get("sku"),
                    row.get("product_name"), row.get("variant_label"), price, itemTax, item.quantity(), line + itemTax);
            int changed = jdbc.update("update commerce_variants set stock_quantity=stock_quantity-? where id=? and stock_quantity>=?",
                    item.quantity(), item.variantId(), item.quantity());
            if (changed != 1) throw new ConflictException("Stock changed while processing sale; please retry");
            jdbc.update("insert into commerce_stock_movements(variant_id,quantity_delta,reason,reference_id,actor_id) values(?,?,'COUNTER_SALE',?,?)",
                    item.variantId(), -item.quantity(), id, actor);
        }
        return sale(id);
    }

    @Transactional
    public Map<String, Object> collectOrderPayment(UUID actor, UUID orderId, CounterPaymentRequest request) {
        Map<String, Object> order = order(orderId, true);
        if (!"PAY_AT_PICKUP".equalsIgnoreCase(String.valueOf(order.get("payment_method")))) {
            throw new ConflictException("This order is not a pay-at-counter order");
        }
        if ("PAID".equalsIgnoreCase(String.valueOf(order.get("payment_status")))) return order(orderId, false);
        String method = request.paymentMethod().trim().toUpperCase();
        if (!Set.of("CASH", "UPI", "CARD").contains(method)) throw new ConflictException("Payment method must be CASH, UPI or CARD");
        jdbc.update("update commerce_orders set payment_status='PAID',status=case when status='PLACED' then 'CONFIRMED' else status end,updated_at=now() where id=?", orderId);
        jdbc.update("insert into commerce_stock_movements(variant_id,quantity_delta,reason,reference_id,actor_id) select variant_id,0,'COUNTER_PAYMENT',?,? from commerce_order_items where order_id=? limit 1",
                orderId, actor, orderId);
        return order(orderId, false);
    }

    @Transactional
    public Map<String, Object> fulfil(UUID actor, UUID orderId) {
        Map<String, Object> order = order(orderId, true);
        if (!"PAID".equalsIgnoreCase(String.valueOf(order.get("payment_status")))) {
            throw new ConflictException("Collect payment before handing over the order");
        }
        String status = String.valueOf(order.get("status"));
        if (Set.of("DELIVERED", "CANCELLED").contains(status)) throw new ConflictException("Order cannot be fulfilled from its current status");
        jdbc.update("update commerce_orders set status='DELIVERED',updated_at=now() where id=?", orderId);
        return order(orderId, false);
    }

    @Transactional
    public ReturnResponse createReturn(UUID actor, ReturnRequest request) {
        String sourceType = request.sourceType().trim().toUpperCase();
        String resolution = request.resolution().trim().toUpperCase();
        if (!Set.of("ORDER", "SALE").contains(sourceType)) throw new ConflictException("Source type must be ORDER or SALE");
        if (!Set.of("EXCHANGE", "STORE_CREDIT", "REFUND").contains(resolution)) throw new ConflictException("Resolution must be EXCHANGE, STORE_CREDIT or REFUND");

        SourceItem source = sourceItem(sourceType, request.sourceId(), request.variantId());
        if (request.quantity() > source.quantity()) throw new ConflictException("Return quantity exceeds purchased quantity");
        if (Instant.now().isAfter(source.purchasedAt().plus(source.returnWindowDays(), ChronoUnit.DAYS))) {
            throw new ConflictException("The return window for this item has expired");
        }
        long amount = Math.round((source.unitPricePaise() + source.taxPerUnitPaise()) * request.quantity());
        UUID id = UUID.randomUUID();
        String status = resolution.equals("REFUND") ? "PENDING_APPROVAL" : "COMPLETED";
        jdbc.update("insert into shop_return_requests(id,source_type,source_id,variant_id,quantity,resolution,replacement_variant_id,reason,amount_paise,status,requested_by,completed_at) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                id, sourceType, request.sourceId(), request.variantId(), request.quantity(), resolution,
                request.replacementVariantId(), request.reason().trim(), amount, status, actor,
                status.equals("COMPLETED") ? Timestamp.from(Instant.now()) : null);

        restock(request.variantId(), request.quantity(), id, actor, "RETURN");
        if (resolution.equals("EXCHANGE")) {
            if (request.replacementVariantId() == null) throw new ConflictException("Choose a replacement item for exchange");
            Map<String, Object> replacement = variant(request.replacementVariantId(), true);
            int stock = ((Number) replacement.get("stock_quantity")).intValue();
            if (stock < request.quantity()) throw new ConflictException("Replacement item is out of stock");
            jdbc.update("update commerce_variants set stock_quantity=stock_quantity-? where id=?", request.quantity(), request.replacementVariantId());
            jdbc.update("insert into commerce_stock_movements(variant_id,quantity_delta,reason,reference_id,actor_id) values(?,?,'EXCHANGE_OUT',?,?)",
                    request.replacementVariantId(), -request.quantity(), id, actor);
        } else if (resolution.equals("STORE_CREDIT")) {
            UUID customer = source.customerUserId();
            String mobile = source.mobile();
            if (customer == null && (mobile == null || mobile.isBlank())) throw new ConflictException("Customer identity or mobile is required for store credit");
            jdbc.update("insert into commerce_store_credits(id,customer_user_id,mobile,amount_paise,reference_id,created_by) values(?,?,?,?,?,?)",
                    UUID.randomUUID(), customer, mobile, amount, id, actor);
        }
        return returnById(id);
    }

    @Transactional
    public ReturnResponse approveRefund(UUID approver, UUID id) {
        ReturnResponse current = returnById(id);
        if (!"REFUND".equals(current.resolution()) || !"PENDING_APPROVAL".equals(current.status())) {
            throw new ConflictException("Only pending refund requests can be approved");
        }
        jdbc.update("update shop_return_requests set status='APPROVED',approved_by=?,reviewed_at=now() where id=?", approver, id);
        return returnById(id);
    }

    @Transactional
    public ReturnResponse rejectRefund(UUID approver, UUID id) {
        ReturnResponse current = returnById(id);
        if (!"REFUND".equals(current.resolution()) || !"PENDING_APPROVAL".equals(current.status())) {
            throw new ConflictException("Only pending refund requests can be rejected");
        }
        jdbc.update("update shop_return_requests set status='REJECTED',approved_by=?,reviewed_at=now() where id=?", approver, id);
        return returnById(id);
    }

    @Transactional
    public ReturnResponse completeRefund(UUID approver, UUID id) {
        ReturnResponse current = returnById(id);
        if (!"APPROVED".equals(current.status())) throw new ConflictException("Refund must be approved before completion");
        jdbc.update("update shop_return_requests set status='COMPLETED',approved_by=coalesce(approved_by,?),completed_at=now() where id=?", approver, id);
        return returnById(id);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> returns() {
        return jdbc.query("select * from shop_return_requests order by requested_at desc", (rs, row) -> mapReturn(rs));
    }

    private WalkInSaleResponse sale(UUID id) {
        return jdbc.queryForObject("select * from shop_counter_sales where id=?", (rs, row) -> new WalkInSaleResponse(
                rs.getObject("id", UUID.class), rs.getString("sale_number"), rs.getString("customer_name"),
                rs.getString("mobile"), rs.getString("payment_method"), rs.getString("payment_status"),
                rs.getLong("subtotal_paise"), rs.getLong("tax_paise"), rs.getLong("total_paise"),
                rs.getObject("created_by", UUID.class), rs.getTimestamp("created_at").toInstant()), id);
    }

    private Map<String, Object> variant(UUID variantId, boolean lock) {
        List<Map<String, Object>> rows = jdbc.queryForList("select v.id,v.sku,v.label variant_label,v.price_paise,v.stock_quantity,p.id product_id,p.name product_name,p.tax_percent,p.return_window_days from commerce_variants v join commerce_products p on p.id=v.product_id where v.id=? and v.status='ACTIVE' and p.status='ACTIVE'" + (lock ? " for update of v" : ""), variantId);
        if (rows.isEmpty()) throw new NotFoundException("Product variant not found");
        return rows.getFirst();
    }

    private Map<String, Object> order(UUID orderId, boolean lock) {
        List<Map<String, Object>> rows = jdbc.queryForList("select * from commerce_orders where id=?" + (lock ? " for update" : ""), orderId);
        if (rows.isEmpty()) throw new NotFoundException("Order not found");
        return rows.getFirst();
    }

    private SourceItem sourceItem(String sourceType, UUID sourceId, UUID variantId) {
        String sql;
        if (sourceType.equals("ORDER")) {
            sql = "select oi.quantity,oi.unit_price_paise,oi.tax_paise,o.created_at,u.id customer_user_id,o.mobile,p.return_window_days from commerce_order_items oi join commerce_orders o on o.id=oi.order_id left join users u on u.id=o.user_id join commerce_products p on p.id=oi.product_id where o.id=? and oi.variant_id=?";
        } else {
            sql = "select si.quantity,si.unit_price_paise,si.tax_paise,s.created_at,s.customer_user_id,s.mobile,p.return_window_days from shop_counter_sale_items si join shop_counter_sales s on s.id=si.sale_id join commerce_products p on p.id=si.product_id where s.id=? and si.variant_id=?";
        }
        List<SourceItem> items = jdbc.query(sql, (rs, row) -> {
            int qty = rs.getInt("quantity");
            long tax = rs.getLong("tax_paise");
            return new SourceItem(qty, rs.getLong("unit_price_paise"), qty == 0 ? 0 : tax / qty,
                    rs.getTimestamp("created_at").toInstant(), rs.getObject("customer_user_id", UUID.class),
                    rs.getString("mobile"), rs.getInt("return_window_days"));
        }, sourceId, variantId);
        if (items.isEmpty()) throw new NotFoundException("Purchased item not found");
        return items.getFirst();
    }

    private void restock(UUID variantId, int quantity, UUID reference, UUID actor, String reason) {
        jdbc.update("update commerce_variants set stock_quantity=stock_quantity+? where id=?", quantity, variantId);
        jdbc.update("insert into commerce_stock_movements(variant_id,quantity_delta,reason,reference_id,actor_id) values(?,?,?,?,?)",
                variantId, quantity, reason, reference, actor);
    }

    private ReturnResponse returnById(UUID id) {
        List<ReturnResponse> rows = jdbc.query("select * from shop_return_requests where id=?", (rs, row) -> mapReturn(rs), id);
        if (rows.isEmpty()) throw new NotFoundException("Return request not found");
        return rows.getFirst();
    }

    private ReturnResponse mapReturn(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp reviewed = rs.getTimestamp("reviewed_at");
        Timestamp completed = rs.getTimestamp("completed_at");
        return new ReturnResponse(rs.getObject("id", UUID.class), rs.getString("source_type"),
                rs.getObject("source_id", UUID.class), rs.getObject("variant_id", UUID.class),
                rs.getInt("quantity"), rs.getString("resolution"), rs.getObject("replacement_variant_id", UUID.class),
                rs.getString("reason"), rs.getLong("amount_paise"), rs.getString("status"),
                rs.getObject("requested_by", UUID.class), rs.getObject("approved_by", UUID.class),
                rs.getTimestamp("requested_at").toInstant(), reviewed == null ? null : reviewed.toInstant(),
                completed == null ? null : completed.toInstant());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record SourceItem(int quantity, long unitPricePaise, long taxPerUnitPaise, Instant purchasedAt,
                              UUID customerUserId, String mobile, int returnWindowDays) {}
}
