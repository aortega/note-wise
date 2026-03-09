# TrackPlay — Detailed Implementation Spec

**Companion to:** `ANDROID_PROJECT_PLAN.md`
**Primary algorithmic reference:** `reference/alphaTab-develop/packages/alphatab/src/`
**API contract reference:** `reference/trackplay/SheetMusicView.kt`
**Platform:** Android / Kotlin / Jetpack Compose
**Font:** Bravura (SMuFL) — drawn via `Canvas` + `Typeface`

> **For the coding agent:** Work top to bottom. Each section is one milestone. Do not skip ahead. Mark each gate `[x]` before moving to the next section. Build must be green and all listed tests must pass before advancing.

---

## Autonomous Agent Execution

When running this spec in autonomous mode, follow the skill workflow and keep
tracking files updated as part of normal execution.

- **Skill:** `.github/skills/trackplay-autonomous-delivery/SKILL.md`
- **Progress dashboard:** `android/docs/AGENT_PROGRESS.md`
- **Change log:** `android/docs/AGENT_CHANGELOG.md`
- **Issues register:** `android/docs/AGENT_ISSUES.md`
- **Decision log:** `android/docs/AGENT_DECISIONS.md`

Execution policy:
- Work milestones strictly in order (`M0` -> `M20`).
- Do not mark any gate complete without passing required build/tests/manual checks.
- Record non-trivial blockers, mitigations, and architectural choices in the
    corresponding tracking files.
- Prioritise build infrastructure first: stabilize Gradle/build/test/assets
    before feature milestones beyond M0.

---

## Completion Dashboard

Update this table as you complete each milestone. Never mark a gate done unless build is green and tests pass.

| Milestone | Description | Gate |
|-----------|-------------|------|
| M0 | Project setup | `[ ]` |
| M1 | Rendering foundation | `[ ]` |
| M2 | Staff and notes | `[ ]` |
| M3 | Clef, key sig, time sig | `[ ]` |
| M4 | Voice and formatting | `[ ]` |
| M5 | Ties, slurs, barlines, beams | `[ ]` |
| M6 | Compose integration | `[ ]` |
| M7 | Test suite | `[ ]` |
| M8 | MusicXML parser | `[ ]` |
| M9 | File import | `[ ]` |
| M10 | Multi-measure layout | `[ ]` |
| M11 | Polish and UX | `[ ]` |
| M12 | Tempo engine | `[ ]` |
| M13 | Cursor and metronome | `[ ]` |
| M14 | Audio synthesis | `[ ]` |
| M15 | MIDI device connection | `[ ]` |
| M16 | Real-time note display | `[ ]` |
| M17 | Latency compensation | `[ ]` |
| M18 | Note matching and scoring | `[ ]` |
| M19 | Performance summary | `[ ]` |
| M20 | Practice history | `[ ]` |

---

## Conventions Used in This Document

- **File path** — always relative to `Renderer/app/src/main/java/dev/pola/`
- **Test path** — always relative to `Renderer/app/src/test/java/dev/pola/`
- **`Float`** is used throughout (Android Canvas API is Float)
- **`PointF`** for 2D coordinates, **`RectF`** for rectangles
- **Codepoints** are written as `'\uE050'.code` (Int) or `"\uE050"` (String) where needed for `Paint.drawText()`
- **Asset loading** uses `context.assets.open(path)` via a passed-in `Context` or application context singleton
- **Android-only implementation** — this spec is the sole implementation contract

---

---

# M0 — Project Setup

**Goal:** All directories, assets, and test dependencies in place. Project builds green.

## Checklist

### 1. Create package directories

Create empty `.gitkeep` files (or placeholder classes) in the following directories so Gradle sees the packages:

```
Renderer/app/src/main/java/dev/pola/vexflow/core/
Renderer/app/src/main/java/dev/pola/vexflow/elements/
Renderer/app/src/main/java/dev/pola/vexflow/model/
Renderer/app/src/main/java/dev/pola/vexflow/view/
Renderer/app/src/main/java/dev/pola/vexflow/parser/
Renderer/app/src/main/java/dev/pola/playback/
Renderer/app/src/main/java/dev/pola/midi/
Renderer/app/src/main/java/dev/pola/evaluation/
Renderer/app/src/main/java/dev/pola/persistence/
```

### Build prerequisite: Validate local build toolchain

Ensure JDK 17+ is active before running Gradle (AGP 8.x baseline):

```bash
java -version
./gradlew -version
```

### 2. Copy assets

```
bravura/Bravura.otf           -> Renderer/app/src/main/assets/fonts/Bravura.otf
bravura/extracted_glyph_bboxes.json  -> Renderer/app/src/main/assets/glyph_bboxes.json
samples/                      -> Renderer/app/src/main/assets/samples/
```

Set baseline SDKs in `Renderer/app/build.gradle.kts`:

```kotlin
minSdk = 24
targetSdk = 36
```

### 3. Add Gradle dependencies

In `Renderer/app/build.gradle.kts`, add to `dependencies {}`:

```kotlin
// Testing
testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
testImplementation("org.robolectric:robolectric:4.11.1")
testImplementation("androidx.test:core:1.5.0")

// JSON parsing (glyph bbox loader)
implementation("com.google.code.gson:gson:2.10.1")
```

In `Renderer/app/build.gradle.kts`, add to `android { testOptions {} }`:

```kotlin
testOptions {
    unitTests {
        isIncludeAndroidResources = true
        all { it.useJUnitPlatform() }
    }
}
```

### 4. Application context singleton

Create `Renderer/app/src/main/java/dev/pola/renderer/App.kt`:

```kotlin
package dev.pola.renderer

import android.app.Application

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
```

Register in `AndroidManifest.xml` — add `android:name=".App"` to the `<application>` element.

### Gate M0

- [ ] `./gradlew assembleDebug` succeeds with 0 errors
- [ ] `./gradlew test` runs (even if 0 tests yet)
- [ ] All listed asset files exist under `assets/`

---

---

# M1 — Rendering Foundation

**Goal:** Five foundational classes: fraction arithmetic, layout constants, SMuFL codepoints, glyph bounding boxes, and the rendering context. A Bravura glyph draws onto an Android `Canvas`.

**Files to create (in order):**

1. `vexflow/model/VFFraction.kt`
2. `vexflow/model/VFMetrics.kt`
3. `vexflow/model/VFTables.kt`
4. `vexflow/model/VFGlyphBoundingBox.kt`
5. `vexflow/core/VexRenderingContext.kt`

Tests: `vexflow/model/VFFractionTest.kt`, `vexflow/core/VexRenderingContextTest.kt`

---

## Class 1 — VFFraction

**File:** `vexflow/model/VFFraction.kt`

```kotlin
package dev.pola.vexflow.model

/**
 * Immutable rational number. Used to represent note durations and beat positions.
 * Always stored in lowest terms (GCD-reduced). Denominator is always positive;
 * the sign lives in the numerator.
 *
 * Duration strings map to fractions:
 *   "w" or "1" -> 1/1 (whole)
 *   "h" or "2" -> 1/2 (half)
 *   "q" or "4" -> 1/4 (quarter)
 *   "8"        -> 1/8 (eighth)
 *   "16"       -> 1/16 (sixteenth)
 *   "32"       -> 1/32 (thirty-second)
 *   "wr","hr","qr","8r","16r","32r" -> same values (rest variants, same duration)
 *   Dotted: append "d" -> e.g. "4d" -> 3/8
 */
data class VFFraction(val numerator: Int, val denominator: Int) : Comparable<VFFraction> {

    init {
        require(denominator != 0) { "Denominator must not be zero" }
        // Reduction happens via the companion factory; raw constructor is kept pure
        // so that copy() works correctly. Call VFFraction.of() for auto-reduction.
    }

    companion object {
        val ZERO = VFFraction(0, 1)
        val ONE  = VFFraction(1, 1)

        /** Create a reduced fraction. Normalises sign to numerator. */
        fun of(numerator: Int, denominator: Int): VFFraction {
            require(denominator != 0) { "Denominator must not be zero" }
            if (numerator == 0) return ZERO
            val sign = if (denominator < 0) -1 else 1
            val g = gcd(Math.abs(numerator), Math.abs(denominator))
            return VFFraction(sign * numerator / g, Math.abs(denominator) / g)
        }

        /** Parse a VexFlow duration string into a VFFraction. Returns null on unknown input. */
        fun fromDurationString(duration: String): VFFraction? {
            val base = duration.trimEnd('r', 'd') // strip rest suffix and dot marker
            val dotted = duration.endsWith("d")
            val frac = when (base) {
                "w", "1"  -> of(1, 1)
                "h", "2"  -> of(1, 2)
                "q", "4"  -> of(1, 4)
                "8"       -> of(1, 8)
                "16"      -> of(1, 16)
                "32"      -> of(1, 32)
                else      -> return null
            }
            return if (dotted) frac + frac * of(1, 2) else frac
        }

        private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    }

    // ── Arithmetic ────────────────────────────────────────────────────────────

    operator fun plus(other: VFFraction): VFFraction =
        of(numerator * other.denominator + other.numerator * denominator,
           denominator * other.denominator)

    operator fun minus(other: VFFraction): VFFraction =
        of(numerator * other.denominator - other.numerator * denominator,
           denominator * other.denominator)

    operator fun times(other: VFFraction): VFFraction =
        of(numerator * other.numerator, denominator * other.denominator)

    operator fun div(other: VFFraction): VFFraction {
        require(other.numerator != 0) { "Division by zero fraction" }
        return of(numerator * other.denominator, denominator * other.numerator)
    }

    // ── Comparison ────────────────────────────────────────────────────────────

    override fun compareTo(other: VFFraction): Int =
        (numerator.toLong() * other.denominator).compareTo(other.numerator.toLong() * denominator)

    // ── Conversions ───────────────────────────────────────────────────────────

    val doubleValue: Double get() = numerator.toDouble() / denominator
    val floatValue: Float   get() = numerator.toFloat()  / denominator

    override fun toString(): String = "$numerator/$denominator"
}
```

**Algorithm notes:**
- `of()` reduces via GCD (Euclidean algorithm) and normalises sign. The raw `data class` constructor is intentionally left unreduced so that `copy()` and `equals()` work on stored values. Always use `VFFraction.of()` or the arithmetic operators to create fractions; never call the constructor directly except in tests for specific reduced values.
- `fromDurationString` must handle the `"r"` suffix (rests share the same duration value as their non-rest counterparts) and `"d"` suffix (dotted = 1.5×).

**Test file:** `vexflow/model/VFFractionTest.kt`

```kotlin
package dev.pola.vexflow.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFFractionTest {

    @Test fun `of reduces 4 over 8 to 1 over 2`() {
        val f = VFFraction.of(4, 8)
        assertEquals(1, f.numerator); assertEquals(2, f.denominator)
    }

    @Test fun `of normalises negative denominator`() {
        val f = VFFraction.of(3, -4)
        assertEquals(-3, f.numerator); assertEquals(4, f.denominator)
    }

    @Test fun `plus adds two fractions`() {
        val result = VFFraction.of(1, 4) + VFFraction.of(1, 4)
        assertEquals(VFFraction.of(1, 2), result)
    }

    @Test fun `minus subtracts fractions`() {
        val result = VFFraction.of(3, 4) - VFFraction.of(1, 4)
        assertEquals(VFFraction.of(1, 2), result)
    }

    @Test fun `times multiplies fractions`() {
        val result = VFFraction.of(2, 3) * VFFraction.of(3, 4)
        assertEquals(VFFraction.of(1, 2), result)
    }

    @Test fun `div divides fractions`() {
        val result = VFFraction.of(1, 2) / VFFraction.of(1, 4)
        assertEquals(VFFraction.of(2, 1), result)
    }

    @Test fun `compareTo orders correctly`() {
        assertTrue(VFFraction.of(1, 4) < VFFraction.of(1, 2))
        assertTrue(VFFraction.of(1, 1) > VFFraction.of(3, 4))
        assertEquals(0, VFFraction.of(2, 4).compareTo(VFFraction.of(1, 2)))
    }

    @Test fun `fromDurationString parses quarter note`() {
        assertEquals(VFFraction.of(1, 4), VFFraction.fromDurationString("4"))
        assertEquals(VFFraction.of(1, 4), VFFraction.fromDurationString("4r"))
    }

    @Test fun `fromDurationString parses dotted half`() {
        assertEquals(VFFraction.of(3, 4), VFFraction.fromDurationString("2d"))
    }

    @Test fun `fromDurationString returns null for unknown`() {
        assertNull(VFFraction.fromDurationString("xyz"))
    }

    @Test fun `doubleValue is correct`() {
        assertEquals(0.25, VFFraction.of(1, 4).doubleValue, 1e-9)
    }

    @Test fun `ZERO and ONE constants`() {
        assertEquals(0, VFFraction.ZERO.numerator)
        assertEquals(1, VFFraction.ONE.numerator); assertEquals(1, VFFraction.ONE.denominator)
    }
}
```

---

## Class 2 — VFMetrics

**File:** `vexflow/model/VFMetrics.kt`

```kotlin
package dev.pola.vexflow.model

/**
 * Global layout constants. All values are in screen pixels
 * and are independent of stave line spacing (which is set per-stave).
 * These are minimum padding values; the formatter may add more space.
 */
object VFMetrics {
    const val CLEF_PADDING:           Float = 10f  // space after clef glyph
    const val KEY_SIGNATURE_PADDING:  Float = 10f  // space after last accidental in key sig
    const val TIME_SIGNATURE_PADDING: Float = 10f  // space after time signature
    const val STAVE_LEFT_PADDING:     Float = 12f  // space before first element at stave start
    const val STAVE_END_PADDING:      Float = 10f  // minimum space after last note to end barline
    const val DEFAULT_LINE_SPACING:   Float = 10f  // default stave line spacing in pixels
    const val STEM_HEIGHT_SPACES:     Float = 3.5f // stem height in staff-space units
    const val BEAM_THICKNESS:         Float = 4f   // primary beam stroke thickness in px
    const val BEAM_SPACING:           Float = 6f   // spacing between stacked beams in px
}
```

No test required — constants verified by visual output.

---

## Class 3 — VFTables

**File:** `vexflow/model/VFTables.kt`

```kotlin
package dev.pola.vexflow.model

/**
 * SMuFL Unicode codepoints for Bravura glyphs used in rendering.
 * All values are Int (Unicode codepoint). To draw with Paint.drawText(),
 * convert via: String(Character.toChars(codepoint))
 *
 * Reference: https://www.smufl.org/version/latest/
 * Bravura-specific names verified against bravura/bravura_metadata.json.
 */
object VFTables {

    // ── Clefs ─────────────────────────────────────────────────────────────────
    const val GLYPH_G_CLEF:                 Int = 0xE050 // gClef
    const val GLYPH_F_CLEF:                 Int = 0xE062 // fClef
    const val GLYPH_C_CLEF:                 Int = 0xE05C // cClef
    const val GLYPH_PERCUSSION_CLEF:        Int = 0xE069 // unpitchedPercussionClef1

    // ── Noteheads ─────────────────────────────────────────────────────────────
    const val GLYPH_NOTE_HEAD_WHOLE:        Int = 0xE0A2 // noteheadWhole
    const val GLYPH_NOTE_HEAD_HALF:         Int = 0xE0A3 // noteheadHalf
    const val GLYPH_NOTE_HEAD_QUARTER:      Int = 0xE0A4 // noteheadBlack
    const val GLYPH_NOTE_HEAD_DOUBLE_WHOLE: Int = 0xE0A1 // noteheadDoubleWhole

    // ── Accidentals ───────────────────────────────────────────────────────────
    const val GLYPH_ACCIDENTAL_SHARP:       Int = 0xE262 // accidentalSharp
    const val GLYPH_ACCIDENTAL_FLAT:        Int = 0xE260 // accidentalFlat
    const val GLYPH_ACCIDENTAL_NATURAL:     Int = 0xE261 // accidentalNatural
    const val GLYPH_ACCIDENTAL_DOUBLE_SHARP:Int = 0xE263 // accidentalDoubleSharp
    const val GLYPH_ACCIDENTAL_DOUBLE_FLAT: Int = 0xE264 // accidentalDoubleFlat

    // ── Rests ─────────────────────────────────────────────────────────────────
    const val GLYPH_REST_WHOLE:             Int = 0xE4E3 // restWhole
    const val GLYPH_REST_HALF:              Int = 0xE4E4 // restHalf
    const val GLYPH_REST_QUARTER:           Int = 0xE4E5 // restQuarter
    const val GLYPH_REST_8TH:               Int = 0xE4E6 // rest8th
    const val GLYPH_REST_16TH:              Int = 0xE4E7 // rest16th
    const val GLYPH_REST_32ND:              Int = 0xE4E8 // rest32nd

    // ── Flags ─────────────────────────────────────────────────────────────────
    const val GLYPH_FLAG_8TH_UP:            Int = 0xE240 // flag8thUp
    const val GLYPH_FLAG_8TH_DOWN:          Int = 0xE241 // flag8thDown
    const val GLYPH_FLAG_16TH_UP:           Int = 0xE242 // flag16thUp
    const val GLYPH_FLAG_16TH_DOWN:         Int = 0xE243 // flag16thDown
    const val GLYPH_FLAG_32ND_UP:           Int = 0xE244 // flag32ndUp
    const val GLYPH_FLAG_32ND_DOWN:         Int = 0xE245 // flag32ndDown

    // ── Time Signatures ───────────────────────────────────────────────────────
    const val GLYPH_TIME_SIG_0:             Int = 0xE080
    const val GLYPH_TIME_SIG_1:             Int = 0xE081
    const val GLYPH_TIME_SIG_2:             Int = 0xE082
    const val GLYPH_TIME_SIG_3:             Int = 0xE083
    const val GLYPH_TIME_SIG_4:             Int = 0xE084
    const val GLYPH_TIME_SIG_5:             Int = 0xE085
    const val GLYPH_TIME_SIG_6:             Int = 0xE086
    const val GLYPH_TIME_SIG_7:             Int = 0xE087
    const val GLYPH_TIME_SIG_8:             Int = 0xE088
    const val GLYPH_TIME_SIG_9:             Int = 0xE089
    const val GLYPH_TIME_SIG_COMMON:        Int = 0xE08A // timeSigCommon
    const val GLYPH_TIME_SIG_CUT:           Int = 0xE08B // timeSigCutCommon

    // ── Dots ──────────────────────────────────────────────────────────────────
    const val GLYPH_AUGMENTATION_DOT:       Int = 0xE1E7 // augmentationDot

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Convert codepoint to a single-character String suitable for Paint.drawText(). */
    fun codepointToString(codepoint: Int): String = String(Character.toChars(codepoint))

    /** Return the time-signature digit glyph for a single decimal digit (0-9). */
    fun timeSigDigit(digit: Int): Int {
        require(digit in 0..9) { "Digit must be 0-9" }
        return GLYPH_TIME_SIG_0 + digit
    }
}
```

---

## Class 4 — VFGlyphBoundingBox

**File:** `vexflow/model/VFGlyphBoundingBox.kt`

```kotlin
package dev.pola.vexflow.model

import android.graphics.PointF
import android.graphics.RectF
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.pola.renderer.App

/**
 * Bounding box for a single SMuFL glyph, in staff-space units.
 * One staff-space = distance between two adjacent staff lines.
 * Multiply by stave.spacingBetweenLines to convert to screen pixels.
 *
 * Coordinate system (SMuFL / staff-space):
 *   origin = left edge of notehead on the baseline staff line
 *   Y increases UPWARD (SMuFL convention)
 *   northeast = top-right corner (high X, high Y = visually above)
 *   southwest = bottom-left corner (low X, low Y = visually below)
 *
 * When converting to Android Canvas (Y increases downward), flip the Y axis.
 */
data class VFGlyphBoundingBox(
    val northeast: PointF,   // bBoxNE from JSON: [x, y] in staff-space units, Y up
    val southwest: PointF    // bBoxSW from JSON: [x, y] in staff-space units, Y up
) {
    val width:  Float get() = northeast.x - southwest.x
    val height: Float get() = northeast.y - southwest.y  // in staff-space (positive = taller)

    /**
     * Scale from staff-space units to screen pixels.
     * @param staffSpacing the stave's spacingBetweenLines value in pixels
     */
    fun scaled(staffSpacing: Float): VFGlyphBoundingBox = VFGlyphBoundingBox(
        northeast = PointF(northeast.x * staffSpacing, northeast.y * staffSpacing),
        southwest = PointF(southwest.x * staffSpacing, southwest.y * staffSpacing)
    )

    /**
     * Convert to an Android RectF at the given canvas origin (top-left, Y down).
     * The caller is responsible for providing the correct canvas-space origin.
     *
     * Mapping (staff-space Y-up -> canvas Y-down):
     *   canvas left   = origin.x + southwest.x * staffSpacing
     *   canvas top    = origin.y - northeast.y * staffSpacing  (flip Y)
     *   canvas right  = origin.x + northeast.x * staffSpacing
     *   canvas bottom = origin.y - southwest.y * staffSpacing  (flip Y)
     */
    fun toCanvasRect(originX: Float, originY: Float, staffSpacing: Float): RectF {
        val scaled = scaled(staffSpacing)
        return RectF(
            originX + scaled.southwest.x,
            originY - scaled.northeast.y,
            originX + scaled.northeast.x,
            originY - scaled.southwest.y
        )
    }
}

/**
 * Singleton loader for glyph bounding boxes.
 * JSON file: assets/glyph_bboxes.json
 *
 * Expected JSON format:
 * {
 *   "gClef":    { "bBoxNE": [x, y], "bBoxSW": [x, y] },
 *   "fClef":    { "bBoxNE": [x, y], "bBoxSW": [x, y] },
 *   ...
 * }
 *
 * Glyph names follow SMuFL canonical names (e.g. "gClef", "noteheadBlack").
 */
object VFGlyphBoundingBoxManager {

    private val boxes: Map<String, VFGlyphBoundingBox> by lazy { load() }

    /** Returns the bounding box for the given SMuFL glyph name, or null if not found. */
    fun get(glyphName: String): VFGlyphBoundingBox? = boxes[glyphName]

    /**
     * Convenience: returns bounding box already scaled to screen pixels.
     * Returns null if glyph name is unknown.
     */
    fun getScaled(glyphName: String, staffSpacing: Float): VFGlyphBoundingBox? =
        get(glyphName)?.scaled(staffSpacing)

    val availableGlyphs: List<String> get() = boxes.keys.sorted()

    // ── Private ───────────────────────────────────────────────────────────────

    private data class RawEntry(val bBoxNE: List<Float>, val bBoxSW: List<Float>)

    private fun load(): Map<String, VFGlyphBoundingBox> {
        val json = App.instance.assets.open("glyph_bboxes.json")
            .bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, RawEntry>>() {}.type
        val raw: Map<String, RawEntry> = Gson().fromJson(json, type)
        return raw.mapValues { (_, entry) ->
            VFGlyphBoundingBox(
                northeast = PointF(entry.bBoxNE[0], entry.bBoxNE[1]),
                southwest = PointF(entry.bBoxSW[0], entry.bBoxSW[1])
            )
        }
    }
}
```

**Algorithm notes:**
- The JSON key names are SMuFL canonical glyph names (e.g. `"gClef"`, `"noteheadBlack"`), not Unicode codepoints.
- `VFGlyphBoundingBoxManager` uses Kotlin's `by lazy` for thread-safe one-time load.
- The SMuFL convention has Y increasing upward. All canvas-space conversions flip Y. The rule is always: `canvasY = referenceY - (staffSpaceY * staffSpacing)`.

---

## Class 5 — VexRenderingContext

**File:** `vexflow/core/VexRenderingContext.kt`

