import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sorter {

    private final FileNameNormalizer normalizer = new FileNameNormalizer();
    private final RuleEngine ruleEngine = new RuleEngine();
    private final UndoJournal undoJournal = new UndoJournal();
    private final FolderScanner folderScanner = new FolderScanner(Paths.get("").toAbsolutePath().normalize());
    private final String reportFile = "organize_report.txt";

    public Sorter() {
    }

    public void reloadRules() {
        ruleEngine.reloadRules();
    }

    public String getRulesSummary() {
        return ruleEngine.getSummary();
    }

    public void configureScanOptions(boolean strictSafetyMode, boolean includeHiddenItems) {
        folderScanner.configure(strictSafetyMode, includeHiddenItems);
    }

    public OrganizePlan buildOrganizePlan(File folder, String mode) {
        List<File> allFiles = folderScanner.collectFilesRecursively(folder);
        List<FileMoveCandidate> moves = new ArrayList<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (File currentFile : allFiles) {
            File targetFile = ruleEngine.resolveTarget(currentFile, folder, mode);
            if (targetFile == null) {
                continue;
            }

            try {
                if (currentFile.getCanonicalFile().equals(targetFile.getCanonicalFile())) {
                    continue;
                }
                String relativeTarget = folder.toPath().relativize(targetFile.toPath()).toString();
                String category = targetFile.getParentFile().getName();
                categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                moves.add(new FileMoveCandidate(currentFile, targetFile, category, relativeTarget));
            } catch (IOException ignored) {
            }
        }

        return new OrganizePlan(mode, moves, categoryCounts);
    }

    public void organizeFiles(File myFolder, String mode) {
        OrganizePlan plan = buildOrganizePlan(myFolder, mode);
        if (plan.isEmpty()) {
            System.out.println("There's nothing here to organize.");
            return;
        }

        System.out.println("\nAlright, starting to move files...");
        int count = 0;
        for (FileMoveCandidate move : plan.getMoves()) {
            try {
                File destination = normalizer.uniquePath(move.getDestination());
                destination.getParentFile().mkdirs();
                Files.move(move.getSource().toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                undoJournal.logMove(move.getSource().getAbsolutePath(), destination.getAbsolutePath());
                System.out.println("Moved: " + move.getSource().getName());
                count++;
            } catch (IOException e) {
                System.out.println("Could not move: " + move.getSource().getName());
            }
        }

        System.out.println("Cleaning up leftover empty folders...");
        int emptyCount = deleteEmptyFolders(myFolder, true);

        System.out.println("\nAll done!");
        System.out.println("Files moved: " + count);
        System.out.println("Empty folders cleared: " + emptyCount);
        for (String cat : plan.getCategoryCounts().keySet()) {
            System.out.println(" -> " + cat + ": " + plan.getCategoryCounts().get(cat));
        }

        writeRunSummary(myFolder, mode, count, emptyCount, plan.getCategoryCounts());
    }

    public void previewChanges(File folder) {
        previewChanges(folder, "hybrid");
    }

    public void previewChanges(File folder, String mode) {
        OrganizePlan plan = buildOrganizePlan(folder, mode);
        if (plan.isEmpty()) {
            System.out.println("There's nothing here to preview.");
            return;
        }

        System.out.println("\n--- Practice Run (Nothing is actually moving) ---");
        System.out.println(plan.describe());

        int shown = 0;
        for (FileMoveCandidate move : plan.getMoves()) {
            if (shown >= 40) {
                System.out.println("... and " + (plan.getMoveCount() - shown) + " more files.");
                break;
            }
            System.out.println("Would move: " + move.getSource().getName() + " -> " + move.getRelativeTarget());
            shown++;
        }
        System.out.println("Finished practice run. " + plan.getMoveCount() + " files are ready to move.");
    }

    public void findDuplications(File folder) {
        List<File> allFiles = folderScanner.collectFilesRecursively(folder);
        if (allFiles.isEmpty()) {
            System.out.println("There's nothing here to scan.");
            return;
        }

        System.out.println("\nScanning for copied files (content-based)...");
        Map<String, String> fileFingerprints = new HashMap<>();
        int dups = 0;

        for (File currentFile : allFiles) {
            String fingerprint = buildFingerprint(currentFile);
            if (fingerprint == null) {
                continue;
            }

            if (fileFingerprints.containsKey(fingerprint)) {
                System.out.println("Found duplicate: " + currentFile.getAbsolutePath());
                System.out.println("  matches: " + fileFingerprints.get(fingerprint));
                dups++;
            } else {
                fileFingerprints.put(fingerprint, currentFile.getAbsolutePath());
            }
        }
        System.out.println("Finished. Found " + dups + " duplicate files.");
    }

    public void fixFilenames(File folder) {
        List<File> allFiles = folderScanner.collectFilesRecursively(folder);
        if (allFiles.isEmpty()) {
            System.out.println("There's nothing here to rename.");
            return;
        }

        int count = 0;
        for (File currentFile : allFiles) {
            String newName = normalizer.normalize(currentFile.getName());
            if (!newName.equals(currentFile.getName())) {
                File renamedFile = normalizer.uniquePath(new File(currentFile.getParentFile(), newName));
                try {
                    Files.move(currentFile.toPath(), renamedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    undoJournal.logMove(currentFile.getAbsolutePath(), renamedFile.getAbsolutePath());
                    System.out.println("Fixed filename: " + renamedFile.getName());
                    count++;
                } catch (IOException e) {
                    System.out.println("Could not rename: " + currentFile.getName());
                }
            }
        }
        System.out.println("Fixed " + count + " messy filenames.");
    }

    public void extractLargeFiles(File folder, long megabytes) {
        List<File> allFiles = folderScanner.collectFilesRecursively(folder);
        if (allFiles.isEmpty()) {
            System.out.println("There's nothing here to scan.");
            return;
        }

        long byteSize = megabytes * 1024 * 1024;
        File giantFolder = new File(folder, "BigFiles");
        int count = 0;

        for (File currentFile : allFiles) {
            if (currentFile.length() < byteSize) {
                continue;
            }

            if (currentFile.getParentFile().equals(giantFolder)) {
                continue;
            }

            try {
                if (!giantFolder.exists()) {
                    giantFolder.mkdirs();
                }

                File safeDest = normalizer.uniquePath(new File(giantFolder, normalizer.normalize(currentFile.getName())));
                Files.move(currentFile.toPath(), safeDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                undoJournal.logMove(currentFile.getAbsolutePath(), safeDest.getAbsolutePath());
                System.out.println("Found huge file: " + currentFile.getName());
                count++;
            } catch (IOException e) {
                System.out.println("Could not move huge file: " + currentFile.getName());
            }
        }
        System.out.println("Moved " + count + " giant files out of the way.");

        System.out.println("Cleaning up leftover empty folders...");
        int emptyCount = deleteEmptyFolders(folder, true);
        System.out.println("Empty folders cleared: " + emptyCount);
        writeRunSummary(folder, "large-files", count, emptyCount, new HashMap<>());
    }

    public void undoLast() {
        undoJournal.undoLast();
    }

    public void undoAll() {
        undoJournal.undoAll();
    }

    private String buildFingerprint(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = stream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            StringBuilder hash = new StringBuilder();
            for (byte b : digest.digest()) {
                hash.append(String.format("%02x", b));
            }

            return file.length() + ":" + hash;
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Could not fingerprint: " + file.getName());
            return null;
        }
    }

    private void writeRunSummary(File rootFolder, String mode, int movedCount, int emptyFoldersRemoved, Map<String, Integer> categoryCounts) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile, true))) {
            String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("[" + stamp + "] mode=" + mode + " root=" + rootFolder.getAbsolutePath());
            writer.println("moved=" + movedCount + ", emptyFoldersDeleted=" + emptyFoldersRemoved);

            if (!categoryCounts.isEmpty()) {
                List<String> categories = new ArrayList<>(categoryCounts.keySet());
                Collections.sort(categories);
                for (String category : categories) {
                    writer.println("  - " + category + ": " + categoryCounts.get(category));
                }
            }
            writer.println();
        } catch (IOException ignored) {
            System.out.println("Could not write organize report.");
        }
    }

    private int deleteEmptyFolders(File mainLogic, boolean isRoot) {
        int removed = 0;
        File[] objects = mainLogic.listFiles();
        if (objects != null) {
            for (File obj : objects) {
                if (obj.isDirectory()) {
                    removed += deleteEmptyFolders(obj, false);
                }
            }
        }
        if (!isRoot && mainLogic.listFiles() != null && mainLogic.listFiles().length == 0) {
            if (mainLogic.delete()) {
                return removed + 1;
            }
        }
        return removed;
    }
}
