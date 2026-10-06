package jp.usagi.railway.domain;

/** 停電作業 状態 */
public enum OutageStatus {
    REQUESTED("申請中"),
    APPROVED("承認済"),
    REJECTED("却下"),
    IN_PROGRESS("き電停止中"),
    COMPLETED("復電完了");

    private final String label;

    OutageStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
