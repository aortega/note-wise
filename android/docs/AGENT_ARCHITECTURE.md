# TrackPlay Android Architecture (Living)

Last updated: 2026-03-10T20:25:00Z
Active app target: `android/NoteWise`

## Purpose
This document captures the current implementation architecture as milestones advance.
It is maintained in lockstep with:
- `android/docs/AGENT_PROGRESS.md`
- `android/docs/AGENT_CHANGELOG.md`
- `android/docs/AGENT_ISSUES.md`
- `android/docs/AGENT_DECISIONS.md`

Use this file for:
- Architecture state by milestone
- Component boundaries and responsibilities
- Known deviations from plan/spec
- Glossary of core concepts

## Source Alignment
- Contract: `android/IMPLEMENTATION_SPEC.md`
- Roadmap and architecture intent: `android/ANDROID_PROJECT_PLAN.md`
- If conflict exists, implementation behavior follows `android/IMPLEMENTATION_SPEC.md`.

## Current Architecture Snapshot
Status: M0-M9 completed; M10 reopened and currently BROKEN during visual-conformance recovery.

### Application Layer
- App module: `android/NoteWise/app`
- Package root: `dev.pola.notewise`
- App singleton context: `android/NoteWise/app/src/main/java/dev/pola/notewise/App.kt`
- Entry activity: `android/NoteWise/app/src/main/java/dev/pola/notewise/MainActivity.kt`
- Temporary manual-validation surface: `android/NoteWise/app/src/main/java/dev/pola/notewise/RendererSmokeView.kt`
- Compose renderer screen host: `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererScreen.kt`
- Compose score state provider: `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererViewModel.kt`

### Compose Integration (M6)
- Android View host for score rendering:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/view/SheetMusicView.kt`
- Compose wrapper via `AndroidView`:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/view/SheetMusicComposable.kt`
- App module Compose enablement/dependencies:
  `android/NoteWise/app/build.gradle.kts`

### Rendering Foundation (M1)
- Fractions/timing math:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFFraction.kt`
- Shared layout constants:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFMetrics.kt`
- SMuFL codepoint table:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFTables.kt`
- Glyph bbox model + asset loader:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFGlyphBoundingBox.kt`
- Canvas rendering context abstraction:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/core/VexRenderingContext.kt`

### Notation Primitives (M2-M3)
- Stave geometry and staff line rendering:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFStave.kt`
- Note model and paint logic (notehead/stem/flag/ledger lines):
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFStaveNote.kt`
- Accidental model and draw behavior:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFAccidental.kt`
- Clef/key/time stave modifiers:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFClef.kt`
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFKeySignature.kt`
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFTimeSignature.kt`

### Voice And Formatting Engine (M4)
- Tick-context grouping across voices:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/core/VFTickContext.kt`
- Voice timing container and stave propagation:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/core/VFVoice.kt`
- Formatter with proportional spacing + minimum-gap enforcement:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/core/VFFormatter.kt`
- M4 behavior tests:
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/core/VFFormatterTest.kt`
- Visual gate harness (formatter-driven smoke scene):
  `android/NoteWise/app/src/main/java/dev/pola/notewise/RendererSmokeView.kt`

### Connections And Grouping (M5)
- Barlines with single/double/end/repeat forms:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFBarline.kt`
- Tie curve element:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFTie.kt`
- Slur curve element:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFSlur.kt`
- Beam element with primary/secondary beam drawing:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFBeam.kt`
- M5 behavior tests:
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/elements/VFBarlineTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/elements/VFBeamTest.kt`

### Test And Coverage Architecture (M7)
- Core coverage target package: `dev/pola/vexflow/core`
- Added M7 tests:
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/core/VFTickContextTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/core/VFVoiceTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/core/VexRenderingContextTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/model/VFGlyphBoundingBoxTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/elements/VFTieTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/elements/VFSlurTest.kt`
- Shared test context helper:
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/core/RecordingContext.kt`
- Coverage task/config:
  `android/NoteWise/app/build.gradle.kts` (`jacocoTestReport` + Robolectric-compatible JaCoCo settings).

### MusicXML Parsing And Conversion Pipeline (M8)
- Parsed score domain model (`MusicSheet`, `Part`, `Measure`, note/rest and attributes):
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicSheet.kt`
- MusicXML/MXL parser (zip sniff/decompress, part/measure/attributes/note parsing):
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicXMLParser.kt`
- Converter from parsed sheet to render primitives (`VFStave`, `VFVoice`, `VFBeam`, `VFTie`):
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicSheetToVF.kt`
- Parser/converter tests:
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/parser/MusicXMLParserTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/parser/MusicSheetToVFTest.kt`
- Sample asset inputs for parser/render smoke path:
  `android/NoteWise/app/src/main/assets/samples/simple.xml`
  `android/NoteWise/app/src/main/assets/samples/clair_de_lune_excerpt.xml`
