package com.recruiting.platform.accounts.importing.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads an uploaded CSV or XLSX file into a header-keyed table. Header names are matched case-insensitively and
 * rows that are completely blank are dropped. Throws IllegalArgumentException when the file is unreadable.
 */
public final class TableReader {

    private TableReader() {
    }

    /** @param headers lower-case header names; @param rows data rows padded to the header width */
    public record Table(List<String> headers, List<List<String>> rows) {
    }

    public static boolean isXlsx(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    public static boolean isCsv(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    public static Table read(String fileName, byte[] content) {
        List<List<String>> raw = isXlsx(fileName) ? XlsxTableReader.read(content) : CsvTableReader.read(content);
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("The file is empty.");
        }
        List<String> headers = raw.get(0).stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
        List<List<String>> rows = new ArrayList<>();
        for (List<String> line : raw.subList(1, raw.size())) {
            if (line.stream().allMatch(c -> c.isBlank())) {
                continue;
            }
            List<String> padded = new ArrayList<>(line);
            while (padded.size() < headers.size()) {
                padded.add("");
            }
            rows.add(padded);
        }
        return new Table(headers, rows);
    }
}
