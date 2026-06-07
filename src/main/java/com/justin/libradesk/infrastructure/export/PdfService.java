package com.justin.libradesk.infrastructure.export;

import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.dto.LoanResult;
import com.justin.libradesk.dto.SpineLabel;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Produces PDF documents (overdue report, loan receipt) with OpenPDF. Mirrors
 * {@link CsvService}: it only renders domain data to a file. Failures surface as
 * {@link UncheckedIOException}.
 */
public class PdfService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);

    public void writeOverdueReport(File file, List<Loan> loans) {
        render(file, document -> {
            document.add(new Paragraph("Overdue Loans Report", TITLE_FONT));
            document.add(new Paragraph("Generated " + LocalDateTime.now().format(STAMP)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{1, 1, 1, 2, 1});
            table.setWidthPercentage(100);
            header(table, "Loan", "Copy", "Patron", "Due", "Status");
            for (Loan loan : loans) {
                table.addCell(String.valueOf(loan.getId()));
                table.addCell(String.valueOf(loan.getCopyId()));
                table.addCell(String.valueOf(loan.getPatronId()));
                table.addCell(loan.getDueAt().format(STAMP));
                table.addCell(loan.getStatus().name());
            }
            document.add(table);
        });
    }

    public void writeLoanReceipt(File file, LoanResult loan, String patronName, String barcode) {
        render(file, document -> {
            document.add(new Paragraph("Loan Receipt", TITLE_FONT));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Issued: " + LocalDateTime.now().format(STAMP)));
            document.add(new Paragraph("Loan ID: " + loan.loanId()));
            document.add(new Paragraph("Patron: " + patronName));
            document.add(new Paragraph("Copy barcode: " + barcode));
            document.add(new Paragraph("Due date: " + loan.dueAt().format(STAMP)));
        });
    }

    /** Writes a grid of spine/pocket labels (3 per row): call number stacked, then barcode. */
    public void writeSpineLabels(File file, List<SpineLabel> labels) {
        Font callFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font barcodeFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        render(file, document -> {
            document.add(new Paragraph("Spine Labels", TITLE_FONT));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            for (SpineLabel label : labels) {
                PdfPCell cell = new PdfPCell();
                cell.setPadding(8);
                cell.setFixedHeight(72);
                String callNumber = label.callNumber() == null ? "" : label.callNumber();
                for (String part : callNumber.split("\\s+")) {
                    cell.addElement(new Paragraph(part, callFont));
                }
                cell.addElement(new Paragraph(label.barcode() == null ? "" : label.barcode(), barcodeFont));
                table.addCell(cell);
            }
            for (int i = labels.size() % 3; i != 0 && i < 3; i++) {
                table.addCell(new PdfPCell());
            }
            if (!labels.isEmpty()) {
                document.add(table);
            }
        });
    }

    private void header(PdfPTable table, String... titles) {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        for (String title : titles) {
            PdfPCell cell = new PdfPCell(new Paragraph(title, bold));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void render(File file, DocumentBody body) {
        Document document = new Document();
        try (OutputStream out = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, out);
            document.open();
            try {
                body.write(document);
            } finally {
                document.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write PDF: " + file, e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to build PDF: " + file, e);
        }
    }

    @FunctionalInterface
    private interface DocumentBody {
        void write(Document document) throws DocumentException;
    }
}
