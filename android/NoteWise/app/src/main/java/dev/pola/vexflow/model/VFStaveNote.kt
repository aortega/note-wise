package dev.pola.vexflow.model

import dev.pola.vexflow.core.VFTickContext
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFAccidental
import dev.pola.vexflow.elements.VFStave

data class VFStaveNoteStruct(
    val keys: List<String>,
    val duration: String,
    val glyphFontScale: Float = 40f,
    val stemDirection: Int = STEM_AUTO
) {
    companion object {
        const val STEM_AUTO = 0
        const val STEM_UP = 1
        const val STEM_DOWN = -1
    }
}

data class VFTickableMetrics(
    val width: Float = 0f,
    val totalLeftPx: Float = 0f,
    val totalRightPx: Float = 0f
)

private data class NoteGlyphPlacement(
    val originX: Float,
    val originY: Float,
    val bbox: VFGlyphBoundingBox
)

/**
 * A single chord (one or more noteheads on the same stem) or a rest.
 */
class VFStaveNote(private val struct: VFStaveNoteStruct) {

    val keys: List<String> = struct.keys
    val durationString: String = struct.duration
    val duration: VFFraction = VFFraction.fromDurationString(struct.duration)
        ?: error("Unknown duration string: '${struct.duration}'")
    val glyphFontScale: Float = struct.glyphFontScale
    val isRest: Boolean = struct.duration.contains('r')

    var x: Float = 0f
    var noteLineIndex: Int = 0

    private var stave: VFStave? = null
    private var tickContext: VFTickContext? = null
    private val accidentalObjects: MutableList<VFAccidental> = mutableListOf()

    fun setStave(stave: VFStave) {
        this.stave = stave
        rebuildAccidentals()
    }

    fun setTickContext(tc: VFTickContext) {
        tickContext = tc
    }

    fun getStave(): VFStave? = stave

    fun getMetrics(): VFTickableMetrics {
        val headWidth = glyphFontScale * 0.65f
        val accidentalSpan = accidentalSpanPx()
        return VFTickableMetrics(
            width = headWidth,
            totalLeftPx = (headWidth / 2f) + accidentalSpan,
            totalRightPx = headWidth / 2f
        )
    }

    fun getTieRightX(): Float = x + getMetrics().totalRightPx

    fun getTieLeftX(): Float = x - getMetrics().totalLeftPx

    fun getYs(): List<Float> {
        val sv = stave ?: return emptyList()
        return keys.map { key -> sv.getYForNote(pitchToNoteLineIndex(key, sv)) }
    }

