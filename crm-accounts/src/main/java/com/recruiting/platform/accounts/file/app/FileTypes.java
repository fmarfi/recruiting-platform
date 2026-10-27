package com.recruiting.platform.accounts.file.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/** Detects the media type from the bytes with Tika (never from the file name) and checks the allow-list. */
@Component
public class FileTypes {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED = Set.of("application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword",
            "application/rtf", "text/rtf", "application/vnd.oasis.opendocument.text", "text/plain");

    private final Tika tika = new Tika();

    public String detect(byte[] bytes) {
        try {
            return tika.detect(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    public boolean allowed(String contentType) {
        return ALLOWED.contains(contentType);
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The original name without any path, at most 255 characters. */
    public static String cleanName(String original) {
        String name = original == null ? "file" : original.replace((char) 92, '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            name = "file";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
