package dev.pola.vexflow.elements

import dev.pola.vexflow.parser.MusicSheetToVF
import dev.pola.vexflow.core.VFTickContext
import dev.pola.vexflow.model.VFFraction
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFMetrics

/**
 * Distributes measures across score systems based on available width.
 */
object VFLineBreaker {

    private fun measureRightSafetySpaces(): Float {
        val raw = System.getenv("LILYPOND_MEASURE_RIGHT_SAFETY_SPACES")?.trim().orEmpty()
        val parsed = raw.toFloatOrNull()
        return if (parsed != null && parsed >= 0f) parsed else 0f
    }

    private data class StaffContentExtents(
        val topDelta: Float,
        val bottomDelta: Float,
        val spacing: Float
    )

    private data class RowContentBounds(
        val top: Float,
        val bottom: Float,
        val maxStaffSpacing: Float
    ) {
        fun height(): Float = (bottom - top).coerceAtLeast(0f)
        fun shifted(deltaY: Float): RowContentBounds =
            copy(top = top + deltaY, bottom = bottom + deltaY)
    }

    data class SystemLayout(
        val rows: List<List<MusicSheetToVF.RenderedMeasure>>,
        val systemY: List<Float>,
        val systemHeights: List<Float>
    )

    fun layout(
        measures: List<MusicSheetToVF.RenderedMeasure>,
        systemWidth: Float,
        startX: Float,
        startY: Float,
        systemSpacing: Float = 80f,
        firstSystemTargetMeasures: Int? = null
    ): SystemLayout {
        if (measures.isEmpty()) {
            return SystemLayout(emptyList(), emptyList(), emptyList())
        }

        val packedRows = mutableListOf<List<MusicSheetToVF.RenderedMeasure>>()
        val ys = mutableListOf<Float>()
        val heights = mutableListOf<Float>()
        val currentRow = mutableListOf<MusicSheetToVF.RenderedMeasure>()
        var currentWidth = 0f
        var currentY = startY
        var previousRowBounds: RowContentBounds? = null

        val firstSystemCount = firstSystemTargetMeasures
            ?.coerceAtLeast(1)
            ?.let { minOf(it, measures.size) }
            ?: 0

        if (firstSystemCount > 0) {
            val firstRow = measures.take(firstSystemCount)
            val (placed, bounds) = placeRow(
                row = firstRow,
                startX = startX,
                proposedY = currentY,
                systemWidth = systemWidth,
                previousBounds = previousRowBounds,
                additionalSystemSpacing = systemSpacing
            )
            packedRows += placed
            ys += bounds.top
            heights += bounds.height()
            currentY = bounds.bottom
            previousRowBounds = bounds
        }

        for (measure in measures.drop(firstSystemCount)) {
            val measureMinWidth = estimateMinWidth(measure)
            if (currentRow.isNotEmpty() && currentWidth + measureMinWidth > systemWidth) {
                val (placed, bounds) = placeRow(
                    row = currentRow.toList(),
                    startX = startX,
                    proposedY = currentY,
                    systemWidth = systemWidth,
                    previousBounds = previousRowBounds,
                    additionalSystemSpacing = systemSpacing
                )
                packedRows += placed
                ys += bounds.top
                heights += bounds.height()
                currentRow.clear()
                currentWidth = 0f
                currentY = bounds.bottom
                previousRowBounds = bounds
            }

            currentRow += measure
            currentWidth += measureMinWidth
        }

        if (currentRow.isNotEmpty()) {
            val (placed, bounds) = placeRow(
                row = currentRow.toList(),
                startX = startX,
                proposedY = currentY,
                systemWidth = systemWidth,
                previousBounds = previousRowBounds,
                additionalSystemSpacing = systemSpacing
            )
            packedRows += placed
            ys += bounds.top
            heights += bounds.height()
        }

        return SystemLayout(rows = packedRows, systemY = ys, systemHeights = heights)
    }

    private fun placeRow(
        row: List<MusicSheetToVF.RenderedMeasure>,
        startX: Float,
        proposedY: Float,
        systemWidth: Float,
        previousBounds: RowContentBounds?,
        additionalSystemSpacing: Float
    ): Pair<List<MusicSheetToVF.RenderedMeasure>, RowContentBounds> {
        var relaid = relayoutRow(row, startX, proposedY, systemWidth)
        var bounds = estimateRowContentBounds(relaid)

        if (previousBounds != null) {
            val spacing = 2f * maxOf(previousBounds.maxStaffSpacing, bounds.maxStaffSpacing)
            val desiredTop = previousBounds.bottom + spacing + additionalSystemSpacing
            val deltaY = desiredTop - bounds.top
            if (deltaY != 0f) {
                relaid = shiftRowVertically(relaid, deltaY)
                bounds = bounds.shifted(deltaY)
            }
        }

        return relaid to bounds
    }

