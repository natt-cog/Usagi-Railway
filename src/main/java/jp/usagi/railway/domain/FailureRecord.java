package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/** 故障記録 */
@Entity
@Table(name = "FAILURE_RECORD")
public class FailureRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqFailure")
    @SequenceGenerator(name = "seqFailure", sequenceName = "SEQ_FAILURE", allocationSize = 1)
    @Column(name = "FAILURE_ID")
    private Long id;

    @Column(name = "FAILURE_NO", nullable = false)
    private String failureNo;

    @ManyToOne
    @JoinColumn(name = "SERIAL_NO", nullable = false)
    private Equipment equipment;

    @Column(name = "FORMATION_NO", nullable = false)
    private String formationNo;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OCCURRED_AT", nullable = false)
    private Date occurredAt;

    @Column(name = "SYMPTOM", nullable = false)
    private String symptom;

    @Column(name = "FAILURE_CODE")
    private String failureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private FailureStatus status;

    @Column(name = "REPORTED_BY", nullable = false)
    private String reportedBy;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFailureNo() {
        return failureNo;
    }

    public void setFailureNo(String failureNo) {
        this.failureNo = failureNo;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public String getFormationNo() {
        return formationNo;
    }

    public void setFormationNo(String formationNo) {
        this.formationNo = formationNo;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Date occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getSymptom() {
        return symptom;
    }

    public void setSymptom(String symptom) {
        this.symptom = symptom;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public FailureStatus getStatus() {
        return status;
    }

    public void setStatus(FailureStatus status) {
        this.status = status;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
