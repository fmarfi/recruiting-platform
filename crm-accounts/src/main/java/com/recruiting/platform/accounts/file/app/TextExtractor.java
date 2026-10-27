package com.recruiting.platform.accounts.file.app;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

/** Apache Tika text extraction (no OCR, no AI). Output is cut at 1,000,000 characters. */
@Component
public class TextExtractor {

    public static final int MAX_CHARS = 1_000_000;

    private final AutoDetectParser parser = new AutoDetectParser();

    /** @return the text, possibly empty; throws when the file cannot be parsed */
    public String extract(byte[] bytes) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(MAX_CHARS);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            parser.parse(in, handler, new Metadata(), new ParseContext());
        } catch (WriteLimitReachedException e) {
            // keep what we have
        }
        String text = handler.toString().replace((char) 0, ' ').strip();
        return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
    }
}
