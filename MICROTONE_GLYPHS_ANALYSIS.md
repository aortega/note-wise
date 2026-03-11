# Microtone Accidental Glyphs in Bravura SMuFL

## Summary

The Bravura font contains comprehensive support for microtone accidentals through the SMuFL standard. Below is a detailed analysis of available microtone glyphs and their current implementation status in the VexFlowRenderer codebase.

---

## Available Microtone Accidental Glyphs

### Half-Tone (Semi-Tone) Accidentals

| Glyph Name | Unicode Codepoint | Description |
|---|---|---|
| `accidentalHalfSharpArrowUp` | **U+E299** | Half sharp (↑ arrow variant) |
| `accidentalHalfSharpArrowDown` | **U+E29A** | Half sharp (↓ arrow variant) |
| `accidentalOneAndAHalfSharpsArrowUp` | **U+E29B** | One-and-a-half sharps (↑ arrow) |
| `accidentalOneAndAHalfSharpsArrowDown` | **U+E29C** | One-and-a-half sharps (↓ arrow) |

**Note**: No dedicated half-flat accidal exists in Bravura. Only half-sharp and one-and-a-half-sharp arrow variants are available.

---

### Quarter-Tone Accidentals

#### Equal-Tempered Quarter-Tones (Generic)
| Glyph Name | Unicode Codepoint | Description |
|---|---|---|
| `accidentalQuarterFlatEqualTempered` | **U+E2F5** | Lower by one equal-tempered quarter-tone |
| `accidentalQuarterSharpEqualTempered` | **U+E2F6** | Raise by one equal-tempered quarter-tone |

#### Composer-Specific Quarter-Tone Variants
| Glyph Name | Unicode Codepoint | Description |
|---|---|---|
| `accidentalOneQuarterToneFlatStockhausen` | **U+ED59** | Quarter-tone flat (Stockhausen) |
| `accidentalOneQuarterToneSharpStockhausen` | **U+ED58** | Quarter-tone sharp (Stockhausen) |
| `accidentalOneQuarterToneFlatFerneyhough` | **U+E275** | Quarter-tone flat (Ferneyhough) |
| `accidentalOneQuarterToneSharpFerneyhough` | **U+E276** | Quarter-tone sharp (Ferneyhough) |
| `accidentalQuarterToneFlatPenderecki` | **U+E277** | Quarter-tone flat (Penderecki) |
| `accidentalQuarterToneSharpStein` | **U+E278** | Quarter-tone sharp (Stein) |
| `accidentalQuarterToneFlatArrowUp` | **U+E272** | Quarter-tone flat (↑ arrow) |
| `accidentalQuarterToneSharpArrowDown` | **U+E273** | Quarter-tone sharp (↓ arrow) |

---

### Three-Quarter-Tone Accidentals

| Glyph Name | Unicode Codepoint | Description |
|---|---|---|
| `accidentalThreeQuarterTonesFlatArrowDown` | **U+E271** | Three-quarter-tones flat (↓ arrow) |
| `accidentalThreeQuarterTonesFlatArrowUp` | **U+E272** | Three-quarter-tones flat (↑ arrow) |
| `accidentalThreeQuarterTonesSharpArrowUp` | **U+E274** | Three-quarter-tones sharp (↑ arrow) |
| `accidentalThreeQuarterTonesSharpArrowDown` | **U+E273** | Three-quarter-tones sharp (↓ arrow) |

---

## Current Implementation Status

### Existing Accidental Support

The codebase currently supports **standard accidentals only**:

**File**: [VFTables.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFTables.kt)

```kotlin
const val GLYPH_ACCIDENTAL_SHARP:        Int = 0xE262  // accidentalSharp
const val GLYPH_ACCIDENTAL_FLAT:         Int = 0xE260  // accidentalFlat
const val GLYPH_ACCIDENTAL_NATURAL:      Int = 0xE261  // accidentalNatural
const val GLYPH_ACCIDENTAL_DOUBLE_SHARP: Int = 0xE263  // accidentalDoubleSharp
const val GLYPH_ACCIDENTAL_DOUBLE_FLAT:  Int = 0xE264  // accidentalDoubleFlat
```

**File**: [VFAccidental.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFAccidental.kt)

```kotlin
enum class AccidentalType(val codepoint: Int, val glyphName: String) {
    SHARP(VFTables.GLYPH_ACCIDENTAL_SHARP, "accidentalSharp"),
    FLAT(VFTables.GLYPH_ACCIDENTAL_FLAT, "accidentalFlat"),
    NATURAL(VFTables.GLYPH_ACCIDENTAL_NATURAL, "accidentalNatural"),
    DOUBLE_SHARP(VFTables.GLYPH_ACCIDENTAL_DOUBLE_SHARP, "accidentalDoubleSharp"),
    DOUBLE_FLAT(VFTables.GLYPH_ACCIDENTAL_DOUBLE_FLAT, "accidentalDoubleFlat");
}
```

