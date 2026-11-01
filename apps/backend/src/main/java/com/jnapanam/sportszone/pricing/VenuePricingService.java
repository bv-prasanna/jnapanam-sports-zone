package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.model.BookingSlot;
import com.jnapanam.sportszone.catalog.model.Facility;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database-backed arena configuration and deterministic booking price calculation. */
@Service
public class VenuePricingService {
    private final JdbcTemplate jdbc;
    public VenuePricingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record PriceRuleInput(
        @NotBlank @Size(max=120) String name,
        @NotNull VenuePriceCalculator.Scope scope, String target,
        @NotNull VenuePriceCalculator.Adjustment adjustment,
        @NotNull @DecimalMin("0.00") BigDecimal value,
        int priority, LocalDate effectiveFrom, LocalDate effectiveTo,
        LocalTime startTime, LocalTime endTime, Set<DayOfWeek> weekdays) {}

    public record TaxRuleInput(
        @NotBlank @Size(max=120) String name,
        @NotNull VenuePriceCalculator.Scope scope, String target,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal ratePercent,
        boolean inclusive, LocalDate effectiveFrom, LocalDate effectiveTo) {}

    public record Preview(BigDecimal unitBase, BigDecimal unitPrice, BigDecimal taxableValue,
        BigDecimal tax, BigDecimal total, String priceRuleId, String taxRuleId, int quantity, BigDecimal taxRatePercent, boolean taxInclusive) {}

