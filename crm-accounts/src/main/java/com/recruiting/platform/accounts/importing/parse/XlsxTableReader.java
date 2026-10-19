package com.recruiting.platform.accounts.importing.parse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Minimal XLSX reader on the JDK only (no Apache POI): the first worksheet's cell texts, with shared and inline
 * strings resolved. Formatting, formulas and dates are not interpreted; a date cell arrives as its stored number.
 */
final class XlsxTableReader {

    private XlsxTableReader() {
    }

    static List<List<String>> read(byte[] content) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            List<String> shared = new ArrayList<>();
            byte[] sheet = null;
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("xl/sharedStrings.xml")) {
                    shared = sharedStrings(zip.readAllBytes());
                } else if (sheet == null && name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                    sheet = zip.readAllBytes();
                }
            }
            if (sheet == null) {
                throw new IllegalArgumentException("The workbook has no worksheet.");
            }
            return sheetRows(sheet, shared);
        } catch (IOException | XMLStreamException e) {
            throw new IllegalArgumentException("The file is not a readable XLSX workbook.", e);
        }
    }

    private static XMLStreamReader open(byte[] xml) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        InputStream in = new ByteArrayInputStream(xml);
        return f.createXMLStreamReader(in);
    }

    private static List<String> sharedStrings(byte[] xml) throws XMLStreamException {
        List<String> out = new ArrayList<>();
        XMLStreamReader r = open(xml);
        StringBuilder current = null;
        boolean inText = false;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                if (r.getLocalName().equals("si")) {
                    current = new StringBuilder();
                } else if (r.getLocalName().equals("t")) {
                    inText = true;
                }
            } else if (ev == XMLStreamConstants.CHARACTERS && inText && current != null) {
                current.append(r.getText());
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                if (r.getLocalName().equals("t")) {
                    inText = false;
                } else if (r.getLocalName().equals("si") && current != null) {
                    out.add(current.toString());
                    current = null;
                }
            }
        }
        return out;
    }

    private static List<List<String>> sheetRows(byte[] xml, List<String> shared) throws XMLStreamException {
        List<List<String>> rows = new ArrayList<>();
        XMLStreamReader r = open(xml);
        List<String> row = null;
        String type = null;
        int column = 0;
        StringBuilder value = null;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "row" -> row = new ArrayList<>();
                    case "c" -> {
                        type = r.getAttributeValue(null, "t");
                        column = columnIndex(r.getAttributeValue(null, "r"));
                        value = new StringBuilder();
                    }
                    default -> {
                        // v, t and is only matter through their characters
                    }
                }
            } else if (ev == XMLStreamConstants.CHARACTERS && value != null) {
                value.append(r.getText());
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                if (r.getLocalName().equals("c") && row != null && value != null) {
                    while (row.size() < column) {
                        row.add("");
                    }
                    row.add(resolve(type, value.toString(), shared));
                    value = null;
                } else if (r.getLocalName().equals("row") && row != null) {
                    rows.add(row);
                    row = null;
                }
            }
        }
        return rows;
    }

    private static String resolve(String type, String raw, List<String> shared) {
        if ("s".equals(type)) {
            int i = Integer.parseInt(raw.trim());
            return i < shared.size() ? shared.get(i) : "";
        }
        return raw;
    }

    /** "C7" to 2; a missing reference counts as column 0. */
    private static int columnIndex(String ref) {
        if (ref == null) {
            return 0;
        }
        int index = 0;
        for (char ch : ref.toCharArray()) {
            if (ch < 'A' || ch > 'Z') {
                break;
            }
            index = index * 26 + (ch - 'A' + 1);
        }
        return Math.max(index - 1, 0);
    }
}
