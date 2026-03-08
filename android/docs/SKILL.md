---
name: trackplay-autonomous-delivery
description: "Autonomous implementation workflow for TrackPlay Android milestones M0-M20. Use when an agent must execute end-to-end without human interaction, enforce milestone gates, resolve problems independently, and keep strict progress/changelog/issue logs."
---

# TrackPlay Autonomous Delivery

## Purpose

Execute the Android TrackPlay roadmap from `android/ANDROID_PROJECT_PLAN.md` and
`android/IMPLEMENTATION_SPEC.md` without requiring human guidance during normal
development.

Primary outcomes:
- Deliver milestone-by-milestone implementation from M0 to M20.
- Keep build and tests green at every gate.
- Resolve blockers autonomously with documented decisions.
- Maintain clear logs for progress, changes, and incidents.

## Source Of Truth

1. `android/IMPLEMENTATION_SPEC.md` is the execution contract.
2. `android/ANDROID_PROJECT_PLAN.md` defines architecture, dependencies, and risks.
3. If they conflict on detail, follow `android/IMPLEMENTATION_SPEC.md` for
   implementation behavior and gate criteria.
4. Do not invent milestone scope beyond M0-M20 unless the spec explicitly allows
   optional work (for example M14).

## Non-Negotiable Rules

- Work milestones in order. Never skip ahead.
- Do not mark a milestone gate complete unless all gate checks pass.
- Keep the branch releasable after each completed milestone.
- Never use destructive git operations.
- Avoid placeholder-only implementations for required milestone exits.
- When uncertain, choose the simplest solution that passes gate tests and
  matches reference behavior.

## Directory And Logging Contract

Create and maintain these files if missing:

- `android/docs/AGENT_PROGRESS.md`
- `android/docs/AGENT_CHANGELOG.md`
- `android/docs/AGENT_ISSUES.md`
- `android/docs/AGENT_DECISIONS.md`
- `android/docs/AGENT_ARCHITECTURE.md`
- `android/docs/SESSION_RECOVERY.md`

Update them continuously while working.

## Session Recovery Protocol

At the start of a new session, recover context in this order:

1. `android/docs/SESSION_RECOVERY.md`
2. `android/docs/AGENT_PROGRESS.md`
3. `android/docs/AGENT_CHANGELOG.md`
4. `android/docs/AGENT_ISSUES.md`
5. `android/docs/AGENT_DECISIONS.md`
6. `android/docs/AGENT_ARCHITECTURE.md`

Notes:
- `android/docs/AGENT_*.md` files are the canonical persistent recovery source.
- `/memories/repo/*.md` can accelerate recovery but is optional and must not be required.
- If recovery data appears inconsistent, trust `AGENT_PROGRESS.md` for milestone status and verify with project tests/build before advancing milestones.

## Execution Loop

Repeat this loop for each milestone `Mx`:

1. Run session recovery protocol and verify current milestone state.
2. Read milestone section `Mx` and its gate criteria in
   `android/IMPLEMENTATION_SPEC.md`.
3. Identify required files, tests, and commands.
4. Implement the smallest complete vertical slice needed for gate passage.
5. Run required tests and build checks.
6. Fix failures until green.
7. Update progress, changelog, issues, decisions, architecture, and recovery docs.
8. Mark `Mx` done only after all checks pass.
9. Proceed to `Mx+1`.

## Standard Command Set

