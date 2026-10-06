package jp.usagi.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.FailureRecord;
import jp.usagi.railway.domain.FailureStatus;

public interface FailureRecordRepository extends JpaRepository<FailureRecord, Long>, FailureRecordRepositoryCustom {

    FailureRecord findByFailureNo(String failureNo);

    List<FailureRecord> findByEquipmentSerialNoOrderByOccurredAtDesc(String serialNo);

    List<FailureRecord> findByFormationNoOrderByOccurredAtDesc(String formationNo);

    long countByStatusNot(FailureStatus status);
}
