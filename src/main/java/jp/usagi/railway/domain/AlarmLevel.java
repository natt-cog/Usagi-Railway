package jp.usagi.railway.domain;

/** 警報レベル */
public enum AlarmLevel {
    MAJOR("重故障"),
    MINOR("軽故障"),
    NOTICE("注意");

    private final String label;

    AlarmLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