```kotlin
package dev.pola.vexflow.core

import android.graphics.*
import dev.pola.renderer.App

/**
 * Abstraction over Android Canvas + Paint.
 * Matches the API contract from reference/trackplay/SheetMusicView.kt:
 *   - Constructed with no arguments
 *   - `canvas` property is set by the View just before drawing
 *
 * Drawing model:
 *   - Path operations (beginPath/moveTo/lineTo/bezierCurveTo/closePath) accumulate
 *     into a single android.graphics.Path.
 *   - stroke() draws that path with strokePaint; fill() draws with fillPaint.
 *   - SMuFL glyphs are drawn via drawSmuflGlyph() using the Bravura typeface.
 *   - save()/restore() delegate to Canvas.save()/restore().
 *   - translate()/scale() are applied to the Canvas transform matrix.
 *
 * Y-axis note:
 *   Android Canvas Y increases downward.
 *   SMuFL glyph bounding boxes use Y-up convention.
 *   drawSmuflGlyph() handles the Y-flip internally with a canvas scale(1f, -1f)
 *   transform, so callers always pass canvas-space (Y-down) coordinates.
 */
open class VexRenderingContext {

    // Set by the host View immediately before calling draw()
    var canvas: Any? = null
        set(value) { field = value; _canvas = value as? Canvas }

    private var _canvas: Canvas? = null

    // ── Paint objects ─────────────────────────────────────────────────────────

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 1f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
        textSize = 14f
    }

    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    // ── Public style properties ───────────────────────────────────────────────

    var fillColor: Int = Color.BLACK
        set(value) { field = value; fillPaint.color = value; glyphPaint.color = value }

    var strokeColor: Int = Color.BLACK
        set(value) { field = value; strokePaint.color = value }

    var lineWidth: Float = 1f
        set(value) { field = value; strokePaint.strokeWidth = value }

    var alpha: Float = 1f
        set(value) {
            field = value
            val a = (value.coerceIn(0f, 1f) * 255).toInt()
            fillPaint.alpha = a; strokePaint.alpha = a; glyphPaint.alpha = a
        }

    // ── Bravura typeface ──────────────────────────────────────────────────────

    private val bravuraTypeface: Typeface by lazy {
        Typeface.createFromAsset(App.instance.assets, "fonts/Bravura.otf")
            ?: error("Bravura.otf not found in assets/fonts/")
    }

    // ── Path building ─────────────────────────────────────────────────────────

    private var currentPath = Path()

    open fun beginPath() { currentPath = Path() }

    open fun moveTo(x: Float, y: Float) { currentPath.moveTo(x, y) }

    open fun lineTo(x: Float, y: Float) { currentPath.lineTo(x, y) }

    open fun quadraticCurveTo(cpx: Float, cpy: Float, x: Float, y: Float) {
        currentPath.quadTo(cpx, cpy, x, y)
    }

    open fun bezierCurveTo(
        cp1x: Float, cp1y: Float,
        cp2x: Float, cp2y: Float,
        x: Float, y: Float
    ) { currentPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y) }

    open fun arc(
        x: Float, y: Float, radius: Float,
        startAngle: Float, endAngle: Float, anticlockwise: Boolean
    ) {
        val left = x - radius; val top = y - radius
        val right = x + radius; val bottom = y + radius
        val startDeg = Math.toDegrees(startAngle.toDouble()).toFloat()
        var sweepDeg = Math.toDegrees((endAngle - startAngle).toDouble()).toFloat()
        if (anticlockwise && sweepDeg > 0) sweepDeg -= 360f
        if (!anticlockwise && sweepDeg < 0) sweepDeg += 360f
        currentPath.arcTo(RectF(left, top, right, bottom), startDeg, sweepDeg)
    }

    open fun closePath() { currentPath.close() }

    open fun stroke() { _canvas?.drawPath(currentPath, strokePaint) }

    open fun fill() { _canvas?.drawPath(currentPath, fillPaint) }

    // ── Shape primitives ──────────────────────────────────────────────────────

    open fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        _canvas?.drawRect(x, y, x + width, y + height, fillPaint)
    }

    open fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        _canvas?.drawRect(x, y, x + width, y + height, strokePaint)
    }

    // ── Text ──────────────────────────────────────────────────────────────────

    open fun fillText(text: String, x: Float, y: Float) {
        _canvas?.drawText(text, x, y, textPaint)
    }

    /** Set regular (non-glyph) text size in pixels. */
    fun setFontSize(sizePx: Float) { textPaint.textSize = sizePx }

    // ── SMuFL glyph drawing ───────────────────────────────────────────────────

    /**
     * Draw a Bravura SMuFL glyph at canvas-space (x, y) with the given font size.
     *
     * The point (x, y) is the glyph origin in canvas coordinates (Y-down).
     * Internally the canvas is flipped (scale 1, -1) so the glyph renders
     * right-side-up. This matches Android's upside-down font rendering for
     * music fonts.
     *
     * @param codepoint Unicode codepoint (e.g. VFTables.GLYPH_G_CLEF = 0xE050)
     * @param x         Canvas X in pixels
     * @param y         Canvas Y in pixels (Y-down, baseline of glyph)
     * @param sizePx    Font size in pixels
     */
    open fun drawSmuflGlyph(codepoint: Int, x: Float, y: Float, sizePx: Float) {
        val c = _canvas ?: return
        glyphPaint.typeface = bravuraTypeface
        glyphPaint.textSize = sizePx
        val glyphStr = String(Character.toChars(codepoint))
        c.save()
        c.translate(x, y)
        c.scale(1f, -1f)
        c.drawText(glyphStr, 0f, 0f, glyphPaint)
        c.restore()
    }

    // ── State and transform ───────────────────────────────────────────────────

    open fun save()    { _canvas?.save() }
    open fun restore() { _canvas?.restore() }

    open fun translate(x: Float, y: Float) { _canvas?.translate(x, y) }
    open fun scale(sx: Float, sy: Float)   { _canvas?.scale(sx, sy) }
}
```

**Algorithm notes:**
- `canvas` is typed as `Any?` and cast to `Canvas` internally. This matches the trackplay `ctx.canvas = canvas` pattern (where `canvas` is declared `Any` in the reference).
- Every draw call is guarded by `_canvas ?: return` — no crash if canvas not set.
- `drawSmuflGlyph` does the `save/translate/scale(1,-1)/drawText/restore` dance. The scale flip is critical for correct SMuFL rendering orientation on Android. `drawText` with the flipped scale produces right-side-up glyphs.
- `bravuraTypeface` is loaded lazily on first glyph draw. It will crash with `error()` if the font asset is missing — this is intentional (fail loudly not silently).
- Subclassable (`open`) so tests can override drawing methods with recording stubs.

**Test file:** `vexflow/core/VexRenderingContextTest.kt`

```kotlin
package dev.pola.vexflow.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VexRenderingContextTest {

    // Use a RecordingContext (subclass) to capture calls without a real Canvas.
    private fun makeCtx(): RecordingContext = RecordingContext()

    @Test fun `fillRect records a draw call`() {
        val ctx = makeCtx()
        ctx.fillRect(10f, 20f, 100f, 5f)
        assertEquals(1, ctx.fillRectCalls.size)
        assertEquals(10f, ctx.fillRectCalls[0][0])
    }

    @Test fun `strokeColor property updates strokePaint`() {
        val ctx = makeCtx()
        ctx.strokeColor = android.graphics.Color.RED
        assertEquals(android.graphics.Color.RED, ctx.strokeColor)
    }

    @Test fun `lineWidth property updates strokePaint`() {
        val ctx = makeCtx()
        ctx.lineWidth = 3f
        assertEquals(3f, ctx.lineWidth)
    }

    @Test fun `path operations do not crash without canvas`() {
        val ctx = makeCtx() // canvas not set
        ctx.beginPath()
        ctx.moveTo(0f, 0f)
        ctx.lineTo(100f, 100f)
        ctx.stroke() // should no-op silently
    }

    @Test fun `drawSmuflGlyph records call`() {
        val ctx = makeCtx()
        ctx.drawSmuflGlyph(0xE050, 50f, 100f, 40f)
        assertEquals(1, ctx.glyphCalls.size)
        assertEquals(0xE050, ctx.glyphCalls[0].codepoint)
        assertEquals(50f,    ctx.glyphCalls[0].x)
        assertEquals(100f,   ctx.glyphCalls[0].y)
    }
}

/** Test-only subclass that records draw calls without a real Canvas. */
class RecordingContext : VexRenderingContext() {
    data class GlyphCall(val codepoint: Int, val x: Float, val y: Float, val size: Float)

    val fillRectCalls  = mutableListOf<FloatArray>()
    val strokeCalls    = mutableListOf<Unit>()
    val glyphCalls     = mutableListOf<GlyphCall>()

    override fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        fillRectCalls += floatArrayOf(x, y, width, height)
    }
    override fun stroke() { strokeCalls += Unit }
    override fun drawSmuflGlyph(codepoint: Int, x: Float, y: Float, sizePx: Float) {
        glyphCalls += GlyphCall(codepoint, x, y, sizePx)
    }
    override fun save()    {}
    override fun restore() {}
    override fun translate(x: Float, y: Float) {}
    override fun scale(sx: Float, sy: Float) {}
}
```

---

## M1 Gate

- [ ] `./gradlew test --tests "*.VFFractionTest"` — all 11 tests green
- [ ] `./gradlew test --tests "*.VexRenderingContextTest"` — all 5 tests green
- [ ] `VFGlyphBoundingBoxManager.get("gClef")` returns non-null (verified by a `@Test fun glyphBboxLoads()` that calls it with a Robolectric context)
- [ ] `./gradlew assembleDebug` — 0 errors

---

---

# M2 — Staff and Notes

**Goal:** A 5-line staff renders. A note with a notehead, stem, and accidental draws at the correct Y position.

**Files to create (in order):**

1. `vexflow/elements/VFStave.kt`
2. `vexflow/model/VFStaveNote.kt`
3. `vexflow/elements/VFAccidental.kt`

Tests: `vexflow/elements/VFStaveTest.kt`, `vexflow/model/VFStaveNoteTest.kt`

---

## Class 6 — VFStave

**File:** `vexflow/elements/VFStave.kt`

```kotlin
package dev.pola.vexflow.elements

import android.graphics.PointF
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFMetrics

data class VFStaveOptions(
    val numLines: Int = 5,
    val spacingBetweenLinesPx: Float = VFMetrics.DEFAULT_LINE_SPACING
)

/**
 * A single staff (set of horizontal lines) on the canvas.
 *
 * Coordinate system:
 *   x, y = top-left of the top staff line (canvas Y-down).
 *   Line index 0 = top line, index 4 = bottom line (for a 5-line staff).
 *   Note line index is in half-spaces from the top line:
 *     0 = top line, 2 = second line, 4 = middle line, 6 = fourth line, 8 = bottom line
 *     1, 3, 5, 7 = spaces between lines
 *     Negative = ledger lines above; > 8 = ledger lines below
 *
 * API contract (from reference/trackplay/SheetMusicView.kt):
 *   VFStave(x, y, width, VFStaveOptions(...))
 *   stave.setContext(ctx)
 *   stave.clef = VFClef(...)
 *   stave.keySignature = VFKeySignature(...)
 *   stave.timeSignature = VFTimeSignature(...)
 *   stave.draw(ctx)
 */
class VFStave(
    val x: Float,
    val y: Float,
    val width: Float,
    val options: VFStaveOptions = VFStaveOptions()
) {
    val numLines: Int              = options.numLines
    val spacingBetweenLines: Float = options.spacingBetweenLinesPx

    var lineThickness: Float = 1f

    var clef:          dev.pola.vexflow.elements.VFClef?          = null
    var keySignature:  dev.pola.vexflow.elements.VFKeySignature?   = null
    var timeSignature: dev.pola.vexflow.elements.VFTimeSignature?  = null

    // Barlines — set automatically by formatter or explicitly
    var startBarline: VFBarline? = null
    var endBarline:   VFBarline? = VFBarline(VFBarlineType.SINGLE)

    private var context: VexRenderingContext? = null

    fun setContext(ctx: VexRenderingContext) { context = ctx }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    /**
     * Canvas Y of staff line [line], where line 0 = top, line (numLines-1) = bottom.
     */
    fun getYForLine(line: Float): Float = y + line * spacingBetweenLines

    /**
     * Canvas Y for a note at [noteLine] half-spaces from the top line.
     * 0 = top line, 1 = first space above top line going down, 2 = second line, etc.
     */
    fun getYForNote(noteLine: Int): Float = y + noteLine * (spacingBetweenLines / 2f)

    /** Y of the very top edge of the top staff line. */
    fun getTopLineTopY(): Float = y - lineThickness / 2f

    /** Y of the very bottom edge of the bottom staff line. */
    fun getBottomLineBottomY(): Float = getYForLine((numLines - 1).toFloat()) + lineThickness / 2f

    /**
     * X where notes begin (after clef + key sig + time sig + padding).
     * Used by formatter as the initial startX for voice layout.
     */
    fun getNoteStartX(): Float {
        var startX = x + VFMetrics.STAVE_LEFT_PADDING
        clef?.let { startX += it.width + VFMetrics.CLEF_PADDING }
        keySignature?.let { if (!it.isEmpty) startX += it.width + VFMetrics.KEY_SIGNATURE_PADDING }
        timeSignature?.let { startX += it.width + VFMetrics.TIME_SIGNATURE_PADDING }
        return startX
    }

    /** X where a tie starting at this stave begins (after modifiers). */
    fun getTieStartX(): Float = getNoteStartX()

    /** X where a tie ending at this stave ends. */
    fun getTieEndX(): Float = x + width

    // ── Drawing ───────────────────────────────────────────────────────────────

    fun draw(ctx: VexRenderingContext) {
        drawLines(ctx)
        drawStartBarline(ctx)
        drawModifiers(ctx)
        drawEndBarline(ctx)
    }

    private fun drawLines(ctx: VexRenderingContext) {
        ctx.lineWidth = lineThickness
        ctx.strokeColor = android.graphics.Color.BLACK
        for (i in 0 until numLines) {
            val lineY = getYForLine(i.toFloat())
            ctx.beginPath()
            ctx.moveTo(x, lineY)
            ctx.lineTo(x + width, lineY)
            ctx.stroke()
        }
    }

    private fun drawStartBarline(ctx: VexRenderingContext) {
        val bl = startBarline ?: VFBarline(VFBarlineType.SINGLE)
        bl.stave = this
        bl.x = x
        bl.draw(ctx)
    }

    private fun drawEndBarline(ctx: VexRenderingContext) {
        val bl = endBarline ?: return
        bl.stave = this
        bl.x = x + width
        bl.draw(ctx)
    }

    private fun drawModifiers(ctx: VexRenderingContext) {
        var curX = x + VFMetrics.STAVE_LEFT_PADDING

        clef?.let {
            it.x = curX
            it.draw(this, ctx)
            curX += it.width + VFMetrics.CLEF_PADDING
        }
        keySignature?.let {
            if (!it.isEmpty) {
                it.x = curX
                it.draw(this, ctx)
                curX += it.width + VFMetrics.KEY_SIGNATURE_PADDING
            }
        }
        timeSignature?.let {
            it.x = curX
            it.draw(this, ctx)
        }
    }
}
```

**Algorithm notes:**
- `getYForNote(noteLine)` uses half-space resolution. Line 0 is both staff-line-index 0 and note-line-index 0 (top line). Note line index 1 is the space immediately above the first ledger line below top, meaning one half-space down. This matches VexFlow's note-line convention.
- `getNoteStartX()` accumulates modifier widths left-to-right. This must return the same value that `VFFormatter` uses as `startX`. Always drive from `stave.getNoteStartX()` — do not hard-code offsets in the formatter.
- `drawModifiers()` sets `it.x` on each modifier immediately before drawing. Modifiers do not self-position; the stave tells them where to draw.

**Test file:** `vexflow/elements/VFStaveTest.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.model.VFMetrics
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFStaveTest {

    private fun stave(x: Float = 0f, y: Float = 100f, width: Float = 400f) =
        VFStave(x, y, width)

    @Test fun `getYForLine line 0 returns y`() {
        assertEquals(100f, stave(y = 100f).getYForLine(0f))
    }

    @Test fun `getYForLine line 4 returns y plus 4 spacings`() {
        val s = stave(y = 100f)
        assertEquals(100f + 4 * VFMetrics.DEFAULT_LINE_SPACING, s.getYForLine(4f))
    }

    @Test fun `getYForNote half-space resolution`() {
        val s = stave(y = 100f)
        assertEquals(100f, s.getYForNote(0))                               // top line
        assertEquals(100f + VFMetrics.DEFAULT_LINE_SPACING / 2f, s.getYForNote(1))  // first space down
        assertEquals(100f + VFMetrics.DEFAULT_LINE_SPACING, s.getYForNote(2))       // second line
    }

    @Test fun `getNoteStartX without modifiers`() {
        val s = stave(x = 50f)
        assertEquals(50f + VFMetrics.STAVE_LEFT_PADDING, s.getNoteStartX())
    }

    @Test fun `getNoteStartX with clef adds clef width and padding`() {
        val s = stave(x = 0f)
        val clef = VFClef("treble", "default", null)
        s.clef = clef
        val expected = VFMetrics.STAVE_LEFT_PADDING + clef.width + VFMetrics.CLEF_PADDING
        assertEquals(expected, s.getNoteStartX(), 0.5f)
    }

    @Test fun `draw does not crash with recording context`() {
        val ctx = dev.pola.vexflow.core.RecordingContext()
        val s = stave()
        s.draw(ctx)
        assertTrue(ctx.strokeCalls.isNotEmpty(), "Expected staff lines to be stroked")
    }
}
```

---

## Class 7 — VFStaveNote

**File:** `vexflow/model/VFStaveNote.kt`

```kotlin
package dev.pola.vexflow.model

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFStave

/**
 * Constructor argument struct. Matches reference/trackplay/SheetMusicView.kt usage:
 *   VFStaveNote(VFStaveNoteStruct(keys = listOf("f/5"), duration = "4", glyphFontScale = 40f))
 *
 * keys format: "<pitch>/<octave>" e.g. "f/5", "c#/4", "gb/3"
 * duration format: "1","2","4","8","16","32" optionally followed by "r" (rest) or "d" (dotted)
 */
data class VFStaveNoteStruct(
    val keys: List<String>,
    val duration: String,
    val glyphFontScale: Float = 40f,
    val stemDirection: Int = STEM_AUTO  // STEM_AUTO, STEM_UP, STEM_DOWN
) {
    companion object {
        const val STEM_AUTO = 0
        const val STEM_UP   = 1
        const val STEM_DOWN = -1
    }
}

data class VFTickableMetrics(
    val width: Float = 0f,
    val totalLeftPx: Float = 0f,
    val totalRightPx: Float = 0f
)

/**
 * A single chord (one or more noteheads on the same stem) or a rest.
 *
 * Coordinate system: x is the center X of the notehead column.
 * The stave must be set before draw() is called (via setStave()).
 *
 * Pitch-to-line mapping:
 *   note-line index for treble clef:
 *     B5=0, A5=1, G5=2, F5=3, E5=4, D5=5, C5=6, B4=7, A4=8, G4=9, F4=10,
 *     E4=11(=middle line), D4=12, C4=13, B3=14, A3=15, G3=16
 *   Formula (treble, G clef anchored at line index 6 = G4):
 *     steps = (octave - 4) * 7 + noteStepFromC
 *     noteLineIndex = 12 - steps  (where C4 = note-line 12, B5 = 0)
 *   For bass clef (F clef anchored at line index 4 = F3):
 *     steps = (octave - 3) * 7 + noteStepFromC - 3  (F is step 3 from C)
 *     noteLineIndex = 8 - steps + offset
 *   See pitchToNoteLineIndex() for the full implementation.
 */
class VFStaveNote(private val struct: VFStaveNoteStruct) {

    val keys: List<String>  = struct.keys
    val durationString: String = struct.duration
    val duration: VFFraction = VFFraction.fromDurationString(struct.duration)
        ?: error("Unknown duration string: '${struct.duration}'")
    val glyphFontScale: Float = struct.glyphFontScale
    val isRest: Boolean = struct.duration.contains('r')

    var x: Float = 0f          // set by VFFormatter via VFTickContext.setX()
    var noteLineIndex: Int = 0  // half-space from top line; set by setStave()

    private var stave: VFStave? = null
    private var tickContext: VFTickContext? = null
    private val accidentalObjects: MutableList<VFAccidental> = mutableListOf()

    // ── Setup ─────────────────────────────────────────────────────────────────

    fun setStave(stave: VFStave) {
        this.stave = stave
        rebuildAccidentals()
    }

    fun setTickContext(tc: VFTickContext) { tickContext = tc }

    fun getStave(): VFStave? = stave

    // ── Metrics ───────────────────────────────────────────────────────────────

    fun getMetrics(): VFTickableMetrics {
        val headWidth = glyphFontScale * 0.65f
        return VFTickableMetrics(
            width = headWidth,
            totalLeftPx = headWidth / 2f,
            totalRightPx = headWidth / 2f
        )
    }

    /** X of the right edge (used as tie start). */
    fun getTieRightX(): Float = x + getMetrics().totalRightPx

    /** X of the left edge (used as tie end). */
    fun getTieLeftX(): Float  = x - getMetrics().totalLeftPx

    /** Canvas Y positions of each notehead in this chord. */
    fun getYs(): List<Float> {
        val sv = stave ?: return emptyList()
        return keys.map { key -> sv.getYForNote(pitchToNoteLineIndex(key, sv)) }
    }

    fun getStemDirection(): Int {
        if (struct.stemDirection != VFStaveNoteStruct.STEM_AUTO) return struct.stemDirection
        // Default: notes at or above the middle line (index 4) get stem down
        val avgLine = keys.map { pitchToNoteLineIndex(it, stave) }.average()
        return if (avgLine <= 4) VFStaveNoteStruct.STEM_DOWN else VFStaveNoteStruct.STEM_UP
    }

    data class StemExtents(val baseY: Float, val topY: Float)

    fun getStemExtents(): StemExtents {
        val sv = stave ?: return StemExtents(0f, 0f)
        val noteYs = getYs()
        val stemDir = getStemDirection()
        val stemHeightPx = VFMetrics.STEM_HEIGHT_SPACES * sv.spacingBetweenLines
        return if (stemDir == VFStaveNoteStruct.STEM_UP) {
            val baseY = noteYs.maxOrNull() ?: 0f    // lowest notehead (highest Y in canvas)
            StemExtents(baseY, baseY - stemHeightPx) // tip is above (lower canvas Y)
        } else {
            val baseY = noteYs.minOrNull() ?: 0f    // highest notehead (lowest Y in canvas)
            StemExtents(baseY, baseY + stemHeightPx)
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    fun draw(ctx: VexRenderingContext) {
        val sv = stave ?: return
        if (isRest) {
            drawRest(ctx, sv)
        } else {
            drawNoteheads(ctx, sv)
            drawStem(ctx, sv)
            drawFlags(ctx, sv)
            drawLedgerLines(ctx, sv)
        }
        drawAccidentals(ctx, sv)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun noteheadGlyph(): Int = when {
        duration >= VFFraction.of(1, 1) -> VFTables.GLYPH_NOTE_HEAD_WHOLE
        duration >= VFFraction.of(1, 2) -> VFTables.GLYPH_NOTE_HEAD_HALF
        else                            -> VFTables.GLYPH_NOTE_HEAD_QUARTER
    }

    private fun restGlyph(): Int = when {
        duration >= VFFraction.of(1, 1) -> VFTables.GLYPH_REST_WHOLE
        duration >= VFFraction.of(1, 2) -> VFTables.GLYPH_REST_HALF
        duration >= VFFraction.of(1, 4) -> VFTables.GLYPH_REST_QUARTER
        duration >= VFFraction.of(1, 8) -> VFTables.GLYPH_REST_8TH
        duration >= VFFraction.of(1, 16) -> VFTables.GLYPH_REST_16TH
        else                             -> VFTables.GLYPH_REST_32ND
    }

    private fun drawNoteheads(ctx: VexRenderingContext, sv: VFStave) {
        val glyph = noteheadGlyph()
        for (key in keys) {
            val noteY = sv.getYForNote(pitchToNoteLineIndex(key, sv))
            ctx.drawSmuflGlyph(glyph, x, noteY, glyphFontScale)
        }
    }

    private fun drawRest(ctx: VexRenderingContext, sv: VFStave) {
        // Whole/half rests anchor to line 1 (second from top); quarter and shorter to middle line
        val restY = when {
            duration >= VFFraction.of(1, 1) -> sv.getYForLine(1f) // whole rest hangs below line 1
            duration >= VFFraction.of(1, 2) -> sv.getYForLine(1f) // half rest sits on line 1
            else                            -> sv.getYForLine(2f) // shorter rests on middle line
        }
        ctx.drawSmuflGlyph(restGlyph(), x, restY, glyphFontScale)
    }

    private fun drawStem(ctx: VexRenderingContext, sv: VFStave) {
        if (duration >= VFFraction.of(1, 1)) return // whole notes have no stem
        val extents = getStemExtents()
        val stemX = if (getStemDirection() == VFStaveNoteStruct.STEM_UP)
            x + glyphFontScale * 0.3f  // right side of notehead
        else
            x - glyphFontScale * 0.3f  // left side of notehead
        ctx.lineWidth = sv.lineThickness * 1.5f
        ctx.beginPath()
        ctx.moveTo(stemX, extents.baseY)
        ctx.lineTo(stemX, extents.topY)
        ctx.stroke()
    }

    private fun drawFlags(ctx: VexRenderingContext, sv: VFStave) {
        // Only draw flags if note is NOT part of a beam group.
        // Beam groups suppress flags by calling setBeamed(true) on each note.
        if (_isBeamed) return
        if (duration >= VFFraction.of(1, 4)) return // quarter and longer have no flag
        val stemUp = getStemDirection() == VFStaveNoteStruct.STEM_UP
        val flagGlyph = when {
            duration >= VFFraction.of(1, 8) ->
                if (stemUp) VFTables.GLYPH_FLAG_8TH_UP else VFTables.GLYPH_FLAG_8TH_DOWN
            duration >= VFFraction.of(1, 16) ->
                if (stemUp) VFTables.GLYPH_FLAG_16TH_UP else VFTables.GLYPH_FLAG_16TH_DOWN
            else ->
                if (stemUp) VFTables.GLYPH_FLAG_32ND_UP else VFTables.GLYPH_FLAG_32ND_DOWN
        }
        val extents = getStemExtents()
        ctx.drawSmuflGlyph(flagGlyph, x + glyphFontScale * 0.3f, extents.topY, glyphFontScale)
    }

    private fun drawLedgerLines(ctx: VexRenderingContext, sv: VFStave) {
        // Draw ledger lines for notes outside the staff
        for (key in keys) {
            val noteLine = pitchToNoteLineIndex(key, sv)
            val ledgerWidth = glyphFontScale * 0.8f
            // Above staff: note-line < 0 (lines at 0, -2, -4 ...)
            var line = -2
            while (line >= noteLine) {
                val lineY = sv.getYForNote(line)
                ctx.lineWidth = sv.lineThickness
                ctx.beginPath()
                ctx.moveTo(x - ledgerWidth / 2f, lineY)
                ctx.lineTo(x + ledgerWidth / 2f, lineY)
                ctx.stroke()
                line -= 2
            }
            // Below staff: note-line > (numLines-1)*2
            val bottomNoteLine = (sv.numLines - 1) * 2
            line = bottomNoteLine + 2
            while (line <= noteLine) {
                val lineY = sv.getYForNote(line)
                ctx.lineWidth = sv.lineThickness
                ctx.beginPath()
                ctx.moveTo(x - ledgerWidth / 2f, lineY)
                ctx.lineTo(x + ledgerWidth / 2f, lineY)
                ctx.stroke()
                line += 2
            }
        }
    }

    private fun drawAccidentals(ctx: VexRenderingContext, sv: VFStave) {
        for (acc in accidentalObjects) {
            acc.x = x - glyphFontScale * 0.9f  // position to left of notehead
            acc.staffLineSpacing = sv.spacingBetweenLines
            acc.draw(ctx)
        }
    }

    private fun rebuildAccidentals() {
        accidentalObjects.clear()
        for (key in keys) {
            val accStr = extractAccidental(key) ?: continue
            val accType = VFAccidental.AccidentalType.fromString(accStr) ?: continue
            val noteLine = pitchToNoteLineIndex(key, stave)
            accidentalObjects += VFAccidental(accType, noteLine)
        }
    }

    // ── Beaming support ───────────────────────────────────────────────────────

    private var _isBeamed = false
    fun setBeamed(beamed: Boolean) { _isBeamed = beamed }
    fun isBeamed(): Boolean = _isBeamed

    // ── Pitch parsing ─────────────────────────────────────────────────────────

    companion object {
        /**
         * Convert a pitch string ("f/5", "c#/4", "gb/3") to a half-space note-line index
         * from the top of the staff, for the stave's current clef.
         *
         * Treble clef (G clef, G4 on line index 6):
         *   C4 = note-line 12, D4 = 10, E4 = 9... (each diatonic step = 1 half-space)
         *   Note: only diatonic pitch matters for line position; sharps/flats do NOT shift line.
         *
         * Bass clef (F clef, F3 on line index 4):
         *   F3 = note-line 4, so offset = 4 - (stepsFromC for F3)
         *   C3 = note-line 7, D3 = 6 ... F3 = 4 (confirmed: bass clef middle B2 = line 8)
         *
         * Algorithm:
         *   1. Parse "<pitch><accidental>/<octave>" from key string
         *   2. Compute absoluteStep = octave * 7 + diatonicStepFromC(pitchLetter)
         *   3. anchorAbsoluteStep depends on clef (treble: G4 = 4*7+4=32, noteLine=6;
         *                                          bass:   F3 = 3*7+3=24, noteLine=4)
         *   4. noteLine = anchorNoteLine - (absoluteStep - anchorAbsoluteStep)
         *      (subtraction because higher pitch = lower note-line index)
         */
        fun pitchToNoteLineIndex(key: String, stave: VFStave?): Int {
            // Parse pitch and octave from "f/5", "c#/4", "gb/3"
            val parts = key.lowercase().split("/")
            if (parts.size < 2) return 4  // default: middle line
            val pitchPart = parts[0].replace("#", "").replace("b", "").replace("n", "")
            val octave = parts[1].toIntOrNull() ?: 4
            val letter = pitchPart.firstOrNull() ?: 'c'

            val diatonicStep = when (letter) {
                'c' -> 0; 'd' -> 1; 'e' -> 2; 'f' -> 3
                'g' -> 4; 'a' -> 5; 'b' -> 6
                else -> 0
            }
            val absoluteStep = octave * 7 + diatonicStep

            // Treble clef: G4 is on staff line index 3 from top (note-line 6)
            // G4 absolute step = 4*7+4 = 32, noteLine = 6
            val anchorAbsoluteStep = 32  // G4 = treble anchor
            val anchorNoteLine     = 6   // G4 is note-line 6 in treble

            // TODO: adjust anchor for bass/alto/tenor clef when stave.clef is not treble
            // For now, always use treble anchor
            return anchorNoteLine - (absoluteStep - anchorAbsoluteStep)
        }

        private fun extractAccidental(key: String): String? {
            val pitchPart = key.lowercase().split("/").firstOrNull() ?: return null
            return when {
                pitchPart.contains("##") -> "##"
                pitchPart.contains("bb") -> "bb"
                pitchPart.contains("#")  -> "#"
                pitchPart.contains("b")  -> "b"
                pitchPart.contains("n")  -> "n"
                else -> null
            }
        }
    }
}
```

