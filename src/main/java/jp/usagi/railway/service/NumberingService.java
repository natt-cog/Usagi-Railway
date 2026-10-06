package jp.usagi.railway.service;

import java.math.BigInteger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帳票番号 採番 (P-YY-NNNN 等). Oracle シーケンスを直接参照する.
 */
@Service
public class NumberingService {

    @PersistenceContext
    private EntityManager em;

    private final OperationDateService operationDate;

    public NumberingService(OperationDateService operationDate) {
        this.operationDate = operationDate;
    }

    @Transactional
    public String next(String prefix, String sequenceName) {
        Object v = em.createNativeQuery("SELECT " + sequenceName + ".NEXTVAL FROM DUAL").getSingleResult();
        long n = v instanceof BigInteger ? ((BigInteger) v).longValue() : ((Number) v).longValue();
        return String.format("%s-%s-%04d", prefix, operationDate.yearSuffix(), n);
    }
}
