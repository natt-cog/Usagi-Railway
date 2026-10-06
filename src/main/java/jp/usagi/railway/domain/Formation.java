package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.*;

/** 編成 */
@Entity
@Table(name = "FORMATION")
public class Formation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "FORMATION_NO", length = 6)
    private String formationNo;

    @Column(name = "SERIES", nullable = false)
    private String series;

    @Column(name = "CAR_COUNT", nullable = false)
    private Integer carCount;

    @Column(name = "DEPOT", nullable = false)
    private String depot;

    @Temporal(TemporalType.DATE)
    @Column(name = "IN_SERVICE_ON", nullable = false)
    private Date inServiceOn;

    @Column(name = "TOTAL_KM", nullable = false)
    private Long totalKm;

    @Temporal(TemporalType.DATE)
    @Column(name = "LAST_KOBAN_ON", nullable = false)
    private Date lastKobanOn;

    @Temporal(TemporalType.DATE)
    @Column(name = "LAST_JUYOBU_ON", nullable = false)
    private Date lastJuyobuOn;

    @Temporal(TemporalType.DATE)
    @Column(name = "LAST_ZENPAN_ON", nullable = false)
    private Date lastZenpanOn;

    @Column(name = "KM_SINCE_JUYOBU", nullable = false)
    private Integer kmSinceJuyobu;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private FormationStatus status;

    @OneToMany(mappedBy = "formation")
    @OrderBy("position")
    private List<Car> cars;

    public String getFormationNo() {
        return formationNo;
    }

    public void setFormationNo(String formationNo) {
        this.formationNo = formationNo;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public Integer getCarCount() {
        return carCount;
    }

    public void setCarCount(Integer carCount) {
        this.carCount = carCount;
    }

    public String getDepot() {
        return depot;
    }

    public void setDepot(String depot) {
        this.depot = depot;
    }

    public Date getInServiceOn() {
        return inServiceOn;
    }

    public void setInServiceOn(Date inServiceOn) {
        this.inServiceOn = inServiceOn;
    }

    public Long getTotalKm() {
        return totalKm;
    }

    public void setTotalKm(Long totalKm) {
        this.totalKm = totalKm;
    }

    public Date getLastKobanOn() {
        return lastKobanOn;
    }

    public void setLastKobanOn(Date lastKobanOn) {
        this.lastKobanOn = lastKobanOn;
    }

    public Date getLastJuyobuOn() {
        return lastJuyobuOn;
    }

    public void setLastJuyobuOn(Date lastJuyobuOn) {
        this.lastJuyobuOn = lastJuyobuOn;
    }

    public Date getLastZenpanOn() {
        return lastZenpanOn;
    }

    public void setLastZenpanOn(Date lastZenpanOn) {
        this.lastZenpanOn = lastZenpanOn;
    }

    public Integer getKmSinceJuyobu() {
        return kmSinceJuyobu;
    }

    public void setKmSinceJuyobu(Integer kmSinceJuyobu) {
        this.kmSinceJuyobu = kmSinceJuyobu;
    }

    public FormationStatus getStatus() {
        return status;
    }

    public void setStatus(FormationStatus status) {
        this.status = status;
    }

    public List<Car> getCars() {
        return cars;
    }

    public void setCars(List<Car> cars) {
        this.cars = cars;
    }

}
