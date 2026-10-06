package jp.usagi.railway.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jp.usagi.railway.domain.Measurement;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    Measurement findBySubstationCodeAndMeasuredAt(String substationCode, Date measuredAt);

    List<Measurement> findBySubstationCodeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtAsc(
            String substationCode, Date from, Date to);

    List<Measurement> findByMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderBySubstationCodeAscMeasuredAtAsc(
            Date from, Date to);

    /** 最新の正常計測値 (Oracle ROWNUM 使用) */
    @Query(value = "SELECT * FROM (SELECT * FROM MEASUREMENT WHERE SUBSTATION_CODE = ?1 AND QUALITY = '0' "
            + "ORDER BY MEASURED_AT DESC) WHERE ROWNUM <= 1", nativeQuery = true)
    Measurement findLatest(String substationCode);
}