    private fun relayoutRow(
        row: List<MusicSheetToVF.RenderedMeasure>,
        startX: Float,
        y: Float,
        systemWidth: Float
    ): List<MusicSheetToVF.RenderedMeasure> {
        val minWidths = row.map { estimateMinWidth(it) }
        val minWidthSum = minWidths.sum()
        val weights = row.map { estimateWeight(it) }
        val weightSum = weights.sum().coerceAtLeast(1f)
        val extraSpace = (systemWidth - minWidthSum).coerceAtLeast(0f)

        val allocatedWidths: List<Float> = if (minWidthSum <= systemWidth) {
            // Normal expansion path: distribute remaining width by rhythmic weight.
            minWidths.mapIndexed { index, base ->
                base + extraSpace * (weights[index] / weightSum)
            }
        } else {
            // Compression path: fit the row exactly into available width.
            // For row 1 (measure 1...), keep bar 1 at minimum width and compress bars 2..n.
            allocateCompressedWidths(row, minWidths, systemWidth)
        }

        val rowTopY = row.minOfOrNull { it.topY() } ?: y
        val staffNumbersInRow = row
            .flatMap { measure -> measure.staves.map { staff -> staff.staffNumber } }
            .distinct()
            .sorted()
        val targetStaffYs = computeTargetStaffYs(row, y, rowTopY, staffNumbersInRow)

        var cursor = startX
        return row.mapIndexed { index, measure ->
            val rawWidth = allocatedWidths[index]
            val fallbackWidth = (systemWidth / row.size.coerceAtLeast(1)).coerceAtLeast(56f)
            val width = if (rawWidth.isFinite() && rawWidth > 1f) rawWidth else fallbackWidth
            val orderedStaves = measure.staves.sortedBy { it.stave.y }
            val relaidStaves = orderedStaves.mapIndexed { staffIndex, staffRender ->
                val sourceStave = staffRender.stave
                val relaidStave = cloneStave(
                    source = sourceStave,
                    x = cursor,
                    y = targetStaffYs[staffRender.staffNumber] ?: (y + (sourceStave.y - rowTopY)),
                    width = width
                )
                if (index == 0 && relaidStave.clef == null) {
                    relaidStave.clef = VFClef(staffRender.resolvedClefType, "default", null).apply {
                        sizePx = relaidStave.spacingBetweenLines * 4f
                    }
                }
                staffRender.copy(stave = relaidStave)
            }
            cursor += width

            measure.copy(staves = relaidStaves)
        }
    }

    private fun computeTargetStaffYs(
        row: List<MusicSheetToVF.RenderedMeasure>,
        rowStartY: Float,
        sourceRowTopY: Float,
        staffNumbersInRow: List<Int>
    ): Map<Int, Float> {
        if (staffNumbersInRow.isEmpty()) return emptyMap()

        val stavesByMeasure = row.map { measure ->
            measure.staves.associateBy { it.staffNumber }
        }

        val preferredOffsets = staffNumbersInRow.mapIndexed { index, staffNumber ->
            stavesByMeasure
                .firstNotNullOfOrNull { measureStaffs -> measureStaffs[staffNumber]?.stave?.y }
                ?.minus(sourceRowTopY)
                ?: (index * 80f)
        }

        val extentsByStaff = staffNumbersInRow.map { staffNumber ->
            val extents = stavesByMeasure.mapNotNull { measureStaffs ->
                measureStaffs[staffNumber]?.let { estimateStaffContentExtents(it) }
            }
            if (extents.isEmpty()) {
                StaffContentExtents(topDelta = 0f, bottomDelta = 40f, spacing = 10f)
            } else {
                StaffContentExtents(
                    topDelta = extents.minOf { it.topDelta },
                    bottomDelta = extents.maxOf { it.bottomDelta },
                    spacing = extents.maxOf { it.spacing }
                )
            }
        }

        val ys = MutableList(staffNumbersInRow.size) { 0f }
        ys[0] = rowStartY + preferredOffsets[0]
        for (index in 1 until staffNumbersInRow.size) {
            val preferredY = rowStartY + preferredOffsets[index]
            val previous = extentsByStaff[index - 1]
            val current = extentsByStaff[index]
            val requiredGap = 2f * maxOf(previous.spacing, current.spacing)
            // Use a small safety buffer in addition to the explicit 2x spacing gap,
            // because content extents are estimated from rendered geometry.
            val contentSafety = maxOf(previous.spacing, current.spacing)
            val minY = ys[index - 1] + previous.bottomDelta - current.topDelta + requiredGap + contentSafety
            ys[index] = maxOf(preferredY, minY)
        }

        // Keep absolute row content inside the visible canvas budget anchored by rowStartY.
        // Dynamic staff spacing avoids overlaps between staves; this shift avoids clipping above the canvas.
        val minContentTop = ys.indices.minOfOrNull { idx ->
            ys[idx] + extentsByStaff[idx].topDelta
        } ?: rowStartY
        val topSafetyMargin = extentsByStaff.maxOfOrNull { it.spacing } ?: 0f
        val targetTop = rowStartY + topSafetyMargin
        if (minContentTop < targetTop) {
            val shiftDown = targetTop - minContentTop
            for (idx in ys.indices) {
                ys[idx] += shiftDown
            }
        }

        return staffNumbersInRow.zip(ys).toMap()
    }

