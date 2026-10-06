package jp.usagi.railway.service;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Equipment;
import jp.usagi.railway.domain.FailureRecord;
import jp.usagi.railway.domain.FailureStatus;
import jp.usagi.railway.domain.Formation;
import jp.usagi.railway.domain.FormationStatus;
import jp.usagi.railway.domain.RepairOrder;
import jp.usagi.railway.domain.RepairStatus;
import jp.usagi.railway.domain.Severity;
import jp.usagi.railway.repository.EquipmentRepository;
import jp.usagi.railway.repository.FailureRecordRepository;
import jp.usagi.railway.repository.RepairOrderRepository;

/**
 * 故障記録とメーカー修理依頼.
 *
 *   受付 / 調査中 --修理依頼--> 修理依頼中 --(メーカー返却)--> 修理完了 --完了--> 完了
 *   受付 / 調査中 --完了 (異常なし)--> 完了
 */
@Service
@Transactional(readOnly = true)
public class FailureService {

    private static final Logger log = LoggerFactory.getLogger(FailureService.class);

    private final FailureRecordRepository failureRepository;
    private final RepairOrderRepository repairRepository;
    private final EquipmentRepository equipmentRepository;
    private final NumberingService numbering;
    private final OperationDateService operationDate;

    public FailureService(FailureRecordRepository failureRepository, RepairOrderRepository repairRepository,
                          EquipmentRepository equipmentRepository, NumberingService numbering,
                          OperationDateService operationDate) {
        this.failureRepository = failureRepository;
        this.repairRepository = repairRepository;
        this.equipmentRepository = equipmentRepository;
        this.numbering = numbering;
        this.operationDate = operationDate;
    }

    public List<FailureRecord> search(String formationNo, String status, String severity) {
        return failureRepository.search(formationNo, status, severity);
    }

    public List<FailureRecord> forEquipment(String serialNo) {
        return failureRepository.findByEquipmentSerialNoOrderByOccurredAtDesc(serialNo);
    }

    public List<FailureRecord> forFormation(String formationNo) {
        return failureRepository.findByFormationNoOrderByOccurredAtDesc(formationNo);
    }

    public long countOpen() {
        return failureRepository.countByStatusNot(FailureStatus.CLOSED);
    }

    public FailureRecord get(String failureNo) {
        FailureRecord f = failureRepository.findByFailureNo(failureNo);
        if (f == null) {
            throw new NotFoundException("故障記録", failureNo);
        }
        return f;
    }

    public List<RepairOrder> repairsOf(FailureRecord f) {
        return repairRepository.findByFailureIdOrderByRequestedAtDesc(f.getId());
    }

    public List<RepairOrder> openRepairs() {
        return repairRepository.findByStatusNotOrderByRequestedAtAsc(RepairStatus.RETURNED);
    }

    public RepairOrder repair(String orderNo) {
        RepairOrder r = repairRepository.findByOrderNo(orderNo);
        if (r == null) {
            throw new NotFoundException("修理依頼", orderNo);
        }
        return r;
    }

    /** 故障登録. 重要度 A (運行支障) の場合は編成を休車にする. */
    @Transactional
    public FailureRecord register(String serialNo, Date occurredAt, String symptom, String failureCode,
                                  Severity severity, String user) {
        Equipment e = equipmentRepository.findOne(serialNo);
        if (e == null) {
            throw new NotFoundException("機器 (製造番号)", serialNo);
        }
        if (e.getCar() == null) {
            throw new BusinessRuleException("UR-3001", serialNo + " は予備品 (未搭載) のため故障登録できません");
        }
        Formation formation = e.getCar().getFormation();
        FailureRecord f = new FailureRecord();
        f.setFailureNo(numbering.next("F", "SEQ_FAILURE_NO"));
        f.setEquipment(e);
        f.setFormationNo(formation.getFormationNo());
        f.setOccurredAt(occurredAt != null ? occurredAt : operationDate.now());
        f.setSymptom(symptom);
        f.setFailureCode(failureCode == null || failureCode.trim().isEmpty() ? null : failureCode.trim());
        f.setSeverity(severity);
        f.setStatus(FailureStatus.OPEN);
        f.setReportedBy(user);
        if (severity == Severity.A && formation.getStatus() == FormationStatus.IN_SERVICE) {
            formation.setStatus(FormationStatus.OUT_OF_SERVICE);
            log.warn("重要度 A 故障のため {} を休車", formation.getFormationNo());
        }
        log.info("故障登録 {} {} {} by {}", f.getFailureNo(), serialNo, severity, user);
        return failureRepository.save(f);
    }

    /** メーカー修理依頼 */
    @Transactional
    public RepairOrder requestRepair(String failureNo, String user) {
        FailureRecord f = get(failureNo);
        if (f.getStatus() != FailureStatus.OPEN && f.getStatus() != FailureStatus.INVESTIGATING) {
            throw new BusinessRuleException("UR-3002",
                    failureNo + " は「" + f.getStatus().getLabel() + "」のため修理依頼できません");
        }
        RepairOrder r = new RepairOrder();
        r.setOrderNo(numbering.next("R", "SEQ_REPAIR_NO"));
        r.setFailure(f);
        r.setMaker(f.getEquipment().getMaker());
        r.setRequestedAt(operationDate.now());
        r.setRequestedBy(user);
        r.setStatus(RepairStatus.REQUESTED);
        f.setStatus(FailureStatus.REPAIR_REQUESTED);
        return repairRepository.save(r);
    }

    /** メーカーによる修理進捗の更新. 返却済で故障は修理完了になる. */
    @Transactional
    public RepairOrder updateProgress(String orderNo, RepairStatus status, String note, String user) {
        RepairOrder r = repair(orderNo);
        if (status.ordinal() < r.getStatus().ordinal()) {
            throw new BusinessRuleException("UR-3003", "修理進捗を「" + r.getStatus().getLabel() + "」から「"
                    + status.getLabel() + "」に戻すことはできません");
        }
        r.setStatus(status);
        r.setProgressNote(note);
        r.setUpdatedBy(user);
        r.setUpdatedAt(operationDate.now());
        if (status == RepairStatus.RETURNED) {
            r.getFailure().setStatus(FailureStatus.REPAIRED);
        }
        return r;
    }

    @Transactional
    public FailureRecord investigate(String failureNo) {
        FailureRecord f = get(failureNo);
        if (f.getStatus() == FailureStatus.OPEN) {
            f.setStatus(FailureStatus.INVESTIGATING);
        }
        return f;
    }

    @Transactional
    public FailureRecord close(String failureNo, String user) {
        FailureRecord f = get(failureNo);
        if (f.getStatus() == FailureStatus.REPAIR_REQUESTED) {
            throw new BusinessRuleException("UR-3004", failureNo + " はメーカー修理中のため完了にできません");
        }
        if (f.getStatus() == FailureStatus.CLOSED) {
            throw new BusinessRuleException("UR-3004", failureNo + " は既に完了しています");
        }
        f.setStatus(FailureStatus.CLOSED);
        log.info("故障完了 {} by {}", failureNo, user);
        return f;
    }
}
