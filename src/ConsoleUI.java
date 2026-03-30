import java.io.File;
import java.util.Scanner;

public class ConsoleUI {
    
    private Scanner scanner;
    private FileOrganizer organizer;
    private File targetFolder;

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
        this.organizer = new FileOrganizer();
    }

    public void startApp() {
        System.out.println("---------------------------------");
        System.out.println("SMART FILE ORGANIZER");
        System.out.println("---------------------------------");

        // Force the user to pick a folder right away to keep things simple
        while (targetFolder == null) {
            System.out.println("Enter folder path:");
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            File dir = new File(input);
            if (dir.exists() && dir.isDirectory()) {
                targetFolder = dir;
            } else {
                System.out.println("Folder doesn't exist. Try again.");
            }
        }
        
        System.out.println("\n(Automatically enabled 'Undo all changes' for this session via action_log.txt)");

        // The main loop
        boolean running = true;
        while (running) {
            System.out.println("\nThen show options:");
            System.out.println("1. Organize files (recommended)");
            System.out.println("2. Preview changes (no files will be moved)");
            System.out.println("3. Undo last change");
            System.out.println("4. Undo all changes");
            System.out.println("5. Find duplicate files");
            System.out.println("6. Clean file names");
            System.out.println("7. Extract large files (Space Saver)");
            System.out.println("8. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleOrganize();
                    break;
                case "2":
                    organizer.previewChanges(targetFolder);
                    break;
                case "3":
                    organizer.getLogger().undoLast();
                    break;
                case "4":
                    organizer.getLogger().undoAll();
                    break;
                case "5":
                    organizer.findDuplicates(targetFolder);
                    break;
                case "6":
                    organizer.cleanFilenames(targetFolder);
                    break;
                case "7":
                    System.out.print("Enter size limit in MB (e.g., 50): ");
                    try {
                        long size = Long.parseLong(scanner.nextLine().trim());
                        organizer.extractLargeFiles(targetFolder, size);
                    } catch (Exception e) {
                        System.out.println("Oops, that wasn't a valid number.");
                    }
                    break;
                case "8":
                    System.out.println("Exiting... Stay organized!");
                    running = false;
                    break;
                default:
                    System.out.println("Pick a number between 1 and 8.");
            }
        }
    }

    private void handleOrganize() {
        System.out.println("\nHow do you want to organize it?");
        System.out.println("- Organize by file type");
        System.out.println("- Organize by rules");
        System.out.println("- Hybrid (recommended)");
        System.out.print("> ");
        String modeString = scanner.nextLine().trim().toLowerCase();
        
        String mode;
        if (modeString.contains("rules")) {
            mode = "rules";
        } else if (modeString.contains("type")) {
            mode = "type";
        } else {
            mode = "hybrid"; // default to hybrid if they type something weird
        }

        organizer.organizeNow(targetFolder, mode, scanner);
    }
}
