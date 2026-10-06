package jp.usagi.railway.service;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Formation;
import jp.usagi.railway.domain.InspectionJudge;
import jp.usagi.railway.domain.InspectionKind;
import jp.usagi.railway.repository.FormationRepository;

/**
 * 検査期限算出. COBOL バッチ URINS01 (batch/cobol/URINS01.cbl) と同一ロジック.
 *
 *   交番検査   : 前回 + 90 日
 *   重要部検査 : 前回 + 4 年 または 走行 60 万 km
 *   全般検査   : 前回 + 8 年
 */
@Service
@Transactional(readOnly = true)
public class InspectionService {

    public static final int KOBAN_DAYS = 90;
    public static final int JUYOBU_YEARS = 4;
    public static final int ZENPAN_YEARS = 8;
    public static final int JUYOBU_KM_LIMIT = 600000;
    public static final int JUYOBU_KM_WARN = 570000;

    private static final DateTimeFormatter YMD = DateTimeFormat.forPattern("yyyyMMdd");

    private final FormationRepository formationRepository;
    private final OperationDateService operationDate;

    @Value("${urms.inspection.warn-days:14}")
    private int warnDays;

    public InspectionService(FormationRepository formationRepository, OperationDateService operationDate) {
        this.formationRepository = formationRepository;
        this.operationDate = operationDate;
    }

    public List<InspectionDue> listAll() {
        List<InspectionDue> list = new ArrayList<InspectionDue>();
        for (Formation f : formationRepository.findAllByOrderByFormationNoAsc()) {
            list.add(calculate(f));
        }
        return list;
    }

    public InspectionDue calculate(Formation f) {
        return calculate(f.getFormationNo(), new LocalDate(f.getLastKobanOn()), new LocalDate(f.getLastJuyobuOn()),
                new LocalDate(f.getLastZenpanOn()), f.getKmSinceJuyobu(), operationDate.today(), warnDays);
    }

    public static InspectionDue calculate(String formationNo, LocalDate lastKoban, LocalDate lastJuyobu,
                                          LocalDate lastZenpan, int kmSinceJuyobu, LocalDate base, int warnDays) {
        InspectionDue d = new InspectionDue();
        d.setFormationNo(formationNo);
        d.setKobanDue(lastKoban.plusDays(KOBAN_DAYS));
        // Joda の plusYears は 2/29 → 2/28 に丸める (URINS01 ADD-YEARS と同じ)
        d.setJuyobuDue(lastJuyobu.plusYears(JUYOBU_YEARS));
        d.setZenpanDue(lastZenpan.plusYears(ZENPAN_YEARS));

        d.setNextKind(InspectionKind.K);
        d.setNextDue(d.getKobanDue());
        if (d.getJuyobuDue().isBefore(d.getNextDue())) {
            d.setNextKind(InspectionKind.J);
            d.setNextDue(d.getJuyobuDue());
        }
        if (d.getZenpanDue().isBefore(d.getNextDue())) {
            d.setNextKind(InspectionKind.Z);
            d.setNextDue(d.getZenpanDue());
        }
        d.setDaysRemaining(Days.daysBetween(base, d.getNextDue()).getDays());
        d.setKmSinceJuyobu(kmSinceJuyobu);
        d.setKmExceeded(kmSinceJuyobu >= JUYOBU_KM_LIMIT);

        if (d.getDaysRemaining() < 0 || d.isKmExceeded()) {
            d.setJudge(InspectionJudge.X);
        } else if (d.getDaysRemaining() <= warnDays || kmSinceJuyobu >= JUYOBU_KM_WARN) {
            d.setJudge(InspectionJudge.W);
        } else {
            d.setJudge(InspectionJudge.N);
        }
        return d;
    }

    // ---------------------------------------------------------------
    //  ファイル連携 (FORMATIONS.DAT / INSPDUE.DAT)
    // ---------------------------------------------------------------

    /** URINS01 入力ファイル (FORMATIONS.DAT) を DB から作成する. */
    public List<String> formationsFile() {
        List<String> out = new ArrayList<String>();
        out.add("H" + operationDate.today().toString(YMD));
        List<Formation> list = formationRepository.findAllByOrderByFormationNoAsc();
        for (Formation f : list) {
            out.add(String.format("D%-6s%s%s%s%07d", f.getFormationNo(),
                    new LocalDate(f.getLastKobanOn()).toString(YMD), new LocalDate(f.getLastJuyobuOn()).toString(YMD),
                    new LocalDate(f.getLastZenpanOn()).toString(YMD), f.getKmSinceJuyobu()));
        }
        out.add(String.format("T%06d", list.size()));
        return out;
    }

    /** 検査期限ファイル (INSPDUE.DAT) を DB から作成する. */
    public List<String> inspectionDueFile() {
        return formatDueFile(operationDate.today(), listAll());
    }

    /** FORMATIONS.DAT を読み, URINS01 と同じ計算で INSPDUE.DAT 形式の行を返す. */
    public static List<String> processFormationsFile(List<String> lines, int warnDays) {
        LocalDate base = null;
        List<InspectionDue> list = new ArrayList<InspectionDue>();
        for (String rec : lines) {
            if (rec.isEmpty()) {
                continue;
            }
            switch (rec.charAt(0)) {
            case 'H':
                base = LocalDate.parse(rec.substring(1, 9), YMD);
                break;
            case 'D':
                list.add(calculate(rec.substring(1, 7).trim(), LocalDate.parse(rec.substring(7, 15), YMD),
                        LocalDate.parse(rec.substring(15, 23), YMD), LocalDate.parse(rec.substring(23, 31), YMD),
                        Integer.parseInt(rec.substring(31, 38)), base, warnDays));
                break;
            default:
                break;
            }
        }
        return formatDueFile(base, list);
    }

    public static List<String> formatDueFile(LocalDate base, List<InspectionDue> list) {
        List<String> out = new ArrayList<String>();
        out.add(formatHeader(base));
        int warn = 0;
        int over = 0;
        for (InspectionDue d : list) {
            out.add(formatRecord(d));
            if (d.getJudge() == InspectionJudge.W) {
                warn++;
            } else if (d.getJudge() == InspectionJudge.X) {
                over++;
            }
        }
        out.add(formatTrailer(list.size(), warn, over));
        return out;
    }

    public static String formatHeader(LocalDate base) {
        return "H" + base.toString(YMD);
    }

    public static String formatTrailer(int count, int warn, int over) {
        return String.format("T%06d%06d%06d", count, warn, over);
    }

    public static String formatRecord(InspectionDue d) {
        return String.format("D%-6s%s%s%s%s%s%s%04d%s%s", d.getFormationNo(), d.getKobanDue().toString(YMD),
                d.getJuyobuDue().toString(YMD), d.getZenpanDue().toString(YMD), d.getNextKind().name(),
                d.getNextDue().toString(YMD), d.getDaysRemaining() < 0 ? "-" : "+",
                Math.abs(d.getDaysRemaining()), d.isKmExceeded() ? "K" : " ", d.getJudge().name());
    }

    public int getWarnDays() {
        return warnDays;
    }
}
