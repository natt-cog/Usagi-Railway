package jp.usagi.railway.batch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.experimental.runners.Enclosed;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * 境界値ケースの Java 出力を COBOL ゴールデンと比較する。標準エラーは実行環境依存のため比較せず、
 * ケース 12 は COBOL と同じ既定引数で実行する。
 */
@RunWith(Enclosed.class)
public class Urins01BoundaryParityTest {

    private static final Path BOUNDARY = Paths.get("src/test/resources/golden/boundary");

    @RunWith(Parameterized.class)
    public static class BoundaryCases {

        @Rule
        public TemporaryFolder temporaryFolder = new TemporaryFolder();

        private final String caseName;
        private final String mode;
        private final int javaRc;

        public BoundaryCases(String caseName, String mode, int javaRc) {
            this.caseName = caseName;
            this.mode = mode;
            this.javaRc = javaRc;
        }

        @Parameters(name = "{0}")
        public static Collection<Object[]> cases() throws Exception {
            List<Object[]> cases = new ArrayList<Object[]>();
            int goldenCount = 0;
            int errorCount = 0;
            for (String line : Files.readAllLines(BOUNDARY.resolve("cases.tsv"), StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length != 5) {
                    throw new IllegalStateException("Expected 5 columns in cases.tsv: " + line);
                }
                int expectedRc;
                if ("golden".equals(columns[1])) {
                    goldenCount++;
                    if (!"=".equals(columns[2])) {
                        throw new IllegalStateException("Expected '=' java_rc for golden case: " + columns[0]);
                    }
                    expectedRc = -1;
                } else if ("error".equals(columns[1])) {
                    errorCount++;
                    expectedRc = Integer.parseInt(columns[2]);
                } else {
                    throw new IllegalStateException("Unknown mode in cases.tsv: " + columns[1]);
                }
                cases.add(new Object[] { columns[0], columns[1], expectedRc });
            }
            if (cases.size() != 19 || goldenCount != 14 || errorCount != 5) {
                throw new IllegalStateException("Expected 19 boundary cases (14 golden, 5 error), found "
                        + cases.size() + " (" + goldenCount + " golden, " + errorCount + " error)");
            }
            return cases;
        }

        @Test
        public void matchesExpectedBoundaryResult() throws Exception {
            Path caseDirectory = BOUNDARY.resolve(caseName);
            Path source = caseDirectory.resolve("FORMATIONS.DAT");
            boolean defaultPaths = !Files.exists(source);
            String[] args;
            Path output;
            if (defaultPaths) {
                assertFalse("FORMATIONS.DAT must not exist in the repository working directory",
                        Files.exists(Paths.get("FORMATIONS.DAT")));
                assertFalse("INSPDUE.DAT must not exist in the repository working directory",
                        Files.exists(Paths.get("INSPDUE.DAT")));
                output = Paths.get("INSPDUE.DAT");
                args = new String[0];
            } else {
                Path workDirectory = temporaryFolder.newFolder(caseName).toPath();
                Path input = workDirectory.resolve("FORMATIONS.DAT");
                Files.copy(source, input);
                output = workDirectory.resolve("INSPDUE.DAT");
                args = new String[] { input.toString(), output.toString() };
            }

            RunResult result = run(args);

            if ("golden".equals(mode)) {
                assertEquals(Integer.parseInt(new String(Files.readAllBytes(caseDirectory.resolve("RC")),
                        StandardCharsets.UTF_8).trim()), result.rc);
                assertStdoutEquals(Files.readAllBytes(caseDirectory.resolve("STDOUT.txt")), result.stdout);
                Path expectedOutput = caseDirectory.resolve("INSPDUE.DAT");
                if (Files.exists(expectedOutput)) {
                    assertTrue("Expected output file " + output, Files.exists(output));
                    assertArrayEquals(Files.readAllBytes(expectedOutput), Files.readAllBytes(output));
                } else {
                    assertFalse("Unexpected output file " + output, Files.exists(output));
                }
            } else {
                assertEquals(javaRc, result.rc);
                String[] lines = new String(result.stdout, StandardCharsets.UTF_8).split("\n", -1);
                int lastLine = lines.length - 1;
                if (lastLine >= 0 && lines[lastLine].isEmpty()) {
                    lastLine--;
                }
                assertTrue("Expected final stdout line to start with 'URINS01 E:' but was: "
                        + new String(result.stdout, StandardCharsets.UTF_8),
                        lastLine >= 0 && lines[lastLine].startsWith("URINS01 E:"));
                assertFalse("Unexpected output file " + output, Files.exists(output));
            }
        }
    }

    public static class ExistingGolden {

        @Rule
        public TemporaryFolder temporaryFolder = new TemporaryFolder();

        @Test
        public void matchesExistingCobolGoldenFiles() throws Exception {
            Path input = temporaryFolder.getRoot().toPath().resolve("FORMATIONS.DAT");
            Path output = temporaryFolder.getRoot().toPath().resolve("INSPDUE.DAT");
            Files.copy(Paths.get("src/test/resources/golden/FORMATIONS.DAT"), input);

            RunResult result = run(input.toString(), output.toString());

            assertEquals(4, result.rc);
            assertArrayEquals(Files.readAllBytes(Paths.get("src/test/resources/golden/INSPDUE_COBOL.DAT")),
                    Files.readAllBytes(output));
            assertStdoutEquals(Files.readAllBytes(BOUNDARY.resolve("00-golden-current/STDOUT.txt")),
                    result.stdout);
        }
    }

    private static RunResult run(String... args) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream console = new PrintStream(bytes, true, "UTF-8");
        int rc = Urins01Batch.run(args, console);
        console.flush();
        console.close();
        return new RunResult(rc, bytes.toByteArray());
    }

    private static void assertStdoutEquals(byte[] expected, byte[] actual) {
        assertArrayEquals("stdout mismatch\nExpected:\n" + new String(expected, StandardCharsets.UTF_8)
                + "\nActual:\n" + new String(actual, StandardCharsets.UTF_8), expected, actual);
    }

    private static final class RunResult {
        private final int rc;
        private final byte[] stdout;

        private RunResult(int rc, byte[] stdout) {
            this.rc = rc;
            this.stdout = stdout;
        }
    }
}
