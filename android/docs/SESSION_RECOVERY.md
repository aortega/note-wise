# Session Recovery Checklist

Use this checklist when a new agent session starts or chat context is missing.

## Read Order (Required)

1. `android/docs/AGENT_PROGRESS.md`
- Confirm active milestone and gate status.
- Continue from `Current task` and `Next task`.

2. `android/docs/AGENT_CHANGELOG.md`
- Review latest entries for modified files and verification evidence.

3. `android/docs/AGENT_ISSUES.md`
- Check unresolved or recently resolved issues to avoid regressions.

4. `android/docs/AGENT_DECISIONS.md`
- Reuse prior implementation decisions instead of re-litigating architecture.

5. `android/docs/AGENT_ARCHITECTURE.md`
- Confirm current boundaries, package responsibilities, and known deviations.

## Optional Accelerator

- `/memories/repo/notewise-m8-session-handoff.md`
- `/memories/repo/notewise-android-notes.md`

If `/memories` is unavailable, continue using only `android/docs/AGENT_*.md` files.

## Recovery Validation Command

Run in `android/NoteWise` after reading state:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

If this fails, create/update an `ISSUE-###` entry before changing milestone status.

## Continuation Rules

- Keep exactly one milestone `IN_PROGRESS` at a time.
- Do not mark a milestone `DONE` until all gate checks pass.
- Log every substantial change in changelog/issues/decisions/architecture.
