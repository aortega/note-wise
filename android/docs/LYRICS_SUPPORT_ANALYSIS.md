# NoteWise Lyrics Support Analysis

**Date:** 2026-03-10  
**Status:** NOT IMPLEMENTED

## Executive Summary

Lyrics from MusicXML files are **currently not supported** in the NoteWise Android app. While the infrastructure for text rendering exists in the rendering context, there is no parsing, data model support, or rendering implementation for lyrics.

Lyrics **are being tested** in the visual test suite (13 test fixtures), but they render blank/empty where lyrics should appear.

---

## 1. Files That Handle Lyric Parsing: ❌ NONE

### Current Parser Implementation

**File:** [android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicXMLParser.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicXMLParser.kt)

The `parseNote()` function (line 250-350) reads the following `<note>` element children but **explicitly skips `<lyric>` elements**:

```kotlin
when (parser.name) {
    "step" -> step = parser.nextText().trim()
    "octave" -> octave = parser.nextText().trim().toIntOrNull() ?: 4
    "alter" -> alter = parser.nextText().trim().toFloatOrNull() ?: 0f
    "duration" -> duration = parser.nextText().trim().toIntOrNull() ?: 1
    "voice" -> voice = parser.nextText().trim().toIntOrNull() ?: 1
    "staff" -> { /* ... */ }
    "rest" -> isRest = true
    "chord" -> isChord = true
    "tie" -> { /* handles tie start/end */ }
    "beam" -> { /* handles beam states */ }
    "accidental" -> { /* ... */ }
    "slur" -> { /* handles slur start/end */ }
    // ❌ NO CASE FOR "lyric"
}
```

When the parser encounters a `<lyric>` element, it simply skips it (the XmlPullParser moves to the next element without processing).

---

## 2. Data Model Support: ❌ NOT PRESENT

**File:** [android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicSheet.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicSheet.kt)

The `NoteData` class (lines 44-60) has NO field for lyrics:

```kotlin
data class NoteData(
    val pitch: Pitch,
    override val duration: Int,
    override val voice: Int = 1,
    override val staff: Int = 1,
    val staffExplicit: Boolean = false,
    override val isChordNote: Boolean = false,
    val tieStart: Boolean = false,
    val tieEnd: Boolean = false,
    val slurStart: Boolean = false,
    val slurEnd: Boolean = false,
    val beamState: BeamState = BeamState.NONE,
    val accidental: String? = null,    // ← accent/accidental supported
    val notationType: String = "normal"  // ← normal/grace notes supported
    // ❌ NO lyrics field
) : NoteOrRest()
```

Supporting fields exist for:
- Accidentals (`#`, `b`, `n`, `##`, `bb`)
- Note types (`normal`, `grace`)
- Tie state (start/end)
- Slur state (start/end)
- Beam state (begin/continue/end)

But **not for lyrics**.

---

## 3. Files That Handle Lyric Rendering: ❌ NONE

### Conversion Layer

**File:** [android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicSheetToVF.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/parser/MusicSheetToVF.kt)

The `buildVFNote()` function and `convertMeasure()` method have no logic for handling lyrics. Lyrics data (if it existed) would be lost during the MusicSheet→VexFlow conversion.

### Rendering Engine

The codebase has **no lyric rendering class**. There is no:
- `VFLyric.kt` element class
- Lyric positioning/layout logic
- Lyric text drawable in any rendering pipeline

Related files and what they handle:
- [VFStave.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFStave.kt) – staff lines, clef, key sig, time sig
- [VFStaveNote.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFStaveNote.kt) – note heads and stems
- [VFAccidental.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFAccidental.kt) – accidentals
- [VFBeam.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFBeam.kt) – beamed groups
- [VFTie.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFTie.kt) – ties
- [VFSlur.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFSlur.kt) – slurs

**None of these handle lyrics.**

---

## 4. Known Limitations & TODOs

### Test Fixtures With Lyrics (NOT PASSING)

The visual test suite explicitly tests lyrics in 13 fixtures, but they are **expected to fail** because lyrics are not implemented:

[android/NoteWise/app/src/test/java/dev/pola/notewise/visual/MusicXMLSuiteVisualTest.kt](android/NoteWise/app/src/test/java/dev/pola/notewise/visual/MusicXMLSuiteVisualTest.kt) (lines 186–194):

