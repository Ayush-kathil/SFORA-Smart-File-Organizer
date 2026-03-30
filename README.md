# Smart File Organizer (SFORA)

My computer's `Downloads` folder is generally an absolute disaster. Every semester it just fills up with thousands of random PDFs from university, tons of project images, zip files, and identical assignment duplicates. I finally got annoyed with manually dragging files around every single week, so I decided to learn native Java file I/O and build **SFORA** over a weekend to completely automate the process.

**Note: This is a 100% pure Java project. It utilizes exactly zero external dependencies, libraries, or APIs. It leverages native `java.io.*` and `java.nio.file.*` standard libraries to safely achieve everything.**

## What it Does (Best Features)
1. **Total Auto-Organization**: Just point it at a messy folder and it scoops everything up, organizing the clutter logically into dedicated folders like `Documents/` and `Images/` based on extensions.
2. **Custom Rules**: You can edit the `rules.txt` config file. Tell it to specifically snag anything with the word "finance" in the file name and dump it into a `Taxes/` folder, completely ignoring its extension.
3. **Dry Run (Preview Mode)**: I was honestly terrified of running it for the first time and dumping my entire hard drive somewhere I couldn't find it. So I built a "Preview changes" mode. It just simulates the logic and spits out what *would* happen (without actually moving a single file).
4. **Permanent Undo Tracker**: Every move is appended straight to an `action_log.txt` file. You can close the app entirely, run it exactly a week later, select `Undo all changes`, and the program natively parses the text log backward and perfectly restores every single file.
5. **Advanced Space Saver**: I added an advanced filter option that allows you to specify a Megabyte threshold (e.g. > `500` MB). The program actively hunts down giant forgotten video/ISO files and surgically extracts them into a `BigFiles/` folder.
6. **Fast Dupe Checking & Filename Cleaning**: Easily removes awful invisible character spaces from file names and catches exact duplicates by cross-checking matching file sizes + name attributes natively.

## How to run it

Because it explicitly avoids complex build wrappers, you can test it on any machine running a JVM instantaneously:

1. Compile the 5 root `.java` source files inside the `src/` directory:
```bash
mkdir bin
javac -d bin src/*.java
```

2. Run the main app!
```bash
java -cp bin Main
```

## Sample Output
```text
---------------------------------
SMART FILE ORGANIZER
---------------------------------
Enter folder path:
> C:\Downloads

Then show options:
1. Organize files (recommended)
2. Preview changes (no files will be moved)
3. Undo last change
4. Undo all changes
5. Find duplicate files
6. Clean file names
7. Extract large files (Space Saver)
8. Exit
> 7
Enter size limit in MB (e.g., 50): 100
Moved huge file: big_system_image.iso (1400MB)

Isolated 1 huge files into the 'BigFiles' folder.
```

Hope this tool natively cleans up your filesystem!
