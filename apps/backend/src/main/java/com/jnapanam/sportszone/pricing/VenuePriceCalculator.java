package com.jnapanam.sportszone.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

/**
 * Side-effect-free pricing calculation. Rules must be loaded from persisted configuration
 * and the resulting quote must be stored on the booking before payment collection.
 */
public final class VenuePriceCalculator {
    private VenuePriceCalculator() {}

    public enum Scope { SITE, SPORT, FACILITY, SLOT }
    public enum Adjustment { FIXED_PRICE, ADD_AMOUNT, DISCOUNT_AMOUNT, ADD_PERCENT, DISCOUNT_PERCENT }

    public record Rule(String id, Scope scope, String target, Adjustment adjustment,
                       BigDecimal value, int priority, LocalDate effectiveFrom, LocalDate effectiveTo,
                       LocalTime startTime, LocalTime endTime, Set<DayOfWeek> weekdays) {
        public boolean matches(String sport, String facility, String slot, LocalDate date, LocalTime time) {
            if (effectiveFrom != null && date.isBefore(effectiveFrom)) return false;
            if (effectiveTo != null && date.isAfter(effectiveTo)) return false;
            if (weekdays != null && !weekdays.isEmpty() && !weekdays.contains(date.getDayOfWeek())) return false;
            if (startTime != null && time.isBefore(startTime)) return false;
            if (endTime != null && !time.isBefore(endTime)) return false;
            return switch (scope) {
                case SITE -> true;
                case SPORT -> Objects.equals(target, sport);
                case FACILITY -> Objects.equals(target, facility);
                case SLOT -> Objects.equals(target, slot);
            };
        }
    }

    public record Tax(String id, Scope scope, String target, BigDecimal ratePercent,
                      boolean inclusive, LocalDate effectiveFrom, LocalDate effectiveTo) {
        public boolean matches(String sport, String facility, String slot, LocalDate date) {
            if (effectiveFrom != null && date.isBefore(effectiveFrom)) return false;
            if (effectiveTo != null && date.isAfter(effectiveTo)) return false;
            return switch (scope) {
                case SITE -> true;
                case SPORT -> Objects.equals(target, sport);
                case FACILITY -> Objects.equals(target, facility);
                case SLOT -> Objects.equals(target, slot);
            };
        }
    }

    public record Quote(BigDecimal unitBase, BigDecimal unitAfterAdjustment, BigDecimal net,
                        BigDecimal tax, BigDecimal total, String priceRuleId, String taxRuleId) {}

    public static Quote calculate(BigDecimal base, int quantity, String sport, String facility,
                                  String slot, LocalDate date, LocalTime time,
                                  Collection<Rule> rules, Collection<Tax> taxes) {
        Objects.requireNonNull(base); Objects.requireNonNull(date); Objects.requireNonNull(time);
        if (base.signum() < 0 || quantity < 1) throw new IllegalArgumentException("Invalid base price or quantity");
        Rule rule = rules.stream().filter(r -> r.matches(sport, facility, slot, date, time))
            .sorted(Comparator.comparingInt((Rule r) -> r.scope().ordinal()).reversed()
                .thenComparing(Comparator.comparingInt(Rule::priority).reversed())
                .thenComparing(Rule::id)).findFirst().orElse(null);
        BigDecimal unit = base;
        if (rule != null) {
            BigDecimal v = rule.value();
            if (v == null || v.signum() < 0) throw new IllegalArgumentException("Invalid price adjustment");
            unit = switch(rule.adjustment()) {
                case FIXED_PRICE -> v;
                case ADD_AMOUNT -> base.add(v);
                case DISCOUNT_AMOUNT -> base.subtract(v);
                case ADD_PERCENT -> base.multiply(BigDecimal.ONE.add(v.movePointLeft(2)));
                case DISCOUNT_PERCENT -> base.multiply(BigDecimal.ONE.subtract(v.movePointLeft(2)));
            };
        }
        unit = unit.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        Tax taxRule = taxes.stream().filter(t -> t.matches(sport, facility, slot, date))
            .sorted(Comparator.comparingInt((Tax t) -> t.scope().ordinal()).reversed()
                .thenComparing(Tax::id)).findFirst().orElse(null);
        BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(quantity));
        BigDecimal tax = BigDecimal.ZERO.setScale(2);
        BigDecimal net = subtotal;
        if (taxRule != null) {
            BigDecimal rate = taxRule.ratePercent();
            if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)
                throw new IllegalArgumentException("Invalid tax rate");
            BigDecimal multiplier = rate.movePointLeft(2);
            if (taxRule.inclusive()) {
                net = subtotal.divide(BigDecimal.ONE.add(multiplier), 2, RoundingMode.HALF_UP);
                tax = subtotal.subtract(net);
            } else {
                tax = subtotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return new Quote(base.setScale(2, RoundingMode.HALF_UP),unit,net,tax,
            net.add(tax).setScale(2, RoundingMode.HALF_UP),
            rule == null ? null : rule.id(), taxRule == null ? null : taxRule.id());
    }
}
