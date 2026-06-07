package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.domain.model.CalendarDay;
import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.CalendarRepository;
import com.justin.libradesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JdbcCalendarRepository implements CalendarRepository {

    private final DatabaseManager db;

    public JdbcCalendarRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public boolean isClosed(LocalDate date) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM calendar_days WHERE closed_date = ?")) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check calendar for " + date, e);
        }
    }

    @Override
    public List<CalendarDay> findRange(LocalDate from, LocalDate to) {
        List<CalendarDay> days = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM calendar_days WHERE closed_date BETWEEN ? AND ? ORDER BY closed_date")) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    days.add(mapRow(rs));
                }
            }
            return days;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list calendar range", e);
        }
    }

    @Override
    public List<CalendarDay> findAll() {
        List<CalendarDay> days = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM calendar_days ORDER BY closed_date");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                days.add(mapRow(rs));
            }
            return days;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list calendar days", e);
        }
    }

    @Override
    public void add(CalendarDay day) {
        String sql = """
                INSERT INTO calendar_days (closed_date, note) VALUES (?, ?)
                ON CONFLICT (closed_date) DO UPDATE SET note = EXCLUDED.note
                """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(day.date()));
            ps.setString(2, day.note());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add calendar day " + day.date(), e);
        }
    }

    @Override
    public void remove(LocalDate date) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM calendar_days WHERE closed_date = ?")) {
            ps.setDate(1, Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to remove calendar day " + date, e);
        }
    }

    private CalendarDay mapRow(ResultSet rs) throws SQLException {
        return new CalendarDay(rs.getDate("closed_date").toLocalDate(), rs.getString("note"));
    }
}
