package org.miesgroup.emailbot.service.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.miesgroup.emailbot.persistence.entity.*;
import org.miesgroup.emailbot.persistence.repository.*;
import org.miesgroup.emailbot.service.Futures.FuturesService;
import org.miesgroup.emailbot.service.cliente.ClienteService;
import org.miesgroup.emailbot.utils.CalendarUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.quarkus.arc.impl.UncaughtExceptions.LOGGER;

@ApplicationScoped
public class EmailService {

    @Inject ClienteService clienteService;
    @Inject FuturesService futuresService;
    @Inject EmailTemplateGenerator emailTempGen;
    @Inject GeneralAlertRepo generalAlertRepo;
    @Inject MonthlyAlertRepo monthlyAlertRepo;
    @Inject QuarterlyAlertRepo quarterlyAlertRepo;
    @Inject YearlyAlertRepo yearlyAlertRepo;
    @Inject Mailer mailer;
    @Inject CalendarUtils calendarUtils;

    private static class AlertInfo {
        final String futuresType;
        final double maxPriceValue;
        final double minPriceValue;
        final boolean checkModality;
        final int idUtente;

        public AlertInfo(String futuresType, double maxPriceValue, double minPriceValue, boolean checkModality, int idUtente) {
            this.futuresType = futuresType;
            this.maxPriceValue = maxPriceValue;
            this.minPriceValue = minPriceValue;
            this.checkModality = checkModality;
            this.idUtente = idUtente;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("futuresType", futuresType);
            map.put("maxPriceValue", maxPriceValue);
            map.put("minPriceValue", minPriceValue);
            map.put("checkModality", checkModality);
            map.put("idUtente", idUtente);
            return map;
        }
    }

    public List<Map<String, Object>> checkUserAlertFillField(int idUtente) {
        List<AlertInfo> alerts = new ArrayList<>();

        generalAlertRepo.findByUserId(idUtente)
                .ifPresent(g -> alerts.add(new AlertInfo(
                        "GeneralAlert",
                        g.getMaxPriceValue(),
                        g.getMinPriceValue(),
                        g.getCheckModality(),
                        idUtente
                )));

        monthlyAlertRepo.findByUserId(idUtente)
                .ifPresent(m -> alerts.add(new AlertInfo(
                        "MonthlyAlert",
                        m.getMaxPriceValue(),
                        m.getMinPriceValue(),
                        m.getCheckModality(),
                        idUtente
                )));

        quarterlyAlertRepo.findByUserId(idUtente)
                .ifPresent(q -> alerts.add(new AlertInfo(
                        "QuarterlyAlert",
                        q.getMaxPriceValue(),
                        q.getMinPriceValue(),
                        q.getCheckModality(),
                        idUtente
                )));

        yearlyAlertRepo.findByUserId(idUtente)
                .ifPresent(y -> alerts.add(new AlertInfo(
                        "YearlyAlert",
                        y.getMaxPriceValue(),
                        y.getMinPriceValue(),
                        y.getCheckModality(),
                        idUtente
                )));

        return alerts.stream()
                .map(AlertInfo::toMap)
                .collect(Collectors.toList());
    }

    private String generateFutureLabel(String type, Map<String, Object> future) {
        return switch (type) {
            case "Monthly" -> future.get("year") + "-" + String.format("%02d", future.get("month"));
            case "Quarterly" -> future.get("year") + " Q" + future.get("quarter");
            case "Yearly" -> future.get("year").toString();
            default -> "N/A";
        };
    }

