import java.io.File;

public class FileMoveCandidate {
    private final File source;
    private final File destination;
    private final String category;
    private final String relativeTarget;

    public FileMoveCandidate(File source, File destination, String category, String relativeTarget) {
        this.source = source;
        this.destination = destination;
        this.category = category;
        this.relativeTarget = relativeTarget;
    }

    public File getSource() {
        return source;
    }

    public File getDestination() {
        return destination;
    }

    public String getCategory() {
        return category;
    }

    public String getRelativeTarget() {
        return relativeTarget;
    }
}
