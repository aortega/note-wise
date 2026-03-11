# Agent Decisions Log

Capture architecture and process decisions made during autonomous implementation.

## DEC-027 - Share Rendered-Bounds Row Spacing Refinement Across Runtime And Visual Tests
- Date: 2026-03-10
- Milestone: M10
- Context: Inter-row vertical spacing was inconsistent because `VFLineBreaker` estimated
  staff content extents from note/stem/accidental heuristics and did not account for all
  rendered staff-owned primitives. The visual harness had begun to accumulate debug glyph
  bounds, but runtime and test paths still diverged.
- Decision: Introduce `VFRenderedRowSpacingRefiner` in main source and wire both
  `MultiStaveSheetMusicView` and `VisualRenderHarness` through the same rendered-bounds
  refinement pass. Extend `VexRenderingContext` debug bounds collection from SMuFL glyphs
  only to general draw primitives (`stroke`, `fill`, `fillRect`, `strokeRect`).
- Alternatives considered: (1) Keep heuristic spacing only — too inconsistent for staff
  gap debugging. (2) Implement a full bitmap pixel-scan pass — more expensive and harder
  to reuse in runtime layout.
- Consequences: Runtime and visual tests now share one vertical row-spacing refinement path.
  The change is explicitly experimental; focused `01a` visual diff worsened to `70.12%`, so
  the refiner may still require tuning or rollback.

## DEC-026 - Adopt Two-Phase Visual Conformance Strategy (alphaTab → LilyPond)
- Date: 2026-03-08
- Milestone: M10
- Context: Direct pixel comparison against LilyPond reference images is unrealistic
  because LilyPond uses a fundamentally different spacing algorithm (compact, ~10
  measures/row at 635px) vs NoteWise (~6) and alphaTab (~4). The quality gap is too
  large for a pixel-diff gate to be meaningful.
- Decision: Two-phase plan.
  Phase 1 — Match alphaTab: use alphaTab renders as the NoteWise golden reference.
  Comparison: YIQ perceptual pixel-diff ≤1% (mirrors alphaTab's VisualTestHelper/PixelMatch
  algorithm: threshold=0.3, includeAA=false, diffMask=true). On fail: save *.new.png
  + *.diff.png. Human-driven golden promotion via approval_manifest.json.
  Phase 2 — Improve toward LilyPond/MuseScore: once Phase 1 passes for all Tier-1
  fixtures, tune note-spacing constants (spring-rod minDurationWidth, stretchForce)
  to approach LilyPond's professional engraving density.
- Artifacts:
  - alphaTab test suite: `android/reference/alphaTab-develop/packages/alphatab/test/visualTests/features/LilyPondMusicXML.test.ts`
  - 3-way review panels: LilyPond | alphaTab | NoteWise (LilyPond is quality compass only)
  - Density observation: LilyPond ~10 bars/row, alphaTab 4 bars/row, NoteWise 6 bars/row (all at 635px)
- Alternatives considered: (1) Match LilyPond directly — unrealistic given algorithm
  gap. (2) Self-consistency only (NoteWise matches its own previous renders) — too
  easy, provides no quality floor.
- Consequences: M10 Phase 1 gate = all Tier-1 fixtures ≤1% diff against alphaTab
  golden. Phase 2 is a separate quality improvement phase after M10 closes.

## DEC-025 - Reserve Clef Injection Width In relayoutRow Budget Only
- Date: 2026-03-08
- Milestone: M10
- Context: `relayoutRow` injects a clef into the first measure of each non-first row (`if (index == 0 && relaidStave.clef == null)`). The clef is added AFTER the source `minWidths` are computed, so the first measure's allocated budget excluded the clef overhead, causing notes to overflow the right barline in measures 7, 13, 18, 23.
- Decision: Compute `clefInjectionExtra` (scaled glyph bbox width + one staff spacing boundary) in `relayoutRow` only, adding it to `minWidths[0]` before proportional expansion. Deliberately do NOT propagate this correction to the packing loop in `layout()`, so the 6-measures-per-row decision is preserved while the per-measure width budget within `relayoutRow` is accurate.
- Alternatives considered: (1) Add clef overhead in `layout()` too — would cause the packer to fit fewer measures per row, regressing the improved packing density. (2) Move clef injection into the parsing/source-stave preparation stage — would require significant structural changes to data flow.
- Consequences: Notes in row-head first measures stay within barlines. Row count remains at 5 (matching LilyPond). If the clef injection policy changes, `relayoutRow` must be updated alongside it.

