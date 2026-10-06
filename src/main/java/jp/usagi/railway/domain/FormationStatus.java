package jp.usagi.railway.domain;

/** 編成 運用状態 */
public enum FormationStatus {
    IN_SERVICE("運用中"),
    IN_DEPOT("入場中"),
    OUT_OF_SERVICE("休車");

    private final String label;

    FormationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
