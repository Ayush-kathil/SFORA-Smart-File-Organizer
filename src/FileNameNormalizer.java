import java.io.File;

public class FileNameNormalizer {

    public String normalize(String originalName) {
        int dot = originalName.lastIndexOf('.');
        String base = dot > 0 ? originalName.substring(0, dot) : originalName;
        String extension = dot > 0 ? originalName.substring(dot).toLowerCase() : "";

        String normalized = base.toLowerCase()
            .replaceAll("[_\\-]+", " ")
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        if (normalized.isEmpty()) {
            normalized = "untitled";
        }

        return normalized.replace(" ", "-") + extension;
    }

    public File uniquePath(File intendedTarget) {
        File testPath = intendedTarget;
        int count = 1;
        while (testPath.exists()) {
            String name = intendedTarget.getName();
            int dotSpot = name.lastIndexOf('.');
            String basicName = dotSpot == -1 ? name : name.substring(0, dotSpot);
            String extension = dotSpot == -1 ? "" : name.substring(dotSpot);
            testPath = new File(intendedTarget.getParentFile(), basicName + "_" + count + extension);
            count++;
        }
        return testPath;
    }
}