## DEC-024 - Mirror Four-Quarter Formatter Formula In Estimator Fast Path
- Date: 2026-03-08
- Milestone: M10
- Context: `estimateStaffNoteAreaWidth` used `minTickGap = 10f` between consecutive tick contexts (3 gaps × 10px = 30px overhead for 4-note measures). `VFFormatter.applyFourQuarterGridIfApplicable` sets `separation = gap.coerceAtLeast(0f)` — minimum 0 — so the formatter can pack notes with zero inter-note gap. The estimator was therefore systematically over-conservative for 4-quarter measures.
- Decision: Detect `contexts.size == 4 && contexts.all { it.getMaxDuration() == VFFraction.of(1,4) }` and return `signatureGap + totalNoteVisualWidth + rightSafety` where `totalNoteVisualWidth = sum(rightPx*2) + sum((leftPx−rightPx).coerceAtLeast(0))`, exactly matching the formatter. Fall through to the existing chain formula for all other patterns.
- Alternatives considered: (1) Lower `minTickGap` globally — would under-estimate dense non-quarter patterns. (2) Eliminate the gap requirement entirely — would cause over-packing for irregular rhythms.
- Consequences: Row packing for 4/4 measures with 4 quarter notes now matches what the formatter actually produces, enabling 5-row layout that matches LilyPond's reference. Other rhythmic patterns continue using the conservative chain formula.

## DEC-023 - Use 0.1 Staff-Spaces As Accidental-To-Notehead Gap
- Date: 2026-03-08
- Milestone: M10
- Context: `noteGap = spacing * 0.5f` was chosen in the previous session as a "staff-space-relative" value, but 0.5 staff spaces (~3.5px at spacing=7) is 3-4x larger than standard music engraving practice. LilyPond uses approximately 0.1-0.15 staff spaces between the notehead left edge and the nearest accidental column right edge.
- Decision: Change `noteGap` to `spacing * 0.1f` in both `accidentalSpanPx()` and `accidentalColumnCenters()`. This matches standard engraving and reduces the per-accidental note-area estimate by ~11px, which contributes to improved row packing density.
- Alternatives considered: (1) Keep 0.5 — visually too much separation. (2) Use 0.0 — accidentals would touch notehead edge with no buffer.
- Consequences: Accidentals are visually tighter to noteheads, row packing density improves, and the value is closer to professional engraving standards. If the note gap needs adjustment for other clef/font combinations, this constant should be revisited.

## DEC-022 - Require Multi-Clef Evidence Before Virtual Grand-Staff Inference
- Date: 2026-03-08
- Milestone: M10
- Context: `01a-Pitches-Pitches.xml` (single staff, wide register) was incorrectly rendered with inferred two-staff layout when no explicit staff tags were present.
- Decision: In `MusicSheetToVF.buildStaffResolver`, allow pitch-range-based virtual staff inference only when measure attributes contain multi-staff clef declarations (`clefByStaff` has staff index > 1).
- Alternatives considered: keep range-only inference (causes false splits); disable inference entirely (regresses clef-driven two-staff files lacking explicit staff tags).
- Consequences: Single-staff pitch exercises remain on one stave, while clef-declared multi-staff pieces still get inferred split behavior when needed.

## DEC-001 - Initialize Autonomous Tracking Files
- Date: 2026-03-07
- Milestone: M0
- Context: Autonomous workflow requires durable logs for progress, changelog, issues, and decisions.
- Decision: Create four tracker files under android/docs and seed each with a template.
- Alternatives considered: Keep notes only in chat output.
- Consequences: Improves traceability and enables uninterrupted agent execution.

## Template

### DEC-### - Title
- Date: YYYY-MM-DD
- Milestone: Mx
- Context: ...
- Decision: ...
- Alternatives considered: ...
- Consequences: ...

