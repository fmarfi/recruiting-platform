package com.recruiting.platform.accounts.importing.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class TableReaderTest {

    @Test
    void readsQuotedCsvWithBomAndBlankRows() {
        String csv = "﻿Name, Sector\n\"Öztürk, Holding\",\"He said \"\"hi\"\"\"\n\n,\nB,C\n";
        var table = TableReader.read("a.csv", csv.getBytes(StandardCharsets.UTF_8));
        assertThat(table.headers()).containsExactly("name", "sector");
        assertThat(table.rows()).hasSize(2);
        assertThat(table.rows().get(0)).containsExactly("Öztürk, Holding", "He said \"hi\"");
    }

    @Test
    void detectsSemicolonDelimiterAndPadsShortRows() {
        var table = TableReader.read("a.csv", "name;sector;city\nA;Energy\n".getBytes(StandardCharsets.UTF_8));
        assertThat(table.rows().get(0)).containsExactly("A", "Energy", "");
    }

    @Test
    void readsXlsxSharedAndInlineStringsWithGaps() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zip.write(("<sst><si><t>name</t></si><si><t>city</t></si><si><t>Acme</t></si></sst>")
                    .getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            zip.write(("<worksheet><sheetData>"
                    + "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"C1\" t=\"s\"><v>1</v></c></row>"
                    + "<row r=\"2\"><c r=\"A2\" t=\"s\"><v>2</v></c><c r=\"C2\" t=\"inlineStr\"><is><t>Izmir</t></is></c></row>"
                    + "</sheetData></worksheet>").getBytes(StandardCharsets.UTF_8));
        }
        var table = TableReader.read("a.xlsx", out.toByteArray());
        assertThat(table.headers()).containsExactly("name", "", "city");
        assertThat(table.rows().get(0)).containsExactly("Acme", "", "Izmir");
    }

    @Test
    void rejectsEmptyAndBrokenFiles() {
        assertThatThrownBy(() -> TableReader.read("a.csv", new byte[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TableReader.read("a.xlsx", new byte[] {1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
