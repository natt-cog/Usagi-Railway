package jp.usagi.railway.repository;

import java.util.List;

import jp.usagi.railway.domain.FailureRecord;

public interface FailureRecordRepositoryCustom {

    /** 故障検索 (条件未指定の項目は無視) */
    List<FailureRecord> search(String formationNo, String status, String severity);
}
