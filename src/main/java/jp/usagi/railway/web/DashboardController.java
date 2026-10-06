package jp.usagi.railway.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.usagi.railway.domain.InspectionJudge;
import jp.usagi.railway.service.AlarmService;
import jp.usagi.railway.service.FailureService;
import jp.usagi.railway.service.InspectionDue;
import jp.usagi.railway.service.InspectionService;
import jp.usagi.railway.service.OutageService;
import jp.usagi.railway.service.SubstationService;

@Controller
public class DashboardController {

    private final SubstationService substationService;
    private final AlarmService alarmService;
    private final OutageService outageService;
    private final InspectionService inspectionService;
    private final FailureService failureService;

    public DashboardController(SubstationService substationService, AlarmService alarmService,
                               OutageService outageService, InspectionService inspectionService,
                               FailureService failureService) {
        this.substationService = substationService;
        this.alarmService = alarmService;
        this.outageService = outageService;
        this.inspectionService = inspectionService;
        this.failureService = failureService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("substations", substationService.list());
        model.addAttribute("latest", substationService.latestAll());
        model.addAttribute("unackedAlarms", alarmService.unacknowledged());
        model.addAttribute("majorAlarmCount", alarmService.countUnacknowledgedMajor());
        model.addAttribute("pendingOutageCount", outageService.countPending());

        List<InspectionDue> attention = new ArrayList<InspectionDue>();
        for (InspectionDue d : inspectionService.listAll()) {
            if (d.getJudge() != InspectionJudge.N) {
                attention.add(d);
            }
        }
        model.addAttribute("inspectionAttention", attention);
        model.addAttribute("openFailureCount", failureService.countOpen());
        model.addAttribute("openRepairs", failureService.openRepairs());
        return "dashboard";
    }
}
