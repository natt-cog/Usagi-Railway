package jp.usagi.railway.service;

import org.joda.time.LocalDate;

import jp.usagi.railway.domain.InspectionJudge;
import jp.usagi.railway.domain.InspectionKind;

/** 編成別 検査期限 (URINS01 出力 D レコード相当). */
public class InspectionDue {

    private String formationNo;
    private LocalDate kobanDue;
    private LocalDate juyobuDue;
    private LocalDate zenpanDue;
    private InspectionKind nextKind;
    private LocalDate nextDue;
    private int daysRemaining;
    private int kmSinceJuyobu;
    private boolean kmExceeded;
    private InspectionJudge judge;

    public String getFormationNo() { return formationNo; }
    public void setFormationNo(String formationNo) { this.formationNo = formationNo; }
    public LocalDate getKobanDue() { return kobanDue; }
    public void setKobanDue(LocalDate kobanDue) { this.kobanDue = kobanDue; }
    public LocalDate getJuyobuDue() { return juyobuDue; }
    public void setJuyobuDue(LocalDate juyobuDue) { this.juyobuDue = juyobuDue; }
    public LocalDate getZenpanDue() { return zenpanDue; }
    public void setZenpanDue(LocalDate zenpanDue) { this.zenpanDue = zenpanDue; }
    public InspectionKind getNextKind() { return nextKind; }
    public void setNextKind(InspectionKind nextKind) { this.nextKind = nextKind; }
    public LocalDate getNextDue() { return nextDue; }
    public void setNextDue(LocalDate nextDue) { this.nextDue = nextDue; }
    public int getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }
    public int getKmSinceJuyobu() { return kmSinceJuyobu; }
    public void setKmSinceJuyobu(int kmSinceJuyobu) { this.kmSinceJuyobu = kmSinceJuyobu; }
    public boolean isKmExceeded() { return kmExceeded; }
    public void setKmExceeded(boolean kmExceeded) { this.kmExceeded = kmExceeded; }
    public InspectionJudge getJudge() { return judge; }
    public void setJudge(InspectionJudge judge) { this.judge = judge; }

    /** JSP 表示用 */
    public String getNextDueText() { return nextDue.toString("yyyy/MM/dd"); }
    public String getKobanDueText() { return kobanDue.toString("yyyy/MM/dd"); }
    public String getJuyobuDueText() { return juyobuDue.toString("yyyy/MM/dd"); }
    public String getZenpanDueText() { return zenpanDue.toString("yyyy/MM/dd"); }
}