- Screen-state integration loads sample assets and renders parsed measures:
  `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererViewModel.kt`
  `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererScreen.kt`

### File Import Flow (M9)
- Home entry UI exposes SAF document picker and import action:
  `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/HomeScreen.kt`
- URI import bridge opens SAF streams and delegates to parser:
  `android/NoteWise/app/src/main/java/dev/pola/notewise/renderer/FileImportHandler.kt`
- View model owns imported-sheet and error state; renderer screen reacts to selected sheet:
  `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererViewModel.kt`
  `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererScreen.kt`
- Activity entry switched from direct renderer view to home/import surface:
  `android/NoteWise/app/src/main/java/dev/pola/notewise/MainActivity.kt`
- M9 integration tests for real sample imports:
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/parser/MusicXMLSampleImportIntegrationTest.kt`
  `android/NoteWise/app/src/test/java/dev/pola/notewise/renderer/FileImportHandlerTest.kt`

### Multi-Measure Layout (M10)
- System row renderer encapsulates stave + voice/beam/tie drawing per row:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFSystem.kt`
- Greedy line-break and row relayout engine that clones staves with per-row geometry:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFLineBreaker.kt`
- Shared rendered-bounds row-spacing refiner used by runtime view and visual harness:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFRenderedRowSpacingRefiner.kt`
- Multi-row Android view that computes system layout on size changes:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/view/MultiStaveSheetMusicView.kt`
- Compose wrapper for M10 multi-row view:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/view/MultiStaveSheetMusicComposable.kt`
- Renderer integration now supplies full rendered measure lists and hosts a vertical-scroll score container:
  `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererViewModel.kt`
  `android/NoteWise/app/src/main/java/dev/pola/notewise/screens/RendererScreen.kt`
- Layout overlap regression test:
  `android/NoteWise/app/src/test/java/dev/pola/vexflow/elements/VFLineBreakerTest.kt`
- Rendered draw-bounds collection now lives in the shared canvas abstraction and is used for
  row-spacing refinement in both runtime and visual tests:
  `android/NoteWise/app/src/main/java/dev/pola/vexflow/core/VexRenderingContext.kt`

### Assets (M0)
- Font: `android/NoteWise/app/src/main/assets/fonts/Bravura.otf`
- Glyph bbox data: `android/NoteWise/app/src/main/assets/glyph_bboxes.json`
- Samples: `android/NoteWise/app/src/main/assets/samples/`

### Reserved Package Structure (M0)
- `dev/pola/vexflow/core`
- `dev/pola/vexflow/elements`
- `dev/pola/vexflow/model`
- `dev/pola/vexflow/view`
- `dev/pola/vexflow/parser`
- `dev/pola/playback`
- `dev/pola/midi`
- `dev/pola/evaluation`
- `dev/pola/persistence`

## Milestone-to-Architecture Map
- M0: project/build/assets/package scaffolding
- M1: rendering primitives, glyph metadata, timing fractions
- M2: stave + note + accidental rendering on canvas (automated + device visual gates passed)
- M3: clef/key signature/time signature rendering and integration (tests/build passed)
- M4: voice/tick-context/formatter spacing and alignment (tests/build + device visual gate passed)
- M5: ties/slurs/barlines/beams rendering primitives (tests/build + device visual gate passed)
- M6: Compose host integration for rendering view and renderer screen wiring (build/tests + rotation + device visual gate passed)
- M7: test-suite expansion and core coverage verification (unit tests + coverage + debug assemble passed)
- M8: MusicXML/MXL parser model + converter + sample-score renderer integration (parser/converter tests + debug assemble passed)
- M9: Storage Access Framework file import wiring and sample-backed integration validation completed (targeted import tests + full unit tests + debug assemble passed)
- M10: Multi-measure system layout and scrollable multi-row renderer completed; test/build gate passed and manual on-device render validation captured

## Decision Index
The authoritative decisions are in `android/docs/AGENT_DECISIONS.md`.

