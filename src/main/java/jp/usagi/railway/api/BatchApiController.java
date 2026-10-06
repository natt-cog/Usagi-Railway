package jp.usagi.railway.api;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.usagi.railway.service.DailyReportService;
import jp.usagi.railway.service.InspectionService;
import jp.usagi.railway.service.OperationDateService;

/**
 * バッチ連携ファイル出力 (運用管理サーバの JP1 ジョブから取得される).
 */
@RestController
@RequestMapping(value = "/api/batch", produces = "text/plain;charset=UTF-8")
public class BatchApiController {

    private final DailyReportService dailyReportService;
    private final InspectionService inspectionService;
    private final OperationDateService operationDate;

    public BatchApiController(DailyReportService dailyReportService, InspectionService inspectionService,
                              OperationDateService operationDate) {
        this.dailyReportService = dailyReportService;
        this.inspectionService = inspectionService;
        this.operationDate = operationDate;
    }

    /** DAILY_YYYYMMDD.DAT 相当 */
    @GetMapping("/daily-report")
    public String dailyReport(@RequestParam(required = false) String date) {
        LocalDate d = StringUtils.isBlank(date) ? operationDate.today()
                : DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(date);
        return join(dailyReportService.buildFile(d));
    }

    /** URINS01 入力 FORMATIONS.DAT */
    @GetMapping("/formations-file")
    public String formationsFile() {
        return join(inspectionService.formationsFile());
    }

    /** INSPDUE.DAT 相当 */
    @GetMapping("/inspection-due")
    public String inspectionDue() {
        return join(inspectionService.inspectionDueFile());
    }

    private static String join(List<String> lines) {
        return StringUtils.join(lines, "\n") + "\n";
    }
}
