package jp.usagi.railway.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Breaker;
import jp.usagi.railway.domain.BreakerState;
import jp.usagi.railway.domain.OutageRequest;
import jp.usagi.railway.domain.OutageStatus;
import jp.usagi.railway.repository.BreakerRepository;
import jp.usagi.railway.repository.OutageRequestRepository;

/**
 * 停電作業 (き電停止) 管理.
 *
 *   申請中 --承認--> 承認済 --き電停止--> き電停止中 --復電--> 復電完了
 *          --却下--> 却下
 */
@Service
@Transactional(readOnly = true)
public class OutageService {

    private static final Logger log = LoggerFactory.getLogger(OutageService.class);

    private static final List<OutageStatus> ACTIVE =
            Arrays.asList(OutageStatus.REQUESTED, OutageStatus.APPROVED, OutageStatus.IN_PROGRESS);

    private final OutageRequestRepository outageRepository;
    private final BreakerRepository breakerRepository;
    private final NumberingService numbering;
    private final OperationDateService operationDate;

    public OutageService(OutageRequestRepository outageRepository, BreakerRepository breakerRepository,
                         NumberingService numbering, OperationDateService operationDate) {
        this.outageRepository = outageRepository;
        this.breakerRepository = breakerRepository;
        this.numbering = numbering;
        this.operationDate = operationDate;
    }

    public List<OutageRequest> list() {
        return outageRepository.findAllByOrderByWorkDateDescRequestNoDesc();
    }

    public long countPending() {
        return outageRepository.countByStatus(OutageStatus.REQUESTED);
    }

    public OutageRequest get(String requestNo) {
        OutageRequest o = outageRepository.findByRequestNo(requestNo);
        if (o == null) {
            throw new NotFoundException("停電作業申請", requestNo);
        }
        return o;
    }

    @Transactional
    public OutageRequest create(Long breakerId, Date workDate, String startTime, String endTime,
                                String description, String user) {
        Breaker b = breakerRepository.findOne(breakerId);
        if (b == null) {
            throw new NotFoundException("遮断器", String.valueOf(breakerId));
        }
        if (workDate == null || !isTime(startTime) || !isTime(endTime) || startTime.compareTo(endTime) >= 0) {
            throw new BusinessRuleException("UR-2004", "作業日・作業時間の指定が不正です");
        }
        if (!outageRepository.findByBreakerIdAndWorkDateAndStatusIn(breakerId, workDate, ACTIVE).isEmpty()) {
            throw new BusinessRuleException("UR-2001",
                    b.getSubstation().getName() + " " + b.getBreakerCode() + " は同日に停電作業が申請済みです");
        }
        OutageRequest o = new OutageRequest();
        o.setRequestNo(numbering.next("P", "SEQ_OUTAGE_NO"));
        o.setBreaker(b);
        o.setWorkDate(workDate);
        o.setStartTime(startTime);
        o.setEndTime(endTime);
        o.setDescription(description);
        o.setStatus(OutageStatus.REQUESTED);
        o.setRequestedBy(user);
        o.setRequestedAt(operationDate.now());
        log.info("停電作業 申請 {} {} by {}", o.getRequestNo(), b.getBreakerCode(), user);
        return outageRepository.save(o);
    }

    @Transactional
    public OutageRequest approve(String requestNo, String user) {
        OutageRequest o = transition(requestNo, OutageStatus.REQUESTED, OutageStatus.APPROVED);
        o.setApprovedBy(user);
        o.setApprovedAt(operationDate.now());
        return o;
    }

    @Transactional
    public OutageRequest reject(String requestNo, String user, String remarks) {
        OutageRequest o = transition(requestNo, OutageStatus.REQUESTED, OutageStatus.REJECTED);
        o.setApprovedBy(user);
        o.setApprovedAt(operationDate.now());
        o.setRemarks(remarks);
        return o;
    }

    /** き電停止: 遮断器を開放する */
    @Transactional
    public OutageRequest start(String requestNo, String user) {
        OutageRequest o = get(requestNo);
        if (o.getBreaker().getState() == BreakerState.TRIPPED) {
            throw new BusinessRuleException("UR-2003",
                    o.getBreaker().getBreakerCode() + " はトリップ中です. 故障復旧後に操作してください");
        }
        transition(requestNo, OutageStatus.APPROVED, OutageStatus.IN_PROGRESS);
        o.getBreaker().setState(BreakerState.OPEN);
        log.info("き電停止 {} {} by {}", requestNo, o.getBreaker().getBreakerCode(), user);
        return o;
    }

    /** 復電: 遮断器を投入する */
    @Transactional
    public OutageRequest complete(String requestNo, String user) {
        OutageRequest o = transition(requestNo, OutageStatus.IN_PROGRESS, OutageStatus.COMPLETED);
        o.getBreaker().setState(BreakerState.CLOSED);
        log.info("復電 {} {} by {}", requestNo, o.getBreaker().getBreakerCode(), user);
        return o;
    }

    private OutageRequest transition(String requestNo, OutageStatus from, OutageStatus to) {
        OutageRequest o = get(requestNo);
        if (o.getStatus() != from) {
            throw new BusinessRuleException("UR-2002", requestNo + " は「" + o.getStatus().getLabel()
                    + "」のため「" + to.getLabel() + "」にできません");
        }
        o.setStatus(to);
        return o;
    }

    private static boolean isTime(String s) {
        return s != null && s.matches("([01][0-9]|2[0-3]):[0-5][0-9]");
    }
}
