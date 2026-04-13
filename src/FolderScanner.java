import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderScanner {

    private static final Set<String> CRITICAL_SKIPPED_DIRECTORIES = new HashSet<>(Arrays.asList(
        ".git", ".svn", ".hg"
    ));
    private static final Set<String> SAFE_MODE_SKIPPED_DIRECTORIES = new HashSet<>(Arrays.asList(
        ".idea", ".vscode", "node_modules", "__pycache__",
        "src", "bin", "build", "dist", "target", "out"
    ));

    private final Path appWorkspaceRoot;
    private boolean strictSafetyMode = true;
    private boolean includeHiddenItems = false;

    public FolderScanner(Path appWorkspaceRoot) {
        this.appWorkspaceRoot = appWorkspaceRoot;
    }

    public void configure(boolean strictSafetyMode, boolean includeHiddenItems) {
        this.strictSafetyMode = strictSafetyMode;
        this.includeHiddenItems = includeHiddenItems;
    }

    public List<File> collectFilesRecursively(File root) {
        List<File> files = new ArrayList<>();
        if (root == null || !root.exists() || !root.isDirectory()) {
            return files;
        }

        File[] entries = root.listFiles();
        if (entries == null) {
            return files;
        }

        for (File entry : entries) {
            if (entry.isDirectory()) {
                if (!shouldSkipDirectory(entry)) {
                    files.addAll(collectFilesRecursively(entry));
                }
            } else if (!shouldSkipFile(entry)) {
                files.add(entry);
            }
        }
        return files;
    }

    public boolean shouldSkipFile(File file) {
        String name = file.getName();
        if (name.equals("rules.txt") || name.equals("action_log.txt") || name.equals("organize_report.txt")) {
            return true;
        }

        if (strictSafetyMode && isApplicationWorkspacePath(file)) {
            return true;
        }

        if (!includeHiddenItems) {
            try {
                return file.isHidden();
            } catch (SecurityException ignored) {
                return false;
            }
        }

        return false;
    }

    public boolean shouldSkipDirectory(File directory) {
        String lowerName = directory.getName().toLowerCase();
        if (CRITICAL_SKIPPED_DIRECTORIES.contains(lowerName)) {
            return true;
        }

        if (strictSafetyMode && SAFE_MODE_SKIPPED_DIRECTORIES.contains(lowerName)) {
            return true;
        }

        if (strictSafetyMode && isApplicationWorkspacePath(directory)) {
            return true;
        }

        if (!includeHiddenItems) {
            try {
                return directory.isHidden();
            } catch (SecurityException ignored) {
                return false;
            }
        }

        return false;
    }

    public boolean isApplicationWorkspacePath(File file) {
        try {
            Path normalizedPath = file.toPath().toAbsolutePath().normalize();
            if (!normalizedPath.startsWith(appWorkspaceRoot)) {
                return false;
            }

            Path relative = appWorkspaceRoot.relativize(normalizedPath);
            if (relative.getNameCount() == 0) {
                return false;
            }

            String relativePath = relative.toString().replace('\\', '/').toLowerCase();
            return relativePath.startsWith("src/")
                || relativePath.startsWith("bin/")
                || relativePath.startsWith(".git/")
                || relativePath.equals("readme.md")
                || relativePath.equals("project_report.md")
                || relativePath.equals(".gitignore")
                || relativePath.equals("rules.txt")
                || relativePath.equals("action_log.txt")
                || relativePath.equals("organize_report.txt");
        } catch (Exception ignored) {
            return false;
        }
    }
}
