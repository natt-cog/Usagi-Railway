package jp.usagi.railway.web;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.usagi.railway.domain.OutageRequest;
import jp.usagi.railway.service.BusinessRuleException;
import jp.usagi.railway.service.OutageService;
import jp.usagi.railway.service.SubstationService;

@Controller
@RequestMapping("/power/outages")
public class OutageController {

    private final OutageService outageService;
    private final SubstationService substationService;

    public OutageController(OutageService outageService, SubstationService substationService) {
        this.outageService = outageService;
        this.substationService = substationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("outages", outageService.list());
        return "power/outages";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("breakers", substationService.breakers());
        return "power/outage-form";
    }

    @PostMapping
    public String create(@RequestParam Long breakerId, @RequestParam String workDate, @RequestParam String startTime,
                         @RequestParam String endTime, @RequestParam String description, Principal principal,
                         RedirectAttributes ra) {
        try {
            OutageRequest o = outageService.create(breakerId, parseDate(workDate), startTime, endTime, description,
                    principal.getName());
            ra.addFlashAttribute("message", "停電作業を申請しました (" + o.getRequestNo() + ")");
            return "redirect:/power/outages/" + o.getRequestNo();
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getErrorCode() + " " + e.getMessage());
            return "redirect:/power/outages/new";
        }
    }

    @GetMapping("/{requestNo}")
    public String detail(@PathVariable String requestNo, Model model) {
        model.addAttribute("outage", outageService.get(requestNo));
        return "power/outage";
    }

    @PostMapping("/{requestNo}/{action}")
    public String action(@PathVariable String requestNo, @PathVariable String action,
                         @RequestParam(required = false) String remarks, Principal principal,
                         RedirectAttributes ra) {
        String user = principal.getName();
        try {
            if ("approve".equals(action)) {
                outageService.approve(requestNo, user);
                ra.addFlashAttribute("message", requestNo + " を承認しました");
            } else if ("reject".equals(action)) {
                outageService.reject(requestNo, user, remarks);
                ra.addFlashAttribute("message", requestNo + " を却下しました");
            } else if ("start".equals(action)) {
                outageService.start(requestNo, user);
                ra.addFlashAttribute("message", requestNo + " き電停止 (遮断器 切) を記録しました");
            } else if ("complete".equals(action)) {
                outageService.complete(requestNo, user);
                ra.addFlashAttribute("message", requestNo + " 復電 (遮断器 入) を記録しました");
            } else {
                throw new IllegalArgumentException(action);
            }
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getErrorCode() + " " + e.getMessage());
        }
        return "redirect:/power/outages/" + requestNo;
    }

    private static Date parseDate(String s) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd");
            f.setLenient(false);
            return f.parse(s.trim());
        } catch (ParseException e) {
            throw new BusinessRuleException("UR-2004", "作業日は yyyy/MM/dd 形式で入力してください");
        }
    }
}
