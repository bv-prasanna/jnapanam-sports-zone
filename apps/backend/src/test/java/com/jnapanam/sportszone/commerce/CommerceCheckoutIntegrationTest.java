package com.jnapanam.sportszone.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jnapanam.sportszone.commerce.dto.CommerceDtos.CheckoutRequest;
import com.jnapanam.sportszone.commerce.dto.CommerceDtos.Order;
import com.jnapanam.sportszone.commerce.service.CommerceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceCheckoutIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CommerceService commerce;

    @Test
    void checkoutPersistsOrderItemsUpdatesStockAndClearsCart() throws Exception {
        Fixture fixture = fixture(10, 2, 10_000, 18.0);

        Order order = commerce.checkout(fixture.userId(), pickupRequest("ONLINE"));

        assertThat(order.status()).isEqualTo("PLACED");
        assertThat(order.fulfilmentType()).isEqualTo("PICKUP");
        assertThat(order.subtotalPaise()).isEqualTo(20_000);
        assertThat(order.taxPaise()).isEqualTo(3_600);
        assertThat(order.originalTotalPaise()).isEqualTo(23_600);
        assertThat(order.totalPaise()).isEqualTo(23_600);
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().getFirst().quantity()).isEqualTo(2);
        assertThat(order.items().getFirst().lineTotalPaise()).isEqualTo(23_600);

        assertThat(count("select count(*) from commerce_orders where id=?", order.id())).isEqualTo(1);
        assertThat(count("select count(*) from commerce_order_items where order_id=?", order.id())).isEqualTo(1);
        assertThat(value("select stock_quantity from commerce_variants where id=?", fixture.variantId())).isEqualTo(8);
        assertThat(count("select count(*) from commerce_cart_items where user_id=?", fixture.userId())).isZero();
        assertThat(count("select count(*) from commerce_stock_movements where reference_id=?", order.id())).isEqualTo(1);
    }

    @Test
    void payAtPickupCheckoutCreatesPendingCounterPaymentOrder() throws Exception {
        Fixture fixture = fixture(5, 1, 12_500, 0.0);
        jdbc.update("update commerce_shop_settings set pay_at_facility_enabled=true where singleton_id=true");

        Order order = commerce.checkout(fixture.userId(), pickupRequest("PAY_AT_PICKUP"));

        assertThat(order.paymentMethod()).isEqualTo("PAY_AT_PICKUP");
        assertThat(order.paymentStatus()).isEqualTo("PENDING");
        assertThat(order.status()).isEqualTo("PLACED");
        assertThat(order.totalPaise()).isEqualTo(12_500);
        assertThat(value("select stock_quantity from commerce_variants where id=?", fixture.variantId())).isEqualTo(4);
        assertThat(count("select count(*) from commerce_order_items where order_id=?", order.id())).isEqualTo(1);
    }

    @Test
    void checkoutRollsBackWhenCartQuantityExceedsAvailableStock() throws Exception {
        Fixture fixture = fixture(1, 2, 10_000, 18.0);

        assertThatThrownBy(() -> commerce.checkout(fixture.userId(), pickupRequest("ONLINE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only 1 item");

        assertThat(count("select count(*) from commerce_orders where user_id=?", fixture.userId())).isZero();
        assertThat(value("select stock_quantity from commerce_variants where id=?", fixture.variantId())).isEqualTo(1);
        assertThat(count("select count(*) from commerce_cart_items where user_id=?", fixture.userId())).isEqualTo(1);
        assertThat(count("select count(*) from commerce_stock_movements where variant_id=?", fixture.variantId())).isZero();
    }

    private Fixture fixture(int stock, int cartQuantity, long pricePaise, double taxPercent) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String email = "commerce." + suffix.toLowerCase() + "@example.com";
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Commerce Test\",\"email\":\"" + email
                                + "\",\"mobile\":null,\"password\":\"Test@12345\"}"))
                .andExpect(status().isCreated());

        UUID userId = jdbc.queryForObject("select id from users where email=?", UUID.class, email);
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        jdbc.update("insert into commerce_categories(id,code,name,status) values(?,?,?,'ACTIVE')",
                categoryId, "CAT_" + suffix, "Checkout Test Category");
        jdbc.update("insert into commerce_products(id,category_id,sku,name,tax_percent,status) values(?,?,?,?,?,'ACTIVE')",
                productId, categoryId, "PROD_" + suffix, "Checkout Test Product", taxPercent);
        jdbc.update("insert into commerce_variants(id,product_id,sku,label,price_paise,stock_quantity,low_stock_threshold,status) values(?,?,?,?,?,?,?,'ACTIVE')",
                variantId, productId, "VAR_" + suffix, "Standard", pricePaise, stock, 1);
        jdbc.update("insert into commerce_cart_items(user_id,variant_id,quantity) values(?,?,?)",
                userId, variantId, cartQuantity);

        return new Fixture(userId, variantId);
    }

    private CheckoutRequest pickupRequest(String paymentMethod) {
        return new CheckoutRequest(
                "PICKUP", paymentMethod, "Commerce Customer", "8296022107",
                null, null, null, null, 0L);
    }

    private long count(String sql, Object argument) {
        Long result = jdbc.queryForObject(sql, Long.class, argument);
        return result == null ? 0 : result;
    }

    private int value(String sql, Object argument) {
        Integer result = jdbc.queryForObject(sql, Integer.class, argument);
        return result == null ? 0 : result;
    }

    private record Fixture(UUID userId, UUID variantId) {}
}
