package org.miesgroup.emailbot.service.email;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static io.quarkus.arc.impl.UncaughtExceptions.LOGGER;

@ApplicationScoped
public class EmailManager {

    @Inject
    EmailService emailService;

    public void DailyEmailAlert() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();

        if (day != DayOfWeek.SUNDAY && day != DayOfWeek.MONDAY) {
            LOGGER.info("📨 Avvio invio email giornaliera (oggi è " + day + ")");
            emailService.sendDailyEmail();
        } else {
            LOGGER.error("⛔ Invio email giornaliera disattivato (oggi è " + day + ")");
        }
    }

    public void WeeklyEmailAlert() {
        emailService.sendWeeklyEmail();
    }

    public void MonthlyEmailAlert() {
        emailService.sendMonthlyEmail();
    }
}
