package jp.usagi.railway.api.dto;

import java.util.Date;

import jp.usagi.railway.domain.Equipment;

public class EquipmentDto {

    public String serialNo;
    public String type;
    public String typeLabel;
    public String model;
    public String maker;
    public Date manufacturedOn;
    public String carNo;
    public String formationNo;
    public Date installedOn;

    public static EquipmentDto of(Equipment e) {
        EquipmentDto d = new EquipmentDto();
        d.serialNo = e.getSerialNo();
        d.type = e.getEquipmentType().name();
        d.typeLabel = e.getEquipmentType().getLabel();
        d.model = e.getModel();
        d.maker = e.getMaker();
        d.manufacturedOn = e.getManufacturedOn();
        d.installedOn = e.getInstalledOn();
        if (e.getCar() != null) {
            d.carNo = e.getCar().getCarNo();
            d.formationNo = e.getCar().getFormation().getFormationNo();
        }
        return d;
    }
}