    private fun allocateCompressedWidths(
        row: List<MusicSheetToVF.RenderedMeasure>,
        minWidths: List<Float>,
        systemWidth: Float
    ): List<Float> {
        if (row.isEmpty()) return emptyList()

        val sum = minWidths.sum()
        if (sum <= 0f) {
            val equal = systemWidth / row.size.coerceAtLeast(1)
            return List(row.size) { equal }
        }

        val firstIsOpeningBar = row.firstOrNull()?.measureNumber == 1
        if (firstIsOpeningBar && row.size > 1) {
            val firstWidth = minWidths[0].coerceAtMost(systemWidth * 0.72f)
            val restMin = minWidths.drop(1).sum().coerceAtLeast(1f)
            val restTarget = (systemWidth - firstWidth).coerceAtLeast(0f)

            if (restTarget > 0f) {
                val restScale = restTarget / restMin
                return buildList(row.size) {
                    add(firstWidth)
                    minWidths.drop(1).forEach { add((it * restScale).coerceAtLeast(28f)) }
                }
            }
        }

        val scale = (systemWidth / sum).coerceAtLeast(0f)
        return minWidths.map { (it * scale).coerceAtLeast(28f) }
    }

    private fun estimateWeight(measure: MusicSheetToVF.RenderedMeasure): Float {
        val noteCount = measure.totalNoteCount()
        return noteCount.coerceAtLeast(1).toFloat()
    }

    private fun estimateMinWidth(measure: MusicSheetToVF.RenderedMeasure): Float {
        val heuristicWidth = run {
            val noteCount = measure.totalNoteCount()
            // Keep one-note/key-signature measures compact as a fallback.
            val noteAreaMin = maxOf(52f, noteCount * 20f)
            measure.noteStartOffset() + noteAreaMin
        }

        val contentAware = estimateContentAwareMinWidth(measure)
        return maxOf(heuristicWidth, contentAware)
    }

    private fun estimateContentAwareMinWidth(measure: MusicSheetToVF.RenderedMeasure): Float {
        val staffWidths = measure.staves.map { staffRender ->
            estimateStaffNoteAreaWidth(staffRender)
        }
        val maxNoteAreaWidth = staffWidths.maxOrNull() ?: 0f
        if (maxNoteAreaWidth <= 0f) return 0f
        return measure.noteStartOffset() + maxNoteAreaWidth
    }

