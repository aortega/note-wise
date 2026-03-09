# Agent Issues Register

Track defects, blockers, and mitigations discovered during autonomous delivery.

## ISSUE-013 - Post-Fix MusicXML Visual Regression (Status: Broken)
- Status: OPEN
- Severity: S1
- Milestone: M10
- Type: TEST
- Symptoms: After applying opening-padding/time-signature alignment changes, targeted
	fixtures `01a/01b/01c` all fail visual conformance against current approved goldens.
	Latest diffs: `01a=64.90%`, `01b=64.52%`, `01c=70.40%`.
- Root cause: Recent horizontal-layout calibration moved geometry in the intended
	direction for 01c width, but current aggregate placement remains incompatible with
	the currently approved musicxml-suite baseline.
- Fix: IN_PROGRESS. Keep the new diagnostic changes, review post-fix renders,
	and either (a) continue calibration toward visual convergence, or (b) rollback if
	convergence is not reached quickly.
- Verification:
	`MUSICXML_SUITE_FIXTURES="01a-Pitches-Pitches.xml,01b-Pitches-Intervals.xml,01c-Pitches-NoVoiceElement.xml" ./gradlew :app:testDebugUnitTest --tests dev.pola.notewise.visual.MusicXMLSuiteVisualTest`
	-> FAIL (3/3) with metrics above.
- Follow-up: Do not promote new goldens while ISSUE-013 remains open; use review PNGs in
	`app/build/reports/visual-tests/lilypond/review/` for sign-off.

## ISSUE-012 - Wide Single-Staff Scores Misclassified As Virtual Grand Staff
- Status: RESOLVED
- Severity: S2
- Milestone: M10
- Type: RUNTIME
- Symptoms: `01a-Pitches-Pitches.xml` rendered with inferred multi-staff layout despite no explicit staff tags and only staff-1 clef declarations.
- Root cause: `MusicSheetToVF.buildStaffResolver` auto-split by pitch range without requiring multi-staff clef/staff declarations.
- Fix: Gate virtual grand-staff inference behind multi-staff clef declarations (`clefByStaff` containing staff numbers > 1); keep single-staff fallback when absent.
- Verification: `:app:testDebugUnitTest --tests "*MusicSheetToVFTest"` -> SUCCESS (new regression test added); fixture run with `LILYPOND_DEBUG_LAYOUT=true` shows `staff=1` for all rendered 01a measures.
- Follow-up: Continue 01a visual parity adjustments after inference fix (spacing/glyph fidelity) before re-approval.

## ISSUE-001 - Shell CWD Instability During Gradle/Path Commands
- Status: RESOLVED
- Severity: S2
- Milestone: M0
- Type: BUILD
- Symptoms: Intermittent `./gradlew` not found and `getcwd` errors in tool terminal despite files existing.
- Root cause: Tool terminal session occasionally loses valid working directory context.
- Fix: Switched to absolute paths and explicit directory references for all file/build operations.
- Verification: Full M0/M1 build and tests passed using absolute-path Gradle invocation.
- Follow-up: Continue using absolute paths for long command chains.

## ISSUE-002 - Unexpected NoteWise State Regression
- Status: RESOLVED
- Severity: S2
- Milestone: M0
- Type: RUNTIME
- Symptoms: `android/NoteWise` unexpectedly appeared empty in one terminal view while files existed in tool reads.
- Root cause: Inconsistent workspace/terminal view state.
- Fix: Recreated `android/NoteWise` from scratch and restored required project structure.
- Verification: NoteWise root/module files and source/resource directories recreated.
- Follow-up: Keep using absolute paths and verify state with file API before critical edits.

## ISSUE-003 - CLI Build Blocked By JDK/SDK Configuration Drift
- Status: RESOLVED
- Severity: S1
- Milestone: M0
- Type: BUILD
- Symptoms: AGP failed under Java 16; adaptive icon resources failed linking; SDK path pointed to non-existent user path.
- Root cause: Terminal JVM differed from Android Studio JBR; copied `local.properties` contained stale `sdk.dir`; adaptive icon XMLs were placed in non-`v26` folder.
- Fix: Set `org.gradle.java.home` to Android Studio JBR in `gradle.properties`, corrected `local.properties` SDK path, moved icon XMLs to `mipmap-anydpi-v26`.
- Verification: `assembleDebug` and `testDebugUnitTest` succeeded.
- Follow-up: Keep `local.properties` machine-specific and validate icon resource qualifiers when copying templates.

