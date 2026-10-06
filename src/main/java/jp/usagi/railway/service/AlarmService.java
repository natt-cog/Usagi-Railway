package jp.usagi.railway.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Alarm;
import jp.usagi.railway.domain.AlarmLevel;
import jp.usagi.railway.repository.AlarmRepository;

/**
 * 警報管理.
 *
 *   UV01 電圧低下 : 電圧 &lt; urms.alarm.undervoltage-v
 *   OC01 過電流   : 電流 &gt; urms.alarm.overcurrent-a
 *   CM01 伝送異常 : 欠測
 * 閾値は日報バッチ (batch/c/urpwd01.c) の UV_THRESHOLD / OC_THRESHOLD と同一であること.
 */
@Service
@Transactional(readOnly = true)
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final OperationDateService operationDate;

    @Value("${urms.alarm.undervoltage-v:1350}")
    private int undervoltageV;

    @Value("${urms.alarm.overcurrent-a:4000}")
    private int overcurrentA;

    public AlarmService(AlarmRepository alarmRepository, OperationDateService operationDate) {
        this.alarmRepository = alarmRepository;
        this.operationDate = operationDate;
    }

    public Page<Alarm> list(int page) {
        return alarmRepository.findAllByOrderByOccurredAtDescIdDesc(new PageRequest(page, 50));
    }

    public List<Alarm> unacknowledged() {
        return alarmRepository.findByAckFlgOrderByOccurredAtDescIdDesc("N");
    }

    public List<Alarm> forSubstation(String code) {
        return alarmRepository.findBySubstationCodeOrderByOccurredAtDescIdDesc(code);
    }

    public long countUnacknowledged() {
        return alarmRepository.countByAckFlg("N");
    }

    public long countUnacknowledgedMajor() {
        return alarmRepository.countByAckFlgAndLevel("N", AlarmLevel.MAJOR);
    }

    @Transactional
    public Alarm acknowledge(Long id, String user) {
        Alarm a = alarmRepository.findOne(id);
        if (a == null) {
            throw new NotFoundException("警報", String.valueOf(id));
        }
        if (a.isAcknowledged()) {
            throw new BusinessRuleException("UR-2101", "警報は既に確認済みです (" + a.getAckBy() + ")");
        }
        a.setAckFlg("Y");
        a.setAckBy(user);
        a.setAckAt(operationDate.now());
        return alarmRepository.save(a);
    }

    /** 計測値から警報を判定する (保存はしない). */
    public List<Alarm> evaluate(TelemetryRecord r, Date measuredAt) {
        return evaluate(r, measuredAt, undervoltageV, overcurrentA);
    }

    public static List<Alarm> evaluate(TelemetryRecord r, Date measuredAt, int uvThreshold, int ocThreshold) {
        List<Alarm> alarms = new ArrayList<Alarm>();
        if (r.isMissing()) {
            alarms.add(newAlarm(r.getSubstationCode(), measuredAt, AlarmLevel.NOTICE, "CM01", "伝送異常 (欠測)"));
            return alarms;
        }
        if (r.getVoltageV() < uvThreshold) {
            alarms.add(newAlarm(r.getSubstationCode(), measuredAt, AlarmLevel.MINOR, "UV01",
                    "電圧低下 " + r.getVoltageV() + "V (閾値 " + uvThreshold + "V)"));
        }
        if (r.getCurrentA() > ocThreshold) {
            alarms.add(newAlarm(r.getSubstationCode(), measuredAt, AlarmLevel.MINOR, "OC01",
                    "過電流 " + r.getCurrentA() + "A (閾値 " + ocThreshold + "A)"));
        }
        return alarms;
    }

    private static Alarm newAlarm(String ss, Date at, AlarmLevel level, String code, String message) {
        Alarm a = new Alarm();
        a.setSubstationCode(ss);
        a.setOccurredAt(at);
        a.setLevel(level);
        a.setAlarmCode(code);
        a.setMessage(message);
        a.setAckFlg("N");
        return a;
    }

    @Transactional
    public List<Alarm> saveAll(List<Alarm> alarms) {
        return alarmRepository.save(alarms);
    }

    public int getUndervoltageV() {
        return undervoltageV;
    }

    public int getOvercurrentA() {
        return overcurrentA;
    }
}
