package jp.usagi.railway.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.FailureRecord;
import jp.usagi.railway.domain.FailureStatus;
import jp.usagi.railway.domain.FormationStatus;
import jp.usagi.railway.domain.RepairOrder;
import jp.usagi.railway.domain.RepairStatus;
import jp.usagi.railway.domain.Severity;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FailureServiceIT {

    @Autowired
    private FailureService failureService;

    @Autowired
    private FormationService formationService;

    @Test
    public void severityAFailureTakesFormationOutOfService() {
        assertEquals(FormationStatus.IN_SERVICE, formationService.get("U5101").getStatus());
        FailureRecord f = failureService.register("UV08-0001", null, "主電動機 異音", "E-MM1", Severity.A, "kenshu");
        assertTrue(f.getFailureNo().matches("F-26-\\d{4}"));
        assertEquals("U5101", f.getFormationNo());
        assertEquals(FormationStatus.OUT_OF_SERVICE, formationService.get("U5101").getStatus());
    }

    @Test
    public void spareEquipmentCannotBeRegistered() {
        OutageServiceIT.assertRule("UR-3001", new Runnable() {
            public void run() { failureService.register("UV25-0901", null, "x", null, Severity.C, "kenshu"); }
        });
    }

    @Test
    public void repairFlowFromRequestToReturn() {
        RepairOrder r = failureService.requestRepair("F-26-0004", "kenshu");
        assertEquals(FailureStatus.REPAIR_REQUESTED, failureService.get("F-26-0004").getStatus());
        failureService.updateProgress(r.getOrderNo(), RepairStatus.REPAIRING, "受領", "maker");
        failureService.updateProgress(r.getOrderNo(), RepairStatus.RETURNED, "返却", "maker");
        assertEquals(FailureStatus.REPAIRED, failureService.get("F-26-0004").getStatus());
        failureService.close("F-26-0004", "kenshu");
        assertEquals(FailureStatus.CLOSED, failureService.get("F-26-0004").getStatus());
    }

    @Test
    public void repairProgressCannotGoBackwards() {
        OutageServiceIT.assertRule("UR-3003", new Runnable() {
            public void run() { failureService.updateProgress("R-26-0001", RepairStatus.REQUESTED, null, "maker"); }
        });
    }

    @Test
    public void failureUnderRepairCannotBeClosed() {
        OutageServiceIT.assertRule("UR-3004", new Runnable() {
            public void run() { failureService.close("F-26-0001", "kenshu"); }
        });
    }
}
