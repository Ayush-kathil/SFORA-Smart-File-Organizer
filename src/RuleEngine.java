import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class RuleEngine {
    
    // mapping conditions from the text file into memory
    private Map<String, String> keyRules = new HashMap<>();
    private Map<String, String> extRules = new HashMap<>();

    public RuleEngine() {
        loadRules();
    }

    private void loadRules() {
        File file = new File("rules.txt");
        if (!file.exists()) {
            return; // no big deal, we just use the default folder mapping
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // skip comments
                }

                // parse lines like KEYWORD=finance,FOLDER=Taxes
                String[] split = line.split(",");
                if (split.length == 2) {
                    try {
                        String type = split[0].split("=")[0].toUpperCase();
                        String condition = split[0].split("=")[1].toLowerCase();
                        String destFolder = split[1].split("=")[1];

                        if (type.equals("KEYWORD")) {
                            keyRules.put(condition, destFolder);
                        } else if (type.equals("EXTENSION")) {
                            extRules.put(condition, destFolder);
                        }
                    } catch (Exception badFormat) {
                        System.out.println("Skipping a broken rule line: " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Trouble reading the rules: " + e.getMessage());
        }
    }

    // returns the folder name if it matches a custom rule, otherwise null
    public String checkRules(String fileName, String extension) {
        fileName = fileName.toLowerCase();
        
        // keywords first because they are more specific (like "assignment.pdf" should go to University, not just Documents)
        for (String key : keyRules.keySet()) {
            if (fileName.contains(key)) {
                return keyRules.get(key);
            }
        }

        // override extensions next
        if (extension != null && extRules.containsKey(extension.toLowerCase())) {
            return extRules.get(extension.toLowerCase());
        }

        return null; // fallback to hardcoded types in the organizer
    }
}
