package jp.usagi.railway.domain;

import java.io.Serializable;

import javax.persistence.*;

/** 遮断器 */
@Entity
@Table(name = "BREAKER")
public class Breaker implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "BREAKER_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SUBSTATION_CODE", nullable = false)
    private Substation substation;

    @Column(name = "BREAKER_CODE", nullable = false)
    private String breakerCode;

    @Column(name = "BREAKER_NAME", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE", nullable = false)
    private BreakerState state;

    @Column(name = "RATED_CURRENT_A", nullable = false)
    private Integer ratedCurrentA;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Substation getSubstation() {
        return substation;
    }

    public void setSubstation(Substation substation) {
        this.substation = substation;
    }

    public String getBreakerCode() {
        return breakerCode;
    }

    public void setBreakerCode(String breakerCode) {
        this.breakerCode = breakerCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BreakerState getState() {
        return state;
    }

    public void setState(BreakerState state) {
        this.state = state;
    }

    public Integer getRatedCurrentA() {
        return ratedCurrentA;
    }

    public void setRatedCurrentA(Integer ratedCurrentA) {
        this.ratedCurrentA = ratedCurrentA;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
