package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.List;

import javax.persistence.*;

/** 車両 */
@Entity
@Table(name = "CAR")
public class Car implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "CAR_NO", length = 6)
    private String carNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FORMATION_NO", nullable = false)
    private Formation formation;

    @Column(name = "POSITION", nullable = false)
    private Integer position;

    @Column(name = "CAR_TYPE", nullable = false)
    private String carType;

    @OneToMany(mappedBy = "car")
    @OrderBy("equipmentType")
    private List<Equipment> equipment;

    public String getCarNo() {
        return carNo;
    }

    public void setCarNo(String carNo) {
        this.carNo = carNo;
    }

    public Formation getFormation() {
        return formation;
    }

    public void setFormation(Formation formation) {
        this.formation = formation;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }

    public List<Equipment> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<Equipment> equipment) {
        this.equipment = equipment;
    }

}