    private Optional<Map<String, Object>> checkPriceThreshold(String type, Map<String, Object> yesterdayFuture, Map<String, Object> dayBeforeFuture, double minThreshold, double maxThreshold, boolean checkModality) {
        double priceYesterday = ((Number) yesterdayFuture.get("settlementPrice")).doubleValue();

        boolean priceOutOfRange = false;
        double variation = 0.0;

        if (checkModality) {
            // Verifico se ho i dati del giorno precedente
            if (dayBeforeFuture == null) {
                System.out.println("⚠️ Dati del giorno precedente non disponibili per modalità di variazione percentuale");
                return Optional.empty();
            }

            double priceDayBefore = ((Number) dayBeforeFuture.get("settlementPrice")).doubleValue();
            System.out.println("💰 Prezzo ieri: " + priceYesterday + ", Prezzo giorno prima: " + priceDayBefore);

            if (priceDayBefore == 0) {
                System.out.println("⚠️ Divisione per zero evitata (prezzo giorno prima è 0)");
                return Optional.empty();
            }

            variation = ((priceYesterday - priceDayBefore) / priceDayBefore) * 100;
            System.out.println("📈 Variazione percentuale: " + variation + "%");

            if (variation < -minThreshold || variation > maxThreshold) {
                priceOutOfRange = true;
            }
        } else {
            // modalità assoluta - non ha bisogno dei dati del giorno precedente
            System.out.println("💰 Prezzo ieri: " + priceYesterday + " (modalità assoluta)");

            if (priceYesterday < minThreshold || priceYesterday > maxThreshold) {
                priceOutOfRange = true;
                variation = 0.0;
            }
        }

        if (priceOutOfRange) {
            String futureLabel = generateFutureLabel(type, yesterdayFuture);

            Map<String, Object> alertEntry = new HashMap<>();
            alertEntry.put("futuresType", type);
            alertEntry.put("futureLabel", futureLabel);
            alertEntry.put("price", priceYesterday);
            alertEntry.put("variation", variation);
            alertEntry.put("min", minThreshold);
            alertEntry.put("max", maxThreshold);
            alertEntry.put("checkModality", checkModality);

            return Optional.of(alertEntry);
        }

        return Optional.empty();
    }


