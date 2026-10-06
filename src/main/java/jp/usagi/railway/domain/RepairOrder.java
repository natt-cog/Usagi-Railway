package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/** メーカー修理依頼 */
@Entity
@Table(name = "REPAIR_ORDER")
public class RepairOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqRepair")
    @SequenceGenerator(name = "seqRepair", sequenceName = "SEQ_REPAIR", allocationSize = 1)
    @Column(name = "REPAIR_ID")
    private Long id;

    @Column(name = "ORDER_NO", nullable = false)
    private String orderNo;

    @ManyToOne
    @JoinColumn(name = "FAILURE_ID", nullable = false)
    private FailureRecord failure;

    @Column(name = "MAKER", nullable = false)
    private String maker;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "REQUESTED_AT", nullable = false)
    private Date requestedAt;

    @Column(name = "REQUESTED_BY", nullable = false)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private RepairStatus status;

    @Column(name = "PROGRESS_NOTE")
    private String progressNote;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATED_AT")
    private Date updatedAt;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public FailureRecord getFailure() {
        return failure;
    }

    public void setFailure(FailureRecord failure) {
        this.failure = failure;
    }

    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public String getProgressNote() {
        return progressNote;
    }

    public void setProgressNote(String progressNote) {
        this.progressNote = progressNote;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
