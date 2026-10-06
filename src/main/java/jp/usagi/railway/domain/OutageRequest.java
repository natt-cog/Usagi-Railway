package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/** 停電作業申請 (き電停止申請) */
@Entity
@Table(name = "OUTAGE_REQUEST")
public class OutageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqOutage")
    @SequenceGenerator(name = "seqOutage", sequenceName = "SEQ_OUTAGE", allocationSize = 1)
    @Column(name = "OUTAGE_ID")
    private Long id;

    @Column(name = "REQUEST_NO", nullable = false)
    private String requestNo;

    @ManyToOne
    @JoinColumn(name = "BREAKER_ID", nullable = false)
    private Breaker breaker;

    @Temporal(TemporalType.DATE)
    @Column(name = "WORK_DATE", nullable = false)
    private Date workDate;

    @Column(name = "START_TIME", nullable = false)
    private String startTime;

    @Column(name = "END_TIME", nullable = false)
    private String endTime;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private OutageStatus status;

    @Column(name = "REQUESTED_BY", nullable = false)
    private String requestedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "REQUESTED_AT", nullable = false)
    private Date requestedAt;

    @Column(name = "APPROVED_BY")
    private String approvedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "APPROVED_AT")
    private Date approvedAt;

    @Column(name = "REMARKS")
    private String remarks;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public Breaker getBreaker() {
        return breaker;
    }

    public void setBreaker(Breaker breaker) {
        this.breaker = breaker;
    }

    public Date getWorkDate() {
        return workDate;
    }

    public void setWorkDate(Date workDate) {
        this.workDate = workDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OutageStatus getStatus() {
        return status;
    }

    public void setStatus(OutageStatus status) {
        this.status = status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