    public void sendDailyEmail() {
        LocalDate today = LocalDate.now();

        // Trova l'ultimo giorno lavorativo (per i dati più recenti)
        LocalDate lastWorkingDay = findLastWorkingDay(today);

        // Trova il giorno lavorativo precedente a quello (per i confronti percentuali)
        LocalDate previousWorkingDay = findPreviousWorkingDay(lastWorkingDay);

        // Per la simulazione possiamo usare date specifiche (commentare in produzione)
        //yesterday = LocalDate.of(2025, 4, 8); // Per simulare "ieri"
        //previousWorkingDay = findPreviousWorkingDay(yesterday); // Calcola il giorno lavorativo precedente

        // Carico i dati dei futures di "ieri" (i più recenti disponibili)
        Map<String, List<Map<String, Object>>> futuresByType = Map.of(
                "Monthly", futuresService.getFuturesMonth(lastWorkingDay.toString()),
                "Quarterly", futuresService.getFuturesQuarter(lastWorkingDay.toString()),
                "Yearly", futuresService.getFuturesYear(lastWorkingDay.toString())
        );

        // Carico i dati dei futures del giorno lavorativo precedente, serviranno solo per modalità percentuale
        Map<String, List<Map<String, Object>>> previousWorkingDayFuturesByType = Map.of(
                "Monthly", futuresService.getFuturesMonth(previousWorkingDay.toString()),
                "Quarterly", futuresService.getFuturesQuarter(previousWorkingDay.toString()),
                "Yearly", futuresService.getFuturesYear(previousWorkingDay.toString())
        );

        List<Cliente> clients = clienteService.getClientsCheckEmail();
        System.out.println("Clienti trovati: " + (clients != null ? clients.size() : "null"));

        if (clients == null || clients.isEmpty()) {
            System.out.println("⚠️ Nessun cliente trovato per l'invio email");
            return;
        }

        for (Cliente cliente : clients) {
            System.out.println("Elaboro cliente: " + cliente.getUsername());
            int userId = cliente.getId();
            List<Map<String, Object>> alertData = checkUserAlertFillField(userId);

            if (alertData.isEmpty()) {
                System.out.println("⚠️ Nessun alert configurato per l'utente: " + cliente.getUsername());
                continue;
            }

            System.out.println("📋 Trovati " + alertData.size() + " alert per l'utente: " + cliente.getUsername());
            List<Map<String, Object>> triggeredAlerts = new ArrayList<>();

            // Process each alert configuration
            for (Map<String, Object> alert : alertData) {
                String alertType = alert.get("futuresType").toString();
                String futureType = alertType.replace("Alert", "");
                double max = (double) alert.get("maxPriceValue");
                double min = (double) alert.get("minPriceValue");
                boolean checkModality = (boolean) alert.get("checkModality");

                // Controllo del giorno della settimana globale
                DayOfWeek day = today.getDayOfWeek();
                if ((day == DayOfWeek.SUNDAY || day == DayOfWeek.MONDAY) && checkModality) {
                    LOGGER.error("⛔ Invio email giornaliera disattivato (oggi è " + day + ")");
                    return; // Esci dalla funzione se è domenica o lunedì
                } else if (day == DayOfWeek.SUNDAY) {
                    LOGGER.error("⛔ Invio email giornaliera disattivato (oggi è " + day + ")");
                    return; // Esci dalla funzione se è domenica o lunedì
                }

                LOGGER.info("📨 Avvio invio email giornaliera (oggi è " + day + ")");

                System.out.println("🔍 Elaboro alert: " + alertType +
                        ", checkModality: " + checkModality +
                        ", min: " + min +
                        ", max: " + max);

                // Recupera la lista dei futures per questo tipo
                List<Map<String, Object>> futuresList = futuresByType.get(futureType);
                if (futuresList == null || futuresList.isEmpty()) {
                    System.out.println("⚠️ Nessun dato disponibile per il tipo: " + futureType);
                    continue;
                }

                System.out.println("📊 Trovati " + futuresList.size() + " futures di tipo " + futureType);

                // Elabora ogni future di questo tipo
                for (Map<String, Object> future : futuresList) {
                    Map<String, Object> dayBeforeFuture = null;

                    // Solo se è in modalità percentuale, recupera i dati del giorno precedente
                    if (checkModality) {
                        System.out.println("📈 Modalità percentuale: cerco dati del giorno precedente");

                        List<Map<String, Object>> previousWorkingDayFuturesList = previousWorkingDayFuturesByType.get(futureType);
                        if (previousWorkingDayFuturesList != null && !previousWorkingDayFuturesList.isEmpty()) {
                            // Cerca il future corrispondente del giorno lavorativo precedente
                            Optional<Map<String, Object>> matchingPreviousWorkingDayFuture = previousWorkingDayFuturesList.stream()
                                    .filter(dbf -> matchFutures(futureType, future, dbf))
                                    .findFirst();

                            if (matchingPreviousWorkingDayFuture.isPresent()) {
                                dayBeforeFuture = matchingPreviousWorkingDayFuture.get();
                                System.out.println("✅ Trovato dato del giorno precedente per: " + generateFutureLabel(futureType, future));
                            } else {
                                System.out.println("⚠️ Nessun dato del giorno lavorativo precedente (" + previousWorkingDay + ") trovato per: " +
                                        generateFutureLabel(futureType, future) + " (necessario per modalità percentuale)");
                                continue; // Skip this future
                            }
                        } else {
                            System.out.println("⚠️ Nessun dato del giorno lavorativo precedente (" + previousWorkingDay + ") disponibile per il tipo: " +
                                    futureType + " (necessario per modalità percentuale)");
                            continue; // Skip this future type
                        }
                    } else {
                        System.out.println("💰 Modalità valore assoluto: controllo diretto con soglie");
                    }

                    // Chiama la funzione di controllo che gestirà entrambe le modalità
                    Optional<Map<String, Object>> alertResult = checkPriceThreshold(
                            futureType,
                            future,
                            dayBeforeFuture, // null se checkModality == false
                            min,
                            max,
                            checkModality
                    );

                    alertResult.ifPresent(alertEntry -> {
                        String futureLabel = (String) alertEntry.get("futureLabel");
                        double price = (double) alertEntry.get("price");
                        double variation = (double) alertEntry.get("variation");

                        System.out.println("⚠️ ALERT SCATENATO: Cliente " + cliente.getUsername() +
                                ", tipo: " + futureType + " (" + futureLabel + "), prezzo fuori soglia (" +
                                price + "), variazione: " + String.format("%.1f", variation) + "%");

                        triggeredAlerts.add(alertEntry);
                    });
                }
            }

            // Send email if there are alerts to report
            if (!triggeredAlerts.isEmpty()) {
                System.out.println("📧 Invio email con " + triggeredAlerts.size() + " alert a: " + cliente.getEmail());

                String htmlBody = emailTempGen.generateAlertEmail(
                        cliente.getUsername(),
                        cliente.getUsername(),
                        triggeredAlerts
                );

                System.out.println("✅ HTML generato, lunghezza: " + htmlBody.length());
                System.out.println("📤 Tentativo invio email...");

                try {
                    mailer.send(
                            Mail.withHtml(cliente.getEmail(), "⚠️ Alert giornalieri sui futures", htmlBody)
                    );
                    System.out.println("✅ Email inviata con successo a: " + cliente.getEmail());
                } catch (Exception e) {
                    System.out.println("❌ ERRORE nell'invio email: " + e.getMessage());
                    e.printStackTrace();
                }

                System.out.println("📬 Processo invio email completato");
            } else {
                System.out.println("ℹ️ Nessun alert scatenato per l'utente: " + cliente.getUsername());
            }
        }

        System.out.println("🏁 Elaborazione email giornaliere completata");
    }

