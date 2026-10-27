package com.recruiting.platform.accounts.person.app;

import java.net.URI;
import java.util.Locale;

/** Normal forms for the duplicate check (columns phone_normalized and linkedin_url_normalized). */
public final class PersonNormalizer {

    private PersonNormalizer() {
    }

    /**
     * Digits with country code, default +90: "0555 000 00 00", "555 000 00 00", "0090 555 ..." and "+90 (555) ..."
     * all give "+905550000000". Null when there are fewer than 7 digits or more than 15.
     */
    public static String phone(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        boolean plus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return null;
        }
        String full;
        if (plus) {
            full = digits;
        } else if (digits.startsWith("00")) {
            full = digits.substring(2);
        } else if (digits.startsWith("0")) {
            full = "90" + digits.substring(1);
        } else if (digits.length() == 12 && digits.startsWith("90")) {
            full = digits;
        } else if (digits.length() == 10) {
            full = "90" + digits;
        } else {
            full = digits;
        }
        if (full.length() > 15) {
            return null;
        }
        return "+" + full;
    }

    /** "https://www.linkedin.com/in/slug", lower case, no query or trailing slash; null if not a profile URL. */
    public static String linkedin(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        if (!text.contains("://")) {
            text = "https://" + text;
        }
        try {
            URI uri = URI.create(text);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!(host.equals("linkedin.com") || host.endsWith(".linkedin.com"))) {
                return null;
            }
            String path = uri.getPath() == null ? "" : uri.getPath();
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("in") && !parts[i + 1].isBlank()) {
                    return "https://www.linkedin.com/in/" + parts[i + 1].toLowerCase(Locale.ROOT);
                }
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
