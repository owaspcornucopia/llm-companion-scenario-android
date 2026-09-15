# Architecture and threat boundaries

The rendered diagrams are also embedded in the repository
[README](../README.md):

* [Data-flow diagram with trust boundaries](diagrams/data-flow.svg)
* [Mobile investigation sequence diagram](diagrams/sequence.svg)

```mermaid
sequenceDiagram
    actor Tester
    participant UI as Android UI
    participant Model as Embedded llama.cpp model
    participant Parser as SQL tool parser
    participant DB as Local SQLite
    participant Decision as Fraud decision
    participant IPC as Exported Android components
    participant Clipboard as System clipboard
    participant Prefs as Plain SharedPreferences

    Tester->>UI: Ask whether a transaction is fraudulent
    UI->>Model: Untrusted prompt
    Model-->>UI: On-device generated SQL
    UI->>Parser: Extract sql field or raw SELECT
    Parser->>DB: Execute generated SQL without binding
    DB-->>Decision: Transaction rows
    Decision-->>UI: Verdict, SQL, and rows
    IPC->>UI: Untrusted question and approval extras
    UI->>Clipboard: Full result without sensitive flag
    UI->>Prefs: Last result and fraud override
```

This diagram is intentionally a threat map as well as an architecture sketch.
Every arrow is a place where the lab lets untrusted data cross a boundary:

* the tester controls the natural-language prompt;
* the parser accepts several permissive response formats;
* SQLite receives model-generated SQL directly; and
* the UI, clipboard, preferences, exported components.
