package jp.usagi.railway.batch;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import jp.usagi.railway.domain.InspectionJudge;
import jp.usagi.railway.service.InspectionDue;
import jp.usagi.railway.service.InspectionService;

public class Urins01Batch {

    private static final int RECORD_LENGTH = 40;
    private static final int WARN_DAYS = 14;
    private static final DateTimeFormatter YMD = DateTimeFormat.forPattern("yyyyMMdd");

    private Urins01Batch() {
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        PrintStream console = new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8");
        int rc;
        try {
            rc = run(args, console);
        } catch (Throwable t) {
            console.print("URINS01 E: 予期しない例外 " + t.getClass().getName() + "\n");
            t.printStackTrace();
            rc = 12;
        }
        console.flush();
        System.exit(rc);
    }

    static int run(String[] args, PrintStream console) {
        if (args.length > 2) {
            console.print("URINS01 E: 引数不正\n");
            return 12;
        }

        String inPath = args.length >= 1 ? args[0] : "FORMATIONS.DAT";
        String outPath = args.length >= 2 ? args[1] : "INSPDUE.DAT";
        Path input = Paths.get(inPath);
        Path output = Paths.get(outPath);
        byte[] inputBytes;
        try {
            inputBytes = Files.readAllBytes(input);
        } catch (NoSuchFileException e) {
            console.print("URINS01 E: " + inPath + " OPEN ERROR 35\n");
            return 12;
        } catch (AccessDeniedException e) {
            console.print("URINS01 E: " + inPath + " OPEN ERROR 37\n");
            return 12;
        } catch (IOException e) {
            console.print("URINS01 E: " + inPath + " OPEN ERROR 30\n");
            return 12;
        }

        List<String> records = splitRecords(new String(inputBytes, StandardCharsets.ISO_8859_1));
        int rc = 0;
        LocalDate base = null;
        int count = 0;
        int warn = 0;
        int over = 0;
        boolean summary = false;

        try (java.io.BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.ISO_8859_1)) {
            for (int i = 0; i < records.size(); i++) {
                String raw = records.get(i);
                int recordLength = Math.min(raw.length(), RECORD_LENGTH);
                String rec = raw.length() > RECORD_LENGTH ? raw.substring(0, RECORD_LENGTH) : raw;
                String padded = padRecord(rec);
                char kind = rec.isEmpty() ? ' ' : rec.charAt(0);
                String recordNo = String.format("%06d", i + 1);

                if (kind == 'H') {
                    LocalDate parsed = parseDate(padded.substring(1, 9));
                    if (parsed == null) {
                        console.print("URINS01 E: 日付不正 レコード=" + recordNo + "\n");
                        rc = 12;
                        break;
                    }
                    base = parsed;
                    writeRecord(writer, InspectionService.formatHeader(base));
                } else if (kind == 'D') {
                    if (base == null) {
                        console.print("URINS01 E: ヘッダレコード無し\n");
                        rc = 12;
                        break;
                    }
                    if (recordLength < 38) {
                        console.print("URINS01 E: レコード長不足 レコード=" + recordNo + "\n");
                        rc = 12;
                        break;
                    }
                    LocalDate koban = parseDate(padded.substring(7, 15));
                    LocalDate juyobu = parseDate(padded.substring(15, 23));
                    LocalDate zenpan = parseDate(padded.substring(23, 31));
                    if (koban == null || juyobu == null || zenpan == null) {
                        console.print("URINS01 E: 日付不正 レコード=" + recordNo + "\n");
                        rc = 12;
                        break;
                    }
                    String kmText = padded.substring(31, 38);
                    if (!kmText.matches("[0-9]{7}")) {
                        console.print("URINS01 E: 走行KM不正 レコード=" + recordNo + "\n");
                        rc = 12;
                        break;
                    }
                    InspectionDue due = InspectionService.calculate(rec.substring(1, 7), koban, juyobu, zenpan,
                            Integer.parseInt(kmText), base, WARN_DAYS);
                    writeRecord(writer, formatBatchRecord(due));
                    count++;
                    if (due.getJudge() == InspectionJudge.W) {
                        warn++;
                    } else if (due.getJudge() == InspectionJudge.X) {
                        over++;
                    }
                } else if (kind == 'T') {
                    if (!padded.substring(1, 7).equals(String.format("%06d", count))) {
                        console.print("URINS01 E: トレーラ件数不一致\n");
                        rc = 8;
                        break;
                    }
                } else {
                    console.print("URINS01 W: 不明なレコード区分 " + kind + "\n");
                }
            }

            if (rc == 0) {
                if (base == null) {
                    console.print("URINS01 E: ヘッダレコード無し\n");
                    rc = 12;
                } else {
                    writeRecord(writer, InspectionService.formatTrailer(count, warn, over));
                    rc = over > 0 ? 4 : 0;
                    summary = true;
                }
            }
        } catch (IOException e) {
            console.print("URINS01 E: " + outPath + " WRITE ERROR\n");
            deleteOutput(output);
            return 12;
        } catch (RuntimeException e) {
            console.print("URINS01 E: 予期しない例外 " + e.getClass().getName() + "\n");
            e.printStackTrace(System.err);
            deleteOutput(output);
            return 12;
        }

        if (rc == 12) {
            deleteOutput(output);
        }
        if (summary) {
            console.print(String.format("URINS01 I: 基準日=%s 編成=%06d 注意=%06d 超過=%06d\n",
                    base.toString(YMD), count, warn, over));
        }
        return rc;
    }

    private static List<String> splitRecords(String input) {
        List<String> records = new ArrayList<String>();
        if (input.isEmpty()) {
            return records;
        }
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\n') {
                records.add(removeTrailingCarriageReturn(input.substring(start, i)));
                start = i + 1;
            }
        }
        if (start < input.length()) {
            records.add(removeTrailingCarriageReturn(input.substring(start)));
        }
        return records;
    }

    private static String removeTrailingCarriageReturn(String record) {
        if (record.endsWith("\r")) {
            return record.substring(0, record.length() - 1);
        }
        return record;
    }

    private static String padRecord(String record) {
        if (record.length() >= RECORD_LENGTH) {
            return record;
        }
        StringBuilder padded = new StringBuilder(record);
        while (padded.length() < RECORD_LENGTH) {
            padded.append(' ');
        }
        return padded.toString();
    }

    private static LocalDate parseDate(String value) {
        if (!value.matches("[0-9]{8}")) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value, YMD);
            return date.getYear() >= 1601 && date.getYear() <= 9999 ? date : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String formatBatchRecord(InspectionDue due) {
        String record = InspectionService.formatRecord(due);
        if (Math.abs(due.getDaysRemaining()) >= 10000) {
            int end = record.length() - 2;
            String days = String.format("%04d", Math.abs(due.getDaysRemaining()) % 10000);
            record = record.substring(0, 41) + days + record.substring(end);
        }
        return record;
    }

    private static void writeRecord(java.io.BufferedWriter writer, String record) throws IOException {
        int end = record.length();
        while (end > 0 && record.charAt(end - 1) == ' ') {
            end--;
        }
        writer.write(record, 0, end);
        writer.write('\n');
    }

    private static void deleteOutput(Path output) {
        try {
            Files.deleteIfExists(output);
        } catch (IOException ignored) {
        }
    }
}
