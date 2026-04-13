# File Organizer Project (SFORA)

This is a Java-based desktop application designed to automatically sort and clean up messy directories (like a typical `Downloads` folder). It organizes files into appropriate sub-folders based on file extensions or custom keywords. The project uses only the Java standard library (`java.io`, `java.nio`, `javax.swing`) and does not require external dependencies.

## 📌 Project Overview
The application functions as a hybrid tool. When executed, it allows the user to run standard Terminal interactions (CLI) or a graphical window (GUI). The core mechanism recursively iterates through the selected directory tree (including folders inside folders), physically moves files using `Files.move()` into target folders according to simple conditions, and automatically removes empty subfolders left behind.

## ✨ Key Features
1. **Hybrid Interface**: Supports both a command-line prompt and an advanced Swing control panel.
2. **Smart Folder Structure**: Automatically reads file extensions and places files into clean nested folders such as `Documents/Text`, `Media/Images`, `Media/Audio`, `Development/Code`, `Installers`, and more.
3. **Recursive Processing**: Sorting, previewing, duplicate scanning, filename cleanup, and large-file extraction all work across nested folders.
4. **Auto Empty-Folder Cleanup**: After file move operations, empty subfolders are deleted automatically.
5. **Easy-To-Type File Names**: Files are automatically normalized to type-friendly names (example: `My Summer Photo (Final).JPG` becomes `my-summer-photo-final.jpg`).
6. **Custom Sorting Rules**: The application reads a local `rules.txt` file at runtime. You can specify custom mapping (e.g., if a file name contains "assignment", force it to a `/University` folder).
7. **Accurate Duplicate Finder**: Finds duplicates using content fingerprints (SHA-256), not just name/size guesses.
8. **Action Logging (Undo)**: Every file move or filename cleanup is recorded in `action_log.txt` so changes can be reversed later.
9. **Run Report Logging**: Each organize/extract run appends a short summary to `organize_report.txt` (mode, moved file count, and category totals).
10. **Big File Search**: Scans and isolates massive video or zip files exceeding a specified Megabyte limit into a separate folder.
11. **Rules Management**: Rules can be reloaded from disk, opened directly, and edited from inside the GUI.
12. **Mode-Aware Preview**: Preview follows the selected organize mode instead of hardcoding a single behavior.
13. **Pre-Move Confirmation**: Organize actions now show a detailed move plan before files are changed.
14. **Automated Self-Tests**: A built-in test runner covers rules, undo, and filename normalization.

## 🗂️ Default Folder Layout
When no custom rule matches, files are organized using this structure:

- `Documents/Text`
- `Documents/Spreadsheets`
- `Documents/Presentations`
- `Media/Images`
- `Media/Audio`
- `Media/Video`
- `Archives/Compressed`
- `Development/Code`
- `Installers`
- `Misc`

## 🖥️ Advanced GUI Highlights
- Dashboard-style layout with dedicated operations panel and live output panel.
- Busy state protection to prevent conflicting actions from running at the same time.
- Organize mode selector (`hybrid`, `rules`, `default`) and configurable large-file threshold.
- Organize mode selector (`hybrid`, `rules`, `default`) and configurable large-file threshold.
- Advanced folder selection with quick shortcuts (Documents, Downloads, Desktop), recent-folder memory, and live folder insights (file/folder count + size).
- User comfort and safety controls including `Safe mode` and optional hidden-file scanning.
- Direct action buttons for organize, preview, rename cleanup, duplicate scan, big-file extraction, undo, and report viewing.
- Built-in rules editor plus reload/open actions for `rules.txt`.
- Real-time status badge and progress indicator while background tasks run.

## 📂 File Structure
The project is intentionally kept simple and compressed into three fundamental Java files:
*   `App.java`: Contains the `main` method. It prompts the user for CLI/GUI selection and houses the `Scanner` terminal loop.
*   `GUI.java`: Contains the advanced operator-style Swing interface, background task handling, and live output capture.
*   `Sorter.java`: Facade that coordinates the smaller services.
*   `RuleEngine.java`: Loads and resolves custom file rules.
*   `FolderScanner.java`: Applies safety rules while collecting files.
*   `FileNameNormalizer.java`: Normalizes filenames and avoids collisions.
*   `UndoJournal.java`: Records and restores move operations.
*   `OrganizePlan.java` and `FileMoveCandidate.java`: Represent the preview/confirmation diff.
*   `SforaTests.java`: Self-contained automated test runner.

## 💻 How to Compile & Run

To evaluate the application, open your terminal (Command Prompt, PowerShell, or bash) in the root directory where the `src/` folder is located.

`bin/` is generated during compilation, so it may be missing before the first build.

**Step 1. Compile the code:**
```bash
mkdir bin
javac -d bin src/*.java
```

**Step 2. Run the program:**
```bash
java -cp bin App
```

**Step 3. Run the automated tests:**
```bash
java -cp bin SforaTests
```

*Note: Once executed, simply follow the on-screen prompts to navigate between the Terminal and Graphical window.*
