package com.sfora;

/**
 * A simple container class for the rules defined in rules.txt.
 * Built simply and quickly to map keywords/extensions to an output folder.
 */
public class Rule {
    private String type; // KEYWORD or EXTENSION
    private String pattern;
    private String targetFolder;

    public Rule(String type, String pattern, String targetFolder) {
        this.type = type.toUpperCase().trim();
        this.pattern = pattern.toLowerCase().trim();
        this.targetFolder = targetFolder.trim();
    }

    public String getType() {
        return type;
    }

    public String getPattern() {
        return pattern;
    }

    public String getTargetFolder() {
        return targetFolder;
    }

    /**
     * Checks if a file name matches this rule.
     */
    public boolean matches(String fileName, String extension) {
        if ("KEYWORD".equals(type) && fileName.toLowerCase().contains(pattern)) {
            return true;
        }
        if ("EXTENSION".equals(type) && extension.equalsIgnoreCase(pattern)) {
            return true;
        }
        return false;
    }
}
