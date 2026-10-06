package jp.usagi.railway.api;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jp.usagi.railway.api.dto.EquipmentDto;
import jp.usagi.railway.api.dto.FailureDto;
import jp.usagi.railway.api.dto.FailureRequest;
import jp.usagi.railway.api.dto.FormationDto;
import jp.usagi.railway.domain.FailureRecord;
import jp.usagi.railway.domain.Formation;
import jp.usagi.railway.domain.Severity;
import jp.usagi.railway.service.FailureService;
import jp.usagi.railway.service.FormationService;
import jp.usagi.railway.service.InspectionService;

/** 車両保守 API. メーカー (MAKER) との機器データ共有にも使用する. */
@RestController
@RequestMapping("/api")
public class RollingStockApiController {

    private final FormationService formationService;
    private final InspectionService inspectionService;
    private final FailureService failureService;

    public RollingStockApiController(FormationService formationService, InspectionService inspectionService,
                                     FailureService failureService) {
        this.formationService = formationService;
        this.inspectionService = inspectionService;
        this.failureService = failureService;
    }

    @GetMapping("/formations")
    public List<FormationDto> formations() {
        List<FormationDto> list = new ArrayList<FormationDto>();
        for (Formation f : formationService.list()) {
            list.add(FormationDto.of(f, inspectionService.calculate(f), null));
        }
        return list;
    }

    @GetMapping("/formations/{formationNo}")
    public FormationDto formation(@PathVariable String formationNo) {
        Formation f = formationService.get(formationNo);
        return FormationDto.of(f, inspectionService.calculate(f), formationService.equipmentOf(formationNo));
    }

    @GetMapping("/equipment/{serialNo}")
    public EquipmentDto equipment(@PathVariable String serialNo) {
        return EquipmentDto.of(formationService.equipment(serialNo));
    }

    @GetMapping("/failures")
    public List<FailureDto> failures(@RequestParam(required = false) String formationNo,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String severity) {
        List<FailureDto> list = new ArrayList<FailureDto>();
        for (FailureRecord f : failureService.search(formationNo, status, severity)) {
            list.add(FailureDto.of(f));
        }
        return list;
    }

    @GetMapping("/failures/{failureNo}")
    public FailureDto failure(@PathVariable String failureNo) {
        return FailureDto.of(failureService.get(failureNo));
    }

    @PostMapping("/failures")
    @ResponseStatus(HttpStatus.CREATED)
    public FailureDto register(@Valid @RequestBody FailureRequest req, Principal principal) {
        return FailureDto.of(failureService.register(req.getSerialNo(), null, req.getSymptom(),
                req.getFailureCode(), Severity.valueOf(req.getSeverity()), principal.getName()));
    }
}
