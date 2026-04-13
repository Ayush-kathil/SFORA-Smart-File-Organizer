import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class SforaTests {

    private static int assertions = 0;

    public static void main(String[] args) throws Exception {
        testFilenameNormalization();
        testRuleResolution();
        testUndoLast();
        testUndoAll();
        System.out.println("All tests passed (" + assertions + " assertions).");
    }

    private static void testFilenameNormalization() {
        FileNameNormalizer normalizer = new FileNameNormalizer();

        assertEquals("my-summer-photo-final.jpg", normalizer.normalize("My Summer Photo (Final).JPG"), "normalize mixed-case image name");
        assertEquals("untitled.txt", normalizer.normalize("###.txt"), "normalize empty basename");
        assertEquals("hello-world.md", normalizer.normalize("hello_world.md"), "normalize underscores");
    }

    private static void testRuleResolution() throws Exception {
        Path tempDir = Files.createTempDirectory("sfora-rules");
        Path rulesFile = tempDir.resolve("rules.txt");
        Files.writeString(rulesFile, String.join(System.lineSeparator(), List.of(
            "KEYWORD=assignment,FOLDER=University/Assignments",
            "EXTENSION=pdf,FOLDER=Documents/PDFs"
        )), StandardCharsets.UTF_8);

        RuleEngine ruleEngine = new RuleEngine(rulesFile.toFile());
        File root = tempDir.toFile();

        File keywordMatch = new File(root, "My assignment.pdf");
        File keywordTarget = ruleEngine.resolveTarget(keywordMatch, root, "hybrid");
        assertEquals("University/Assignments/my-assignment.pdf", normalizePath(root.toPath().relativize(keywordTarget.toPath()).toString()), "keyword rule resolution");

        File extensionMatch = new File(root, "Report.pdf");
        File extensionTarget = ruleEngine.resolveTarget(extensionMatch, root, "hybrid");
        assertEquals("Documents/PDFs/report.pdf", normalizePath(root.toPath().relativize(extensionTarget.toPath()).toString()), "extension rule resolution");
    }

    private static void testUndoLast() throws Exception {
        Path tempDir = Files.createTempDirectory("sfora-undo-last");
        Path sourceDir = tempDir.resolve("source");
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);

        Path sourceFile = sourceDir.resolve("example.txt");
        Path targetFile = targetDir.resolve("example.txt");
        Files.writeString(sourceFile, "undo last", StandardCharsets.UTF_8);

        File logFile = tempDir.resolve("action_log.txt").toFile();
        UndoJournal journal = new UndoJournal(logFile);
        journal.logMove(sourceFile.toString(), targetFile.toString());
        Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

        journal.undoLast();

        assertTrue(Files.exists(sourceFile), "undoLast restored source file");
        assertFalse(Files.exists(targetFile), "undoLast removed target file");
    }

    private static void testUndoAll() throws Exception {
        Path tempDir = Files.createTempDirectory("sfora-undo-all");
        Path sourceDir = tempDir.resolve("source");
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(sourceDir);
        Files.createDirectories(targetDir);

        Path firstSource = sourceDir.resolve("one.txt");
        Path secondSource = sourceDir.resolve("two.txt");
        Path firstTarget = targetDir.resolve("one.txt");
        Path secondTarget = targetDir.resolve("two.txt");
        Files.writeString(firstSource, "one", StandardCharsets.UTF_8);
        Files.writeString(secondSource, "two", StandardCharsets.UTF_8);

        File logFile = tempDir.resolve("action_log.txt").toFile();
        UndoJournal journal = new UndoJournal(logFile);
        journal.logMove(firstSource.toString(), firstTarget.toString());
        Files.move(firstSource, firstTarget, StandardCopyOption.REPLACE_EXISTING);
        journal.logMove(secondSource.toString(), secondTarget.toString());
        Files.move(secondSource, secondTarget, StandardCopyOption.REPLACE_EXISTING);

        journal.undoAll();

        assertTrue(Files.exists(firstSource), "undoAll restored first file");
        assertTrue(Files.exists(secondSource), "undoAll restored second file");
        assertFalse(Files.exists(firstTarget), "undoAll removed first target");
        assertFalse(Files.exists(secondTarget), "undoAll removed second target");
    }

    private static void assertEquals(String expected, String actual, String message) {
        assertions++;
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertions++;
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static String normalizePath(String value) {
        return value.replace('\\', '/');
    }
}
