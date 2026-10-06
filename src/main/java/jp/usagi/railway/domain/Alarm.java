package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/** 警報 */
@Entity
@Table(name = "ALARM")
public class Alarm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqAlarm")
    @SequenceGenerator(name = "seqAlarm", sequenceName = "SEQ_ALARM", allocationSize = 1)
    @Column(name = "ALARM_ID")
    private Long id;

    @Column(name = "SUBSTATION_CODE", nullable = false)
    private String substationCode;

    @Column(name = "BREAKER_CODE")
    private String breakerCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OCCURRED_AT", nullable = false)
    private Date occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ALARM_LEVEL", nullable = false)
    private AlarmLevel level;

    @Column(name = "ALARM_CODE", nullable = false)
    private String alarmCode;

    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @Column(name = "ACK_FLG", nullable = false)
    private String ackFlg;

    @Column(name = "ACK_BY")
    private String ackBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ACK_AT")
    private Date ackAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubstationCode() {
        return substationCode;
    }

    public void setSubstationCode(String substationCode) {
        this.substationCode = substationCode;
    }

    public String getBreakerCode() {
        return breakerCode;
    }

    public void setBreakerCode(String breakerCode) {
        this.breakerCode = breakerCode;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Date occurredAt) {
        this.occurredAt = occurredAt;
    }

    public AlarmLevel getLevel() {
        return level;
    }

    public void setLevel(AlarmLevel level) {
        this.level = level;
    }

    public String getAlarmCode() {
        return alarmCode;
    }

    public void setAlarmCode(String alarmCode) {
        this.alarmCode = alarmCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAckFlg() {
        return ackFlg;
    }

    public void setAckFlg(String ackFlg) {
        this.ackFlg = ackFlg;
    }

    public String getAckBy() {
        return ackBy;
    }

    public void setAckBy(String ackBy) {
        this.ackBy = ackBy;
    }

    public Date getAckAt() {
        return ackAt;
    }

    public void setAckAt(Date ackAt) {
        this.ackAt = ackAt;
    }

    public boolean isAcknowledged() {
        return "Y".equals(ackFlg);
    }
}
