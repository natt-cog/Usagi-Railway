package jp.usagi.railway.web;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.usagi.railway.service.AlarmService;
import jp.usagi.railway.service.BusinessRuleException;
import jp.usagi.railway.service.DailyReportService;
import jp.usagi.railway.service.OperationDateService;
import jp.usagi.railway.service.SubstationService;

@Controller
@RequestMapping("/power")
public class PowerController {

    private final SubstationService substationService;
    private final AlarmService alarmService;
    private final DailyReportService dailyReportService;
    private final OperationDateService operationDate;

    public PowerController(SubstationService substationService, AlarmService alarmService,
                           DailyReportService dailyReportService, OperationDateService operationDate) {
        this.substationService = substationService;
        this.alarmService = alarmService;
        this.dailyReportService = dailyReportService;
        this.operationDate = operationDate;
    }

    @GetMapping("/substations")
    public String substations(Model model) {
        model.addAttribute("substations", substationService.list());
        model.addAttribute("latest", substationService.latestAll());
        return "power/substations";
    }

    @GetMapping("/substations/{code}")
    public String substation(@PathVariable String code, @RequestParam(required = false) String date, Model model) {
        LocalDate d = parseDate(date);
        model.addAttribute("substation", substationService.get(code));
        model.addAttribute("measurements", substationService.measurements(code, d));
        model.addAttribute("alarms", alarmService.forSubstation(code));
        model.addAttribute("date", d.toString("yyyy/MM/dd"));
        model.addAttribute("uvThreshold", alarmService.getUndervoltageV());
        model.addAttribute("ocThreshold", alarmService.getOvercurrentA());
        return "power/substation";
    }

    @GetMapping("/alarms")
    public String alarms(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("alarms", alarmService.list(page));
        return "power/alarms";
    }

    @PostMapping("/alarms/{id}/ack")
    public String acknowledge(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            alarmService.acknowledge(id, principal.getName());
            ra.addFlashAttribute("message", "警報 #" + id + " を確認しました");
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getErrorCode() + " " + e.getMessage());
        }
        return "redirect:/power/alarms";
    }

    @GetMapping("/daily")
    public String daily(@RequestParam(required = false) String date, Model model) {
        LocalDate d = parseDate(date);
        model.addAttribute("date", d.toString("yyyy/MM/dd"));
        model.addAttribute("rows", dailyReportService.build(d));
        model.addAttribute("fileLines", dailyReportService.buildFile(d));
        return "power/daily";
    }

    @GetMapping(value = "/daily.dat", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String dailyFile(@RequestParam(required = false) String date, HttpServletResponse response) {
        LocalDate d = parseDate(date);
        response.setHeader("Content-Disposition", "attachment; filename=DAILY_" + d.toString("yyyyMMdd") + ".DAT");
        List<String> lines = dailyReportService.buildFile(d);
        return StringUtils.join(lines, "\n") + "\n";
    }

    private LocalDate parseDate(String date) {
        if (StringUtils.isBlank(date)) {
            return operationDate.today();
        }
        return DateTimeFormat.forPattern("yyyy/MM/dd").parseLocalDate(date.trim());
    }
}
