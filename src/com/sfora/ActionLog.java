package com.sfora;

/**
 * Keeps track of where files are moved. 
 * Essential for the Undo feature we built.
 */
public class ActionLog {
    private String originalPath;
    private String newPath;

    public ActionLog(String originalPath, String newPath) {
        this.originalPath = originalPath;
        this.newPath = newPath;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public String getNewPath() {
        return newPath;
    }
}
