import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileLogger {
    // keeping track of everything so we don't accidentally lose files forever
    private final String logFileName = "action_log.txt";
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void logMove(String originalPath, String newPath) {
        // Just append the latest move to the bottom of the log file
        try (FileWriter fw = new FileWriter(logFileName, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String time = formatter.format(new Date());
            // using a simple pipe delimiter: old | new | time
            pw.println(originalPath + " | " + newPath + " | " + time);
            
        } catch (Exception e) {
            System.out.println("Failed to write to log. This is bad. Error: " + e.getMessage());
        }
    }

    public void undoLast() {
        File log = new File(logFileName);
        if (!log.exists()) {
            System.out.println("No log found to undo.");
            return;
        }

        List<String> lines = readAllLines(log);
        if (lines.isEmpty()) {
            System.out.println("Nothing left in the log to undo.");
            return;
        }

        // grab the very last line
        String lastLine = lines.remove(lines.size() - 1);
        boolean success = revertLogLine(lastLine);

        if (success) {
            // write the file back without that last line
            writeAllLines(log, lines);
            System.out.println("Successfully undid the last file move.");
        } else {
            System.out.println("Failed to undo that file.");
        }
    }

    public void undoAll() {
        File log = new File(logFileName);
        if (!log.exists()) {
            System.out.println("No log file found. Can't undo what isn't recorded.");
            return;
        }

        List<String> lines = readAllLines(log);
        if (lines.isEmpty()) {
            System.out.println("Log is empty.");
            return;
        }

        System.out.println("Reverting " + lines.size() + " files...");
        int count = 0;

        // go backward to avoid weird conflicts
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (revertLogLine(lines.get(i))) {
                count++;
            }
        }

        System.out.println("Done! Successfully put back " + count + " files.");
        log.delete(); // delete the file so we start fresh next time
    }

    private boolean revertLogLine(String line) {
        String[] parts = line.split(" \\| ");
        if (parts.length < 2) return false;

        Path original = Paths.get(parts[0]);
        Path current = Paths.get(parts[1]);

        try {
            if (Files.exists(current)) {
                if (!Files.exists(original.getParent())) {
                    Files.createDirectories(original.getParent()); // recreate folders if they got deleted
                }
                Files.move(current, original, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error moving " + current.getFileName() + " back: " + e.getMessage());
        }
        return false;
    }

    private List<String> readAllLines(File f) {
        List<String> l = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String s;
            while ((s = br.readLine()) != null) l.add(s);
        } catch (Exception ignored) {}
        return l;
    }

    private void writeAllLines(File f, List<String> lines) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for (String s : lines) pw.println(s);
        } catch (Exception ignored) {}
    }
}
