package jp.usagi.railway.service;

/** 電力日報 1 行 (変電所別). */
public class DailyReportRow {

    private String substationCode;
    private String substationName;
    private int validCount;
    private int minVoltageV;
    private long sumVoltageV;
    private int maxCurrentA;
    private long energyKwh;
    private int undervoltageCount;
    private int overcurrentCount;
    private int missingCount;

    public DailyReportRow(String substationCode) {
        this.substationCode = substationCode;
        this.minVoltageV = Integer.MAX_VALUE;
    }

    void add(TelemetryRecord r, int uvThreshold, int ocThreshold) {
        if (r.isMissing()) {
            missingCount++;
            return;
        }
        validCount++;
        sumVoltageV += r.getVoltageV();
        minVoltageV = Math.min(minVoltageV, r.getVoltageV());
        maxCurrentA = Math.max(maxCurrentA, r.getCurrentA());
        energyKwh += r.getEnergyKwh();
        if (r.getVoltageV() < uvThreshold) {
            undervoltageCount++;
        }
        if (r.getCurrentA() > ocThreshold) {
            overcurrentCount++;
        }
    }

    public String getSubstationCode() { return substationCode; }
    public String getSubstationName() { return substationName; }
    public void setSubstationName(String substationName) { this.substationName = substationName; }
    public int getValidCount() { return validCount; }
    public int getMinVoltageV() { return validCount > 0 ? minVoltageV : 0; }
    /** 平均電圧 (小数点以下切捨て) */
    public int getAvgVoltageV() { return validCount > 0 ? (int) (sumVoltageV / validCount) : 0; }
    public int getMaxCurrentA() { return maxCurrentA; }
    public long getEnergyKwh() { return energyKwh; }
    public int getUndervoltageCount() { return undervoltageCount; }
    public int getOvercurrentCount() { return overcurrentCount; }
    public int getMissingCount() { return missingCount; }
}
