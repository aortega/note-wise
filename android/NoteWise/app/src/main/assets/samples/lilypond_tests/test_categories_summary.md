# LilyPond Test Categories Summary

Use this file to prioritize MusicXML coverage for NoteWise rendering.

## Reference Policy

- Source of truth: `test_metadata.json`
- Required keys per reference entry: `title`, `xml_filename`, `image_filename`
- If `xml_filename` or `image_filename` is missing, the entry is treated as no-reference and excluded from reference comparison.

## Priority Tiers

### P0 - Core Notation (must pass first)

- `01*` Pitches and accidentals
- `02*` Rests and durations
- `03*` Rhythm, divisions, backup/forward
- `11*` Time signatures
- `12*` Clefs
- `13*` Key signatures
- `21*` Basic chords
- `23*` Tuplets
- `33b`, `33c`, `33i` ties and slurs essentials
- `46*` Barlines and measure structure

Why this matters:
- These categories drive most day-to-day score readability in the app.

### P1 - High-Value Layout and Multi-Stave

- `14*` Staff details and line changes
- `41*` Multi-part ordering and grouping
- `42*` Multi-voice handling
- `43*` Piano and multi-staff behavior
- `45*` Repeats and endings
- `52*` Page layout and breaks
- `61*` Lyrics alignment and melisma

Why this matters:
- These scenarios appear frequently in real-world scores and strongly impact user trust.

### P2 - Advanced and Edge Engraving

- `22*` Notehead styles and variants
- `24*` Grace-note variants
- `31*` Direction combinations
- `32*` Notation detail combinations
- `33a`, `33d*`, `33e`, `33f`, `33h`, `33j` complex spanners
- `34*` Print-object, color, and font-size attributes
- `71*`, `72*`, `73*`, `74*`, `75*` specialized domains (chord names, transposing instruments, percussion, figured bass, accordion)
- `90*`, `99*` compressed and interoperability cases

Why this matters:
- Important for completeness and interoperability, but lower impact for baseline app correctness.

## Current Tier-1 App Focus

- `11a-TimeSignatures.xml`
- `12aa-Clefs_Pitch_Traditional.xml`
- `13a-KeySignatures.xml`

These sentinel tests are useful for regression detection, but should be expanded with P0 rhythm/rest/chord coverage.

## Suggested Next Additions (P0)

- `01a-Pitches-Pitches.xml`
- `02a-Rests-Durations.xml`
- `03a-Rhythm-Durations.xml`
- `21a-Chord-Basic.xml`
- `46a-Barlines.xml`
