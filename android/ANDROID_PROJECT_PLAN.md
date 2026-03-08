# TrackPlay — Android Project Plan

**App Name:** TrackPlay
**Package:** `dev.pola.renderer`
**Last Updated:** 2026-03-07

---

## Table of Contents

1. [App Vision](#1-app-vision)
2. [Feature Roadmap](#2-feature-roadmap)
3. [Architecture](#3-architecture)
4. [Current State](#4-current-state)
5. [Class Inventory](#5-class-inventory)
6. [Milestones](#6-milestones)
7. [Technical Decisions](#7-technical-decisions)
8. [Android Implementation Reference](#8-android-implementation-reference)
9. [Risk Register](#9-risk-register)
10. [Reference Material Index](#10-reference-material-index)

---

## 1. App Vision

### What It Is

TrackPlay is a practice companion for music students. It shows you the sheet music, plays it for you, listens to you play, and tells you how you did.

Think of it as the "How Did I Play" feature from early GarageBand — but built natively for Android with a focus on structured practice and measurable progress.

### Who It's For

- Music students at any level learning to read and play sheet music
- Self-taught musicians who want objective feedback without a teacher in the room
- Piano students first (MIDI input), expanding to any instrument (audio input) later

### What It Does

The app has three core modes, each building on the previous:

**1. Read — Sheet Music Rendering**
Load a MusicXML file and see it rendered as professional-quality sheet music. Scroll through measures, zoom in on passages, see clefs, key signatures, time signatures, notes, rests, ties, slurs, dynamics.

**2. Watch — Auto-Play with Scrolling Cursor**
Press play. A cursor slides across the score in real time at the marked tempo. Notes light up as they're reached. The student follows along visually before attempting to play. This builds score-reading fluency and internalizes tempo/rhythm before a single note is played.

**3. Play — Performance Capture and Evaluation**
The student plays along with the scrolling score. The app captures their input via:
- **MIDI** (initial target: electric piano / digital keyboard connected via USB or Bluetooth)
- **Audio** (future: microphone input with pitch detection for any acoustic instrument)

The app compares what was played against what the score says, measuring:

| Metric | What It Measures |
|--------|-----------------|
| **Note accuracy** | Did the student play the right pitches? Which notes were missed, added, or substituted? |
| **Rhythm accuracy** | Were notes played at the right time relative to the beat? How far off in milliseconds? |
| **Tempo consistency** | Did the student maintain the indicated tempo? Where did they rush or drag? |
| **Dynamics** | Did the student follow dynamic markings (p, f, crescendo)? MIDI velocity or audio amplitude vs. score indications. |

After playing, the student sees a performance summary: an overall score, a measure-by-measure breakdown, and the specific notes or passages that need work. Over time, the app tracks progress across practice sessions.

### Why Native Rendering

The rendering engine is a ground-up Kotlin port of VexFlow (a proven JavaScript music notation library) drawing directly onto Android Canvas with the Bravura SMuFL font. No WebView, no JavaScript bridge. This gives us:

- **Low-latency cursor sync** — the rendering engine and the playback/capture engine share the same process, no IPC lag
- **Pixel-precise hit regions** — every note knows its exact screen coordinates, so the cursor, highlighting, and performance annotations are perfectly aligned to the rendered score
- **Offline-first** — no network dependency, no web runtime overhead
- **Full control** — we own the rendering pipeline, so we can overlay performance annotations (green/red note coloring, timing deviation markers) directly on the score without fighting a third-party renderer

### Why Electric Piano First

Electric pianos and digital keyboards output MIDI over USB. MIDI gives us:
- Exact note-on/note-off events with millisecond timestamps
- Key velocity (0-127) for dynamics measurement
- No pitch detection ambiguity — a C4 is a C4
- Zero signal processing overhead

This lets us build and validate the entire evaluation engine against clean, unambiguous input. Audio capture (microphone → pitch detection → note estimation) introduces noise, latency, polyphony challenges, and instrument-specific timbre handling — all solvable, but separate problems best tackled after the core evaluation logic is proven.

---

## 2. Feature Roadmap

The app is built in four phases. Each phase produces a usable app — not a demo, not a scaffold.

### Phase A: Render (Milestones M0–M11)

The sheet music rendering engine. Load MusicXML, render it natively with professional quality.

| Capability | Description |
|-----------|-------------|
| MusicXML import | Open `.xml` and `.mxl` files from device storage |
| Stave rendering | 5-line staff with clef, key signature, time signature |
| Note rendering | Noteheads, stems, flags, accidentals, rests |
| Layout | Multi-voice formatting, justified spacing, multi-measure systems with line breaks |
| Articulations | Ties, slurs, barlines (including repeats), beams |
| Navigation | Vertical scroll, pinch-to-zoom, page layout |

**Exit criteria for Phase A:** Open Clair de Lune, scroll through the entire piece, every measure renders correctly.

### Phase B: Auto-Play (Milestones M12–M14)

Add a time dimension to the score. The cursor moves through the music at the correct tempo.

| Capability | Description |
|-----------|-------------|
| Tempo engine | Parse tempo markings from MusicXML. Map beats to wall-clock time. Handle tempo changes and fermatas. |
| Playback cursor | A vertical line or highlight that slides smoothly across the score, synchronized to the tempo engine. Automatically scrolls the view to follow. |
| Note highlighting | Notes change color (or glow, or enlarge slightly) as the cursor reaches them. Fades after passing. |
| Metronome | Optional audible click track. Configurable: on/off, subdivisions, accent pattern. Uses Android `AudioTrack` or `SoundPool` for low-latency clicks. |
| Playback controls | Play/pause/stop. Tempo adjustment (50%–150% of marked tempo). Jump to measure/rehearsal mark. Loop a selected range of measures. |
| Audio synthesis (optional) | Basic MIDI playback so the student can hear the piece. General MIDI soundfont via Android `Synthesizer` or bundled FluidSynth/Oboe. Not required — the value is visual, not audio — but nice to have. |

**Exit criteria for Phase B:** Press play on Clair de Lune. Cursor slides across the score at the marked tempo. Notes light up. View auto-scrolls. Student can adjust tempo to 60% and loop measures 5–8.

### Phase C: MIDI Capture (Milestones M15–M17)

Connect a MIDI keyboard and capture what the student plays in real time.

| Capability | Description |
|-----------|-------------|
| MIDI input | Android MIDI API (`android.media.midi`). Detect connected USB/Bluetooth MIDI devices. Receive note-on, note-off, control change messages. |
| Real-time note display | As the student plays, their notes appear on the score — green for correct, red for wrong, gray for missed. Think "Guitar Hero meets sheet music." |
| Recording | Capture the full MIDI stream with timestamps. Store as a `PerformanceRecord` alongside the `MusicSheet` reference. |
| Latency compensation | Measure and compensate for USB/Bluetooth MIDI latency. Configurable offset in settings. |

**Exit criteria for Phase C:** Connect a USB MIDI keyboard. Start a practice session on a simple piece. Play it through. See notes appear on the score in real time — green for correct, red for wrong.

### Phase D: Evaluation (Milestones M18–M20)

Compare the performance recording against the score and generate feedback.

| Capability | Description |
|-----------|-------------|
| Note matching | Align performed notes to score notes using dynamic time warping or a beat-aligned matching algorithm. Handle inserted/deleted/substituted notes. |
| Scoring engine | Calculate per-note and per-measure scores across four axes: pitch accuracy, rhythm accuracy, tempo consistency, dynamics adherence. Weight and combine into an overall percentage. |
| Performance summary | Post-session screen showing: overall score, per-measure heatmap (green→yellow→red), list of trouble spots, comparison to previous attempts. |
| Practice history | Room/DataStore persistence. Track scores over time per piece. Show progress graphs. |
| Targeted practice | "Practice these measures" — auto-loop the measures the student scored lowest on. |

**Exit criteria for Phase D:** Play a piece, see a score of 78%. Tap a red measure to see "Measure 12: missed F#4, rushed beat 3 by 120ms." Tap "Practice this section" to loop measures 11–14 at 70% tempo.

### Future (Post-1.0)

| Feature | Description |
|---------|-------------|
| Audio capture | Microphone input → pitch detection (YIN, pYIN, or CREPE model) → note estimation. Supports any acoustic instrument. |
| Multi-instrument | Score parts for different instruments. Student selects their part. |
| Teacher mode | Teacher assigns pieces, sets tempo targets, reviews student progress remotely. |
| Cross-platform backport | Planned in a separate document after Android milestones stabilize. |

---

## 3. Architecture

### Layer Diagram

```
┌─────────────────────────────────────────────────────┐
│                  Compose UI Layer                    │
│  HomeScreen · ScoreScreen · PlayScreen              │
│  ResultsScreen · HistoryScreen · SettingsScreen     │
├─────────────────────────────────────────────────────┤
│               Compose <-> View Bridge               │
│  AndroidView { SheetMusicView(context) }            │
├─────────────────────────────────────────────────────┤
│  Playback Engine          │  MIDI Engine            │
│  TempoMap · Cursor        │  MidiDeviceManager      │
│  Metronome · AudioSynth   │  MidiRecorder           │
├───────────────────────────┤  NoteEventStream        │
│  Evaluation Engine        ├─────────────────────────┤
│  NoteMatcher              │  VexFlow Rendering      │
│  ScoringEngine            │  VexRenderingContext     │
│  PerformanceRecord        │  VFFormatter · VFVoice   │
├───────────────────────────┤  VFTickContext           │
│  Persistence              ├─────────────────────────┤
│  Room DB                  │  Notation Elements      │
│  PracticeSession          │  VFStave · VFClef       │
│  ScoreHistory             │  VFKeySignature · etc.  │
├───────────────────────────┴─────────────────────────┤
│                    Music Data Model                  │
│  MusicSheet · VFStaveNote · VFFraction · VFMetrics   │
│  VFTables · VFGlyphBoundingBox                       │
├─────────────────────────────────────────────────────┤
│                  MusicXML Parser                     │
│  MusicXMLParser · MusicSheetReader                   │
├─────────────────────────────────────────────────────┤
│                  Platform / Assets                   │
│  Bravura.otf · glyph_bboxes.json · Canvas · MIDI API│
└─────────────────────────────────────────────────────┘
```

### Package Structure

```
Renderer/app/src/main/java/dev/pola/
├── renderer/                  # App shell
│   ├── MainActivity.kt
│   ├── navigation/
│   ├── screens/
│   └── ui/theme/
├── vexflow/                   # Rendering engine
│   ├── core/                  #   VexRenderingContext, VFFormatter, VFVoice, VFTickContext
│   ├── elements/              #   VFStave, VFClef, VFKeySignature, VFTimeSignature, ...
│   ├── model/                 #   VFStaveNote, VFFraction, VFMetrics, VFTables, VFGlyphBoundingBox
│   ├── view/                  #   SheetMusicView (custom View), SheetMusicComposable
│   └── parser/                #   MusicXMLParser, MusicSheet, MusicSheetReader
├── playback/                  # Auto-play engine (Phase B)
│   ├── TempoMap.kt            #   Beat -> wall-clock time mapping
│   ├── PlaybackCursor.kt      #   Cursor position over time
│   ├── Metronome.kt           #   Audible click track
│   └── AudioSynthesizer.kt   #   Optional MIDI audio playback
├── midi/                      # MIDI input (Phase C)
│   ├── MidiDeviceManager.kt   #   Device detection and connection
│   ├── MidiRecorder.kt        #   Timestamped note event capture
│   └── NoteEvent.kt           #   Data class for note-on/off/velocity
├── evaluation/                # Performance evaluation (Phase D)
│   ├── NoteMatcher.kt         #   Align performed notes to score
│   ├── ScoringEngine.kt       #   Per-note and per-measure scoring
│   └── PerformanceRecord.kt   #   Evaluation results data model
└── persistence/               # Practice history (Phase D)
    ├── AppDatabase.kt         #   Room database
    ├── PracticeSession.kt     #   Session entity
    └── ScoreHistory.kt        #   Per-piece progress tracking
```

---

## 4. Current State

### Renderer/ (Target App)

| Component | Status |
|-----------|--------|
| Gradle build config | DONE — AGP 8.13.2, Kotlin 2.0.21, Compose BOM 2024.09.00 |
| MainActivity + navigation | DONE — 4 destinations, adaptive bottom nav / rail |
| Material3 theme | DONE |
| RendererScreen | PLACEHOLDER — buttons with no logic |
| VexFlow library | NOT STARTED |
| Bravura font asset | NOT PRESENT |
| Glyph bbox JSON | NOT PRESENT |
| Playback engine | NOT STARTED |
| MIDI engine | NOT STARTED |
| Evaluation engine | NOT STARTED |

### Legacy: __TrackPlay/ (Reference Only)

| Component | Status |
|-----------|--------|
| VexFlow Kotlin library | COMPILED ONLY — source code lost, ~200 classes in DEX artifacts |
| SheetMusicView.kt | EXISTS — shows VexFlow API usage patterns |
| Test suite | 20/32 passing (Phases 1–4 done) |

### Build Baseline

| Component | Status |
|-----------|--------|
| Android module compiles | IN PROGRESS |
| Rendering core packages | NOT STARTED |
| Test harness reintegration | NOT STARTED |
| Assets wired to app module | NOT STARTED |

---

## 5. Class Inventory

### Rendering engine

| # | Class | Target Path | Milestone | Status |
|---|-------|-------------|-----------|--------|
| 1 | VFFraction | vexflow/model/ | M1 | NOT STARTED |
| 2 | VFMetrics | vexflow/model/ | M1 | NOT STARTED |
| 3 | VFTables | vexflow/model/ | M1 | NOT STARTED |
| 4 | VFGlyphBoundingBox | vexflow/model/ | M1 | NOT STARTED |
| 5 | VexRenderingContext | vexflow/core/ | M1 | NOT STARTED |
| 6 | VFStave | vexflow/elements/ | M2 | NOT STARTED |
| 7 | VFStaveNote | vexflow/model/ | M2 | NOT STARTED |
| 8 | VFAccidental | vexflow/elements/ | M2 | NOT STARTED |
| 9 | VFClef | vexflow/elements/ | M3 | NOT STARTED |
| 10 | VFKeySignature | vexflow/elements/ | M3 | NOT STARTED |
| 11 | VFTimeSignature | vexflow/elements/ | M3 | NOT STARTED |
| 12 | VFVoice | vexflow/core/ | M4 | NOT STARTED |
| 13 | VFTickContext | vexflow/core/ | M4 | NOT STARTED |
| 14 | VFFormatter | vexflow/core/ | M4 | NOT STARTED |
| 15 | VFTie | vexflow/elements/ | M5 | NOT STARTED |
| 16 | VFSlur | vexflow/elements/ | M5 | NOT STARTED |
| 17 | VFBarline | vexflow/elements/ | M5 | NOT STARTED |
| 18 | VFBeam | vexflow/elements/ | M5 | NOT STARTED |
| 19 | SheetMusicView | vexflow/view/ | M6 | NOT STARTED |
| 20 | SheetMusicComposable | vexflow/view/ | M6 | NOT STARTED |
| 21 | MusicXMLParser | vexflow/parser/ | M8 | NOT STARTED |
| 22 | MusicSheet | vexflow/parser/ | M8 | NOT STARTED |

### Playback engine (new)

| # | Class | Target Path | Milestone | Status |
|---|-------|-------------|-----------|--------|
| 23 | TempoMap | playback/ | M12 | NOT STARTED |
| 24 | PlaybackCursor | playback/ | M13 | NOT STARTED |
| 25 | Metronome | playback/ | M13 | NOT STARTED |
| 26 | AudioSynthesizer | playback/ | M14 | NOT STARTED |

### MIDI engine (new)

| # | Class | Target Path | Milestone | Status |
|---|-------|-------------|-----------|--------|
| 27 | NoteEvent | midi/ | M15 | NOT STARTED |
| 28 | MidiDeviceManager | midi/ | M15 | NOT STARTED |
| 29 | MidiRecorder | midi/ | M16 | NOT STARTED |

### Evaluation engine (new)

| # | Class | Target Path | Milestone | Status |
|---|-------|-------------|-----------|--------|
| 30 | NoteMatcher | evaluation/ | M18 | NOT STARTED |
| 31 | ScoringEngine | evaluation/ | M18 | NOT STARTED |
| 32 | PerformanceRecord | evaluation/ | M19 | NOT STARTED |

---

## 6. Milestones

### Phase A: Render

#### M0: Project Setup

- [ ] Create package directories
- [ ] Copy Bravura.otf to assets/fonts/
- [ ] Copy extracted_glyph_bboxes.json to assets/
- [ ] Copy sample MusicXML files to assets/samples/
- [ ] Add test dependencies (JUnit 5, Mockito-Kotlin, Robolectric)
- [ ] Verify project builds

---

#### M1: Rendering Foundation

Port the data model and rendering context.

| Class | Implementation Notes |
|-------|----------------------|
| VFFraction | Kotlin operator overloading + `Comparable`. |
| VFMetrics | `object` with `const val` properties. |
| VFTables | `object` with `const val` Unicode chars (`'\uE050'`). |
| VFGlyphBoundingBox | JSON from `context.assets.open()`, singleton via `by lazy`. |
| VexRenderingContext | `Canvas` + `Paint` primitives; per-glyph Y-flip for SMuFL rendering. |

**Exit:** All 5 classes compile and pass tests. Bravura font loads. A glyph draws onto Canvas.

---

#### M2: Staff and Notes

First visible output.

| Class | Lines | Notes |
|-------|-------|-------|
| VFStave | 174 | Staff lines, `getYForLine()`, `getYForNote()` |
| VFStaveNote | 490 | Pitch-to-line mapping, accidentals, stems, metrics |
| VFAccidental | ~100 | Type enum, glyph selection, relative positioning |

**Exit:** 5-line staff with a notehead and stem renders on Canvas.

---

#### M3: Clef, Key Signature, Time Signature

| Class | Lines | Notes |
|-------|-------|-------|
| VFClef | 234 | Treble/bass/alto/tenor enums, glyph + bbox centering |
| VFKeySignature | 213 | 15 major + 15 minor keys, accidental ordering |
| VFTimeSignature | 286 | Numeric two-row layout + common/cut symbols |

**Exit:** Stave renders with clef + key signature + time signature.

---

#### M4: Voice and Formatting Engine

The core layout engine.

| Class | Lines | Notes |
|-------|-------|-------|
| VFVoice | 118 | Tickable container, `preFormat()` |
| VFTickContext | 218 | Alignment bucket, metrics, `setX()` propagation |
| VFFormatter | 276 | Tick context creation, two-pass layout, justification |

**Exit:** Multiple notes render with even spacing. Multi-voice alignment works.

---

#### M5: Ties, Slurs, Barlines, Beams

| Class | Lines | Notes |
|-------|-------|-------|
| VFTie | 203 | Bezier curves, partial ties |
| VFSlur | 191 | Bezier curves, configurable arc |
| VFBarline | 227 | 7 types including repeats |
| VFBeam | 93 | Placeholder (straight line). Full impl in M12. |

**Exit:** Notes connected by ties/slurs. Measures delimited by barlines.

---

#### M6: Compose Integration

| Deliverable | Description |
|-------------|-------------|
| SheetMusicView.kt | Custom `View` with `onDraw(Canvas)` |
| SheetMusicComposable.kt | `AndroidView` wrapper |
| RendererScreen update | Replace placeholders with rendered score |
| RendererViewModel.kt | Music data state holder |

**Exit:** Launch app -> Renderer screen -> see rendered staff with clef, key sig, time sig, notes.

---

#### M7: Test Suite

30+ test files covering model, rendering, and integration layers.

---

#### M8: MusicXML Parsing

| Component | Description |
|-----------|-------------|
| MusicXMLParser.kt | `XmlPullParser` for `.xml`, ZIP decompression for `.mxl` |
| MusicSheet.kt | Hierarchical data model: Sheet -> Part -> Measure -> Note |
| MusicSheetToVF.kt | Convert MusicSheet to VexFlow rendering objects |

**Exit:** Load and render the first few measures of Clair de Lune.

---

#### M9: File Import

SAF file picker, URI handling, recent files list.

**Exit:** User picks a `.mxl` from their device and sees it rendered.

---

#### M10: Multi-Measure Layout

| Component | Description |
|-----------|-------------|
| VFSystem.kt | Horizontal row of staves |
| VFLineBreaker.kt | Automatic line breaking based on available width |
| VFPageLayout.kt | Vertical system positioning, margins |

**Exit:** Multi-measure piece renders across multiple lines with auto line breaks.

---

#### M11: Polish & UX

Pinch-to-zoom, smooth scroll, dark mode, settings, error handling, performance optimization.

---

### Phase B: Auto-Play

#### M12: Tempo Engine

| Component | Description |
|-----------|-------------|
| TempoMap.kt | Parse MusicXML `<direction>` elements for tempo markings. Build a map from beat position to wall-clock milliseconds. Handle: initial tempo, mid-piece tempo changes, ritardando/accelerando (linear interpolation), fermatas (configurable hold duration). |
| TempoMapTest.kt | Verify: constant tempo mapping, tempo changes at measure boundaries, fermata hold calculation. |

**Data model:**
```
TempoMap:
  - entries: List<TempoEntry>  // (beatPosition: VFFraction, bpm: Float)
  - beatToMs(beat: VFFraction): Long
  - msToBeat(ms: Long): VFFraction
```

**Exit:** Given a MusicSheet, produce a TempoMap. `beatToMs(beat 4.0)` returns correct ms at 120 BPM.

---

#### M13: Playback Cursor and Metronome

| Component | Description |
|-----------|-------------|
| PlaybackCursor.kt | Driven by a `Choreographer` or `ValueAnimator` frame callback. Each frame: query TempoMap for current beat -> resolve to x-coordinate on the rendered score -> update cursor position. Triggers view auto-scroll when cursor nears edge. |
| CursorOverlay.kt | Draws a translucent vertical line at the cursor's x position. Optionally highlights the current note(s) with a color tint. |
| Metronome.kt | Pre-generate click samples. Schedule playback via `AudioTrack` (low-latency mode) at beat boundaries from TempoMap. Support: on/off, subdivisions (quarter, eighth), accent on beat 1. |
| Playback controls | Play/pause/stop buttons. Tempo slider (50%-150%). Loop range selector (start measure, end measure). Jump to measure. |

**Exit:** Press play. Cursor moves across the score at the correct tempo. Notes highlight. Optional metronome clicks. Tempo adjustable. Measures 5-8 can be looped.

---

#### M14: Audio Synthesis (Optional)

| Component | Description |
|-----------|-------------|
| AudioSynthesizer.kt | Load a General MIDI SoundFont (e.g., FluidR3). Use Android's `Synthesizer` API or bundle Oboe/FluidSynth. Schedule note events from the MusicSheet at times from the TempoMap. |

**Exit:** Press play and hear the piece played back with a piano sound while the cursor moves.

---

### Phase C: MIDI Capture

#### M15: MIDI Device Connection

| Component | Description |
|-----------|-------------|
| NoteEvent.kt | `data class NoteEvent(val pitch: Int, val velocity: Int, val timestampMs: Long, val type: NoteEventType)` where type is ON or OFF. |
| MidiDeviceManager.kt | Use `android.media.midi.MidiManager`. Listen for device connect/disconnect. Open input port. Parse MIDI messages (status byte 0x90 = note-on, 0x80 = note-off). Emit `NoteEvent`s via a `Flow<NoteEvent>`. Handle USB and Bluetooth MIDI. |
| Device selection UI | Settings screen lists available MIDI devices. User selects one. Connection status indicator. |

**Exit:** Connect USB MIDI keyboard. Play notes. See `NoteEvent`s logged with correct pitch, velocity, and timestamp.

---

#### M16: Real-Time Note Display

| Component | Description |
|-----------|-------------|
| MidiRecorder.kt | Collects all `NoteEvent`s during a practice session into a `PerformanceRecording` with the session's start timestamp as reference. |
| Live annotation overlay | As notes arrive from MIDI: find the expected note at the current cursor beat. If pitch matches -> highlight green on the score. If pitch is wrong -> highlight red. If no note was expected -> dim indicator for extra notes. Missed notes (cursor passed, no input received) -> gray out. |

**Exit:** Start session. Play along. Correct notes turn green, wrong notes turn red, missed notes go gray — all in real time as the cursor scrolls.

---

#### M17: Latency Compensation

| Component | Description |
|-----------|-------------|
| Latency calibration | Settings screen: "Tap along with the metronome" calibration flow. Measure average offset between MIDI event timestamp and expected beat time. Store as `latencyOffsetMs`. |
| Offset application | Subtract `latencyOffsetMs` from all incoming MIDI timestamps before note matching. |

**Exit:** Calibrate. Offset applied. Timing accuracy improves measurably vs. uncalibrated.

---

### Phase D: Evaluation

#### M18: Note Matching and Scoring

| Component | Description |
|-----------|-------------|
| NoteMatcher.kt | Align performed NoteEvents to score notes. Algorithm: for each score note, find the closest performed note within a time window (based on tempo). Handle: correct notes, missed notes (in score but not played), extra notes (played but not in score), substituted notes (wrong pitch at right time). |
| ScoringEngine.kt | Calculate four scores per note: **Pitch** (binary: correct/incorrect). **Rhythm** (continuous: deviation in ms from expected time, normalized to beat duration). **Tempo** (continuous: local tempo vs. expected tempo over a rolling window). **Dynamics** (continuous: performed velocity vs. expected velocity from dynamic markings). Aggregate per-measure and per-piece. |

**Exit:** After a session, `ScoringEngine` produces per-note scores. Overall score: 78%.

---

#### M19: Performance Summary Screen

| Component | Description |
|-----------|-------------|
| PerformanceRecord.kt | Data model: session timestamp, piece reference, per-note match results, per-measure scores, overall scores, tempo graph data. |
| ResultsScreen.kt | Overall score (large number with color). Per-measure heatmap (green-yellow-red bar below the score). Trouble spot list: "Measure 12: missed F#4, rushed beat 3 by 120ms." Tap a measure to see detail. |
| Score annotation | Re-render the score with performance overlay: green/red noteheads, timing deviation arrows, dynamic deviation indicators. |

**Exit:** Play a piece. See results screen with 78%, red on measure 12, tap to see "missed F#4."

---

#### M20: Practice History and Targeted Practice

| Component | Description |
|-----------|-------------|
| AppDatabase.kt | Room database with entities for PracticeSession, PieceRecord, MeasureScore. |
| HistoryScreen.kt | List of pieces practiced. Per-piece: score trend graph over sessions. Per-session: detailed breakdown. |
| Targeted practice | From results screen: "Practice this section" button. Auto-sets loop range to the lowest-scoring consecutive measures. Defaults to 70% tempo. |

**Exit:** Practice history shows 5 sessions on same piece with improving scores. "Practice this section" loops measures 11-14 at 70% tempo.

---

## Milestone Dependency Graph

```
M0 (Setup)
 └─> M1 (Foundation) ─> M2 (Staff+Notes) ─> M3 (Modifiers) ─> M4 (Layout)
      │                                                           │
      │                                                     ┌─────┴─────┐
      │                                                     v           v
      │                                                M5 (Ties/      M6 (Compose
      │                                                 Barlines)      Integration)
      │                                                     │           │
      │                                                     v           v
      │                                                     M7 (Tests)
      │                                                     │
      │                                                     v
      │                                                M8 (MusicXML) ─> M9 (File Import)
      │                                                     │
      │                                                     v
      │                                                M10 (Multi-measure) ─> M11 (Polish)
      │
      │  ─── Phase B ───────────────────────────────────────────────────
      │
      └─> M12 (Tempo Engine) ─> M13 (Cursor + Metronome) ─> M14 (Audio Synth)
                                      │
      │  ─── Phase C ─────────────────┼────────────────────────────────
      │                               │
      └─> M15 (MIDI Connect) ─> M16 (Real-time Display) ─> M17 (Latency)
                                      │
      │  ─── Phase D ─────────────────┼────────────────────────────────
      │                               │
      └─> M18 (Matching + Scoring) ─> M19 (Results Screen) ─> M20 (History)
```

**Critical path:** M0 -> M1 -> M2 -> M3 -> M4 -> M6 -> M8 -> M10 -> M12 -> M13 -> M15 -> M16 -> M18 -> M19

---

## 7. Technical Decisions

### TD-1: Bravura Font Loading

Load via `Typeface.createFromAsset(assetManager, "fonts/Bravura.otf")`. Android's `Paint.drawText()` accepts a `Typeface` directly.

### TD-2: SMuFL Glyph Y-Flip

Per-glyph `Canvas.save()` -> `translate()` -> `scale(1f, -1f)` -> `drawText()` -> `restore()`.

### TD-3: Compose Integration via AndroidView

Custom `View` with `onDraw(Canvas)` wrapped in `AndroidView` composable. Gives full Canvas API access and pixel-precise control needed for notation rendering and cursor overlay.

### TD-4: Glyph Bounding Box JSON

Use `extracted_glyph_bboxes.json` loaded from Android assets for deterministic glyph positioning.

### TD-5: Test Strategy

JUnit 5 + Mockito-Kotlin + Robolectric. Mock Canvas records draw calls. Matches __TrackPlay test infrastructure.

### TD-6: Single Module

`dev.pola.vexflow` as a package within the app module. No separate Gradle module until needed.

### TD-7: MIDI via Android MIDI API

Use `android.media.midi.MidiManager` (API 24+ project baseline). Handles USB and Bluetooth MIDI. `NoteEvent` flow emitted from a background coroutine listening on the MIDI input port.

### TD-10: Build Infrastructure First

Execution starts with infrastructure stabilization: fix compile blockers, set SDK baseline, verify Gradle wrapper, restore test runtime, and validate asset plumbing before implementing rendering features.

### TD-8: Low-Latency Audio for Metronome

`AudioTrack` in low-latency mode or Oboe library. Pre-generated PCM samples for click sounds. Frame-accurate scheduling synchronized to the tempo engine.

### TD-9: Performance Evaluation Algorithm

Beat-aligned greedy matching: for each score note (in chronological order), find the closest unmatched performed note within a configurable time window (default: +/- 50% of beat duration). This is simpler and more predictable than dynamic time warping for single-voice, single-instrument input.

---

## 8. Android Implementation Reference

### Primary Code References

| Reference | Path | Use |
|----------|------|-----|
| Legacy Android API contract | `reference/trackplay/SheetMusicView.kt` | Validate expected rendering API shape |
| Legacy custom view sample | `reference/trackplay/CustomSheetMusicView.kt` | Canvas drawing and usage patterns |
| Legacy demo activity | `reference/trackplay/DemoActivity.kt` | Integration behavior examples |
| alphaTab source | `reference/alphaTab-develop/` | Beam/layout/parser algorithm reference |
| OSMD source | `reference/opensheetmusicdisplay/` | MusicXML architecture and rendering flow |

---

## 9. Risk Register

| # | Risk | Impact | Mitigation |
|---|------|--------|------------|
| R1 | Glyph position differences across Android devices/font stacks | Visual inconsistencies | Use bounding box data for positioning, not font metrics. Visual regression tests. |
| R2 | VFBeam is a placeholder — no reference implementation | Blocks professional note rendering | Reference VexFlow JS `beam.ts` and alphaTab source. |
| R3 | MusicXML parsing edge cases | Parser fails on real-world files | Start with single-part, single-voice subset. Expand incrementally. |
| R4 | MIDI latency varies by device and connection type | Timing accuracy suffers | Calibration flow in settings. Per-device latency profiles. |
| R5 | Audio pitch detection (future) is fundamentally harder than MIDI | Audio capture feature quality risk | Defer to post-1.0. Build and validate evaluation engine on clean MIDI first. |
| R6 | Multi-stave layout is architecturally complex | Gap between demo and real app | Reference VexFlow System/Factory classes. |
| R7 | __TrackPlay VexFlow Kotlin source permanently lost | Cannot reference prior Android-specific decisions | Use `reference/trackplay/` files and algorithmic references (`alphaTab`, `OSMD`) as the contract baseline. |

---

## 10. Reference Material Index

| Resource | Path | Purpose |
|----------|------|---------|
| Legacy Android API contract | reference/trackplay/SheetMusicView.kt | VexFlow API usage patterns |
| Legacy Android custom view sample | reference/trackplay/CustomSheetMusicView.kt | Canvas behavior reference |
| Legacy Android demo activity | reference/trackplay/DemoActivity.kt | End-to-end flow reference |
| Bravura font | bravura/otf/Bravura.otf | SMuFL reference font |
| Bravura metadata | bravura/bravura_metadata.json | Glyph metadata (733KB) |
| Extracted glyph bboxes | bravura/extracted_glyph_bboxes.json | Runtime bounding box data |
| alphaTab source | reference/alphaTab-develop/ | MusicXML parsing, beam, layout reference |
| OSMD source | reference/opensheetmusicdisplay/ | MusicXML rendering architecture reference |
| MusicXML samples | samples/ | Test content for parser |
| LilyPond test suite | samples/lilypond_tests/ | Visual regression reference images |
| OSMD architecture doc | docs/OpenSheetMusicDisplay_Technical_Architecture.md | Architecture reference |
| Legacy test reintegration plan | docs/ANDROID_TEST_REINTEGRATION_PLAN.md | Test inventory and dependency map |

---

## Summary

| Phase | Milestones | Key Outcome |
|-------|-----------|-------------|
| **A: Render** | M0–M11 | Load MusicXML, render full scores natively |
| **B: Auto-Play** | M12–M14 | Cursor scrolls at tempo, notes highlight, metronome clicks |
| **C: MIDI Capture** | M15–M17 | Connect keyboard, see correct/wrong notes in real time |
| **D: Evaluation** | M18–M20 | Performance score, trouble spots, practice history |

**Total: 21 milestones, 32 classes, 4 phases.**
