<h1 align="center">SFORA - Smart File Organizer</h1>

<p align="center">
An intelligent desktop application for categorizing, deduplicating, and normalizing files automatically using configurable rules and a graphical interface.
</p>

## Overview

SFORA is a Java-based utility designed to automate file organization. It scans target directories and intelligently relocates files into a structured hierarchy. The system operates on a dual-mode engine, allowing classification through explicitly defined keyword and extension rules, or via built-in default categorizations. 

It provides both a Command Line Interface (CLI) and a Swing-based Graphical User Interface (GUI), ensuring flexibility for different deployment environments and user preferences.

## Key Features

- **Rule-Based Routing**: Define destination paths based on filename keywords or extensions using a plaintext configuration file (`rules.txt`).
- **Content-Based Deduplication**: Identifies exact duplicate files by computing and comparing SHA-256 cryptographic hashes of their contents.
- **Filename Normalization**: Cleans chaotic filenames by replacing special characters, collapsing whitespace, and enforcing consistent casing.
- **Large File Extraction**: Scans and segregates files exceeding a user-defined size threshold into a dedicated directory.
- **Preview Mechanism**: Generates an execution plan detailing proposed file movements without making disk modifications.
- **Journaled Undo System**: Records all file operations in a persistent action log (`action_log.txt`), enabling single-step or full-state rollbacks.
- **Directory Safety Filters**: Skips version control directories (`.git`), IDE configuration folders (`.idea`), and internal application files to prevent accidental corruption.

## System Architecture

The application follows a modular architecture separating the execution engine, rule processing, and user interfaces.

```text
       CLI Mode                           GUI Mode (Swing)
          │                                      │
          └─────────────────┬────────────────────┘
                            │
                            ▼
                    Sorter (Controller)
                            │
       ┌────────────────────┼────────────────────┐
       ▼                    ▼                    ▼
 FolderScanner         RuleEngine           UndoJournal
 (Traversal &         (Classification)     (State Tracking)
  Safety Checks)            │                    │
       │                    ▼                    ▼
       └──────────────► OrganizePlan ◄───────────┘
                            │
                            ▼
                       File System
```

## Project Structure

```text
src/
 ├── App.java                   // Application entry point; CLI implementation
 ├── GUI.java                   // Main Swing application window and event handling
 ├── Sorter.java                // Core controller bridging logic and UI
 ├── FolderScanner.java         // Recursive directory traversal with safety exclusions
 ├── RuleEngine.java            // Parses rules.txt and computes target destinations
 ├── FileNameNormalizer.java    // String manipulation utility for sanitizing filenames
 ├── UndoJournal.java           // Transaction log manager for rollback support
 ├── OrganizePlan.java          // Data model containing the computed execution plan
 ├── FileMoveCandidate.java     // Data model representing a single file transfer
 └── SforaTests.java            // Test harness
```

## Design Decisions

- **Two-Pass Execution**: The system separates evaluation from execution. `Sorter` first generates an `OrganizePlan` containing `FileMoveCandidate` objects. This allows the GUI and CLI to present a preview before executing any blocking I/O operations.
- **Transaction Logging**: To implement the undo feature, `UndoJournal` writes source and destination paths to a persistent log upon every successful move. During a rollback, it reads the log in reverse order, verifying file existence before attempting restoration.
- **Dependency Minimization**: The project relies exclusively on the standard Java library (`java.io`, `java.nio`, `javax.swing`). This eliminates external dependencies and simplifies the build process.
- **Safe Traversal**: `FolderScanner` maintains static sets of restricted directory names (e.g., `node_modules`, `build`) and verifies paths against the application's working directory to prevent recursive modification of source code.

## Technology Stack

- **Java SE**: Core programming language.
- **Java Swing**: Graphical user interface components.
- **Java NIO**: Non-blocking I/O file operations and path manipulation.
- **java.security.MessageDigest**: SHA-256 implementation for duplicate detection.
- **Collections Framework**: Data structures for tracking execution plans and rules.

## Installation and Execution

The project requires a standard Java Development Kit (JDK). No external build tools are strictly required.

### Compiling from Source

Navigate to the project root and compile the source files into the `bin` directory:

```bash
mkdir bin
javac -d bin src/*.java
```

### Running the Application

Execute the compiled classes. If invoked without standard input redirection, the application defaults to GUI mode.

```bash
java -cp bin App
```

To force the CLI mode, interact with the terminal prompt upon execution.

## Configuration

Rules are defined in `rules.txt` located in the working directory. The syntax supports basic keyword and extension mapping.

```text
# Example Configuration
KEYWORD=invoice, Documents/Financial
EXTENSION=pdf, Documents/PDFs
EXTENSION=log, Misc/Logs
```

The application attempts to reload these rules dynamically when initiated or manually refreshed. If no rule matches, the system falls back to a hardcoded mapping mechanism categorizing standard formats (e.g., audio, images, code).

## License

This project is provided as-is without any warranties. See repository details for specific licensing terms.
