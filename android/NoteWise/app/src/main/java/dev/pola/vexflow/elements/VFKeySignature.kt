package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFGlyphAnchorPointManager
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFMetrics
import dev.pola.vexflow.model.VFTables

/**
 * Key signature element (M3).
 */
class VFKeySignature(keySpec: String = "C") {
    private val keySpec: String = keySpec
    private val accidentalsToDraw: List<AccidentalInfo> = determineAccidentals(keySpec)

    private data class AccidentalInfo(val glyphCodepoint: Int, val glyphName: String, val line: Float)

    // Optional explicit glyph font size. When <= 0, default to SMuFL's 4 spaces per em.
    var sizePx: Float = 0f
    var x: Float = 0f
    val width: Float
        get() {
            if (accidentalsToDraw.isEmpty()) return 0f
            val spacing = VFMetrics.DEFAULT_LINE_SPACING
            return widthForStaffSpacing(spacing)
        }

    fun widthForStaffSpacing(staffSpacing: Float): Float {
        if (accidentalsToDraw.isEmpty()) return 0f
        val glyphSize = effectiveGlyphSizePx(staffSpacing)
        return accidentalsToDraw.indices.sumOf { index ->
            accidentalAdvance(index, staffSpacing, glyphSize).toDouble()
        }.toFloat()
    }
    val accidentalCount: Int
        get() = accidentalsToDraw.size
    val isEmpty: Boolean
        get() = accidentalsToDraw.isEmpty()

    fun draw(stave: VFStave, ctx: VexRenderingContext) {
        var curX = x
        val glyphSize = effectiveGlyphSizePx(stave.spacingBetweenLines)
        for ((index, acc) in accidentalsToDraw.withIndex()) {
            val desiredAnchorY = stave.getYForLine(acc.line)
            val raw = VFGlyphBoundingBoxManager.get(acc.glyphName)
            if (raw == null) {
                ctx.drawSmuflGlyph(acc.glyphCodepoint, curX, desiredAnchorY, glyphSize)
                curX += glyphSize * 0.8f
                continue
            }
            val scaled = raw.scaled(stave.spacingBetweenLines)
            val centerOffsetX = (scaled.northeast.x + scaled.southwest.x) / 2f
            val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f
            val metadataAnchorOffsetY =
                VFGlyphAnchorPointManager.get(acc.glyphName)?.yAnchor?.times(stave.spacingBetweenLines)
            val glyphAnchorOffsetY = metadataAnchorOffsetY ?: centerOffsetY
            val originX = curX - centerOffsetX
            val originY = desiredAnchorY - glyphAnchorOffsetY
            ctx.drawSmuflGlyph(acc.glyphCodepoint, originX, originY, glyphSize)
            curX += accidentalAdvance(index = index, staffSpacing = stave.spacingBetweenLines, glyphSizePx = glyphSize)
        }
    }

    private fun accidentalAdvance(index: Int, staffSpacing: Float, glyphSizePx: Float): Float {
        val acc = accidentalsToDraw[index]
        if (index >= accidentalsToDraw.lastIndex) return standaloneAdvance(acc, staffSpacing, glyphSizePx)
        val next = accidentalsToDraw[index + 1]
        return interlockedAdvance(acc, next, staffSpacing, glyphSizePx)
    }

    private fun standaloneAdvance(acc: AccidentalInfo, staffSpacing: Float, glyphSizePx: Float): Float {
        val raw = VFGlyphBoundingBoxManager.get(acc.glyphName)
        if (raw == null) return glyphSizePx * 0.5f
        return (raw.width * staffSpacing) + (staffSpacing * 0.16f)
    }

