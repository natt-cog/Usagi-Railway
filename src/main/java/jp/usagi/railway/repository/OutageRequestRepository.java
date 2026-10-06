package jp.usagi.railway.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.OutageRequest;
import jp.usagi.railway.domain.OutageStatus;

public interface OutageRequestRepository extends JpaRepository<OutageRequest, Long> {

    OutageRequest findByRequestNo(String requestNo);

    List<OutageRequest> findAllByOrderByWorkDateDescRequestNoDesc();

    List<OutageRequest> findByBreakerIdAndWorkDateAndStatusIn(Long breakerId, Date workDate,
                                                              Collection<OutageStatus> statuses);

    long countByStatus(OutageStatus status);
}
