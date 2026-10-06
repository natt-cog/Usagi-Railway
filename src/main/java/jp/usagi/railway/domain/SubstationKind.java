package jp.usagi.railway.domain;

/** 設備種別 */
public enum SubstationKind {
    SS("変電所"),
    SP("き電区分所"),
    SSP("補助き電区分所");

    private final String label;

    SubstationKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