    private fun interlockedAdvance(
        prev: AccidentalInfo,
        next: AccidentalInfo,
        staffSpacing: Float,
        glyphSizePx: Float
    ): Float {
        val prevBox = VFGlyphBoundingBoxManager.get(prev.glyphName) ?: return standaloneAdvance(prev, staffSpacing, glyphSizePx)
        val nextBox = VFGlyphBoundingBoxManager.get(next.glyphName) ?: return standaloneAdvance(prev, staffSpacing, glyphSizePx)
        val prevAnchors = VFGlyphAnchorPointManager.get(prev.glyphName)
        val nextAnchors = VFGlyphAnchorPointManager.get(next.glyphName)

        val dySpaces = (next.line - prev.line) * 0.5f
        val gapSpaces = 0.16f
        val baseSpaces = prevBox.width + gapSpaces

        val sampleY = buildSampleYList(prevBox, prevAnchors, nextBox, nextAnchors, dySpaces)
        var requiredOriginDx = Float.NEGATIVE_INFINITY
        var overlapFound = false
        for (y in sampleY) {
            val prevRight = rightBoundaryX(prevBox, prevAnchors, y) ?: continue
            val nextLeft = leftBoundaryX(nextBox, nextAnchors, y + dySpaces) ?: continue
            overlapFound = true
            requiredOriginDx = maxOf(requiredOriginDx, prevRight - nextLeft + gapSpaces)
        }

        if (!overlapFound) return baseSpaces * staffSpacing

        val prevCenterX = (prevBox.northeast.x + prevBox.southwest.x) / 2f
        val nextCenterX = (nextBox.northeast.x + nextBox.southwest.x) / 2f
        val requiredAnchorDx = requiredOriginDx + (nextCenterX - prevCenterX)
        val minAnchorDx = 0.18f

        return maxOf(requiredAnchorDx, minAnchorDx) * staffSpacing
    }

    private fun effectiveGlyphSizePx(staffSpacing: Float): Float {
        return if (sizePx > 0f) sizePx else staffSpacing * 4f
    }

    private fun buildSampleYList(
        prevBox: dev.pola.vexflow.model.VFGlyphBoundingBox,
        prevAnchors: dev.pola.vexflow.model.VFGlyphAnchorPoint?,
        nextBox: dev.pola.vexflow.model.VFGlyphBoundingBox,
        nextAnchors: dev.pola.vexflow.model.VFGlyphAnchorPoint?,
        dySpaces: Float
    ): List<Float> {
        val points = mutableSetOf<Float>()
        val epsilon = 0.001f

        fun addBreak(y: Float) {
            points += y
            points += y - epsilon
            points += y + epsilon
        }

        addBreak(prevBox.southwest.y)
        addBreak(prevBox.northeast.y)
        prevAnchors?.cutOutNE?.y?.let(::addBreak)
        prevAnchors?.cutOutSE?.y?.let(::addBreak)
        prevAnchors?.cutOutSW?.y?.let(::addBreak)
        prevAnchors?.cutOutNW?.y?.let(::addBreak)

        addBreak(nextBox.southwest.y - dySpaces)
        addBreak(nextBox.northeast.y - dySpaces)
        nextAnchors?.cutOutNE?.y?.let { addBreak(it - dySpaces) }
        nextAnchors?.cutOutSE?.y?.let { addBreak(it - dySpaces) }
        nextAnchors?.cutOutSW?.y?.let { addBreak(it - dySpaces) }
        nextAnchors?.cutOutNW?.y?.let { addBreak(it - dySpaces) }

        return points.sorted()
    }

    private fun rightBoundaryX(
        box: dev.pola.vexflow.model.VFGlyphBoundingBox,
        anchors: dev.pola.vexflow.model.VFGlyphAnchorPoint?,
        y: Float
    ): Float? {
        if (y < box.southwest.y || y > box.northeast.y) return null
        var x = box.northeast.x
        anchors?.cutOutSE?.let { cutOut ->
            if (y <= cutOut.y) x = minOf(x, cutOut.x)
        }
        anchors?.cutOutNE?.let { cutOut ->
            if (y >= cutOut.y) x = minOf(x, cutOut.x)
        }
        return x
    }

