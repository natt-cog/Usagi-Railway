package jp.usagi.railway.domain;

/** メーカー修理 進捗 */
public enum RepairStatus {
    REQUESTED("依頼中"),
    RECEIVED("受領"),
    REPAIRING("修理中"),
    RETURNED("返却済");

    private final String label;

    RepairStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