Referenced decisions:
- DEC-002: use `android/NoteWise` as active target
- DEC-003: prefer absolute paths for terminal execution reliability
- DEC-004: pin Gradle JVM to Android Studio JBR
- DEC-006: use minimal forward-compatibility shims for M2 compile integrity
- DEC-007: use temporary renderer smoke view for M2 device validation evidence
- DEC-008: defer zoom/reflow implementation to M11 scope
- DEC-009: 32nd-tick context grouping with two-pass formatter spacing
- DEC-010: hard-beam geometry for M5
- DEC-011: promote beam geometry to engraving options
- DEC-012: enforce x-ordered beaming geometry before note draw
- DEC-013: use shared notehead-based stem anchor for beam stems
- DEC-014: compose host via AndroidView bridge
- DEC-015: enable JaCoCo no-location classes for Robolectric coverage attribution
- DEC-016: normalize MusicXML durations in quarter units
- DEC-017: use AGENT docs as canonical session recovery source
- DEC-018: keep SAF import surface in NoteWise app layer
- DEC-019: close M9 with sample-backed runtime import tests
- DEC-020: rebuild staves per system row in M10 line breaking
- DEC-027: share rendered-bounds row spacing refinement across runtime and visual tests

## Known Deviations
- App target differs from original `Renderer` module due missing legacy Android source.
- Validation command variant used in this environment:
  `testDebugUnitTest` (for Android unit tests with filtering), not plain `test --tests`.
- M2 manual visual gate used a temporary app layout path (`RendererSmokeView`) to keep verification explicit and low-risk.
- M4 visual gate uses the same smoke view with formatter-driven note placement and screenshot evidence (`android/docs/notewise_m4_formatter_smoke.png`).
- M5 visual gate uses smoke-view tie/beam/barline composition with final ordering/slope and stem-anchor correction evidence (`android/docs/notewise_m5_beam_anchor_fix.png`).
- M6 visual gate confirms Compose renderer tab display and rotation stability (`android/docs/notewise_m6_compose_renderer.png`).
- M7 coverage gate is package-scoped to `dev/pola/vexflow/core`, validated via `jacocoTestReport` and includes Robolectric-executed classes through `isIncludeNoLocationClasses`.
- M8 renderer integration currently targets the first parsed part and an initial measure subset for fast visual iteration; expanded pagination/system layout remains in later milestones.
- M9 closure uses repository-sample integration tests for parser and URI import-handler paths in this environment; optional device picker walkthrough can still be run as supplemental UX evidence.
- M10 uses an estimated-width + proportional-extra-space line-break strategy and relaid stave clones; closure was accepted with on-device rendering evidence (`android/docs/notewise_m10_render.png`).
- M10 currently layers an experimental rendered-bounds row-spacing refiner on top of the base
  line-break layout so runtime and visual harness share vertical spacing logic. This reduced
  heuristic divergence but has not yet improved overall `01a` conformance and may be rolled back.
- Pinch-to-zoom and related reflow/repagination behavior are scoped to M11 per implementation spec.
- Virtual staff inference for missing `<staff>` tags now requires multi-staff clef declarations; wide single-staff ranges (for example `01a-Pitches-Pitches.xml`) are intentionally kept on one staff.

## Glossary
- AGP: Android Gradle Plugin. Build plugin used by Android modules.
- Bravura: SMuFL-compliant music notation font used for glyph rendering.
- Canvas: Android 2D drawing surface used by `VexRenderingContext`.
- Clef: Symbol that defines staff pitch reference (e.g., treble/bass).
- Codepoint: Unicode numeric identifier for a glyph.
- Dynamic time warping: Alignment technique planned for performed-vs-score matching in later milestones.
- Glyph bbox: Bounding box metadata for a glyph (SMuFL staff-space coordinates).
- JBR: JetBrains Runtime bundled with Android Studio.
- M0..M20: Ordered milestone sequence from setup through evaluation/history.
- MIDI: Musical Instrument Digital Interface event protocol used for keyboard input.
- MusicXML: XML format used as score input data.
- Notehead: Core note symbol shape (whole/half/black, etc.).
- Robolectric: JVM-based Android test framework for local unit tests with Android APIs.
- SMuFL: Standard Music Font Layout, the canonical music glyph mapping standard.
- Staff-space: Relative notation coordinate unit based on distance between stave lines.
- Stave: Five-line staff on which notes/modifiers are laid out.
- Tick context: Time-position bucket used by formatters to align notes across voices.
- Formatter: Layout component that assigns note x positions from tick contexts.
- Beam: Horizontal connector replacing individual flags for grouped short-duration notes.
- Tie: Curved connector indicating sustained pitch across adjacent notes.
- Slur: Curved phrase connector indicating legato articulation.
- Accidental: Modifier glyph (sharp/flat/natural/double variants) attached to note pitch.
- Ledger line: Short horizontal line used for notes outside the standard five staff lines.
- VexFlow: Notation rendering model being ported to Kotlin/Android.
- System (layout): One horizontal row containing one or more measures.
- Line breaker: Algorithm that partitions measures into system rows based on available width.

## Update Rules
- Update this file for every milestone state transition.
- Add/refresh glossary entries when introducing new architecture terms.
- Mirror major architecture-impact decisions in both this file and `AGENT_DECISIONS.md`.