    private LocalDate findPreviousWorkingDay(LocalDate date) {
        LocalDate previousDay = date.minusDays(1);

        // Se il giorno precedente è domenica, torniamo a venerdì
        if (previousDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return previousDay.minusDays(2); // Da domenica torna a venerdì
        }

        // Se il giorno precedente è sabato, torniamo a venerdì
        if (previousDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return previousDay.minusDays(1); // Da sabato torna a venerdì
        }

        // Altrimenti è già un giorno lavorativo
        return previousDay;
    }

    private LocalDate findLastWorkingDay(LocalDate date) {
        LocalDate checkDate = date.minusDays(1); // Partiamo da ieri

        // Se ieri era domenica, andiamo a venerdì
        if (checkDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return checkDate.minusDays(2); // Da domenica torna a venerdì
        }

        // Se ieri era sabato, andiamo a venerdì
        if (checkDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return checkDate.minusDays(1); // Da sabato torna a venerdì
        }

        // Altrimenti ieri era già un giorno lavorativo
        return checkDate;
    }

    private Map<String, List<Map<String, Object>>> groupFuturesByType(List<Map<String, Object>> futures) {
        return futures.stream()
                .collect(Collectors.groupingBy(f -> f.get("future_type").toString()));
    }

    private boolean matchFutures(String type, Map<String, Object> start, Map<String, Object> end) {
        return switch (type) {
            case "Monthly" -> Objects.equals(end.get("year"), start.get("year")) &&
                    Objects.equals(end.get("month"), start.get("month"));
            case "Quarterly" -> Objects.equals(end.get("year"), start.get("year")) &&
                    Objects.equals(end.get("quarter"), start.get("quarter"));
            case "Yearly" -> Objects.equals(end.get("year"), start.get("year"));
            default -> false;
        };
    }

    private List<Map<String, Object>> processFuturesForReport(Map<String, List<Map<String, Object>>> groupedStart, Map<String, List<Map<String, Object>>> groupedEnd, boolean isMonthly) {
        List<Map<String, Object>> reportData = new ArrayList<>();
        Set<String> allTypes = Stream.concat(
                groupedStart.keySet().stream(),
                groupedEnd.keySet().stream()
        ).collect(Collectors.toSet());

        for (String type : allTypes) {
            List<Map<String, Object>> startList = groupedStart.getOrDefault(type, List.of());
            List<Map<String, Object>> endList = groupedEnd.getOrDefault(type, List.of());

            for (Map<String, Object> start : startList) {
                String futureLabel = generateFutureLabel(type, start);

                Optional<Map<String, Object>> matchOpt = endList.stream()
                        .filter(end -> matchFutures(type, start, end))
                        .findFirst();

                if (matchOpt.isPresent()) {
                    Map<String, Object> end = matchOpt.get();
                    double startPrice = ((Number) start.get("settlementPrice")).doubleValue();
                    double endPrice = ((Number) end.get("settlementPrice")).doubleValue();

                    if (startPrice != 0) {
                        double variation = ((endPrice - startPrice) / startPrice) * 100;

                        Map<String, Object> entry = new HashMap<>();
                        entry.put("futuresType", type);
                        entry.put("futureLabel", futureLabel);

                        if (isMonthly) {
                            entry.put("startMonthPrice", startPrice);
                            entry.put("currentPrice", endPrice);
                            entry.put("monthlyTrend", variation);
                            entry.put("volatility", 0.0);
                        } else {
                            entry.put("startPrice", startPrice);
                            entry.put("endPrice", endPrice);
                            entry.put("weeklyTrend", variation);
                        }

                        reportData.add(entry);
                    }
                } else {
                    System.out.println("❗ Nessun dato di fine " +
                            (isMonthly ? "mese" : "settimana") +
                            " per: " + futureLabel + " (" + type + ")");
                }
            }
        }

        return reportData;
    }

