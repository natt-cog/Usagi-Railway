package jp.usagi.railway.api.dto;

import java.util.ArrayList;
import java.util.List;

import jp.usagi.railway.domain.Equipment;
import jp.usagi.railway.domain.Formation;
import jp.usagi.railway.service.InspectionDue;

public class FormationDto {

    public String formationNo;
    public String series;
    public Integer carCount;
    public String depot;
    public String status;
    public String statusLabel;
    public Long totalKm;
    public Integer kmSinceJuyobu;
    public Inspection inspection;
    public List<EquipmentDto> equipment = new ArrayList<EquipmentDto>();

    public static class Inspection {
        public String kobanDue;
        public String juyobuDue;
        public String zenpanDue;
        public String nextKind;
        public String nextKindLabel;
        public String nextDue;
        public int daysRemaining;
        public boolean kmExceeded;
        public String judge;
        public String judgeLabel;
    }

    public static FormationDto of(Formation f, InspectionDue due, List<Equipment> equipment) {
        FormationDto d = new FormationDto();
        d.formationNo = f.getFormationNo();
        d.series = f.getSeries();
        d.carCount = f.getCarCount();
        d.depot = f.getDepot();
        d.status = f.getStatus().name();
        d.statusLabel = f.getStatus().getLabel();
        d.totalKm = f.getTotalKm();
        d.kmSinceJuyobu = f.getKmSinceJuyobu();
        if (due != null) {
            Inspection i = new Inspection();
            i.kobanDue = due.getKobanDueText();
            i.juyobuDue = due.getJuyobuDueText();
            i.zenpanDue = due.getZenpanDueText();
            i.nextKind = due.getNextKind().name();
            i.nextKindLabel = due.getNextKind().getLabel();
            i.nextDue = due.getNextDueText();
            i.daysRemaining = due.getDaysRemaining();
            i.kmExceeded = due.isKmExceeded();
            i.judge = due.getJudge().name();
            i.judgeLabel = due.getJudge().getLabel();
            d.inspection = i;
        }
        if (equipment != null) {
            for (Equipment e : equipment) {
                d.equipment.add(EquipmentDto.of(e));
            }
        }
        return d;
    }
}