```kotlin
// 61 Lyrics
Fixture("61a-Lyrics.xml",                                       "61a_lyrics"),
Fixture("61b-MultipleLyrics.xml",                               "61b_multiplelyrics"),
Fixture("61c-Lyrics-Pianostaff.xml",                            "61c_lyrics_pianostaff"),
Fixture("61d-Lyrics-Melisma.xml",                               "61d_lyrics_melisma"),
Fixture("61e-Lyrics-Chords.xml",                                "61e_lyrics_chords"),
Fixture("61f-Lyrics-GracedNotes.xml",                           "61f_lyrics_gracednotes"),
Fixture("61g-Lyrics-NameNumber.xml",                            "61g_lyrics_namenumber"),
Fixture("61h-Lyrics-BeamsMelismata.xml",                        "61h_lyrics_beamsmelismata"),
Fixture("61i-Lyrics-Chords.xml",                                "61i_lyrics_chords"),
Fixture("61j-Lyrics-Elisions.xml",                              "61j_lyrics_elisions"),
Fixture("61k-Lyrics-SpannersExtenders.xml",                     "61k_lyrics_spannersextenders"),
```

Additional fixtures that include lyrics as part of a larger feature:
- `42a-MultiVoice-TwoVoicesOnStaff-Lyrics.xml` (lyrics on multi-voice staves)
- `43f-MultiStaff-Lyrics.xml` (lyrics across grand staff)

### No Open Issues or TODOs

A search of the codebase for `TODO|FIXME` related to lyrics yields:
- **0 results in main source code** (`android/NoteWise/app/src/main/**`)
- No explicit tracking in issue registers (AGENT_ISSUES.md, AGENT_DECISIONS.md)

This suggests lyrics were never started as a development task.

---

## 5. MusicXML Lyric Structure (For Reference)

### Example from 61a-Lyrics.xml

```xml
<note>
  <pitch>
    <step>A</step>
    <octave>4</octave>
  </pitch>
  <duration>1</duration>
  <voice>1</voice>
  <type>quarter</type>
  <lyric number="1">
    <syllabic>begin</syllabic>
    <text>Tra</text>
  </lyric>
</note>
<note>
  <pitch>
    <step>A</step>
    <octave>4</octave>
  </pitch>
  <duration>1</duration>
  <voice>1</voice>
  <type>quarter</type>
  <lyric number="1">
    <syllabic>middle</syllabic>
    <text>la</text>
  </lyric>
</note>
```

**Key lyric element attributes/children:**
- `number` – identifies lyric line (1, 2, 3…for stanzas)
- `syllabic` – syllable continuation (`single`, `begin`, `middle`, `end`)
- `text` – the actual lyric text
- Optional: `extend`, `elision`, `endLine`, `endParagraph` for advanced formatting

### Lyric Test Complexity

The 13 test fixtures cover:
- **Simple lyrics** (61a): basic syllables and syllable spanning
- **Multiple stanzas** (61b): multiple `<lyric>` elements with different `number` attributes
- **Grand staff lyrics** (43f, 61c): lyrics aligned to specific staves
- **Melisma** (61d): one syllable over multiple notes
- **Chords** (61e, 61i): lyrics on chord notes
- **Graced notes** (61f): lyrics skipping grace notes
- **Name/Number references** (61g): named lyric stanzas
- **Beam/melisma interactions** (61h): syllable spanning beamed groups
- **Elisions** (61j): syllable elision marks (dashes/underscores)
- **Spanners/Extenders** (61k): lyric line connectors between measures

---

## 6. Rendering Infrastructure: ✅ PRESENT BUT UNUSED

While lyrics are not implemented, **the infrastructure to render text to the canvas exists**:

### VexRenderingContext Text Methods

**File:** [android/NoteWise/app/src/main/java/dev/pola/vexflow/core/VexRenderingContext.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/core/VexRenderingContext.kt)

Available methods:
```kotlin
open fun fillText(text: String, x: Float, y: Float) {
    _canvas?.drawText(text, x, y, textPaint)
}

open fun measureText(text: String): Float = textPaint.measureText(text)

fun setFontSize(sizePx: Float) {
    textPaint.textSize = sizePx
}
```

