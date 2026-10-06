package jp.usagi.railway.domain;

/** 故障 処置状況 */
public enum FailureStatus {
    OPEN("受付"),
    INVESTIGATING("調査中"),
    REPAIR_REQUESTED("修理依頼中"),
    REPAIRED("修理完了"),
    CLOSED("完了");

    private final String label;

    FailureStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
