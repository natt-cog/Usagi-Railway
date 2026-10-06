package jp.usagi.railway.domain;

/** 故障 重要度 */
public enum Severity {
    A("A: 運行支障"),
    B("B: 軽微"),
    C("C: 経過観察");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
