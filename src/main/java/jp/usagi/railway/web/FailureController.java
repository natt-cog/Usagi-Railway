package jp.usagi.railway.web;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.usagi.railway.domain.FailureRecord;
import jp.usagi.railway.domain.FailureStatus;
import jp.usagi.railway.domain.RepairOrder;
import jp.usagi.railway.domain.RepairStatus;
import jp.usagi.railway.domain.Severity;
import jp.usagi.railway.service.BusinessRuleException;
import jp.usagi.railway.service.FailureService;
import jp.usagi.railway.service.FormationService;

@Controller
public class FailureController {

    private final FailureService failureService;
    private final FormationService formationService;

    public FailureController(FailureService failureService, FormationService formationService) {
        this.failureService = failureService;
        this.formationService = formationService;
    }

    @GetMapping("/rolling/failures")
    public String list(@RequestParam(required = false) String formationNo,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String severity, Model model) {
        model.addAttribute("failures", failureService.search(formationNo, status, severity));
        model.addAttribute("formations", formationService.list());
        model.addAttribute("statuses", FailureStatus.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("openRepairs", failureService.openRepairs());
        return "rolling/failures";
    }

    @GetMapping("/rolling/failures/new")
    public String form(@RequestParam(required = false) String serialNo, Model model) {
        model.addAttribute("serialNo", serialNo);
        model.addAttribute("severities", Severity.values());
        return "rolling/failure-form";
    }

    @PostMapping("/rolling/failures")
    public String register(@RequestParam String serialNo, @RequestParam(required = false) String occurredAt,
                           @RequestParam String symptom, @RequestParam(required = false) String failureCode,
                           @RequestParam String severity, Principal principal, RedirectAttributes ra) {
        try {
            if (StringUtils.isBlank(symptom)) {
                throw new BusinessRuleException("UR-0001", "故障内容を入力してください");
            }
            FailureRecord f = failureService.register(serialNo.trim().toUpperCase(), parseDateTime(occurredAt),
                    symptom, failureCode, Severity.valueOf(severity), principal.getName());
            ra.addFlashAttribute("message", "故障を登録しました (" + f.getFailureNo() + ")");
            return "redirect:/rolling/failures/" + f.getFailureNo();
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getErrorCode() + " " + e.getMessage());
            ra.addAttribute("serialNo", serialNo);
            return "redirect:/rolling/failures/new";
        }
    }

    @GetMapping("/rolling/failures/{failureNo}")
    public String detail(@PathVariable String failureNo, Model model) {
        FailureRecord f = failureService.get(failureNo);
        model.addAttribute("failure", f);
        model.addAttribute("repairs", failureService.repairsOf(f));
        model.addAttribute("repairStatuses", RepairStatus.values());
        return "rolling/failure";
    }

    @PostMapping("/rolling/failures/{failureNo}/{action}")
    public String action(@PathVariable String failureNo, @PathVariable String action, Principal principal,
                         RedirectAttributes ra) {
        try {
            if ("investigate".equals(action)) {
                failureService.investigate(failureNo);
                ra.addFlashAttribute("message", failureNo + " を調査中にしました");
            } else if ("repair".equals(action)) {
                RepairOrder r = failureService.requestRepair(failureNo, principal.getName());
                ra.addFlashAttribute("message", r.getMaker() + " へ修理依頼しました (" + r.getOrderNo() + ")");
            } else if ("close".equals(action)) {
                failureService.close(failureNo, principal.getName());
                ra.addFlashAttribute("message", failureNo + " を完了にしました");
            } else {
                throw new IllegalArgumentException(action);
            }
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getErrorCode() + " " + e.getMessage());
        }
        return "redirect:/rolling/failures/" + failureNo;
    }

    /** メーカーによる修理進捗更新 */
    @PostMapping("/rolling/repairs/{orderNo}/progress")
    public String progress(@PathVariable String orderNo, @RequestParam String status,
                           @RequestParam(required = false) String note, Principal principal, RedirectAttributes ra) {
        RepairOrder r = failureService.repair(orderNo);
        try {
            failureService.updateProgress(orderNo, RepairStatus.valueOf(status), note, principal.getName());
            ra.addFlashAttribute("message", orderNo + " の進捗を更新しました");
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getErrorCode() + " " + e.getMessage());
        }
        return "redirect:/rolling/failures/" + r.getFailure().getFailureNo();
    }

    private static Date parseDateTime(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            f.setLenient(false);
            return f.parse(s.trim());
        } catch (ParseException e) {
            throw new BusinessRuleException("UR-0001", "発生日時は yyyy/MM/dd HH:mm 形式で入力してください");
        }
    }
}
