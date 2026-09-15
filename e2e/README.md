# Android end-to-end vulnerability checks

These checks are intentionally separate from `app/src/test`: they need a built
debug APK, an Android emulator, and ADB. The APK uses only its embedded
single-APK model and does not call a host service or heuristic implementation.

## Run

From the repository root:

```powershell
.\scripts\download-model.ps1
.\gradlew.bat :app:assembleDebug
.\e2e\run-vulnerability-tests.ps1
```

The harness starts `MainActivity` with a test question through the training
intent hook, waits for the investigation worker, captures the screen, reads
Logcat, pulls the debug database, and inspects the APK's synthetic crypto key.
Artifacts are written under `e2e/artifacts/` and are ignored by Git.

The embedded model can take several minutes to copy and initialize on a cold
emulator. The harness waits until inference completes and imposes no
investigation timeout.

## Checks

| Check | Card or scenario | Evidence |
| --- | --- | --- |
| Broad question returns all seeded records | LLMX / LLMQ | Logcat contains the unscoped `SELECT` and all three IDs |
| Crafted question changes generated SQL | LLMX / LLMQ | Logcat contains the injected `OR 1=1` SQL |
| Sensitive values are logged | NS2 / RS2 | Logcat contains the prompt, SQL, and rows |
| Local database is readable | NS4 | Pulled SQLite file has the `transactions` table and rows |
| Sensitive result is capturable | PC2 | A PNG screenshot is created while the result is visible |
| Synthetic key is shipped in the APK | CRM2 | APK bytes contain `PwnedNextKey` |
| Exported IPC exposes transaction rows | PC5 / PC6 / PC7 / AA9 | `content query` reaches the unprotected provider with an injected `1=1` selection |
| Exported components are discoverable | PC6 / AA9 / RS5 | `dumpsys package` lists the exported receiver and providers |

The checks are deliberately evidence-oriented. They do not claim that the
behavior is safe; they prove that the training vulnerabilities are still
reachable after a refactor.