## DEC-002 - Use NoteWise As Active Milestone Target
- Date: 2026-03-07
- Milestone: M0
- Context: Original Android renderer source was unavailable; user requested new app setup and continuation.
- Decision: Execute TrackPlay milestones on `android/NoteWise` as the active Android app module.
- Alternatives considered: Continue in `android/Renderer` despite missing implementation baseline.
- Consequences: Milestone work proceeds without dependency on lost legacy sources.

## DEC-003 - Prefer Absolute Paths For All Terminal Actions
- Date: 2026-03-07
- Milestone: M0
- Context: Terminal session intermittently lost valid CWD, causing false command failures.
- Decision: Use absolute paths for file operations and build invocation where possible.
- Alternatives considered: Retry relative-path commands repeatedly.
- Consequences: Improved reliability and reduced interruption risk during autonomous execution.

## DEC-004 - Pin Gradle JVM To Android Studio JBR
- Date: 2026-03-07
- Milestone: M0
- Context: CLI Gradle used Java 16 by default while AGP required 17+.
- Decision: Set `org.gradle.java.home` in `android/NoteWise/gradle.properties` to Android Studio bundled JBR path.
- Alternatives considered: Install and globally switch system JDK; rely on IDE-only builds.
- Consequences: CLI build/test verification now matches IDE runtime and passes consistently.

## DEC-005 - Enforce Living Architecture Log With Glossary
- Date: 2026-03-07
- Milestone: M2
- Context: Progress/decisions were tracked, but architecture context and shared terminology were not centralized.
- Decision: Introduce `android/docs/AGENT_ARCHITECTURE.md` and require updates alongside progress/changelog/issues/decisions logs.
- Alternatives considered: Keep architecture notes only in AGENT_DECISIONS or ad hoc chat notes.
- Consequences: Architecture intent, concept glossary, and decision linkage remain synchronized during autonomous delivery.

## DEC-006 - Use Minimal Forward-Compatibility Shims For M2 Compile Integrity
- Date: 2026-03-07
- Milestone: M2
- Context: M2 spec types reference classes formally scheduled for later milestones (`VFTickContext`, barline/clef/key/time signature classes).
- Decision: Add minimal compile-safe shims now and keep full feature implementations deferred to their milestone scope.
- Alternatives considered: Change M2 signatures away from spec; skip compile until later milestones.
- Consequences: M2 remains spec-aligned and buildable while preserving milestone order for full implementations.

## DEC-007 - Add Temporary Smoke Render Surface For M2 Visual Gate
- Date: 2026-03-07
- Milestone: M2
- Context: Main app UI did not yet host notation rendering, but M2 requires on-device visual verification.
- Decision: Add a minimal `RendererSmokeView` and route it through `activity_main.xml` to render one stave and one note for validation.
- Alternatives considered: Defer visual validation to later milestone UI work; rely only on automated tests.
- Consequences: M2 visual gate is verifiable now with low implementation overhead; view can be removed or replaced in M3+ UI integration.

## DEC-008 - Defer Zoom/Reflow To M11 Scope
- Date: 2026-03-07
- Milestone: M2
- Context: Temporary zoom transform in smoke view caused rendering regression; implementation spec places pinch-to-zoom under M11 polish/UX.
- Decision: Keep M2 smoke view unzoomed and defer zoom/reflow/repagination work to M11 implementation.
- Alternatives considered: Keep ad hoc zoom in M2 smoke view; add partial zoom now without layout reflow.
- Consequences: M2 remains stable and scope-correct; zoom implementation will be done with proper layout/interaction architecture in M11.

## DEC-009 - Use 32nd-Tick Context Grouping With Two-Pass Spacing
- Date: 2026-03-07
- Milestone: M4
- Context: Formatter must align notes from multiple voices at shared beat positions while preventing notehead overlap.
- Decision: Group notes by cumulative 32nd-note tick IDs (`resolution=32`) and assign x with proportional weighting, then enforce minimum spacing in a second pass by rightward pushes only.
- Alternatives considered: Purely equal spacing; spring-mass relaxation in M4.
- Consequences: Delivers deterministic multi-voice beat alignment now, with overlap protection; total width may exceed justification under dense passages until later advanced layout milestones.