    public void sendWeeklyEmail() {
        // Fixed dates for simulation (use commented code for production)
        //LocalDate previousMonday = LocalDate.of(2025, 3, 24);
        //LocalDate previousFriday = LocalDate.of(2025, 3, 28);

        // For production:
        LocalDate previousMonday = CalendarUtils.getPreviousMonday(LocalDate.now());
        LocalDate previousFriday = CalendarUtils.getPreviousFriday(LocalDate.now());

        List<Map<String, Object>> futuresStart = futuresService.getAllFuturesByDate(previousMonday.toString());
        List<Map<String, Object>> futuresEnd = futuresService.getAllFuturesByDate(previousFriday.toString());

        Map<String, List<Map<String, Object>>> groupedStart = groupFuturesByType(futuresStart);
        Map<String, List<Map<String, Object>>> groupedEnd = groupFuturesByType(futuresEnd);

        List<Cliente> clients = clienteService.getClientsCheckEmail();

        for (Cliente cliente : clients) {
            List<Map<String, Object>> weeklyData = processFuturesForReport(groupedStart, groupedEnd, false);

            if (!weeklyData.isEmpty()) {
                String html = emailTempGen.generateWeeklyReport(
                        cliente.getUsername(),
                        cliente.getUsername(),
                        weeklyData
                );

                mailer.send(
                        Mail.withHtml(cliente.getEmail(), "📈 Report settimanale mercati energetici", html)
                );

                System.out.println("📧 Report settimanale inviato a: " + cliente.getEmail());
            } else {
                System.out.println("⚠️ Nessun dato settimanale da inviare a: " + cliente.getEmail());
            }
        }
    }

    public void sendMonthlyEmail(){
        //YearMonth referenceMonth = YearMonth.of(2025, 3);
        // For production:
        YearMonth referenceMonth = YearMonth.now().minusMonths(1);
        LocalDate firstPossible = calendarUtils.getFirstWorkingDay(referenceMonth);
        LocalDate lastPossible = calendarUtils.getLastWorkingDay(referenceMonth);

        List<Map<String, Object>> futuresStart = futuresService.getAllFuturesByDate(firstPossible.toString());
        List<Map<String, Object>> futuresEnd = futuresService.getAllFuturesByDate(lastPossible.toString());

        if (futuresStart.isEmpty() || futuresEnd.isEmpty()) {
            System.out.println("❌ Nessun dato disponibile per il mese: " + referenceMonth);
            return;
        }

        Map<String, List<Map<String, Object>>> groupedStart = groupFuturesByType(futuresStart);
        Map<String, List<Map<String, Object>>> groupedEnd = groupFuturesByType(futuresEnd);

        List<Cliente> clients = clienteService.getClientsCheckEmail();

        for (Cliente cliente : clients) {
            List<Map<String, Object>> monthlyData = processFuturesForReport(groupedStart, groupedEnd, true);

            if (!monthlyData.isEmpty()) {
                String html = emailTempGen.generateMonthlyReport(
                        cliente.getUsername(),
                        cliente.getUsername(),
                        monthlyData
                );

                mailer.send(
                        Mail.withHtml(cliente.getEmail(), "📊 Report mensile mercati energetici", html)
                );

                System.out.println("📧 Email mensile inviata a: " + cliente.getEmail());
            } else {
                System.out.println("🚫 Nessun invio effettuato per: " + cliente.getEmail());
            }
        }
    }
}