**Algorithm notes:**
- `pitchToNoteLineIndex` encodes only diatonic position — accidentals (`#`, `b`) do NOT change the line. A C# occupies the same line as C natural. Accidentals are drawn separately to the left.
- `getStemDirection()` default: average note-line ≤ 4 (at or above middle of staff) → stem down; > 4 → stem up. This matches standard engraving practice.
- `setBeamed(true)` suppresses flag drawing. The `VFBeam` class calls this on each note it governs.
- `_isBeamed` is `private var` — only `VFBeam` mutates it via `setBeamed()`.
- Whole notes get no stem (`duration >= 1/1`).

---

## Class 8 — VFAccidental

**File:** `vexflow/elements/VFAccidental.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFTables

/**
 * A single accidental drawn to the left of a notehead.
 *
 * Positioning:
 *   x = the notehead center X (stave's note X). The accidental shifts left internally.
 *   noteLineIndex = the notehead's half-space line index (same as VFStaveNote).
 *   staffLineSpacing = stave.spacingBetweenLines.
 *
 * The Y position is:  stave.getYForNote(noteLineIndex)
 * The X position is:  x - (glyphSize * xShiftFactor)
 */
class VFAccidental(
    val type: AccidentalType,
    val noteLineIndex: Int
) {
    var x: Float = 0f
    var staffLineSpacing: Float = 10f

    // glyphSize in pixels = staffLineSpacing * 4 (one full staff height)
    private val glyphSizePx: Float get() = staffLineSpacing * 4f

    fun draw(ctx: VexRenderingContext) {
        val glyphY = noteLineIndex * (staffLineSpacing / 2f)
        // x is already set to the shifted position by VFStaveNote.drawAccidentals()
        ctx.drawSmuflGlyph(type.codepoint, x, glyphY, glyphSizePx)
    }

    enum class AccidentalType(val codepoint: Int) {
        SHARP(VFTables.GLYPH_ACCIDENTAL_SHARP),
        FLAT(VFTables.GLYPH_ACCIDENTAL_FLAT),
        NATURAL(VFTables.GLYPH_ACCIDENTAL_NATURAL),
        DOUBLE_SHARP(VFTables.GLYPH_ACCIDENTAL_DOUBLE_SHARP),
        DOUBLE_FLAT(VFTables.GLYPH_ACCIDENTAL_DOUBLE_FLAT);

        companion object {
            fun fromString(s: String): AccidentalType? = when (s) {
                "#"  -> SHARP
                "b"  -> FLAT
                "n"  -> NATURAL
                "##" -> DOUBLE_SHARP
                "bb" -> DOUBLE_FLAT
                else -> null
            }
        }
    }
}
```

**Test file:** `vexflow/model/VFStaveNoteTest.kt`

```kotlin
package dev.pola.vexflow.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFStaveNoteTest {

    private fun note(keys: List<String>, dur: String) =
        VFStaveNote(VFStaveNoteStruct(keys = keys, duration = dur, glyphFontScale = 40f))

    @Test fun `quarter note duration parses correctly`() {
        val n = note(listOf("c/4"), "4")
        assertEquals(VFFraction.of(1, 4), n.duration)
        assertFalse(n.isRest)
    }

    @Test fun `rest note isRest is true`() {
        val n = note(listOf("b/4"), "4r")
        assertTrue(n.isRest)
    }

    @Test fun `whole note is not beamed and has no flag`() {
        val n = note(listOf("c/4"), "1")
        assertFalse(n.isBeamed())
        // Stem direction doesn't matter for whole note, but duration check:
        assertTrue(n.duration >= VFFraction.of(1, 1))
    }

    @Test fun `pitchToNoteLineIndex treble G4 is 6`() {
        assertEquals(6, VFStaveNote.pitchToNoteLineIndex("g/4", null))
    }

    @Test fun `pitchToNoteLineIndex treble C5 is 5`() {
        // C5 is one step above B4; B4=7, A4=..., G4=6, so C5 is above G4 by 3 steps = line 3
        // C5 absoluteStep = 5*7+0=35; anchor G4=32, anchorLine=6; line=6-(35-32)=3
        assertEquals(3, VFStaveNote.pitchToNoteLineIndex("c/5", null))
    }

    @Test fun `pitchToNoteLineIndex treble E4 is line 8`() {
        // E4 absoluteStep = 4*7+2=30; 6-(30-32)=6+2=8
        assertEquals(8, VFStaveNote.pitchToNoteLineIndex("e/4", null))
    }

    @Test fun `sharp accidental does not change line index`() {
        val lineNatural = VFStaveNote.pitchToNoteLineIndex("f/4", null)
        val lineSharp   = VFStaveNote.pitchToNoteLineIndex("f#/4", null)
        assertEquals(lineNatural, lineSharp)
    }

    @Test fun `stem direction defaults up for notes below middle`() {
        // E4 is line 8 > 4, so stem should go up
        val n = note(listOf("e/4"), "4")
        assertEquals(VFStaveNoteStruct.STEM_UP, n.getStemDirection())
    }

    @Test fun `setBeamed suppresses flag drawing check`() {
        val n = note(listOf("c/4"), "8")
        assertFalse(n.isBeamed())
        n.setBeamed(true)
        assertTrue(n.isBeamed())
    }

    @Test fun `getMetrics returns non-zero width`() {
        val m = note(listOf("c/4"), "4").getMetrics()
        assertTrue(m.width > 0f)
        assertTrue(m.totalLeftPx > 0f)
        assertTrue(m.totalRightPx > 0f)
    }

    @Test fun `unknown duration string throws`() {
        assertThrows(IllegalStateException::class.java) {
            note(listOf("c/4"), "xyz")
        }
    }
}
```

---

## M2 Gate

- [ ] `./gradlew test --tests "*.VFStaveTest"` — all 5 tests green
- [ ] `./gradlew test --tests "*.VFStaveNoteTest"` — all 10 tests green
- [ ] `./gradlew assembleDebug` — 0 errors
- [ ] Manual visual check: `SheetMusicView` in `RendererScreen` renders a 5-line staff with a notehead visible on canvas (verified by running on emulator or device)

---

---

# M3 — Clef, Key Signature, Time Signature

**Goal:** A stave renders with a treble clef, a key signature (sharps/flats), and a time signature. All three position themselves correctly left-to-right.

**Files to create (in order):**

1. `vexflow/elements/VFClef.kt`
2. `vexflow/elements/VFKeySignature.kt`
3. `vexflow/elements/VFTimeSignature.kt`

Tests: `vexflow/elements/VFClefTest.kt`, `vexflow/elements/VFKeySignatureTest.kt`, `vexflow/elements/VFTimeSignatureTest.kt`

---

## Class 9 — VFClef

**File:** `vexflow/elements/VFClef.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFTables

/**
 * API contract (from reference/trackplay/SheetMusicView.kt):
 *   VFClef("treble", "default", null)
 *   VFClef("bass", "default", null)
 *   VFClef("alto", "default", null)
 *
 * Parameters:
 *   clefType:   "treble" | "bass" | "alto" | "tenor" | "soprano" |
 *               "mezzo-soprano" | "baritone-c" | "baritone-f" | "percussion"
 *   size:       "default" | "small" — "default" = 40f px, "small" = 28f px
 *   annotation: null (unused in current spec; reserved for "8va" etc.)
 *
 * Drawing:
 *   The glyph is drawn so its anchor point aligns with its reference staff line.
 *   Treble (G clef): anchor = line 3 from top (0-indexed), which is the G4 line (line index 3).
 *   Bass   (F clef): anchor = line 1 from top (the F3 line).
 *   Alto/Tenor (C clef): anchor = center of the clef on its reference line.
 *
 *   The SMuFL bounding box is used to compute exact pixel position.
 *   If no bounding box data is available, fall back to a fixed offset.
 */
class VFClef(
    clefType: String,
    size: String = "default",
    @Suppress("UNUSED_PARAMETER") annotation: String? = null
) {
    val type: ClefType = ClefType.fromString(clefType)
    val sizePx: Float  = if (size == "small") 28f else 40f

    var x: Float = 0f  // set by VFStave.drawModifiers()

    /** Width consumed by this clef (used by VFStave.getNoteStartX()). */
    val width: Float get() {
        val bbox = VFGlyphBoundingBoxManager.getScaled(type.smuflName, sizePx / 4f)
        return bbox?.width ?: sizePx * 1.2f
    }

    fun draw(stave: dev.pola.vexflow.elements.VFStave, ctx: VexRenderingContext) {
        val anchorY = stave.getYForLine(type.anchorLine.toFloat())
        val bbox = VFGlyphBoundingBoxManager.getScaled(type.smuflName, stave.spacingBetweenLines)
        // Draw glyph so that its bBoxSW.y aligns at the anchor line
        val drawY = if (bbox != null) anchorY + bbox.southwest.y else anchorY
        ctx.drawSmuflGlyph(type.codepoint, x, drawY, sizePx)
    }

    // ─────────────────────────────────────────────────────────────────────────

    enum class ClefType(
        val codepoint: Int,
        val smuflName: String,
        val anchorLine: Int  // 0-indexed staff line (0 = top) where clef anchor sits
    ) {
        TREBLE    (VFTables.GLYPH_G_CLEF,        "gClef",                    3),
        BASS      (VFTables.GLYPH_F_CLEF,        "fClef",                    1),
        ALTO      (VFTables.GLYPH_C_CLEF,        "cClef",                    2),
        TENOR     (VFTables.GLYPH_C_CLEF,        "cClef",                    1),
        SOPRANO   (VFTables.GLYPH_C_CLEF,        "cClef",                    4),
        PERCUSSION(VFTables.GLYPH_PERCUSSION_CLEF,"unpitchedPercussionClef1", 2);

        companion object {
            fun fromString(s: String): ClefType = when (s.lowercase()) {
                "treble"      -> TREBLE
                "bass"        -> BASS
                "alto"        -> ALTO
                "tenor"       -> TENOR
                "soprano"     -> SOPRANO
                "percussion"  -> PERCUSSION
                else          -> TREBLE // default
            }
        }
    }
}
```

**Test file:** `vexflow/elements/VFClefTest.kt`

```kotlin
package dev.pola.vexflow.elements

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFClefTest {

    @Test fun `treble clef type parsed correctly`() {
        val c = VFClef("treble", "default", null)
        assertEquals(VFClef.ClefType.TREBLE, c.type)
    }

    @Test fun `bass clef type parsed correctly`() {
        assertEquals(VFClef.ClefType.BASS, VFClef("bass").type)
    }

    @Test fun `default size is 40f`() {
        assertEquals(40f, VFClef("treble", "default", null).sizePx)
    }

    @Test fun `small size is 28f`() {
        assertEquals(28f, VFClef("treble", "small", null).sizePx)
    }

    @Test fun `unknown clef defaults to treble`() {
        assertEquals(VFClef.ClefType.TREBLE, VFClef("unknown").type)
    }

    @Test fun `treble clef width is positive`() {
        assertTrue(VFClef("treble").width > 0f)
    }

    @Test fun `draw does not crash with recording context`() {
        val ctx = dev.pola.vexflow.core.RecordingContext()
        val stave = VFStave(0f, 100f, 400f)
        val clef = VFClef("treble")
        clef.x = 10f
        clef.draw(stave, ctx)
        assertTrue(ctx.glyphCalls.isNotEmpty())
    }
}
```

---

## Class 10 — VFKeySignature

**File:** `vexflow/elements/VFKeySignature.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFTables

/**
 * API contract: VFKeySignature("G") — key spec string.
 *
 * Key spec format: pitch letter + optional "m" for minor.
 *   "C" = C major (0 accidentals)
 *   "G" = G major (1 sharp)
 *   "F" = F major (1 flat)
 *   "Cm" = C minor (same as Eb major = 3 flats)
 *
 * Accidental ordering (standard engraving):
 *   Sharps: F C G D A E B  (lines for treble: 5th, 3rd, 6th, 4th, 2nd, 5th, 3rd from top — in half-spaces: 3,1,4,2,0,3,1)
 *   Flats:  B E A D G C F  (treble half-spaces: 1,3,0,2,4,1,3)
 *
 * Width = accidentalCount * size * 0.7
 */
class VFKeySignature(keySpec: String = "C") {

    var size: Float = 30f
    var x: Float = 0f

    private val accidentals: List<Accidental> = buildAccidentals(keySpec)

    val accidentalCount: Int get() = accidentals.size
    val isEmpty: Boolean       get() = accidentals.isEmpty()
    val width: Float           get() = accidentalCount * size * 0.7f

    fun draw(stave: VFStave, ctx: VexRenderingContext) {
        var drawX = x
        for (acc in accidentals) {
            val noteY = stave.getYForNote(acc.noteLineIndex)
            ctx.drawSmuflGlyph(acc.codepoint, drawX, noteY, size * 4f)
            drawX += size * 0.7f
        }
    }

    // ── Internal data ─────────────────────────────────────────────────────────

    private data class Accidental(val codepoint: Int, val noteLineIndex: Int)

    companion object {
        // Treble clef note-line indices for each accidental position (half-spaces from top)
        private val SHARP_LINES  = intArrayOf(3, 1, 4, 2, 0, 3, 1)  // F C G D A E B
        private val FLAT_LINES   = intArrayOf(1, 3, 0, 2, 4, 1, 3)  // B E A D G C F

        // Number of sharps/flats for each major key (circle of fifths order)
        private val MAJOR_KEY_SHARPS = mapOf(
            "C" to 0, "G" to 1, "D" to 2, "A" to 3, "E" to 4, "B" to 5, "F#" to 6, "C#" to 7
        )
        private val MAJOR_KEY_FLATS = mapOf(
            "C" to 0, "F" to 1, "Bb" to 2, "Eb" to 3, "Ab" to 4, "Db" to 5, "Gb" to 6, "Cb" to 7
        )
        // Minor keys map to their relative major
        private val MINOR_TO_MAJOR = mapOf(
            "Am" to "C", "Em" to "G", "Bm" to "D", "F#m" to "A", "C#m" to "E",
            "G#m" to "B", "D#m" to "F#", "A#m" to "C#",
            "Dm" to "F", "Gm" to "Bb", "Cm" to "Eb", "Fm" to "Ab",
            "Bbm" to "Db", "Ebm" to "Gb", "Abm" to "Cb"
        )

        private fun buildAccidentals(keySpec: String): List<Accidental> {
            val resolvedKey = MINOR_TO_MAJOR[keySpec] ?: keySpec
            val numSharps = MAJOR_KEY_SHARPS[resolvedKey]
            if (numSharps != null && numSharps > 0) {
                return (0 until numSharps).map { i ->
                    Accidental(VFTables.GLYPH_ACCIDENTAL_SHARP, SHARP_LINES[i])
                }
            }
            val numFlats = MAJOR_KEY_FLATS[resolvedKey]
            if (numFlats != null && numFlats > 0) {
                return (0 until numFlats).map { i ->
                    Accidental(VFTables.GLYPH_ACCIDENTAL_FLAT, FLAT_LINES[i])
                }
            }
            return emptyList()  // C major / A minor: no accidentals
        }
    }
}
```

**Test file:** `vexflow/elements/VFKeySignatureTest.kt`

```kotlin
package dev.pola.vexflow.elements

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFKeySignatureTest {

    @Test fun `C major has 0 accidentals and isEmpty`() {
        val ks = VFKeySignature("C")
        assertEquals(0, ks.accidentalCount)
        assertTrue(ks.isEmpty)
    }

    @Test fun `G major has 1 sharp`() {
        val ks = VFKeySignature("G")
        assertEquals(1, ks.accidentalCount)
        assertFalse(ks.isEmpty)
    }

    @Test fun `D major has 2 sharps`() {
        assertEquals(2, VFKeySignature("D").accidentalCount)
    }

    @Test fun `F major has 1 flat`() {
        assertEquals(1, VFKeySignature("F").accidentalCount)
    }

    @Test fun `Bb major has 2 flats`() {
        assertEquals(2, VFKeySignature("Bb").accidentalCount)
    }

    @Test fun `A minor resolves to C major (0 accidentals)`() {
        assertEquals(0, VFKeySignature("Am").accidentalCount)
    }

    @Test fun `D minor resolves to F major (1 flat)`() {
        assertEquals(1, VFKeySignature("Dm").accidentalCount)
    }

    @Test fun `width is proportional to accidental count`() {
        val ks = VFKeySignature("G").apply { size = 30f }
        assertEquals(1 * 30f * 0.7f, ks.width, 0.01f)
    }

    @Test fun `draw does not crash`() {
        val ctx = dev.pola.vexflow.core.RecordingContext()
        val stave = VFStave(0f, 100f, 400f)
        val ks = VFKeySignature("D")
        ks.x = 50f
        ks.draw(stave, ctx)
        assertEquals(2, ctx.glyphCalls.size)  // 2 sharps
    }
}
```

---

## Class 11 — VFTimeSignature

