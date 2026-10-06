package jp.usagi.railway.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

/**
 * C バッチ URPWD01 (batch/c/urpwd01.c) と Java 電力日報集計の同値性テスト.
 *
 * golden/TLM_20261005.DAT を URPWD01 に入力した結果 (golden/DAILY_20261005_C.DAT) と,
 * 同じ入力に {@link DailyReportService#summarize} / {@link DailyReportService#format} を
 * 適用した結果が 1 バイト単位で一致することを検証する.
 */
public class CBatchParityTest {

    @Test
    public void javaDailyReportMatchesCBatchOutput() throws IOException {
        TelemetryFile tlm = TelemetryFile.parse(GoldenFiles.read("TLM_20261005.DAT"));
        List<String> expected = GoldenFiles.read("DAILY_20261005_C.DAT");

        List<String> actual = DailyReportService.format(tlm.getDate(),
                DailyReportService.summarize(tlm.getRecords(), 1400, 3800));

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("line " + (i + 1), expected.get(i), actual.get(i));
        }
    }
}