## DEC-010 - Implement Hard-Beam Geometry For M5
- Date: 2026-03-07
- Milestone: M5
- Context: M5 requires beams with slope and secondary levels, but full soft-beam optimization is unnecessary for gate quality.
- Decision: Use deterministic hard-beam placement (all stem tips forced to beam line), clamped slope, and run-based secondary beam drawing; keep tie/slur as cubic stroke/fill bezier implementations.
- Alternatives considered: alphaTab-style soft-beam fitting and advanced partial tie/slur continuation logic in M5.
- Consequences: Achieves stable M5 rendering and tests quickly; finer engraving polish can be layered in later milestones without API churn.

## DEC-011 - Promote Beam Geometry To Engraving Options
- Date: 2026-03-07
- Milestone: M5
- Context: Beam thickness was hardcoded in `VFMetrics`, making style tuning difficult and causing user-visible mismatch for mixed-pitch sequences.
- Decision: Introduce `VFEngravingOptions` and thread beam thickness/spacing/max-slope through `VFStaveOptions`, with `VFBeam` reading values from the active stave.
- Alternatives considered: keep fixed `VFMetrics` constants or pass per-beam overrides only.
- Consequences: Beam styling is now explicitly part of engraving configuration and can be tuned per stave/system without API breakage.

## DEC-012 - Enforce X-Ordered Beaming Geometry Before Note Draw
- Date: 2026-03-07
- Milestone: M5
- Context: Mixed-pitch beamed groups showed incorrect visual slope/connection when note processing order differed from rendered x-order, and flags could appear if beaming state was applied after notes were drawn.
- Decision: Build beam geometry from notes sorted by x-position, derive beam line from leftmost/rightmost stem tips, and instantiate beam grouping before `VFVoice.draw()` so note-local flag drawing is suppressed for beamed notes.
- Alternatives considered: preserve insertion-order processing and post-note beam instantiation.
- Consequences: Beam shape follows rendered direction and middle-note stems dynamically connect to the same beam line while avoiding disconnected/flag artifacts.

## DEC-013 - Use Shared Notehead-Based Stem Anchor For Beam Stems
- Date: 2026-03-07
- Milestone: M5
- Context: Beam stems used a hardcoded x-offset derived from font scale, which diverged from notehead glyph geometry and caused visible stem/notehead horizontal disconnect.
- Decision: Expose stem-anchor x computation in `VFStaveNote` and have `VFBeam` reuse it so standalone and beamed stems attach with identical notehead geometry.
- Alternatives considered: keep beam-local x offset constants or tune magic multipliers by visual trial.
- Consequences: Stem anchoring is consistent across rendering paths, reducing x-offset regressions and making future notehead metric updates propagate automatically.

## DEC-014 - Compose Host Via AndroidView Bridge
- Date: 2026-03-07
- Milestone: M6
- Context: M6 requires Compose integration while existing notation rendering is implemented as a custom Android `View` pipeline.
- Decision: Keep `SheetMusicView` as the rendering engine host and wrap it in `SheetMusicComposable` (`AndroidView`) while moving app entry to Compose (`ComponentActivity` + `setContent`).
- Alternatives considered: rewrite renderer directly in Compose Canvas for M6; keep XML host and delay Compose migration.
- Consequences: Meets M6 gate quickly with minimal rendering risk and preserves compatibility with existing Canvas-based VexFlow port.

## DEC-015 - Enable JaCoCo No-Location Classes For Robolectric Coverage
- Date: 2026-03-07
- Milestone: M7
- Context: Core coverage appeared far below target because Robolectric-executed rendering paths were not attributed by JaCoCo in default unit-test configuration.
- Decision: Configure `JacocoTaskExtension` on unit-test tasks with `isIncludeNoLocationClasses = true` and exclude `jdk.internal.*` so Robolectric-loaded classes contribute to coverage reports.
- Alternatives considered: rely only on plain JVM tests for coverage; exclude rendering context classes from report scope.
- Consequences: Coverage now accurately reflects tested rendering behavior and satisfies the M7 core package threshold without narrowing scope.

