package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/** 計測値 (1 時間値) */
@Entity
@Table(name = "MEASUREMENT")
public class Measurement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqMeasurement")
    @SequenceGenerator(name = "seqMeasurement", sequenceName = "SEQ_MEASUREMENT", allocationSize = 1)
    @Column(name = "MEASUREMENT_ID")
    private Long id;

    @Column(name = "SUBSTATION_CODE", nullable = false)
    private String substationCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "MEASURED_AT", nullable = false)
    private Date measuredAt;

    @Column(name = "VOLTAGE_V", nullable = false)
    private int voltageV;

    @Column(name = "CURRENT_A", nullable = false)
    private int currentA;

    @Column(name = "ENERGY_KWH", nullable = false)
    private int energyKwh;

    @Column(name = "QUALITY", nullable = false)
    private String quality;

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

    public Date getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(Date measuredAt) {
        this.measuredAt = measuredAt;
    }

    public int getVoltageV() {
        return voltageV;
    }

    public void setVoltageV(int voltageV) {
        this.voltageV = voltageV;
    }

    public int getCurrentA() {
        return currentA;
    }

    public void setCurrentA(int currentA) {
        this.currentA = currentA;
    }

    public int getEnergyKwh() {
        return energyKwh;
    }

    public void setEnergyKwh(int energyKwh) {
        this.energyKwh = energyKwh;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public boolean isMissing() {
        return "1".equals(quality);
    }
}
