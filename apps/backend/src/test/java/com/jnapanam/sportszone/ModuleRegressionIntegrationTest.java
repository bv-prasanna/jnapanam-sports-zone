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
        String bookingNumber=field(bookingResult,Pattern.compile("\\\"bookingNumber\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        mvc.perform(get("/api/v1/bookings/me").header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(bookingId));
        mvc.perform(post("/api/v1/operations/bookings/{bookingNumber}/check-in",bookingNumber).header("Authorization",bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.checkedInAt").isNotEmpty()).andExpect(jsonPath("$.checkedInBy").value("Automation User"));
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

    @Test
    void adminCreatesTrainingAndCustomerEnrollsAttendsAndCancels() throws Exception {
        String adminEmail=uniqueEmail("training-admin");register(adminEmail);promote(adminEmail,"ADMIN");String adminToken=login(adminEmail,"Test@12345").accessToken();
        String coachEmail=uniqueEmail("coach");register(coachEmail);promote(coachEmail,"COACH");
        String suffix=UUID.randomUUID().toString().substring(0,8).toUpperCase();String facilityCode="TRAIN_"+suffix;
        mvc.perform(post("/api/v1/admin/catalog/facilities").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\""+facilityCode+"\",\"sportCode\":\"SWIMMING\",\"name\":\"Training Pool\",\"slotDurationMinutes\":60,\"capacity\":20}"))
                .andExpect(status().isCreated());
        String programCode="PROG_"+suffix;
        mvc.perform(post("/api/v1/admin/training/programs").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\""+programCode+"\",\"sportCode\":\"SWIMMING\",\"name\":\"Junior Swimming\",\"description\":\"Beginner camp\",\"minAge\":8,\"maxAge\":14,\"level\":\"BEGINNER\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));
        java.time.LocalDate start=java.time.LocalDate.now().plusDays(14);while(start.getDayOfWeek()!=java.time.DayOfWeek.MONDAY)start=start.plusDays(1);java.time.LocalDate end=start.plusDays(28);
        MvcResult batch=mvc.perform(post("/api/v1/admin/training/batches").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"programCode\":\""+programCode+"\",\"facilityCode\":\""+facilityCode+"\",\"coachEmail\":\""+coachEmail+"\",\"name\":\"Morning Batch\",\"startDate\":\""+start+"\",\"endDate\":\""+end+"\",\"startTime\":\"07:00:00\",\"endTime\":\"08:00:00\",\"daysOfWeek\":[\"MONDAY\"],\"capacity\":1,\"fee\":1500.00}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.available").value(1)).andExpect(jsonPath("$.coachName").value("Automation User")).andReturn();
        String batchId=field(batch,ID);TokenPair customer=register(uniqueEmail("trainee"));
        MvcResult enrollment=mvc.perform(post("/api/v1/training/batches/{id}/enrollments",batchId).header("Authorization",bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON).content("{\"participantName\":\"Junior Test\",\"participantAge\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ENROLLED")).andReturn();String enrollmentId=field(enrollment,ID);
        mvc.perform(post("/api/v1/training/batches/{id}/enrollments",batchId).header("Authorization",bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON).content("{\"participantName\":\"Junior Test\",\"participantAge\":10}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/admin/training/batches/{id}/participants",batchId).header("Authorization",bearer(adminToken))).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(1)));
        mvc.perform(put("/api/v1/admin/training/enrollments/{id}/attendance",enrollmentId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"sessionDate\":\""+start+"\",\"present\":true}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/training/enrollments/{id}/cancel",enrollmentId).header("Authorization",bearer(customer.accessToken()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/v1/public/training/batches")).andExpect(status().isOk()).andExpect(jsonPath("$[*].id",hasItem(batchId))).andExpect(jsonPath("$[0].available").value(1));
    }

    @Test
    void customerPaysMembershipWithIdempotencyAndAdminRefunds() throws Exception {
        String adminEmail=uniqueEmail("payment-admin");register(adminEmail);promote(adminEmail,"ADMIN");String adminToken=login(adminEmail,"Test@12345").accessToken();
        String code="PAY_"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        mvc.perform(post("/api/v1/admin/memberships/plans").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\""+code+"\",\"name\":\"Payment Plan\",\"durationDays\":30,\"price\":1250.00,\"sportCodes\":[\"GYM\"]}"))
                .andExpect(status().isCreated());
        TokenPair customer=register(uniqueEmail("payer"));
        MvcResult membership=mvc.perform(post("/api/v1/memberships").header("Authorization",bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planCode\":\""+code+"\"}"))
                .andExpect(status().isCreated()).andReturn();
        String membershipId=field(membership,ID);String idempotency="pay-"+UUID.randomUUID();
        String createBody="{\"purpose\":\"MEMBERSHIP\",\"referenceId\":\""+membershipId+"\"}";
        MvcResult created=mvc.perform(post("/api/v1/payments/orders").header("Authorization",bearer(customer.accessToken())).header("Idempotency-Key",idempotency).contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amountPaise").value(125000)).andExpect(jsonPath("$.provider").value("MOCK")).andReturn();
        String paymentId=field(created,ID);String providerOrderId=field(created,Pattern.compile("\\\"providerOrderId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""));
        mvc.perform(post("/api/v1/payments/orders").header("Authorization",bearer(customer.accessToken())).header("Idempotency-Key",idempotency).contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(paymentId));
        mvc.perform(post("/api/v1/payments/verify").header("Authorization",bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentOrderId\":\""+paymentId+"\",\"providerPaymentId\":\"pay_mock_123\",\"signature\":\"mock-signature\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.providerOrderId").value(providerOrderId)).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(get("/api/v1/memberships/me").header("Authorization",bearer(customer.accessToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("ACTIVE"));
        mvc.perform(post("/api/v1/admin/payments/{id}/refunds",paymentId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Automation refund\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amountPaise").value(125000)).andExpect(jsonPath("$.status").value("PROCESSED"));
        mvc.perform(get("/api/v1/notifications/me").header("Authorization",bearer(customer.accessToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].category",hasItems("PAYMENT","REFUND")));
    }

    @Test
    void customerControlsPreferencesAndReceivesAdminBroadcast() throws Exception {
        String adminEmail=uniqueEmail("notification-admin");register(adminEmail);promote(adminEmail,"ADMIN");String adminToken=login(adminEmail,"Test@12345").accessToken();
        TokenPair customer=register(uniqueEmail("notification-customer"));
        mvc.perform(put("/api/v1/notifications/preferences").header("Authorization",bearer(customer.accessToken())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailEnabled\":false,\"smsEnabled\":true,\"pushEnabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emailEnabled").value(false)).andExpect(jsonPath("$.smsEnabled").value(true));
        mvc.perform(post("/api/v1/admin/notifications/broadcasts").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"IN_APP\",\"category\":\"ANNOUNCEMENT\",\"subject\":\"Pool maintenance\",\"message\":\"Pool closes at 8 PM today.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.queued",greaterThanOrEqualTo(1)));
        MvcResult inbox=mvc.perform(get("/api/v1/notifications/me").header("Authorization",bearer(customer.accessToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].category").value("ANNOUNCEMENT")).andExpect(jsonPath("$[0].readAt").doesNotExist()).andReturn();
        String notificationId=field(inbox,ID);
        mvc.perform(patch("/api/v1/notifications/{id}/read",notificationId).header("Authorization",bearer(customer.accessToken())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.readAt").isNotEmpty());
    }

    @Test
    void approvedOrganizerRunsTournamentTeamsFixturesAndResults() throws Exception {
        String adminEmail=uniqueEmail("tournament-admin");register(adminEmail);promote(adminEmail,"ADMIN");String adminToken=login(adminEmail,"Test@12345").accessToken();
        TokenPair organizer=register(uniqueEmail("organizer"));
        MvcResult profile=mvc.perform(get("/api/v1/profile/me").header("Authorization",bearer(organizer.accessToken()))).andExpect(status().isOk()).andReturn();
        String organizerId=field(profile,ID);
        MvcResult application=mvc.perform(post("/api/v1/tournaments/organizer-applications").header("Authorization",bearer(organizer.accessToken())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationName\":\"Holalkere Sports Club\",\"experience\":\"Local competitions\",\"contactNumber\":\"8296022107\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andReturn();
        String applicationId=field(application,ID);
        mvc.perform(patch("/api/v1/admin/tournaments/organizer-applications/{id}",applicationId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\",\"reviewNote\":\"Verified\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        java.time.LocalDate opens=java.time.LocalDate.now().minusDays(1),closes=java.time.LocalDate.now().plusDays(7),starts=closes.plusDays(1),ends=starts.plusDays(2);
        String code="TOUR_"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        MvcResult tournament=mvc.perform(post("/api/v1/admin/tournaments").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\""+code+"\",\"name\":\"Jnapanam Cup\",\"sportCode\":\"CRICKET\",\"organizerUserId\":\""+organizerId+"\",\"format\":\"SINGLE_ELIMINATION\",\"registrationOpens\":\""+opens+"\",\"registrationCloses\":\""+closes+"\",\"startDate\":\""+starts+"\",\"endDate\":\""+ends+"\",\"teamSize\":2,\"maxTeams\":4,\"entryFee\":500.00}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
        String tournamentId=field(tournament,ID);
        mvc.perform(patch("/api/v1/admin/tournaments/{id}/status",tournamentId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REGISTRATION_OPEN\"}"))
                .andExpect(status().isOk());
        TokenPair captain1=register(uniqueEmail("captain-one"));TokenPair captain2=register(uniqueEmail("captain-two"));
        mvc.perform(post("/api/v1/tournaments/{id}/teams",tournamentId).header("Authorization",bearer(captain1.accessToken())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"teamName\":\"Warriors\",\"members\":[{\"name\":\"Player One\"},{\"name\":\"Player Two\"}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.members",hasSize(2)));
        mvc.perform(post("/api/v1/tournaments/{id}/teams",tournamentId).header("Authorization",bearer(captain2.accessToken())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"teamName\":\"Challengers\",\"members\":[{\"name\":\"Player Three\"},{\"name\":\"Player Four\"}]}"))
                .andExpect(status().isCreated());
        MvcResult generated=mvc.perform(post("/api/v1/admin/tournaments/{id}/fixtures/generate",tournamentId).header("Authorization",bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(1))).andReturn();
        String fixtureId=field(generated,ID);
        mvc.perform(patch("/api/v1/admin/tournaments/fixtures/{id}/result",fixtureId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"homeScore\":120,\"awayScore\":110}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.winner").value("Warriors"));
        mvc.perform(get("/api/v1/public/tournaments/{id}/fixtures",tournamentId)).andExpect(status().isOk()).andExpect(jsonPath("$[0].homeScore").value(120));
    }

    @Test
    void adminDashboardReportsReconciliationAndAuditAreProtected() throws Exception {
        String adminEmail=uniqueEmail("report-admin");register(adminEmail);promote(adminEmail,"ADMIN");String adminToken=login(adminEmail,"Test@12345").accessToken();
        String code="AUDIT_"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        mvc.perform(post("/api/v1/admin/catalog/sports").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\""+code+"\",\"name\":\"Audit Sport\",\"displayOrder\":500}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/admin/dashboard").header("Authorization",bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.users",greaterThanOrEqualTo(1))).andExpect(jsonPath("$.activeSports",greaterThanOrEqualTo(8))).andExpect(jsonPath("$.netRevenuePaise").isNumber());
        mvc.perform(get("/api/v1/admin/reconciliation").header("Authorization",bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paidOrders").isNumber());
        mvc.perform(get("/api/v1/admin/audit-logs").header("Authorization",bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].path",hasItem("/api/v1/admin/catalog/sports")));
        TokenPair customer=register(uniqueEmail("report-customer"));
        mvc.perform(get("/api/v1/admin/dashboard").header("Authorization",bearer(customer.accessToken()))).andExpect(status().isForbidden());
    }

    @Test
    void adminPublishesWebsiteContentAndHandlesContactInquiry() throws Exception {
        String adminEmail=uniqueEmail("content-admin");register(adminEmail);promote(adminEmail,"ADMIN");String adminToken=login(adminEmail,"Test@12345").accessToken();
        String slug="summer-camp-"+UUID.randomUUID().toString().substring(0,8);
        java.time.Instant start=java.time.Instant.now().plusSeconds(86400),end=start.plusSeconds(7200);
        MvcResult event=mvc.perform(post("/api/v1/admin/content/events").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\""+slug+"\",\"title\":\"Summer Cricket Camp\",\"description\":\"Coaching camp for young players\",\"eventType\":\"CAMP\",\"sportCode\":\"CRICKET\",\"startsAt\":\""+start+"\",\"endsAt\":\""+end+"\",\"imageUrl\":\"https://example.com/cricket.jpg\",\"featured\":true,\"displayOrder\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
        String eventId=field(event,ID);
        mvc.perform(patch("/api/v1/admin/content/events/{id}/status",eventId).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk());
        MvcResult announcement=mvc.perform(post("/api/v1/admin/content/announcements").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Registration Open\",\"body\":\"Summer registrations are now open.\",\"priority\":10,\"publishFrom\":\""+java.time.Instant.now().minusSeconds(60)+"\"}"))
                .andExpect(status().isCreated()).andReturn();
        mvc.perform(patch("/api/v1/admin/content/announcements/{id}/status",field(announcement,ID)).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/content/gallery").header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Cricket Training\",\"mediaType\":\"IMAGE\",\"mediaUrl\":\"https://example.com/gallery.jpg\",\"altText\":\"Children training in cricket\",\"sportCode\":\"CRICKET\",\"published\":true,\"displayOrder\":1}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/public/content/events")).andExpect(status().isOk()).andExpect(jsonPath("$[*].slug",hasItem(slug)));
        mvc.perform(get("/api/v1/public/content/announcements")).andExpect(status().isOk()).andExpect(jsonPath("$[*].title",hasItem("Registration Open")));
        mvc.perform(get("/api/v1/public/content/gallery")).andExpect(status().isOk()).andExpect(jsonPath("$[*].title",hasItem("Cricket Training")));
        mvc.perform(get("/api/v1/public/content/pages/about-us")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        MvcResult inquiry=mvc.perform(post("/api/v1/public/content/contact").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Website Visitor\",\"email\":\"visitor@example.com\",\"mobile\":\"8296022107\",\"subject\":\"Swimming enquiry\",\"message\":\"Please share membership details.\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("NEW")).andReturn();
        mvc.perform(patch("/api/v1/admin/content/inquiries/{id}/status",field(inquiry,ID)).header("Authorization",bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\",\"adminNote\":\"Customer contacted\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolvedAt").isNotEmpty());
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
