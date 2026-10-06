package jp.usagi.railway.domain;

/** 搭載機器 種別 */
public enum EquipmentType {
    VVVF("VVVFインバータ"),
    SIV("補助電源装置 (SIV)"),
    BCU("ブレーキ制御装置");

    private final String label;

    EquipmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
