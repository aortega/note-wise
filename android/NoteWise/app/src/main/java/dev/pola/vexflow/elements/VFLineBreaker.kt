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

    // Compact 2/4-like opening bars (for example MusicXML testsuite 01b) need a
    // small packing slack so the first row can host one extra bar before compression.
    private const val COMPACT_OPENING_PACKING_SLACK_PX = 12f

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
        systemSpacing: Float = 0f,
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
            var packingWidth = estimatePackingWidthForLineBreak(
                measure = measure,
                isRowStart = currentRow.isEmpty(),
                isFirstRowStart = packedRows.isEmpty() && currentRow.isEmpty() && firstSystemCount == 0
            )
            if (currentRow.isNotEmpty() && currentWidth + packingWidth > systemWidth) {
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

                // Re-evaluate as the first measure in the next row.
                packingWidth = estimatePackingWidthForLineBreak(
                    measure = measure,
                    isRowStart = true,
                    isFirstRowStart = false
                )
            }

            currentRow += measure
            currentWidth += packingWidth
        }

        if (currentRow.isNotEmpty()) {
            // Compute the "natural" width of the last row: the sum of each measure's minimum
            // content width with no stretching to fill the full system.
            // Also include the clef injection extra for the first measure: when a measure that had
            // no clef on its source stave becomes first in a new row, relayoutRow injects a clef
            // which expands the signature area. naturalWidth must include that extra so the compact
            // row isn't compressed below the actual minimum.
            val naturalWidth = currentRow.sumOf { estimateMinWidth(it).toDouble() }.toFloat() +
                computeClefInjectionExtra(currentRow)
            // Fill-ratio rule: if the last row is ≥67% full, expand it to match the other rows
            // so the score looks justified. Below that threshold the row is sparse, and stretching
            // would spread notes too far apart, so we use the natural compact width instead.
            val lastRowFillRatio = naturalWidth / systemWidth
            val lastRowWidth = if (lastRowFillRatio >= 0.67f) systemWidth else naturalWidth
            val (placed, bounds) = placeRow(
                row = currentRow.toList(),
                startX = startX,
                proposedY = currentY,
                systemWidth = lastRowWidth,
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
            val spacing = maxOf(
                additionalSystemSpacing,
                2f * maxOf(previousBounds.maxStaffSpacing, bounds.maxStaffSpacing)
            )
            val desiredTop = previousBounds.bottom + spacing
            val deltaY = desiredTop - bounds.top
            if (deltaY != 0f) {
                relaid = shiftRowVertically(relaid, deltaY)
                bounds = bounds.shifted(deltaY)
            }
        }

        return relaid to bounds
    }

    private fun computeClefInjectionExtra(row: List<MusicSheetToVF.RenderedMeasure>): Float {
        val firstStaff = row.firstOrNull()?.staves?.firstOrNull() ?: return 0f
        if (firstStaff.stave.clef != null) return 0f
        val spacing = firstStaff.stave.spacingBetweenLines
        val clef = VFClef(firstStaff.resolvedClefType, "default", null).apply {
            sizePx = spacing * 4f
        }
        return runCatching { clef.widthForStaffSpacing(spacing) + spacing }.getOrElse { 0f }
    }

    private fun relayoutRow(
        row: List<MusicSheetToVF.RenderedMeasure>,
        startX: Float,
        y: Float,
        systemWidth: Float
    ): List<MusicSheetToVF.RenderedMeasure> {
        // If the first measure in this row will receive an injected clef (its source stave has
        // none), add the clef width to its minimum budget so notes don't overflow the barline.
        val clefInjectionExtra = computeClefInjectionExtra(row)
        val minWidths = row.mapIndexed { index, measure ->
            estimateMinWidth(measure) + if (index == 0) clefInjectionExtra else 0f
        }
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
        val targetStaffYs = computeTargetStaffYs(row, y, staffNumbersInRow)

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
                staffRender.voices.forEach { voice ->
                    voice.setStave(relaidStave)
                    voice.tickables.forEach { note -> note.setStave(relaidStave) }
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
        staffNumbersInRow: List<Int>
    ): Map<Int, Float> {
        if (staffNumbersInRow.isEmpty()) return emptyMap()

        val stavesByMeasure = row.map { measure ->
            measure.staves.associateBy { it.staffNumber }
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
        // Rule 1: first staff's stave.y is fixed at rowStartY.
        // This matches alphaTab's behavior: the first stave starts at a fixed pixel offset
        // from the canvas top (rowStartY), regardless of content extents above the staff lines.
        // Content (clefs, high notes) may extend above rowStartY and will be clipped at y=0.
        ys[0] = rowStartY
        // Rule 2: each subsequent staff's content top is exactly 2 × staffSpacing below
        //         the previous staff's content bottom.
        for (index in 1 until staffNumbersInRow.size) {
            val prev = extentsByStaff[index - 1]
            val curr = extentsByStaff[index]
            val gap = 2f * maxOf(prev.spacing, curr.spacing)
            ys[index] = ys[index - 1] + prev.bottomDelta - curr.topDelta + gap
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

    private fun estimatePackingWidthForLineBreak(
        measure: MusicSheetToVF.RenderedMeasure,
        isRowStart: Boolean,
        isFirstRowStart: Boolean
    ): Float {
        var width = estimateMinWidth(measure)

        // When a row starts with a bar that originally had no clef, relayoutRow injects one.
        // Include this here too so packing decisions match actual rendered row widths.
        if (isRowStart && isCompactMeasure(measure)) {
            width += computeClefInjectionExtra(listOf(measure))
        }

        // First compact opening bar tends to be conservatively overestimated; allow a small
        // slack so row 1 can pack like alphaTab before proportional compression is applied.
        if (isFirstRowStart && measure.measureNumber == 1 && isCompactMeasure(measure)) {
            width -= COMPACT_OPENING_PACKING_SLACK_PX
        }

        return width.coerceAtLeast(0f)
    }

    private fun isCompactMeasure(measure: MusicSheetToVF.RenderedMeasure): Boolean {
        val expectedTicks = measure.staves
            .flatMap { it.voices }
            .map { it.getExpectedTotalTicks().doubleValue }

        val smallestExpected = expectedTicks.minOrNull() ?: return false
        return smallestExpected <= 0.5
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
        // Reserve space for the physical extent of the right barline plus a 2 px breathing gap.
        // For a light-heavy (END) barline this is ~9.75 px; for a single barline ~2.75 px.
        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f

        // Gourlay spring-rod model (mirrors alphaTab's BarLayoutingInfo).
        // Each beat slot contributes phi × minDurationWidth pixels of horizontal space, where:
        //   phi = 1 + 0.85 × log₂(slotDuration / minDuration)
        //   minDuration = 1/64 (64th note as fraction of a whole note)
        //   minDurationWidth = 7 px  (alphaTab default)
        // This replaces the flat minTickGap=10 approach and yields ~30.8 px/quarter vs the old
        // 10 px gap, closely matching alphaTab's bar-width and therefore its line-break points.
        val minDurationFraction = 1.0 / 64.0   // 64th note
        val minDurationWidth = 7.0
        val resolution = voices.firstOrNull()?.getResolutionMultiplier()?.toDouble() ?: 4096.0

        if (contexts.size == 1) {
            // Single beat: apply Gourlay spring width for the beat's own duration.
            val slotDuration = contexts[0].getMaxDuration().doubleValue.coerceAtLeast(minDurationFraction)
            val phi = 1.0 + 0.85 * (Math.log(slotDuration / minDurationFraction) / Math.log(2.0))
            val singleWidth = contexts[0].leftPx + (phi * minDurationWidth).toFloat() + contexts[0].rightPx + rightSafety
            return signatureGap + singleWidth
        }

        // preSpringWidth: left glyph extent of the first beat (accidentals + half-notehead).
        var voiceWidth = contexts.first().leftPx

        for (i in contexts.indices) {
            val slotVexTicks: Double = if (i < contexts.size - 1) {
                (contexts[i + 1].tickID - contexts[i].tickID).toDouble()
            } else {
                // Last slot: use the beat's own maximum note duration.
                contexts[i].getMaxDuration().doubleValue * resolution
            }
            val slotDuration = (slotVexTicks / resolution).coerceAtLeast(minDurationFraction)
            val phi = 1.0 + 0.85 * (Math.log(slotDuration / minDurationFraction) / Math.log(2.0))
            voiceWidth += (phi * minDurationWidth).toFloat()

            // Include the left accidental/glyph extent of the next beat position.
            if (i < contexts.size - 1) {
                voiceWidth += contexts[i + 1].leftPx
            }
        }

        // postSpringWidth: right glyph extent of the last beat.
        voiceWidth += contexts.last().rightPx
        voiceWidth += rightSafety
        return signatureGap + voiceWidth
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
                // Include stave lines themselves in bounds so inter-row spacing is measured
                // from stave bottom (not just note content), matching alphaTab's system-distance.
                top = minOf(top, stave.y + extents.topDelta, stave.getTopLineTopY())
                bottom = maxOf(bottom, stave.y + extents.bottomDelta, stave.getBottomLineBottomY())
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
        val suffix = pitch.substring(1)
        return VFAccidental.AccidentalType.fromString(suffix)?.glyphName
    }

    private fun safeGlyphBoundingBox(glyphName: String) =
        runCatching { VFGlyphBoundingBoxManager.get(glyphName) }.getOrNull()

    private fun keyHasAccidental(key: String): Boolean {
        val pitch = key.substringBefore('/')
        if (pitch.length <= 1) return false
        val suffix = pitch.substring(1)
        return VFAccidental.AccidentalType.fromString(suffix) != null
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
