package org.miesgroup.emailbot.utils;

import java.util.Map;

public class EmailUtils {
    public static String generateFutureLabel(Map<String, Object> future) {
        String type = future.get("future_type").toString();
        int year = (int) future.get("year");

        return switch (type) {
            case "Monthly" -> future.get("month") != null ? year + "-" + String.format("%02d", future.get("month")) : "N/A";
            case "Quarterly" -> future.get("quarter") != null ? year + " Q" + future.get("quarter") : "N/A";
            case "Yearly" -> String.valueOf(year);
            default -> "N/A";
        };
    }
}


