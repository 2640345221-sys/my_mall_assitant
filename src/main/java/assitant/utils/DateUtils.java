package assitant.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class DateUtils {

    public static LocalDateTime[] parsePeriod(String period) {
        if (period == null) return new LocalDateTime[]{null, null};
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        return switch (period) {
            case "today" -> new LocalDateTime[]{dayStart, dayEnd};
            case "yesterday" -> new LocalDateTime[]{today.minusDays(1).atStartOfDay(),
                    today.minusDays(1).atTime(LocalTime.MAX)};
            case "this_week" -> new LocalDateTime[]{
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(), dayEnd};
            case "last_week" -> new LocalDateTime[]{
                    today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(),
                    today.minusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX)};
            case "this_month" -> new LocalDateTime[]{
                    today.withDayOfMonth(1).atStartOfDay(), dayEnd};
            case "last_month" -> new LocalDateTime[]{
                    today.minusMonths(1).withDayOfMonth(1).atStartOfDay(),
                    today.withDayOfMonth(1).minusDays(1).atTime(LocalTime.MAX)};
            case "last_3_months" -> new LocalDateTime[]{
                    today.minusMonths(3).withDayOfMonth(1).atStartOfDay(), dayEnd};
            case "this_year" -> new LocalDateTime[]{
                    today.withDayOfYear(1).atStartOfDay(), dayEnd};
            default -> new LocalDateTime[]{null, null};
        };
    }
}
