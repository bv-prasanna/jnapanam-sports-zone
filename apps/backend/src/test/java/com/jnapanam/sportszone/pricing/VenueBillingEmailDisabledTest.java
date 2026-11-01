package com.jnapanam.sportszone.pricing;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.notification.service.NotificationProviderSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class VenueBillingEmailDisabledTest {
    @Test void disabledInvoiceEmailNeverProcessesTheOutbox(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("invoice_email_enabled"),eq(Boolean.class))).thenReturn(false);
        VenueBillingEmailDelivery mail=new VenueBillingEmailDelivery(jdbc,new ObjectMapper(),
            new VenueInvoicePdfService(),mock(NotificationProviderSettingsService.class),
            "localhost",587,"","","");
        assertDoesNotThrow(mail::dispatch);
        verify(jdbc,never()).queryForList(anyString());
    }
}