    fun getStemDirection(): Int {
        if (struct.stemDirection != VFStaveNoteStruct.STEM_AUTO) return struct.stemDirection
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
            val baseY = noteYs.maxOrNull() ?: 0f
            // For low notes with upward stems, ensure the stem reaches at least
            // the third staff line (middle line in a 5-line staff).
            val minTopY = sv.getYForLine(2f)
            val targetTopY = minOf(baseY - stemHeightPx, minTopY)
            StemExtents(baseY, targetTopY)
        } else {
            val baseY = noteYs.minOrNull() ?: 0f
            StemExtents(baseY, baseY + stemHeightPx)
        }
    }

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

    private fun noteheadGlyph(): Int = when {
        duration >= VFFraction.of(1, 1) -> VFTables.GLYPH_NOTE_HEAD_WHOLE
        duration >= VFFraction.of(1, 2) -> VFTables.GLYPH_NOTE_HEAD_HALF
        else -> VFTables.GLYPH_NOTE_HEAD_QUARTER
    }

    private fun restGlyph(): Int = when {
        duration >= VFFraction.of(1, 1) -> VFTables.GLYPH_REST_WHOLE
        duration >= VFFraction.of(1, 2) -> VFTables.GLYPH_REST_HALF
        duration >= VFFraction.of(1, 4) -> VFTables.GLYPH_REST_QUARTER
        duration >= VFFraction.of(1, 8) -> VFTables.GLYPH_REST_8TH
        duration >= VFFraction.of(1, 16) -> VFTables.GLYPH_REST_16TH
        else -> VFTables.GLYPH_REST_32ND
    }

    private fun drawNoteheads(ctx: VexRenderingContext, sv: VFStave) {
        val glyph = noteheadGlyph()
        val headWidth = getMetrics().width
        for (key in keys) {
            val noteLine = pitchToNoteLineIndex(key, sv)
            val placement = noteheadPlacement(key, sv)
            val noteCenterY = sv.getYForNote(noteLine)
            if (placement != null) {
                // Use SMuFL bbox center to keep notehead visually centered on x.
                ctx.drawSmuflGlyph(glyph, placement.originX, placement.originY, glyphFontScale)
            } else {
                ctx.drawSmuflGlyph(glyph, x - headWidth / 2f, noteCenterY, glyphFontScale)
            }
            if (shouldDrawDebugGlyphLabels()) {
                drawDebugLabel(
                    ctx = ctx,
                    label = debugNoteLabel(key),
                    centerX = x,
                    centerY = noteCenterY,
                    glyphSizePx = glyphFontScale
                )
            }
        }
    }

    private fun drawRest(ctx: VexRenderingContext, sv: VFStave) {
        val restY = when {
            duration >= VFFraction.of(1, 1) -> sv.getYForLine(1f)
            duration >= VFFraction.of(1, 2) -> sv.getYForLine(1f)
            else -> sv.getYForLine(2f)
        }
        ctx.drawSmuflGlyph(restGlyph(), x, restY, glyphFontScale)
    }

    private fun drawStem(ctx: VexRenderingContext, sv: VFStave) {
        if (_isBeamed) return
        if (duration >= VFFraction.of(1, 1)) return
        val extents = getStemExtents()
        val stemX = getStemX(getStemDirection())

        ctx.lineWidth = maxOf(1f, sv.spacingBetweenLines * VFMetrics.STEM_THICKNESS_TO_SPACING)
        ctx.beginPath()
        ctx.moveTo(stemX, extents.baseY)
        ctx.lineTo(stemX, extents.topY)
        ctx.stroke()
    }

    private fun drawFlags(ctx: VexRenderingContext, sv: VFStave) {
        if (_isBeamed) return
        if (duration >= VFFraction.of(1, 4)) return

        val firstKey = keys.firstOrNull() ?: return
        val stemX = getStemX(getStemDirection())
        val extents = getStemExtents()
        val stemUp = getStemDirection() == VFStaveNoteStruct.STEM_UP
        val tipOffsetPx = sv.spacingBetweenLines * VFMetrics.FLAG_ATTACH_OFFSET_SPACES
        val flagY = if (stemUp) extents.topY + tipOffsetPx else extents.topY - tipOffsetPx
        val flagGlyph = when {
            duration >= VFFraction.of(1, 8) ->
                if (stemUp) VFTables.GLYPH_FLAG_8TH_UP else VFTables.GLYPH_FLAG_8TH_DOWN
            duration >= VFFraction.of(1, 16) ->
                if (stemUp) VFTables.GLYPH_FLAG_16TH_UP else VFTables.GLYPH_FLAG_16TH_DOWN
            else ->
                if (stemUp) VFTables.GLYPH_FLAG_32ND_UP else VFTables.GLYPH_FLAG_32ND_DOWN
        }
        ctx.drawSmuflGlyph(flagGlyph, stemX, flagY, glyphFontScale)
    }

    private fun stemXForPlacement(placement: NoteGlyphPlacement?, direction: Int): Float {
        val noteheadWidth = placement?.bbox?.width ?: getMetrics().width
        val stemInset = noteheadWidth * VFMetrics.STEM_ATTACH_INSET_TO_HEAD_WIDTH
        return if (direction == VFStaveNoteStruct.STEM_UP) {
            placement?.let { (it.originX + it.bbox.northeast.x) - stemInset } ?: (x + glyphFontScale * 0.3f)
        } else {
            placement?.let { (it.originX + it.bbox.southwest.x) + stemInset } ?: (x - glyphFontScale * 0.3f)
        }
    }

    fun getStemX(direction: Int = getStemDirection()): Float {
        val sv = stave ?: return x
        val firstKey = keys.firstOrNull() ?: return x
        val placement = noteheadPlacement(firstKey, sv)
        return stemXForPlacement(placement, direction)
    }

    private fun drawLedgerLines(ctx: VexRenderingContext, sv: VFStave) {
        for (key in keys) {
            val noteLine = pitchToNoteLineIndex(key, sv)
            val ledgerWidth = ledgerWidthForKey(key, sv)

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

    private fun ledgerWidthForKey(key: String, sv: VFStave): Float {
        val noteheadWidth = noteheadPlacement(key, sv)?.bbox?.width ?: getMetrics().width
        return noteheadWidth * VFMetrics.LEDGER_LINE_WIDTH_FACTOR
    }

    private fun drawAccidentals(ctx: VexRenderingContext, sv: VFStave) {
        val centers = accidentalColumnCenters(sv)
        for ((index, acc) in accidentalObjects.withIndex()) {
            acc.x = centers[index]
            acc.staveY = sv.y
            acc.staffLineSpacing = sv.spacingBetweenLines
            acc.draw(ctx)
            if (shouldDrawDebugGlyphLabels()) {
                val accidentalCenterY = sv.getYForNote(acc.noteLineIndex)
                drawDebugLabel(
                    ctx = ctx,
                    label = debugAccidentalLabel(acc),
                    centerX = acc.x,
                    centerY = accidentalCenterY,
                    glyphSizePx = glyphFontScale
                )
            }
        }
    }

    private fun accidentalSpanPx(): Float {
        if (accidentalObjects.isEmpty()) return 0f
        val spacing = stave?.spacingBetweenLines ?: (glyphFontScale / 4f)
        val noteGap = spacing * 0.5f
        val columnGap = spacing * 0.5f
        val widths = accidentalWidthsPx(spacing)
        // Extra-left span beyond the notehead's own left half-width.
        return noteGap + widths.sum() + columnGap * (widths.size - 1).coerceAtLeast(0)
    }

    private fun accidentalColumnCenters(sv: VFStave): List<Float> {
        if (accidentalObjects.isEmpty()) return emptyList()

        val firstKey = keys.firstOrNull() ?: return emptyList()
        val noteHalfWidth = (safeNoteheadWidth(firstKey, sv) ?: getMetricsFallbackHeadWidth()) / 2f
        val noteGap = sv.spacingBetweenLines * 0.5f
        val columnGap = sv.spacingBetweenLines * 0.5f
        val widths = accidentalWidthsPx(sv.spacingBetweenLines)

        val centers = MutableList(accidentalObjects.size) { 0f }
        centers[0] = x - noteHalfWidth - noteGap - (widths[0] / 2f)
        for (i in 1 until centers.size) {
            centers[i] = centers[i - 1] - (widths[i - 1] / 2f) - columnGap - (widths[i] / 2f)
        }
        return centers
    }

    private fun accidentalWidthsPx(staffSpacing: Float): List<Float> {
        return accidentalObjects.map { accidental ->
            runCatching {
                VFGlyphBoundingBoxManager.get(accidental.type.glyphName)
                    ?.scaled(staffSpacing)
                    ?.width
            }.getOrNull()
                ?.coerceAtLeast(staffSpacing * 0.35f)
                ?: (staffSpacing * 0.75f)
        }
    }

    private fun safeNoteheadWidth(key: String, sv: VFStave): Float? {
        return runCatching { noteheadPlacement(key, sv)?.bbox?.width }.getOrNull()
    }

    private fun getMetricsFallbackHeadWidth(): Float = glyphFontScale * 0.65f

    private fun drawDebugLabel(
        ctx: VexRenderingContext,
        label: String,
        centerX: Float,
        centerY: Float,
        glyphSizePx: Float
    ) {
        val fontSize = (glyphSizePx * 0.32f).coerceIn(10f, 16f)
        val baselineY = centerY + glyphSizePx * 0.95f
        ctx.setFontSize(fontSize)
        val xLeft = centerX - (ctx.measureText(label) / 2f)
        ctx.fillText(label, xLeft, baselineY)
    }

    private fun debugNoteLabel(key: String): String {
        val parts = key.lowercase().split("/")
        if (parts.size != 2) return key
        val pitch = parts[0]
        val octave = parts[1]
        if (pitch.isEmpty()) return key

        val step = pitch.first().uppercaseChar()
        val accidental = pitch.drop(1)
        return "$step$accidental$octave"
    }

    private fun debugAccidentalLabel(acc: VFAccidental): String = when (acc.type) {
        VFAccidental.AccidentalType.SHARP -> "#"
        VFAccidental.AccidentalType.FLAT -> "b"
        VFAccidental.AccidentalType.NATURAL -> "n"
        VFAccidental.AccidentalType.DOUBLE_SHARP -> "##"
        VFAccidental.AccidentalType.DOUBLE_FLAT -> "bb"
    }

    private fun shouldDrawDebugGlyphLabels(): Boolean {
        return System.getenv("LILYPOND_DEBUG_LAYOUT")?.equals("true", ignoreCase = true) == true
    }

    private fun noteheadPlacement(key: String, sv: VFStave): NoteGlyphPlacement? {
        val raw = VFGlyphBoundingBoxManager.get("noteheadBlack") ?: return null
        val staffSpacingForGlyph = glyphFontScale / 4f
        val scaled = raw.scaled(staffSpacingForGlyph)

        val noteCenterX = x
        val noteCenterY = sv.getYForNote(pitchToNoteLineIndex(key, sv))
        val centerOffsetX = (scaled.northeast.x + scaled.southwest.x) / 2f
        val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f

        return NoteGlyphPlacement(
            originX = noteCenterX - centerOffsetX,
            originY = noteCenterY - centerOffsetY,
            bbox = scaled
        )
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

    private var _isBeamed = false

    fun setBeamed(beamed: Boolean) {
        _isBeamed = beamed
    }

    fun isBeamed(): Boolean = _isBeamed

    companion object {
        fun pitchToNoteLineIndex(key: String, stave: VFStave?): Int {
            val parts = key.lowercase().split("/")
            if (parts.size < 2) return 4
            val pitchToken = parts[0]
            val octave = parts[1].toIntOrNull() ?: 4
            // Pitch token format is "<step><optional accidental>", e.g. b, bb, c#, fn.
            // The first character is always the diatonic step and must not be removed.
            val letter = pitchToken.firstOrNull() ?: 'c'

            val diatonicStep = when (letter) {
                'c' -> 0
                'd' -> 1
                'e' -> 2
                'f' -> 3
                'g' -> 4
                'a' -> 5
                'b' -> 6
                else -> 0
            }
            val absoluteStep = octave * 7 + diatonicStep

            val anchorAbsoluteStep = 32
            val anchorNoteLine = 6

            return anchorNoteLine - (absoluteStep - anchorAbsoluteStep)
        }

        private fun extractAccidental(key: String): String? {
            val pitchPart = key.lowercase().split("/").firstOrNull() ?: return null
            if (pitchPart.isEmpty()) return null

            // Key format is "<letter><optional accidental>/<octave>", e.g. "b/4", "bb/4", "c#/5".
            // The first character is always the diatonic step and must not be treated as an accidental.
            val accidentalSuffix = pitchPart.drop(1)
            return when (accidentalSuffix) {
                "##" -> "##"
                "bb" -> "bb"
                "#" -> "#"
                "b" -> "b"
                "n" -> "n"
                else -> null
            }
        }
    }
}
