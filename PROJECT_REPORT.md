# Academic Project Report: Smart File Organizer (SFORA)

## 1. Abstract
The **Smart File Organizer with Rule-Based Automation (SFORA)** is a robust filesystem utility designed to tackle digital clutter. Recognizing the disorganization naturally occurring in typical user workspaces (such as typical `Downloads` directories), this application employs intelligent mapping and hashing routines to categorize, rename, and structurally distribute files reliably.

## 2. Architecture & Design Choices
Rather than building an over-engineered enterprise architecture bloated with interfaces and deep package hierarchies, I deliberately designed SFORA to prioritize **speed, file I/O efficiency, and readability**. This aligns well with standard agile development focused deeply on solving the problem dynamically using merely four core Java `.java` files:

1.  **`SforaApp.java`**: The main interface serving via console CLI interactions.
2.  **`FileOrganizer.java`**: The centralized core engine handling file mapping, IO streaming, the rule engine interpretations, and cryptographic checksums using native libraries.
3.  **`Rule.java`**: A plain old Java object defining custom user sorting criteria.
4.  **`ActionLog.java`**: A specific tracking object granting robust "Undo" (Command/Memento equivalent mapping) features protecting the system against unintended user errors.

## 3. Notable Implementations & Technical Challenges

### A. Cryptographic Duplicate Detection
Filenames are unreliable uniqueness indicators (`data_final.pdf` vs `data_final (1).pdf`). SFORA uses `java.security.MessageDigest` executing an **SHA-256 binary validation flow** tracking unique byte structures. If streams match exactly within the target cache, the algorithm migrates the file seamlessly to an isolated `Duplicates/` folder instead of permanently overwriting the original.

### B. Recursive Directory Cleansing
File migrations typically result in "dead paths" — directories containing nothing but other empty directories. At execution culmination, SFORA provides a recursive cleanup procedure purging empty leftover roots explicitly mapped exclusively inside the execution boundaries yielding an utterly pristine workspace.

### C. OS Interaction Resilience
Operating systems universally handle naming exceptions severely. Before allocating any file, the core script forces regex-based (`[^a-zA-Z0-9_.-]`) sanitization substituting empty/null spaces with underscores eliminating breaking execution behaviors.

## 4. Conclusion
Executing SFORA significantly removes manual classification labor executing multi-GB workloads asynchronously in milliseconds. Utilizing standard core Java packages inherently avoids complex deployment topologies maximizing its reliability and inherent project scale portability.
