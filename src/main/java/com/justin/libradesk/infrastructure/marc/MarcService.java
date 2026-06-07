package com.justin.libradesk.infrastructure.marc;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes MARC records (ISO 2709 {@code .mrc} and MARCXML) via marc4j,
 * delegating field mapping to {@link MarcMapper}. Like {@code CsvService}/{@code
 * PdfService} it only does file I/O; the catalog service resolves names to ids.
 */
public class MarcService {

    private final MarcMapper mapper = new MarcMapper();

    /** Reads records from a {@code .xml} (MARCXML) or {@code .mrc} (ISO 2709) file. */
    public List<MarcData> read(File file) {
        boolean xml = file.getName().toLowerCase().endsWith(".xml");
        try (InputStream in = new FileInputStream(file)) {
            MarcReader reader = xml ? new MarcXmlReader(in) : new MarcStreamReader(in, "UTF-8");
            List<MarcData> records = new ArrayList<>();
            while (reader.hasNext()) {
                Record record = reader.next();
                MarcData data = mapper.toMarcData(record);
                data.book().setMarcXml(recordToXml(record));
                records.add(data);
            }
            return records;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read MARC file: " + file, e);
        }
    }

    public void writeMarcXml(File file, List<MarcData> records) {
        try (OutputStream out = new FileOutputStream(file)) {
            MarcXmlWriter writer = new MarcXmlWriter(out, true);
            for (MarcData data : records) {
                writer.write(mapper.toRecord(data));
            }
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write MARCXML file: " + file, e);
        }
    }

    public void writeMrc(File file, List<MarcData> records) {
        try (OutputStream out = new FileOutputStream(file)) {
            MarcStreamWriter writer = new MarcStreamWriter(out, "UTF-8");
            for (MarcData data : records) {
                writer.write(mapper.toRecord(data));
            }
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write MARC file: " + file, e);
        }
    }

    /** Renders a record as a MARCXML string (for the "View MARC" preview). */
    public String toXmlString(MarcData data) {
        return recordToXml(mapper.toRecord(data));
    }

    /** Renders an arbitrary marc4j record as MARCXML (used by the full MARC editor). */
    public String toXml(Record record) {
        return recordToXml(record);
    }

    /** Parses a single MARCXML string into a full marc4j record (preserving every field). */
    public Record parseXml(String xml) {
        MarcReader reader = new MarcXmlReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        if (!reader.hasNext()) {
            throw new IllegalArgumentException("No MARC record found in XML");
        }
        return reader.next();
    }

    /** Parses a single MARCXML string back into {@link MarcData} (used by SRU import in Phase 11). */
    public MarcData fromXmlString(String xml) {
        MarcReader reader = new MarcXmlReader(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        if (!reader.hasNext()) {
            throw new IllegalArgumentException("No MARC record found in XML");
        }
        Record record = reader.next();
        MarcData data = mapper.toMarcData(record);
        data.book().setMarcXml(recordToXml(record));
        return data;
    }

    private String recordToXml(Record record) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MarcXmlWriter writer = new MarcXmlWriter(out, true);
        writer.write(record);
        writer.close();
        return out.toString(StandardCharsets.UTF_8);
    }
}
