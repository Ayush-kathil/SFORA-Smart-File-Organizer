package com.sfora;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * The heart of the application. Handles rules parsing, directory scanning,
 * duplication detection via hashing, and moving files.
 */
public class FileOrganizer {

    private List<Rule> rules;
    private Stack<ActionLog> undoHistory;
    private Map<String, Path> hashCache;

    // Statistics for the summary report feature
    private int filesMoved = 0;
    private int duplicatesFound = 0;
    private long totalBytesProcessed = 0; // New metric!

    public FileOrganizer() {
        this.rules = new ArrayList<>();
        this.undoHistory = new Stack<>();
        this.hashCache = new HashMap<>();
    }

    public void loadRules(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Warning: " + filePath + " not found. Falling back to default organization.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String[] condSplit = parts[0].split("=");
                    String[] folderSplit = parts[1].split("=");
                    if (condSplit.length == 2 && folderSplit.length == 2) {
                        rules.add(new Rule(condSplit[0], condSplit[1], folderSplit[1]));
                    }
                }
            }
            System.out.println("-> Loaded " + rules.size() + " custom rules successfully.");
        } catch (Exception e) {
            System.out.println("Error reading rules: " + e.getMessage());
        }
    }

    // --- UNDO FEATURES ---

    public void undoLast() {
        if (undoHistory.isEmpty()) {
            System.out.println("Nothing to undo!");
            return;
        }
        revertAction(undoHistory.pop());
    }

    public void undoAll() {
        if (undoHistory.isEmpty()) {
            System.out.println("No operations to undo!");
            return;
        }
        System.out.println("Reverting ALL " + undoHistory.size() + " recently moved files...");
        int count = 0;
        while (!undoHistory.isEmpty()) {
            revertAction(undoHistory.pop());
            count++;
        }
        System.out.println("Successfully reverted " + count + " files to their original locations.");
    }

    private void revertAction(ActionLog last) {
        try {
            // Must recreate original parent directories just in case Empty Folder Cleanup deleted them!
            Path original = Paths.get(last.getOriginalPath());
            if (!Files.exists(original.getParent())) {
                Files.createDirectories(original.getParent());
            }
            
            Files.move(Paths.get(last.getNewPath()), original, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("-> Restored: " + original.toString());
        } catch (IOException e) {
            System.out.println("-> Undo failed for " + last.getNewPath() + " : " + e.getMessage());
        }
    }

    // --- MAIN ENGINE ---

    public void organize(String targetDir, boolean cleanupFolders, boolean deepScan) {
        Path root = Paths.get(targetDir);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            System.out.println("Invalid directory: " + targetDir);
            return;
        }

        long startTime = System.currentTimeMillis();
        filesMoved = 0;
        duplicatesFound = 0;
        totalBytesProcessed = 0;
        hashCache.clear();

        System.out.println("\n--- Organization Started ---");
        
        // Start the recursive/flat scan
        scanDirectory(root.toFile(), root, deepScan);
        
        // Cleanup leftover empty folders if asked
        if (cleanupFolders) {
            File[] subdirs = root.toFile().listFiles();
            if (subdirs != null) {
                for (File sf : subdirs) {
                    if (sf.isDirectory()) {
                        deleteEmptyFolders(sf);
                    }
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double mbProcessed = totalBytesProcessed / (1024.0 * 1024.0);
        
        System.out.println("\n--- Summary Report ---");
        System.out.println("Total time taken : " + totalTime + " ms");
        System.out.println("Data Processed   : " + String.format("%.2f", mbProcessed) + " MB");
        System.out.println("Files Organized  : " + filesMoved);
        System.out.println("Duplicates Saved : " + duplicatesFound);
        System.out.println("----------------------\n");
    }

    private void scanDirectory(File currentDir, Path root, boolean deepScan) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                // If it's one of the root category folders we created, we probably shouldn't scan inside it to avoid loops
                // But for a simple student project, assuming it's a messy generic folder.
                if (deepScan) {
                    // Prevent recursively organizing files we just moved into our own rule folders
                    boolean isCategoryFolder = f.getParentFile().equals(root.toFile()) && 
                        (f.getName().equals("Documents") || f.getName().equals("Media") || 
                         f.getName().equals("Images") || f.getName().equals("Archives") || f.getName().equals("Duplicates"));
                    
                    if (!isCategoryFolder) {
                        scanDirectory(f, root, deepScan);
                    }
                }
            } else {
                if (!f.getName().equals("rules.txt")) {
                    processFile(f.toPath(), root);
                }
            }
        }
    }

    private void processFile(Path file, Path root) {
        String fileName = file.getFileName().toString();
        String extension = getExtension(fileName);

        try {
            totalBytesProcessed += Files.size(file);
        } catch (IOException ignored) {}

        String safeName = fileName.replace(" ", "_").replaceAll("[^a-zA-Z0-9_.-]", "");
        if (!safeName.equals(fileName)) {
            try {
                Path renamed = file.getParent().resolve(safeName);
                Files.move(file, renamed);
                file = renamed;
                fileName = safeName;
                System.out.println("Cleaned name: " + fileName);
            } catch (IOException ignored) {}
        }

        String hash = getFileChecksum(file);
        if (hash != null) {
            if (hashCache.containsKey(hash)) {
                moveToFolder(file, root.resolve("Duplicates"));
                duplicatesFound++;
                System.out.println("Duplicate caught -> " + fileName);
                return;
            } else {
                hashCache.put(hash, file);
            }
        }

        for (Rule rule : rules) {
            if (rule.matches(fileName, extension)) {
                moveToFolder(file, root.resolve(rule.getTargetFolder()));
                filesMoved++;
                return;
            }
        }

        String categoryFolder = getCategoryForExtension(extension);
        moveToFolder(file, root.resolve(categoryFolder));
        filesMoved++;
    }

    private void moveToFolder(Path source, Path targetDir) {
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String name = source.getFileName().toString();
            Path dest = targetDir.resolve(name);
            int counter = 1;

            while (Files.exists(dest)) {
                String rawStr = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                String extStr = getExtension(name);
                dest = targetDir.resolve(rawStr + "_" + counter + (extStr.isEmpty() ? "" : "." + extStr));
                counter++;
            }

            Files.move(source, dest);
            undoHistory.push(new ActionLog(source.toString(), dest.toString())); // Tracks absolute original path!
            
        } catch (IOException e) {
            System.out.println("Failed to move: " + source.getFileName());
        }
    }

    private String getCategoryForExtension(String ext) {
        ext = ext.toLowerCase();
        List<String> docs = Arrays.asList("pdf", "doc", "docx", "txt", "xlsx", "csv");
        List<String> images = Arrays.asList("jpg", "jpeg", "png", "gif", "svg");
        List<String> media = Arrays.asList("mp4", "mkv", "mp3", "wav");
        List<String> archives = Arrays.asList("zip", "rar", "tar", "gz", "7z");

        if (docs.contains(ext)) return "Documents";
        if (images.contains(ext)) return "Images";
        if (media.contains(ext)) return "Media";
        if (archives.contains(ext)) return "Archives";
        return "Others";
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0 && dot < fileName.length() - 1) ? fileName.substring(dot + 1) : "";
    }

    private String getFileChecksum(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest.digest()) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    private void deleteEmptyFolders(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File subFile : files) {
                if (subFile.isDirectory()) {
                    deleteEmptyFolders(subFile);
                }
            }
        }
        
        if (folder.listFiles() != null && folder.listFiles().length == 0) {
            folder.delete();
            System.out.println("Cleaned up empty folder: " + folder.getName());
        }
    }
}
