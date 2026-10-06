package jp.usagi.railway.api.dto;

import java.util.Date;

import jp.usagi.railway.domain.Alarm;

public class AlarmDto {

    public Long id;
    public String substationCode;
    public String breakerCode;
    public Date occurredAt;
    public String level;
    public String levelLabel;
    public String alarmCode;
    public String message;
    public boolean acknowledged;
    public String ackBy;

    public static AlarmDto of(Alarm a) {
        AlarmDto d = new AlarmDto();
        d.id = a.getId();
        d.substationCode = a.getSubstationCode();
        d.breakerCode = a.getBreakerCode();
        d.occurredAt = a.getOccurredAt();
        d.level = a.getLevel().name();
        d.levelLabel = a.getLevel().getLabel();
        d.alarmCode = a.getAlarmCode();
        d.message = a.getMessage();
        d.acknowledged = a.isAcknowledged();
        d.ackBy = a.getAckBy();
        return d;
    }
}
