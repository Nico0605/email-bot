package org.miesgroup.emailbot.scheduler;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.scheduler.Scheduled;
import org.miesgroup.emailbot.service.email.EmailManager;

import java.time.DayOfWeek;
import java.time.LocalDate;

@ApplicationScoped
public class ReportScheduler {

    @Inject
    EmailManager emailManager;

    @ConfigProperty(name = "scraper.schedule.enabled", defaultValue = "true")
    boolean scheduleEnabled;

    private LocalDate lastMonthlyEmailSent = null;

    private static final Logger LOGGER = Logger.getLogger(ReportScheduler.class);

    @Scheduled(cron = "0 0 8 * * ?")
    void scheduledDailyEmail() {
        if (!scheduleEnabled) return;

        LOGGER.info("📬 Inizio invio email giornaliere...");
        emailManager.DailyEmailAlert();
        LOGGER.info("✅ Invio email giornaliere completato.");
    }

    @Scheduled(cron = "0 0 7 ? * MON")
    void scheduledWeeklyEmail() {
        if (!scheduleEnabled) return;

        LOGGER.info("⏳ Invio report settimanale...");
        emailManager.WeeklyEmailAlert();
        LOGGER.info("✅ Report settimanale inviato.");
    }

    @Scheduled(cron = "0 0 6 1-3 * ?")
    void scheduledMonthlyEmail() {
        if (!scheduleEnabled) return;

        LocalDate today = LocalDate.now();

        // Se oggi è sabato o domenica, salta
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            LOGGER.info("⛔ Giorno non lavorativo, si rimanda il report mensile.");
            return;
        }

        // Evita invii duplicati: controlla se è già stato inviato questo mese
        if (alreadySentThisMonth(today)) {
            LOGGER.info("ℹ️ Report mensile già inviato per " + today.getMonth());
            return;
        }

        LOGGER.info("📊 Invio report mensile...");
        emailManager.MonthlyEmailAlert();
        LOGGER.info("✅ Report mensile inviato.");
    }

    private boolean alreadySentThisMonth(LocalDate today) {
        if (lastMonthlyEmailSent == null || !lastMonthlyEmailSent.getMonth().equals(today.getMonth())) {
            lastMonthlyEmailSent = today;
            return false;
        }
        return true;
    }

}
