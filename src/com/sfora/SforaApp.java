package com.sfora;

import java.util.Scanner;

/**
 * The entry point for the SFORA application.
 * Provides a clean console menu.
 */
public class SforaApp {
    public static void main(String[] args) {
        System.out.println("===============================");
        System.out.println("  SFORA - Smart File Organizer ");
        System.out.println("===============================");
        
        Scanner scanner = new Scanner(System.in);
        FileOrganizer organizer = new FileOrganizer();

        // Check root dir for our user's custom rules
        organizer.loadRules("rules.txt");

        boolean keepRunning = true;
        while (keepRunning) {
            System.out.println("\nOptions:");
            System.out.println("1 - Organize a Directory");
            System.out.println("2 - Undo exactly ONE Last Move");
            System.out.println("3 - Undo ALL RECENT Moves (Full Revert)");
            System.out.println("4 - Exit");
            System.out.print("> Choice: ");

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    System.out.print("Enter full directory path to clean: ");
                    String dir = scanner.nextLine().trim();
                    
                    System.out.print("Deep Scan? (Extract out files trapped inside sub-folders too) (y/n): ");
                    boolean deep = scanner.nextLine().trim().equalsIgnoreCase("y");
                    
                    System.out.print("Do you want to delete empty remnant folders afterwards? (y/n): ");
                    boolean cleanup = scanner.nextLine().trim().equalsIgnoreCase("y");
                    
                    organizer.organize(dir, cleanup, deep);
                    break;
                case "2":
                    organizer.undoLast();
                    break;
                case "3":
                    organizer.undoAll();
                    break;
                case "4":
                    System.out.println("Bye, hope you stay organized!");
                    keepRunning = false;
                    break;
                default:
                    System.out.println("Hmm, try 1, 2, 3, or 4.");
            }
        }
        scanner.close();
    }
}