Prefer project-local commands from `android/NoteWise/`:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew test --tests "*ClassNameTest"
./gradlew lint
./gradlew assembleRelease
```

If command names differ in the local project, discover equivalents and record
them in `android/docs/AGENT_DECISIONS.md`.

## Milestone Gate Enforcement

For each milestone gate:

- Validate all checklist items in the spec.
- Validate compile status.
- Validate test status.
- Validate manual visual behavior when the gate requires it.

If any gate criterion fails, milestone remains `IN_PROGRESS`.

## Autonomous Problem Resolution

When blocked, follow this sequence without asking a human unless safety or data
loss risk exists.

1. Reproduce and isolate
- Capture failing command, stack trace, and scope.
- Reduce to a minimal failing unit (single class/test if possible).

2. Classify
- `BUILD` (compile, dependency, Gradle)
- `TEST` (logic mismatch, assertion failure)
- `RUNTIME` (crash, state bug)
- `PERF` (frame time, latency)
- `SPEC_GAP` (missing or ambiguous requirement)

3. Resolve by priority
- Fix local code defect first.
- If API mismatch, adapt to project conventions while preserving spec intent.
- If dependency issue, pin stable version compatible with current toolchain.
- If ambiguous spec, choose conservative behavior consistent with gates and
  document decision.

4. Retry policy
- Attempt up to 3 distinct fixes for same root cause.
- After each attempt, rerun the smallest relevant verification command first,
  then full gate checks.

5. Escalation without human
- If unresolved after 3 attempts, create a fallback implementation that keeps
  architecture clean and preserves forward compatibility.
- Mark issue as `DEFERRED_WITH_MITIGATION` in `AGENT_ISSUES.md`.
- Record exact tradeoff and follow-up action in `AGENT_DECISIONS.md`.
- Continue with next unblocked tasks only if doing so does not violate
  dependency order or gate integrity.

## Change Logging Rules

For every merged logical change set, append to `android/docs/AGENT_CHANGELOG.md`:

- Date/time (ISO 8601)
- Milestone
- Files changed
- Behavioral impact
- Tests run and results
- Risk level (`LOW`, `MEDIUM`, `HIGH`)
- Rollback note

Template:

```md
## YYYY-MM-DDTHH:MM:SSZ - Mx - Short Title
- Files: path/a, path/b
- Behavior: what changed for users/developers
- Verification: command -> result
- Risk: LOW|MEDIUM|HIGH
- Rollback: how to revert safely
```

## Progress Tracking Rules

Maintain `android/docs/AGENT_PROGRESS.md` with one active milestone at a time.

Required fields per milestone:
- Status: `NOT_STARTED`, `IN_PROGRESS`, `BLOCKED`, `DONE`
- Percent complete (0-100)
- Current task
- Next task
- Gate checklist status
- Last verification run

Template:

```md
# Autonomous Progress Dashboard

## Current Focus
- Milestone: Mx
- Status: IN_PROGRESS
- Percent: 0
- Current task: ...
- Next task: ...
- Last updated: YYYY-MM-DDTHH:MM:SSZ

## Milestone Board
- M0: DONE
- M1: IN_PROGRESS
- M2: NOT_STARTED
```

## Issue Tracking Rules

Record all non-trivial defects and blockers in `android/docs/AGENT_ISSUES.md`.

Severity levels:
- `S1` blocks milestone completion
- `S2` degrades correctness/performance but has workaround
- `S3` minor or cosmetic

Template:

```md
## ISSUE-### - Short Title
- Status: OPEN|IN_PROGRESS|RESOLVED|DEFERRED_WITH_MITIGATION
- Severity: S1|S2|S3
- Milestone: Mx
- Type: BUILD|TEST|RUNTIME|PERF|SPEC_GAP
- Symptoms: ...
- Root cause: ...
- Fix: ...
- Verification: ...
- Follow-up: ...
```

## Decision Log Rules

Use `android/docs/AGENT_DECISIONS.md` for architecture or spec-interpretation
choices.

Template:

```md
## DEC-### - Title
- Date: YYYY-MM-DD
- Milestone: Mx
- Context: ...
- Decision: ...
- Alternatives considered: ...
- Consequences: ...

## Architecture Log Rules

Use `android/docs/AGENT_ARCHITECTURE.md` as the living architecture source.

Required updates when architecture changes:
- Current architecture snapshot by milestone state.
- Component/package responsibility updates.
- Decision index references to relevant `DEC-###` entries.
- Deviations from plan/spec and rationale.
- Glossary updates for newly introduced terms.

Minimum sections to maintain:
- Purpose
- Current Architecture Snapshot
- Milestone-to-Architecture Map
- Decision Index
- Known Deviations
- Glossary
```

## Quality Bar

Before closing any milestone:
- No failing required tests.
- No compile errors.
- No unresolved S1 issues.
- All new public APIs have unit tests.
- Any temporary workaround is logged with exit criteria.

## Completion Criteria

Phase completion requires:
- All milestone gates in that phase checked.
- Progress dashboard updated.
- Changelog current.
- Issues and decisions logs reconciled.

Project completion (M20) requires:
- Final `assembleRelease` success.
- End-to-end scenario checks as defined by the plan/spec.
- Final summary entry in all four tracking files.

## Agent Behavior Style

- Be deterministic, concise, and evidence-driven.
- Prefer measurable outcomes over narrative updates.
- Always attach verification evidence to claims.
- Continue autonomously unless a destructive or privacy-sensitive action is
  required.
