import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RuleEngine {

    private final Map<String, String> wordRules = new HashMap<>();
    private final Map<String, String> extensionRules = new HashMap<>();
    private final File rulesFile;
    private final FileNameNormalizer normalizer;

    public RuleEngine() {
        this(new File("rules.txt"), new FileNameNormalizer());
    }

    public RuleEngine(File rulesFile) {
        this(rulesFile, new FileNameNormalizer());
    }

    public RuleEngine(File rulesFile, FileNameNormalizer normalizer) {
        this.rulesFile = rulesFile;
        this.normalizer = normalizer;
        reloadRules();
    }

    public void reloadRules() {
        wordRules.clear();
        extensionRules.clear();
        loadRules();
    }

    public String getSummary() {
        return "Rules loaded: " + wordRules.size() + " keyword, " + extensionRules.size() + " extension";
    }

    public File resolveTarget(File item, File root, String mode) {
        String name = item.getName().toLowerCase();
        int dotSpot = name.lastIndexOf('.');
        String ext = dotSpot > 0 ? name.substring(dotSpot + 1) : "unknown";

        String folderTarget = null;
        if ("rules".equals(mode) || "hybrid".equals(mode)) {
            for (Map.Entry<String, String> entry : wordRules.entrySet()) {
                if (name.contains(entry.getKey())) {
                    folderTarget = entry.getValue();
                }
            }
            if (folderTarget == null) {
                folderTarget = extensionRules.get(ext);
            }
        }

        if (folderTarget == null && (!"rules".equals(mode) || "default".equals(mode) || "standard".equals(mode))) {
            folderTarget = getDefaultCategoryPath(ext);
        }

        if (folderTarget != null) {
            return new File(root, folderTarget + "/" + normalizer.normalize(item.getName()));
        }
        return null;
    }

    public String getDefaultCategoryPath(String ext) {
        return switch (ext) {
            case "pdf", "txt", "doc", "docx", "rtf", "odt" -> "Documents/Text";
            case "xls", "xlsx", "csv", "tsv", "ods" -> "Documents/Spreadsheets";
            case "ppt", "pptx", "key" -> "Documents/Presentations";
            case "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "heic" -> "Media/Images";
            case "mp3", "wav", "flac", "m4a", "aac", "ogg" -> "Media/Audio";
            case "mp4", "mkv", "mov", "avi", "webm" -> "Media/Video";
            case "zip", "rar", "7z", "tar", "gz", "bz2" -> "Archives/Compressed";
            case "java", "py", "js", "ts", "cpp", "c", "h", "cs", "go", "rs", "html", "css", "json", "xml", "yml", "yaml", "md", "sql" -> "Development/Code";
            case "exe", "msi", "apk", "dmg", "deb" -> "Installers";
            default -> "Misc";
        };
    }

    private void loadRules() {
        if (!rulesFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(rulesFile))) {
            String row;
            while ((row = reader.readLine()) != null) {
                row = row.trim();
                if (row.isEmpty() || row.startsWith("#")) {
                    continue;
                }

                String[] chunks = row.split(",");
                if (chunks.length != 2) {
                    continue;
                }

                try {
                    String[] left = chunks[0].split("=", 2);
                    String[] right = chunks[1].split("=", 2);
                    if (left.length != 2 || right.length != 2) {
                        continue;
                    }

                    String ruleType = left[0].trim().toUpperCase();
                    String matchWord = left[1].trim().toLowerCase();
                    String destination = right[1].trim();

                    if ("KEYWORD".equals(ruleType)) {
                        wordRules.put(matchWord, destination);
                    } else if ("EXTENSION".equals(ruleType)) {
                        extensionRules.put(matchWord, destination);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }
}
