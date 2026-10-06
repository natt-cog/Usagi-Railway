package jp.usagi.railway.api.dto;

import java.util.Date;

import jp.usagi.railway.domain.FailureRecord;

public class FailureDto {

    public String failureNo;
    public String serialNo;
    public String formationNo;
    public Date occurredAt;
    public String symptom;
    public String failureCode;
    public String severity;
    public String status;
    public String statusLabel;
    public String reportedBy;

    public static FailureDto of(FailureRecord f) {
        FailureDto d = new FailureDto();
        d.failureNo = f.getFailureNo();
        d.serialNo = f.getEquipment().getSerialNo();
        d.formationNo = f.getFormationNo();
        d.occurredAt = f.getOccurredAt();
        d.symptom = f.getSymptom();
        d.failureCode = f.getFailureCode();
        d.severity = f.getSeverity().name();
        d.status = f.getStatus().name();
        d.statusLabel = f.getStatus().getLabel();
        d.reportedBy = f.getReportedBy();
        return d;
    }
}
