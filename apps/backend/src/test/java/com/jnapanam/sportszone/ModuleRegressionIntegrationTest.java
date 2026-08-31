package com.jnapanam.sportszone;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModuleRegressionIntegrationTest {
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern REFRESH_TOKEN = Pattern.compile("\\\"refreshToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void authenticationHappyPathRefreshRotationAndLogout() throws Exception {
        String email = uniqueEmail("auth");
        TokenPair registered = register(email);

        mvc.perform(get("/api/v1/profile/me").header("Authorization", bearer(registered.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles", hasItem("CUSTOMER")));

        TokenPair loggedIn = login(email, "Test@12345");
        MvcResult refreshedResult = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loggedIn.refreshToken() + "\"}"))
                .andExpect(status().isOk()).andReturn();
        TokenPair refreshed = tokens(refreshedResult);

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loggedIn.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshed.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshed.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticationNegativeCasesReturnExpectedStatuses() throws Exception {
        String email = uniqueEmail("negative");
        register(email);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "WrongPassword")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Bad Input\",\"email\":\"invalid\",\"mobile\":\"123\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void publicCatalogueIsOpenButCustomerCannotUseAdminApis() throws Exception {
        mvc.perform(get("/api/v1/public/sports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$[*].code", hasItems("SWIMMING", "CRICKET", "BADMINTON", "CARROM", "CHESS", "GYM", "AEROBICS")));

        TokenPair customer = register(uniqueEmail("customer"));
        mvc.perform(get("/api/v1/admin/catalog/sports").header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConfiguresSportFacilityHoursClosuresAndOverrides() throws Exception {
        String email = uniqueEmail("admin");
        register(email);
        promote(email, "ADMIN");
        String adminToken = login(email, "Test@12345").accessToken();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sportCode = "AUTO_" + suffix;

        MvcResult sportResult = mvc.perform(post("/api/v1/admin/catalog/sports")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + sportCode + "\",\"name\":\"Automation Sport\",\"description\":\"Created by integration test\",\"iconKey\":\"test\",\"displayOrder\":50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE")).andReturn();
        String sportId = field(sportResult, ID);

        mvc.perform(patch("/api/v1/admin/catalog/sports/{id}/status", sportId)
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TEMPORARILY_SUSPENDED\",\"suspendedFrom\":\"2030-01-10\",\"suspendedUntil\":\"2030-01-20\",\"reason\":\"Maintenance\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TEMPORARILY_SUSPENDED"));
        mvc.perform(patch("/api/v1/admin/catalog/sports/{id}/status", sportId)
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\",\"suspendedFrom\":null,\"suspendedUntil\":null,\"reason\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        String facilityCode = "FAC_" + suffix;
        MvcResult facilityResult = mvc.perform(post("/api/v1/admin/catalog/facilities")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + facilityCode + "\",\"sportCode\":\"" + sportCode + "\",\"name\":\"Automation Court\",\"description\":\"Test facility\",\"locationLabel\":\"Block A\",\"slotDurationMinutes\":60,\"capacity\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotDurationMinutes").value(60))
                .andExpect(jsonPath("$.capacity").value(4)).andReturn();
        String facilityId = field(facilityResult, ID);

        mvc.perform(put("/api/v1/admin/catalog/facilities/{id}/operating-hours", facilityId)
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"06:00:00\",\"closeTime\":\"22:00:00\",\"closed\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dayOfWeek").value("MONDAY"));
        mvc.perform(post("/api/v1/admin/catalog/facilities/{id}/closures", facilityId)
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closureDate\":\"2030-01-15\",\"reason\":\"Annual maintenance\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/admin/catalog/facilities/{id}/schedule-overrides", facilityId)
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleDate\":\"2030-01-16\",\"startTime\":\"18:00:00\",\"endTime\":\"20:00:00\",\"availability\":\"AVAILABLE\",\"slotDurationMinutes\":30,\"capacity\":6,\"reason\":\"Special session\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/public/facilities").param("sportCode", sportCode).param("date", "2030-01-05"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value(facilityCode));
        mvc.perform(get("/api/v1/public/facilities/{id}/schedule", facilityId)
                        .param("from", "2030-01-01").param("to", "2030-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyOperatingHours", hasSize(1)))
                .andExpect(jsonPath("$.closures", hasSize(1)))
                .andExpect(jsonPath("$.overrides", hasSize(1)));
    }

    @Test
    void customerGeneratesHoldsConfirmsAndCancelsBooking() throws Exception {
        String adminEmail = uniqueEmail("booking-admin");
        register(adminEmail); promote(adminEmail, "ADMIN");
        String adminToken = login(adminEmail, "Test@12345").accessToken();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sportCode = "BOOK_" + suffix;
        mvc.perform(post("/api/v1/admin/catalog/sports").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + sportCode + "\",\"name\":\"Booking Test Sport\",\"description\":\"Automated\",\"iconKey\":\"test\",\"displayOrder\":99}"))
                .andExpect(status().isCreated());
        MvcResult facilityResult = mvc.perform(post("/api/v1/admin/catalog/facilities").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BF_" + suffix + "\",\"sportCode\":\"" + sportCode + "\",\"name\":\"Booking Court\",\"slotDurationMinutes\":60,\"capacity\":2}"))
                .andExpect(status().isCreated()).andReturn();
        String facilityId = field(facilityResult, ID);
        java.time.LocalDate bookingDate = java.time.LocalDate.now().plusDays(14);
        while (bookingDate.getDayOfWeek() != java.time.DayOfWeek.MONDAY) bookingDate = bookingDate.plusDays(1);
        mvc.perform(put("/api/v1/admin/catalog/facilities/{id}/operating-hours", facilityId).header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"openTime\":\"06:00:00\",\"closeTime\":\"08:00:00\",\"closed\":false}"))
                .andExpect(status().isOk());
        MvcResult slotResult = mvc.perform(get("/api/v1/public/facilities/{id}/slots", facilityId).param("date", bookingDate.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].available").value(2)).andReturn();
        String slotId = field(slotResult, ID);
        TokenPair customer = register(uniqueEmail("booking-customer"));
        MvcResult holdResult = mvc.perform(post("/api/v1/bookings/holds").header("Authorization", bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":\"" + slotId + "\",\"quantity\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE")).andReturn();
        String holdId = field(holdResult, ID);
        mvc.perform(post("/api/v1/bookings/holds").header("Authorization", bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":\"" + slotId + "\",\"quantity\":1}"))
                .andExpect(status().isConflict());
        MvcResult bookingResult = mvc.perform(post("/api/v1/bookings/confirm").header("Authorization", bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holdId\":\"" + holdId + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CONFIRMED")).andReturn();
        String bookingId = field(bookingResult, ID);
        mvc.perform(get("/api/v1/bookings/me").header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(bookingId));
        mvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId).header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/v1/public/facilities/{id}/slots", facilityId).param("date", bookingDate.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].available").value(2));
    }

    @Test
    void adminCreatesPlanAndCustomerCompletesMembershipLifecycle() throws Exception {
        String adminEmail=uniqueEmail("membership-admin");register(adminEmail);promote(adminEmail,"ADMIN");String adminToken=login(adminEmail,"Test@12345").accessToken();
        String code="MEM_"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        MvcResult plan=mvc.perform(post("/api/v1/admin/memberships/plans").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\""+code+"\",\"name\":\"Family Fitness\",\"description\":\"Test plan\",\"durationDays\":30,\"price\":999.00,\"dailyBookingLimit\":2,\"monthlyBookingLimit\":20,\"sportCodes\":[\"SWIMMING\",\"GYM\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.sportCodes",hasItems("SWIMMING","GYM"))).andReturn();
        String planId=field(plan,ID);
        mvc.perform(get("/api/v1/public/membership-plans")).andExpect(status().isOk()).andExpect(jsonPath("$[*].code",hasItem(code)));
        TokenPair customer=register(uniqueEmail("member"));
        MvcResult subscribed=mvc.perform(post("/api/v1/memberships").header("Authorization",bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON).content("{\"planCode\":\""+code+"\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING_PAYMENT")).andReturn();
        String membershipId=field(subscribed,ID);
        mvc.perform(post("/api/v1/memberships").header("Authorization",bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON).content("{\"planCode\":\""+code+"\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/admin/memberships/{id}/status",membershipId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\",\"startDate\":\"2030-01-01\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endDate").value("2030-01-30"));
        mvc.perform(get("/api/v1/memberships/me").header("Authorization",bearer(customer.accessToken()))).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("ACTIVE"));
        mvc.perform(post("/api/v1/memberships/{id}/cancel",membershipId).header("Authorization",bearer(customer.accessToken()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(patch("/api/v1/admin/memberships/plans/{id}/status",planId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    private TokenPair register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                .andExpect(status().isCreated()).andReturn();
        return tokens(result);
    }

    private TokenPair login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(email, password)))
                .andExpect(status().isOk()).andReturn();
        return tokens(result);
    }

    private void promote(String email, String role) {
        int changed = jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u CROSS JOIN roles r WHERE u.email=? AND r.name=? ON CONFLICT DO NOTHING", email, role);
        Assertions.assertEquals(1, changed);
    }

    private static TokenPair tokens(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return new TokenPair(field(body, ACCESS_TOKEN), field(body, REFRESH_TOKEN));
    }

    private static String field(MvcResult result, Pattern pattern) throws Exception { return field(result.getResponse().getContentAsString(), pattern); }
    private static String field(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) throw new AssertionError("Expected field missing from response: " + body);
        return matcher.group(1);
    }
    private static String uniqueEmail(String prefix) { return prefix + "." + UUID.randomUUID() + "@example.com"; }
    private static String bearer(String token) { return "Bearer " + token; }
    private static String registerBody(String email) {
        return "{\"fullName\":\"Automation User\",\"email\":\"" + email + "\",\"mobile\":null,\"password\":\"Test@12345\"}";
    }
    private static String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
    private record TokenPair(String accessToken, String refreshToken) {}
}
