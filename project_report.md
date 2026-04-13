# SFORA: Smart File Organizer with Rule-Based Automation

**Project Report (Markdown Edition)**  
**Date:** March 31, 2026  
**Platform:** Java (Standard Library), Desktop (CLI + Swing GUI)

---

## Table of Contents

1. [Abstract](#1-abstract)
2. [Introduction](#2-introduction)
3. [Problem Statement](#3-problem-statement)
4. [Project Objectives](#4-project-objectives)
5. [Scope and Constraints](#5-scope-and-constraints)
6. [Technology Stack](#6-technology-stack)
7. [System Architecture](#7-system-architecture)
8. [Detailed Module Design](#8-detailed-module-design)
9. [Core Algorithms and Logic](#9-core-algorithms-and-logic)
10. [User Interface Design](#10-user-interface-design)
11. [Rule Engine Design (`rules.txt`)](#11-rule-engine-design-rulestxt)
12. [Undo and Logging Strategy](#12-undo-and-logging-strategy)
13. [Error Handling and Robustness](#13-error-handling-and-robustness)
14. [Complexity and Performance Discussion](#14-complexity-and-performance-discussion)
15. [Testing Strategy and Test Cases](#15-testing-strategy-and-test-cases)
16. [Results and Evaluation](#16-results-and-evaluation)
17. [Limitations](#17-limitations)
18. [Future Improvements](#18-future-improvements)
19. [Deployment and Usage Guide](#19-deployment-and-usage-guide)
20. [Conclusion](#20-conclusion)
21. [Appendix A: Sample Session](#21-appendix-a-sample-session)
22. [Appendix B: Rules Examples](#22-appendix-b-rules-examples)
23. [Appendix C: Suggested Project Rubric Mapping](#23-appendix-c-suggested-project-rubric-mapping)

---

## 1. Abstract

Modern personal computers accumulate mixed files quickly, especially in high-traffic folders such as `Downloads`, `Desktop`, or temporary coursework directories. Over time, these folders become difficult to navigate and maintain. Manual cleanup is repetitive, error-prone, and often postponed. This project addresses the issue through **SFORA (Smart File Organizer with Rule-Based Automation)**, a Java-based desktop application that performs deterministic sorting, file cleanup support, duplicate detection, large-file extraction, and rollback of operations.

SFORA is implemented entirely with Java Standard Library components and does not require external dependencies. The system offers two interaction modes:

- **CLI mode** for straightforward operation and quick scripting-style workflows.
- **GUI mode** using Swing for users who prefer visual interaction.

The core logic maps files into categories using extension and keyword rules, records movements in an action log, and supports reversal of recent or complete operations. The project demonstrates practical software engineering fundamentals including modular design, rule parsing, defensive checks for filesystem actions, and user-oriented features such as preview and undo.

This report presents architecture, implementation details, tested behavior, computational considerations, and realistic future enhancements.

---

## 2. Introduction

File management tools are often either too simple (only extension-based grouping) or too heavy (cloud-synced suites with unnecessary complexity for local student/personal use). SFORA intentionally targets a middle ground:

- Lightweight,
- Local-first,
- Understandable source code,
- Useful in daily workflows.

The project is especially suitable for educational contexts where students are expected to build practical desktop software while demonstrating command over core Java topics: file I/O, collections, control flow, string parsing, exception management, and GUI event handling.

SFORA enables a user to choose a folder and perform one or more cleanup actions safely. The user can first preview what will happen, then run actual organization, detect duplicates, normalize file names, separate very large files, and undo changes if needed.

The design philosophy is:

1. **Simple to run** (compile and execute with plain `javac`/`java` commands).
2. **Simple to understand** (three primary source files).
3. **Safe enough for real folders** (preview + log + undo).
4. **Configurable** (`rules.txt` supports custom routing).

---

## 3. Problem Statement

Users repeatedly face the following practical issues:

- Mixed files in one location (documents, images, media, archives, executables).
- Important files hidden among irrelevant downloads.
- Duplicate files consuming storage.
- Inconsistent file naming (spaces/symbols causing readability or script issues).
- Large files obscuring storage usage patterns.
- Fear of “breaking things” when moving files manually.

A useful organizer must therefore provide:

- Automatic categorization,
- User-defined exceptions and overrides,
- Low-friction operation,
- Reversibility of actions,
- Understandable output feedback.

SFORA targets all of these in a compact implementation.

---

## 4. Project Objectives

### 4.1 Functional Objectives

1. Provide CLI and GUI entry paths.
2. Organize files by extension and custom rules.
3. Support preview mode (dry run).
4. Detect basic duplicates.
5. Normalize malformed file names.
6. Extract files larger than user threshold.
7. Record moves and support undo operations.

### 4.2 Non-Functional Objectives

1. Maintain dependency-free build.
2. Keep architecture readable and beginner-friendly.
3. Run on typical Java-supported desktop systems.
4. Use deterministic logic and predictable folder output.
5. Avoid destructive operations without traceability.

---

## 5. Scope and Constraints

### 5.1 Scope

Included capabilities:

- Local directory processing (single selected folder root).
- Rule-driven and default category mapping.
- Move operations with conflict-safe naming fallback.
- Logging for reverse operations.
- Swing UI for core actions.

### 5.2 Out of Scope

Not included in this version:

- Cryptographic duplicate detection by file content hash.
- Recursive full-tree organize policy (current behavior is top-level file iteration for most operations).
- Real-time background monitoring (“watch folder” daemon mode).
- Multi-user synchronization or cloud backup.
- Cross-device database of file metadata.

### 5.3 Constraints

- Must work with Java standard APIs only.
- Must remain simple enough for academic demonstration and manual evaluation.
- Must avoid over-engineering that reduces clarity.

---

## 6. Technology Stack

### 6.1 Language and Runtime

- **Java**
- Standard JDK runtime and compiler

### 6.2 Libraries/Packages Used

- `java.io.*` for file I/O and stream classes
- `java.nio.file.*` for modern move/path handling
- `java.util.*` for collections and utility classes
- `javax.swing.*` and `java.awt.*` for desktop GUI
- `java.text.SimpleDateFormat` for timestamping action logs

### 6.3 Why This Stack

- Available by default with JDK.
- No setup friction for evaluators.
- Strong educational value for foundational Java concepts.

---

## 7. System Architecture

SFORA follows a simple layered structure:

1. **Entry Layer** (`App.java`)  
   Handles startup prompt and mode selection.

2. **Interaction Layer** (`GUI.java`)  
   Manages user actions in Swing and prints status output in a text area.

3. **Core Logic Layer** (`Sorter.java`)  
   Implements organization, preview, duplicate detection, rename cleanup, large-file extraction, rule parsing, and undo/logging.

### 7.1 Data Flow Summary

- User chooses folder + action.
- Sorter scans folder entries.
- Rule engine determines destination.
- File move operation occurs (if applicable).
- Action is logged in `action_log.txt`.
- Result summary is printed.

### 7.2 Safety Flow

- Folder existence checked before operation.
- Rule file and log file are excluded from movement loops.
- On naming collision, destination name is auto-adjusted.
- Undo reads log and restores in reverse order.

---

## 8. Detailed Module Design

## 8.1 `App.java`

Primary responsibilities:

- Display startup prompt.
- Accept mode choice (`1` CLI, `2` GUI).
- Launch GUI on Swing Event Dispatch Thread.
- In CLI mode, run an action menu loop.

Important design choices:

- Uses `Scanner` for stable console interaction.
- Performs basic input fallback (invalid option defaults to CLI).
- Centralizes menu logic and delegates file operations to `Sorter`.

CLI menu features include:

- Organize files (`rules` or `hybrid` mode)
- Preview changes
- Undo last / undo all
- Find duplicates
- Fix filenames
- Extract large files
- Quit

## 8.2 `GUI.java`

Primary responsibilities:

- Build visual layout with three regions:
  - Top: folder selection
  - Center: output log text area
  - Bottom: action buttons
- Validate folder selection before actions.
- Execute long-running tasks in background thread to keep UI responsive.
- Redirect `System.out` / `System.err` into GUI text display.

UI design characteristics:

- Minimal monochrome style.
- Rounded custom buttons.
- Border/layout spacing for readability.

Notable implementation points:

- `JFileChooser` restricted to directory selection.
- `doWork(...)` helper wraps actions in `new Thread(...)`.
- Undo-all action includes confirmation dialog.

## 8.3 `Sorter.java`

This file is the project’s core engine and includes:

- Organizing logic
- Preview logic
- Duplicate detection
- Filename normalization
- Large file extraction
- Rule parsing and routing
- Logging and undo operations
- Utility helpers (name conflict resolution, empty-folder cleanup)

Data structures used:

- `Map<String, String> wordRules`
- `Map<String, String> extensionRules`
- `Map<String, Integer> mathTally` (per-run category count)
- `List<String>` for log replay operations

---

## 9. Core Algorithms and Logic

## 9.1 Organize Files

### Input

- Source folder
- Mode (`rules`, `hybrid`, etc.)

### Steps

1. List entries in folder.
2. Skip directories, `rules.txt`, and `action_log.txt`.
3. Determine destination based on rules/default categories.
4. Ensure destination directory exists.
5. Resolve name conflict with suffix (`_1`, `_2`, ...).
6. Move file.
7. Log old/new paths with timestamp.
8. Track per-category movement counts.
9. Remove empty leftover folders.
10. Print summary.

### Discussion

The algorithm is deterministic and user-auditable through console output and log records. This improves confidence when organizing important folders.

## 9.2 Preview Changes

A dry-run mode computes destinations but does not execute move operations. This feature is critical for user trust and reduces accidental misclassification.

## 9.3 Duplicate Detection

Current method approximates duplicates by key:

`fileName + '_' + fileSize`

If key repeats, file is flagged as probable duplicate. This is efficient but not content-accurate in all edge cases.

## 9.4 Filename Cleanup

Normalization applies:

- Spaces replaced by underscores
- Non `[a-zA-Z0-9_.-]` symbols removed

This improves shell-friendliness and script compatibility.

## 9.5 Large File Extraction

Given threshold in MB:

- Convert to bytes
- Move files above threshold to `BigFiles/`
- Log operations for undo

Useful for rapid storage triage.

## 9.6 Undo Logic

Two modes:

- **Undo Last**: restores most recent move from end of log.
- **Undo All**: replays log in reverse and then deletes log file.

Reverse order restoration is essential for correctness when multiple chained moves exist.

---

## 10. User Interface Design

The GUI prioritizes practical interaction over decorative complexity.

### 10.1 Layout Composition

- `BorderLayout` root container
- Top panel for folder path and browse button
- Scrollable center text output
- Bottom panel with core action buttons

### 10.2 Interaction Pattern

1. User selects folder.
2. User clicks action button.
3. Task runs in worker thread.
4. Output appears in real-time in text area.

### 10.3 Usability Notes

- Folder path is read-only (avoids accidental manual edits).
- Output panel uses monospaced font for log readability.
- Confirmation shown before irreversible-seeming batch action (`Undo All`).

---

## 11. Rule Engine Design (`rules.txt`)

The rules file allows user customization without code changes.

### 11.1 Supported Rule Types

- `KEYWORD=...,FOLDER=...`
- `EXTENSION=...,FOLDER=...`

### 11.2 Parsing Strategy

- Ignore empty/comment lines.
- Split line by comma into two chunks.
- Parse `ruleType`, `matchWord`, `destination`.
- Store in corresponding map.

### 11.3 Resolution Order

In `rules`/`hybrid` modes:

1. Keyword rules checked first.
2. Extension overrides checked second.
3. If unresolved and not pure `rules` mode, default category mapping is applied.

This allows intentional override behavior while preserving fallback coverage.

---

## 12. Undo and Logging Strategy

### 12.1 Log Record Format

Each move appends one line in `action_log.txt`:

`oldAbsolutePath | newAbsolutePath | yyyy-MM-dd HH:mm:ss`

### 12.2 Why Append-Only Logging

- Simple implementation
- Human-readable for debugging
- Easy reverse replay

### 12.3 Recovery Behavior

- If source parent directory does not exist during undo, it is recreated.
- If moved file is missing, line is skipped safely.
- Undo-all removes log when operation is complete.

This model balances simplicity and practical reliability for local desktop use.

---

## 13. Error Handling and Robustness

SFORA uses defensive checks across major operations:

1. **Input validation**
   - Ensures folder exists and is directory.
   - Handles invalid numeric threshold input.

2. **Filesystem operation guards**
   - Wraps move/rename/file reading in try-catch blocks.
   - Avoids hard crash from single problematic file.

3. **Null safety on directory listing**
   - Verifies `listFiles()` output before iteration.

4. **Non-target file exclusions**
   - Protects rule and log files from accidental movement.

5. **Conflict-safe naming**
   - Auto-increments destination filename if collision exists.

### 13.1 Practical Impact

The tool remains responsive and useful even in imperfect file environments (locked files, permission mismatch, malformed names, partially missing paths).

---

## 14. Complexity and Performance Discussion

Let:

- $n$ = number of top-level files in selected folder
- $r$ = number of active rules

### 14.1 Organize Complexity

- File scan: $O(n)$
- Rule checks per file: up to $O(r)$ in keyword pass
- Total: roughly $O(n \cdot r)$ in worst case (keyword-heavy)

In realistic local folders, both $n$ and $r$ are moderate, and runtime is acceptable.

### 14.2 Duplicate Detection

- Single pass with hash map: $O(n)$ average
- Memory overhead proportional to scanned files

### 14.3 Undo Operations

- Undo last: $O(m)$ to read log lines + $O(1)$ restore operation
- Undo all: $O(m)$ where $m$ is logged actions

### 14.4 I/O Dominance

Actual runtime is often dominated by disk speed, file sizes, and OS filesystem behavior, not purely computational complexity.

---

## 15. Testing Strategy and Test Cases

A practical testing matrix was used focusing on function-level correctness and user-level scenarios.

### 15.1 Test Categories

1. Startup and mode selection
2. Folder validation
3. Rule parsing behavior
4. Movement correctness
5. Undo correctness
6. Edge-case resilience

### 15.2 Representative Test Cases

| Test ID | Scenario | Input | Expected Result |
|---|---|---|---|
| T1 | CLI startup valid option | `1` | CLI menu displayed |
| T2 | GUI startup valid option | `2` | Window opens |
| T3 | Invalid folder path | random invalid path | Re-prompted for folder |
| T4 | Organize hybrid mode | Mixed file set | Files moved to correct folders |
| T5 | Preview mode | Same folder | “Would move…” output only |
| T6 | Undo last | After one organize action | Last moved file restored |
| T7 | Undo all | After multiple moves | All moved files restored |
| T8 | Duplicate detection | Two files same name+size | Duplicate count increments |
| T9 | Filename cleanup | Names with spaces/symbols | Sanitized file names created |
| T10 | Large file extraction | Threshold 50 MB | Files > 50 MB moved to `BigFiles` |
| T11 | Missing rules.txt | No rules file present | App still works using defaults |
| T12 | Name conflict handling | Destination file already exists | New `_1`, `_2` naming applied |

### 15.3 Manual Verification Notes

- Output messages were reviewed for clarity.
- Undo behavior was validated across multiple sessions where log persisted.
- GUI remained responsive during long operations due to threaded execution.

---

## 16. Results and Evaluation

### 16.1 Functional Achievement

The implemented system successfully satisfies the target feature set:

- Multi-mode interface (CLI + GUI)
- Rule-based and default sorting
- Preview support
- Undo mechanisms
- Duplicate and large-file utilities

### 16.2 Usability Outcome

Users can quickly perform folder cleanup with low cognitive load. The workflow is understandable and action feedback is visible immediately.

### 16.3 Reliability Outcome

In normal desktop conditions, file operations complete consistently. Error tolerance prevents abrupt crashes in most practical scenarios.

### 16.4 Educational Outcome

The project demonstrates key academic learning outcomes:

- Object-oriented decomposition,
- Core Java APIs,
- Event-driven GUI,
- Basic state persistence through logging,
- User-oriented software behavior.

---

## 17. Limitations

Despite strong functionality for a lightweight tool, current limitations include:

1. Duplicate detection based only on name+size, not content hash.
2. Extensive try-catch suppression in some areas limits detailed diagnostics.
3. Most operations scan immediate folder level rather than deep recursive organization.
4. No dedicated unit-test framework integration.
5. Log location is fixed and plain-text only.

These are acceptable for an educational release but represent natural upgrade points.

---

## 18. Future Improvements

A practical roadmap can evolve SFORA toward production readiness:

1. **Hash-based duplicates** (`SHA-256` content checks).
2. **Recursive mode toggle** for full directory tree processing.
3. **Structured logging** (JSON/CSV) with operation IDs.
4. **Selective undo** (restore specific batch or category).
5. **Rule editor inside GUI** (create/edit rules without opening text file).
6. **Progress bars and cancellation controls** for long operations.
7. **Automated tests** with mock filesystem scenarios.
8. **Packaging** into executable JAR with launcher script.

---

## 19. Deployment and Usage Guide

### 19.1 Build

From project root:

```bash
mkdir bin
javac -d bin src/*.java
```

### 19.2 Run

```bash
java -cp bin App
```

### 19.3 Recommended First Run

1. Start in CLI mode.
2. Use preview first.
3. Validate mappings and `rules.txt` behavior.
4. Execute organize.
5. Test undo last.

### 19.4 Operational Safety Tips

- Use a backup or sample folder for first trial.
- Keep `rules.txt` syntax clean and consistent.
- Avoid running organizer on system-critical directories.

---

## 20. Conclusion

SFORA provides an effective, comprehensible, and practical solution for local file clutter management using pure Java. The system combines usability (GUI), control (CLI), configurability (`rules.txt`), and recoverability (undo log) in a compact architecture.

The project meets its academic and functional goals by translating common real-world pain points into deterministic automation flows. It demonstrates disciplined software construction under constraints: minimal dependencies, clean separation of concerns, and user-centric safety mechanisms.

With targeted future enhancements such as hash-based duplicate analysis and recursive organization, SFORA can progress from a strong academic project into a robust everyday productivity utility.

---

## 21. Appendix A: Sample Session

### CLI Example (Condensed)

1. User runs app and selects `1` for CLI.
2. User enters folder path.
3. User selects `2` preview:
   - App prints projected target categories.
4. User selects `1` organize in `hybrid` mode:
   - Files moved,
   - Summary count shown.
5. User selects `3` undo last:
   - Last file restored.
6. User selects `8` quit.

### Expected Practical Benefit

- Folder becomes navigable.
- Category buckets are clearer.
- Risk is reduced due to log-backed reversibility.

---

## 22. Appendix B: Rules Examples

Example rules that can be extended in `rules.txt`:

```txt
KEYWORD=assignment,FOLDER=University/Assignments
KEYWORD=invoice,FOLDER=Finances/Invoices
KEYWORD=resume,FOLDER=Career/Resumes
EXTENSION=epub,FOLDER=Documents/Books
EXTENSION=torrent,FOLDER=Downloads/Torrents
```

Best practices:

- Keep keywords lowercase for consistency.
- Avoid overlapping keywords unless order behavior is acceptable.
- Validate with preview mode before large operations.

---

## 23. Appendix C: Suggested Project Rubric Mapping

| Rubric Area | Evidence in SFORA |
|---|---|
| Problem Relevance | Real-world folder clutter automation |
| Technical Implementation | Java I/O, NIO moves, Swing GUI, collections |
| Modularity | Distinct entry, UI, and logic classes |
| User Experience | CLI + GUI, preview mode, clear logs |
| Data Handling | Rule parsing, movement logging, undo replay |
| Reliability | Validation checks, conflict-safe naming |
| Innovation/Practicality | Rule-driven overrides + reversible actions |
| Documentation Quality | README + this detailed Markdown report |

---

## 24. Design Rationale and Trade-Off Analysis

This section explains key implementation decisions and the reasoning behind them.

### 24.1 Why Keep a Three-File Core

The project intentionally concentrates logic into `App.java`, `GUI.java`, and `Sorter.java` to make grading, debugging, and concept tracing straightforward. While larger projects usually split concerns into more packages, a compact topology can be beneficial in academic contexts where evaluator time is limited.

### 24.2 Why Use Plain Text Log Instead of Database

An embedded database could support richer querying and safer transaction semantics, but introduces setup and migration overhead. A plain text append-only log offers:

- Immediate inspectability,
- Zero dependency friction,
- Sufficient functionality for undo-focused workflows.

### 24.3 Why `Files.move(...)` with Replace Existing

For file organization, move semantics are simpler than copy+delete and reduce transient duplication. The code still protects naming collisions through precomputed alternate destination names, so replace behavior applies only after conflict-safe path generation.

### 24.4 Why Keyword Rules Before Extension Rules

Keyword rules represent user intent at a semantic level. A file named `project_report.pdf` may be more meaningful in `University/Projects` than generic `Documents`. Prioritizing keyword matches gives users fine control while extension-based mapping remains fallback-friendly.

### 24.5 Why Keep Default Categories

A pure custom-rules mode can leave unmatched files uncategorized. Default categories ensure every file is still processed in hybrid flows, improving practical usefulness.

### 24.6 Why GUI Captures `System.out`

Instead of duplicating logging logic for CLI and GUI, redirecting standard streams allows a single source of status output. This reduces code duplication and keeps both interfaces behaviorally aligned.

---

## 25. Quality Attributes Evaluation

Beyond functionality, software quality is assessed across multiple dimensions.

### 25.1 Usability

- Clear action menus and button labels.
- Preview mode before mutation.
- Readable operation summaries.

### 25.2 Maintainability

- Concentrated, readable methods.
- Descriptive naming and clear control flow.
- Low cognitive overhead for feature additions.

### 25.3 Reliability

- Defensive file checks.
- Non-fatal exception boundaries.
- Undo and log system for reversibility.

### 25.4 Portability

Built with Java Standard Library and no OS-specific native dependencies. Behavior may vary slightly by filesystem permissions and path semantics, but core compilation and runtime remain cross-platform.

### 25.5 Performance

Sufficient for typical personal folder sizes. I/O remains dominant cost; algorithmic overhead remains moderate.

---

## 26. Security and Data-Safety Considerations

SFORA is a local file utility and does not transmit data over network channels. Even so, data-safety principles are important.

### 26.1 Positive Safety Properties

1. No external network dependency.
2. No hidden background scheduler.
3. No deletion-only destructive workflow in core organize flow.
4. Log-backed recovery for moved files.

### 26.2 Potential Risk Areas

1. Misconfigured rules can relocate files unexpectedly.
2. Permissions mismatch can cause partial operations.
3. Concurrent external edits in same folder may race with organizer actions.

### 26.3 Recommended Operational Controls

1. Run preview first.
2. Test rules on sample folders.
3. Keep backup snapshots for highly important directories.
4. Avoid processing system or application install directories.

---

## 27. Extended Algorithm Pseudocode

### 27.1 Organize Procedure

```text
FUNCTION ORGANIZE(folder, mode):
   files ← listFiles(folder)
   IF files is null OR empty:
      print "Nothing to organize"
      RETURN

   movedCount ← 0
   tally ← empty map

   FOR each file in files:
      IF file is directory OR file name is rules/log:
         CONTINUE

      target ← resolveTarget(file, folder, mode)
      IF target is null:
         CONTINUE

      ensure parent directories of target exist
      safeTarget ← makeUniqueName(target)
      move file -> safeTarget
      append (oldPath, newPath, timestamp) to action log

      movedCount ← movedCount + 1
      tally[parentFolderName(safeTarget)]++

   removedEmptyFolders ← cleanupEmpty(folder)
   print summary(movedCount, removedEmptyFolders, tally)
END FUNCTION
```

### 27.2 Undo Last Procedure

```text
FUNCTION UNDO_LAST():
   IF log file missing:
      print "Nothing to undo"
      RETURN

   lines ← readAllLines(log)
   IF lines empty:
      print "Log empty"
      RETURN

   last ← removeLast(lines)
   IF restoreMove(last) == true:
      rewrite log with remaining lines
      print "Undo last success"
END FUNCTION
```

### 27.3 Resolve Target Procedure

```text
FUNCTION RESOLVE_TARGET(file, root, mode):
   name ← lowercase(file.name)
   ext ← extension(name) OR "unknown"
   targetFolder ← null

   IF mode in {"rules", "hybrid"}:
      FOR each keyword in keywordRules:
         IF name contains keyword:
            targetFolder ← keywordRules[keyword]
      IF targetFolder is null AND ext in extensionRules:
         targetFolder ← extensionRules[ext]

   IF targetFolder is null AND mode != "rules":
      targetFolder ← defaultCategory(ext)

   IF targetFolder is not null:
      RETURN root/targetFolder/file.name
   RETURN null
END FUNCTION
```

---

## 28. Expanded Validation Narrative

This section supplements the test table with scenario-driven observations.

### 28.1 Scenario A: Student Download Folder Cleanup

Initial state:

- `assignment1.pdf`
- `holiday_photo.jpg`
- `setup.zip`
- `invoice_march.pdf`
- `song.mp3`

Rules include `KEYWORD=assignment,FOLDER=University/Assignments` and `KEYWORD=invoice,FOLDER=Finances/Invoices`.

Observed outcome:

- `assignment1.pdf` routed to `University/Assignments`
- `invoice_march.pdf` routed to `Finances/Invoices`
- Remaining files routed by defaults (`Images`, `Archives`, `Media`)

Conclusion: semantic keyword routing successfully overrides generic extension behavior.

### 28.2 Scenario B: Collision Handling

Two files with same destination name encountered sequentially.

Observed outcome:

- First file retains original name.
- Second file automatically renamed with `_1` suffix.

Conclusion: no data loss through overwrite in normal conflict paths.

### 28.3 Scenario C: Undo Reliability

After organizing 40+ files, `undoAll` executed.

Observed outcome:

- Files restored in reverse order,
- Original folder structure recreated where necessary,
- Log removed after completion.

Conclusion: recovery pathway is operational and practically useful.

### 28.4 Scenario D: GUI Responsiveness Under Batch Moves

When processing larger file sets, GUI remained responsive because operations execute in worker threads.

Conclusion: user interaction quality remains acceptable during I/O-heavy workloads.

---

## 29. Comparative Positioning

SFORA can be contrasted with three common alternatives:

1. **Manual sorting**
   - Maximum control, minimal automation, high effort.

2. **Script-only solutions**
   - Flexible but less accessible to non-technical users.

3. **Heavy full-featured organizer tools**
   - Rich capabilities but often overkill for small local workflows.

SFORA occupies a practical middle tier:

- Lightweight,
- Human-readable,
- GUI-accessible,
- Rule-driven,
- Undo-capable.

---

## 30. Maintenance Guide

### 30.1 Updating Default Categories

Edit the extension-to-folder mapping logic inside `figureOutWhereItGoes(...)` in `Sorter.java`.

### 30.2 Adding New Action Buttons in GUI

1. Add button creation in `makeBottomSection()`.
2. Bind handler using `doWork(...)`.
3. Route to existing/new `Sorter` method.

### 30.3 Refactoring Recommendation

For future scaling, extract these classes from `Sorter.java`:

- `RuleEngine`
- `ActionLogger`
- `DuplicateService`
- `CleanupService`

This modularization improves testability and reduces class size.

---

## 31. Final Project Summary

SFORA demonstrates a complete end-to-end desktop utility pipeline:

- Input acquisition,
- Rule-aware decision logic,
- Safe filesystem mutation,
- Transparent output,
- Reversible operations.

It is both academically strong and practically useful. The implementation remains intentionally simple while delivering substantial utility in real user environments.

---

### End of Report
