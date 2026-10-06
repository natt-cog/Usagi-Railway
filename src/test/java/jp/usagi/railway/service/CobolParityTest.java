package jp.usagi.railway.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

/**
 * 旧 COBOL バッチ URINS01 (Git 履歴) と Java 検査期限算出の同値性テスト.
 *
 * golden/FORMATIONS.DAT を URINS01 に入力した結果 (golden/INSPDUE_COBOL.DAT) と,
 * {@link InspectionService#processFormationsFile} の結果が 1 バイト単位で一致することを検証する.
 */
public class CobolParityTest {

    @Test
    public void javaInspectionDueMatchesCobolOutput() throws IOException {
        List<String> expected = GoldenFiles.read("INSPDUE_COBOL.DAT");
        List<String> actual = InspectionService.processFormationsFile(GoldenFiles.read("FORMATIONS.DAT"), 14);

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("line " + (i + 1), expected.get(i), actual.get(i));
        }
    }
}
