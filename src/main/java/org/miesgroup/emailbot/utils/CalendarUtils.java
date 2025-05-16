package org.miesgroup.emailbot.utils;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

@ApplicationScoped
public class CalendarUtils {

    public LocalDate getFirstWorkingDay(YearMonth month) {
        LocalDate date = month.atDay(1);
        while (isWeekend(date)) {
            date = date.plusDays(1);
        }
        return date;
    }
    public LocalDate getLastWorkingDay(YearMonth month) {
        LocalDate date = month.atEndOfMonth();
        while (isWeekend(date)) {
            date = date.minusDays(1);
        }
        return date;
    }
    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static LocalDate getPreviousMonday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    public static LocalDate getPreviousFriday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.FRIDAY) {
            date = date.minusDays(1);
        }
        return date;
    }
}