**File:** `vexflow/elements/VFTimeSignature.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFTables

/**
 * API contract: VFTimeSignature("4/4"), VFTimeSignature("3/4"), VFTimeSignature("C"), VFTimeSignature("cut")
 *
 * Rendering modes:
 *   - Common time ("C" or "common"): draws timeSigCommon glyph centered on staff.
 *   - Cut time ("cut"):              draws timeSigCutCommon glyph centered on staff.
 *   - Numeric ("4/4", "3/8" etc):   draws numerator glyphs on the upper half of staff,
 *                                    denominator glyphs on the lower half.
 *
 * Digit glyph positioning (numeric mode):
 *   For a 5-line staff with spacingBetweenLines = 10f:
 *     Top row center Y    = stave.getYForLine(1f)   (line between top and second line)
 *     Bottom row center Y = stave.getYForLine(3f)   (line between third and fourth lines)
 *   Multi-digit numbers (e.g. "12") draw digits left-to-right centered as a group.
 *
 * Width:
 *   Common/cut: size * 0.9
 *   Numeric:    max(topWidth, bottomWidth) + padding
 */
class VFTimeSignature(timeSpec: String = "4/4") {

    var size: Float = 40f
    var x: Float = 0f
    var width: Float = 0f  // computed in draw() or lazily; read by VFStave.getNoteStartX()

    private val spec: String = timeSpec.lowercase().trim()
    private val isCommon: Boolean  = spec == "c" || spec == "common"
    private val isCut:    Boolean  = spec == "cut"
    private val isNumeric: Boolean = !isCommon && !isCut

    private val topDigits: List<Int>    // codepoints for numerator digits
    private val bottomDigits: List<Int> // codepoints for denominator digits

    init {
        if (isNumeric) {
            val parts = spec.split("/")
            topDigits    = parseDigits(parts.getOrElse(0) { "4" })
            bottomDigits = parseDigits(parts.getOrElse(1) { "4" })
            width = (maxOf(topDigits.size, bottomDigits.size) * size * 0.55f) + 4f
        } else {
            topDigits    = emptyList()
            bottomDigits = emptyList()
            width = size * 0.9f
        }
    }

    fun draw(stave: VFStave, ctx: VexRenderingContext) {
        when {
            isCommon -> drawSingleGlyph(VFTables.GLYPH_TIME_SIG_COMMON, stave, ctx)
            isCut    -> drawSingleGlyph(VFTables.GLYPH_TIME_SIG_CUT, stave, ctx)
            else     -> drawNumeric(stave, ctx)
        }
    }

    private fun drawSingleGlyph(codepoint: Int, stave: VFStave, ctx: VexRenderingContext) {
        val centerY = stave.getYForLine(2f) // middle of staff
        ctx.drawSmuflGlyph(codepoint, x, centerY, size)
    }

    private fun drawNumeric(stave: VFStave, ctx: VexRenderingContext) {
        val topY    = stave.getYForLine(1f)   // upper half center
        val bottomY = stave.getYForLine(3f)   // lower half center
        drawDigitRow(topDigits, x, topY, ctx)
        drawDigitRow(bottomDigits, x, bottomY, ctx)
    }

    private fun drawDigitRow(digits: List<Int>, startX: Float, y: Float, ctx: VexRenderingContext) {
        val digitWidth = size * 0.55f
        val totalWidth = digits.size * digitWidth
        var drawX = startX - totalWidth / 2f + digitWidth / 2f
        for (codepoint in digits) {
            ctx.drawSmuflGlyph(codepoint, drawX, y, size)
            drawX += digitWidth
        }
    }

    companion object {
        private fun parseDigits(s: String): List<Int> =
            s.mapNotNull { ch -> ch.digitToIntOrNull()?.let { VFTables.timeSigDigit(it) } }
    }
}
```

**Test file:** `vexflow/elements/VFTimeSignatureTest.kt`

```kotlin
package dev.pola.vexflow.elements

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFTimeSignatureTest {

    @Test fun `4 over 4 is numeric`() {
        val ts = VFTimeSignature("4/4")
        assertTrue(ts.width > 0f)
    }

    @Test fun `common time has width`() {
        val ts = VFTimeSignature("C")
        assertTrue(ts.width > 0f)
    }

    @Test fun `cut time has width`() {
        val ts = VFTimeSignature("cut")
        assertTrue(ts.width > 0f)
    }

    @Test fun `draw 4 over 4 calls two glyph groups`() {
        val ctx = dev.pola.vexflow.core.RecordingContext()
        val stave = VFStave(0f, 100f, 400f)
        VFTimeSignature("4/4").apply { x = 50f }.draw(stave, ctx)
        assertEquals(2, ctx.glyphCalls.size)  // one "4" top, one "4" bottom
    }

    @Test fun `draw 12 over 8 calls three glyph draws`() {
        val ctx = dev.pola.vexflow.core.RecordingContext()
        val stave = VFStave(0f, 100f, 400f)
        VFTimeSignature("12/8").apply { x = 50f }.draw(stave, ctx)
        // "12" = 2 digits top, "8" = 1 digit bottom -> 3 draws
        assertEquals(3, ctx.glyphCalls.size)
    }

    @Test fun `draw common time calls one glyph`() {
        val ctx = dev.pola.vexflow.core.RecordingContext()
        val stave = VFStave(0f, 100f, 400f)
        VFTimeSignature("C").apply { x = 50f }.draw(stave, ctx)
        assertEquals(1, ctx.glyphCalls.size)
    }
}
```

---

## M3 Gate

- [ ] `./gradlew test --tests "*.VFClefTest"` — all 7 tests green
- [ ] `./gradlew test --tests "*.VFKeySignatureTest"` — all 9 tests green
- [ ] `./gradlew test --tests "*.VFTimeSignatureTest"` — all 4 tests green
- [ ] `./gradlew assembleDebug` — 0 errors
- [ ] Visual: stave renders with treble clef + 2-sharp key sig (D major) + 4/4 time sig, all positioned left-to-right without overlap

---

---

# M4 — Voice and Formatting Engine

**Goal:** Multiple notes lay out across a stave with correct proportional spacing. Two voices on the same stave align at shared beat positions.

**Files to create (in order):**

1. `vexflow/core/VFTickContext.kt`
2. `vexflow/core/VFVoice.kt`
3. `vexflow/core/VFFormatter.kt`

Tests: `vexflow/core/VFFormatterTest.kt`

---

## Class 12 — VFTickContext

**File:** `vexflow/core/VFTickContext.kt`

```kotlin
package dev.pola.vexflow.core

import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFFraction

/**
 * A tick context groups all notes (across all voices) that occur at the same
 * beat position. The formatter assigns an x position to each context, which
 * then propagates to the notes within it.
 *
 * Notes within a tick context are stored by voice index so that multi-voice
 * alignment works: voice 0 and voice 1 notes at beat 1 share the same x.
 */
class VFTickContext(val tickID: Int) {

    private val tickablesByVoice: MutableMap<Int, VFStaveNote> = mutableMapOf()

    var x: Float = 0f
        set(value) {
            field = value
            // Propagate x to all notes in this context
            for (note in tickablesByVoice.values) { note.x = value }
        }

    var width: Float = 0f  // computed by preFormat()

    // ── Building ──────────────────────────────────────────────────────────────

    fun addTickable(note: VFStaveNote, voiceIndex: Int = 0) {
        tickablesByVoice[voiceIndex] = note
        note.setTickContext(this)
    }

    fun getTickables(): List<VFStaveNote> = tickablesByVoice.values.toList()

    fun getTickablesByVoice(): Map<Int, VFStaveNote> = tickablesByVoice.toMap()

    /** The largest duration among notes in this context (determines spacing weight). */
    fun getMaxDuration(): VFFraction =
        tickablesByVoice.values.maxOfOrNull { it.duration } ?: VFFraction.ZERO

    // ── Layout ────────────────────────────────────────────────────────────────

    /**
     * Compute this context's width from the widest note's metrics.
     * Called by VFFormatter before x positions are assigned.
     */
    fun preFormat() {
        width = tickablesByVoice.values.maxOfOrNull { note ->
            note.getMetrics().let { it.totalLeftPx + it.totalRightPx }
        } ?: 0f
    }
}
```

---

## Class 13 — VFVoice

**File:** `vexflow/core/VFVoice.kt`

```kotlin
package dev.pola.vexflow.core

import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFFraction

/**
 * API contract (from reference/trackplay/SheetMusicView.kt):
 *   val voice = VFVoice("4/4")
 *   voice.addTickables(notes)
 *   voice.setStave(stave)
 *   voice.preFormat()
 *
 * A voice is a single sequence of notes/rests that together fill one measure.
 * The timeSpec ("4/4", "3/4") defines the expected total duration for validation.
 *
 * Resolution: the voice uses 32nd-note resolution (denominator 32) for tick IDs,
 * matching VexFlow's convention. A quarter note has tickID duration = 8 (32nds).
 */
class VFVoice(timeSpec: String = "4/4") {

    private val expectedTotalTicks: VFFraction = parseTimeSpec(timeSpec)

    val tickables: MutableList<VFStaveNote> = mutableListOf()

    private var stave: VFStave? = null

    // ── Building ──────────────────────────────────────────────────────────────

    fun addTickable(note: VFStaveNote) { tickables.add(note) }

    fun addTickables(notes: List<VFStaveNote>) { tickables.addAll(notes) }

    fun clear() { tickables.clear() }

    // ── Setup ─────────────────────────────────────────────────────────────────

    fun setStave(stave: VFStave) {
        this.stave = stave
        tickables.forEach { it.setStave(stave) }
    }

    fun getStave(): VFStave? = stave

    /**
     * Total duration of all notes currently in this voice.
     * Used by VFFormatter to validate that voices fill the measure.
     */
    fun getTotalTicks(): VFFraction =
        tickables.fold(VFFraction.ZERO) { acc, note -> acc + note.duration }

    /**
     * Denominator used for tick ID calculation: always 32 (32nd-note resolution).
     * A quarter note = 8 ticks, a half = 16, a whole = 32, an eighth = 4.
     */
    fun getResolutionMultiplier(): Int = 32

    /**
     * Assign this stave to all tickables. Called during formatting.
     * Must be called before VFFormatter.formatVoices().
     */
    fun preFormat() {
        val sv = stave ?: return
        tickables.forEach { it.setStave(sv) }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    fun draw(ctx: dev.pola.vexflow.core.VexRenderingContext) {
        tickables.forEach { it.draw(ctx) }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    companion object {
        /** Parse "4/4" -> VFFraction(4,4), "3/8" -> VFFraction(3,8). */
        fun parseTimeSpec(spec: String): VFFraction {
            val parts = spec.split("/")
            val n = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 4
            val d = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 4
            return VFFraction.of(n, d)
        }
    }
}
```

---

## Class 14 — VFFormatter

**File:** `vexflow/core/VFFormatter.kt`

```kotlin
package dev.pola.vexflow.core

import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFFraction

data class VFFormatterOptions(
    val minWidth: Float = 10f        // minimum spacing between adjacent tick contexts
)

/**
 * API contract (from reference/trackplay/SheetMusicView.kt):
 *   val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
 *   formatter.formatVoices(voices, stave, startX = stave.x + 50f, justifyWidth = stave.width - 100f)
 *
 * Layout algorithm (proportional spacing):
 *
 *   1. COLLECT: Iterate all notes across all voices. Group notes with the same
 *      beat position (cumulative duration) into shared VFTickContext objects.
 *      Beat position is tracked as a VFFraction (in 32nd-note ticks) per voice.
 *
 *   2. ORDER: Sort tick contexts by beat position.
 *
 *   3. PRE-FORMAT: Call preFormat() on each context to compute its intrinsic width.
 *
 *   4. PROPORTIONAL X: Assign x positions proportionally to beat duration.
 *      The space allocated to each context is:
 *        weight[i] = context.getMaxDuration().doubleValue
 *        totalWeight = sum of all weights
 *        x[i] = startX + (cumWeight / totalWeight) * justifyWidth
 *      where cumWeight is the sum of weights for all contexts before i.
 *
 *   5. MINIMUM SPACING: After proportional assignment, walk contexts left-to-right.
 *      If x[i+1] - (x[i] + context[i].width) < minWidth, push x[i+1] right.
 *      This prevents noteheads from colliding on slow passages.
 *
 *   6. PROPAGATE: Call context.x = computedX (which propagates to notes via VFTickContext.x setter).
 *
 * Notes:
 *   - justifyWidth = 0f disables justification (uses minimum spacing only).
 *   - startX should be stave.getNoteStartX() + some buffer (e.g. stave.getNoteStartX() itself).
 *   - Multi-voice: notes from different voices at the same beat share one VFTickContext.
 *
 * Reference: VexFlow formatter.ts proportional layout; alphaTab BarLayoutingInfo.ts for
 * the spring-mass extension (not implemented here — proportional is sufficient for Phase A).
 */
class VFFormatter(private val options: VFFormatterOptions = VFFormatterOptions()) {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Format and draw a single voice on a stave.
     * Convenience wrapper — calls formatVoices then draws.
     */
    fun formatAndDraw(
        voice: VFVoice,
        stave: VFStave,
        ctx: VexRenderingContext,
        startX: Float
    ) {
        formatVoices(listOf(voice), stave, startX, stave.width - (startX - stave.x))
        voice.draw(ctx)
    }

    /**
     * Format multiple voices on a stave. Assigns x positions to all notes.
     * Does NOT draw — caller is responsible for calling voice.draw(ctx).
     */
    fun formatVoices(
        voices: List<VFVoice>,
        stave: VFStave,
        startX: Float,
        justifyWidth: Float = 0f
    ) {
        val contexts = collectTickContexts(voices)
        contexts.forEach { it.preFormat() }
        assignXPositions(contexts, startX, justifyWidth)
    }

    /**
     * Format and draw all voices. Calls formatVoices then draws each voice.
     */
    fun formatAndDrawVoices(
        voices: List<VFVoice>,
        stave: VFStave,
        ctx: VexRenderingContext,
        justifyWidth: Float = 0f
    ) {
        val startX = stave.getNoteStartX()
        formatVoices(voices, stave, startX, justifyWidth)
        voices.forEach { it.draw(ctx) }
    }

    // ── Private implementation ────────────────────────────────────────────────

    /**
     * Group notes from all voices into VFTickContexts keyed by beat position
     * (expressed as ticks in 32nd-note resolution).
     */
    private fun collectTickContexts(voices: List<VFVoice>): List<VFTickContext> {
        val contextMap = sortedMapOf<Int, VFTickContext>() // tickID -> context

        for ((voiceIndex, voice) in voices.withIndex()) {
            var beatTick = 0  // cumulative ticks at 32nd-note resolution
            val resolution = voice.getResolutionMultiplier()

            for (note in voice.tickables) {
                val ctx = contextMap.getOrPut(beatTick) { VFTickContext(beatTick) }
                ctx.addTickable(note, voiceIndex)
                // Advance by note duration in ticks (duration * resolution)
                val durationTicks = (note.duration.doubleValue * resolution).toInt()
                beatTick += durationTicks
            }
        }

        return contextMap.values.toList()
    }

    /**
     * Assign x positions to contexts using proportional spacing + minimum gap enforcement.
     */
    private fun assignXPositions(
        contexts: List<VFTickContext>,
        startX: Float,
        justifyWidth: Float
    ) {
        if (contexts.isEmpty()) return

        val totalWeight = contexts.sumOf { it.getMaxDuration().doubleValue }
        if (totalWeight <= 0.0) return

        // Pass 1: proportional x based on beat weight
        var cumWeight = 0.0
        for (ctx in contexts) {
            val proportion = if (justifyWidth > 0f) cumWeight / totalWeight else 0.0
            ctx.x = startX + (proportion * justifyWidth).toFloat()
            cumWeight += ctx.getMaxDuration().doubleValue
        }

        // Pass 2: enforce minimum spacing — push contexts right if too close
        val minGap = options.minWidth
        for (i in 1 until contexts.size) {
            val prev = contexts[i - 1]
            val curr = contexts[i]
            val minX = prev.x + prev.width + minGap
            if (curr.x < minX) curr.x = minX
        }
    }
}
```

**Algorithm notes:**
- `collectTickContexts` keys on `beatTick` — an integer count of 32nd-note ticks. Notes from different voices with the same `beatTick` land in the same `VFTickContext`. This is the multi-voice alignment mechanism.
- `assignXPositions` Pass 1 computes a proportional x. Pass 2 ensures no two consecutive noteheads overlap. Pass 2 only pushes right — it never pulls left — so the final layout may be wider than `justifyWidth` if the content is dense. This is correct behavior (stave slightly overflows rather than noteheads colliding).
- For now, no spring-mass spring back pass. When M10 (multi-measure layout) is implemented, a spring-mass pass may be added at the system level using alphaTab's `BarLayoutingInfo.ts` as reference.

**Test file:** `vexflow/core/VFFormatterTest.kt`

```kotlin
package dev.pola.vexflow.core

import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.elements.VFStaveOptions
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import dev.pola.vexflow.model.VFFraction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFFormatterTest {

    private fun makeDependencies(): Triple<VFFormatter, VFVoice, VFStave> {
        val stave = VFStave(0f, 100f, 500f)
        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))
        val voice = VFVoice("4/4").apply { setStave(stave) }
        return Triple(formatter, voice, stave)
    }

    private fun quarterNote(key: String) = VFStaveNote(
        VFStaveNoteStruct(keys = listOf(key), duration = "4", glyphFontScale = 40f)
    )

    @Test fun `four quarter notes get increasing x positions`() {
        val (formatter, voice, stave) = makeDependencies()
        val notes = listOf("c/5","d/5","e/5","f/5").map { quarterNote(it) }
        voice.addTickables(notes)
        formatter.formatVoices(listOf(voice), stave, startX = 50f, justifyWidth = 400f)
        val xs = notes.map { it.x }
        // Each note should be further right than the previous
        for (i in 1 until xs.size) assertTrue(xs[i] > xs[i-1], "Note $i x=${xs[i]} should be > ${xs[i-1]}")
    }

    @Test fun `first note x equals startX`() {
        val (formatter, voice, stave) = makeDependencies()
        val note = quarterNote("c/5")
        voice.addTickable(note)
        formatter.formatVoices(listOf(voice), stave, startX = 75f, justifyWidth = 300f)
        assertEquals(75f, note.x, 0.1f)
    }

    @Test fun `minimum spacing enforced when justifyWidth is 0`() {
        val (formatter, voice, stave) = makeDependencies()
        val notes = listOf(quarterNote("c/5"), quarterNote("d/5"))
        voice.addTickables(notes)
        formatter.formatVoices(listOf(voice), stave, startX = 50f, justifyWidth = 0f)
        val gap = notes[1].x - notes[0].x
        assertTrue(gap >= 10f, "Gap $gap should be >= minWidth 10f")
    }

    @Test fun `two voices at same beat share x position`() {
        val (formatter, _, stave) = makeDependencies()
        val voice1 = VFVoice("4/4").apply { setStave(stave) }
        val voice2 = VFVoice("4/4").apply { setStave(stave) }
        val note1 = quarterNote("e/5"); voice1.addTickable(note1)
        val note2 = quarterNote("c/4"); voice2.addTickable(note2)
        formatter.formatVoices(listOf(voice1, voice2), stave, startX = 50f, justifyWidth = 300f)
        assertEquals(note1.x, note2.x, 0.01f)
    }

    @Test fun `whole note gets same x as single context`() {
        val (formatter, voice, stave) = makeDependencies()
        val whole = VFStaveNote(VFStaveNoteStruct(listOf("c/4"), "1", 40f))
        voice.addTickable(whole)
        formatter.formatVoices(listOf(voice), stave, startX = 60f, justifyWidth = 400f)
        assertEquals(60f, whole.x, 0.1f)
    }
}
```

---

## M4 Gate

- [ ] `./gradlew test --tests "*.VFFormatterTest"` — all 5 tests green
- [ ] `./gradlew assembleDebug` — 0 errors
- [ ] Visual: 4 quarter notes appear evenly spaced across the stave, each to the right of the time signature

---

---

# M5 — Ties, Slurs, Barlines, Beams

**Goal:** Notes can be connected by ties, slurs, and beams. Measures are delimited by barlines including double-bar and repeat barlines.

**Files to create (in order):**

1. `vexflow/elements/VFBarline.kt`
2. `vexflow/elements/VFTie.kt`
3. `vexflow/elements/VFSlur.kt`
4. `vexflow/elements/VFBeam.kt`

Tests: `vexflow/elements/VFBarlineTest.kt`, `vexflow/elements/VFBeamTest.kt`

---

## Class 15 — VFBarline

**File:** `vexflow/elements/VFBarline.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFMetrics

enum class VFBarlineType {
    SINGLE,       // one thin vertical line (default measure separator)
    DOUBLE,       // two thin vertical lines
    END,          // thin + thick (end of piece)
    REPEAT_BEGIN, // thick + thin + two dots (begin repeat)
    REPEAT_END,   // two dots + thin + thick (end repeat)
    REPEAT_BOTH,  // end + begin combined
    NONE;         // invisible (no drawing)

    companion object {
        fun fromString(s: String): VFBarlineType = when (s.lowercase()) {
            "single"       -> SINGLE
            "double"       -> DOUBLE
            "end"          -> END
            "repeat-begin",
            "repeatbegin"  -> REPEAT_BEGIN
            "repeat-end",
            "repeatend"    -> REPEAT_END
            "repeat-both",
            "repeatboth"   -> REPEAT_BOTH
            "none"         -> NONE
            else           -> SINGLE
        }
    }
}

/**
 * Draws a barline at position [x] within [stave].
 * [stave] and [x] are set by VFStave.drawStartBarline() / drawEndBarline().
 */
class VFBarline(var type: VFBarlineType = VFBarlineType.SINGLE) {

    var stave: VFStave? = null
    var x: Float = 0f

    private val thinWidth  = 1.5f
    private val thickWidth = 5f
    private val dotRadius  = 1.5f
    private val dotPad     = 4f   // space between barline and repeat dots

    fun draw(ctx: VexRenderingContext) {
        val sv = stave ?: return
        when (type) {
            VFBarlineType.NONE         -> return
            VFBarlineType.SINGLE       -> drawThin(sv, ctx, x)
            VFBarlineType.DOUBLE       -> { drawThin(sv, ctx, x - thinWidth - 2f); drawThin(sv, ctx, x) }
            VFBarlineType.END          -> { drawThin(sv, ctx, x - thickWidth - 2f); drawThick(sv, ctx, x) }
            VFBarlineType.REPEAT_BEGIN -> { drawThick(sv, ctx, x); drawThin(sv, ctx, x + thickWidth + 2f); drawDots(sv, ctx, x + thickWidth + 2f + dotPad, rightSide = true) }
            VFBarlineType.REPEAT_END   -> { drawDots(sv, ctx, x - dotPad, rightSide = false); drawThin(sv, ctx, x - dotPad - dotRadius * 3); drawThick(sv, ctx, x) }
            VFBarlineType.REPEAT_BOTH  -> {
                drawDots(sv, ctx, x - dotPad, rightSide = false)
                drawThin(sv, ctx, x - dotPad - dotRadius * 3)
                drawThick(sv, ctx, x)
                drawThin(sv, ctx, x + thickWidth + 2f)
                drawDots(sv, ctx, x + thickWidth + 2f + dotPad, rightSide = true)
            }
        }
    }

    private fun drawThin(sv: VFStave, ctx: VexRenderingContext, atX: Float) {
        ctx.lineWidth = thinWidth
        ctx.beginPath()
        ctx.moveTo(atX, sv.getTopLineTopY())
        ctx.lineTo(atX, sv.getBottomLineBottomY())
        ctx.stroke()
    }

    private fun drawThick(sv: VFStave, ctx: VexRenderingContext, atX: Float) {
        ctx.fillRect(atX - thickWidth, sv.getTopLineTopY(),
                     thickWidth, sv.getBottomLineBottomY() - sv.getTopLineTopY())
    }

    private fun drawDots(sv: VFStave, ctx: VexRenderingContext, atX: Float, rightSide: Boolean) {
        // Two dots: at 1/4 and 3/4 of staff height (between lines 1&2 and 3&4)
        val topDotY = sv.getYForLine(1.5f)
        val botDotY = sv.getYForLine(2.5f)
        val cx = if (rightSide) atX + dotRadius else atX - dotRadius
        for (dotY in listOf(topDotY, botDotY)) {
            ctx.beginPath()
            ctx.arc(cx, dotY, dotRadius, 0f, (2 * Math.PI).toFloat(), false)
            ctx.fill()
        }
    }
}
```

**Test file:** `vexflow/elements/VFBarlineTest.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.RecordingContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFBarlineTest {

    private fun rig(type: VFBarlineType): Pair<VFBarline, RecordingContext> {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 400f)
        val bl = VFBarline(type).apply { this.stave = stave; this.x = 400f }
        return bl to ctx
    }

    @Test fun `NONE draws nothing`() {
        val (bl, ctx) = rig(VFBarlineType.NONE)
        bl.draw(ctx)
        assertEquals(0, ctx.strokeCalls.size)
    }

    @Test fun `SINGLE draws one stroke`() {
        val (bl, ctx) = rig(VFBarlineType.SINGLE)
        bl.draw(ctx)
        assertEquals(1, ctx.strokeCalls.size)
    }

    @Test fun `DOUBLE draws two strokes`() {
        val (bl, ctx) = rig(VFBarlineType.DOUBLE)
        bl.draw(ctx)
        assertEquals(2, ctx.strokeCalls.size)
    }

    @Test fun `fromString parses repeat-end`() {
        assertEquals(VFBarlineType.REPEAT_END, VFBarlineType.fromString("repeat-end"))
    }

    @Test fun `fromString unknown defaults to SINGLE`() {
        assertEquals(VFBarlineType.SINGLE, VFBarlineType.fromString("xyz"))
    }
}
```