## DEC-016 - Normalize MusicXML Durations In Quarter Units
- Date: 2026-03-07
- Milestone: M8
- Context: MusicXML `duration/divisions` values are quarter-note based, while VexFlow duration tokens are symbolic (`1`, `2`, `4`, `8`, dotted variants).
- Decision: Convert MusicXML duration ratios to quarter-note units first, then map to VexFlow durations with dotted thresholds (`4.0->1`, `2.0->2`, `1.0->4`, `0.5->8`, etc.).
- Alternatives considered: treat `duration/divisions` as whole-note ratios; hardcode fixed assumptions for only quarter/eighth values.
- Consequences: Parser/converter output matches MusicXML timing semantics and avoids systemic note-length regressions in rendered output.

## DEC-017 - Use AGENT Docs As Canonical Session Recovery Source
- Date: 2026-03-07
- Milestone: M9
- Context: New sessions may not have `/memories` context available, causing continuity risk.
- Decision: Add `android/docs/SESSION_RECOVERY.md` and require recovery from `AGENT_PROGRESS/CHANGELOG/ISSUES/DECISIONS/ARCHITECTURE` in that order, with `/memories/repo` as optional accelerator only.
- Alternatives considered: rely on `/memories` alone; rely on ad hoc chat summaries.
- Consequences: Recovery remains deterministic and repository-local even when external memory persistence is unavailable.

## DEC-018 - Keep SAF Import Surface In NoteWise App Layer
- Date: 2026-03-07
- Milestone: M9
- Context: M9 requires Android SAF integration while parser logic is in `dev.pola.vexflow.parser`.
- Decision: Implement URI import in `dev.pola.notewise.renderer.FileImportHandler` and keep parsing delegated to `MusicXMLParser`, with state handling in `RendererViewModel` and picker UX in `HomeScreen`.
- Alternatives considered: put Android URI handling in parser package; parse directly inside composables.
- Consequences: Android-specific file IO stays in app-layer code while parser package remains platform-agnostic and testable.

## DEC-019 - Close M9 With Sample-Backed Runtime Import Tests
- Date: 2026-03-07
- Milestone: M9
- Context: Manual SAF picker interaction is valuable but not always reproducible in headless CI/tool sessions; M9 still requires confidence for `.xml`/`.mxl` import correctness.
- Decision: Treat M9 as complete when both parser and file-import-handler integration tests pass against real repository samples (`01a-Pitches-Pitches.xml`, `90a-Compressed-MusicXML.mxl`) and full unit-test/build gates are green.
- Alternatives considered: require only manual device picker validation; rely solely on parser unit tests without URI/import-handler coverage.
- Consequences: M9 closure remains evidence-driven and repeatable in automation while preserving end-to-end import-path confidence.

## DEC-020 - Rebuild Staves Per System Row In M10 Line Breaking
- Date: 2026-03-07
- Milestone: M10
- Context: `VFStave` uses immutable constructor coordinates (`x`, `y`, `width`), while M10 requires packing many measures per row with adjusted widths and non-overlapping x positions.
- Decision: Implement `VFLineBreaker` as a two-phase layout: greedy row packing via estimated minimum widths, then row relayout by cloning each measure's stave with new `x/y/width` and preserving voices/beams/ties references for formatter-driven redraw.
- Alternatives considered: canvas-translation-only placement (insufficient because original measure widths remain too large); mutating existing staves (not supported by immutable properties).
- Consequences: Enables deterministic multi-row layout without changing existing core note/voice APIs; future M11 zoom/reflow can reuse the same relayout pipeline.

## DEC-021 - Use Manifest-Driven Progressive Approval For LilyPond Tier-1 Goldens
- Date: 2026-03-07
- Milestone: M10
- Context: Full-batch golden updates can hide fixture-specific rendering defects and make manual review/error triage difficult when Tier-1 output is unstable.
- Decision: Add fixture-scoped execution (`LILYPOND_FIXTURES`) and a manifest-driven CLI workflow (`init/next/status/approve/reject/reset`) so each fixture is rendered, reviewed, and promoted independently.
- Alternatives considered: Continue all-fixtures batch update mode only; rely on ad hoc manual file copying without workflow state.
- Consequences: Review state is explicit and resumable, promotion is deterministic, and regression baselines can be curated incrementally while rendering correctness is still improving.
