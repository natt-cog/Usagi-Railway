package jp.usagi.railway.service;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 運用日管理. 検証環境では urms.operation-date で運用日を固定する.
 */
@Service
public class OperationDateService {

    @Value("${urms.operation-date:}")
    private String fixedDate;

    public LocalDate today() {
        if (StringUtils.isNotBlank(fixedDate)) {
            return DateTimeFormat.forPattern("yyyy/MM/dd").parseLocalDate(fixedDate);
        }
        return new LocalDate();
    }

    /** 運用日 + 現在時刻 */
    public Date now() {
        return today().toLocalDateTime(new LocalTime()).toDate();
    }

    /** 帳票番号用の西暦下 2 桁 */
    public String yearSuffix() {
        return String.format("%02d", today().getYear() % 100);
    }
}