    private fun leftBoundaryX(
        box: dev.pola.vexflow.model.VFGlyphBoundingBox,
        anchors: dev.pola.vexflow.model.VFGlyphAnchorPoint?,
        y: Float
    ): Float? {
        if (y < box.southwest.y || y > box.northeast.y) return null
        var x = box.southwest.x
        anchors?.cutOutSW?.let { cutOut ->
            if (y <= cutOut.y) x = maxOf(x, cutOut.x)
        }
        anchors?.cutOutNW?.let { cutOut ->
            if (y >= cutOut.y) x = maxOf(x, cutOut.x)
        }
        return x
    }

    private fun determineAccidentals(spec: String): List<AccidentalInfo> {
        val fifthsMatch = Regex("^FIFTHS([+-]?\\d+)$").matchEntire(spec.trim().uppercase())
        if (fifthsMatch != null) {
            val fifths = fifthsMatch.groupValues[1].toIntOrNull() ?: 0
            return determineAccidentalsFromFifths(fifths)
        }

        val upper = spec.uppercase()
        val (count, isSharp) = when (upper) {
            "C", "AM" -> 0 to true
            "G", "EM" -> 1 to true
            "D", "BM" -> 2 to true
            "A", "F#M" -> 3 to true
            "E", "C#M" -> 4 to true
            "B", "G#M" -> 5 to true
            "F#", "D#M" -> 6 to true
            "C#", "A#M" -> 7 to true
            "F", "DM" -> 1 to false
            "BB", "GM" -> 2 to false
            "EB", "CM" -> 3 to false
            "AB", "FM" -> 4 to false
            "DB", "BBM" -> 5 to false
            "GB", "EBM" -> 6 to false
            "CB", "ABM" -> 7 to false
            else -> 0 to true
        }

        // Treble clef key-signature order (line positions from top staff line, spaces as .5).
        val sharpLines = floatArrayOf(0f, 2.5f, 0.5f, 3f, 1.5f, 3.5f, 2f)
        val flatLines = floatArrayOf(2f, 0.5f, 3f, 1.5f, 4f, 2.5f, 5f)
        val codepoint = if (isSharp) VFTables.GLYPH_ACCIDENTAL_SHARP else VFTables.GLYPH_ACCIDENTAL_FLAT
        val glyphName = if (isSharp) "accidentalSharp" else "accidentalFlat"
        val lines = if (isSharp) sharpLines else flatLines

        return buildList {
            for (i in 0 until count) {
                add(AccidentalInfo(codepoint, glyphName, lines[i]))
            }
        }
    }

    private fun determineAccidentalsFromFifths(fifths: Int): List<AccidentalInfo> {
        if (fifths == 0) return emptyList()

        // Treble clef key-signature order (line positions from top staff line, spaces as .5).
        val sharpLines = floatArrayOf(0f, 2.5f, 0.5f, 3f, 1.5f, 3.5f, 2f)
        val flatLines = floatArrayOf(2f, 0.5f, 3f, 1.5f, 4f, 2.5f, 5f)

        val useSharps = fifths > 0
        val count = kotlin.math.abs(fifths)
        val lines = if (useSharps) sharpLines else flatLines

        return buildList {
            for (i in 0 until count) {
                val line = lines[i % lines.size]
                val isDouble = i >= lines.size
                val codepoint = when {
                    useSharps && isDouble -> VFTables.GLYPH_ACCIDENTAL_DOUBLE_SHARP
                    useSharps -> VFTables.GLYPH_ACCIDENTAL_SHARP
                    !useSharps && isDouble -> VFTables.GLYPH_ACCIDENTAL_DOUBLE_FLAT
                    else -> VFTables.GLYPH_ACCIDENTAL_FLAT
                }
                val glyphName = when {
                    useSharps && isDouble -> "accidentalDoubleSharp"
                    useSharps -> "accidentalSharp"
                    !useSharps && isDouble -> "accidentalDoubleFlat"
                    else -> "accidentalFlat"
                }
                add(AccidentalInfo(codepoint, glyphName, line))
            }
        }
    }
}
