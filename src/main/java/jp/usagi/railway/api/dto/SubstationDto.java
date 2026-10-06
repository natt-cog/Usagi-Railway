package jp.usagi.railway.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jp.usagi.railway.domain.Breaker;
import jp.usagi.railway.domain.Measurement;
import jp.usagi.railway.domain.Substation;

public class SubstationDto {

    public String code;
    public String name;
    public String kind;
    public String kindLabel;
    public String lineName;
    public BigDecimal kmPost;
    public Integer feedVoltageV;
    public boolean telemetry;
    public Latest latest;
    public List<BreakerDto> breakers = new ArrayList<BreakerDto>();

    public static class Latest {
        public Date measuredAt;
        public int voltageV;
        public int currentA;
        public int energyKwh;
    }

    public static class BreakerDto {
        public Long id;
        public String code;
        public String name;
        public String state;
        public String stateLabel;
        public Integer ratedCurrentA;
    }

    public static SubstationDto of(Substation s, Measurement m) {
        SubstationDto d = new SubstationDto();
        d.code = s.getCode();
        d.name = s.getName();
        d.kind = s.getKind().name();
        d.kindLabel = s.getKind().getLabel();
        d.lineName = s.getLineName();
        d.kmPost = s.getKmPost();
        d.feedVoltageV = s.getFeedVoltageV();
        d.telemetry = s.isTelemetryEnabled();
        if (m != null) {
            d.latest = new Latest();
            d.latest.measuredAt = m.getMeasuredAt();
            d.latest.voltageV = m.getVoltageV();
            d.latest.currentA = m.getCurrentA();
            d.latest.energyKwh = m.getEnergyKwh();
        }
        for (Breaker b : s.getBreakers()) {
            BreakerDto bd = new BreakerDto();
            bd.id = b.getId();
            bd.code = b.getBreakerCode();
            bd.name = b.getName();
            bd.state = b.getState().name();
            bd.stateLabel = b.getState().getLabel();
            bd.ratedCurrentA = b.getRatedCurrentA();
            d.breakers.add(bd);
        }
        return d;
    }
}
