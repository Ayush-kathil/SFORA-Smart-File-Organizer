import java.io.File;
import java.util.Scanner;
import javax.swing.SwingUtilities;

public class App {
    
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        
        System.out.println("----- File Organizer Project -----");
        System.out.println("Type 1 to run in Terminal (CLI)");
        System.out.println("Type 2 to run visually (GUI)");
        System.out.print("> ");

        if (!scan.hasNextLine()) {
            System.out.println("No terminal input detected. Launching GUI mode.");
            SwingUtilities.invokeLater(() -> {
                GUI window = new GUI();
                window.setLocationRelativeTo(null);
                window.setVisible(true);
            });
            return;
        }
        String choice = scan.nextLine().trim();

        switch (choice) {
            case "1" -> {
                System.out.println("\n--- Terminal Mode ---\n");
                runCLI(scan);
            }
            case "2" -> {
                System.out.println("\n--- Loading GUI Window ---\n");
                SwingUtilities.invokeLater(() -> {
                    GUI window = new GUI();
                    window.setLocationRelativeTo(null);
                    window.setVisible(true);
                });
            }
            default -> {
                System.out.println("Wrong input. Starting in terminal anyway.");
                runCLI(scan);
            }
        }
    }
    
    // handles the terminal logic natively
    public static void runCLI(Scanner scan) {
        Sorter mySorter = new Sorter();
        File folder = null;

        while (folder == null) {
            System.out.println("Enter the folder path you want to organize:");
            System.out.print("> ");
            if (!scan.hasNextLine()) return;
            String input = scan.nextLine().trim();
            File dir = new File(input);
            if (dir.exists() && dir.isDirectory()) {
                folder = dir;
            } else {
                System.out.println("That folder doesn't exist. Try another path.");
            }
        }

        boolean keepRunning = true;
        while (keepRunning) {
            System.out.println("\nWhat would you like to do?");
            System.out.println("1. Organize my files");
            System.out.println("2. Preview (don't move anything yet)");
            System.out.println("3. Undo my last action");
            System.out.println("4. Undo everything (reset)");
            System.out.println("5. Find any duplicates");
            System.out.println("6. Clean up weird filenames");
            System.out.println("7. Move really large files");
            System.out.println("8. Reload rules from rules.txt");
            System.out.println("9. Show loaded rule summary");
            System.out.println("10. Quit program");
            System.out.print("> ");

            if (!scan.hasNextLine()) return;
            String choice = scan.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.println("Organize mode? (type 'rules' or 'hybrid')");
                    System.out.print("> ");
                    String mode = scan.nextLine().trim().toLowerCase();
                    OrganizePlan plan = mySorter.buildOrganizePlan(folder, mode);
                    if (plan.isEmpty()) {
                        System.out.println("Nothing to organize.");
                        break;
                    }

                    System.out.println("\n--- Confirm Organize ---");
                    System.out.println(plan.describe());
                    int shown = 0;
                    for (FileMoveCandidate move : plan.getMoves()) {
                        if (shown >= 25) {
                            System.out.println("... and " + (plan.getMoveCount() - shown) + " more files.");
                            break;
                        }
                        System.out.println(" - " + move.getSource().getName() + " -> " + move.getRelativeTarget());
                        shown++;
                    }
                    System.out.print("Proceed with these moves? (y/n): ");
                    String confirm = scan.nextLine().trim().toLowerCase();
                    if (confirm.startsWith("y")) {
                        mySorter.organizeFiles(folder, mode);
                    } else {
                        System.out.println("Organize cancelled.");
                    }
                }
                case "2" -> {
                    System.out.println("Preview mode? (type 'rules', 'hybrid', or 'default')");
                    System.out.print("> ");
                    String mode = scan.nextLine().trim().toLowerCase();
                    if (mode.isBlank()) {
                        mode = "hybrid";
                    }
                    mySorter.previewChanges(folder, mode);
                }
                case "3" -> mySorter.undoLast();
                case "4" -> mySorter.undoAll();
                case "5" -> mySorter.findDuplications(folder);
                case "6" -> mySorter.fixFilenames(folder);
                case "7" -> {
                    System.out.print("Enter giant file size limit in MB (e.g., 50): ");
                    try {
                        long size = Long.parseLong(scan.nextLine().trim());
                        mySorter.extractLargeFiles(folder, size);
                    } catch (NumberFormatException e) {
                        System.out.println("That's not a number.");
                    }
                }
                case "8" -> {
                    mySorter.reloadRules();
                    System.out.println("Rules reloaded. " + mySorter.getRulesSummary());
                }
                case "9" -> System.out.println(mySorter.getRulesSummary());
                case "10" -> {
                    System.out.println("Bye!");
                    keepRunning = false;
                }
                default -> System.out.println("Just type a number from 1 to 10.");
            }
        }
    }
}
