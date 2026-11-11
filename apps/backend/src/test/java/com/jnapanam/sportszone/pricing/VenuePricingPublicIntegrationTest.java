package com.jnapanam.sportszone.pricing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Reproduces the live 500: open-in-view=false detaches BookingSlot before pricing
 * accesses facility/sport. Unit mocks do not detect that lazy-loading failure.
 * Do not annotate this test @Transactional, or it would hide the production bug.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VenuePricingPublicIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void publicBatchAndSingleQuotesLoadFacilityAndSportWithoutOpenSessionInView() throws Exception {
        // Seed a private facility: migration V3 supplies sports but no facilities.
        UUID facilityId=UUID.randomUUID();
        UUID sportId=UUID.fromString("10000000-0000-0000-0000-000000000001");
        jdbc.update("""
            insert into facilities(id,sport_id,code,name,slot_duration_minutes,capacity,booking_price,status,created_at,updated_at)
            values(?,?,?,'Pricing Quote Integration Court',60,2,500.00,'ACTIVE',now(),now())
            """,facilityId,sportId,"QUOTE_TEST_"+facilityId.toString().substring(0,8));
        UUID slotId=UUID.randomUUID();
        LocalDate day=LocalDate.of(2048,6,14);
        LocalTime start=LocalTime.of(5,15);
        jdbc.update("""
           insert into booking_slots(id,facility_id,slot_date,start_time,end_time,capacity,held_count,booked_count,version,created_at)
           values(?,?,?,?,?,1,0,0,0,now())
           """,slotId,facilityId,day,start,start.plusHours(1));
        try {
            mvc.perform(post("/api/v1/public/pricing/slots/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slotIds\":[\""+slotId+"\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['"+slotId+"'].unitBase").isNumber())
                .andExpect(jsonPath("$['"+slotId+"'].total").isNumber());

            mvc.perform(get("/api/v1/public/pricing/slots/{slotId}",slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber());

            mvc.perform(post("/api/v1/public/pricing/slots/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"slotIds\":[\""+slotId+"\",\""+slotId+"\"]}"))
                .andExpect(status().isBadRequest());

            mvc.perform(get("/api/v1/public/pricing/slots/{slotId}",UUID.randomUUID()))
                .andExpect(status().isNotFound());
        } finally {
            jdbc.update("delete from booking_slots where id=?",slotId);
            jdbc.update("delete from facilities where id=?",facilityId);
        }
    }
}
