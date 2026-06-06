package com.justin.libradesk.infrastructure.export;

import com.justin.libradesk.domain.enumtype.PatronStatus;
import com.justin.libradesk.domain.enumtype.PatronType;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.domain.model.Patron;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes catalog/patron/loan data as CSV using Apache Commons CSV.
 *
 * <p>This class only maps between CSV and domain objects; persistence stays in
 * the services. Parsed objects are returned with sensible defaults (e.g. new
 * patrons ACTIVE) so the caller can hand them straight to the relevant service.
 * IO failures surface as {@link UncheckedIOException}.
 */
public class CsvService {

    // --- Books ---

    private static final String[] BOOK_HEADER = {"isbn", "title", "published_year"};

    public void writeBooks(File file, List<Book> books) {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format(BOOK_HEADER))) {
            for (Book book : books) {
                printer.printRecord(book.getIsbn(), book.getTitle(), book.getPublishedYear());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write books CSV", e);
        }
    }

    public List<Book> readBooks(File file) {
        List<Book> books = new ArrayList<>();
        try (CSVParser parser = parse(file)) {
            for (CSVRecord record : parser) {
                Book book = new Book();
                book.setIsbn(blankToNull(record.get("isbn")));
                book.setTitle(record.get("title"));
                book.setPublishedYear(parseIntOrNull(get(record, "published_year")));
                books.add(book);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read books CSV", e);
        }
        return books;
    }

    // --- Patrons ---

    private static final String[] PATRON_HEADER = {"membership_no", "full_name", "email", "phone", "patron_type"};

    public void writePatrons(File file, List<Patron> patrons) {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format(PATRON_HEADER))) {
            for (Patron patron : patrons) {
                printer.printRecord(patron.getMembershipNo(), patron.getFullName(),
                        patron.getEmail(), patron.getPhone(), patron.getPatronType());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write patrons CSV", e);
        }
    }

    public List<Patron> readPatrons(File file) {
        List<Patron> patrons = new ArrayList<>();
        try (CSVParser parser = parse(file)) {
            for (CSVRecord record : parser) {
                Patron patron = new Patron();
                patron.setMembershipNo(blankToNull(record.get("membership_no")));
                patron.setFullName(record.get("full_name"));
                patron.setEmail(blankToNull(get(record, "email")));
                patron.setPhone(blankToNull(get(record, "phone")));
                patron.setPatronType(PatronType.valueOf(record.get("patron_type").trim().toUpperCase()));
                patron.setStatus(PatronStatus.ACTIVE);
                patron.setCreatedAt(LocalDateTime.now());
                patrons.add(patron);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read patrons CSV", e);
        }
        return patrons;
    }

    // --- Loans (export only, e.g. an overdue report) ---

    private static final String[] LOAN_HEADER = {"loan_id", "copy_id", "patron_id", "loaned_at", "due_at", "status"};

    public void writeLoans(File file, List<Loan> loans) {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format(LOAN_HEADER))) {
            for (Loan loan : loans) {
                printer.printRecord(loan.getId(), loan.getCopyId(), loan.getPatronId(),
                        loan.getLoanedAt(), loan.getDueAt(), loan.getStatus());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write loans CSV", e);
        }
    }

    private static CSVFormat format(String[] header) {
        return CSVFormat.DEFAULT.builder().setHeader(header).build();
    }

    private static CSVParser parse(File file) throws IOException {
        return CSVParser.parse(file, StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build());
    }

    private static String get(CSVRecord record, String column) {
        return record.isMapped(column) ? record.get(column) : null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer parseIntOrNull(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
    }
}
