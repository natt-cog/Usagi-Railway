package jp.usagi.railway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.BreakerState;
import jp.usagi.railway.domain.OutageRequest;
import jp.usagi.railway.domain.OutageStatus;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OutageServiceIT {

    @Autowired
    private OutageService outageService;

    @Test
    public void approveStartCompleteOpensAndClosesBreaker() {
        outageService.approve("P-26-0041", "shirei");
        OutageRequest o = outageService.start("P-26-0041", "shirei");
        assertEquals(OutageStatus.IN_PROGRESS, o.getStatus());
        assertEquals(BreakerState.OPEN, o.getBreaker().getState());

        o = outageService.complete("P-26-0041", "shirei");
        assertEquals(OutageStatus.COMPLETED, o.getStatus());
        assertEquals(BreakerState.CLOSED, o.getBreaker().getState());
    }

    @Test
    public void cannotStartBeforeApproval() {
        assertRule("UR-2002", new Runnable() {
            public void run() { outageService.start("P-26-0041", "shirei"); }
        });
    }

    @Test
    public void duplicateRequestOnSameBreakerAndDayIsRejected() {
        assertRule("UR-2001", new Runnable() {
            public void run() { outageService.create(1L, date(2026, 10, 7), "02:00", "03:00", "重複", "kenshu"); }
        });
    }

    @Test
    public void trippedBreakerCannotBeOpenedByOutageFlow() {
        final OutageRequest o = outageService.create(13L, date(2026, 10, 8), "01:00", "03:00", "52F2 点検", "kenshu");
        assertEquals(OutageStatus.REQUESTED, o.getStatus());
        outageService.approve(o.getRequestNo(), "shirei");
        assertRule("UR-2003", new Runnable() {
            public void run() { outageService.start(o.getRequestNo(), "shirei"); }
        });
    }

    @Test
    public void invalidTimeRangeIsRejected() {
        assertRule("UR-2004", new Runnable() {
            public void run() { outageService.create(2L, date(2026, 10, 9), "04:00", "01:00", "逆転", "kenshu"); }
        });
    }

    private static Date date(int y, int m, int d) {
        return new LocalDate(y, m, d).toDate();
    }

    static void assertRule(String code, Runnable r) {
        try {
            r.run();
            fail("expected " + code);
        } catch (BusinessRuleException e) {
            assertEquals(code, e.getErrorCode());
        }
    }
}
