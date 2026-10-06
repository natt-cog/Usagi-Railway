package jp.usagi.railway.service;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class TelemetryFileTest {

    @Test
    public void parsesHeaderDetailTrailer() {
        TelemetryFile f = TelemetryFile.parse(Arrays.asList(
                "H20261006", "DSS0108000138004200000580001", "T000001"));
        assertEquals("2026-10-06", f.getDate().toString());
        assertEquals(1, f.getRecords().size());
        TelemetryRecord r = f.getRecords().get(0);
        assertEquals("SS01", r.getSubstationCode());
        assertEquals(1380, r.getVoltageV());
        assertEquals(4200, r.getCurrentA());
    }

    @Test(expected = BusinessRuleException.class)
    public void rejectsTrailerCountMismatch() {
        TelemetryFile.parse(Arrays.asList("H20261006", "DSS0108000138004200000580001", "T000002"));
    }

    @Test(expected = BusinessRuleException.class)
    public void rejectsMissingHeader() {
        TelemetryFile.parse(Arrays.asList("DSS0108000138004200000580001", "T000001"));
    }
}