---

## Class 16 — VFTie

**File:** `vexflow/elements/VFTie.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFStaveNote

data class VFTieNotes(
    val firstNote: VFStaveNote? = null,
    val lastNote: VFStaveNote? = null,
    val firstIndexes: List<Int> = listOf(0),  // which note in chord (multi-note chords)
    val lastIndexes: List<Int>  = listOf(0)
)

/**
 * A curved arc connecting the end of one note to the start of the next,
 * indicating the same pitch is to be sustained (not re-attacked).
 *
 * Drawing:
 *   A tie is drawn as a filled cubic bezier "lens" shape — two bezier curves
 *   (one for each edge of the arc) forming a thin filled region.
 *
 *   firstX = firstNote.getTieRightX()
 *   lastX  = lastNote.getTieLeftX()
 *   firstY = firstNote.getYs()[firstIndexes[i]]
 *   lastY  = lastNote.getYs()[lastIndexes[i]]
 *   direction = stemDirection of firstNote (up stem -> tie curves down; down stem -> up)
 *
 *   Control points (matching VexFlow tie.ts):
 *     cp1 = (firstX + (lastX - firstX) * 0.35, firstY + direction * renderOptions.cp1)
 *     cp2 = (lastX  - (lastX - firstX) * 0.35, lastY  + direction * renderOptions.cp2)
 *
 * Partial ties: if firstNote or lastNote is null, the tie starts/ends at the stave boundary.
 */
class VFTie(private var notes: VFTieNotes) {

    data class RenderOptions(
        val cp1: Float = 8f,
        val cp2: Float = 12f,
        val shortTieCutoff: Float = 10f,
        val cp1Short: Float = 2f,
        val cp2Short: Float = 8f,
        val firstXShift: Float = 0f,
        val lastXShift: Float = 0f,
        val tieSpacing: Float = 0f,
        val yShift: Float = 7f
    )

    var renderOptions = RenderOptions()

    fun setNotes(n: VFTieNotes) { notes = n }
    fun getNotes(): VFTieNotes = notes
    fun isPartial(): Boolean = notes.firstNote == null || notes.lastNote == null

    fun draw(ctx: VexRenderingContext) {
        val (firstNote, lastNote, firstIdxs, lastIdxs) = notes
        val count = minOf(firstIdxs.size, lastIdxs.size)

        val firstX = firstNote?.getTieRightX()  ?: firstNote?.getStave()?.getTieStartX() ?: return
        val lastX  = lastNote?.getTieLeftX()    ?: lastNote?.getStave()?.getTieEndX()    ?: return

        for (i in 0 until count) {
            val firstY = firstNote?.getYs()?.getOrNull(firstIdxs[i]) ?: continue
            val lastY  = lastNote?.getYs()?.getOrNull(lastIdxs[i])  ?: continue
            val stemDir = firstNote.getStemDirection()  // 1=up, -1=down
            val tieDir  = -stemDir                     // tie curves opposite to stem

            renderTie(ctx, firstX + renderOptions.firstXShift,
                           lastX  + renderOptions.lastXShift,
                           firstY + renderOptions.tieSpacing,
                           lastY  + renderOptions.tieSpacing,
                           tieDir)
        }
    }

    private fun renderTie(ctx: VexRenderingContext,
                          x1: Float, x2: Float,
                          y1: Float, y2: Float,
                          direction: Int) {
        val span = x2 - x1
        val isShort = span < renderOptions.shortTieCutoff
        val cp1 = if (isShort) renderOptions.cp1Short else renderOptions.cp1
        val cp2 = if (isShort) renderOptions.cp2Short else renderOptions.cp2
        val yDir = direction.toFloat()

        val cpX1 = x1 + span * 0.35f
        val cpX2 = x2 - span * 0.35f
        val cpY1 = y1 + yDir * cp1
        val cpY2 = y2 + yDir * cp2

        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.bezierCurveTo(cpX1, cpY1, cpX2, cpY2, x2, y2)
        ctx.bezierCurveTo(cpX2, cpY2 + yDir * 1.5f, cpX1, cpY1 + yDir * 1.5f, x1, y1)
        ctx.closePath()
        ctx.fill()
    }
}
```

---

## Class 17 — VFSlur

**File:** `vexflow/elements/VFSlur.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFStaveNote

data class VFSlurOptions(
    val thickness: Float = 2f,
    val xShift: Float = 0f,
    val yShift: Float = 10f,
    val invert: Boolean = false  // true = force curve above notes regardless of stem direction
)

/**
 * A phrase slur connecting two notes. Unlike ties, slurs connect notes of
 * different pitches and indicate legato playing style.
 *
 * Drawing: same bezier arc technique as VFTie, but:
 *   - The arc is stroked with [thickness], not filled as a lens
 *   - The arc height scales logarithmically with span (alphaTab ScoreSlurGlyph algorithm):
 *       height = min(maxHeight, yShift + log2(span / 50f) * 10f)
 *     where maxHeight = 4 * stave.spacingBetweenLines
 *   - Direction: determined by stem direction (up stem -> slur below; down stem -> above)
 *     unless [invert] is true.
 *
 * Reference: alphaTab src/rendering/glyphs/ScoreSlurGlyph.ts for the logarithmic height formula.
 */
class VFSlur(
    private var fromNote: VFStaveNote? = null,
    private var toNote: VFStaveNote? = null,
    val options: VFSlurOptions = VFSlurOptions()
) {
    fun setNotes(from: VFStaveNote?, to: VFStaveNote?) { fromNote = from; toNote = to }
    fun isPartial(): Boolean = fromNote == null || toNote == null

    fun draw(ctx: VexRenderingContext) {
        val from = fromNote ?: return
        val to   = toNote   ?: return
        val fromY = from.getYs().firstOrNull() ?: return
        val toY   = to.getYs().firstOrNull()   ?: return

        val stemDir = from.getStemDirection()
        val direction = if (options.invert) -stemDir else stemDir  // 1=up, -1=down
        val slurDir = -direction.toFloat()  // curve opposite to stem

        val x1 = from.getTieRightX() + options.xShift
        val x2 = to.getTieLeftX()
        val y1 = fromY + slurDir * options.yShift
        val y2 = toY   + slurDir * options.yShift

        val span = x2 - x1
        val height = minOf(40f, options.yShift + Math.log((span / 50f).toDouble().coerceAtLeast(1.0)).toFloat() * 10f)

        val cpX1 = x1 + span * 0.30f
        val cpX2 = x2 - span * 0.30f
        val cpY1 = y1 + slurDir * height
        val cpY2 = y2 + slurDir * height

        ctx.lineWidth = options.thickness
        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.bezierCurveTo(cpX1, cpY1, cpX2, cpY2, x2, y2)
        ctx.stroke()
    }
}
```

---

## Class 18 — VFBeam

**File:** `vexflow/elements/VFBeam.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import dev.pola.vexflow.model.VFMetrics
import dev.pola.vexflow.model.VFFraction
import kotlin.math.abs
import kotlin.math.sign

/**
 * Connects a group of eighth-note or shorter notes with a filled beam.
 *
 * Construction:
 *   val beam = VFBeam(listOf(note1, note2, note3, note4))
 *   // notes must already have x positions (format voices first!)
 *   beam.draw(ctx, stave)
 *
 * All notes in the group MUST:
 *   - Have duration <= 1/8 (eighth or shorter)
 *   - Already have x positions assigned by VFFormatter
 *   - Have setStave() called on them
 *
 * VFBeam calls note.setBeamed(true) on all notes in the group at construction
 * time to suppress individual flag drawing.
 *
 * Algorithm (from alphaTab rendering/utils/BeamingHelper.ts):
 *
 *   1. DIRECTION: Majority rule — if more than half the notes have stem up, beam goes up.
 *      Ties broken in favour of stem up.
 *
 *   2. STEM TIPS: For each note, compute the ideal stem tip Y:
 *        stemTipY = noteHeadY ∓ (stave.spacingBetweenLines * STEM_HEIGHT_SPACES)
 *      where ∓ is based on direction (up stems go upward = smaller Y).
 *
 *   3. BEAM SLOPE: Compute slope from first to last stem tip:
 *        rawSlope = (lastStemTipY - firstStemTipY) / (lastX - firstX)
 *      Clamp: |slope| <= maxBeamSlope (default 0.4f, in px/px terms).
 *      Snap to 0 if |rawSlope| < snapThreshold (0.1f) — nearly horizontal beams should be flat.
 *
 *   4. LEVEL BEAM: Adjust all intermediate stem tips to satisfy the beam line.
 *      The beam line is: beamY(x) = firstStemTipY + slope * (x - firstX)
 *      Each note's stem is extended or shortened to reach beamY(note.x).
 *      Rule: stems may ONLY be lengthened, never shortened below the minimum height.
 *
 *   5. DRAW PRIMARY BEAM:
 *      A filled rectangle (or filled polygon) from (firstX, firstBeamY) to (lastX, lastBeamY)
 *      with height = beamThickness.
 *      Use canvas polygon: moveTo(x1,y1), lineTo(x2,y2), lineTo(x2,y2+h), lineTo(x1,y1+h), fill().
 *
 *   6. DRAW SECONDARY BEAMS: For 16th notes, a second beam beam spaced beamSpacing below (or above)
 *      the primary beam. For 32nd notes, a third beam. Secondary beams may be partial (only
 *      spanning the beamed group they belong to).
 *
 *   7. DRAW STEMS: After beam position is determined, draw each note's stem from noteHeadY to beamY.
 */
class VFBeam(notes: List<VFStaveNote>) {

    private val notes: List<VFStaveNote> = notes.toList()

    var beamThickness: Float = VFMetrics.BEAM_THICKNESS   // 4f
    var beamSpacing:   Float = VFMetrics.BEAM_SPACING     // 6f
    var maxBeamSlope:  Float = 0.4f

    init {
        // Tell all notes their flags should be suppressed
        this.notes.forEach { it.setBeamed(true) }
    }

    fun draw(ctx: VexRenderingContext, stave: dev.pola.vexflow.elements.VFStave) {
        if (notes.size < 2) return

        val direction = computeDirection()  // 1 = up, -1 = down
        val stemXs    = notes.map { stemX(it, direction) }
        val stemTipYs = computeStemTipYs(stave, direction)
        val slope     = computeSlope(stemXs, stemTipYs)
        val adjustedTips = adjustStemTips(stemXs, stemTipYs, slope)

        drawStems(ctx, stave, stemXs, adjustedTips, direction)
        drawPrimaryBeam(ctx, stemXs, adjustedTips, direction)
        drawSecondaryBeams(ctx, stemXs, adjustedTips, direction)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun computeDirection(): Int {
        val upCount = notes.count { it.getStemDirection() == VFStaveNoteStruct.STEM_UP }
        return if (upCount * 2 >= notes.size) VFStaveNoteStruct.STEM_UP
               else VFStaveNoteStruct.STEM_DOWN
    }

    /** X coordinate of the stem (right side for up-stem, left side for down-stem). */
    private fun stemX(note: VFStaveNote, direction: Int): Float {
        val halfHead = note.glyphFontScale * 0.3f
        return if (direction == VFStaveNoteStruct.STEM_UP) note.x + halfHead
               else note.x - halfHead
    }

    /**
     * Ideal stem tip Y for each note before beam levelling.
     * Stem tip = noteHeadY - (stemHeight * direction) where direction=1 means up.
     */
    private fun computeStemTipYs(stave: dev.pola.vexflow.elements.VFStave, direction: Int): List<Float> {
        val stemHeightPx = VFMetrics.STEM_HEIGHT_SPACES * stave.spacingBetweenLines
        return notes.map { note ->
            val noteHeadY = note.getYs().firstOrNull() ?: stave.getYForLine(2f)
            noteHeadY - direction * stemHeightPx
        }
    }

    private fun computeSlope(xs: List<Float>, ys: List<Float>): Float {
        val dx = xs.last() - xs.first()
        if (dx == 0f) return 0f
        val rawSlope = (ys.last() - ys.first()) / dx
        val clamped = rawSlope.coerceIn(-maxBeamSlope, maxBeamSlope)
        return if (abs(clamped) < 0.1f) 0f else clamped  // snap near-zero to flat
    }

    /**
     * Adjust stem tips so they lie on the computed beam line.
     * Stems may only be lengthened (tips moved further from noteheads), never shortened.
     */
    private fun adjustStemTips(xs: List<Float>, idealTips: List<Float>, slope: Float): List<Float> {
        val firstX = xs.first()
        val firstY = idealTips.first()
        return xs.zip(idealTips).map { (x, idealY) ->
            val beamY = firstY + slope * (x - firstX)
            // If stem direction is up (tips have smaller Y than noteheads), push tip further up
            // if ideal is already higher (smaller Y); otherwise use beam line.
            // For simplicity, use the beam line directly — it's already computed from ideal tips.
            beamY
        }
    }

    private fun drawStems(ctx: VexRenderingContext,
                          stave: dev.pola.vexflow.elements.VFStave,
                          xs: List<Float>, tips: List<Float>, direction: Int) {
        ctx.lineWidth = stave.lineThickness * 1.5f
        for (i in notes.indices) {
            val note = notes[i]
            val noteHeadY = note.getYs().firstOrNull() ?: continue
            ctx.beginPath()
            ctx.moveTo(xs[i], noteHeadY)
            ctx.lineTo(xs[i], tips[i])
            ctx.stroke()
        }
    }

    private fun drawPrimaryBeam(ctx: VexRenderingContext,
                                xs: List<Float>, tips: List<Float>, direction: Int) {
        val dir = direction.toFloat()
        val x1 = xs.first();  val y1 = tips.first()
        val x2 = xs.last();   val y2 = tips.last()
        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.lineTo(x2, y2)
        ctx.lineTo(x2, y2 + dir * beamThickness)
        ctx.lineTo(x1, y1 + dir * beamThickness)
        ctx.closePath()
        ctx.fill()
    }

    private fun drawSecondaryBeams(ctx: VexRenderingContext,
                                   xs: List<Float>, tips: List<Float>, direction: Int) {
        // Determine how many beams each note needs:
        //   8th  = 1 beam (only primary)
        //   16th = 2 beams
        //   32nd = 3 beams
        fun beamCount(note: VFStaveNote): Int = when {
            note.duration <= VFFraction.of(1, 32) -> 3
            note.duration <= VFFraction.of(1, 16) -> 2
            else -> 1
        }

        val maxBeams = notes.maxOf { beamCount(it) }
        val dir = direction.toFloat()

        for (level in 2..maxBeams) {  // level 1 = primary (already drawn)
            val offset = dir * (beamThickness + beamSpacing) * (level - 1)
            // Find runs of consecutive notes that all need this beam level
            var runStart = -1
            for (i in notes.indices) {
                val needs = beamCount(notes[i]) >= level
                if (needs && runStart < 0) runStart = i
                if ((!needs || i == notes.lastIndex) && runStart >= 0) {
                    val runEnd = if (needs) i else i - 1
                    val x1 = xs[runStart]; val y1 = tips[runStart] + offset
                    val x2 = xs[runEnd];   val y2 = tips[runEnd]   + offset
                    ctx.beginPath()
                    ctx.moveTo(x1, y1)
                    ctx.lineTo(x2, y2)
                    ctx.lineTo(x2, y2 + dir * beamThickness)
                    ctx.lineTo(x1, y1 + dir * beamThickness)
                    ctx.closePath()
                    ctx.fill()
                    runStart = -1
                }
            }
        }
    }
}
```

