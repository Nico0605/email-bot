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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // Convert to list of maps for backwards compatibility
        return alerts.stream()
                .map(AlertInfo::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, List<Map<String, Object>>> loadPreviousDayFutures() {
        // In production, use: LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate yesterday = LocalDate.of(2025, 4, 10);

        // Optimize by collecting all futures types in a single loop
        return Map.of(
                "Monthly", futuresService.getFuturesMonth(yesterday.toString()),
                "Quarterly", futuresService.getFuturesQuarter(yesterday.toString()),
                "Yearly", futuresService.getFuturesYear(yesterday.toString())
        );
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
        //LocalDate checkDate = LocalDate.of(2025, 4, 10); // For simulation
        LocalDate checkDate = LocalDate.now(); // For production
        LocalDate dayBeforeCheckDate = checkDate.minusDays(1);

        // Carico i dati dei futures del giorno di controllo
        Map<String, List<Map<String, Object>>> futuresByType = loadPreviousDayFutures();

        // Carico anche i dati del giorno precedente, che serviranno solo in modalità percentuale
        Map<String, List<Map<String, Object>>> dayBeforeFuturesByType = Map.of(
                "Monthly", futuresService.getFuturesMonth(dayBeforeCheckDate.toString()),
                "Quarterly", futuresService.getFuturesQuarter(dayBeforeCheckDate.toString()),
                "Yearly", futuresService.getFuturesYear(dayBeforeCheckDate.toString())
        );

        List<Cliente> clients = clienteService.getClientsCheckEmail();

        for (Cliente cliente : clients) {
            int userId = cliente.getId();
            List<Map<String, Object>> alertData = checkUserAlertFillField(userId);

            if (alertData.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> triggeredAlerts = new ArrayList<>();

            // Process each alert configuration
            for (Map<String, Object> alert : alertData) {
                String alertType = alert.get("futuresType").toString();
                String futureType = alertType.replace("Alert", "");
                double max = (double) alert.get("maxPriceValue");
                double min = (double) alert.get("minPriceValue");
                boolean checkModality = (boolean) alert.get("checkModality");

                List<Map<String, Object>> futuresList = futuresByType.get(futureType);
                if (futuresList == null || futuresList.isEmpty()) {
                    System.out.println("⚠️ Nessun dato disponibile per il tipo: " + futureType);
                    continue;
                }

                for (Map<String, Object> future : futuresList) {
                    Map<String, Object> dayBeforeFuture = null;

                    // Se è in modalità variazione percentuale, devo trovare il dato corrispondente del giorno prima
                    if (checkModality) {
                        List<Map<String, Object>> dayBeforeFuturesList = dayBeforeFuturesByType.get(futureType);
                        if (dayBeforeFuturesList != null && !dayBeforeFuturesList.isEmpty()) {
                            // Cerca il future corrispondente del giorno precedente
                            Optional<Map<String, Object>> matchingDayBeforeFuture = dayBeforeFuturesList.stream()
                                    .filter(dbf -> matchFutures(futureType, future, dbf))
                                    .findFirst();

                            if (matchingDayBeforeFuture.isPresent()) {
                                dayBeforeFuture = matchingDayBeforeFuture.get();
                            } else {
                                System.out.println("⚠️ Nessun dato del giorno precedente trovato per: " +
                                        generateFutureLabel(futureType, future) + " (necessario per modalità percentuale)");
                                continue; // Skip this future
                            }
                        } else {
                            System.out.println("⚠️ Nessun dato del giorno precedente disponibile per il tipo: " +
                                    futureType + " (necessario per modalità percentuale)");
                            continue; // Skip this future type
                        }
                    }

                    // Chiama la funzione di controllo che gestirà autonomamente i casi con dayBeforeFuture null
                    Optional<Map<String, Object>> alertResult = checkPriceThreshold(
                            futureType,
                            future,
                            dayBeforeFuture,
                            min,
                            max,
                            checkModality
                    );

                    alertResult.ifPresent(alertEntry -> {
                        String futureLabel = (String) alertEntry.get("futureLabel");
                        double price = (double) alertEntry.get("price");
                        double variation = (double) alertEntry.get("variation");

                        System.out.println("⚠️ ALERT: Cliente " + cliente.getUsername() +
                                ", tipo: " + futureType + " (" + futureLabel + "), prezzo fuori soglia (" +
                                price + "), variazione: " + String.format("%.1f", variation) + "%");

                        triggeredAlerts.add(alertEntry);
                    });
                }
            }

            // Send email if there are alerts to report
            if (!triggeredAlerts.isEmpty()) {
                String htmlBody = emailTempGen.generateAlertEmail(
                        cliente.getUsername(),
                        cliente.getUsername(),
                        triggeredAlerts
                );

                mailer.send(
                        Mail.withHtml(cliente.getEmail(), "⚠️ Alert giornalieri sui futures", htmlBody)
                );

                System.out.println("📧 Email inviata a: " + cliente.getEmail());
            }
        }
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