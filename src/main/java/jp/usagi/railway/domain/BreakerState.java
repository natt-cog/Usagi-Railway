package jp.usagi.railway.domain;

/** 遮断器状態 */
public enum BreakerState {
    CLOSED("入"),
    OPEN("切"),
    TRIPPED("トリップ");

    private final String label;

    BreakerState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
