package com.remail.mail;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class RegistrationMailParser {

    private static final List<String> URGENCY_KEYWORDS = List.of(
            "action required",
            "fill by eod",
            "urgent",
            "last date",
            "register",
            "deadline",
            "no manual registration"
    );

    public RegistrationMailSnapshot parse(MailIntakeDocument document) {
        String normalizedBody = normalize(document.body());
        String subject = firstNonBlank(document.subject(), extractTopTitle(normalizedBody));

        return new RegistrationMailSnapshot(
                subject,
                subject,
                extractBetween(normalizedBody, "Name of the Company", "Category"),
                extractBetween(normalizedBody, "Category", "Date of Visit:"),
                extractBetween(normalizedBody, "Last date for Registration", "Website"),
                extractBetween(normalizedBody, "Job location:", "Designation :"),
                extractWebsite(normalizedBody),
                extractBetween(normalizedBody, "Designation :", "Job Description :"),
                containsUrgency(normalizedBody + " " + subject),
                normalizedBody
        );
    }

    private String normalize(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        String stripped = Jsoup.parse(body).text();
        String normalized = Normalizer.normalize(stripped, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private String extractTopTitle(String normalizedBody) {
        Matcher matcher = Pattern.compile("(?i)^(?:##\\s*)?(.+?)\\s+(?:external|inbox|to\\b|$)").matcher(normalizedBody);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String extractBetween(String normalizedBody, String startLabel, String endLabel) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(startLabel) + "\\s*(.+?)\\s*(?:" + Pattern.quote(endLabel) + ")",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(normalizedBody);
        if (matcher.find()) {
            return cleanValue(matcher.group(1));
        }
        return "";
    }

    private String extractWebsite(String normalizedBody) {
        Matcher matcher = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE).matcher(normalizedBody);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return "";
    }

    private boolean containsUrgency(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return URGENCY_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String cleanValue(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }
}