package jp.usagi.railway.service;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Breaker;
import jp.usagi.railway.domain.Measurement;
import jp.usagi.railway.domain.Substation;
import jp.usagi.railway.repository.BreakerRepository;
import jp.usagi.railway.repository.MeasurementRepository;
import jp.usagi.railway.repository.SubstationRepository;

@Service
@Transactional(readOnly = true)
public class SubstationService {

    private final SubstationRepository substationRepository;
    private final BreakerRepository breakerRepository;
    private final MeasurementRepository measurementRepository;

    public SubstationService(SubstationRepository substationRepository, BreakerRepository breakerRepository,
                             MeasurementRepository measurementRepository) {
        this.substationRepository = substationRepository;
        this.breakerRepository = breakerRepository;
        this.measurementRepository = measurementRepository;
    }

    @Cacheable("substations")
    public List<Substation> list() {
        return substationRepository.findAllByOrderByCodeAsc();
    }

    public Substation get(String code) {
        Substation s = substationRepository.findOne(code);
        if (s == null) {
            throw new NotFoundException("変電所", code);
        }
        return s;
    }

    public List<Breaker> breakers() {
        return breakerRepository.findAllByOrderBySubstationCodeAscBreakerCodeAsc();
    }

    public Breaker breaker(Long id) {
        Breaker b = breakerRepository.findOne(id);
        if (b == null) {
            throw new NotFoundException("遮断器", String.valueOf(id));
        }
        return b;
    }

    public Measurement latest(String code) {
        return measurementRepository.findLatest(code);
    }

    /** 変電所コード → 最新計測値 (計測対象外は含まない) */
    public Map<String, Measurement> latestAll() {
        Map<String, Measurement> map = new LinkedHashMap<String, Measurement>();
        for (Substation s : list()) {
            if (s.isTelemetryEnabled()) {
                map.put(s.getCode(), measurementRepository.findLatest(s.getCode()));
            }
        }
        return map;
    }

    public List<Measurement> measurements(String code, LocalDate date) {
        Calendar from = Calendar.getInstance();
        from.setTime(date.toDate());
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.DATE, 1);
        return measurementRepository
                .findBySubstationCodeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtAsc(
                        code, from.getTime(), to.getTime());
    }
}