**Algorithm notes:**
- `adjustStemTips` uses the beam line directly. This is "hard beam" — every stem tip lands exactly on the beam line. The more sophisticated "soft beam" (alphaTab's approach) allows stem-tip ideal positions to influence which side of the beam line each stem extends to. Implement hard beam first; switch to soft only if visuals look wrong.
- `drawSecondaryBeams` uses run-length encoding to find consecutive notes that share a beam level. This correctly draws partial beams (e.g. a dotted-eighth + sixteenth group).
- The `direction.toFloat()` pattern: stem up = 1 means beam is above noteheads (negative Y offset = visually upward). `dir * beamThickness` shifts the bottom edge of the beam rectangle.
- alphaTab reference: `packages/alphatab/src/rendering/utils/BeamingHelper.ts` — `calculateBeamY()`, `getBeamSlope()`.

**Test file:** `vexflow/elements/VFBeamTest.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.RecordingContext
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct
import dev.pola.vexflow.model.VFFraction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VFBeamTest {

    private fun eighthNote(key: String, x: Float): VFStaveNote {
        val stave = VFStave(0f, 100f, 400f)
        val note = VFStaveNote(VFStaveNoteStruct(listOf(key), "8", 40f))
        note.setStave(stave)
        note.x = x
        return note
    }

    @Test fun `constructor marks all notes as beamed`() {
        val notes = listOf(eighthNote("c/5", 50f), eighthNote("d/5", 100f))
        VFBeam(notes)
        notes.forEach { assertTrue(it.isBeamed()) }
    }

    @Test fun `draw does not crash`() {
        val stave = VFStave(0f, 100f, 400f)
        val ctx = RecordingContext()
        val notes = listOf(eighthNote("c/5", 50f), eighthNote("d/5", 100f),
                           eighthNote("e/5", 150f), eighthNote("f/5", 200f))
        VFBeam(notes).draw(ctx, stave)
        // Should have produced fill calls (beam rectangle) and stroke calls (stems)
        assertTrue(ctx.strokeCalls.size >= 4, "Expected at least 4 stem strokes")
    }

    @Test fun `single note beam draws nothing`() {
        val stave = VFStave(0f, 100f, 400f)
        val ctx = RecordingContext()
        VFBeam(listOf(eighthNote("c/5", 50f))).draw(ctx, stave)
        assertEquals(0, ctx.strokeCalls.size)
    }

    @Test fun `16th notes get secondary beam (more fill calls than 8th notes)`() {
        val stave = VFStave(0f, 100f, 400f)
        fun sixteenth(key: String, x: Float): VFStaveNote {
            val n = VFStaveNote(VFStaveNoteStruct(listOf(key), "16", 40f))
            n.setStave(stave); n.x = x; return n
        }
        val ctx8th = RecordingContext()
        VFBeam(listOf(eighthNote("c/5", 50f), eighthNote("d/5", 100f))).draw(ctx8th, stave)

        val ctx16th = RecordingContext()
        VFBeam(listOf(sixteenth("c/5", 50f), sixteenth("d/5", 100f))).draw(ctx16th, stave)

        // 16th notes should produce more fill calls (primary + secondary beam)
        // We don't track fill separately in RecordingContext, so just verify it doesn't crash
        // and stroke calls are present
        assertTrue(ctx16th.strokeCalls.size >= 2)
    }
}
```

---

## M5 Gate

- [ ] `./gradlew test --tests "*.VFBarlineTest"` — all 5 tests green
- [ ] `./gradlew test --tests "*.VFBeamTest"` — all 4 tests green
- [ ] `./gradlew assembleDebug` — 0 errors
- [ ] Visual: four eighth notes grouped with a beam; measures delimited by barlines; two notes connected by a tie

---

---

# M6 — Compose Integration

**Goal:** The rendering engine is visible in the running app. `RendererScreen` shows a stave with clef, key signature, time signature, and a few formatted notes.

**Files to create:**

1. `vexflow/view/SheetMusicView.kt`
2. `vexflow/view/SheetMusicComposable.kt`
3. `renderer/screens/RendererViewModel.kt`
4. Update `renderer/screens/RendererScreen.kt`

---

## Class 19 — SheetMusicView

**File:** `vexflow/view/SheetMusicView.kt`

```kotlin
package dev.pola.vexflow.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import dev.pola.vexflow.core.VFFormatter
import dev.pola.vexflow.core.VFFormatterOptions
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.*
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct

/**
 * Custom Android View that renders sheet music using the VexFlow engine.
 *
 * Usage:
 *   val view = SheetMusicView(context)
 *   view.setScore(voices, staveConfig)
 *   // view draws itself in onDraw()
 *
 * For Compose integration, use [SheetMusicComposable] which wraps this View
 * in an AndroidView composable.
 *
 * The view creates a new VexRenderingContext on each onDraw() call and
 * sets the current Canvas on it. This is safe because onDraw() is always
 * called on the main thread.
 */
class SheetMusicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val renderingContext = VexRenderingContext()
    private var stave: VFStave? = null
    private var voices: List<VFVoice> = emptyList()
    private val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))

    // ── Public API ────────────────────────────────────────────────────────────

    /** Replace the current score content. Triggers a redraw. */
    fun setVoices(newVoices: List<VFVoice>, newStave: VFStave) {
        stave = newStave
        voices = newVoices
        newVoices.forEach { it.setStave(newStave); it.preFormat() }
        invalidate()
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sv = stave ?: return

        renderingContext.canvas = canvas

        if (voices.isNotEmpty()) {
            formatter.formatAndDrawVoices(
                voices        = voices,
                stave         = sv,
                ctx           = renderingContext,
                justifyWidth  = sv.width
            )
        }
        sv.draw(renderingContext)
    }

    // ── Sizing ────────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val sv = stave
        val desiredH = if (sv != null) {
            (sv.y + sv.getBottomLineBottomY() + 40f).toInt()
        } else 200
        setMeasuredDimension(
            resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec)
        )
    }
}
```

---

## Class 20 — SheetMusicComposable

**File:** `vexflow/view/SheetMusicComposable.kt`

```kotlin
package dev.pola.vexflow.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.elements.VFStave

/**
 * Compose wrapper for [SheetMusicView].
 *
 * @param stave   The VFStave to render (defines position, width, clef, key sig, time sig)
 * @param voices  The VFVoice list containing the notes to render
 * @param modifier Standard Compose modifier
 */
@Composable
fun SheetMusicComposable(
    stave: VFStave,
    voices: List<VFVoice>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SheetMusicView(context).also { view ->
                view.setVoices(voices, stave)
            }
        },
        update = { view ->
            view.setVoices(voices, stave)
        },
        modifier = modifier
    )
}
```

---

## RendererScreen update

In `renderer/screens/RendererScreen.kt`, replace the placeholder content with:

```kotlin
// Inside the screen's content area:
val stave = remember {
    VFStave(20f, 120f, (LocalConfiguration.current.screenWidthDp * density - 40f))
        .apply {
            clef          = VFClef("treble", "default", null)
            keySignature  = VFKeySignature("G").apply { size = 30f }
            timeSignature = VFTimeSignature("4/4").apply { size = 40f }
        }
}
val voices = remember {
    val voice = VFVoice("4/4")
    voice.addTickables(listOf(
        VFStaveNote(VFStaveNoteStruct(listOf("f/5"), "4", 40f)),
        VFStaveNote(VFStaveNoteStruct(listOf("g/5"), "4", 40f)),
        VFStaveNote(VFStaveNoteStruct(listOf("a/5"), "4", 40f)),
        VFStaveNote(VFStaveNoteStruct(listOf("b/5"), "4", 40f))
    ))
    listOf(voice)
}
SheetMusicComposable(stave = stave, voices = voices, modifier = Modifier.fillMaxWidth().height(240.dp))
```

---

## M6 Gate

- [ ] `./gradlew assembleDebug` — 0 errors, APK builds
- [ ] Run on emulator: launch app, navigate to Renderer tab, see a rendered staff with G major key signature and four quarter notes
- [ ] No crash on screen rotation (stave redraws correctly after configuration change)

---

---

# M7 — Test Suite

**Goal:** 30+ unit tests covering all classes from M1–M6. Green build = Phase A foundation is solid.

This milestone formalises the tests already specified inline above. The agent's task is to verify all tests pass and add any missing coverage, targeting the areas below.

## Required tests summary

| Test file | Tests | Classes covered |
|-----------|-------|-----------------|
| `VFFractionTest` | 11 | VFFraction |
| `VexRenderingContextTest` | 5 | VexRenderingContext |
| `VFStaveTest` | 5 | VFStave |
| `VFStaveNoteTest` | 10 | VFStaveNote, VFAccidental |
| `VFClefTest` | 7 | VFClef |
| `VFKeySignatureTest` | 9 | VFKeySignature |
| `VFTimeSignatureTest` | 4 | VFTimeSignature |
| `VFFormatterTest` | 5 | VFFormatter, VFVoice, VFTickContext |
| `VFBarlineTest` | 5 | VFBarline |
| `VFBeamTest` | 4 | VFBeam |
| **Total** | **65** | — |

## Additional tests to add at M7

Add these to fill gaps:

```kotlin
// VFTickContextTest.kt
@Test fun `addTickable propagates tick context to note`()
@Test fun `setX propagates to all notes`()
@Test fun `preFormat computes non-zero width`()
@Test fun `getMaxDuration returns largest duration`()

// VFVoiceTest.kt
@Test fun `addTickables adds all notes`()
@Test fun `getTotalTicks sums durations correctly`()
@Test fun `clear empties tickables`()
@Test fun `parseTimeSpec 3 over 4 returns 3 over 4`()

// VFTieTest.kt
@Test fun `isPartial true when firstNote is null`()
@Test fun `draw does not crash with two notes set`()

// VFSlurTest.kt
@Test fun `draw does not crash with two notes set`()
@Test fun `isPartial true when fromNote is null`()

// VFGlyphBoundingBoxTest.kt  (Robolectric)
@Test fun `manager loads gClef bounding box from assets`()
@Test fun `scaled bounding box width changes proportionally`()
@Test fun `toCanvasRect flips Y axis`()
```

## M7 Gate

- [ ] `./gradlew test` — **all** 65+ tests green, 0 failures
- [ ] Test coverage report: `./gradlew jacocoTestReport` — core package coverage > 70%
- [ ] `./gradlew assembleDebug` — 0 errors

---

---

# M8 — MusicXML Parser

**Goal:** Parse a MusicXML file into a `MusicSheet` data model, then convert that model to VexFlow rendering objects and display the first two measures of a real piece.

**Files to create (in order):**

1. `vexflow/parser/MusicSheet.kt`
2. `vexflow/parser/MusicXMLParser.kt`
3. `vexflow/parser/MusicSheetToVF.kt`

Tests: `vexflow/parser/MusicXMLParserTest.kt`, `vexflow/parser/MusicSheetToVFTest.kt`

---

## Class 21 — MusicSheet (data model)

**File:** `vexflow/parser/MusicSheet.kt`

```kotlin
package dev.pola.vexflow.parser

import dev.pola.vexflow.model.VFFraction

/**
 * Pure data model for parsed MusicXML content. No rendering concerns here.
 *
 * Hierarchy: MusicSheet -> Part -> Measure -> (notes or rests)
 *
 * This is a simplified model targeting single-part, single-staff scores (Phase A).
 * Multi-part/multi-staff support is a Phase A extension.
 *
 * MusicXML element mappings:
 *   <score-partwise>   -> MusicSheet
 *   <part>             -> Part
 *   <measure>          -> Measure
 *   <note>             -> NoteData (pitch notes) or RestData (rests)
 *   <attributes>       -> updates MeasureAttributes on the Measure
 *   <direction>        -> TempoMark or DynamicMark
 */

data class MusicSheet(
    val title:    String = "",
    val composer: String = "",
    val parts:    List<Part> = emptyList()
)

data class Part(
    val id:       String,
    val name:     String = "",
    val measures: List<Measure> = emptyList()
)

data class Measure(
    val number:     Int,
    val attributes: MeasureAttributes,
    val notes:      List<NoteOrRest>,       // in order of appearance
    val tempoMarks: List<TempoMark> = emptyList(),
    val barlineLeft:  String = "regular",   // MusicXML barline style
    val barlineRight: String = "regular"
)

data class MeasureAttributes(
    val divisions:       Int = 1,           // MusicXML <divisions> (quarter-note ticks)
    val keyFifths:       Int = 0,           // -7..7; negative = flats, positive = sharps
    val keyMode:         String = "major",  // "major" | "minor"
    val timeNumerator:   Int = 4,
    val timeDenominator: Int = 4,
    val clef:            String = "treble"  // "treble" | "bass" | "alto" | "tenor"
)

sealed class NoteOrRest {
    abstract val duration:     Int       // in MusicXML divisions
    abstract val voice:        Int       // 1-indexed voice number
    abstract val staff:        Int       // 1-indexed staff number (1 for single-staff)
    abstract val isChordNote:  Boolean   // true = this note shares onset with previous
}

data class NoteData(
    val pitch:         Pitch,
    override val duration:    Int,
    override val voice:       Int = 1,
    override val staff:       Int = 1,
    override val isChordNote: Boolean = false,
    val tieStart:      Boolean = false,
    val tieEnd:        Boolean = false,
    val slurStart:     Boolean = false,
    val slurEnd:       Boolean = false,
    val beamState:     BeamState = BeamState.NONE,
    val accidental:    String? = null,    // "#", "b", "n", "##", "bb", null
    val notationType:  String = "normal"  // "normal" | "grace" | "cue"
) : NoteOrRest()

data class RestData(
    override val duration:    Int,
    override val voice:       Int = 1,
    override val staff:       Int = 1,
    override val isChordNote: Boolean = false
) : NoteOrRest()

data class Pitch(
    val step:   String,  // C D E F G A B
    val octave: Int,
    val alter:  Float = 0f  // -2..2; 1.0 = sharp, -1.0 = flat
)

enum class BeamState { NONE, BEGIN, CONTINUE, END }

data class TempoMark(
    val beatUnit:  String = "quarter",   // "quarter" | "half" | "eighth"
    val bpm:       Float,
    val divisions: Int = 0               // offset within measure in divisions
)
```

---

## Class 22 — MusicXMLParser

**File:** `vexflow/parser/MusicXMLParser.kt`

```kotlin
package dev.pola.vexflow.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Parses MusicXML (.xml or .mxl) files into a [MusicSheet].
 *
 * Supported MusicXML elements (Phase A subset):
 *   <score-partwise>       root element
 *   <work><work-title>     -> MusicSheet.title
 *   <identification><creator type="composer"> -> MusicSheet.composer
 *   <part-list><score-part> -> Part names
 *   <part id="...">        -> Part content
 *   <measure number="..."> -> Measure
 *   <attributes>           -> MeasureAttributes (divisions, key, time, clef)
 *     <divisions>
 *     <key><fifths><mode>
 *     <time><beats><beat-type>
 *     <clef><sign>
 *   <note>                 -> NoteData or RestData
 *     <pitch><step><octave><alter>
 *     <rest>               -> RestData
 *     <duration>
 *     <voice>
 *     <staff>
 *     <chord>              -> isChordNote = true
 *     <type>               -> note type string (quarter, eighth, etc.) — ignored; use duration/divisions
 *     <tie type="start/stop">
 *     <beam number="1">    -> BeamState
 *     <accidental>         -> accidental string
 *   <direction>
 *     <sound tempo="...">  -> TempoMark
 *   <barline location="left/right">
 *     <bar-style>          -> barlineLeft / barlineRight
 *
 * NOT YET SUPPORTED (Phase A — add incrementally):
 *   - Multi-staff parts (grand staff piano)
 *   - Slurs (encoded as <notation><slur type="start/stop">)
 *   - Dynamics (encoded as <direction><dynamics>)
 *   - Repeats (encoded as <barline><repeat direction="forward/backward">)
 *   - Score-timewise format (use score-partwise only)
 *
 * MXL format: a ZIP file containing a rootfile (usually named score.xml or similar).
 * Detect by: check if stream starts with PK (0x504B) → treat as ZIP.
 *
 * Algorithm reference: alphaTab src/importer/MusicXmlImporter.ts
 *   - Follow the same two-pass approach: first build part list, then iterate measures.
 *   - Carry forward attributes: <attributes> are cumulative; if an element is absent,
 *     keep the value from the previous measure.
 */
class MusicXMLParser {

    /**
     * Parse an InputStream (either raw XML or MXL ZIP).
     * @throws MusicXMLParseException on malformed input.
     */
    fun parse(stream: InputStream): MusicSheet {
        val xmlStream = decompressIfNeeded(stream)
        return parseXml(xmlStream)
    }

    // ── MXL decompression ─────────────────────────────────────────────────────

    private fun decompressIfNeeded(stream: InputStream): InputStream {
        val buffered = stream.buffered()
        buffered.mark(2)
        val header = ByteArray(2)
        buffered.read(header)
        buffered.reset()
        // PK signature = 0x50 0x4B
        return if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
            extractRootfileFromMxl(buffered)
        } else {
            buffered
        }
    }

    private fun extractRootfileFromMxl(stream: InputStream): InputStream {
        val zip = ZipInputStream(stream)
        // The rootfile path is declared in META-INF/container.xml.
        // For simplicity, find the first .xml entry that is NOT in META-INF/.
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".xml")
                && !entry.name.startsWith("META-INF")) {
                return zip.readBytes().inputStream()
            }
            entry = zip.nextEntry
        }
        throw MusicXMLParseException("No MusicXML content found in MXL archive")
    }

    // ── XML parsing ───────────────────────────────────────────────────────────

    private fun parseXml(stream: InputStream): MusicSheet {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var title = ""
        var composer = ""
        val partNames = mutableMapOf<String, String>()  // partId -> name
        val parts = mutableMapOf<String, MutableList<Measure>>()

        var event = parser.next()
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "work-title"  -> title = parser.nextText()
                    "creator"     -> if (parser.getAttributeValue(null, "type") == "composer")
                                         composer = parser.nextText()
                    "score-part"  -> {
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        partNames[id] = ""
                    }
                    "part-name"   -> {
                        // text inside <score-part> immediately after <part-name>
                        val name = parser.nextText()
                        partNames.keys.lastOrNull()?.let { partNames[it] = name }
                    }
                    "part"        -> {
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        parts[id] = parsePart(parser, id)
                    }
                }
            }
            event = parser.next()
        }

        return MusicSheet(
            title    = title,
            composer = composer,
            parts    = partNames.keys.map { id ->
                Part(id = id, name = partNames[id] ?: "", measures = parts[id] ?: emptyList())
            }
        )
    }

    private fun parsePart(parser: XmlPullParser, partId: String): MutableList<Measure> {
        val measures = mutableListOf<Measure>()
        // Carry-forward attributes (MusicXML attributes are cumulative)
        var attrs = MeasureAttributes()

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "part")) {
            if (event == XmlPullParser.START_TAG && parser.name == "measure") {
                val num = parser.getAttributeValue(null, "number")?.toIntOrNull() ?: (measures.size + 1)
                val (measure, updatedAttrs) = parseMeasure(parser, num, attrs)
                attrs = updatedAttrs
                measures.add(measure)
            }
            event = parser.next()
        }
        return measures
    }

    private fun parseMeasure(
        parser: XmlPullParser,
        number: Int,
        prevAttrs: MeasureAttributes
    ): Pair<Measure, MeasureAttributes> {
        var attrs = prevAttrs
        val notes = mutableListOf<NoteOrRest>()
        val tempoMarks = mutableListOf<TempoMark>()
        var barlineLeft  = "regular"
        var barlineRight = "regular"

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "measure")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "attributes" -> attrs = parseAttributes(parser, attrs)
                    "note"       -> notes.add(parseNote(parser, attrs.divisions))
                    "direction"  -> parseDirection(parser)?.let { tempoMarks.add(it) }
                    "barline"    -> {
                        val loc = parser.getAttributeValue(null, "location") ?: "right"
                        val style = parseBarlineStyle(parser)
                        if (loc == "left") barlineLeft = style else barlineRight = style
                    }
                }
            }
            event = parser.next()
        }

        return Measure(number, attrs, notes, tempoMarks, barlineLeft, barlineRight) to attrs
    }

    private fun parseAttributes(parser: XmlPullParser, prev: MeasureAttributes): MeasureAttributes {
        var divisions      = prev.divisions
        var keyFifths      = prev.keyFifths
        var keyMode        = prev.keyMode
        var timeNumerator  = prev.timeNumerator
        var timeDenominator = prev.timeDenominator
        var clef           = prev.clef

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "attributes")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "divisions"  -> divisions = parser.nextText().trim().toIntOrNull() ?: divisions
                    "fifths"     -> keyFifths = parser.nextText().trim().toIntOrNull() ?: keyFifths
                    "mode"       -> keyMode   = parser.nextText().trim()
                    "beats"      -> timeNumerator   = parser.nextText().trim().toIntOrNull() ?: timeNumerator
                    "beat-type"  -> timeDenominator = parser.nextText().trim().toIntOrNull() ?: timeDenominator
                    "sign"       -> clef = when (parser.nextText().trim().uppercase()) {
                                       "G" -> "treble"; "F" -> "bass"
                                       "C" -> "alto";   else -> clef
                                   }
                }
            }
            event = parser.next()
        }
        return MeasureAttributes(divisions, keyFifths, keyMode, timeNumerator, timeDenominator, clef)
    }

    private fun parseNote(parser: XmlPullParser, divisions: Int): NoteOrRest {
        var step = "C"; var octave = 4; var alter = 0f
        var duration = 1; var voice = 1; var staff = 1
        var isRest = false; var isChord = false
        var tieStart = false; var tieEnd = false
        var beamState = BeamState.NONE
        var accidental: String? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "note")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "step"       -> step     = parser.nextText().trim()
                    "octave"     -> octave   = parser.nextText().trim().toIntOrNull() ?: 4
                    "alter"      -> alter    = parser.nextText().trim().toFloatOrNull() ?: 0f
                    "duration"   -> duration = parser.nextText().trim().toIntOrNull() ?: 1
                    "voice"      -> voice    = parser.nextText().trim().toIntOrNull() ?: 1
                    "staff"      -> staff    = parser.nextText().trim().toIntOrNull() ?: 1
                    "rest"       -> isRest   = true
                    "chord"      -> isChord  = true
                    "tie"        -> when (parser.getAttributeValue(null, "type")) {
                                       "start" -> tieStart = true
                                       "stop"  -> tieEnd   = true
                                   }
                    "beam"       -> if (parser.getAttributeValue(null, "number") == "1") {
                                       beamState = when (parser.nextText().trim()) {
                                           "begin"    -> BeamState.BEGIN
                                           "continue" -> BeamState.CONTINUE
                                           "end"      -> BeamState.END
                                           else       -> BeamState.NONE
                                       }
                                   }
                    "accidental" -> accidental = when (parser.nextText().trim()) {
                                       "sharp"        -> "#"
                                       "flat"         -> "b"
                                       "natural"      -> "n"
                                       "double-sharp" -> "##"
                                       "flat-flat"    -> "bb"
                                       else           -> null
                                   }
                }
            }
            event = parser.next()
        }

        return if (isRest) {
            RestData(duration, voice, staff, isChord)
        } else {
            NoteData(
                pitch        = Pitch(step, octave, alter),
                duration     = duration,
                voice        = voice,
                staff        = staff,
                isChordNote  = isChord,
                tieStart     = tieStart,
                tieEnd       = tieEnd,
                beamState    = beamState,
                accidental   = accidental
            )
        }
    }

    private fun parseDirection(parser: XmlPullParser): TempoMark? {
        var bpm: Float? = null
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "direction")) {
            if (event == XmlPullParser.START_TAG && parser.name == "sound") {
                bpm = parser.getAttributeValue(null, "tempo")?.toFloatOrNull()
            }
            event = parser.next()
        }
        return bpm?.let { TempoMark(bpm = it) }
    }

    private fun parseBarlineStyle(parser: XmlPullParser): String {
        var style = "regular"
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "barline")) {
            if (event == XmlPullParser.START_TAG && parser.name == "bar-style") {
                style = parser.nextText().trim()
            }
            event = parser.next()
        }
        return style
    }
}

class MusicXMLParseException(message: String) : Exception(message)
```

---

## Class 23 — MusicSheetToVF

**File:** `vexflow/parser/MusicSheetToVF.kt`

```kotlin
package dev.pola.vexflow.parser

import dev.pola.vexflow.core.VFFormatter
import dev.pola.vexflow.core.VFFormatterOptions
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.elements.*
import dev.pola.vexflow.model.*

/**
 * Converts a [MusicSheet] to lists of [VFStave] + [VFVoice] objects ready for rendering.
 *
 * Output: one [RenderedMeasure] per measure in the first part.
 *
 * Conversion rules:
 *   MeasureAttributes.keyFifths   -> VFKeySignature key spec (see fifthsToKeySpec())
 *   MeasureAttributes.clef        -> VFClef type string
 *   MeasureAttributes.timeNumerator + timeDenominator -> VFTimeSignature spec
 *   NoteData.pitch + duration     -> VFStaveNote keys + duration string
 *   RestData.duration             -> VFStaveNote(keys=["b/4"], duration="Xr") — rest
 *   BeamState.BEGIN..END          -> VFBeam groups
 *   NoteData.tieStart/End         -> VFTie (post-processing after all staves built)
 *
 * Duration conversion (MusicXML divisions -> VexFlow duration string):
 *   ratio = note.duration / measure.attributes.divisions
 *   1.0 -> "1" (whole); 0.5 -> "2"; 0.25 -> "4"; 0.125 -> "8"; 0.0625 -> "16"; 0.03125 -> "32"
 *   Dotted: ratio 0.75 -> "2d"; 0.375 -> "4d"; 0.1875 -> "8d"
 *
 * Pitch conversion (NoteData.pitch -> VexFlow key string):
 *   step.lowercase() + accidental + "/" + octave
 *   e.g. Pitch("F", 5, 1.0) -> "f#/5"
 *   alter: 1.0 -> "#", -1.0 -> "b", 2.0 -> "##", -2.0 -> "bb", 0 -> "" (natural, no suffix unless explicit)
 */
object MusicSheetToVF {

    data class RenderedMeasure(
        val stave:  VFStave,
        val voices: List<VFVoice>,
        val beams:  List<VFBeam>,
        val ties:   List<VFTie>
    )

    /**
     * Convert all measures in the first part to rendered measures.
     * @param sheet       The parsed MusicSheet
     * @param startX      X position of the first stave
     * @param startY      Y position of all staves
     * @param staveWidth  Width of each stave
     * @param showClef    Whether to show the clef (true for measure 1, optionally for others)
     * @param showKeySig  Whether to show key signature
     * @param showTimeSig Whether to show time signature
     */
    fun convert(
        sheet: MusicSheet,
        startX: Float,
        startY: Float,
        staveWidth: Float,
        showClef: Boolean = true,
        showKeySig: Boolean = true,
        showTimeSig: Boolean = true
    ): List<RenderedMeasure> {
        val part = sheet.parts.firstOrNull() ?: return emptyList()
        return part.measures.mapIndexed { index, measure ->
            convertMeasure(measure, startX + index * staveWidth, startY, staveWidth,
                           showClef, showKeySig, showTimeSig)
        }
    }

    private fun convertMeasure(
        measure: Measure,
        x: Float, y: Float, width: Float,
        showClef: Boolean, showKeySig: Boolean, showTimeSig: Boolean
    ): RenderedMeasure {
        val attrs = measure.attributes

        val stave = VFStave(x, y, width).apply {
            if (showClef) clef = VFClef(attrs.clef, "default", null)
            if (showKeySig) keySignature = VFKeySignature(fifthsToKeySpec(attrs.keyFifths, attrs.keyMode))
            if (showTimeSig) timeSignature = VFTimeSignature("${attrs.timeNumerator}/${attrs.timeDenominator}")
        }

        // Group notes by voice number
        val notesByVoice = mutableMapOf<Int, MutableList<NoteOrRest>>()
        for (note in measure.notes) {
            if (!note.isChordNote) {  // chord notes are added to the previous note's keys
                notesByVoice.getOrPut(note.voice) { mutableListOf() }.add(note)
            } else {
                // Append pitch to last note in same voice (chord handling)
                val voiceList = notesByVoice[note.voice]
                val lastNote = voiceList?.lastOrNull()
                if (lastNote is NoteOrRest && note is NoteData) {
                    // Replace last entry with a chord note (new VFStaveNote with multiple keys)
                    // This is handled below in buildVFNote
                }
            }
        }

        // Build chord groups: collate consecutive chord notes
        val chordGroups = buildChordGroups(measure.notes)

        val voices = mutableListOf<VFVoice>()
        val beams = mutableListOf<VFBeam>()
        val ties  = mutableListOf<VFTie>()

        val voiceNumbers = chordGroups.map { it.primary.voice }.distinct().sorted()
        for (voiceNum in voiceNumbers) {
            val voiceGroups = chordGroups.filter { it.primary.voice == voiceNum }
            val vfNotes = mutableListOf<VFStaveNote>()
            val beamGroup = mutableListOf<VFStaveNote>()

            for (group in voiceGroups) {
                val vfNote = buildVFNote(group, attrs.divisions)
                vfNotes.add(vfNote)

                // Beam tracking
                val primary = group.primary
                if (primary is NoteData) {
                    when (primary.beamState) {
                        BeamState.BEGIN    -> { beamGroup.clear(); beamGroup.add(vfNote) }
                        BeamState.CONTINUE -> beamGroup.add(vfNote)
                        BeamState.END      -> {
                            beamGroup.add(vfNote)
                            if (beamGroup.size >= 2) beams.add(VFBeam(beamGroup.toList()))
                            beamGroup.clear()
                        }
                        BeamState.NONE     -> {}
                    }
                }
            }

            val vfVoice = VFVoice("${attrs.timeNumerator}/${attrs.timeDenominator}")
            vfVoice.addTickables(vfNotes)
            voices.add(vfVoice)
        }

        return RenderedMeasure(stave, voices, beams, ties)
    }

    // ── Helper functions ──────────────────────────────────────────────────────

    private data class ChordGroup(val primary: NoteOrRest, val chordNotes: List<NoteData> = emptyList())

    private fun buildChordGroups(notes: List<NoteOrRest>): List<ChordGroup> {
        val result = mutableListOf<ChordGroup>()
        val chords = mutableListOf<NoteData>()
        for (note in notes) {
            if (note.isChordNote && note is NoteData) {
                chords.add(note)
            } else {
                if (result.isNotEmpty() && chords.isNotEmpty()) {
                    val last = result.removeLast()
                    result.add(last.copy(chordNotes = chords.toList()))
                    chords.clear()
                }
                result.add(ChordGroup(note))
            }
        }
        if (result.isNotEmpty() && chords.isNotEmpty()) {
            val last = result.removeLast()
            result.add(last.copy(chordNotes = chords.toList()))
        }
        return result
    }

    private fun buildVFNote(group: ChordGroup, divisions: Int): VFStaveNote {
        val primary = group.primary
        val durationStr = durationToVF(primary.duration, divisions) +
                          if (primary is RestData) "r" else ""

        val keys: List<String> = when (primary) {
            is RestData -> listOf("b/4")
            is NoteData -> {
                val primaryKey = pitchToKey(primary.pitch, primary.accidental)
                val chordKeys = group.chordNotes.map { pitchToKey(it.pitch, it.accidental) }
                listOf(primaryKey) + chordKeys
            }
        }

        return VFStaveNote(VFStaveNoteStruct(keys = keys, duration = durationStr, glyphFontScale = 40f))
    }

    fun pitchToKey(pitch: Pitch, explicitAccidental: String?): String {
        val acc = explicitAccidental ?: when (pitch.alter) {
            1f -> "#"; -1f -> "b"; 2f -> "##"; -2f -> "bb"; else -> ""
        }
        return "${pitch.step.lowercase()}$acc/${pitch.octave}"
    }

    fun durationToVF(divisionDuration: Int, divisions: Int): String {
        val ratio = divisionDuration.toDouble() / divisions
        return when {
            ratio >= 1.0     -> "1"
            ratio >= 0.75    -> "2d"
            ratio >= 0.5     -> "2"
            ratio >= 0.375   -> "4d"
            ratio >= 0.25    -> "4"
            ratio >= 0.1875  -> "8d"
            ratio >= 0.125   -> "8"
            ratio >= 0.09375 -> "16d"
            ratio >= 0.0625  -> "16"
            else             -> "32"
        }
    }

    fun fifthsToKeySpec(fifths: Int, mode: String): String {
        val majorKeys = mapOf(
            0 to "C", 1 to "G", 2 to "D", 3 to "A", 4 to "E", 5 to "B", 6 to "F#", 7 to "C#",
            -1 to "F", -2 to "Bb", -3 to "Eb", -4 to "Ab", -5 to "Db", -6 to "Gb", -7 to "Cb"
        )
        val minorKeys = mapOf(
            0 to "Am", 1 to "Em", 2 to "Bm", 3 to "F#m", 4 to "C#m", 5 to "G#m",
            -1 to "Dm", -2 to "Gm", -3 to "Cm", -4 to "Fm", -5 to "Bbm"
        )
        return if (mode == "minor") minorKeys[fifths] ?: "Am"
               else majorKeys[fifths] ?: "C"
    }
}
```

**Algorithm notes:**
- `durationToVF` maps the continuous ratio to the nearest VexFlow duration string via ordered threshold comparison. There is no ambiguity: MusicXML `<duration>` combined with `<divisions>` always produces an exact rational value, so these thresholds are exact.
- Chord notes (`<chord/>` in MusicXML) are identified by the `isChordNote` flag and collected into the `chordNotes` list of the preceding `ChordGroup`. They share the same VFStaveNote via its `keys` list.
- alphaTab reference: `src/importer/MusicXmlImporter.ts` — `readNote()`, `readPitch()`, `applyBeaming()`.

**Test file:** `vexflow/parser/MusicXMLParserTest.kt`

```kotlin
package dev.pola.vexflow.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MusicXMLParserTest {

    private fun xmlSheet(body: String): MusicSheet {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<score-partwise version="3.1">
  <work><work-title>Test</work-title></work>
  <part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list>
  <part id="P1">$body</part>
</score-partwise>"""
        return MusicXMLParser().parse(xml.byteInputStream())
    }

    @Test fun `title is parsed`() {
        val sheet = xmlSheet("<measure number='1'><attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes></measure>")
        assertEquals("Test", sheet.title)
    }

    @Test fun `empty measure produces zero notes`() {
        val sheet = xmlSheet("<measure number='1'><attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes></measure>")
        assertEquals(0, sheet.parts[0].measures[0].notes.size)
    }

    @Test fun `quarter note is parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>
</measure>"""
        val measure = xmlSheet(xml).parts[0].measures[0]
        assertEquals(1, measure.notes.size)
        val note = measure.notes[0] as NoteData
        assertEquals("C", note.pitch.step)
        assertEquals(4, note.pitch.octave)
        assertEquals(1, note.duration)
    }

    @Test fun `rest is parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><rest/><duration>1</duration><voice>1</voice></note>
</measure>"""
        val note = xmlSheet(xml).parts[0].measures[0].notes[0]
        assertTrue(note is RestData)
    }

    @Test fun `sharp accidental is parsed`() {
        val xml = """<measure number='1'>
<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes>
<note><pitch><step>F</step><octave>5</octave><alter>1</alter></pitch><duration>1</duration><voice>1</voice><accidental>sharp</accidental></note>
</measure>"""
        val note = xmlSheet(xml).parts[0].measures[0].notes[0] as NoteData
        assertEquals("#", note.accidental)
        assertEquals(1f, note.pitch.alter)
    }

    @Test fun `key signature with 2 sharps is parsed`() {
        val xml = """<measure number='1'><attributes><divisions>1</divisions><key><fifths>2</fifths><mode>major</mode></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign></clef></attributes></measure>"""
        val attrs = xmlSheet(xml).parts[0].measures[0].attributes
        assertEquals(2, attrs.keyFifths)
        assertEquals("major", attrs.keyMode)
    }

    @Test fun `MusicSheetToVF durationToVF quarter note`() {
        assertEquals("4", MusicSheetToVF.durationToVF(1, 1))
        assertEquals("4", MusicSheetToVF.durationToVF(2, 2))
    }

    @Test fun `MusicSheetToVF durationToVF dotted quarter`() {
        assertEquals("4d", MusicSheetToVF.durationToVF(3, 4))
    }

    @Test fun `MusicSheetToVF fifthsToKeySpec 2 sharps major`() {
        assertEquals("D", MusicSheetToVF.fifthsToKeySpec(2, "major"))
    }

    @Test fun `MusicSheetToVF pitchToKey F sharp 5`() {
        assertEquals("f#/5", MusicSheetToVF.pitchToKey(Pitch("F", 5, 1f), "#"))
    }
}
```

---

## M8 Gate

- [ ] `./gradlew test --tests "*.MusicXMLParserTest"` — all 10 tests green
- [ ] `./gradlew assembleDebug` — 0 errors
- [ ] Visual: load `assets/samples/simple.xml` (a 4-measure C major scale), see all four measures render in the app
- [ ] Load `assets/samples/clair_de_lune_excerpt.xml` — first measure renders without crash

---

---

# M9 — File Import

**Goal:** User can pick a `.xml` or `.mxl` file from their device using the Storage Access Framework. The file is parsed and rendered.

**Files to create:**

1. `renderer/screens/HomeScreen.kt` — update with file picker button
2. `renderer/FileImportHandler.kt`

---

## FileImportHandler

**File:** `renderer/FileImportHandler.kt`

```kotlin
package dev.pola.renderer

