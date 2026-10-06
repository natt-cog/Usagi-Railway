package jp.usagi.railway.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.usagi.railway.api.dto.AlarmDto;
import jp.usagi.railway.api.dto.SubstationDto;
import jp.usagi.railway.domain.Alarm;
import jp.usagi.railway.domain.Substation;
import jp.usagi.railway.service.AlarmService;
import jp.usagi.railway.service.SubstationService;
import jp.usagi.railway.service.TelemetryResult;
import jp.usagi.railway.service.TelemetryService;

@RestController
@RequestMapping("/api")
public class PowerApiController {

    private final SubstationService substationService;
    private final AlarmService alarmService;
    private final TelemetryService telemetryService;

    public PowerApiController(SubstationService substationService, AlarmService alarmService,
                              TelemetryService telemetryService) {
        this.substationService = substationService;
        this.alarmService = alarmService;
        this.telemetryService = telemetryService;
    }

    @GetMapping("/substations")
    public List<SubstationDto> substations() {
        List<SubstationDto> list = new ArrayList<SubstationDto>();
        for (Substation s : substationService.list()) {
            list.add(SubstationDto.of(substationService.get(s.getCode()), substationService.latest(s.getCode())));
        }
        return list;
    }

    @GetMapping("/substations/{code}")
    public SubstationDto substation(@PathVariable String code) {
        return SubstationDto.of(substationService.get(code), substationService.latest(code));
    }

    @GetMapping("/alarms")
    public List<AlarmDto> alarms(@RequestParam(defaultValue = "false") boolean unacked) {
        List<Alarm> src = unacked ? alarmService.unacknowledged() : alarmService.list(0).getContent();
        List<AlarmDto> list = new ArrayList<AlarmDto>();
        for (Alarm a : src) {
            list.add(AlarmDto.of(a));
        }
        return list;
    }

    /** RTU 計測伝文の受信 (伝送装置から 1 時間ごとに POST される) */
    @PostMapping(value = "/telemetry", consumes = MediaType.TEXT_PLAIN_VALUE)
    public TelemetryResult telemetry(@RequestBody String body) {
        return telemetryService.ingest(body);
    }
}
