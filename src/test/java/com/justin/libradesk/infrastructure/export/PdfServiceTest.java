package com.justin.libradesk.infrastructure.export;

import com.justin.libradesk.domain.enumtype.LoanStatus;
import com.justin.libradesk.domain.model.Loan;
import com.justin.libradesk.dto.LoanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    private boolean isPdf(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return bytes.length > 4
                && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    @Test
    void overdueReportProducesAPdfFile(@TempDir Path dir) throws Exception {
        File file = dir.resolve("overdue.pdf").toFile();
        Loan loan = new Loan(1L, 2L, 3L, LocalDateTime.now().minusDays(20),
                LocalDateTime.now().minusDays(6), null, LoanStatus.OVERDUE);

        pdfService.writeOverdueReport(file, List.of(loan));

        assertTrue(isPdf(file));
    }

    @Test
    void loanReceiptProducesAPdfFile(@TempDir Path dir) throws Exception {
        File file = dir.resolve("receipt.pdf").toFile();
        LoanResult result = new LoanResult(1L, 2L, 3L, LocalDateTime.now().plusDays(14));

        pdfService.writeLoanReceipt(file, result, "Alice", "BC-1");

        assertTrue(isPdf(file));
    }
}