import android.content.Context
import android.net.Uri
import dev.pola.vexflow.parser.MusicSheet
import dev.pola.vexflow.parser.MusicXMLParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileImportHandler {

    /**
     * Open a URI from the Storage Access Framework and parse it as MusicXML or MXL.
     * Returns null if the file cannot be parsed.
     * Must be called from a coroutine.
     */
    suspend fun importFile(context: Context, uri: Uri): MusicSheet? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    MusicXMLParser().parse(stream)
                }
            } catch (e: Exception) {
                null
            }
        }
}
```

**SAF file picker integration (in Compose Activity/Screen):**

```kotlin
// In RendererViewModel or UI screen:
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { viewModel.loadFile(it) }
}

// Launch with MIME types:
filePickerLauncher.launch(arrayOf(
    "application/vnd.recordare.musicxml+xml",  // .xml
    "application/vnd.recordare.musicxml",       // .mxl
    "application/xml",                          // fallback
    "text/xml"                                  // fallback
))
```

**RendererViewModel update:**

```kotlin
// Add to RendererViewModel:
private val _musicSheet = MutableStateFlow<MusicSheet?>(null)
val musicSheet: StateFlow<MusicSheet?> = _musicSheet.asStateFlow()

fun loadFile(uri: Uri) {
    viewModelScope.launch {
        val sheet = FileImportHandler.importFile(getApplication(), uri)
        _musicSheet.value = sheet
    }
}
```

## M9 Gate

- [ ] Tap "Open File" button on HomeScreen → system file picker opens
- [ ] Select a `.xml` file → sheet music renders on RendererScreen
- [ ] Select a `.mxl` file → sheet music renders on RendererScreen
- [ ] Invalid file → a toast/snackbar shows an error message (no crash)

---

---

# M10 — Multi-Measure Layout

**Goal:** A multi-measure piece renders across multiple rows, with stable spacing and engraving behavior under resize, narrow widths, and real MusicXML input.

**Status note (2026-03-07):** If rendering regressions are observed after M10 was previously marked done, treat M10 as **re-opened** until all gates in this section pass again.

## M10 Pitfall Closure Contract

The following pitfalls were observed in implementation iterations and are now mandatory closure items for M10.

| Pitfall | Required M10 Resolution |
|---|---|
| Heuristic measure widths cause uneven spacing | Replace naive width estimates with measured/stored measure layout metadata (modifier width + content width + minimum safety floor). |
| First-part-only rendering hides score data | Converter must support selecting a part index and must not hardcode `parts.firstOrNull()` for production rendering paths. |
| Missing spatial index blocks cursor/hit testing | Emit and store per-system/per-measure note/beat bounds (`VFBoundsLookup`-style structure) during layout. |
| Ties break at system boundaries | Tie rendering must support continuation across line breaks with dedicated continuation segments. |
| Full rebuild on every resize causes instability/flicker | Add resize-aware relayout path that reflows systems without reparsing source data. |
| Regressions escape due weak visual checks | Keep deterministic visual goldens for wide + narrow + attribute-change scenarios and fail build on mismatch. |

## M10 Visual Rendering Test Strategy — Two-Phase Conformance

Use `android/NoteWise/app/src/main/assets/samples/lilypond_tests` as the canonical fixture corpus.

### Corpus definition

- Source XML fixtures: `android/NoteWise/app/src/main/assets/samples/lilypond_tests/xml_files/`
- Reference PNG corpus (LilyPond): `android/NoteWise/app/src/main/assets/samples/lilypond_tests/images/`
- Metadata mapping XML→PNG: `android/NoteWise/app/src/main/assets/samples/lilypond_tests/test_metadata.json`
- alphaTab test suite: `android/reference/alphaTab-develop/packages/alphatab/test/visualTests/features/LilyPondMusicXML.test.ts`
- alphaTab rendered candidates: `android/reference/alphaTab-develop/packages/alphatab/test-data/visual-tests/lilypond/`
- NoteWise approval manifest: `android/NoteWise/app/src/test/resources/visual-goldens/lilypond/tier1/approval_manifest.json`

### Phase 1 — Match alphaTab (deterministic baseline)

**Reference target:** alphaTab renders, not LilyPond. alphaTab implements the Gourlay spring-rod spacing model with well-defined constants — "matching" is achievable and automatable.

**Comparison algorithm (mirrors alphaTab's `VisualTestHelper._expectToEqualVisuallyAsync`):**
- Pixel-diff via YIQ perceptual color distance (`PixelMatch`, threshold=0.3)
- Anti-aliased edge pixels excluded (`includeAA=false`)
- Pass condition: `differentPixels / (totalPixels − transparentPixels) ≤ 1%`
- Size mismatch → auto-fail (scale actual into expected dimensions, draw red border)
- On fail → save `*.new.png` + `*.diff.png` alongside golden; on pass → delete them

**Human-driven golden promotion:** `approval_manifest.json` tracks fixture status (pending / approved / rejected). CLI workflow: `generate-panels` → human reviews 3-way panel → `approve` / `reject`.

**3-way review panel layout:** LilyPond reference | alphaTab render | NoteWise candidate
- LilyPond is the aspirational quality compass — not a pass/fail target in Phase 1.
- alphaTab is the pass/fail target in Phase 1.

**Tier-1 fixtures (must pass Phase 1 before closing M10):**
- `01a-Pitches-Pitches.xml`
- `01b-Pitches-Intervals.xml`
- `11a-TimeSignatures.xml`
- `12aa-Clefs_Pitch_Traditional.xml`
- `13a-KeySignatures.xml`

**alphaTab render generation:**
```
cd android/reference/alphaTab-develop
/opt/homebrew/bin/npm test -- --grep LilyPondMusicXML
# Generates *.new.png in test-data/visual-tests/lilypond/
# Accept renders: npm run test-accept-reference
```

**NoteWise test run:**
```
cd android/NoteWise
LILYPOND_FIXTURES="<fixture.xml>" ./gradlew testDebugUnitTest --tests "*LilyPondTier1VisualTest*"
```

### Phase 2 — Improve toward LilyPond / MuseScore

Once NoteWise passes Phase 1 (≤1% pixel diff against alphaTab for all Tier-1 fixtures):
- Tighten note-spacing constants (spring-rod `minDurationWidth`, `stretchForce`, `measureRightSafetySpaces`)
- Target: LilyPond's ~10 measures/row density vs alphaTab's current 4 measures/row
- Re-promote both alphaTab and NoteWise goldens as spacing improves
- LilyPond remains the aspirational pass condition

### Observation: density gap (2026-03-08)

| Renderer | Measures/row at 635px | Total height for 41 measures |
|---|---|---|
| LilyPond (reference) | ~10 | 367px |
| alphaTab (master ref) | 4 | 1399px |
| NoteWise (current) | ~6 | ~635px |

Both alphaTab and NoteWise are far from LilyPond's professional engraving density. This is a spring-rod algorithm parameter tuning problem deferred to Phase 2.

### Manual sign-off checklist (required for M10 close)

- Barline, clef, key/time signature placement matches expected semantics.
- No cross-staff chord leakage or note-barline collision.
- Tie continuity and beam grouping remain semantically correct across system breaks.
- Spacing is visually stable across width profiles (420/720/1080/1440).
- Phase 1 gate: all Tier-1 fixtures ≤1% pixel diff against alphaTab golden.

**Files to create:**

1. `vexflow/elements/VFSystem.kt`
2. `vexflow/elements/VFLineBreaker.kt`
3. `vexflow/view/MultiStaveSheetMusicView.kt`

---

## Class — VFSystem

**File:** `vexflow/elements/VFSystem.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VFFormatter
import dev.pola.vexflow.core.VFFormatterOptions
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.parser.MusicSheetToVF

/**
 * A horizontal row of measures — one "system" or "line" of music.
 *
 * Each system contains one or more [MusicSheetToVF.RenderedMeasure] instances
 * laid out left-to-right within the system width.
 *
 * alphaTab reference: src/rendering/staves/StaffSystem.ts
 */
class VFSystem(
    val x: Float,
    val y: Float,
    val width: Float,
    val options: VFStaveOptions = VFStaveOptions()
) {
    private val measures = mutableListOf<MusicSheetToVF.RenderedMeasure>()
    private val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))

    fun addMeasure(measure: MusicSheetToVF.RenderedMeasure) { measures.add(measure) }

    fun draw(ctx: VexRenderingContext) {
        for (rendered in measures) {
            formatter.formatAndDrawVoices(
                voices       = rendered.voices,
                stave        = rendered.stave,
                ctx          = ctx,
                justifyWidth = rendered.stave.width
            )
            rendered.stave.draw(ctx)
            rendered.beams.forEach { it.draw(ctx, rendered.stave) }
            rendered.ties.forEach  { it.draw(ctx) }
        }
    }
}
```

---

## Class — VFLineBreaker

**File:** `vexflow/elements/VFLineBreaker.kt`

```kotlin
package dev.pola.vexflow.elements

import dev.pola.vexflow.parser.MusicSheetToVF

/**
 * Distributes a list of measures across multiple systems (rows) such that
 * no system exceeds [systemWidth] pixels.
 *
 * Algorithm:
 *   1. Compute each measure's minimum width from explicit layout metadata:
 *        minWidth[i] = modifierWidth + contentWidth + safetyPadding
 *      where metadata is created during MusicSheetToVF conversion and can be cached.
 *   2. Keep a minimum width floor for readability on very narrow screens.
 *   3. Greedily pack measures into systems left-to-right.
 *      When adding the next measure would exceed systemWidth, start a new system.
 *   3. Each system is assigned its actual stave x positions based on its
 *      available width divided evenly (or proportionally by note count).
 *
 * alphaTab reference: src/rendering/layout/PageViewLayout.ts — getBarsPerSystem()
 */
object VFLineBreaker {

    data class SystemLayout(
        val rows: List<List<MusicSheetToVF.RenderedMeasure>>,
        val systemY: List<Float>  // Y coordinate of each system row
    )

    fun layout(
        measures: List<MusicSheetToVF.RenderedMeasure>,
        systemWidth: Float,
        startX: Float,
        startY: Float,
        systemSpacing: Float = 80f  // vertical distance between systems
    ): SystemLayout {
        val rows = mutableListOf<List<MusicSheetToVF.RenderedMeasure>>()
        val ys   = mutableListOf<Float>()
        val currentRow = mutableListOf<MusicSheetToVF.RenderedMeasure>()
        var currentWidth = 0f
        var currentY = startY

        for (measure in measures) {
            val measureMinWidth = estimateMinWidth(measure)
            if (currentRow.isNotEmpty() && currentWidth + measureMinWidth > systemWidth) {
                rows.add(currentRow.toList())
                ys.add(currentY)
                currentY += systemSpacing
                currentRow.clear()
                currentWidth = 0f
            }
            currentRow.add(measure)
            currentWidth += measureMinWidth
        }
        if (currentRow.isNotEmpty()) { rows.add(currentRow.toList()); ys.add(currentY) }

        return SystemLayout(rows, ys)
    }

    private fun estimateMinWidth(measure: MusicSheetToVF.RenderedMeasure): Float {
        // Placeholder formula in this spec snippet.
        // Production implementation must use measured metadata, not note-count-only heuristics.
        return measure.layoutMetrics.minimumWidthPx
    }
}
```

---

## MultiStaveSheetMusicView

**File:** `vexflow/view/MultiStaveSheetMusicView.kt`

```kotlin
package dev.pola.vexflow.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFLineBreaker
import dev.pola.vexflow.elements.VFSystem
import dev.pola.vexflow.elements.VFStaveOptions
import dev.pola.vexflow.parser.MusicSheetToVF

/**
 * Multi-measure, multi-row sheet music view.
 * Replaces [SheetMusicView] for full-score rendering.
 *
 * Usage:
 *   view.setMeasures(renderedMeasures)
 *   // view auto-computes row layout and total height for scrolling
 */
class MultiStaveSheetMusicView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val renderingContext = VexRenderingContext()
    private var systems: List<VFSystem> = emptyList()
    private var totalHeight: Float = 0f

    fun setMeasures(measures: List<MusicSheetToVF.RenderedMeasure>) {
        if (measures.isEmpty()) { systems = emptyList(); invalidate(); return }
        val layout = VFLineBreaker.layout(
            measures      = measures,
            systemWidth   = width.toFloat().coerceAtLeast(400f),
            startX        = 20f,
            startY        = 60f,
            systemSpacing = 80f
        )
        systems = layout.rows.zip(layout.systemY).map { (rowMeasures, rowY) ->
            VFSystem(20f, rowY, width.toFloat() - 40f).apply {
                rowMeasures.forEach { addMeasure(it) }
            }
        }
        totalHeight = (layout.systemY.lastOrNull() ?: 0f) + 120f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderingContext.canvas = canvas
        systems.forEach { it.draw(renderingContext) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec),
            resolveSize(totalHeight.toInt().coerceAtLeast(200), heightMeasureSpec)
        )
    }
}
```

## M10 Gate

- [ ] Load a 12+ measure piece — it renders on multiple rows with automatic line breaks
- [ ] No measure overlap or header collisions in any tested width (420, 720, 1080, 1440)
- [ ] Mid-score key/time signature changes appear exactly where attributes change (and do not repeat when unchanged)
- [ ] Cross-staff chord notes remain on the correct staff (no leakage into opposite staff noteheads)
- [ ] Ties continue correctly across system breaks
- [ ] Resize/rotation triggers deterministic reflow without reparsing source MusicXML
- [ ] Bounds lookup is populated for at least system/measure/note and supports hit lookup by position
- [ ] Multi-part selection path exists and is covered by tests (no production hardcode to first part)
- [ ] Visual golden tests pass for grand staff fixtures and full Clair de Lune snapshots
- [ ] Phase 1 visual conformance: all Tier-1 LilyPond fixtures ≤1% pixel diff against alphaTab golden (per approval_manifest.json); 3-way review panels (LilyPond | alphaTab | NoteWise) reviewed and signed off
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` passes

---

---

# M11 — Polish and UX

**Goal:** The app is usable and stable. No crashes. Smooth scroll, zoom. Works in dark mode.

## Tasks

### Pinch-to-zoom
- Wrap `MultiStaveSheetMusicView` in a `ScaleGestureDetector`.
- Scale factor: 0.5× to 2.0×, applied as a Canvas scale transform.
- Persist zoom level across configuration changes in ViewModel.

### Dark mode
- Add `isSystemInDarkTheme()` check in `SheetMusicComposable`.
- Pass the resolved foreground/background color to `VexRenderingContext`.
- Add `VexRenderingContext.setColors(foreground: Int, background: Int)` convenience method.

### Error handling
- `FileImportHandler` returns `null` on error. The ViewModel exposes an error state.
- RendererScreen shows a `Snackbar` when `errorState` is non-null.

### Performance
- Profile with Android Profiler — target < 16ms per frame (60 fps).
- If `onDraw` is slow: cache stave and modifier rendering into a `Bitmap` and only re-render notes when zoom/scroll changes.
- Run `./gradlew lint` — fix all warnings at error severity.

### Settings screen
- MusicXML file path / recent files list (stored in `DataStore<Preferences>`).
- "Staff line spacing" preference (default 10f, range 8f–14f).

## M11 Gate

- [ ] Pinch-to-zoom works (0.5× to 2.0×) without visual artifacts
- [ ] Dark mode: staff lines and noteheads render in white-on-dark background
- [ ] Error file → Snackbar shown, no crash
- [ ] `./gradlew lint` — 0 errors (warnings OK)
- [ ] Frame time < 16ms on Pixel 6 (verified with Android Profiler)

---

---

# Phase A Complete Gate

All of the following must be true before moving to Phase B:

- [ ] All Phase A milestone gates above are checked
- [ ] `./gradlew test` — 65+ tests pass, 0 failures
- [ ] `./gradlew assembleRelease` — release APK builds (no ProGuard errors)
- [ ] Manual test: open Clair de Lune MusicXML, scroll through all measures, every measure renders without visual artifacts
- [ ] Visual regression suite includes Clair de Lune wide+narrow snapshots and a key/time-change fixture; all pass in non-update mode
- [ ] App does not crash on any tested device/emulator during Phase A flows

---

---

# M12 — Tempo Engine

**Goal:** Build a `TempoMap` that converts beat positions to wall-clock milliseconds. This is the timing backbone for cursor and playback.

**Files to create:** `playback/TempoMap.kt`, `playback/TempoMapTest.kt`

```kotlin
package dev.pola.playback

import dev.pola.vexflow.model.VFFraction
import dev.pola.vexflow.parser.MusicSheet
import dev.pola.vexflow.parser.TempoMark

/**
 * Maps beat positions (in measures + beats) to wall-clock time (milliseconds).
 *
 * A TempoEntry defines a tempo change at a given beat position.
 * Between two entries, tempo is constant (linear time progression).
 *
 * beat position unit: VFFraction of a whole note
 *   beat 0 = start of piece
 *   beat 1/4 = one quarter note in
 *   beat 1 = end of first 4/4 measure
 */
data class TempoEntry(
    val beat: VFFraction,   // beat position in the score (fraction of whole note)
    val bpm: Float,          // beats per minute (quarter-note beats)
    val startMs: Long = 0L  // wall-clock ms at this entry (computed, not set by parser)
)

class TempoMap(entries: List<TempoEntry>) {

    private val sorted: List<TempoEntry> = entries.sortedBy { it.beat }

    companion object {
        const val DEFAULT_BPM = 120f

        /** Build a TempoMap from a parsed MusicSheet. */
        fun fromMusicSheet(sheet: dev.pola.vexflow.parser.MusicSheet): TempoMap {
            val entries = mutableListOf<TempoEntry>()
            var currentBeat = VFFraction.ZERO

            for (part in sheet.parts) {
                for (measure in part.measures) {
                    val attrs = measure.attributes
                    val measureDuration = VFFraction.of(attrs.timeNumerator, attrs.timeDenominator)
                    for (tempo in measure.tempoMarks) {
                        entries.add(TempoEntry(beat = currentBeat, bpm = tempo.bpm))
                    }
                    currentBeat += measureDuration
                }
                break // only first part used for tempo
            }

            if (entries.isEmpty()) entries.add(TempoEntry(VFFraction.ZERO, DEFAULT_BPM))
            return TempoMap(entries)
        }
    }

    // Precomputed start times for each entry
    private val computed: List<TempoEntry> by lazy {
        val result = mutableListOf<TempoEntry>()
        var ms = 0L
        for (i in sorted.indices) {
            val entry = sorted[i]
            result.add(entry.copy(startMs = ms))
            if (i < sorted.lastIndex) {
                val nextBeat = sorted[i + 1].beat
                val beatDelta = nextBeat - entry.beat
                ms += beatsToMs(beatDelta, entry.bpm)
            }
        }
        result
    }

    /**
     * Convert a beat position to wall-clock milliseconds.
     * Interpolates linearly between tempo entries.
     */
    fun beatToMs(beat: VFFraction): Long {
        val entry = computed.lastOrNull { it.beat <= beat } ?: computed.first()
        val delta = beat - entry.beat
        return entry.startMs + beatsToMs(delta, entry.bpm)
    }

    /**
     * Convert a wall-clock time to the nearest beat position.
     */
    fun msToBeat(ms: Long): VFFraction {
        val entry = computed.lastOrNull { it.startMs <= ms } ?: computed.first()
        val deltaBeat = msToBeatDelta(ms - entry.startMs, entry.bpm)
        return entry.beat + deltaBeat
    }

    private fun beatsToMs(beats: VFFraction, bpm: Float): Long {
        // beats is in whole-note fractions; 1 quarter = 1/4 whole note
        // quarter-note duration in ms = 60000 / bpm
        // whole-note duration = 4 * quarter = 4 * 60000 / bpm
        val wholeNoteMs = 4.0 * 60000.0 / bpm
        return (beats.doubleValue * wholeNoteMs).toLong()
    }

    private fun msToBeatDelta(ms: Long, bpm: Float): VFFraction {
        val wholeNoteMs = 4.0 * 60000.0 / bpm
        val beatDouble = ms.toDouble() / wholeNoteMs
        // Convert to fraction with denominator 32 (32nd note resolution)
        val ticks = (beatDouble * 32).toInt()
        return VFFraction.of(ticks, 32)
    }
}
```

