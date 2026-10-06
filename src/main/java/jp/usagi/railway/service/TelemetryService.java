package jp.usagi.railway.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Alarm;
import jp.usagi.railway.domain.Measurement;
import jp.usagi.railway.repository.MeasurementRepository;
import jp.usagi.railway.repository.SubstationRepository;

/**
 * RTU 計測伝文の受信・登録. 同一変電所・同一時刻の計測値は上書きする (再送対応).
 */
@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final MeasurementRepository measurementRepository;
    private final SubstationRepository substationRepository;
    private final AlarmService alarmService;

    public TelemetryService(MeasurementRepository measurementRepository, SubstationRepository substationRepository,
                            AlarmService alarmService) {
        this.measurementRepository = measurementRepository;
        this.substationRepository = substationRepository;
        this.alarmService = alarmService;
    }

    @Transactional
    public TelemetryResult ingest(String body) {
        TelemetryFile file = TelemetryFile.parse(Arrays.asList(body.split("\r?\n")));
        List<Alarm> raised = new ArrayList<Alarm>();
        for (TelemetryRecord r : file.getRecords()) {
            if (!substationRepository.exists(r.getSubstationCode())) {
                throw new NotFoundException("変電所", r.getSubstationCode());
            }
            Date at = file.getDate().toLocalDateTime(new org.joda.time.LocalTime(r.getHour(), r.getMinute()))
                    .toDate();
            Measurement m = measurementRepository.findBySubstationCodeAndMeasuredAt(r.getSubstationCode(), at);
            if (m == null) {
                m = new Measurement();
                m.setSubstationCode(r.getSubstationCode());
                m.setMeasuredAt(at);
            }
            m.setVoltageV(r.getVoltageV());
            m.setCurrentA(r.getCurrentA());
            m.setEnergyKwh(r.getEnergyKwh());
            m.setQuality(r.isMissing() ? "1" : "0");
            measurementRepository.save(m);
            raised.addAll(alarmService.evaluate(r, at));
        }
        alarmService.saveAll(raised);
        log.info("計測伝文 受信 計測日={} 件数={} 警報={}", file.getDate(), file.getRecords().size(), raised.size());
        return new TelemetryResult(file.getDate().toString("yyyyMMdd"), file.getRecords().size(), raised.size());
    }
}
