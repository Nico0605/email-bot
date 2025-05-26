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
        emailService.sendDailyEmail();
    }

    public void WeeklyEmailAlert() {
        emailService.sendWeeklyEmail();
    }

    public void MonthlyEmailAlert() {
        emailService.sendMonthlyEmail();
    }
}
