package jp.usagi.railway.domain;

/** 検査種別 */
public enum InspectionKind {
    K("交番検査"),
    J("重要部検査"),
    Z("全般検査");

    private final String label;

    InspectionKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
