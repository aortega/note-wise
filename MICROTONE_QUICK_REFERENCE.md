# Microtone Accidental Quick Reference

## Requested Glyphs

| Type | SMuFL Glyph Name | Unicode Codepoint | Available in Bravura |
|---|---|---|---|
| **Half-flat** | ❌ Not defined | — | ❌ No |
| **Half-sharp** | `accidentalHalfSharpArrowUp` | **U+E299** | ✅ Yes |
| **Quarter-flat** | `accidentalQuarterFlatEqualTempered` | **U+E2F5** | ✅ Yes |
| **Quarter-sharp** | `accidentalQuarterSharpEqualTempered` | **U+E2F6** | ✅ Yes |
| **Three-quarter-flat** | `accidentalThreeQuarterTonesFlatArrowDown` | **U+E271** | ✅ Yes |
| **Three-quarter-sharp** | `accidentalThreeQuarterTonesSharpArrowUp` | **U+E274** | ✅ Yes |

---

## Key Findings

### 1. **Half-flat Accidental: NOT AVAILABLE**
SMuFL does not define a standard `accidentalHalfFlat` glyph. Only half-sharp variants exist in Bravura.

**Alternatives:**
- Use `accidentalHalfSharpArrowDown` (U+E299) rotated/mirrored
- Combine flat + down-arrow accent
- Use natural + quarter-flat combination

---

### 2. **Available Half-Tone Glyphs**
```
accidentalHalfSharpArrowUp:       U+E299  ✅
accidentalHalfSharpArrowDown:     U+E29A  ✅
accidentalOneAndAHalfSharpsArrowUp:   U+E29B  ✅
accidentalOneAndAHalfSharpsArrowDown: U+E29C  ✅
```

---

### 3. **Available Quarter-Tone Glyphs**

**Generic (Equal-Tempered):**
```
accidentalQuarterFlatEqualTempered:   U+E2F5  ✅
accidentalQuarterSharpEqualTempered:  U+E2F6  ✅
```

**Arrow Variants:**
```
accidentalQuarterToneFlatArrowUp:     U+E272  ✅
accidentalQuarterToneSharpArrowDown:  U+E273  ✅
```

**Composer-Specific:**
```
accidentalOneQuarterToneFlatStockhausen:  U+ED59  ✅
accidentalOneQuarterToneSharpStockhausen: U+ED58  ✅
accidentalOneQuarterToneFlatFerneyhough:  U+E275  ✅
accidentalOneQuarterToneSharpFerneyhough: U+E276  ✅
```

---

### 4. **Available Three-Quarter-Tone Glyphs**
```
accidentalThreeQuarterTonesFlatArrowDown:  U+E271  ✅
accidentalThreeQuarterTonesFlatArrowUp:    U+E272  ✅
accidentalThreeQuarterTonesSharpArrowDown: U+E273  ✅
accidentalThreeQuarterTonesSharpArrowUp:   U+E274  ✅
```

Plus composer-specific variants (Couper, Grisey, Tartini, Zimmermann, Busotti, Stein, Stockhausen)

---

## Current Codebase Status

### Mapped ✅
- Standard accidentals (sharp, flat, natural, double-sharp, double-flat)
- See: [VFAccidental.kt](android/NoteWise/app/src/main/java/dev/pola/vexflow/elements/VFAccidental.kt)

### NOT Mapped ❌
- **All microtone accidentals** (half, quarter, three-quarter variants)
- Must be added to `VFTables.kt` and `VFAccidental.kt` enum

### Bounding Boxes 📦
- Standard accidentals: ✅ Extracted
- Microtone accidentals: ❌ Not yet extracted (would need to run metadata extraction tool)

---

## Recommended Unicode Format for Annotations

| Glyph | Format | Example |
|---|---|---|
| Half-sharp | `U+E299` | ♯½ |
| Quarter-flat | `U+E2F5` | ♭¼ |
| Quarter-sharp | `U+E2F6` | ♯¼ |
| Three-quarter-flat | `U+E271` | ♭¾ |
| Three-quarter-sharp | `U+E274` | ♯¾ |

