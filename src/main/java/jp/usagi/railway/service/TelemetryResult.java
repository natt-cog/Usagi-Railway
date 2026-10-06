package jp.usagi.railway.service;

public class TelemetryResult {

    private final String date;
    private final int accepted;
    private final int alarmsRaised;

    public TelemetryResult(String date, int accepted, int alarmsRaised) {
        this.date = date;
        this.accepted = accepted;
        this.alarmsRaised = alarmsRaised;
    }

    public String getDate() {
        return date;
    }

    public int getAccepted() {
        return accepted;
    }

    public int getAlarmsRaised() {
        return alarmsRaised;
    }
}