    @Transactional(readOnly=true)
    public List<Map<String,Object>> prices() {
        return jdbc.queryForList("""
            select id,name,scope,target,adjustment,value,priority,effective_from,effective_to,
            start_time,end_time,weekdays,active,case
              when scope='SITE' then 'Entire site'
              when scope='SPORT' then (select name from sports where code=target)
              when scope='FACILITY' then (select name from facilities where code=target)
              when scope='SLOT' then (
                select f.name || ' · ' || to_char(s.slot_date,'DD Mon YYYY') || ' · ' ||
                to_char(s.start_time,'HH24:MI') || '–' || to_char(s.end_time,'HH24:MI')
                from booking_slots s join facilities f on f.id=s.facility_id
                where s.id::text=target
              )
              end as "targetLabel"
            from venue_price_rules order by updated_at desc
            """);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> taxes() {
        return jdbc.queryForList("""
            select id,name,scope,target,rate_percent,inclusive,effective_from,effective_to,active,case
              when scope='SITE' then 'Entire site'
              when scope='SPORT' then (select name from sports where code=target)
              when scope='FACILITY' then (select name from facilities where code=target)
              when scope='SLOT' then (
                select f.name || ' · ' || to_char(s.slot_date,'DD Mon YYYY') || ' · ' ||
                to_char(s.start_time,'HH24:MI') || '–' || to_char(s.end_time,'HH24:MI')
                from booking_slots s join facilities f on f.id=s.facility_id
                where s.id::text=target
              )
              end as "targetLabel"
            from venue_tax_rules order by updated_at desc
            """);
    }

    private void scope(VenuePriceCalculator.Scope scope, String target) {
        if (scope == null) throw new BadRequestException("Choose site, sport, facility or slot");
        if (scope == VenuePriceCalculator.Scope.SITE) {
            if (target != null && !target.isBlank()) throw new BadRequestException("Site-wide rules cannot specify a target");
        } else {
            if (target == null || target.isBlank() || target.length() > 80)
                throw new BadRequestException("Choose a valid sport, facility or slot");
            String sql = switch(scope) {
                case SPORT -> "select exists(select 1 from sports where code=?)";
                case FACILITY -> "select exists(select 1 from facilities where code=?)";
                case SLOT -> "select exists(select 1 from booking_slots where id::text=?)";
                default -> throw new IllegalStateException("Unexpected scope");
            };
            if (!Boolean.TRUE.equals(jdbc.queryForObject(sql,Boolean.class,target)))
                throw new BadRequestException("Selected "+scope.name().toLowerCase(Locale.ROOT)+" does not exist");
        }
    }

    private static void dates(LocalDate start,LocalDate end) {
        if (start != null && end != null && end.isBefore(start))
            throw new BadRequestException("End date cannot precede start date");
    }

    private void validate(PriceRuleInput r) {
        scope(r.scope(),r.target()); dates(r.effectiveFrom(),r.effectiveTo());
        if (r.startTime()!=null && r.endTime()!=null && !r.endTime().isAfter(r.startTime()))
            throw new BadRequestException("End time must be after start time; split overnight rules");
        if (r.value()==null || r.value().signum()<0) throw new BadRequestException("Adjustment cannot be negative");
        if ((r.adjustment()==VenuePriceCalculator.Adjustment.ADD_PERCENT || r.adjustment()==VenuePriceCalculator.Adjustment.DISCOUNT_PERCENT)
            && r.value().compareTo(new BigDecimal("100"))>0)
            throw new BadRequestException("Percentage adjustment must be at most 100%");
    }

    private void validate(TaxRuleInput r) {
        scope(r.scope(),r.target()); dates(r.effectiveFrom(),r.effectiveTo());
        if (r.ratePercent()==null || r.ratePercent().signum()<0 || r.ratePercent().compareTo(new BigDecimal("100"))>0)
            throw new BadRequestException("Tax rate is outside the supported range");
    }

    @Transactional
    public UUID savePrice(UUID id, PriceRuleInput r) {
        validate(r);
        UUID key=id==null?UUID.randomUUID():id;
        if (id==null) {
            jdbc.update("""
              insert into venue_price_rules(id,name,scope,target,adjustment,value,priority,effective_from,effective_to,start_time,end_time,weekdays)
              values(?,?,?,?,?,?,?,?,?,?,?,?)
              """,key,r.name().trim(),r.scope().name(),r.target(),r.adjustment().name(),r.value(),r.priority(),
                 r.effectiveFrom(),r.effectiveTo(),r.startTime(),r.endTime(),encodeDays(r.weekdays()));
        } else {
            int count=jdbc.update("""
              update venue_price_rules set name=?,scope=?,target=?,adjustment=?,value=?,priority=?,effective_from=?,effective_to=?,
              start_time=?,end_time=?,weekdays=?,updated_at=now() where id=?
              """,r.name().trim(),r.scope().name(),r.target(),r.adjustment().name(),r.value(),r.priority(),
                 r.effectiveFrom(),r.effectiveTo(),r.startTime(),r.endTime(),encodeDays(r.weekdays()),key);
            if(count==0) throw new NotFoundException("Pricing rule not found");
        }
        return key;
    }

    @Transactional
    public UUID saveTax(UUID id,TaxRuleInput r) {
        validate(r);
        rejectOverlappingTaxRules(id,r);
        UUID key=id==null?UUID.randomUUID():id;
        if(id==null){
            jdbc.update("""
              insert into venue_tax_rules(id,name,scope,target,rate_percent,inclusive,effective_from,effective_to)
              values(?,?,?,?,?,?,?,?)
              """,key,r.name().trim(),r.scope().name(),r.target(),r.ratePercent(),r.inclusive(),r.effectiveFrom(),r.effectiveTo());
        }else{
            int count=jdbc.update("""
              update venue_tax_rules set name=?,scope=?,target=?,rate_percent=?,inclusive=?,effective_from=?,effective_to=?,updated_at=now()
              where id=?
              """,r.name().trim(),r.scope().name(),r.target(),r.ratePercent(),r.inclusive(),r.effectiveFrom(),r.effectiveTo(),key);
            if(count==0)throw new NotFoundException("Tax rule not found");
        }
        return key;
    }

    private void rejectOverlappingTaxRules(UUID editingId, TaxRuleInput rule){
        List<Map<String,Object>> existing=jdbc.queryForList("""
            select id,effective_from,effective_to from venue_tax_rules
            where active=true and scope=? and target is not distinct from ?
            """,rule.scope().name(),rule.target());
        for(Map<String,Object> prior:existing){
            if(prior.get("id").toString().equals(String.valueOf(editingId)))continue;
            LocalDate previousStart=prior.get("effective_from")==null?null:((java.sql.Date)prior.get("effective_from")).toLocalDate();
            LocalDate previousEnd=prior.get("effective_to")==null?null:((java.sql.Date)prior.get("effective_to")).toLocalDate();
            boolean overlaps=(previousEnd==null||rule.effectiveFrom()==null||!previousEnd.isBefore(rule.effectiveFrom()))
                         &&(rule.effectiveTo()==null||previousStart==null||!rule.effectiveTo().isBefore(previousStart));
            if(overlaps)throw new com.jnapanam.sportszone.common.exception.ConflictException(
                "Another tax rule is already active for this scope and date range. Edit or deactivate it first.");
        }
    }

    @Transactional
    public void deactivate(String table,UUID id) {
        if(!table.equals("venue_tax_rules")&&!table.equals("venue_price_rules"))throw new BadRequestException("Invalid rule type");
        int count=jdbc.update("update "+table+" set active=false,updated_at=now() where id=?",id);
        if(count==0)throw new NotFoundException("Rule not found");
    }

    @Transactional(readOnly=true)
    public Preview quote(BookingSlot slot,int quantity) {
        if(quantity<1 || quantity>100)throw new BadRequestException("Quantity must be between 1 and 100");
        Facility facility=slot.getFacility();
        String sport=facility.getSport().getCode(), fac=facility.getCode(), slotId=slot.getId().toString();
        List<VenuePriceCalculator.Rule> pricing=jdbc.query("""
          select * from venue_price_rules where active=true and (effective_from is null or effective_from<=?)
          and (effective_to is null or effective_to>=?) and (
          scope='SITE' or (scope='SPORT' and target=?) or (scope='FACILITY' and target=?) or (scope='SLOT' and target=?))
          """,this::priceRow,slot.getDate(),slot.getDate(),sport,fac,slotId);
        List<VenuePriceCalculator.Tax> tax=jdbc.query("""
          select * from venue_tax_rules where active=true and (effective_from is null or effective_from<=?)
          and (effective_to is null or effective_to>=?) and (
          scope='SITE' or (scope='SPORT' and target=?) or (scope='FACILITY' and target=?) or (scope='SLOT' and target=?))
          """,this::taxRow,slot.getDate(),slot.getDate(),sport,fac,slotId);
        var result=VenuePriceCalculator.calculate(facility.getBookingPrice(),quantity,sport,fac,slotId,
            slot.getDate(),slot.getStartTime(),pricing,tax);
        VenuePriceCalculator.Tax applied=tax.stream().filter(t->t.id().equals(result.taxRuleId())).findFirst().orElse(null);
        return new Preview(result.unitBase(),result.unitAfterAdjustment(),result.net(),result.tax(),
            result.total(),result.priceRuleId(),result.taxRuleId(),quantity,
            applied==null?BigDecimal.ZERO:applied.ratePercent(),applied!=null&&applied.inclusive());
    }

    private VenuePriceCalculator.Rule priceRow(ResultSet r,int n)throws SQLException {
        return new VenuePriceCalculator.Rule(r.getString("id"),VenuePriceCalculator.Scope.valueOf(r.getString("scope")),
            r.getString("target"),VenuePriceCalculator.Adjustment.valueOf(r.getString("adjustment")),
            r.getBigDecimal("value"),r.getInt("priority"),date(r,"effective_from"),date(r,"effective_to"),
            time(r,"start_time"),time(r,"end_time"),decodeDays(r.getString("weekdays")));
    }
    private VenuePriceCalculator.Tax taxRow(ResultSet r,int n)throws SQLException {
        return new VenuePriceCalculator.Tax(r.getString("id"),VenuePriceCalculator.Scope.valueOf(r.getString("scope")),
            r.getString("target"),r.getBigDecimal("rate_percent"),r.getBoolean("inclusive"),
            date(r,"effective_from"),date(r,"effective_to"));
    }
    private static LocalDate date(ResultSet r,String column)throws SQLException {
        java.sql.Date v=r.getDate(column);return v==null?null:v.toLocalDate();
    }
    private static LocalTime time(ResultSet r,String column)throws SQLException {
        java.sql.Time v=r.getTime(column);return v==null?null:v.toLocalTime();
    }
    private static String encodeDays(Set<DayOfWeek> weekdays) {
        if(weekdays==null||weekdays.isEmpty())return null;
        return weekdays.stream().sorted().map(Enum::name).collect(java.util.stream.Collectors.joining(","));
    }
    private static Set<DayOfWeek> decodeDays(String value){
        if(value==null||value.isBlank())return Set.of();
        EnumSet<DayOfWeek> days=EnumSet.noneOf(DayOfWeek.class);
        for(String v:value.split(","))days.add(DayOfWeek.valueOf(v.trim()));
        return days;
    }
}