## ISSUE-004 - Forward-Milestone Type Coupling In M2
- Status: RESOLVED
- Severity: S2
- Milestone: M2
- Type: SPEC_GAP
- Symptoms: M2 class signatures reference types scoped to later milestones.
- Root cause: Spec-level coupling between early stave/note classes and later formatter/modifier classes.
- Fix: Added minimal compatibility classes (`VFTickContext`, `VFBarline`, `VFClef`, `VFKeySignature`, `VFTimeSignature`) with explicit deferred behavior.
- Verification: M2 tests and build pass with current signatures; M3/M4 replaced `VFClef`/`VFKeySignature`/`VFTimeSignature` and `VFTickContext`; M5 replaced `VFBarline` with full behavior and added dedicated tests (`VFBarlineTest`).
- Follow-up: None.

## ISSUE-005 - Smoke View White Screen After Zoom Transform
- Status: RESOLVED
- Severity: S2
- Milestone: M2
- Type: RUNTIME
- Symptoms: Validation screen rendered all white after applying a temporary global canvas zoom in `RendererSmokeView`.
- Root cause: Canvas scaling around the view center shifted the staged staff/note scene outside visible bounds in the smoke layout.
- Fix: Removed smoke-view global zoom transform and restored stable scene coordinates; retained notehead centering correction.
- Verification: Device deploy + launch + screenshot artifact (`android/docs/notewise_m2_nozoom_fix.png`) confirmed notation is visible.
- Follow-up: Implement true pinch-to-zoom/reflow only in M11 where it is specified.

## ISSUE-006 - Disconnected Beam Artifact In M5 Mixed-Pitch Group
- Status: RESOLVED
- Severity: S2
- Milestone: M5
- Type: RUNTIME
- Symptoms: Beamed eighth-note smoke sequence showed a disconnected beam/box look and occasional flag artifacts.
- Root cause: Beam processing used non-rendered ordering and beam state was applied too late in draw order, allowing note-local flag/stem drawing before beam grouping took effect.
- Fix: Suppressed note-local stems/flags for beamed notes, created beam grouping before note draw, and generated beam geometry from x-ordered notes using outer stem tips for slope.
- Verification: `:app:testDebugUnitTest --tests "*VFBeamTest" --tests "*VFBarlineTest"` + `:app:assembleDebug` + `:app:installDebug` + launch + screenshot (`android/docs/notewise_m5_beam_order_fix.png`) all passed.
- Follow-up: Revisit partial-beam edge-cases in later engraving-polish milestones.

## ISSUE-007 - Stem/Notehead X-Offset And Mis-Captured Smoke Screenshot
- Status: RESOLVED
- Severity: S2
- Milestone: M5
- Type: RUNTIME
- Symptoms: Beam validation screenshot showed Android home in one run; when app rendered, stems appeared horizontally disconnected from noteheads.
- Root cause: Screenshot workflow lacked a focused-window check before capture; beam stem x-position used a hardcoded font-scale offset that did not match notehead glyph geometry.
- Fix: Added focused-app verification (`dumpsys window` focus check) before screenshot capture and switched `VFBeam` stem anchoring to `VFStaveNote`'s notehead-based stem x computation.
- Verification: `:app:testDebugUnitTest --tests "*VFBeamTest" --tests "*VFBarlineTest"` + `:app:assembleDebug` + `:app:installDebug` + `adb shell am start -W -n dev.pola.notewise/.MainActivity` + focus check + screenshot (`android/docs/notewise_m5_beam_anchor_fix.png`) passed.
- Follow-up: Keep focused-window check in manual visual gates to avoid home-screen captures.

