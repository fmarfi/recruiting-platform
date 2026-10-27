package com.recruiting.platform.accounts.file.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds e-mail addresses, phone numbers and LinkedIn URLs in CV text with plain patterns (no AI). Suggestions only. */
public final class ContactDetector {

    private static final Pattern EMAIL = Pattern
            .compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+|00)?\\d[\\d\\s().-]{7,18}\\d(?!\\d)");
    private static final Pattern LINKEDIN = Pattern.compile(
            "(?:https?://)?(?:[a-z]{2,3}\\.)?linkedin\\.com/in/[A-Za-z0-9_%-]+", Pattern.CASE_INSENSITIVE);

    private ContactDetector() {
    }

    public static Map<String, List<String>> detect(String text) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("emails", emails(text));
        out.put("phones", phones(text));
        out.put("linkedinUrls", linkedin(text));
        return out;
    }

    private static List<String> emails(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher m = EMAIL.matcher(text);
        while (m.find() && found.size() < 5) {
            found.add(m.group().toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(found);
    }

    private static List<String> phones(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher m = PHONE.matcher(text);
        while (m.find() && found.size() < 5) {
            String raw = m.group().trim();
            int digits = raw.replaceAll("\\D", "").length();
            if (digits >= 10 && digits <= 15) {
                found.add(raw);
            }
        }
        return new ArrayList<>(found);
    }

    private static List<String> linkedin(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher m = LINKEDIN.matcher(text);
        while (m.find() && found.size() < 5) {
            String url = m.group();
            found.add(url.startsWith("http") ? url : "https://" + url);
        }
        return new ArrayList<>(found);
    }
}
