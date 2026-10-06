package jp.usagi.railway.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.Alarm;
import jp.usagi.railway.domain.AlarmLevel;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    Page<Alarm> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);

    List<Alarm> findByAckFlgOrderByOccurredAtDescIdDesc(String ackFlg);

    List<Alarm> findBySubstationCodeOrderByOccurredAtDescIdDesc(String substationCode);

    long countByAckFlg(String ackFlg);

    long countByAckFlgAndLevel(String ackFlg, AlarmLevel level);
}