    private fun estimateStaffNoteAreaWidth(staffRender: MusicSheetToVF.RenderedStaff): Float {
        val stave = staffRender.stave
        val voices = staffRender.voices
        if (voices.isEmpty()) return 0f

        // Ensure metrics reflect the current stave geometry (accidental spans depend on spacing).
        voices.forEach { voice ->
            if (voice.getStave() !== stave) {
                voice.setStave(stave)
            }
            voice.preFormat()
        }

        val contextMap = sortedMapOf<Int, VFTickContext>()
        for ((voiceIndex, voice) in voices.withIndex()) {
            var beatTick = 0
            val resolution = voice.getResolutionMultiplier()
            for (note in voice.tickables) {
                val ctx = contextMap.getOrPut(beatTick) { VFTickContext(beatTick) }
                ctx.addTickable(note, voiceIndex)
                beatTick += (note.duration.doubleValue * resolution).toInt()
            }
        }

        val contexts = contextMap.values.toList()
        if (contexts.isEmpty()) return 0f
        contexts.forEach { it.preFormat() }

        val signatureGap = VFMetrics.signatureToNotesGapPx(stave.spacingBetweenLines)
        val minTickGap = 10f

        if (contexts.size == 1) {
            // First context is anchored with its left extent consumed by x-positioning logic.
            val rightSafety = stave.spacingBetweenLines * measureRightSafetySpaces()
            return signatureGap + contexts.first().rightPx + rightSafety
        }

        var chainWidth = signatureGap
        for (i in 1 until contexts.size) {
            val previous = contexts[i - 1]
            val current = contexts[i]
            chainWidth += previous.rightPx + current.leftPx + minTickGap
        }
        chainWidth += contexts.last().rightPx
        // Explicit tail room prevents right-edge glyph overhangs crossing barlines.
        chainWidth += stave.spacingBetweenLines * measureRightSafetySpaces()
        return chainWidth
    }

    private fun estimateRowContentBounds(row: List<MusicSheetToVF.RenderedMeasure>): RowContentBounds {
        if (row.isEmpty()) {
            return RowContentBounds(top = 0f, bottom = 80f, maxStaffSpacing = 10f)
        }

        var top = Float.MAX_VALUE
        var bottom = Float.MIN_VALUE
        var maxStaffSpacing = 0f
        row.forEach { measure ->
            measure.staves.forEach { staffRender ->
                val stave = staffRender.stave
                val extents = estimateStaffContentExtents(staffRender)
                top = minOf(top, stave.y + extents.topDelta)
                bottom = maxOf(bottom, stave.y + extents.bottomDelta)
                maxStaffSpacing = maxOf(maxStaffSpacing, stave.spacingBetweenLines)
            }
        }

        if (top == Float.MAX_VALUE || bottom == Float.MIN_VALUE) {
            return RowContentBounds(top = 0f, bottom = 80f, maxStaffSpacing = maxOf(maxStaffSpacing, 10f))
        }
        return RowContentBounds(top = top, bottom = bottom, maxStaffSpacing = maxStaffSpacing)
    }

    private fun shiftRowVertically(
        row: List<MusicSheetToVF.RenderedMeasure>,
        deltaY: Float
    ): List<MusicSheetToVF.RenderedMeasure> {
        if (deltaY == 0f) return row

        return row.map { measure ->
            val shiftedStaves = measure.staves.map { staffRender ->
                val sourceStave = staffRender.stave
                val shiftedStave = cloneStave(
                    source = sourceStave,
                    x = sourceStave.x,
                    y = sourceStave.y + deltaY,
                    width = sourceStave.width
                )
                staffRender.voices.forEach { voice ->
                    voice.setStave(shiftedStave)
                    voice.tickables.forEach { note -> note.setStave(shiftedStave) }
                }
                staffRender.copy(stave = shiftedStave)
            }
            measure.copy(staves = shiftedStaves)
        }
    }

    private fun estimateStaffContentExtents(
        staffRender: MusicSheetToVF.RenderedStaff
    ): StaffContentExtents {
        val stave = staffRender.stave
        val spacing = stave.spacingBetweenLines
        var top = Float.MAX_VALUE
        var bottom = Float.MIN_VALUE
        var sawGlyphExtent = false

        for (voice in staffRender.voices) {
            for (note in voice.tickables) {
                // Bind to the current relaid stave so extent estimation does not use stale Y geometry.
                note.setStave(stave)
                val ys = note.getYs()
                if (ys.isEmpty()) continue

                val noteCenters = ys
                val noteheadExtents = noteheadVerticalExtents(noteCenters, note.glyphFontScale, note.duration)
                if (noteheadExtents != null) {
                    top = minOf(top, noteheadExtents.first)
                    bottom = maxOf(bottom, noteheadExtents.second)
                    sawGlyphExtent = true
                } else {
                    val noteTop = noteCenters.minOrNull() ?: continue
                    val noteBottom = noteCenters.maxOrNull() ?: continue
                    top = minOf(top, noteTop - spacing * 0.65f)
                    bottom = maxOf(bottom, noteBottom + spacing * 0.65f)
                    sawGlyphExtent = true
                }

                if (note.duration < dev.pola.vexflow.model.VFFraction.of(1, 1)) {
                    val stem = note.getStemExtents()
                    top = minOf(top, stem.baseY, stem.topY)
                    bottom = maxOf(bottom, stem.baseY, stem.topY)
                    sawGlyphExtent = true
                }

                val accidentalExtents = accidentalVerticalExtents(note.keys, noteCenters, note.glyphFontScale)
                if (accidentalExtents != null) {
                    top = minOf(top, accidentalExtents.first)
                    bottom = maxOf(bottom, accidentalExtents.second)
                    sawGlyphExtent = true
                }
            }
        }

        if (!sawGlyphExtent || top == Float.MAX_VALUE || bottom == Float.MIN_VALUE) {
            top = stave.getTopLineTopY()
            bottom = stave.getBottomLineBottomY()
        }

        return StaffContentExtents(
            topDelta = top - stave.y,
            bottomDelta = bottom - stave.y,
            spacing = spacing
        )
    }