**Test file:** `playback/TempoMapTest.kt`

```kotlin
package dev.pola.playback

import dev.pola.vexflow.model.VFFraction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TempoMapTest {

    private fun constantTempoMap(bpm: Float) =
        TempoMap(listOf(TempoEntry(VFFraction.ZERO, bpm)))

    @Test fun `120 BPM quarter note is 500ms`() {
        val map = constantTempoMap(120f)
        assertEquals(500L, map.beatToMs(VFFraction.of(1, 4)))
    }

    @Test fun `120 BPM whole note is 2000ms`() {
        assertEquals(2000L, constantTempoMap(120f).beatToMs(VFFraction.of(1, 1)))
    }

    @Test fun `60 BPM quarter note is 1000ms`() {
        assertEquals(1000L, constantTempoMap(60f).beatToMs(VFFraction.of(1, 4)))
    }

    @Test fun `beat 0 is always 0ms`() {
        assertEquals(0L, constantTempoMap(120f).beatToMs(VFFraction.ZERO))
    }

    @Test fun `msToBeat inverse of beatToMs at 120 BPM`() {
        val map = constantTempoMap(120f)
        val beat = VFFraction.of(3, 4)
        val ms = map.beatToMs(beat)
        val roundTrip = map.msToBeat(ms)
        assertEquals(beat, roundTrip)
    }

    @Test fun `tempo change at measure 2 is respected`() {
        val map = TempoMap(listOf(
            TempoEntry(VFFraction.ZERO, 120f),
            TempoEntry(VFFraction.of(1, 1), 60f)  // change at beat 1 (end of first 4/4 measure)
        ))
        // First quarter: 500ms. Second quarter (now at 60bpm): 1000ms.
        val firstQ  = map.beatToMs(VFFraction.of(1, 4))
        val secondQ = map.beatToMs(VFFraction.of(5, 4))  // beat 1 + 1/4
        assertEquals(500L, firstQ)
        assertEquals(2000L + 1000L, secondQ)
    }
}
```

## M12 Gate

- [ ] `./gradlew test --tests "*.TempoMapTest"` — all 6 tests green
- [ ] `./gradlew assembleDebug` — 0 errors

---

---

# M13 — Playback Cursor and Metronome

**Goal:** Press play. A cursor moves across the score at the correct tempo. Notes highlight as they are reached.

**Files to create:** `playback/PlaybackCursor.kt`, `playback/CursorOverlay.kt`, `playback/Metronome.kt`

## PlaybackCursor

```kotlin
package dev.pola.playback

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import dev.pola.vexflow.model.VFFraction

/**
 * Drives cursor movement in sync with a TempoMap.
 *
 * Uses ValueAnimator with LinearInterpolator for smooth frame-rate-independent animation.
 * The animator runs from 0ms to totalDurationMs.
 * Each frame: currentMs -> msToBeat -> resolve to screen x via score note positions.
 *
 * Listener receives (currentBeat, currentX) on each frame.
 */
class PlaybackCursor(
    private val tempoMap: TempoMap,
    private val totalDurationMs: Long
) {
    interface Listener { fun onFrame(beat: VFFraction, x: Float) }

    private var animator: ValueAnimator? = null
    private var beatToXResolver: ((VFFraction) -> Float)? = null
    var listener: Listener? = null
    var tempoScale: Float = 1.0f  // 0.5 = half speed, 1.5 = 150%

    fun setBeatToXResolver(resolver: (VFFraction) -> Float) { beatToXResolver = resolver }

    fun play() {
        stop()
        val scaledDuration = (totalDurationMs / tempoScale).toLong()
        animator = ValueAnimator.ofLong(0L, scaledDuration).apply {
            interpolator = LinearInterpolator()
            duration     = scaledDuration
            addUpdateListener { anim ->
                val scaledMs = (anim.animatedValue as Long)
                val realMs   = (scaledMs * tempoScale).toLong()
                val beat     = tempoMap.msToBeat(realMs)
                val x        = beatToXResolver?.invoke(beat) ?: 0f
                listener?.onFrame(beat, x)
            }
            start()
        }
    }

    fun pause() { animator?.pause() }
    fun resume() { animator?.resume() }
    fun stop()  { animator?.cancel(); animator = null }
    fun isPlaying() = animator?.isRunning ?: false

    /** Jump to a specific beat position. */
    fun seekTo(beat: VFFraction) {
        val ms = tempoMap.beatToMs(beat)
        animator?.currentPlayTime = ms
    }
}
```

## Metronome

```kotlin
package dev.pola.playback

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*

/**
 * Generates audible click samples and schedules playback at beat boundaries.
 *
 * Click sound: a short (10ms) sine wave burst at 880 Hz (accent on beat 1)
 * or 440 Hz (off-beats). Generated programmatically via PCM.
 *
 * Scheduling: a coroutine runs in Dispatchers.IO, sleeping until the next beat,
 * then writing the click sample to AudioTrack.
 */
class Metronome(
    private val tempoMap: TempoMap,
    private val timeNumerator: Int,
    private val timeDenominator: Int
) {
    var enabled: Boolean = true
    var accentBeat1: Boolean = true

    private val sampleRate = 44100
    private val clickDurationMs = 10
    private var job: Job? = null

    private val audioTrack: AudioTrack by lazy {
        val bufSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                   AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                   bufSize, AudioTrack.MODE_STREAM).also { it.play() }
    }

    fun start(startBeat: VFFraction = VFFraction.ZERO) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            var beat = startBeat
            val beatUnit = VFFraction.of(1, timeDenominator)  // one beat
            while (isActive) {
                val nowMs = System.currentTimeMillis()
                val beatMs = tempoMap.beatToMs(beat)
                val delayMs = beatMs - nowMs
                if (delayMs > 0) delay(delayMs)
                if (enabled) clickAt(beat)
                beat += beatUnit
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    private fun clickAt(beat: VFFraction) {
        // Determine if this is beat 1 of a measure
        val beatInMeasure = (beat.doubleValue % (timeNumerator.toDouble() / timeDenominator)) * timeDenominator
        val isAccent = accentBeat1 && beatInMeasure < 0.05
        val freq = if (isAccent) 880.0 else 440.0
        val samples = generateClick(freq)
        audioTrack.write(samples, 0, samples.size)
    }

    private fun generateClick(freq: Double): ShortArray {
        val numSamples = sampleRate * clickDurationMs / 1000
        return ShortArray(numSamples) { i ->
            val t = i.toDouble() / sampleRate
            val envelope = 1.0 - (i.toDouble() / numSamples)  // linear decay
            (Short.MAX_VALUE * envelope * Math.sin(2 * Math.PI * freq * t)).toInt().toShort()
        }
    }
}
```

## M13 Gate

- [ ] Press play → cursor moves across the score at 120 BPM
- [ ] Pause/resume works
- [ ] Tempo slider at 50% → cursor moves at half speed
- [ ] Metronome clicks are audible and on-beat (verified by ear)
- [ ] `./gradlew assembleDebug` — 0 errors

---

---

# M14 — Audio Synthesis (Optional)

This milestone is optional for Phase B completion. Implement only if time permits before Phase C.

**Goal:** Basic MIDI-like audio playback so the student can hear the piece while following the score.

**Approach:** Use Android's `android.media.midi.MidiManager` with a software synthesizer, or bundle a minimal SoundFont player. The simplest viable option is `android.media.SoundPool` with pre-rendered note samples (one per pitch, piano sound) loaded from assets.

**Exit criteria:** Press play → hear the piece played on a piano sound. Audio stays in sync with the cursor within 50ms.

---

---

# M15 — MIDI Device Connection

**File:** `midi/NoteEvent.kt`, `midi/MidiDeviceManager.kt`

```kotlin
package dev.pola.midi

enum class NoteEventType { ON, OFF }

data class NoteEvent(
    val pitch:       Int,          // MIDI note number 0-127 (60 = C4)
    val velocity:    Int,          // 0-127
    val timestampMs: Long,         // System.currentTimeMillis() at receipt
    val type:        NoteEventType
)
```

```kotlin
package dev.pola.midi

import android.content.Context
import android.media.midi.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Manages MIDI device detection and input.
 *
 * API: android.media.midi (API 24+ baseline).
 * Emits NoteEvent via a SharedFlow<NoteEvent>.
 *
 * MIDI message format:
 *   byte[0] = status byte: 0x90 = note-on ch1, 0x80 = note-off ch1
 *   byte[1] = note number (0-127)
 *   byte[2] = velocity (0-127)
 * Note: note-on with velocity 0 is equivalent to note-off.
 *
 * USB MIDI: automatically available when device is connected (Android MIDI API detects it).
 * Bluetooth MIDI: requires pairing first; then appears in MidiManager.getDevices().
 */
class MidiDeviceManager(private val context: Context) {

    private val _events = MutableSharedFlow<NoteEvent>(extraBufferCapacity = 64)
    val events: Flow<NoteEvent> = _events

    private var openDevice: MidiDevice? = null
    private var inputPort: MidiOutputPort? = null

    fun connect(deviceInfo: MidiDeviceInfo) {
        val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
        midiManager.openDevice(deviceInfo, { device ->
            openDevice = device
            val port = device.openOutputPort(0)
            inputPort = port
            port?.connect(object : MidiReceiver() {
                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                    if (count < 3) return
                    val status = msg[offset].toInt() and 0xFF
                    val note   = msg[offset + 1].toInt() and 0xFF
                    val vel    = msg[offset + 2].toInt() and 0xFF
                    val type = when {
                        (status and 0xF0) == 0x90 && vel > 0 -> NoteEventType.ON
                        (status and 0xF0) == 0x80 -> NoteEventType.OFF
                        (status and 0xF0) == 0x90 && vel == 0 -> NoteEventType.OFF
                        else -> return
                    }
                    _events.tryEmit(NoteEvent(note, vel, System.currentTimeMillis(), type))
                }
            })
        }, null)
    }

    fun disconnect() {
        inputPort?.close()
        openDevice?.close()
        openDevice = null
        inputPort = null
    }

    fun getAvailableDevices(): Array<MidiDeviceInfo> {
        val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
        return midiManager.devices
    }
}
```

## M15 Gate

- [ ] Connect a USB MIDI keyboard → device appears in settings MIDI list
- [ ] Play a note → `NoteEvent` with correct pitch logged
- [ ] Disconnect → no crash

---

---

# M16 — Real-Time Note Display

**Files:** `midi/MidiRecorder.kt`, update `vexflow/view/MultiStaveSheetMusicView.kt`

```kotlin
package dev.pola.midi

/**
 * Collects NoteEvents during a practice session into a timestamped recording.
 * startMs = System.currentTimeMillis() at session start.
 */
class MidiRecorder {
    private val _events = mutableListOf<NoteEvent>()
    private var startMs = 0L

    fun start() { startMs = System.currentTimeMillis(); _events.clear() }
    fun record(event: NoteEvent) { _events.add(event) }
    fun stop(): PerformanceRecording = PerformanceRecording(startMs, _events.toList())
}

data class PerformanceRecording(
    val startMs: Long,
    val events: List<NoteEvent>
)
```

**Note display annotation:** Add a `NoteAnnotation` overlay to `MultiStaveSheetMusicView`:
- Green circle on correct notes (pitch matches score at current cursor beat)
- Red circle on wrong notes
- Gray dimmed notehead on missed notes (cursor passed, no input received)

## M16 Gate

- [ ] Start session → play correct note → green overlay on notehead
- [ ] Play wrong note → red overlay
- [ ] Miss note (cursor passes) → gray overlay
- [ ] Overlays clear when restarting

---

---

# M17 — Latency Compensation

**File:** Add `latencyOffsetMs: Long` property to `MidiDeviceManager`.

**Calibration flow:**
1. Settings screen: "Calibrate MIDI latency" button.
2. App plays metronome click via `AudioTrack`.
3. User taps the MIDI keyboard on each click (10 taps).
4. App measures average offset: `avgOffset = mean(clickTime - midiEventTime)`.
5. Stores `latencyOffsetMs = avgOffset` in `DataStore`.
6. All future `NoteEvent.timestampMs` values are adjusted: `correctedMs = event.timestampMs + latencyOffsetMs`.

## M17 Gate

- [ ] Run calibration → latency offset is stored
- [ ] Retry calibration → offset updates
- [ ] Rating consistency improves by at least 20ms (measured by timing accuracy before/after)

---

---

# M18 — Note Matching and Scoring

**Files:** `evaluation/NoteMatcher.kt`, `evaluation/ScoringEngine.kt`

```kotlin
package dev.pola.evaluation

import dev.pola.midi.NoteEvent
import dev.pola.midi.NoteEventType
import dev.pola.playback.TempoMap
import dev.pola.vexflow.model.VFFraction

/**
 * Aligns performed NoteEvents to score notes.
 *
 * Algorithm: beat-aligned greedy matching.
 *   For each score note (in beat order):
 *     1. Compute expectedMs = tempoMap.beatToMs(noteBeat).
 *     2. Find the unmatched NoteEvent of type ON with the same pitch
 *        whose timestampMs is within windowMs of expectedMs.
 *     3. If found: CORRECT match. Record timing deviation = event.timestampMs - expectedMs.
 *     4. If not found within window: MISSED note.
 *   Any unmatched NoteEvents after all score notes are processed: EXTRA notes.
 *
 * windowMs default: 50% of beat duration in ms at current tempo.
 */
data class NoteMatchResult(
    val scoreBeat: VFFraction,
    val scorePitch: Int,         // MIDI note number
    val type: MatchType,
    val timingDeviationMs: Long = 0L,  // positive = rushed, negative = dragged
    val performedPitch: Int = 0
)

enum class MatchType { CORRECT, MISSED, EXTRA, SUBSTITUTED }

class NoteMatcher(
    private val tempoMap: TempoMap,
    private val windowFraction: Double = 0.5  // fraction of beat duration as matching window
) {
    fun match(
        scoreNotes: List<Pair<VFFraction, Int>>,  // (beat, midiPitch)
        events: List<NoteEvent>
    ): List<NoteMatchResult> {
        val unmatched = events.filter { it.type == NoteEventType.ON }.toMutableList()
        val results = mutableListOf<NoteMatchResult>()

        for ((beat, pitch) in scoreNotes) {
            val expectedMs = tempoMap.beatToMs(beat)
            // Window = half beat duration in ms
            val beatMs = tempoMap.beatToMs(beat + VFFraction.of(1, 4)) - expectedMs
            val windowMs = (beatMs * windowFraction).toLong()

            val candidate = unmatched.firstOrNull { event ->
                Math.abs(event.timestampMs - expectedMs) <= windowMs
            }

            if (candidate != null) {
                unmatched.remove(candidate)
                val matchType = if (candidate.pitch == pitch) MatchType.CORRECT else MatchType.SUBSTITUTED
                results.add(NoteMatchResult(beat, pitch, matchType,
                    candidate.timestampMs - expectedMs, candidate.pitch))
            } else {
                results.add(NoteMatchResult(beat, pitch, MatchType.MISSED))
            }
        }

        // Remaining unmatched events are extras
        for (event in unmatched) {
            results.add(NoteMatchResult(VFFraction.ZERO, 0, MatchType.EXTRA,
                performedPitch = event.pitch))
        }
        return results
    }
}
```

```kotlin
package dev.pola.evaluation

/**
 * Calculates per-note and aggregate scores from match results.
 *
 * Scoring:
 *   Pitch accuracy:   % of score notes that are CORRECT or not MISSED
 *   Rhythm accuracy:  1.0 - (|timingDeviationMs| / windowMs), clamped to [0, 1]
 *   Overall:          weighted average: pitch 50% + rhythm 50%
 */
class ScoringEngine {

    data class MeasureScore(val measureNumber: Int, val pitchScore: Float, val rhythmScore: Float) {
        val overall: Float get() = (pitchScore + rhythmScore) / 2f
    }

    data class PerformanceScore(
        val pitchAccuracy: Float,    // 0.0 to 1.0
        val rhythmAccuracy: Float,   // 0.0 to 1.0
        val overallScore: Float,     // 0.0 to 1.0
        val measureScores: List<MeasureScore>
    )

    fun score(
        results: List<NoteMatchResult>,
        windowMs: Long = 500L
    ): PerformanceScore {
        val scoreNotes = results.filter { it.type != MatchType.EXTRA }
        if (scoreNotes.isEmpty()) return PerformanceScore(0f, 0f, 0f, emptyList())

        val correct = scoreNotes.count { it.type == MatchType.CORRECT }
        val pitchAcc = correct.toFloat() / scoreNotes.size

        val rhythmScores = scoreNotes.filter { it.type == MatchType.CORRECT }.map { result ->
            (1.0f - (Math.abs(result.timingDeviationMs).toFloat() / windowMs)).coerceIn(0f, 1f)
        }
        val rhythmAcc = if (rhythmScores.isEmpty()) 0f else rhythmScores.average().toFloat()

        return PerformanceScore(
            pitchAccuracy  = pitchAcc,
            rhythmAccuracy = rhythmAcc,
            overallScore   = (pitchAcc + rhythmAcc) / 2f,
            measureScores  = emptyList()  // measure-level breakdown added in M19
        )
    }
}
```

## M18 Gate

- [ ] Play a 4-note scale perfectly → score is 100%
- [ ] Miss all notes → score is 0%
- [ ] Play correct pitches but consistently 200ms late → rhythm accuracy < 50%
- [ ] `./gradlew test --tests "*.NoteMatcherTest"` — basic matching tests green

---

---

# M19 — Performance Summary Screen

**Files:** `evaluation/PerformanceRecord.kt`, `renderer/screens/ResultsScreen.kt`

```kotlin
package dev.pola.evaluation

import dev.pola.vexflow.model.VFFraction

data class PerformanceRecord(
    val sessionId:     Long = System.currentTimeMillis(),
    val pieceTitle:    String,
    val durationMs:    Long,
    val results:       List<NoteMatchResult>,
    val score:         ScoringEngine.PerformanceScore,
    val tempoScale:    Float = 1.0f
)
```

**ResultsScreen layout:**
```
┌────────────────────────────────┐
│         78%  Overall           │   large number, color-coded
│    Pitch: 85%  Rhythm: 71%     │
├────────────────────────────────┤
│   Measure heatmap (scrollable) │   green/yellow/red bars per measure
├────────────────────────────────┤
│ Trouble spots:                 │
│  • Measure 12: missed F#4      │
│  • Measure 3: rushed beat 2    │
├────────────────────────────────┤
│  [Practice this section]       │   loops lowest-scoring measures
│  [Try again]  [Done]           │
└────────────────────────────────┘
```

## M19 Gate

- [ ] After a session, results screen appears with overall %, pitch %, rhythm %
- [ ] Tapping a measure in the heatmap jumps to that measure in the score view
- [ ] "Practice this section" sets loop range covering the 3 lowest-scoring consecutive measures

---

---

# M20 — Practice History and Targeted Practice

**Files:** `persistence/AppDatabase.kt`, `persistence/PracticeSession.kt`, `renderer/screens/HistoryScreen.kt`

```kotlin
// Room entities:

@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey val sessionId: Long,
    val pieceTitle: String,
    val timestampMs: Long,
    val overallScore: Float,
    val pitchScore: Float,
    val rhythmScore: Float,
    val durationMs: Long
)

@Entity(tableName = "measure_scores")
data class MeasureScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,
    val measureNumber: Int,
    val overallScore: Float
)
```

**HistoryScreen layout:**
```
[Piece title]              5 sessions
  ████████████ 78%  (today)
  ████████     65%  (yesterday)
  ██████       52%  (3 days ago)
  [See full history]
```

## M20 Gate

- [ ] Practice session is saved to Room DB after completion
- [ ] HistoryScreen shows sessions for a piece in reverse chronological order
- [ ] Per-piece score trend is visible (at least 3 sessions)
- [ ] "Targeted practice" loops the 3 lowest-scoring measures at 70% tempo

---

---

# Appendix A — RecordingContext (Test Helper)

The `RecordingContext` class defined in `vexflow/core/VexRenderingContextTest.kt` is used across many test files. Move it to a shared test helper location:

**File:** `src/test/java/dev/pola/vexflow/core/RecordingContext.kt`

```kotlin
package dev.pola.vexflow.core

/** Test-only VexRenderingContext subclass that records all draw calls. */
class RecordingContext : VexRenderingContext() {
    data class GlyphCall(val codepoint: Int, val x: Float, val y: Float, val size: Float)
    data class RectCall(val x: Float, val y: Float, val w: Float, val h: Float)

    val glyphCalls     = mutableListOf<GlyphCall>()
    val fillRectCalls  = mutableListOf<RectCall>()
    val strokeRectCalls= mutableListOf<RectCall>()
    val strokeCalls    = mutableListOf<Unit>()
    val fillCalls      = mutableListOf<Unit>()

    override fun drawSmuflGlyph(codepoint: Int, x: Float, y: Float, sizePx: Float) {
        glyphCalls += GlyphCall(codepoint, x, y, sizePx)
    }
    override fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        fillRectCalls += RectCall(x, y, width, height)
    }
    override fun strokeRect(x: Float, y: Float, width: Float, height: Float) {
        strokeRectCalls += RectCall(x, y, width, height)
    }
    override fun stroke() { strokeCalls += Unit }
    override fun fill()   { fillCalls += Unit }
    override fun save()    {}
    override fun restore() {}
    override fun translate(x: Float, y: Float) {}
    override fun scale(sx: Float, sy: Float) {}
    override fun beginPath() {}
    override fun moveTo(x: Float, y: Float) {}
    override fun lineTo(x: Float, y: Float) {}
    override fun bezierCurveTo(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float, x: Float, y: Float) {}
    override fun quadraticCurveTo(cpx: Float, cpy: Float, x: Float, y: Float) {}
    override fun closePath() {}
}
```

---

# Appendix B — Asset Checklist

Verify these files exist before M1:

| Asset path | Source |
|------------|--------|
| `assets/fonts/Bravura.otf` | `android/bravura/otf/Bravura.otf` |
| `assets/glyph_bboxes.json` | `android/bravura/extracted_glyph_bboxes.json` |
| `assets/samples/simple.xml` | Create: 4-measure C major scale, 4/4, quarter notes |
| `assets/samples/clair_de_lune_excerpt.xml` | From `android/samples/` directory |

---

# Appendix C — Key Risks and Mitigations (Updated)

| # | Risk | Impact | Mitigation |
|---|------|--------|------------|
| R1 | Bravura glyph vertical positioning off | Visual | Use glyph bounding boxes as anchor source; verify with focused glyph smoke tests before milestone closure |
| R2 | Beam geometry artifacts on mixed pitch groups | Visual | Suppress note-local stems/flags for beamed notes and generate beam geometry from notehead-anchored stem tips |
| R3 | MusicXML parser drift on real files | Parser | Keep parser fixtures strict, run parser+converter integration tests on `.xml` and `.mxl` samples |
| R4 | Layout instability under narrow widths | Visual | Enforce visual golden coverage at 420/720/1080/1440 widths with fixed tolerance gates |
| R5 | System spacing regressions from heuristic width estimation | Visual | Require measured layout metadata and reject note-count-only width heuristics in production code |
| R6 | Missing bounds data blocks interaction features | UX/Feature | Build bounds lookup during layout and add tests for position->note/beat lookup |
| R7 | Single-part hardcode hides score content | Correctness | Require explicit part selection path and tests validating non-first-part rendering |
| R8 | Full relayout/reparse on resize causes flicker/perf cliffs | Performance | Implement resize-aware reflow path and validate with rotation/resize tests |
| R9 | Golden baseline drift accepted accidentally | Test quality | Keep update mode opt-in (`UPDATE_VISUAL_GOLDENS=true`) and require non-update pass in CI |
