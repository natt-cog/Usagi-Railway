package jp.usagi.railway.service;

/**
 * 計測伝文 D レコード (27 桁).
 *
 * <pre>
 *  桁   1     : レコード区分 'D'
 *  桁   2- 5  : 変電所コード
 *  桁   6- 9  : 計測時刻 HHMM
 *  桁  10-14  : 電圧 [V]
 *  桁  15-19  : 電流 [A]
 *  桁  20-26  : 電力量 [kWh]
 *  桁  27     : 品質 '0'=正常 '1'=欠測
 * </pre>
 */
public class TelemetryRecord {

    private String substationCode;
    private int hour;
    private int minute;
    private int voltageV;
    private int currentA;
    private int energyKwh;
    private boolean missing;

    public static TelemetryRecord parse(String rec) {
        if (rec.length() < 27 || rec.charAt(0) != 'D') {
            throw new BusinessRuleException("UR-2201", "計測伝文の形式が不正です: [" + rec + "]");
        }
        try {
            TelemetryRecord r = new TelemetryRecord();
            r.substationCode = rec.substring(1, 5);
            r.hour = Integer.parseInt(rec.substring(5, 7));
            r.minute = Integer.parseInt(rec.substring(7, 9));
            r.voltageV = Integer.parseInt(rec.substring(9, 14));
            r.currentA = Integer.parseInt(rec.substring(14, 19));
            r.energyKwh = Integer.parseInt(rec.substring(19, 26));
            r.missing = rec.charAt(26) == '1';
            return r;
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("UR-2201", "計測伝文の形式が不正です: [" + rec + "]");
        }
    }

    public String format() {
        return String.format("D%-4s%02d%02d%05d%05d%07d%s", substationCode, hour, minute, voltageV, currentA,
                energyKwh, missing ? "1" : "0");
    }

    public String getSubstationCode() { return substationCode; }
    public void setSubstationCode(String substationCode) { this.substationCode = substationCode; }
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
    public int getVoltageV() { return voltageV; }
    public void setVoltageV(int voltageV) { this.voltageV = voltageV; }
    public int getCurrentA() { return currentA; }
    public void setCurrentA(int currentA) { this.currentA = currentA; }
    public int getEnergyKwh() { return energyKwh; }
    public void setEnergyKwh(int energyKwh) { this.energyKwh = energyKwh; }
    public boolean isMissing() { return missing; }
    public void setMissing(boolean missing) { this.missing = missing; }
}
