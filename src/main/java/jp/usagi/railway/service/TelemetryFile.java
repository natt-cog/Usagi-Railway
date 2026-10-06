package jp.usagi.railway.service;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

/**
 * 計測伝文ファイル (TLM_YYYYMMDD.DAT). H / D / T レコードで構成される.
 */
public class TelemetryFile {

    private LocalDate date;
    private final List<TelemetryRecord> records = new ArrayList<TelemetryRecord>();

    public static TelemetryFile parse(List<String> lines) {
        TelemetryFile f = new TelemetryFile();
        Integer trailer = null;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            switch (line.charAt(0)) {
            case 'H':
                f.date = DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(line.substring(1, 9));
                break;
            case 'D':
                f.records.add(TelemetryRecord.parse(line));
                break;
            case 'T':
                trailer = Integer.valueOf(line.substring(1, 7));
                break;
            default:
                throw new BusinessRuleException("UR-2201", "不明なレコード区分です: " + line.charAt(0));
            }
        }
        if (f.date == null) {
            throw new BusinessRuleException("UR-2201", "ヘッダレコードがありません");
        }
        if (trailer == null || trailer.intValue() != f.records.size()) {
            throw new BusinessRuleException("UR-2202",
                    "トレーラ件数が一致しません (T=" + trailer + ", D=" + f.records.size() + ")");
        }
        return f;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<TelemetryRecord> getRecords() {
        return records;
    }
}
