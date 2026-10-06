package jp.usagi.railway.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Measurement;
import jp.usagi.railway.domain.Substation;
import jp.usagi.railway.repository.MeasurementRepository;
import jp.usagi.railway.repository.SubstationRepository;

/**
 * 変電所別 電力日報. 画面表示と帳票ファイル (DAILY_YYYYMMDD.DAT) を作成する.
 * 帳票ファイルは C バッチ URPWD01 (batch/c/urpwd01.c) の出力とバイト単位で一致すること.
 */
@Service
@Transactional(readOnly = true)
public class DailyReportService {

    private final MeasurementRepository measurementRepository;
    private final SubstationRepository substationRepository;
    private final AlarmService alarmService;

    public DailyReportService(MeasurementRepository measurementRepository,
                              SubstationRepository substationRepository, AlarmService alarmService) {
        this.measurementRepository = measurementRepository;
        this.substationRepository = substationRepository;
        this.alarmService = alarmService;
    }

    public List<DailyReportRow> build(LocalDate date) {
        Calendar from = Calendar.getInstance();
        from.setTime(date.toDate());
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.DATE, 1);
        List<Measurement> ms = measurementRepository
                .findByMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderBySubstationCodeAscMeasuredAtAsc(
                        from.getTime(), to.getTime());
        List<TelemetryRecord> records = new ArrayList<TelemetryRecord>();
        for (Measurement m : ms) {
            Calendar c = Calendar.getInstance();
            c.setTime(m.getMeasuredAt());
            TelemetryRecord r = new TelemetryRecord();
            r.setSubstationCode(m.getSubstationCode());
            r.setHour(c.get(Calendar.HOUR_OF_DAY));
            r.setMinute(c.get(Calendar.MINUTE));
            r.setVoltageV(m.getVoltageV());
            r.setCurrentA(m.getCurrentA());
            r.setEnergyKwh(m.getEnergyKwh());
            r.setMissing(m.isMissing());
            records.add(r);
        }
        List<DailyReportRow> rows = summarize(records, alarmService.getUndervoltageV(), alarmService.getOvercurrentA());
        for (DailyReportRow row : rows) {
            Substation s = substationRepository.findOne(row.getSubstationCode());
            if (s != null) {
                row.setSubstationName(s.getName());
            }
        }
        return rows;
    }

    public List<String> buildFile(LocalDate date) {
        return format(date, build(date));
    }

    /** 変電所コード順に集計する. */
    public static List<DailyReportRow> summarize(List<TelemetryRecord> records, int uvThreshold, int ocThreshold) {
        Map<String, DailyReportRow> map = new TreeMap<String, DailyReportRow>();
        for (TelemetryRecord r : records) {
            DailyReportRow row = map.get(r.getSubstationCode());
            if (row == null) {
                row = new DailyReportRow(r.getSubstationCode());
                map.put(r.getSubstationCode(), row);
            }
            row.add(r, uvThreshold, ocThreshold);
        }
        return new ArrayList<DailyReportRow>(map.values());
    }

    public static List<String> format(LocalDate date, List<DailyReportRow> rows) {
        List<String> out = new ArrayList<String>();
        out.add("H" + date.toString("yyyyMMdd"));
        long grand = 0;
        for (DailyReportRow r : rows) {
            out.add(String.format("D%-4.4s%02d%05d%05d%05d%09d%02d%02d%02d", r.getSubstationCode(),
                    r.getValidCount(), r.getMinVoltageV(), r.getAvgVoltageV(), r.getMaxCurrentA(),
                    r.getEnergyKwh(), r.getUndervoltageCount(), r.getOvercurrentCount(), r.getMissingCount()));
            grand += r.getEnergyKwh();
        }
        out.add(String.format("T%04d%011d", rows.size(), grand));
        return out;
    }
}