## ISSUE-008 - M7 Coverage Under-Reported For Robolectric-Executed Core Paths
- Status: RESOLVED
- Severity: S1
- Milestone: M7
- Type: TEST
- Symptoms: JaCoCo report showed `dev/pola/vexflow/core` line coverage at `37.37%`, with `VexRenderingContext.kt` reported as `0/104` despite dedicated Robolectric tests passing.
- Root cause: JaCoCo default unit-test configuration did not include no-location classes used by Robolectric class loading.
- Fix: Added `JacocoTaskExtension` configuration on unit-test tasks (`isIncludeNoLocationClasses = true`, exclude `jdk.internal.*`) and retained real canvas-backed `VexRenderingContext` tests.
- Verification: `:app:testDebugUnitTest` + `:app:jacocoTestReport` -> SUCCESS; `dev/pola/vexflow/core` line coverage increased to `175/198 = 88.38%`.
- Follow-up: Keep this JaCoCo setting for subsequent milestones that rely on Robolectric for Android drawing behavior.

## ISSUE-009 - M8 Parser Tests Failed Due To XML Header Escaping And Duration Expectation Drift
- Status: RESOLVED
- Severity: S1
- Milestone: M8
- Type: TEST
- Symptoms: `MusicXMLParserTest` threw `XmlPullParserException` at parser startup and duration assertions failed in parser/converter tests.
- Root cause: XML test helper used escaped quotes inside Kotlin raw strings (`\"`), producing invalid XML declarations; dotted-quarter test used values representing dotted-eighth duration.
- Fix: Corrected raw-string XML declaration/attributes, normalized converter duration mapping to quarter-note units, and fixed dotted-quarter test input to `duration=6, divisions=4`.
- Verification: `:app:testDebugUnitTest --tests "*MusicXMLParserTest" --tests "*MusicSheetToVFTest" :app:assembleDebug` -> SUCCESS.
- Follow-up: Keep parser fixture XML literal format strict and validate musical-duration semantics when adding new tests.

## ISSUE-010 - M10 Refactor Compile Regression From Missing Import
- Status: RESOLVED
- Severity: S2
- Milestone: M10
- Type: BUILD
- Symptoms: `:app:compileDebugKotlin` failed with `Unresolved reference 'VFVoice'` in `RendererViewModel` after M10 score-data model refactor.
- Root cause: Removed `VFVoice` import while changing `ScoreViewData.measures` to `RenderedMeasure`, but `buildDemoScore()` still constructs a `VFVoice`.
- Fix: Restored `dev.pola.vexflow.core.VFVoice` import and reran targeted + full verification commands.
- Verification: `:app:testDebugUnitTest --tests "*VFLineBreakerTest" --tests "*MusicXMLSampleImportIntegrationTest" --tests "*FileImportHandlerTest"` -> SUCCESS; `:app:testDebugUnitTest :app:assembleDebug` -> SUCCESS.
- Follow-up: Re-run `get_errors`/compile checks immediately after cross-file model refactors that alter imports and type aliases.

## ISSUE-011 - LilyPond Complex Fixture Coverage Temporarily Reduced For Reliable Goldens
- Status: OPEN
- Severity: S2
- Milestone: M10
- Type: TEST
- Symptoms: Some LilyPond fixtures (`03b-Rhythm-Backup.xml`, `21a-Chord-Basic.xml`) produced blank renders in the bootstrap visual harness, resulting in misleading transparent/empty goldens.
- Root cause: Current NoteWise parser/layout coverage does not yet fully support specific constructs used by these fixtures under deterministic visual test settings.
- Fix: Switched visual test capture to deterministic direct rendering, enforced non-empty ink guards, and reduced bootstrap Tier-1 baseline set to verified non-empty fixtures (`11a`, `12aa`, `13a`).
- Verification: GrandStaff + LilyPond visual suites pass in update and strict mode with cleaned baseline set.
- Follow-up: Re-introduce excluded fixtures after parser/layout support improvements, then regenerate and re-approve baselines.

## Severity Guide
- S1: Blocks milestone completion.
- S2: Correctness/performance degraded but workaround exists.
- S3: Minor/non-blocking issue.

## Type Guide
- BUILD
- TEST
- RUNTIME
- PERF
- SPEC_GAP

## Template

### ISSUE-### - Short Title
- Status: OPEN|IN_PROGRESS|RESOLVED|DEFERRED_WITH_MITIGATION
- Severity: S1|S2|S3
- Milestone: Mx
- Type: BUILD|TEST|RUNTIME|PERF|SPEC_GAP
- Symptoms: ...
- Root cause: ...
- Fix: ...
- Verification: ...
- Follow-up: ...
