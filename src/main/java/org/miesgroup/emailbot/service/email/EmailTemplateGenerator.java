package org.miesgroup.emailbot.service.email;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class EmailTemplateGenerator {

    public String generateAlertEmail(String clientName, String username, List<Map<String, Object>> alertDataList) {
        Map<String, List<Map<String, Object>>> alertsByType = new HashMap<>();

        for (Map<String, Object> alert : alertDataList) {
            String type = (String) alert.get("futuresType");
            if (type == null) continue;
            alertsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(alert);
        }

        int totalAlerts = alertDataList.size();
        StringBuilder summaryContent = new StringBuilder();
        summaryContent.append("<p>Il sistema ha identificato <strong>" + totalAlerts + " alert</strong> su prodotti energetici, con variazioni significative rispetto alle soglie personalizzate:</p>\n");
        summaryContent.append("<ul>\n");

        for (String type : alertsByType.keySet()) {
            List<Map<String, Object>> alerts = alertsByType.get(type);
            double minVariation = alerts.stream()
                    .mapToDouble(alert -> alert.get("variation") instanceof Number
                            ? ((Number) alert.get("variation")).doubleValue()
                            : 0.0)
                    .min().orElse(0);
            double maxVariation = alerts.stream()
                    .mapToDouble(alert -> alert.get("variation") instanceof Number
                            ? ((Number) alert.get("variation")).doubleValue()
                            : 0.0)
                    .max().orElse(0);

            summaryContent.append("    <li>" + alerts.size() + " prodotti <strong>" + type + "</strong> con variazioni tra "
                    + String.format("%.1f%%", minVariation) + " e " + String.format("%.1f%%", maxVariation) + "</li>\n");
        }

        summaryContent.append("</ul>\n");

        StringBuilder tablesContent = new StringBuilder();
        for (String type : alertsByType.keySet()) {
            List<Map<String, Object>> alerts = alertsByType.get(type);

            tablesContent.append("<p class=\"table-title\">Alert prodotti " + type + "</p>\n");
            tablesContent.append("<table class=\"price-table\">\n");
            tablesContent.append("    <thead>\n");
            tablesContent.append("        <tr>\n");
            tablesContent.append("            <th>" + (type.equals("Yearly") ? "Anno" : "Periodo") + "</th>\n");
            tablesContent.append("            <th>Prezzo attuale (€/MWh)</th>\n");
            tablesContent.append("            <th>Prezzo giorno precedente (€/MWh)</th>\n");
            tablesContent.append("            <th>Variazione</th>\n");
            tablesContent.append("            <th>Status</th>\n");
            tablesContent.append("        </tr>\n");
            tablesContent.append("    </thead>\n");
            tablesContent.append("    <tbody>\n");

            for (Map<String, Object> alert : alerts) {
                String label = alert.get("futureLabel") != null ? alert.get("futureLabel").toString() : "N/A";
                double price = alert.get("price") instanceof Number ? ((Number) alert.get("price")).doubleValue() : 0.0;
                double variation = alert.get("variation") instanceof Number ? ((Number) alert.get("variation")).doubleValue() : 0.0;
                boolean isPercentageMode = alert.get("checkModality") instanceof Boolean ? (Boolean) alert.get("checkModality") : false;

                // Calcolo del prezzo precedente dalla variazione percentuale se in modalità percentuale
                double previousPrice = 0.0;
                if (isPercentageMode && variation != 0.0) {
                    previousPrice = price / (1 + variation / 100);
                }

                // Aggiunta di una descrizione più dettagliata per l'alert
                String thresholdDescription;
                double min = alert.get("min") instanceof Number ? ((Number) alert.get("min")).doubleValue() : 0.0;
                double max = alert.get("max") instanceof Number ? ((Number) alert.get("max")).doubleValue() : 0.0;

                if (isPercentageMode) {
                    thresholdDescription = "Soglia percentuale: -" + String.format("%.1f%%", min) + "/+" + String.format("%.1f%%", max);
                } else {
                    thresholdDescription = "Soglia: " + String.format("%.2f", min) + " - " + String.format("%.2f", max) + " €/MWh";
                }

                // Determina la classe CSS per la variazione (positiva o negativa)
                String variationClass = variation < 0 ? "negative" : "positive";

                tablesContent.append("        <tr>\n");
                tablesContent.append("            <td>" + label + "</td>\n");
                tablesContent.append("            <td>" + String.format("%.2f", price) + "</td>\n");

                // Mostra il prezzo precedente solo se in modalità percentuale
                if (isPercentageMode) {
                    tablesContent.append("            <td>" + String.format("%.2f", previousPrice) + "</td>\n");
                    tablesContent.append("            <td class=\"" + variationClass + "\">" + String.format("%.1f%%", variation) + "</td>\n");
                } else {
                    tablesContent.append("            <td>N/A</td>\n");
                    tablesContent.append("            <td>N/A</td>\n");
                }

                tablesContent.append("            <td><span class=\"alert-icon\">⚠️</span> Fuori soglia<br><small>" + thresholdDescription + "</small></td>\n");
                tablesContent.append("        </tr>\n");
            }

            tablesContent.append("    </tbody>\n");
            tablesContent.append("</table>\n");
        }

        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return "<!DOCTYPE html>\n" +
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>MIES - Alert giornaliero futures</title>\n" +
                getEmailStyles() +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>MIES</h1>\n" +
                "            <p>Alert giornaliero futures</p>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Buongiorno " + clientName + ",</h2>\n" +
                "            <div class=\"alert-message\">\n" +
                "                <p>In seguito al monitoraggio dei mercati, il sistema ha rilevato variazioni anomale rispetto alle soglie da Lei impostate per l'account <strong>" + username + "</strong>.</p>\n" +
                "            </div>\n" +
                "            <div class=\"summary-box\">\n" +
                summaryContent.toString() +
                "            </div>\n" +
                "            <div class=\"table-section\">\n" +
                tablesContent.toString() +
                "            </div>\n" +
                "            <p>Le variazioni segnalate rappresentano opportunità o criticità rispetto alla soglia configurata. Si consiglia di valutare interventi correttivi o azioni di approvvigionamento.</p>\n" +
                "            <p>Per aggiornare le soglie personalizzate o ricevere supporto, contatti il Suo account manager.</p>\n" +
                "            <p>Cordiali saluti,<br>Il team MIES</p>\n" +
                "        </div>\n" +
                getEmailFooter() +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }


    public String generateWeeklyReport(String clientName, String username, List<Map<String, Object>> weeklyDataList) {
        // Organizzare i dati per tipo di prodotto
        Map<String, List<Map<String, Object>>> dataByType = new HashMap<>();

        for (Map<String, Object> data : weeklyDataList) {
            String type = (String) data.get("futuresType");
            if (type == null) continue;
            dataByType.computeIfAbsent(type, k -> new ArrayList<>()).add(data);
        }

        // Calcolare le statistiche settimanali per ogni tipo
        StringBuilder weeklyTrends = new StringBuilder();
        StringBuilder detailedContent = new StringBuilder();
        StringBuilder statsCardsContent = new StringBuilder();

        // Numero totale di prodotti analizzati
        int totalProducts = weeklyDataList.size();

        // Contare quanti prodotti hanno un trend positivo vs negativo
        long productsWithPositiveTrend = weeklyDataList.stream()
                .filter(data -> data.get("weeklyTrend") instanceof Number &&
                        ((Number) data.get("weeklyTrend")).doubleValue() > 0)
                .count();
        long productsWithNegativeTrend = weeklyDataList.stream()
                .filter(data -> data.get("weeklyTrend") instanceof Number &&
                        ((Number) data.get("weeklyTrend")).doubleValue() < 0)
                .count();

        // Calcolare il trend generale del mercato
        String marketTrend = productsWithPositiveTrend > productsWithNegativeTrend ?
                "in aumento" : "in diminuzione";

        // Costruire il riepilogo settimanale
        weeklyTrends.append("<p>Nell'ultima settimana sono stati analizzati <strong>" + totalProducts +
                " prodotti energetici</strong>, con il seguente andamento generale:</p>\n");
        weeklyTrends.append("<ul>\n");
        weeklyTrends.append("    <li><strong>" + productsWithPositiveTrend + " prodotti</strong> hanno registrato un aumento di prezzo</li>\n");
        weeklyTrends.append("    <li><strong>" + productsWithNegativeTrend + " prodotti</strong> hanno registrato una diminuzione di prezzo</li>\n");
        weeklyTrends.append("</ul>\n");
        weeklyTrends.append("<p>Il mercato energetico questa settimana è risultato prevalentemente <strong>" + marketTrend + "</strong>.</p>\n");

        // Costruire le stats card per ogni tipo di prodotto
        statsCardsContent.append("<div class=\"stats-card-container\">\n");

        for (String type : dataByType.keySet()) {
            List<Map<String, Object>> typeData = dataByType.get(type);

            // Calcolare statistiche per questo tipo
            DoubleSummaryStatistics trendStats = typeData.stream()
                    .filter(data -> data.get("weeklyTrend") instanceof Number)
                    .mapToDouble(data -> ((Number) data.get("weeklyTrend")).doubleValue())
                    .summaryStatistics();

            double avgTrend = trendStats.getAverage();
            String trendClass = avgTrend >= 0 ? "positive" : "negative";
            String trendIcon = avgTrend >= 0 ? "↗️" : "↘️";

            // Costruire la card per questo tipo
            statsCardsContent.append("    <div class=\"stats-card\">\n");
            statsCardsContent.append("        <h3>Futures " + type + "</h3>\n");
            statsCardsContent.append("        <div class=\"stats-values\">\n");
            statsCardsContent.append("            <span class=\"stats-value " + trendClass + "\">" +
                    String.format(avgTrend >= 0 ? "+%.2f%%" : "%.2f%%", avgTrend) + "</span>\n");
            statsCardsContent.append("            <span class=\"trend-icon\">" + trendIcon + "</span>\n");
            statsCardsContent.append("        </div>\n");
            statsCardsContent.append("        <p>Variazione media settimanale</p>\n");
            statsCardsContent.append("    </div>\n");
        }

        statsCardsContent.append("</div>\n");

        // Dettagli per tipo di prodotto
        for (String type : dataByType.keySet()) {
            List<Map<String, Object>> typeData = dataByType.get(type);

            // Calcolare statistiche per questo tipo
            DoubleSummaryStatistics trendStats = typeData.stream()
                    .filter(data -> data.get("weeklyTrend") instanceof Number)
                    .mapToDouble(data -> ((Number) data.get("weeklyTrend")).doubleValue())
                    .summaryStatistics();

            double avgTrend = trendStats.getAverage();
            double maxTrend = trendStats.getMax();
            double minTrend = trendStats.getMin();

            // Costruire la sezione per questo tipo
            detailedContent.append("<p class=\"table-title\">Analisi settimanale prodotti " + type + "</p>\n");
            detailedContent.append("<div class=\"trend-summary\">\n");
            detailedContent.append("    <p>Variazione media: <span class=\"" + (avgTrend >= 0 ? "positive" : "negative") +
                    "\">" + String.format(avgTrend >= 0 ? "+%.2f%%" : "%.2f%%", avgTrend) + "</span></p>\n");
            if (maxTrend > 0) {
                detailedContent.append("    <p>Maggiore aumento: <span class=\"positive\">" + String.format("%.2f%%", maxTrend) + "</span></p>\n");
            }

            if (minTrend < 0) {
                detailedContent.append("    <p>Maggiore diminuzione: <span class=\"negative\">" + String.format("%.2f%%", minTrend) + "</span></p>\n");
            }

            detailedContent.append("</div>\n");

            // Tabella dei dati
            detailedContent.append("<table class=\"price-table\">\n");
            detailedContent.append("    <thead>\n");
            detailedContent.append("        <tr>\n");
            detailedContent.append("            <th>").append(type.equals("Yearly") ? "Anno" : "Periodo").append("</th>\n");
            detailedContent.append("            <th>Prezzo inizio settimana (€/MWh)</th>\n");
            detailedContent.append("            <th>Prezzo fine settimana (€/MWh)</th>\n");
            detailedContent.append("            <th>Variazione settimanale</th>\n");
            detailedContent.append("            <th>Trend</th>\n");
            detailedContent.append("        </tr>\n");
            detailedContent.append("    </thead>\n");
            detailedContent.append("    <tbody>\n");

            for (Map<String, Object> data : typeData) {
                String label = data.get("futureLabel") != null ? data.get("futureLabel").toString() : "N/A";
                double startPrice = data.get("startPrice") instanceof Number ?
                        ((Number) data.get("startPrice")).doubleValue() : 0.0;
                double endPrice = data.get("endPrice") instanceof Number ?
                        ((Number) data.get("endPrice")).doubleValue() : 0.0;
                double weeklyTrend = data.get("weeklyTrend") instanceof Number ?
                        ((Number) data.get("weeklyTrend")).doubleValue() : 0.0;

                String trendClass = weeklyTrend >= 0 ? "positive" : "negative";
                String trendIcon = weeklyTrend >= 0 ? "↗️" : "↘️";

                detailedContent.append("        <tr>\n");
                detailedContent.append("            <td>").append(label).append("</td>\n");
                detailedContent.append("            <td>").append(String.format("%.2f", startPrice)).append("</td>\n");
                detailedContent.append("            <td>").append(String.format("%.2f", endPrice)).append("</td>\n");
                detailedContent.append("            <td class=\"" + trendClass + "\">").append(
                        String.format(weeklyTrend >= 0 ? "+%.2f%%" : "%.2f%%", weeklyTrend)).append("</td>\n");
                detailedContent.append("            <td><span class=\"trend-icon\">" + trendIcon + "</span></td>\n");
                detailedContent.append("        </tr>\n");
            }

            detailedContent.append("    </tbody>\n");
            detailedContent.append("</table>\n");
        }

        // Determinare le date della settimana
        LocalDate currentDate = LocalDate.now();
        LocalDate startOfWeek = currentDate.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        LocalDate endOfWeek = startOfWeek.plusDays(4); // Venerdì
        String weekPeriod = startOfWeek.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - " + endOfWeek.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Assemblare l'HTML completo
        return "<!DOCTYPE html>\n" +
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>MIES - Report settimanale futures</title>\n" +
                getEmailStyles() +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <!-- Header -->\n" +
                "        <div class=\"header\">\n" +
                "            <h1>MIES</h1>\n" +
                "            <p>Report settimanale futures</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- Contenuto Principale -->\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Buongiorno " + clientName + ",</h2>\n" +
                "            \n" +
                "            <div class=\"alert-message\">\n" +
                "                <p>Ecco il report settimanale sull'andamento dei mercati energetici per il periodo <strong>" +
                weekPeriod + "</strong> per l'account <strong>" + username + "</strong>.</p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class=\"summary-box\">\n" +
                weeklyTrends.toString() +
                "            </div>\n" +
                "            \n" +
                statsCardsContent.toString() +
                "            \n" +
                "            <div class=\"table-section\">\n" +
                detailedContent.toString() +
                "            </div>\n" +
                "            \n" +
                "            <p>I dati riportati mostrano l'evoluzione settimanale dei prezzi dell'energia, permettendole di valutare le tendenze del mercato e identificare potenziali opportunità di approvvigionamento.</p>\n" +
                "            \n" +
                "            <p>Per un'analisi personalizzata o per discutere le Sue strategie di approvvigionamento energetico, non esiti a contattare il Suo account manager di riferimento.</p>\n" +
                "            \n" +
                "            <p>Cordiali saluti,<br>Il Team MIES</p>\n" +
                "        </div>\n" +
                "        \n" +
                getEmailFooter() +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    public String generateMonthlyReport(String clientName, String username, List<Map<String, Object>> monthlyDataList) {
        Map<String, List<Map<String, Object>>> dataByType = new HashMap<>();

        for (Map<String, Object> data : monthlyDataList) {
            String type = (String) data.get("futuresType");
            if (type == null) continue;
            dataByType.computeIfAbsent(type, k -> new ArrayList<>()).add(data);
        }

        StringBuilder monthlyTrends = new StringBuilder();
        StringBuilder detailedContent = new StringBuilder();
        StringBuilder statsCardsContent = new StringBuilder();

        int totalProducts = monthlyDataList.size();

        long productsWithPositiveTrend = monthlyDataList.stream()
                .filter(data -> data.get("monthlyTrend") instanceof Number &&
                        ((Number) data.get("monthlyTrend")).doubleValue() > 0)
                .count();
        long productsWithNegativeTrend = monthlyDataList.stream()
                .filter(data -> data.get("monthlyTrend") instanceof Number &&
                        ((Number) data.get("monthlyTrend")).doubleValue() < 0)
                .count();

        String marketTrend = productsWithPositiveTrend > productsWithNegativeTrend ?
                "in aumento" : "in diminuzione";

        monthlyTrends.append("<p>Nell'ultimo mese sono stati analizzati <strong>" + totalProducts +
                " prodotti energetici</strong>, con il seguente andamento generale:</p>\n");
        monthlyTrends.append("<ul>\n");
        monthlyTrends.append("    <li><strong>" + productsWithPositiveTrend + " prodotti</strong> hanno registrato un aumento di prezzo</li>\n");
        monthlyTrends.append("    <li><strong>" + productsWithNegativeTrend + " prodotti</strong> hanno registrato una diminuzione di prezzo</li>\n");
        monthlyTrends.append("</ul>\n");
        monthlyTrends.append("<p>Il mercato energetico questo mese è risultato prevalentemente <strong>" + marketTrend + "</strong>.</p>\n");

        statsCardsContent.append("<div class=\"stats-card-container\">\n");

        for (String type : dataByType.keySet()) {
            List<Map<String, Object>> typeData = dataByType.get(type);

            DoubleSummaryStatistics trendStats = typeData.stream()
                    .filter(data -> data.get("monthlyTrend") instanceof Number)
                    .mapToDouble(data -> ((Number) data.get("monthlyTrend")).doubleValue())
                    .summaryStatistics();

            double avgTrend = trendStats.getAverage();
            String trendClass = avgTrend >= 0 ? "positive" : "negative";
            String trendIcon = avgTrend >= 0 ? "↗️" : "↘️";

            statsCardsContent.append("    <div class=\"stats-card\">\n");
            statsCardsContent.append("        <h3>Futures " + type + "</h3>\n");
            statsCardsContent.append("        <div class=\"stats-values\">\n");
            statsCardsContent.append("            <span class=\"stats-value " + trendClass + "\">" +
                    String.format(avgTrend >= 0 ? "+%.2f%%" : "%.2f%%", avgTrend) + "</span>\n");
            statsCardsContent.append("            <span class=\"trend-icon\">" + trendIcon + "</span>\n");
            statsCardsContent.append("        </div>\n");
            statsCardsContent.append("        <p>Variazione media mensile</p>\n");
            statsCardsContent.append("    </div>\n");
        }

        statsCardsContent.append("</div>\n");

        for (String type : dataByType.keySet()) {
            List<Map<String, Object>> typeData = dataByType.get(type);

            DoubleSummaryStatistics trendStats = typeData.stream()
                    .filter(data -> data.get("monthlyTrend") instanceof Number)
                    .mapToDouble(data -> ((Number) data.get("monthlyTrend")).doubleValue())
                    .summaryStatistics();

            double avgTrend = trendStats.getAverage();
            double maxTrend = trendStats.getMax();
            double minTrend = trendStats.getMin();

            detailedContent.append("<p class=\"table-title\">Analisi mensile prodotti " + type + "</p>\n");
            detailedContent.append("<div class=\"trend-summary\">\n");
            detailedContent.append("    <p>Variazione media: <span class=\"" + (avgTrend >= 0 ? "positive" : "negative") +
                    "\">" + String.format(avgTrend >= 0 ? "+%.2f%%" : "%.2f%%", avgTrend) + "</span></p>\n");
            if (maxTrend > 0) {
                detailedContent.append("    <p>Maggiore aumento: <span class=\"positive\">" + String.format("%.2f%%", maxTrend) + "</span></p>\n");
            }
            if (minTrend < 0) {
                detailedContent.append("    <p>Maggiore diminuzione: <span class=\"negative\">" + String.format("%.2f%%", minTrend) + "</span></p>\n");
            }
            detailedContent.append("</div>\n");

            detailedContent.append("<table class=\"price-table\">\n");
            detailedContent.append("    <thead>\n");
            detailedContent.append("        <tr>\n");
            detailedContent.append("            <th>" + (type.equals("Yearly") ? "Anno" : "Periodo") + "</th>\n");
            detailedContent.append("            <th>Prezzo inizio mese (€/MWh)</th>\n");
            detailedContent.append("            <th>Prezzo fine mese (€/MWh)</th>\n");
            detailedContent.append("            <th>Variazione mensile</th>\n");
            detailedContent.append("            <th>Trend</th>\n");
            detailedContent.append("        </tr>\n");
            detailedContent.append("    </thead>\n");
            detailedContent.append("    <tbody>\n");

            for (Map<String, Object> data : typeData) {
                String label = data.get("futureLabel") != null ? data.get("futureLabel").toString() : "N/A";
                double startPrice = data.get("startMonthPrice") instanceof Number ?
                        ((Number) data.get("startMonthPrice")).doubleValue() : 0.0;
                double currentPrice = data.get("currentPrice") instanceof Number ?
                        ((Number) data.get("currentPrice")).doubleValue() : 0.0;
                double monthlyTrend = data.get("monthlyTrend") instanceof Number ?
                        ((Number) data.get("monthlyTrend")).doubleValue() : 0.0;

                String trendClass = monthlyTrend >= 0 ? "positive" : "negative";
                String trendIcon = monthlyTrend >= 0.5 ? "↗️" : (monthlyTrend <= -0.5 ? "↘️" : "↔️");

                detailedContent.append("        <tr>\n");
                detailedContent.append("            <td>" + label + "</td>\n");
                detailedContent.append("            <td>" + String.format("%.2f", startPrice) + "</td>\n");
                detailedContent.append("            <td>" + String.format("%.2f", currentPrice) + "</td>\n");
                detailedContent.append("            <td class=\"" + trendClass + "\">" + String.format("%.2f%%", monthlyTrend) + "</td>\n");
                detailedContent.append("            <td><span class=\"trend-icon\">" + trendIcon + "</span></td>\n");
                detailedContent.append("        </tr>\n");
            }

            detailedContent.append("    </tbody>\n");
            detailedContent.append("</table>\n");
        }

        // Generazione dinamica del periodo mensile
        LocalDate currentDate = LocalDate.now();
        LocalDate startOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = currentDate.with(TemporalAdjusters.lastDayOfMonth());
        String monthPeriod = startOfMonth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - " + endOfMonth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return "<!DOCTYPE html>\n" +
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>MIES - Report mensile futures</title>\n" +
                getEmailStyles() +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>MIES</h1>\n" +
                "            <p>Report mensile futures</p>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Buongiorno " + clientName + ",</h2>\n" +
                "            <div class=\"alert-message\">\n" +
                "                <p>Ecco il report mensile sull'andamento dei mercati energetici per il periodo <strong>" + monthPeriod + "</strong> per l'account <strong>" + username + "</strong>.</p>\n" +
                "            </div>\n" +
                "            <div class=\"summary-box\">\n" +
                monthlyTrends.toString() +
                "            </div>\n" +
                statsCardsContent.toString() +
                "            <div class=\"table-section\">\n" +
                detailedContent.toString() +
                "            </div>\n" +
                "            <p>Questo report mensile aiuta ad analizzare i trend del mercato energetico per una migliore pianificazione degli approvvigionamenti.</p>\n" +
                "            <p>Per un supporto personalizzato, contatti il Suo referente commerciale MIES.</p>\n" +
                "            <p>Cordiali saluti,<br>Il Team MIES</p>\n" +
                "        </div>\n" +
                getEmailFooter() +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }


    private String getEmailStyles() {
        return "    <style>\n" +
                "        /* Reset CSS */\n" +
                "        body, p, h1, h2, h3, h4, h5, h6, ul, ol, li {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        body {\n" +
                "            font-family: 'Helvetica Neue', Arial, sans-serif;\n" +
                "            color: #333333;\n" +
                "            line-height: 1.6;\n" +
                "            background-color: #f5f5f5;\n" +
                "        }\n" +
                "        /* Container principale */\n" +
                "        .email-container {\n" +
                "            max-width: 750px;\n" +
                "            margin: 20px auto;\n" +
                "            background-color: #ffffff;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        /* Header */\n" +
                "        .header {\n" +
                "            background: linear-gradient(135deg, #27526A 0%, #1a3d50 100%);\n" +
                "            padding: 25px 30px;\n" +
                "            color: white;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .logo {\n" +
                "            height: auto;\n" +
                "            max-width: 120px;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            font-size: 32px;\n" +
                "            font-weight: 700;\n" +
                "            margin-bottom: 5px;\n" +
                "            letter-spacing: 1px;\n" +
                "        }\n" +
                "        .header p {\n" +
                "            font-size: 18px;\n" +
                "            opacity: 0.9;\n" +
                "            font-weight: 300;\n" +
                "        }\n" +
                "        /* Contenuto */\n" +
                "        .content {\n" +
                "            padding: 35px;\n" +
                "            background-color: #ffffff;\n" +
                "        }\n" +
                "        .content h2 {\n" +
                "            font-size: 22px;\n" +
                "            margin-bottom: 25px;\n" +
                "            color: #27526A;\n" +
                "            border-bottom: 2px solid #eaeaea;\n" +
                "            padding-bottom: 10px;\n" +
                "        }\n" +
                "        .content p {\n" +
                "            margin-bottom: 20px;\n" +
                "            font-size: 16px;\n" +
                "            color: #4B5563;\n" +
                "        }\n" +
                "        .alert-message {\n" +
                "            background-color: #f9f9f9;\n" +
                "            border-left: 4px solid #27526A;\n" +
                "            padding: 18px;\n" +
                "            margin-bottom: 30px;\n" +
                "            border-radius: 0 4px 4px 0;\n" +
                "        }\n" +
                "        /* Summary Box */\n" +
                "        .summary-box {\n" +
                "            background: linear-gradient(to right, #f8fbfd, #edf5fa);\n" +
                "            border: none;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 25px;\n" +
                "            margin-bottom: 35px;\n" +
                "            box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n" +
                "        }\n" +
                "        .summary-box p {\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .summary-box strong {\n" +
                "            color: #27526A;\n" +
                "        }\n" +
                "        .summary-box ul {\n" +
                "            list-style-type: none;\n" +
                "            margin: 15px 0;\n" +
                "            padding-left: 10px;\n" +
                "        }\n" +
                "        .summary-box li {\n" +
                "            margin-bottom: 10px;\n" +
                "            position: relative;\n" +
                "            padding-left: 25px;\n" +
                "        }\n" +
                "        .summary-box li:before {\n" +
                "            content: \"•\";\n" +
                "            color: #27526A;\n" +
                "            font-weight: bold;\n" +
                "            font-size: 18px;\n" +
                "            position: absolute;\n" +
                "            left: 0;\n" +
                "        }\n" +
                "        /* Stats Card */\n" +
                "        .stats-card-container {\n" +
                "            display: flex;\n" +
                "            flex-wrap: wrap;\n" +
                "            gap: 20px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .stats-card {\n" +
                "            flex: 1;\n" +
                "            min-width: 200px;\n" +
                "            background: white;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 20px;\n" +
                "            box-shadow: 0 2px 8px rgba(0,0,0,0.08);\n" +
                "            border-top: 4px solid #27526A;\n" +
                "        }\n" +
                "        .stats-card h3 {\n" +
                "            font-size: 16px;\n" +
                "            color: #666;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .stats-values {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .stats-value {\n" +
                "            font-size: 28px;\n" +
                "            font-weight: 700;\n" +
                "        }\n" +
                "        .positive {\n" +
                "            color: #28a745;\n" +
                "        }\n" +
                "        .negative {\n" +
                "            color: #dc3545;\n" +
                "        }\n" +
                "        .trend-icon {\n" +
                "            font-size: 24px;\n" +
                "            margin-left: 10px;\n" +
                "        }\n" +
                "        /* Tabelle */\n" +
                "        .table-section {\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .table-title {\n" +
                "            font-size: 20px;\n" +
                "            font-weight: 600;\n" +
                "            margin: 30px 0 15px 0;\n" +
                "            color: #27526A;\n" +
                "            padding-bottom: 8px;\n" +
                "            border-bottom: 2px solid rgba(39, 82, 106, 0.2);\n" +
                "        }\n" +
                "        .price-table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: separate;\n" +
                "            border-spacing: 0;\n" +
                "            margin-bottom: 30px;\n" +
                "            font-size: 14px;\n" +
                "            box-shadow: 0 1px 3px rgba(0,0,0,0.1);\n" +
                "            border-radius: 8px;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        .price-table th {\n" +
                "            background: linear-gradient(to right, #27526A, #1a3d50);\n" +
                "            color: white;\n" +
                "            text-align: left;\n" +
                "            padding: 14px 15px;\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        .price-table td {\n" +
                "            padding: 12px 15px;\n" +
                "            border-top: 1px solid #edf2f7;\n" +
                "        }\n" +
                "        .price-table tr:last-child td {\n" +
                "            border-bottom: none;\n" +
                "        }\n" +
                "        .price-table tr:hover {\n" +
                "            background-color: #f9fafb;\n" +
                "        }\n" +
                "        /* Trend Summary */\n" +
                "        .trend-summary {\n" +
                "            background-color: white;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 15px 20px;\n" +
                "            margin: 15px 0 25px;\n" +
                "            box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            flex-wrap: wrap;\n" +
                "        }\n" +
                "        .trend-summary p {\n" +
                "            margin: 5px 15px 5px 0;\n" +
                "            white-space: nowrap;\n" +
                "        }\n" +
                "        /* Footer */\n" +
                "        .footer {\n" +
                "            background-color: #f8f9fa;\n" +
                "            padding: 25px 35px;\n" +
                "            font-size: 13px;\n" +
                "            color: #666;\n" +
                "            border-top: 1px solid #e1e1e1;\n" +
                "        }\n" +
                "        .footer p {\n" +
                "            margin-bottom: 5px;\n" +
                "        }\n" +
                "        .company-info {\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .company-info a {\n" +
                "            color: #27526A;\n" +
                "            text-decoration: none;\n" +
                "        }\n" +
                "        .legal-info {\n" +
                "            font-size: 11px;\n" +
                "            color: #888;\n" +
                "            border-top: 1px solid #e1e1e1;\n" +
                "            padding-top: 15px;\n" +
                "            margin-top: 15px;\n" +
                "        }\n" +
                "        .environmental-notice {\n" +
                "            font-style: italic;\n" +
                "            color: #27526A;\n" +
                "            margin-top: 20px;\n" +
                "            text-align: center;\n" +
                "            padding: 10px;\n" +
                "            background-color: #f0f5f8;\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        /* Responsive */\n" +
                "        @media only screen and (max-width: 600px) {\n" +
                "            .email-container {\n" +
                "                margin: 10px;\n" +
                "                border-radius: 6px;\n" +
                "            }\n" +
                "            .header, .content, .footer {\n" +
                "                padding: 20px;\n" +
                "            }\n" +
                "            .header h1 {\n" +
                "                font-size: 24px;\n" +
                "            }\n" +
                "            .header p {\n" +
                "                font-size: 16px;\n" +
                "            }\n" +
                "            .price-table {\n" +
                "                font-size: 12px;\n" +
                "            }\n" +
                "            .stats-card-container {\n" +
                "                flex-direction: column;\n" +
                "            }\n" +
                "            .trend-summary {\n" +
                "                flex-direction: column;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n";
    }

    private String getEmailFooter() {
        return "        <!-- Footer -->\n" +
                "        <div class=\"footer\">\n" +
                "            <div class=\"company-info\">\n" +
                "                <p><strong>MIES Group - Energy Portfolio</strong></p>\n" +
                "                <p><a href=\"http://www.miesgroup.it\">www.miesgroup.it</a></p>\n" +
                "                <p><strong>Sede Legale</strong></p>\n" +
                "                <p>Via Puricelli, 1 – Gallarate (VA)</p>\n" +
                "                <p>P.IVA 03635250123</p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class=\"legal-info\">\n" +
                "                <p>** AVVERTENZE AI SENSI DEL DLGS 196/2003 e del Regolamento Europeo 679/2016 (GDPR) **</p>\n" +
                "                <p>**Le informazioni contenute in questo messaggio di posta elettronica e/o nell'eventuale file/s allegato/i, sono da considerarsi strettamente riservate. Il loro utilizzo è consentito esclusivamente al destinatario del messaggio, per le finalità indicate nel messaggio stesso. Costituisce comportamento contrario ai principi dettati dall'art. 616 c.p., ART. 13 Dlgs. 196/2003 e art. 13 UE GDPR il trattenere il messaggio stesso, divulgarlo anche in parte, distribuirlo ad altri soggetti, copiarlo, od utilizzarlo per finalità diverse. Se avete ricevuto questa mail per errore vogliate eliminare il messaggio in modo permanente e darcene cortesemente notizia**</p>\n" +
                "                <p>** The information in this e-mail (which includes any attached files) is confidential and may be legally privileged (art. 616 c.p., Dlgs 196/2003 e UE GDPR). It is intended for the addressee only. Any use, dissemination, forwarding, printing or copying of this e-mail is prohibited by any person other than the addressee. If you have received this e-mail in error please notify us immediately by e-mail promptly and destroy this message**</p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class=\"environmental-notice\">\n" +
                "                <p>** Prima di stampare, pensa all'ambiente! ** Think about the environment before printing **</p>\n" +
                "            </div>\n" +
                "        </div>\n";
    }

    // Metodo ausiliario per calcolare la volatilità dai dati
    private double calculateVolatility(List<Double> prices) {
        if (prices == null || prices.size() < 2) {
            return 0.0;
        }

        // Calcola le variazioni percentuali giornaliere
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            double dailyReturn = (prices.get(i) / prices.get(i-1) - 1) * 100;
            dailyReturns.add(dailyReturn);
        }

        // Calcola la deviazione standard delle variazioni
        double mean = dailyReturns.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = dailyReturns.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    // Metodo per generare l'analisi dei pattern di prezzo
    private String generatePricePatternAnalysis(String type, List<Double> prices) {
        StringBuilder analysis = new StringBuilder();

        if (prices == null || prices.size() < 10) {
            return "";
        }

        // Identifica pattern di prezzo
        boolean hasUptrendPattern = false;
        boolean hasDowntrendPattern = false;
        boolean hasSupportLevel = false;
        boolean hasResistanceLevel = false;

        // Analisi trend recente (ultimi 10 punti)
        List<Double> recentPrices = prices.subList(Math.max(0, prices.size() - 10), prices.size());
        double firstPrice = recentPrices.get(0);
        double lastPrice = recentPrices.get(recentPrices.size() - 1);

        if (lastPrice > firstPrice * 1.05) {
            hasUptrendPattern = true;
        } else if (lastPrice < firstPrice * 0.95) {
            hasDowntrendPattern = true;
        }

        // Analisi livelli di supporto e resistenza (semplificato)
        double min = prices.stream().mapToDouble(d -> d).min().orElse(0.0);
        double max = prices.stream().mapToDouble(d -> d).max().orElse(0.0);

        // Verifica se il prezzo ha "rimbalzato" da livelli minimi
        if (lastPrice > min * 1.02 && lastPrice < min * 1.05) {
            hasSupportLevel = true;
        }

        // Verifica se il prezzo ha incontrato resistenza vicino ai massimi
        if (lastPrice < max * 0.98 && lastPrice > max * 0.95) {
            hasResistanceLevel = true;
        }

        // Genera analisi testuale
        analysis.append("<div class=\"pattern-analysis\">\n");
        analysis.append("<h4>Analisi pattern di prezzo " + type + "</h4>\n");

        if (hasUptrendPattern) {
            analysis.append("<p>✓ <strong>Pattern rialzista identificato</strong> - Il prezzo mostra una tendenza al rialzo negli ultimi periodi.</p>\n");
        }

        if (hasDowntrendPattern) {
            analysis.append("<p>✓ <strong>Pattern ribassista identificato</strong> - Il prezzo mostra una tendenza al ribasso negli ultimi periodi.</p>\n");
        }

        if (hasSupportLevel) {
            analysis.append("<p>✓ <strong>Livello di supporto</strong> - Il prezzo sembra aver trovato supporto intorno a " +
                    String.format("%.2f", min) + " €/MWh.</p>\n");
        }

        if (hasResistanceLevel) {
            analysis.append("<p>✓ <strong>Livello di resistenza</strong> - Il prezzo sembra aver incontrato resistenza intorno a " +
                    String.format("%.2f", max) + " €/MWh.</p>\n");
        }

        if (!hasUptrendPattern && !hasDowntrendPattern && !hasSupportLevel && !hasResistanceLevel) {
            analysis.append("<p>Nessun pattern significativo identificato nel periodo analizzato.</p>\n");
        }

        analysis.append("</div>\n");

        return analysis.toString();
    }
}