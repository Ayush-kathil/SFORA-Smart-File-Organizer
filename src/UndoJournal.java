import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UndoJournal {

    private final File logFile;

    public UndoJournal() {
        this(new File("action_log.txt"));
    }

    public UndoJournal(File logFile) {
        this.logFile = logFile;
    }

    public void logMove(String oldPlace, String newPlace) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            pw.println(oldPlace + " | " + newPlace + " | " + timeStamp);
        } catch (IOException e) {
            System.out.println("Could not write to action log.");
        }
    }

    public void undoLast() {
        if (!logFile.exists()) {
            System.out.println("Nothing to undo.");
            return;
        }

        List<String> entries = readEntries();
        if (entries.isEmpty()) {
            System.out.println("Log is empty. Nothing to undo.");
            return;
        }

        String veryLastAction = entries.remove(entries.size() - 1);
        if (restoreEntry(veryLastAction)) {
            writeEntries(entries);
            System.out.println("Success! Put the last file back.");
        }
    }

    public void undoAll() {
        if (!logFile.exists()) {
            return;
        }

        List<String> entries = readEntries();
        int count = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (restoreEntry(entries.get(i))) {
                count++;
            }
        }

        System.out.println("Success! We put " + count + " files exactly back where they started.");
        logFile.delete();
    }

    public List<String> snapshotEntries() {
        return new ArrayList<>(readEntries());
    }

    private boolean restoreEntry(String row) {
        String[] pieces = row.split(" \\| ");
        if (pieces.length < 2) {
            return false;
        }

        Path original = Paths.get(pieces[0]);
        Path rightNow = Paths.get(pieces[1]);

        try {
            if (Files.exists(rightNow)) {
                if (original.getParent() != null && !Files.exists(original.getParent())) {
                    Files.createDirectories(original.getParent());
                }
                Files.move(rightNow, original, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException e) {
            System.out.println("Oops, could not move " + rightNow.getFileName() + " back.");
        }
        return false;
    }

    private List<String> readEntries() {
        List<String> entries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                entries.add(line);
            }
        } catch (Exception ignored) {
        }
        return entries;
    }

    private void writeEntries(List<String> entries) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile))) {
            for (String entry : entries) {
                pw.println(entry);
            }
        } catch (Exception ignored) {
        }
    }
}