The `textPaint` is a pre-configured `Paint` object with:
- `ANTI_ALIAS_FLAG` for smooth rendering
- `Color.BLACK` foreground (changeable via `fillColor` property)
- Default `textSize = 14f` (adjustable)
- `Paint.Style.FILL` for solid text

**This capability could be used to implement lyric rendering**, but no lyric element class currently uses it.

---

## 7. Project Scope & Development Phase

### Milestone Status

From [AGENT_PROGRESS.md](android/docs/AGENT_PROGRESS.md):

```
Milestone Board
- M0: DONE
- M1: DONE
- M2: DONE
- M3: DONE
- M4: DONE
- M5: DONE
- M6: DONE
- M7: DONE
- M8: DONE
- M9: DONE
- M10: IN_PROGRESS (REOPENED)
- M11: NOT_STARTED
- M12: NOT_STARTED
...
```

The implementation spec (Phase A) covers:
- M0–M10: Core rendering, parsing, layout, and file import
- M11: Polish/UX (zoom, dark mode, error handling)
- M12–M20: Playback and practice features

**Lyrics are not mentioned in any Phase A milestone.** They appear to be deferred to Phase B or identified as a future enhancement.

### What _Is_ Supported in Phase A

The MusicSheet parser and VF converter successfully handle:
- ✅ Pitches (notes, rests, chords)
- ✅ Clefs (treble, bass, alto)
- ✅ Key signatures
- ✅ Time signatures
- ✅ Accidentals (sharp, flat, natural, double-sharp, flat-flat)
- ✅ Ties (start, end, resolving across staves)
- ✅ Slurs (start, end)
- ✅ Beams (begin, continue, end)
- ✅ Tempos (from `<sound tempo="bpm">`)
- ✅ Multi-voice, multi-staff layouts
- ✅ System line breaks and pagination

---

## 8. Summary: Lyrics Readiness

| Layer | Status | Evidence |
|-------|--------|----------|
| **MusicXML Parsing** | ❌ Not implemented | No `<lyric>` case in `parseNote()` function |
| **Data Model** | ❌ Not present | No `lyrics` field in `NoteData` |
| **VF Conversion** | ❌ Not implemented | No lyric handling in `MusicSheetToVF` |
| **Rendering Element** | ❌ Not present | No `VFLyric` class or equivalent |
| **Canvas Text API** | ✅ Ready | `VexRenderingContext.fillText()` available |
| **Test Fixtures** | ⚠️ Present but failing | 13 test files with lyrics, not covered by gate |
| **Project Scope** | ⚠️ Deferred | Not part of Phase A milestones M0–M11 |

---

## 9. Implementation Roadmap (If Lyrics Were to Be Added)

### Prerequisite Phases

1. **Parser Enhancement** (2–3 hours)
   - Add `<lyric>` parsing in `MusicXMLParser.parseNote()`
   - Create `LyricData` class (text, syllabic, number, extend/elision flags)
   - Store in `NoteData` as `lyrics: List<LyricData>`

2. **Data Model** (1 hour)
   - Extend `NoteData` to include `lyrics: List<LyricData>`
   - Handle multi-stanza (multiple `<lyric number="1,2,3">` on same note)

3. **VF Conversion** (2–3 hours)
   - Create `VFLyric` element class with positioning logic
   - Add lyric-rendering pass in `convertMeasure()` after note layout
   - Position lyrics below staves (or above, per voice)
   - Handle syllable spanning (melisma)

4. **Rendering** (3–4 hours)
   - Implement `VFLyric.draw(context)` using `fillText()`
   - Font sizing, baseline alignment, spacing rules
   - Extend/elision marks (underscore connectors, dash breaks)

5. **Visual Testing** (1–2 hours)
   - Re-enable 13 lyrics test fixtures in visual suite
   - Reference against LilyPond golden (from alphaTab suite)

**Estimated total: 9–13 hours of development.**

---

## Conclusion

**Lyrics are intentionally not supported in the current NoteWise implementation.** They fall outside the Phase A scope, which focuses on core notation rendering (notes, rests, clefs, beams, ties, slurs, layout). The infrastructure for text rendering exists, but the full pipeline—parsing, modeling, conversion, and layout—is not implemented.

The 13 lyrics test fixtures in the visual test suite are **infrastructure-ready but feature-not-implemented**. They serve as placeholders for future development.
