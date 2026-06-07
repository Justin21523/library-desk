package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.CalendarDay;
import com.justin.libradesk.repository.CalendarRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The library calendar: which days the library is closed. Used to roll due dates
 * off closed days and to count only open days when accruing overdue fines.
 */
public class CalendarService {

    private static final int MAX_ROLL_DAYS = 366;

    private final CalendarRepository calendarRepository;
    private final AuditLogService auditLogService;

    public CalendarService(CalendarRepository calendarRepository, AuditLogService auditLogService) {
        this.calendarRepository = calendarRepository;
        this.auditLogService = auditLogService;
    }

    public boolean isClosed(LocalDate date) {
        return calendarRepository.isClosed(date);
    }

    /** @return {@code date} itself if open, else the next open day after it. */
    public LocalDate nextOpenDay(LocalDate date) {
        LocalDate d = date;
        for (int i = 0; i < MAX_ROLL_DAYS && calendarRepository.isClosed(d); i++) {
            d = d.plusDays(1);
        }
        return d;
    }

    /**
     * Counts open (non-closed) days strictly after {@code from} up to and
     * including {@code to}. Used to charge overdue fines only for days the
     * library was actually open.
     *
     * @return the number of chargeable open days, never negative
     */
    public long openDaysBetween(LocalDate from, LocalDate to) {
        if (!to.isAfter(from)) {
            return 0;
        }
        Set<LocalDate> closed = calendarRepository.findRange(from.plusDays(1), to).stream()
                .map(CalendarDay::date)
                .collect(Collectors.toSet());
        long count = 0;
        for (LocalDate d = from.plusDays(1); !d.isAfter(to); d = d.plusDays(1)) {
            if (!closed.contains(d)) {
                count++;
            }
        }
        return count;
    }

    public List<CalendarDay> list() {
        return calendarRepository.findAll();
    }

    public void add(LocalDate date, String note, String actor) {
        calendarRepository.add(new CalendarDay(date, note));
        auditLogService.record(actor, "CALENDAR_DAY_ADDED", "CalendarDay", null, date.toString());
    }

    public void remove(LocalDate date, String actor) {
        calendarRepository.remove(date);
        auditLogService.record(actor, "CALENDAR_DAY_REMOVED", "CalendarDay", null, date.toString());
    }
}
