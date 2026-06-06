package com.justin.libradesk.controller;

import com.justin.libradesk.config.AppContext;
import com.justin.libradesk.domain.model.Book;
import com.justin.libradesk.domain.model.Patron;
import com.justin.libradesk.domain.model.Reservation;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reservation management: place a hold for a patron on a book, view the active
 * queue, and cancel a selected reservation. Delegates to {@code ReservationService}.
 */
public class ReservationsController {

    @FXML
    private TextField membershipField;
    @FXML
    private ComboBox<Book> bookCombo;
    @FXML
    private TableView<Reservation> reservationTable;
    @FXML
    private TableColumn<Reservation, String> bookColumn;
    @FXML
    private TableColumn<Reservation, String> patronColumn;
    @FXML
    private TableColumn<Reservation, String> positionColumn;
    @FXML
    private TableColumn<Reservation, String> statusColumn;

    /** Cache of book id -> title so the table can show titles for reservations. */
    private final Map<Long, String> bookTitles = new HashMap<>();

    @FXML
    private void initialize() {
        bookCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Book book) {
                return book == null ? "" : book.getTitle();
            }

            @Override
            public Book fromString(String string) {
                return null;
            }
        });
        bookColumn.setCellValueFactory(c -> new SimpleStringProperty(
                bookTitles.getOrDefault(c.getValue().getBookId(), "#" + c.getValue().getBookId())));
        patronColumn.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getPatronId())));
        positionColumn.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getQueuePosition())));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        onReloadBooks();
        refresh();
    }

    @FXML
    private void onReloadBooks() {
        var books = AppContext.get().catalogService().listBooks();
        bookCombo.setItems(FXCollections.observableArrayList(books));
        bookTitles.clear();
        books.forEach(book -> bookTitles.put(book.getId(), book.getTitle()));
    }

    @FXML
    private void onReserve() {
        String membership = text(membershipField);
        Book book = bookCombo.getValue();
        if (membership == null || book == null) {
            Dialogs.error("Enter a membership number and choose a book.");
            return;
        }
        Optional<Patron> patron = AppContext.get().patronService().findByMembershipNo(membership);
        if (patron.isEmpty()) {
            Dialogs.error("No patron with membership number: " + membership);
            return;
        }
        try {
            AppContext.get().reservationService().reserve(book.getId(), patron.get().getId(), actor());
            membershipField.clear();
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.error("Select a reservation to cancel.");
            return;
        }
        try {
            AppContext.get().reservationService().cancel(selected.getId(), actor());
            refresh();
        } catch (RuntimeException e) {
            Dialogs.error(e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        reservationTable.setItems(FXCollections.observableArrayList(
                AppContext.get().reservationService().listActive()));
    }

    private static String text(TextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actor() {
        return AppContext.get().getCurrentUser().getUsername();
    }
}
