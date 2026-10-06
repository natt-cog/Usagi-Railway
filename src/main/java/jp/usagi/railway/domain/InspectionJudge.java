package jp.usagi.railway.domain;

/** 検査期限 判定 */
public enum InspectionJudge {
    N("正常"),
    W("注意"),
    X("超過");

    private final String label;

    InspectionJudge(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
