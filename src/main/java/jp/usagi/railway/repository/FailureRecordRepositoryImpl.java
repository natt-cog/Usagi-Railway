package jp.usagi.railway.repository;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

import jp.usagi.railway.domain.FailureRecord;
import jp.usagi.railway.domain.FailureStatus;
import jp.usagi.railway.domain.Severity;

public class FailureRecordRepositoryImpl implements FailureRecordRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<FailureRecord> search(String formationNo, String status, String severity) {
        StringBuilder jpql = new StringBuilder("SELECT f FROM FailureRecord f WHERE 1 = 1");
        if (StringUtils.isNotBlank(formationNo)) {
            jpql.append(" AND f.formationNo = :formationNo");
        }
        if (StringUtils.isNotBlank(status)) {
            jpql.append(" AND f.status = :status");
        }
        if (StringUtils.isNotBlank(severity)) {
            jpql.append(" AND f.severity = :severity");
        }
        jpql.append(" ORDER BY f.occurredAt DESC");

        TypedQuery<FailureRecord> q = em.createQuery(jpql.toString(), FailureRecord.class);
        if (StringUtils.isNotBlank(formationNo)) {
            q.setParameter("formationNo", formationNo.trim().toUpperCase());
        }
        if (StringUtils.isNotBlank(status)) {
            q.setParameter("status", FailureStatus.valueOf(status));
        }
        if (StringUtils.isNotBlank(severity)) {
            q.setParameter("severity", Severity.valueOf(severity));
        }
        return q.getResultList();
    }
}
