package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.BookCopy;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.dto.LoanResult;
import com.justin.libradesk.dto.ReturnResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Optional;

/**
 * Circulation desk: check a copy out to a patron (by membership number and
 * barcode) and check copies back in (by barcode). Resolves the human-friendly
 * identifiers to ids, then delegates to {@code CirculationService}.
 */
public class CirculationController {

    @FXML
    private TextField checkoutMembershipField;
    @FXML
    private TextField checkoutBarcodeField;
    @FXML
    private TextField returnBarcodeField;
    @FXML
    private Label resultLabel;

    @FXML
    private void onCheckout() {
        String membership = text(checkoutMembershipField);
        String barcode = text(checkoutBarcodeField);
        if (membership == null || barcode == null) {
            Dialogs.error("Enter both a membership number and a copy barcode.");
            return;
        }

        Optional<Patron> patron = AppContext.get().patronService().findByMembershipNo(membership);
        if (patron.isEmpty()) {
            Dialogs.error("No patron with membership number: " + membership);
            return;
        }
        Optional<BookCopy> copy = AppContext.get().catalogService().findCopyByBarcode(barcode);
        if (copy.isEmpty()) {
            Dialogs.error("No copy with barcode: " + barcode);
            return;
        }

        try {
            LoanResult result = AppContext.get().circulationService()
                    .checkout(patron.get().getId(), copy.get().getId(), actor());
            resultLabel.setText("Checked out copy " + barcode + " to " + patron.get().getFullName()
                    + ". Due " + result.dueAt().toLocalDate() + ".");
            checkoutMembershipField.clear();
            checkoutBarcodeField.clear();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onReturn() {
        String barcode = text(returnBarcodeField);
        if (barcode == null) {
            Dialogs.error("Enter a copy barcode to return.");
            return;
        }
        Optional<BookCopy> copy = AppContext.get().catalogService().findCopyByBarcode(barcode);
        if (copy.isEmpty()) {
            Dialogs.error("No copy with barcode: " + barcode);
            return;
        }

        try {
            ReturnResult result = AppContext.get().circulationService()
                    .returnByCopy(copy.get().getId(), actor());
            StringBuilder message = new StringBuilder("Returned copy " + barcode + ".");
            if (result.wasOverdue()) {
                message.append(" It was overdue.");
            }
            if (result.heldForReservation()) {
                message.append(" Held for the next reservation.");
            }
            resultLabel.setText(message.toString());
            returnBarcodeField.clear();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