### Missing Microtone Support

**❌ No mapping** exists in the codebase for:
- Half-sharp accidentals
- Quarter-flat/sharp accidentals (in any variant)
- Three-quarter-flat/sharp accidentals

---

## Recommended Implementation

### Step 1: Add Microtone Glyph Constants to VFTables

```kotlin
// Microtone accidentals
const val GLYPH_ACCIDENTAL_HALF_SHARP_UP:           Int = 0xE299  // accidentalHalfSharpArrowUp
const val GLYPH_ACCIDENTAL_HALF_SHARP_DOWN:         Int = 0xE29A  // accidentalHalfSharpArrowDown
const val GLYPH_ACCIDENTAL_QUARTER_FLAT_ET:         Int = 0xE2F5  // accidentalQuarterFlatEqualTempered
const val GLYPH_ACCIDENTAL_QUARTER_SHARP_ET:        Int = 0xE2F6  // accidentalQuarterSharpEqualTempered
const val GLYPH_ACCIDENTAL_QUARTER_FLAT_STOCKHAUSEN: Int = 0xED59 // accidentalOneQuarterToneFlatStockhausen
const val GLYPH_ACCIDENTAL_QUARTER_SHARP_STOCKHAUSEN: Int = 0xED58 // accidentalOneQuarterToneSharpStockhausen
const val GLYPH_ACCIDENTAL_THREE_QUARTER_FLAT_DOWN:  Int = 0xE271 // accidentalThreeQuarterTonesFlatArrowDown
const val GLYPH_ACCIDENTAL_THREE_QUARTER_SHARP_UP:   Int = 0xE274 // accidentalThreeQuarterTonesSharpArrowUp
```

### Step 2: Extend AccidentalType Enum in VFAccidental

```kotlin
enum class AccidentalType(val codepoint: Int, val glyphName: String) {
    // ... existing types ...
    
    // Microtone accidentals
    HALF_SHARP(VFTables.GLYPH_ACCIDENTAL_HALF_SHARP_UP, "accidentalHalfSharpArrowUp"),
    QUARTER_FLAT(VFTables.GLYPH_ACCIDENTAL_QUARTER_FLAT_ET, "accidentalQuarterFlatEqualTempered"),
    QUARTER_SHARP(VFTables.GLYPH_ACCIDENTAL_QUARTER_SHARP_ET, "accidentalQuarterSharpEqualTempered"),
    THREE_QUARTER_FLAT(VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_FLAT_DOWN, "accidentalThreeQuarterTonesFlatArrowDown"),
    THREE_QUARTER_SHARP(VFTables.GLYPH_ACCIDENTAL_THREE_QUARTER_SHARP_UP, "accidentalThreeQuarterTonesSharpArrowUp"),
}
```

### Step 3: Update the fromString Method

```kotlin
companion object {
    fun fromString(s: String): AccidentalType? = when (s) {
        // ... existing mappings ...
        "h" -> HALF_SHARP                  // Half sharp
        "q" -> QUARTER_SHARP               // Quarter sharp
        "qb" -> QUARTER_FLAT               // Quarter flat
        "t" -> THREE_QUARTER_SHARP         // Three-quarter sharp
        "tb" -> THREE_QUARTER_FLAT         // Three-quarter flat
        else -> null
    }
}
```

---

## Bounding Box Metadata Status

The extracted glyph bounding boxes file (`extracted_glyph_bboxes.json`) currently contains:
- ✅ Standard accidental glyphs (sharp, flat, natural, double-sharp, double-flat)
- ❌ Microtone accidental glyphs (not yet extracted)

To support microtone rendering with proper positioning, these glyphs need to be added to the bounding box extraction. Run the Bravura metadata analysis tool to extract bbox data for:
- `accidentalHalfSharpArrowUp`
- `accidentalQuarterFlatEqualTempered`
- `accidentalQuarterSharpEqualTempered`
- `accidentalThreeQuarterTonesFlatArrowDown`
- `accidentalThreeQuarterTonesSharpArrowUp`

---

## Microtone File Naming Convention Notes

The SMuFL spec uses the following naming for microtone accidentals:
- **Arrow variants** (↑↓): Used for generic representations when composer/tuning system is unspecified
- **Composer-specific**: Named for specific composers (Ferneyhough, Penderecki, Stockhausen, Stein, etc.)
- **Tuning systems**: "EqualTempered" for generic quarter-tone divisions

For the most broad compatibility, use the "EqualTempered" variants or arrow-notated versions rather than composer-specific glyphs.

---

## References

- **SMuFL Metadata**: [android/reference/smufl/metadata/glyphnames.json](android/reference/smufl/metadata/glyphnames.json)
- **Bravura Metadata**: [android/bravura/bravura_metadata.json](android/bravura/bravura_metadata.json)  
- **Current Accidental Implementation**: [android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFAccidental.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFAccidental.kt)
- **Glyph Tables**: [android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFTables.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/model/VFTables.kt)

