package com.justin.libradesk.domain.service;

import com.justin.libradesk.domain.model.CalendarDay;
import com.justin.libradesk.repository.CalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private AuditLogService auditLogService;

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(calendarRepository, auditLogService);
    }

    @Test
    void nextOpenDayReturnsSameDayWhenOpen() {
        LocalDate day = LocalDate.of(2026, 1, 15);
        when(calendarRepository.isClosed(day)).thenReturn(false);

        assertEquals(day, calendarService.nextOpenDay(day));
    }

    @Test
    void nextOpenDayRollsForwardOverClosedDays() {
        when(calendarRepository.isClosed(LocalDate.of(2026, 1, 1))).thenReturn(true);
        when(calendarRepository.isClosed(LocalDate.of(2026, 1, 2))).thenReturn(true);
        when(calendarRepository.isClosed(LocalDate.of(2026, 1, 3))).thenReturn(false);

        assertEquals(LocalDate.of(2026, 1, 3), calendarService.nextOpenDay(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void openDaysBetweenCountsOnlyOpenDays() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 5);
        // Jan 2..Jan 5 is four candidate days; Jan 3 is closed, leaving three.
        when(calendarRepository.findRange(LocalDate.of(2026, 1, 2), to))
                .thenReturn(List.of(new CalendarDay(LocalDate.of(2026, 1, 3), "Holiday")));

        assertEquals(3, calendarService.openDaysBetween(from, to));
    }

    @Test
    void openDaysBetweenIsZeroWhenNotAfter() {
        LocalDate day = LocalDate.of(2026, 1, 5);
        assertEquals(0, calendarService.openDaysBetween(day, day));
    }
}
