package jp.usagi.railway.batch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Urins01BatchTest {

    private static final Path BOUNDARY = Paths.get("src/test/resources/golden/boundary");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void normalRunWritesGoldenFileAndSummary() throws Exception {
        Path input = BOUNDARY.resolve("13-header-only/FORMATIONS.DAT");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");

        RunResult result = run(input, output);

        assertEquals(0, result.rc);
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("13-header-only/INSPDUE.DAT")),
                Files.readAllBytes(output));
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("13-header-only/STDOUT.txt")), result.stdout);
    }

    @Test
    public void overLimitRunMatchesCobolGoldenFile() throws Exception {
        Path input = Paths.get("batch/cobol/data/FORMATIONS.DAT");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");

        RunResult result = run(input, output);

        assertEquals(4, result.rc);
        assertArrayEquals(Files.readAllBytes(Paths.get("batch/cobol/expected/INSPDUE.DAT")),
                Files.readAllBytes(output));
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("00-golden-current/STDOUT.txt")), result.stdout);
    }

    @Test
    public void trailerMismatchKeepsProcessedRecordsAndStopsBeforeFollowingDetail() throws Exception {
        Path input = BOUNDARY.resolve("07-trailer-mismatch/FORMATIONS.DAT");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");

        RunResult result = run(input, output);

        assertEquals(8, result.rc);
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("07-trailer-mismatch/INSPDUE.DAT")),
                Files.readAllBytes(output));
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("07-trailer-mismatch/STDOUT.txt")), result.stdout);
        assertFalse(new String(result.stdout, StandardCharsets.UTF_8).contains("URINS01 I:"));
    }

    @Test
    public void missingInputDoesNotCreateOrModifyOutput() throws Exception {
        Path input = temporaryFolder.getRoot().toPath().resolve("missing.dat");
        Path output = temporaryFolder.getRoot().toPath().resolve("existing.dat");
        byte[] existing = "leave unchanged".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(output, existing);

        RunResult result = run(input, output);

        assertEquals(12, result.rc);
        assertEquals("URINS01 E: " + input.toString() + " OPEN ERROR 35\n",
                new String(result.stdout, StandardCharsets.UTF_8));
        assertArrayEquals(existing, Files.readAllBytes(output));
        Path absentOutput = temporaryFolder.getRoot().toPath().resolve("absent.dat");
        RunResult absent = run(input, absentOutput);
        assertEquals(12, absent.rc);
        assertFalse(Files.exists(absentOutput));
    }

    @Test
    public void threeArgumentsAreRejectedWithoutTouchingFiles() throws Exception {
        Path input = temporaryFolder.getRoot().toPath().resolve("input.dat");
        Path output = temporaryFolder.getRoot().toPath().resolve("output.dat");
        Files.write(input, "H20261005\n".getBytes(StandardCharsets.ISO_8859_1));
        String[] args = { input.toString(), output.toString(), "extra" };
        RunResult result = run(args);

        assertEquals(12, result.rc);
        assertEquals("URINS01 E: 引数不正\n", new String(result.stdout, StandardCharsets.UTF_8));
        assertFalse(Files.exists(output));
    }

    @Test
    public void invalidInputCasesDeleteOutputAndReportFirstError() throws Exception {
        assertInvalid("", "URINS01 E: ヘッダレコード無し\n");
        assertInvalid("DU0001 2026092520230115201810150000000\n",
                "URINS01 E: ヘッダレコード無し\n");
        assertInvalid("H20261005\nDU0001 2023023020230115201810150000000\n",
                "URINS01 E: 日付不正 レコード=000002\n");
        assertInvalid("H20261005\nDU0001 20260925202301152018101500000A0\n",
                "URINS01 E: 走行KM不正 レコード=000002\n");
        assertInvalid("H20261005\nD\n", "URINS01 E: レコード長不足 レコード=000002\n");
        assertInvalid("H20230230\n", "URINS01 E: 日付不正 レコード=000001\n");
    }

    @Test
    public void unknownRecordsWarnAndProcessingContinues() throws Exception {
        Path input = BOUNDARY.resolve("08-unknown-record/FORMATIONS.DAT");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");

        RunResult result = run(input, output);

        assertEquals(0, result.rc);
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("08-unknown-record/INSPDUE.DAT")),
                Files.readAllBytes(output));
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("08-unknown-record/STDOUT.txt")), result.stdout);
        assertEquals(4, occurrences(new String(result.stdout, StandardCharsets.UTF_8),
                "URINS01 W: 不明なレコード区分"));
    }

    @Test
    public void crlfLongRecordsAndMissingFinalLfMatchNormalGoldenOutput() throws Exception {
        byte[] original = Files.readAllBytes(Paths.get("batch/cobol/data/FORMATIONS.DAT"));
        String[] lines = new String(original, StandardCharsets.ISO_8859_1).split("\n");
        StringBuilder transformed = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            if (transformed.length() > 0) {
                transformed.append("\r\n");
            }
            StringBuilder longLine = new StringBuilder(line);
            while (longLine.length() < 40) {
                longLine.append(' ');
            }
            longLine.append('X');
            transformed.append(longLine);
        }
        Path input = temporaryFolder.getRoot().toPath().resolve("crlf.dat");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");
        Files.write(input, transformed.toString().getBytes(StandardCharsets.ISO_8859_1));

        RunResult result = run(input, output);

        assertEquals(4, result.rc);
        assertArrayEquals(Files.readAllBytes(Paths.get("batch/cobol/expected/INSPDUE.DAT")),
                Files.readAllBytes(output));
        assertOutputLinesAreClean(Files.readAllBytes(output));
    }

    @Test
    public void outputRecordsEndWithLfAndHaveNoCrOrTrailingSpaces() throws Exception {
        Path input = BOUNDARY.resolve("11-formation-spaces/FORMATIONS.DAT");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");

        RunResult result = run(input, output);

        assertEquals(0, result.rc);
        assertOutputLinesAreClean(Files.readAllBytes(output));
    }

    @Test
    public void daysBeyondFourDigitsMatchCobolGoldenFile() throws Exception {
        Path input = BOUNDARY.resolve("05-days-over-9999/FORMATIONS.DAT");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");

        RunResult result = run(input, output);

        assertEquals(4, result.rc);
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("05-days-over-9999/INSPDUE.DAT")),
                Files.readAllBytes(output));
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("05-days-over-9999/STDOUT.txt")), result.stdout);
    }

    @Test
    public void formationNumbersIncludingSpacesAreCopiedVerbatim() throws Exception {
        Path input = BOUNDARY.resolve("11-formation-spaces/FORMATIONS.DAT");
        Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");

        RunResult result = run(input, output);

        assertEquals(0, result.rc);
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("11-formation-spaces/INSPDUE.DAT")),
                Files.readAllBytes(output));
        assertArrayEquals(Files.readAllBytes(BOUNDARY.resolve("11-formation-spaces/STDOUT.txt")), result.stdout);
    }

    @Test
    public void mainUsesUtf8WithDefaultPathsAndReturnsProcessStatus() throws Exception {
        Path workingDirectory = temporaryFolder.newFolder("main-defaults").toPath();
        Files.copy(Paths.get("batch/cobol/data/FORMATIONS.DAT"), workingDirectory.resolve("FORMATIONS.DAT"));

        ProcessResult result = runMain(workingDirectory);

        assertEquals(4, result.rc);
        assertArrayEquals("URINS01 I: 基準日=20261005 編成=000006 注意=000003 超過=000001\n"
                .getBytes(StandardCharsets.UTF_8), result.stdout);
        assertTrue(Files.exists(workingDirectory.resolve("INSPDUE.DAT")));
    }

    @Test
    public void mainReturnsTwelveWhenDefaultInputIsMissing() throws Exception {
        Path workingDirectory = temporaryFolder.newFolder("main-missing").toPath();

        ProcessResult result = runMain(workingDirectory);

        assertEquals(12, result.rc);
        assertEquals("URINS01 E: FORMATIONS.DAT OPEN ERROR 35\n",
                new String(result.stdout, StandardCharsets.UTF_8));
        assertFalse(Files.exists(workingDirectory.resolve("INSPDUE.DAT")));
    }

    private void assertInvalid(String inputText, String expectedMessage) throws Exception {
        Path input = temporaryFolder.getRoot().toPath().resolve("invalid-" + System.nanoTime() + ".dat");
        Path output = temporaryFolder.getRoot().toPath().resolve("invalid-out-" + System.nanoTime() + ".dat");
        Files.write(input, inputText.getBytes(StandardCharsets.ISO_8859_1));

        RunResult result = run(input, output);

        assertEquals(12, result.rc);
        assertEquals(expectedMessage, new String(result.stdout, StandardCharsets.UTF_8));
        assertFalse(Files.exists(output));
    }

    private RunResult run(Path input, Path output) throws Exception {
        return run(new String[] { input.toString(), output.toString() });
    }

    private RunResult run(String[] args) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream console = new PrintStream(bytes, true, "UTF-8");
        int rc = Urins01Batch.run(args, console);
        console.flush();
        return new RunResult(rc, bytes.toByteArray());
    }

    private ProcessResult runMain(Path workingDirectory) throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ProcessBuilder builder = new ProcessBuilder(java, "-Dfile.encoding=ANSI_X3.4-1968", "-cp",
                System.getProperty("java.class.path"), "jp.usagi.railway.batch.Urins01Batch");
        builder.directory(workingDirectory.toFile());
        builder.environment().put("LANG", "C");
        builder.environment().put("LC_ALL", "C");
        Process process = builder.start();
        byte[] stdout = readAll(process.getInputStream());
        process.getErrorStream().close();
        return new ProcessResult(process.waitFor(), stdout);
    }

    private byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            bytes.write(buffer, 0, read);
        }
        return bytes.toByteArray();
    }

    private void assertOutputLinesAreClean(byte[] output) {
        assertTrue(output.length > 0);
        assertEquals('\n', output[output.length - 1]);
        for (int i = 0; i < output.length; i++) {
            assertFalse(output[i] == '\r');
            if (output[i] == '\n') {
                assertTrue(i == 0 || output[i - 1] != ' ');
            }
        }
    }

    private int occurrences(String value, String substring) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(substring, index)) >= 0) {
            count++;
            index += substring.length();
        }
        return count;
    }

    private static final class RunResult {
        private final int rc;
        private final byte[] stdout;

        private RunResult(int rc, byte[] stdout) {
            this.rc = rc;
            this.stdout = stdout;
        }
    }

    private static final class ProcessResult {
        private final int rc;
        private final byte[] stdout;

        private ProcessResult(int rc, byte[] stdout) {
            this.rc = rc;
            this.stdout = stdout;
        }
    }
}
