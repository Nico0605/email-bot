package org.miesgroup.emailbot.service.Futures;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.miesgroup.emailbot.persistence.repository.FuturesRepo;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class FuturesService {

    @Inject FuturesRepo futuresRepo;

    @Inject
    public FuturesService(FuturesRepo futuresRepo) {
        this.futuresRepo = futuresRepo;
    }

    // Metodo generico per ottenere i futures in base al tipo
    public List<Map<String, Object>> getFutures(String date, String type) {
        return switch (type) {
            case "year" -> getFuturesYear(date);
            case "quarter" -> getFuturesQuarter(date);
            case "month" -> getFuturesMonth(date);
            default -> List.of();  // Se il tipo non è valido, restituisci una lista vuota
        };
    }

    public List<Map<String, Object>> getFuturesYear(String date) {
        return futuresRepo.findByYear(date).stream()
                .map(y -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("year", y.getYear());
                    map.put("settlementPrice", y.getFuture().getSettlementPrice());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFuturesQuarter(String date) {
        return futuresRepo.findByQuarter(date).stream()
                .map(q -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("year", q.getYear());
                    map.put("quarter", q.getQuarter());
                    map.put("settlementPrice", q.getFuture().getSettlementPrice());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFuturesMonth(String date) {
        return futuresRepo.findByMonth(date).stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("year", m.getYear());
                    map.put("month", m.getMonth());
                    map.put("settlementPrice", m.getFuture().getSettlementPrice());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public String getLastDate() {
        return futuresRepo.getLastDateFromYearlyFutures();
    }

    private String getQuarter(String month) {
        return switch (month) {
            case "01", "02", "03" -> "Q1";
            case "04", "05", "06" -> "Q2";
            case "07", "08", "09" -> "Q3";
            case "10", "11", "12" -> "Q4";
            default -> "";
        };
    }

    public List<Map<String, Object>> getAllFuturesByDate(String date) {
        List<Map<String, Object>> combined = new ArrayList<>();

        List<Map<String, Object>> monthly = getFuturesMonth(date).stream()
                .map(f -> {
                    f.put("future_type", "Monthly");
                    f.put("date", date);
                    return f;
                }).toList();

        List<Map<String, Object>> quarterly = getFuturesQuarter(date).stream()
                .map(f -> {
                    f.put("future_type", "Quarterly");
                    f.put("date", date);
                    return f;
                }).toList();

        List<Map<String, Object>> yearly = getFuturesYear(date).stream()
                .map(f -> {
                    f.put("future_type", "Yearly");
                    f.put("date", date);
                    return f;
                }).toList();

        combined.addAll(monthly);
        combined.addAll(quarterly);
        combined.addAll(yearly);

        return combined;
    }
    public List<Map<String, Object>> getAllFuturesBetweenDates(LocalDate start, LocalDate end) {
        List<Map<String, Object>> all = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            all.addAll(getAllFuturesByDate(d.toString()));
        }
        return all;
    }

}
