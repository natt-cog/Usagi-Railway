package jp.usagi.railway.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.usagi.railway.domain.Formation;
import jp.usagi.railway.service.FailureService;
import jp.usagi.railway.service.FormationService;
import jp.usagi.railway.service.InspectionDue;
import jp.usagi.railway.service.InspectionService;

@Controller
@RequestMapping("/rolling")
public class RollingStockController {

    private final FormationService formationService;
    private final InspectionService inspectionService;
    private final FailureService failureService;

    public RollingStockController(FormationService formationService, InspectionService inspectionService,
                                  FailureService failureService) {
        this.formationService = formationService;
        this.inspectionService = inspectionService;
        this.failureService = failureService;
    }

    @GetMapping("/formations")
    public String formations(Model model) {
        Map<String, InspectionDue> dues = new LinkedHashMap<String, InspectionDue>();
        for (Formation f : formationService.list()) {
            dues.put(f.getFormationNo(), inspectionService.calculate(f));
        }
        model.addAttribute("formations", formationService.list());
        model.addAttribute("dues", dues);
        model.addAttribute("spares", formationService.spares());
        return "rolling/formations";
    }

    @GetMapping("/formations/{formationNo}")
    public String formation(@PathVariable String formationNo, Model model) {
        Formation f = formationService.get(formationNo);
        model.addAttribute("formation", f);
        model.addAttribute("due", inspectionService.calculate(f));
        model.addAttribute("equipment", formationService.equipmentOf(formationNo));
        model.addAttribute("failures", failureService.forFormation(formationNo));
        return "rolling/formation";
    }

    @GetMapping("/equipment/{serialNo}")
    public String equipment(@PathVariable String serialNo, Model model) {
        model.addAttribute("equipment", formationService.equipment(serialNo));
        model.addAttribute("failures", failureService.forEquipment(serialNo));
        return "rolling/equipment";
    }

    @GetMapping("/inspections")
    public String inspections(Model model) {
        model.addAttribute("dues", inspectionService.listAll());
        model.addAttribute("warnDays", inspectionService.getWarnDays());
        model.addAttribute("fileLines", inspectionService.inspectionDueFile());
        return "rolling/inspections";
    }
}
