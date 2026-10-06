package jp.usagi.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.RepairOrder;
import jp.usagi.railway.domain.RepairStatus;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    RepairOrder findByOrderNo(String orderNo);

    List<RepairOrder> findByFailureIdOrderByRequestedAtDesc(Long failureId);

    List<RepairOrder> findByStatusNotOrderByRequestedAtAsc(RepairStatus status);
}
