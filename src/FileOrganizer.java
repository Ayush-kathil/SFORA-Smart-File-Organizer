import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FileOrganizer {

    private RuleEngine rules = new RuleEngine();
    private FileLogger logger = new FileLogger();

    public FileLogger getLogger() {
        return logger;
    }

    // A simple hack to map file types without needing a huge framework
    private String getCategoryName(String extension) {
        String ext = extension.toLowerCase();
        if (ext.equals("pdf") || ext.equals("docx") || ext.equals("txt")) return "Documents";
        if (ext.equals("jpg") || ext.equals("png") || ext.equals("jpeg")) return "Images";
        if (ext.equals("mp4") || ext.equals("mp3") || ext.equals("wav")) return "Media";
        if (ext.equals("zip") || ext.equals("rar")) return "Archives";
        return "Others";
    }

    // ---------------------------------------------------------
    // 1. ORGANIZE FILES (The heavy lifter)
    // ---------------------------------------------------------
    public void organizeNow(File folder, String mode, Scanner scanner) {
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("Nothing to organize here.");
            return;
        }

        System.out.println("Organizing files...");
        int movedCounter = 0;
        Map<String, Integer> summary = new HashMap<>();

        for (File f : files) {
            if (f.isDirectory() || f.getName().equals("action_log.txt") || f.getName().equals("rules.txt")) {
                continue;
            }

            File dest = determineDestination(f, folder, mode);
            if (dest != null) {
                try {
                    // Make the target folder if it doesn't exist
                    if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();

                    // If a file with the same name exists there, change the name slightly
                    File uniqueDest = makeUniqueDest(dest);

                    Files.move(f.toPath(), uniqueDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.logMove(f.getAbsolutePath(), uniqueDest.getAbsolutePath());

                    movedCounter++;

                    // keep track of the summary tallies
                    String category = uniqueDest.getParentFile().getName();
                    summary.put(category, summary.getOrDefault(category, 0) + 1);

                } catch (Exception e) {
                    System.out.println("Error moving " + f.getName());
                }
            }
        }

        // Feature: empty folder cleanup
        System.out.println("Checking for empty folders left behind...");
        int emptyCount = cleanEmptyFolders(folder);

        System.out.println("\n--- Done! ---");
        System.out.println("Total files moved: " + movedCounter);
        System.out.println("Empty folders deleted: " + emptyCount);
        for (String cat : summary.keySet()) {
            System.out.println(" -> " + cat + ": " + summary.get(cat) + " files");
        }
    }

    // ---------------------------------------------------------
    // 2. PREVIEW / DRY RUN
    // ---------------------------------------------------------
    public void previewChanges(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        System.out.println("\n--- DRY RUN (Nothing will actually move) ---");
        
        int count = 0;
        for (File f : files) {
            if (f.isDirectory() || f.getName().equals("action_log.txt") || f.getName().equals("rules.txt")) {
                continue;
            }

            File dest = determineDestination(f, folder, "hybrid"); // preview defaults to hybrid
            if (dest != null) {
                System.out.println(f.getName() + " --> " + dest.getParentFile().getName() + " folder.");
                
                // Warn about massive files (>100MB)
                if (f.length() > 100 * 1024 * 1024) {
                    System.out.println("   [WARNING: This is a massive file!]");
                }
                count++;
            }
        }
        System.out.println("----------------------------------------------");
        System.out.println("Preview finished. Would move " + count + " files.\n");
    }

    // ---------------------------------------------------------
    // 5. DUPLICATE DETECTION (Simple & effective student logic)
    // ---------------------------------------------------------
    public void findDuplicates(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        System.out.println("\nLooking for identical duplicate files...");
        // using a string matching "Filename_FileSize" to catch duplicates fast
        Map<String, String> sizeNameMap = new HashMap<>();
        int dups = 0;

        for (File f : files) {
            if (f.isDirectory()) continue;
            
            String identifier = f.getName() + "_" + f.length();
            if (sizeNameMap.containsKey(identifier)) {
                System.out.println("Found duplicate: " + f.getName());
                dups++;
            } else {
                sizeNameMap.put(identifier, f.getName());
            }
        }
        System.out.println("Found " + dups + " duplicates total.");
    }

    // ---------------------------------------------------------
    // 6. SANITIZE FILE NAMES
    // ---------------------------------------------------------
    public void cleanFilenames(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        int count = 0;
        for (File f : files) {
            if (f.isFile() && !f.getName().equals("rules.txt") && !f.getName().equals("action_log.txt")) {
                String safe = f.getName().replace(" ", "_").replaceAll("[^a-zA-Z0-9_.-]", "");
                if (!safe.equals(f.getName())) {
                    f.renameTo(new File(folder, safe));
                    System.out.println("Renamed: " + safe);
                    count++;
                }
            }
        }
        System.out.println("Cleaned " + count + " ugly filenames.");
    }

    // ---------------------------------------------------------
    // 7. EXTRACT LARGE FILES (Advanced Option - Very student friendly!)
    // ---------------------------------------------------------
    public void extractLargeFiles(File folder, long megabytes) {
        File[] files = folder.listFiles();
        if (files == null) return;

        long byteLimit = megabytes * 1024 * 1024;
        File largeFolder = new File(folder, "BigFiles");
        int moved = 0;

        for (File f : files) {
            // Ignore folders and files under the limit
            if (f.isDirectory() || f.length() < byteLimit) {
                continue;
            }

            try {
                if (!largeFolder.exists()) largeFolder.mkdirs();

                File uniqueDest = makeUniqueDest(new File(largeFolder, f.getName()));
                Files.move(f.toPath(), uniqueDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // Track it in our log!
                logger.logMove(f.getAbsolutePath(), uniqueDest.getAbsolutePath());
                System.out.println("Moved huge file: " + f.getName() + " (" + (f.length() / 1024 / 1024) + "MB)");
                moved++;
            } catch (Exception e) {
                System.out.println("Failed to move massive file: " + f.getName());
            }
        }
        
        if (moved == 0) {
            System.out.println("No files found over " + megabytes + " MB!");
        } else {
            System.out.println("\nIsolated " + moved + " huge files into the 'BigFiles' folder.");
        }
    }


    // --- Helper Methods ---

    // Decolves exactly where a file should go based on the mode the user picked
    private File determineDestination(File f, File rootFolder, String mode) {
        String name = f.getName();
        int dot = name.lastIndexOf(".");
        String ext = (dot > 0) ? name.substring(dot + 1) : "unknown";

        String targetFolderName = null;

        if (mode.equals("rules")) {
            targetFolderName = rules.checkRules(name, ext);
        } else if (mode.equals("type")) {
            targetFolderName = getCategoryName(ext);
        } else {
            // hybrid: try rules first, then type
            targetFolderName = rules.checkRules(name, ext);
            if (targetFolderName == null) {
                targetFolderName = getCategoryName(ext);
            }
        }

        if (targetFolderName != null) {
            return new File(rootFolder, targetFolderName + "/" + name);
        }
        return null;
    }

    // Handles filename collisions so we don't accidentally overwrite files
    private File makeUniqueDest(File originalDest) {
        File current = originalDest;
        int count = 1;
        while (current.exists()) {
            String name = originalDest.getName();
            int dot = name.lastIndexOf(".");
            String base = (dot == -1) ? name : name.substring(0, dot);
            String ext = (dot == -1) ? "" : name.substring(dot);
            current = new File(originalDest.getParentFile(), base + "_" + count + ext);
            count++;
        }
        return current;
    }

    // Basic recursive empty folder deletion
    private int cleanEmptyFolders(File folder) {
        int deleted = 0;
        File[] children = folder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleted += cleanEmptyFolders(child);
                }
            }
        }
        if (folder.listFiles() != null && folder.listFiles().length == 0) {
            if (folder.delete()) return deleted + 1;
        }
        return deleted;
    }
}
