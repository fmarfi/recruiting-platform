package com.recruiting.platform.privacy.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * Logback converter ({@code %mask}) that hides consent tokens in log lines: anything that follows
 * {@code /public/consent/} is replaced by {@code ***}. The link is a bearer secret, so it must not reach log files.
 */
public class TokenMaskingConverter extends MessageConverter {

    private static final Pattern TOKEN = Pattern.compile("(/public/consent/)[A-Za-z0-9_\\-]{8,}");

    @Override
    public String convert(ILoggingEvent event) {
        return mask(event.getFormattedMessage());
    }

    /** Replaces every consent token in the text. */
    public static String mask(String text) {
        return text == null ? null : TOKEN.matcher(text).replaceAll("$1***");
    }
}
