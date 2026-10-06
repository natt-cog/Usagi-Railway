package jp.usagi.railway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.LocalDate;
import org.junit.Test;

import jp.usagi.railway.domain.InspectionJudge;
import jp.usagi.railway.domain.InspectionKind;

public class InspectionServiceTest {

    private static final LocalDate BASE = new LocalDate(2026, 10, 5);

    @Test
    public void kobanIsNinetyDaysAfterPrevious() {
        InspectionDue d = InspectionService.calculate("U9001", new LocalDate(2026, 9, 1),
                new LocalDate(2025, 1, 10), new LocalDate(2021, 1, 10), 100000, BASE, 14);
        assertEquals("2026/11/30", d.getKobanDueText());
        assertEquals(InspectionKind.K, d.getNextKind());
        assertEquals(56, d.getDaysRemaining());
        assertEquals(InspectionJudge.N, d.getJudge());
    }

    @Test
    public void overdueIsJudgedX() {
        InspectionDue d = InspectionService.calculate("U9002", new LocalDate(2026, 6, 1),
                new LocalDate(2025, 1, 10), new LocalDate(2021, 1, 10), 100000, BASE, 14);
        assertEquals(InspectionJudge.X, d.getJudge());
        assertTrue(d.getDaysRemaining() < 0);
    }

    @Test
    public void withinWarnDaysIsJudgedW() {
        InspectionDue d = InspectionService.calculate("U9003", new LocalDate(2026, 7, 10),
                new LocalDate(2025, 1, 10), new LocalDate(2021, 1, 10), 100000, BASE, 14);
        assertEquals("2026/10/08", d.getKobanDueText());
        assertEquals(InspectionJudge.W, d.getJudge());
    }

    @Test
    public void juyobuKmNearLimitIsWarned() {
        InspectionDue d = InspectionService.calculate("U9004", new LocalDate(2026, 9, 1),
                new LocalDate(2025, 1, 10), new LocalDate(2021, 1, 10), 585400, BASE, 14);
        assertFalse(d.isKmExceeded());
        assertEquals(InspectionJudge.W, d.getJudge());
    }

    @Test
    public void juyobuKmOverLimitIsJudgedX() {
        InspectionDue d = InspectionService.calculate("U9005", new LocalDate(2026, 9, 1),
                new LocalDate(2025, 1, 10), new LocalDate(2021, 1, 10), 600000, BASE, 14);
        assertTrue(d.isKmExceeded());
        assertEquals(InspectionJudge.X, d.getJudge());
    }

    @Test
    public void leapDayStaysLeapDayAfterFourAndEightYears() {
        InspectionDue d = InspectionService.calculate("U9006", new LocalDate(2026, 9, 1),
                new LocalDate(2024, 2, 29), new LocalDate(2020, 2, 29), 100000, BASE, 14);
        assertEquals("2028/02/29", d.getJuyobuDueText());
        assertEquals("2028/02/29", d.getZenpanDueText());
    }

    @Test
    public void leapDayRollsToFebruary28InCenturyYear() {
        InspectionDue d = InspectionService.calculate("U9007", new LocalDate(2026, 9, 1),
                new LocalDate(2096, 2, 29), new LocalDate(2092, 2, 29), 100000, BASE, 14);
        assertEquals("2100/02/28", d.getJuyobuDueText());
        assertEquals("2100/02/28", d.getZenpanDueText());
    }
}
