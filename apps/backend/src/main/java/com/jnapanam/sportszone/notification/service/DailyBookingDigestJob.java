package com.jnapanam.sportszone.notification.service;

import com.jnapanam.sportszone.identity.model.RoleName;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.model.Notification;
import com.jnapanam.sportszone.notification.model.NotificationChannel;
import com.jnapanam.sportszone.notification.provider.NotificationProviderRegistry;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 23:59 IST: send the next day's booking report. Catch up after overnight restarts.
 * Unique recipient/date rows and atomic claims prevent two pods sending simultaneously.
 * Crash after SMTP acceptance but before DB acknowledgement can still cause one duplicate.
 */
@Service
public class DailyBookingDigestJob {
    private static final Logger log = LoggerFactory.getLogger(DailyBookingDigestJob.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final NotificationProviderRegistry providers;
    private final boolean enabled;

    public DailyBookingDigestJob(JdbcTemplate jdbc, UserRepository users,
            NotificationProviderRegistry providers,
            @Value("${app.notification.daily-booking-digest.enabled:true}") boolean enabled) {
        this.jdbc=jdbc;this.users=users;this.providers=providers;this.enabled=enabled;
    }

    @Scheduled(cron="0 59 23 * * *", zone="Asia/Kolkata")
    public void nightly() {
        if (!enabled) return;
        LocalDate date=LocalDate.now(IST).plusDays(1);
        queue(date);
        deliverDue();
    }

    @Scheduled(cron="0 */5 * * * *", zone="Asia/Kolkata")
    public void retryAndCatchUp() {
        if (!enabled) return;
        LocalTime now=LocalTime.now(IST);
        LocalDate date=LocalDate.now(IST);
        if (!now.isBefore(LocalTime.of(23,59))) queue(date.plusDays(1));
        else if (now.isBefore(LocalTime.of(8,0))) queue(date);
        deliverDue();
    }

    void queue(LocalDate reportDate) {
        int count=0;
        for (User user:users.findAll()) {
            if (!eligible(user)) continue;
            jdbc.update("""
                insert into daily_booking_digest_deliveries(report_date,recipient_user_id,status)
                values (?,?,'PENDING')
                on conflict(report_date,recipient_user_id) do nothing
                """,reportDate,user.getId());
            count++;
        }
        log.info("Booking digest queued date={} eligibleRecipients={}",reportDate,count);
    }

    void deliverDue() {
        LocalDate date=LocalDate.now(IST);
        LocalTime now=LocalTime.now(IST);
        LocalDate target=!now.isBefore(LocalTime.of(23,59))?date.plusDays(1)
                :now.isBefore(LocalTime.of(8,0))?date:null;
        if (target==null) return;
        List<UUID> due=jdbc.query("""
            select recipient_user_id from daily_booking_digest_deliveries
            where report_date=? and attempts<6
              and (status='PENDING'
                or (status='FAILED' and next_attempt_at<=now())
                or (status='SENDING' and locked_at<now()-interval '15 minutes'))
            order by recipient_user_id
            """,(rs,i)->rs.getObject(1,UUID.class),target);
        for (UUID recipient:due) deliver(target,recipient);
    }

    void deliver(LocalDate date,UUID recipient) {
        int claim=jdbc.update("""
            update daily_booking_digest_deliveries set status='SENDING',
                attempts=attempts+1,locked_at=now()
            where report_date=? and recipient_user_id=? and attempts<6
              and (status='PENDING'
                or (status='FAILED' and next_attempt_at<=now())
                or (status='SENDING' and locked_at<now()-interval '15 minutes'))
            """,date,recipient);
        if (claim!=1) return;
        try {
            User user=users.findById(recipient).orElse(null);
            if (!eligible(user)) {
                jdbc.update("update daily_booking_digest_deliveries set status='SKIPPED',locked_at=null where report_date=? and recipient_user_id=?",date,recipient);
                return;
            }
            List<DailyBookingDigestReport.Facility> facilities=facilitiesFor(user);
            List<DailyBookingDigestReport.Booking> bookings=bookingsFor(date,facilities);
            String subject="Jnapanam Sports Zone | Bookings for "+date;
            String message=DailyBookingDigestReport.compose(date,facilities,bookings);
            String ref=providers.provider(NotificationChannel.EMAIL)
                .send(new Notification(user,NotificationChannel.EMAIL,"DAILY_BOOKING_DIGEST",subject,message));
            jdbc.update("""
                update daily_booking_digest_deliveries set status='SENT',sent_at=now(),
                    provider_reference=?,locked_at=null,last_error=null
                where report_date=? and recipient_user_id=?
                """,ref,date,recipient);
            log.info("Booking digest sent date={} recipientId={} facilities={} bookings={}",
                date,recipient,facilities.size(),bookings.size());
        } catch(Exception failure) {
            String type=failure.getClass().getSimpleName();
            jdbc.update("""
                update daily_booking_digest_deliveries set status='FAILED',locked_at=null,
                    last_error=?,next_attempt_at=now()+interval '10 minutes'
                where report_date=? and recipient_user_id=?
                """,type,date,recipient);
            log.warn("Booking digest delivery failed date={} recipientId={} errorType={}",date,recipient,type);
        }
    }

    private boolean eligible(User user) {
        return user!=null && user.isActive() && user.getEmail()!=null && !user.getEmail().isBlank()
            && (hasRole(user,RoleName.SUPER_ADMIN)||hasRole(user,RoleName.ADMIN)||hasRole(user,RoleName.OPERATIONS));
    }
    private boolean hasRole(User user,RoleName role) {
        return user.getRoles().stream().anyMatch(r->r.getName()==role);
    }

    private List<DailyBookingDigestReport.Facility> facilitiesFor(User user) {
        boolean admin=hasRole(user,RoleName.SUPER_ADMIN)||hasRole(user,RoleName.ADMIN);
        String sql="""
            select f.id,s.name sport_name,f.name facility_name
            from facilities f join sports s on s.id=f.sport_id
            """+(admin?"":"join operator_facility_assignments a on a.facility_id=f.id ")+
              (admin?"":"where a.operator_id=? ")+
              "order by s.name,f.name";
        var mapper=(org.springframework.jdbc.core.RowMapper<DailyBookingDigestReport.Facility>)
                (rs,i)->new DailyBookingDigestReport.Facility(
                   rs.getObject("id",UUID.class),rs.getString("sport_name"),rs.getString("facility_name"));
        return admin?jdbc.query(sql,mapper):jdbc.query(sql,mapper,user.getId());
    }

    private List<DailyBookingDigestReport.Booking> bookingsFor(
            LocalDate date,List<DailyBookingDigestReport.Facility> facilities) {
        if (facilities.isEmpty()) return List.of();
        UUID[] ids=facilities.stream().map(DailyBookingDigestReport.Facility::id).toArray(UUID[]::new);
        String sql="""
            select f.id facility_id,s.name sport_name,f.name facility_name,
                bs.start_time,bs.end_time,b.booking_number,u.full_name,u.mobile,
                b.status booking_status,b.payment_status
            from bookings b join booking_slots bs on bs.id=b.slot_id
            join facilities f on f.id=bs.facility_id
            join sports s on s.id=f.sport_id
            join users u on u.id=b.user_id
            where bs.slot_date=? and b.status in ('CONFIRMED','PENDING_PAYMENT')
              and f.id=any(?::uuid[])
            order by s.name,f.name,bs.start_time,b.booking_number
            """;
        return jdbc.query(connection->{
            var statement=connection.prepareStatement(sql);
            statement.setObject(1,date);
            statement.setArray(2,connection.createArrayOf("uuid",ids));
            return statement;
        },(rs,i)->new DailyBookingDigestReport.Booking(
            rs.getObject("facility_id",UUID.class),rs.getString("sport_name"),
            rs.getString("facility_name"),rs.getTime("start_time").toLocalTime().toString(),
            rs.getTime("end_time").toLocalTime().toString(),rs.getString("booking_number"),
            rs.getString("full_name"),rs.getString("mobile")==null?"Not provided":rs.getString("mobile"),
            rs.getString("booking_status"),rs.getString("payment_status")));
    }
}
