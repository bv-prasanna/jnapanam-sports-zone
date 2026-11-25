package com.jnapanam.sportszone.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jnapanam.sportszone.commerce.dto.CommerceDtos.CheckoutRequest;
import com.jnapanam.sportszone.commerce.dto.CommerceDtos.Order;
import com.jnapanam.sportszone.commerce.dto.ShopCounterDtos.CounterPaymentRequest;
import com.jnapanam.sportszone.commerce.service.CommerceAdminService;
import com.jnapanam.sportszone.commerce.service.CommerceService;
import com.jnapanam.sportszone.commerce.service.ShopCounterService;
import com.jnapanam.sportszone.common.exception.ConflictException;
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
class CommerceAdminOrderLifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CommerceService commerce;
    @Autowired CommerceAdminService admin;
    @Autowired ShopCounterService counter;

    @Test
    void adminCancellationRestoresUnpaidStockExactlyOnce() throws Exception {
        Fixture fixture=fixture("PICKUP");
        admin.orderStatus(fixture.userId(),fixture.orderId(),"CANCELLED");
        assertThat(text("select status from commerce_orders where id=?",fixture.orderId())).isEqualTo("CANCELLED");
        assertThat(number("select stock_quantity from commerce_variants where id=?",fixture.variantId())).isEqualTo(5);
        assertThat(number("select count(*) from commerce_stock_movements where reference_id=? and reason='CANCELLATION'",fixture.orderId())).isEqualTo(1);

        admin.orderStatus(fixture.userId(),fixture.orderId(),"CANCELLED");
        assertThat(number("select stock_quantity from commerce_variants where id=?",fixture.variantId())).isEqualTo(5);
        assertThat(number("select count(*) from commerce_stock_movements where reference_id=? and reason='CANCELLATION'",fixture.orderId())).isEqualTo(1);
    }

    @Test
    void adminCancellationRestoresRedeemedShopLoyaltyPointsExactlyOnce() throws Exception {
        Fixture fixture=fixture("PICKUP",3);
        assertThat(number("select points_balance from loyalty_wallets where user_id=?",fixture.userId())).isEqualTo(2);
        admin.orderStatus(fixture.userId(),fixture.orderId(),"CANCELLED");
        assertThat(number("select points_balance from loyalty_wallets where user_id=?",fixture.userId())).isEqualTo(5);
        assertThat(number("select count(*) from loyalty_transactions where user_id=? and transaction_type='REVERSAL'",fixture.userId())).isEqualTo(1);
    }

    @Test
    void adminCannotCancelPaidOrderOrRestoreItsStock() throws Exception {
        Fixture fixture=fixture("PICKUP");
        counter.collectOrderPayment(fixture.userId(),fixture.orderId(),new CounterPaymentRequest("CASH",null));

        assertThatThrownBy(()->admin.orderStatus(fixture.userId(),fixture.orderId(),"CANCELLED"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already been paid");
        assertThat(text("select status from commerce_orders where id=?",fixture.orderId())).isEqualTo("CONFIRMED");
        assertThat(number("select stock_quantity from commerce_variants where id=?",fixture.variantId())).isEqualTo(4);
    }

    @Test
    void counterHandoverRequiresPaidReadyForPickupOrder() throws Exception {
        Fixture fixture=fixture("PICKUP");
        counter.collectOrderPayment(fixture.userId(),fixture.orderId(),new CounterPaymentRequest("CASH",null));
        assertThatThrownBy(()->counter.fulfil(fixture.userId(),fixture.orderId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("READY_FOR_PICKUP");

        admin.orderStatus(fixture.userId(),fixture.orderId(),"PACKED");
        admin.orderStatus(fixture.userId(),fixture.orderId(),"READY_FOR_PICKUP");
        var delivered=counter.fulfil(fixture.userId(),fixture.orderId());
        assertThat(delivered.get("status")).isEqualTo("DELIVERED");
        assertThat(text("select status from commerce_orders where id=?",fixture.orderId())).isEqualTo("DELIVERED");
    }

    @Test
    void adminCannotSkipFulfilmentStagesOrDeliverBeforePayment() throws Exception {
        Fixture fixture=fixture("PICKUP");
        assertThatThrownBy(()->admin.orderStatus(fixture.userId(),fixture.orderId(),"DELIVERED"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Invalid order transition");
        admin.orderStatus(fixture.userId(),fixture.orderId(),"CONFIRMED");
        admin.orderStatus(fixture.userId(),fixture.orderId(),"PACKED");
        admin.orderStatus(fixture.userId(),fixture.orderId(),"READY_FOR_PICKUP");
        assertThatThrownBy(()->admin.orderStatus(fixture.userId(),fixture.orderId(),"DELIVERED"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Collect payment");
        assertThat(text("select status from commerce_orders where id=?",fixture.orderId())).isEqualTo("READY_FOR_PICKUP");
    }

    private Fixture fixture(String fulfilment) throws Exception { return fixture(fulfilment,0); }

    private Fixture fixture(String fulfilment,long loyaltyPoints) throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase();
        String email="commerce.admin."+suffix.toLowerCase()+"@example.com";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Commerce Admin Test\",\"email\":\""+email+"\",\"mobile\":null,\"password\":\"Test@12345\"}"))
                .andExpect(status().isCreated());
        UUID user=jdbc.queryForObject("select id from users where email=?",UUID.class,email);
        UUID category=UUID.randomUUID(),product=UUID.randomUUID(),variant=UUID.randomUUID();
        jdbc.update("insert into commerce_categories(id,code,name,status) values(?,?,?,'ACTIVE')",category,"ADM_"+suffix,"Admin Test Category");
        jdbc.update("insert into commerce_products(id,category_id,sku,name,tax_percent,status) values(?,?,?,?,0,'ACTIVE')",product,category,"ADP_"+suffix,"Admin Test Product");
        jdbc.update("insert into commerce_variants(id,product_id,sku,label,price_paise,stock_quantity,low_stock_threshold,status) values(?,?,?,?,?,?,1,'ACTIVE')",variant,product,"ADV_"+suffix,"Standard",12500,5);
        jdbc.update("insert into commerce_cart_items(user_id,variant_id,quantity) values(?,?,1)",user,variant);
        if(loyaltyPoints>0)jdbc.update("insert into loyalty_wallets(user_id,points_balance,lifetime_earned,lifetime_redeemed,updated_at,version) values(?,?,?,0,now(),0)",user,loyaltyPoints+2,loyaltyPoints+2);
        jdbc.update("update commerce_shop_settings set pay_at_facility_enabled=true,delivery_enabled=true where singleton_id=true");
        CheckoutRequest request=fulfilment.equals("DELIVERY")
                ?new CheckoutRequest("DELIVERY","PAY_AT_PICKUP","Customer","8296022107","12 Main Road","Holalkere","Karnataka","577526",loyaltyPoints)
                :new CheckoutRequest("PICKUP","PAY_AT_PICKUP","Customer","8296022107",null,null,null,null,loyaltyPoints);
        Order order=commerce.checkout(user,request);
        return new Fixture(user,variant,order.id());
    }

    private String text(String sql,Object id){return jdbc.queryForObject(sql,String.class,id);}
    private int number(String sql,Object id){Long value=jdbc.queryForObject(sql,Long.class,id);return value==null?0:value.intValue();}
    private record Fixture(UUID userId,UUID variantId,UUID orderId){}
}