    private fun noteheadVerticalExtents(
        noteCentersY: List<Float>,
        glyphFontScale: Float,
        duration: VFFraction
    ): Pair<Float, Float>? {
        val glyphName = when {
            duration >= VFFraction.of(1, 1) -> "noteheadWhole"
            duration >= VFFraction.of(1, 2) -> "noteheadHalf"
            else -> "noteheadBlack"
        }
        val raw = safeGlyphBoundingBox(glyphName) ?: return null
        val scaled = raw.scaled(glyphFontScale / 4f)
        val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f

        var top = Float.MAX_VALUE
        var bottom = Float.MIN_VALUE
        for (centerY in noteCentersY) {
            val originY = centerY - centerOffsetY
            val glyphTop = originY - scaled.northeast.y
            val glyphBottom = originY - scaled.southwest.y
            top = minOf(top, glyphTop)
            bottom = maxOf(bottom, glyphBottom)
        }
        if (top == Float.MAX_VALUE || bottom == Float.MIN_VALUE) return null
        return top to bottom
    }

    private fun accidentalVerticalExtents(
        keys: List<String>,
        noteCentersY: List<Float>,
        glyphFontScale: Float
    ): Pair<Float, Float>? {
        if (keys.isEmpty() || noteCentersY.isEmpty()) return null

        var top = Float.MAX_VALUE
        var bottom = Float.MIN_VALUE
        var sawAccidental = false
        val glyphStaffSpacing = glyphFontScale / 4f

        keys.forEachIndexed { index, key ->
            val accidental = accidentalGlyphName(key) ?: return@forEachIndexed
            val centerY = noteCentersY.getOrNull(index) ?: noteCentersY.last()
            val raw = safeGlyphBoundingBox(accidental)
            if (raw != null) {
                val scaled = raw.scaled(glyphStaffSpacing)
                val centerOffsetY = (scaled.northeast.y + scaled.southwest.y) / 2f
                val originY = centerY - centerOffsetY
                val glyphTop = originY - scaled.northeast.y
                val glyphBottom = originY - scaled.southwest.y
                top = minOf(top, glyphTop)
                bottom = maxOf(bottom, glyphBottom)
            } else {
                top = minOf(top, centerY - glyphStaffSpacing)
                bottom = maxOf(bottom, centerY + glyphStaffSpacing)
            }
            sawAccidental = true
        }

        if (!sawAccidental || top == Float.MAX_VALUE || bottom == Float.MIN_VALUE) return null
        return top to bottom
    }

    private fun accidentalGlyphName(key: String): String? {
        val pitch = key.substringBefore('/').lowercase()
        if (pitch.length <= 1) return null
        return when (pitch.substring(1)) {
            "#" -> "accidentalSharp"
            "b" -> "accidentalFlat"
            "n" -> "accidentalNatural"
            "##" -> "accidentalDoubleSharp"
            "bb" -> "accidentalDoubleFlat"
            else -> null
        }
    }

    private fun safeGlyphBoundingBox(glyphName: String) =
        runCatching { VFGlyphBoundingBoxManager.get(glyphName) }.getOrNull()

    private fun keyHasAccidental(key: String): Boolean {
        val pitch = key.substringBefore('/')
        if (pitch.length <= 1) return false
        val suffix = pitch.substring(1)
        return suffix == "#" || suffix == "b" || suffix == "n" || suffix == "##" || suffix == "bb"
    }

    private fun cloneStave(source: VFStave, x: Float, y: Float, width: Float): VFStave {
        return VFStave(
            x = x,
            y = y,
            width = width,
            options = source.options
        ).apply {
            lineThickness = source.lineThickness
            clef = source.clef
            keySignature = source.keySignature
            timeSignature = source.timeSignature
            startBarline = source.startBarline
            endBarline = source.endBarline
        }
    }
}
