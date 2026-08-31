package com.jnapanam.sportszone.admin.service;

import com.jnapanam.sportszone.admin.dto.*;
import com.jnapanam.sportszone.admin.repository.AuditLogRepository;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportingService {
    private final JdbcTemplate jdbc;
    private final AuditLogRepository audit;
    public AdminReportingService(JdbcTemplate jdbc, AuditLogRepository audit) { this.jdbc = jdbc; this.audit = audit; }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(LocalDate from, LocalDate to) {
        LocalDate[] r = range(from, to);
        long gross = value("SELECT COALESCE(SUM(amount_paise),0) FROM payment_orders WHERE status IN ('PAID','PARTIALLY_REFUNDED','REFUNDED') AND created_at::date BETWEEN ? AND ?", r);
        long refunds = value("SELECT COALESCE(SUM(pr.amount_paise),0) FROM payment_refunds pr JOIN payment_orders po ON po.id=pr.payment_order_id WHERE pr.status='PROCESSED' AND pr.processed_at::date BETWEEN ? AND ?", r);
        return new DashboardResponse(r[0], r[1], value("SELECT COUNT(*) FROM users"), value("SELECT COUNT(*) FROM sports WHERE status='ACTIVE'"), value("SELECT COUNT(*) FROM facilities WHERE status='ACTIVE'"), value("SELECT COUNT(*) FROM bookings WHERE confirmed_at::date BETWEEN ? AND ?", r), value("SELECT COUNT(*) FROM bookings WHERE checked_in_at::date BETWEEN ? AND ?", r), value("SELECT COUNT(*) FROM customer_memberships WHERE status='ACTIVE'"), value("SELECT COUNT(*) FROM training_enrollments WHERE status='ENROLLED'"), value("SELECT COUNT(*) FROM tournaments WHERE status IN ('REGISTRATION_OPEN','REGISTRATION_CLOSED','IN_PROGRESS')"), gross, refunds, gross-refunds);
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse reconciliation() {
        return new ReconciliationResponse(value("SELECT COUNT(*) FROM payment_orders WHERE status='CREATED'"), value("SELECT COUNT(*) FROM payment_orders WHERE status='PAID'"), value("SELECT COUNT(*) FROM payment_orders WHERE status='PARTIALLY_REFUNDED'"), value("SELECT COUNT(*) FROM payment_orders WHERE status='REFUNDED'"), value("SELECT COUNT(*) FROM payment_refunds WHERE status='PROCESSED'"), value("SELECT COUNT(*) FROM payment_orders WHERE status IN ('PAID','PARTIALLY_REFUNDED','REFUNDED') AND provider_payment_id IS NULL"));
    }

    @Transactional(readOnly = true) public List<Map<String,Object>> revenue(LocalDate from,LocalDate to){LocalDate[]r=range(from,to);return jdbc.queryForList("SELECT created_at::date AS date,currency,SUM(amount_paise) AS gross_paise,COUNT(*) AS payments FROM payment_orders WHERE status IN ('PAID','PARTIALLY_REFUNDED','REFUNDED') AND created_at::date BETWEEN ? AND ? GROUP BY created_at::date,currency ORDER BY date",r[0],r[1]);}
    @Transactional(readOnly = true) public List<Map<String,Object>> bookings(LocalDate from,LocalDate to){LocalDate[]r=range(from,to);return jdbc.queryForList("SELECT f.name AS facility,s.name AS sport,COUNT(*) AS bookings,COALESCE(SUM(b.quantity),0) AS participants,COUNT(b.checked_in_at) AS checked_in FROM bookings b JOIN booking_slots bs ON bs.id=b.slot_id JOIN facilities f ON f.id=bs.facility_id JOIN sports s ON s.id=f.sport_id WHERE b.confirmed_at::date BETWEEN ? AND ? GROUP BY f.name,s.name ORDER BY bookings DESC",r[0],r[1]);}
    @Transactional(readOnly = true) public List<Map<String,Object>> memberships(){return jdbc.queryForList("SELECT mp.name AS plan,cm.status,COUNT(*) AS subscriptions FROM customer_memberships cm JOIN membership_plans mp ON mp.id=cm.plan_id GROUP BY mp.name,cm.status ORDER BY mp.name,cm.status");}

    @Transactional(readOnly = true)
    public List<Map<String,Object>> utilization(LocalDate from,LocalDate to){LocalDate[]r=range(from,to);return jdbc.queryForList("SELECT f.code AS facility_code,f.name AS facility,s.name AS sport,COUNT(bs.id) AS slots,COALESCE(SUM(bs.capacity),0) AS offered_capacity,COALESCE(SUM(bs.booked_count),0) AS booked_capacity,CASE WHEN SUM(bs.capacity)=0 THEN 0 ELSE ROUND(SUM(bs.booked_count)*100.0/SUM(bs.capacity),2) END AS utilization_percent FROM booking_slots bs JOIN facilities f ON f.id=bs.facility_id JOIN sports s ON s.id=f.sport_id WHERE bs.slot_date BETWEEN ? AND ? GROUP BY f.code,f.name,s.name ORDER BY utilization_percent DESC,f.name",r[0],r[1]);}

    @Transactional(readOnly = true)
    public List<Map<String,Object>> customers(){return jdbc.queryForList("SELECT u.status,COUNT(DISTINCT u.id) AS customers,COUNT(DISTINCT b.id) AS bookings,COUNT(DISTINCT cm.id) AS memberships,COUNT(DISTINCT te.id) AS training_enrollments FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id LEFT JOIN bookings b ON b.user_id=u.id LEFT JOIN customer_memberships cm ON cm.user_id=u.id LEFT JOIN training_enrollments te ON te.user_id=u.id WHERE r.name='CUSTOMER' GROUP BY u.status ORDER BY u.status");}

    @Transactional(readOnly = true)
    public List<Map<String,Object>> training(LocalDate from,LocalDate to){LocalDate[]r=range(from,to);return jdbc.queryForList("SELECT tp.code AS program_code,tp.name AS program,tb.name AS batch,tb.status,tb.capacity,COUNT(te.id) FILTER (WHERE te.status='ENROLLED') AS enrolled,COUNT(ta.id) FILTER (WHERE ta.present=TRUE) AS attendance FROM training_batches tb JOIN training_programs tp ON tp.id=tb.program_id LEFT JOIN training_enrollments te ON te.batch_id=tb.id LEFT JOIN training_attendance ta ON ta.enrollment_id=te.id AND ta.session_date BETWEEN ? AND ? WHERE tb.start_date<=? AND tb.end_date>=? GROUP BY tp.code,tp.name,tb.name,tb.status,tb.capacity ORDER BY tp.name,tb.name",r[0],r[1],r[1],r[0]);}

    @Transactional(readOnly = true)
    public List<Map<String,Object>> tournaments(LocalDate from,LocalDate to){LocalDate[]r=range(from,to);return jdbc.queryForList("SELECT t.code,t.name,s.name AS sport,t.status,t.start_date,t.end_date,t.max_teams,COUNT(DISTINCT tt.id) AS registered_teams,COUNT(DISTINCT tf.id) AS fixtures,COUNT(DISTINCT tf.id) FILTER (WHERE tf.status='COMPLETED') AS completed_fixtures FROM tournaments t JOIN sports s ON s.id=t.sport_id LEFT JOIN tournament_teams tt ON tt.tournament_id=t.id LEFT JOIN tournament_fixtures tf ON tf.tournament_id=t.id WHERE t.start_date<=? AND t.end_date>=? GROUP BY t.code,t.name,s.name,t.status,t.start_date,t.end_date,t.max_teams ORDER BY t.start_date,t.name",r[1],r[0]);}

    @Transactional(readOnly = true) public List<AuditLogResponse> audit(int limit){int safe=Math.max(1,Math.min(limit,500));return audit.findAllByOrderByOccurredAtDesc(PageRequest.of(0,safe)).stream().map(AuditLogResponse::from).toList();}

    @Transactional(readOnly = true)
public List<Map<String, Object>> searchAudit(
        UUID actor,
        String action,
        String resourceType,
        Boolean successful,
        Instant from,
        Instant to,
        int limit) {

    int safe = Math.max(1, Math.min(limit, 500));

    return jdbc.queryForList(
        """
        SELECT id, actor_user_id, http_method, request_path, response_status,
               remote_address, occurred_at, action, resource_type, resource_id,
               query_string, correlation_id, user_agent, duration_ms, successful
        FROM audit_logs
        WHERE (CAST(? AS uuid) IS NULL OR actor_user_id = CAST(? AS uuid))
          AND (CAST(? AS varchar) IS NULL
               OR action ILIKE '%' || CAST(? AS varchar) || '%')
          AND (CAST(? AS varchar) IS NULL
               OR resource_type = CAST(? AS varchar))
          AND (CAST(? AS boolean) IS NULL
               OR successful = CAST(? AS boolean))
          AND (CAST(? AS timestamptz) IS NULL
               OR occurred_at >= CAST(? AS timestamptz))
          AND (CAST(? AS timestamptz) IS NULL
               OR occurred_at <= CAST(? AS timestamptz))
        ORDER BY occurred_at DESC
        LIMIT ?
        """,
        actor, actor,
        blankNull(action), blankNull(action),
        blankNull(resourceType), blankNull(resourceType),
        successful, successful,
        from, from,
        to, to,
        safe
    );
}

    @Transactional(readOnly = true)
    public byte[] csv(String report,LocalDate from,LocalDate to){
        List<Map<String,Object>> rows=switch(report.toLowerCase(Locale.ROOT)){
            case "revenue"->revenue(from,to);case "bookings"->bookings(from,to);case "memberships"->memberships();case "utilization"->utilization(from,to);case "customers"->customers();case "training"->training(from,to);case "tournaments"->tournaments(from,to);
            default->throw new BadRequestException("Unsupported report. Use revenue, bookings, memberships, utilization, customers, training or tournaments");};
        if(rows.isEmpty())return "message\r\nNo data\r\n".getBytes(StandardCharsets.UTF_8);
        List<String> columns=new ArrayList<>(rows.get(0).keySet());StringBuilder csv=new StringBuilder(String.join(",",columns)).append("\r\n");
        for(Map<String,Object>row:rows){for(int i=0;i<columns.size();i++){if(i>0)csv.append(',');csv.append(quote(row.get(columns.get(i))));}csv.append("\r\n");}
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private long value(String sql,Object...args){Number n=jdbc.queryForObject(sql,Number.class,args);return n==null?0:n.longValue();}
    private static LocalDate[] range(LocalDate from,LocalDate to){LocalDate end=to==null?LocalDate.now():to;LocalDate start=from==null?end.withDayOfMonth(1):from;if(end.isBefore(start))throw new BadRequestException("Invalid report date range");if(start.plusYears(2).isBefore(end))throw new BadRequestException("Report range cannot exceed two years");return new LocalDate[]{start,end};}
    private static String blankNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private static String quote(Object value){String text=value==null?"":String.valueOf(value);return "\""+text.replace("\"","\"\"")+"\"";}
}
