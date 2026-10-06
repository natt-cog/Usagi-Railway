package jp.usagi.railway.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

/** 搭載機器. 製造番号をキーに製造から廃車までのライフサイクルを管理する */
@Entity
@Table(name = "EQUIPMENT")
public class Equipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SERIAL_NO", length = 12)
    private String serialNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "EQUIPMENT_TYPE", nullable = false)
    private EquipmentType equipmentType;

    @Column(name = "MODEL", nullable = false)
    private String model;

    @Column(name = "MAKER", nullable = false)
    private String maker;

    @Temporal(TemporalType.DATE)
    @Column(name = "MANUFACTURED_ON", nullable = false)
    private Date manufacturedOn;

    @ManyToOne
    @JoinColumn(name = "CAR_NO")
    private Car car;

    @Temporal(TemporalType.DATE)
    @Column(name = "INSTALLED_ON")
    private Date installedOn;

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(EquipmentType equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public Date getManufacturedOn() {
        return manufacturedOn;
    }

    public void setManufacturedOn(Date manufacturedOn) {
        this.manufacturedOn = manufacturedOn;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Date getInstalledOn() {
        return installedOn;
    }

    public void setInstalledOn(Date installedOn) {
        this.installedOn = installedOn;
    }

}
