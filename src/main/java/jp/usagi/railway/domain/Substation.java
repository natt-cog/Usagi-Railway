package jp.usagi.railway.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import javax.persistence.*;

/** 変電所・き電区分所 */
@Entity
@Table(name = "SUBSTATION")
public class Substation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SUBSTATION_CODE", length = 4)
    private String code;

    @Column(name = "SUBSTATION_NAME", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "KIND", nullable = false)
    private SubstationKind kind;

    @Column(name = "LINE_NAME", nullable = false)
    private String lineName;

    @Column(name = "KM_POST", nullable = false)
    private BigDecimal kmPost;

    @Column(name = "FEED_VOLTAGE_V", nullable = false)
    private Integer feedVoltageV;

    @Column(name = "TELEMETRY_FLG", nullable = false)
    private String telemetryFlg;

    @OneToMany(mappedBy = "substation", fetch = FetchType.EAGER)
    @OrderBy("breakerCode")
    private List<Breaker> breakers;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SubstationKind getKind() {
        return kind;
    }

    public void setKind(SubstationKind kind) {
        this.kind = kind;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public BigDecimal getKmPost() {
        return kmPost;
    }

    public void setKmPost(BigDecimal kmPost) {
        this.kmPost = kmPost;
    }

    public Integer getFeedVoltageV() {
        return feedVoltageV;
    }

    public void setFeedVoltageV(Integer feedVoltageV) {
        this.feedVoltageV = feedVoltageV;
    }

    public String getTelemetryFlg() {
        return telemetryFlg;
    }

    public void setTelemetryFlg(String telemetryFlg) {
        this.telemetryFlg = telemetryFlg;
    }

    public List<Breaker> getBreakers() {
        return breakers;
    }

    public void setBreakers(List<Breaker> breakers) {
        this.breakers = breakers;
    }

    public boolean isTelemetryEnabled() {
        return "Y".equals(telemetryFlg);
    }
}
