package dev.pola.notewise.visual

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFLineBreaker
import dev.pola.vexflow.elements.VFSystem
import dev.pola.vexflow.parser.MusicSheetToVF

object VisualRenderHarness {

    private data class ContentBBox(
        val staffNumber: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        fun rect(): RectF = RectF(left, top, right, bottom)
    }

    private data class GlyphBBox(
        val staffNumber: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        fun rect(): RectF = RectF(left, top, right, bottom)

        fun shifted(deltaY: Float): GlyphBBox =
            copy(top = top + deltaY, bottom = bottom + deltaY)
    }

    private data class RowRenderState(
        val measures: List<MusicSheetToVF.RenderedMeasure>,
        val glyphBBoxes: List<GlyphBBox>
    )

    private data class MeasureBBoxComparison(
        val measureNumber: Int,
        val barRect: RectF,
        val contentRect: RectF?
    )

    fun renderMeasuresToBitmap(
        measures: List<MusicSheetToVF.RenderedMeasure>,
        widthPx: Int,
        fixedHeightPx: Int? = null,
        horizontalPadding: Float = 0f,
        startY: Float = 70f,
        systemSpacing: Float = parseSystemSpacingFromEnv(default = 28.6f)
    ): Bitmap {
        val debugLayout = System.getenv("LILYPOND_DEBUG_LAYOUT")?.trim()?.lowercase() in setOf("1", "true", "yes", "on")
        val debugRefineRows = System.getenv("LILYPOND_DEBUG_REFINE_ROWS")?.trim()?.lowercase() in setOf("1", "true", "yes", "on")
        val showBarNumbers = System.getenv("LILYPOND_SHOW_BAR_NUMBERS")?.trim()?.lowercase() in setOf("1", "true", "yes", "on")
        val firstSystemTargetMeasures = parseFirstSystemTargetFromEnv(default = 6)
        val maxStaffSpacingPx = measures
            .flatMap { it.staves }
            .maxOfOrNull { it.stave.spacingBetweenLines }
            ?: 7f
        val leftDebugMarginPx = if (debugLayout) 2f * maxStaffSpacingPx else 0f
        val startX = horizontalPadding + leftDebugMarginPx
        val availableWidth = (widthPx.toFloat() - startX - horizontalPadding).coerceAtLeast(240f)

        val layout = VFLineBreaker.layout(
            measures = measures,
            systemWidth = availableWidth,
            startX = startX,
            startY = startY,
            systemSpacing = systemSpacing,
            firstSystemTargetMeasures = firstSystemTargetMeasures
        )

        val rowStates = if (debugLayout && debugRefineRows) {
            refineRowsUsingGlyphBBoxes(
                rows = layout.rows,
                widthPx = widthPx,
                horizontalPadding = startX,
                systemWidth = availableWidth
            )
        } else {
            layout.rows.map { row -> RowRenderState(measures = row, glyphBBoxes = emptyList()) }
        }

        val rowYs = rowStates.map { state ->
            state.measures
                .flatMap { it.staves }
                .minOfOrNull { it.stave.y }
                ?: startY
        }

        val maxGlyphBottom = rowStates
            .flatMap { it.glyphBBoxes }
            .maxOfOrNull { it.bottom }
            ?: 0f
        val maxStaffBottom = rowStates
            .flatMap { it.measures }
            .flatMap { it.staves }
            .maxOfOrNull { it.stave.getBottomLineBottomY() }
            ?: (startY + 120f)
        val dynamicHeight = (maxOf(maxGlyphBottom, maxStaffBottom) + 48f).toInt().coerceAtLeast(220)
        val totalHeight = fixedHeightPx?.coerceAtLeast(64) ?: dynamicHeight

        val bitmap = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val canvas = Canvas(bitmap)

        val ctx = VexRenderingContext()
        ctx.canvas = canvas
        ctx.debugCollectGlyphBoxes = debugLayout

        // Keep debug layout active, but do not truncate the rendered notes budget.
        // This allows all bars to be visible for inspection.
        VFSystem.resetGlobalDebugFirstStaffNoteBudget(null)

        val barNumberPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(30, 30, 30)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val barNumberBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(235, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val bboxStrokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val bboxFillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val bboxLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(20, 20, 20)
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val measureBarBoxPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.rgb(45, 95, 180)
        }
        val measureContentBoxPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            color = Color.rgb(220, 90, 20)
        }

        rowStates.zip(rowYs).forEachIndexed { rowIndex, (state, rowY) ->
            val rowMeasures = state.measures
            val system = VFSystem(
                x = startX,
                y = rowY,
                width = availableWidth
            )
            if (debugLayout) {
                println("[LILYPOND_DEBUG_LAYOUT] rowY=$rowY measures=${rowMeasures.size} systemWidth=$availableWidth")
                rowMeasures.forEach { rendered ->
                    rendered.staves.forEach { staff ->
                        println(
                            "[LILYPOND_DEBUG_LAYOUT] measure=${rendered.measureNumber} " +
                                "staff=${staff.staffNumber} x=${staff.stave.x} y=${staff.stave.y} " +
                                "width=${staff.stave.width} numLines=${staff.stave.numLines} " +
                                "voices=${staff.voices.size} notes=${staff.voices.sumOf { it.tickables.size }}"
                        )
                    }
                }
            }
            rowMeasures.forEach { system.addMeasure(it) }
            if (debugLayout) {
                ctx.consumeDebugGlyphBoxes()
            }
            system.draw(ctx)

            if (debugLayout) {
                val drawnGlyphBoxes = ctx.consumeDebugGlyphBoxes()
                val splitBBoxes = computeSplitContentBBoxes(rowMeasures)
                val glyphBBoxes = if (state.glyphBBoxes.isNotEmpty()) {
                    state.glyphBBoxes
                } else {
                    computeSplitGlyphBBoxes(drawnGlyphBoxes)
                }
                val measureComparisons = computeMeasureBBoxComparisons(rowMeasures, drawnGlyphBoxes)
                splitBBoxes.forEachIndexed { splitIndex, box ->
                    val hueColor = bboxPaletteColor(splitIndex)
                    bboxStrokePaint.color = hueColor
                    bboxFillPaint.color = Color.argb(24, Color.red(hueColor), Color.green(hueColor), Color.blue(hueColor))

                    val rect = box.rect()
                    canvas.drawRect(rect, bboxFillPaint)
                    canvas.drawRect(rect, bboxStrokePaint)

                    val label = "R${rowIndex + 1} S${box.staffNumber}"
                    val labelX = rect.left + 2f
                    val labelY = (rect.top - 3f).coerceAtLeast(11f)
                    canvas.drawText(label, labelX, labelY, bboxLabelPaint)

                    println(
                        "[LILYPOND_DEBUG_BBOX] row=${rowIndex + 1} split=${splitIndex + 1} staff=${box.staffNumber} " +
                            "left=${"%.2f".format(box.left)} top=${"%.2f".format(box.top)} " +
                            "right=${"%.2f".format(box.right)} bottom=${"%.2f".format(box.bottom)} " +
                            "points=[(${"%.2f".format(box.left)},${"%.2f".format(box.top)})," +
                            "(${"%.2f".format(box.right)},${"%.2f".format(box.top)})," +
                            "(${"%.2f".format(box.right)},${"%.2f".format(box.bottom)})," +
                            "(${"%.2f".format(box.left)},${"%.2f".format(box.bottom)})]"
                    )
                }

                glyphBBoxes.forEachIndexed { splitIndex, box ->
                    val hueColor = bboxPaletteColor(splitIndex)
                    bboxStrokePaint.color = Color.argb(255, Color.red(hueColor), Color.green(hueColor), Color.blue(hueColor))
                    bboxStrokePaint.strokeWidth = 2.25f
                    bboxFillPaint.color = Color.argb(36, Color.red(hueColor), Color.green(hueColor), Color.blue(hueColor))

                    val rect = box.rect()
                    canvas.drawRect(rect, bboxFillPaint)
                    canvas.drawRect(rect, bboxStrokePaint)

                    val label = "GLYPH R${rowIndex + 1} S${box.staffNumber}"
                    val labelX = rect.left + 2f
                    val labelY = (rect.bottom + 11f).coerceAtMost((bitmap.height - 3).toFloat())
                    canvas.drawText(label, labelX, labelY, bboxLabelPaint)

                    println(
                        "[LILYPOND_DEBUG_GLYPH_BBOX] row=${rowIndex + 1} split=${splitIndex + 1} staff=${box.staffNumber} " +
                            "left=${"%.2f".format(box.left)} top=${"%.2f".format(box.top)} " +
                            "right=${"%.2f".format(box.right)} bottom=${"%.2f".format(box.bottom)} " +
                            "points=[(${"%.2f".format(box.left)},${"%.2f".format(box.top)})," +
                            "(${"%.2f".format(box.right)},${"%.2f".format(box.top)})," +
                            "(${"%.2f".format(box.right)},${"%.2f".format(box.bottom)})," +
                            "(${"%.2f".format(box.left)},${"%.2f".format(box.bottom)})]"
                    )
                }

                measureComparisons.forEach { comparison ->
                    canvas.drawRect(comparison.barRect, measureBarBoxPaint)
                    comparison.contentRect?.let { canvas.drawRect(it, measureContentBoxPaint) }

                    val content = comparison.contentRect
                    val leftDelta = content?.left?.minus(comparison.barRect.left)
                    val topDelta = content?.top?.minus(comparison.barRect.top)
                    val rightDelta = content?.right?.minus(comparison.barRect.right)
                    val bottomDelta = content?.bottom?.minus(comparison.barRect.bottom)

                    println(
                        "[LILYPOND_DEBUG_MEASURE_BBOX] measure=${comparison.measureNumber} " +
                            "bar=[${"%.2f".format(comparison.barRect.left)},${"%.2f".format(comparison.barRect.top)}," +
                            "${"%.2f".format(comparison.barRect.right)},${"%.2f".format(comparison.barRect.bottom)}] " +
                            "content=${if (content == null) "none" else "[${"%.2f".format(content.left)},${"%.2f".format(content.top)},${"%.2f".format(content.right)},${"%.2f".format(content.bottom)}]"} " +
                            "deltaLTRB=${if (content == null) "none" else "[${"%.2f".format(leftDelta ?: 0f)},${"%.2f".format(topDelta ?: 0f)},${"%.2f".format(rightDelta ?: 0f)},${"%.2f".format(bottomDelta ?: 0f)}]"}"
                    )
                }
            }

            if (showBarNumbers) {
                rowMeasures.forEach { rendered ->
                    val topStave = rendered.staves.firstOrNull()?.stave ?: return@forEach
                    val label = rendered.measureNumber.toString()
                    val textW = barNumberPaint.measureText(label)
                    val baselineY = (topStave.getTopLineTopY() - 10f).coerceAtLeast(14f)
                    val bgLeft = topStave.x + 2f
                    val bgTop = baselineY - barNumberPaint.textSize + 2f
                    val bgRight = bgLeft + textW + 6f
                    val bgBottom = baselineY + 3f
                    canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, barNumberBgPaint)
                    canvas.drawText(label, bgLeft + 3f, baselineY, barNumberPaint)
                }
            }

            if (debugLayout && rowIndex > 0) {
                val firstMeasure = rowMeasures.firstOrNull()
                val topStave = firstMeasure?.staves?.firstOrNull()?.stave
                if (firstMeasure != null && topStave != null) {
                    val label = firstMeasure.measureNumber.toString()
                    val textW = barNumberPaint.measureText(label)
                    val targetBottomY = topStave.getTopLineTopY() - maxStaffSpacingPx
                    val fm = barNumberPaint.fontMetrics
                    // Keep the text box fully above targetBottomY so it never overlaps the staff bbox.
                    val baselineY = maxOf(12f - fm.ascent, targetBottomY - fm.descent - 2f)
                    val leftX = (topStave.x - leftDebugMarginPx + maxStaffSpacingPx * 0.2f).coerceAtLeast(2f)

                    val bgLeft = leftX
                    val bgTop = baselineY + fm.ascent - 2f
                    val bgRight = bgLeft + textW + 6f
                    val bgBottom = baselineY + fm.descent + 2f
                    canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, barNumberBgPaint)
                    canvas.drawText(label, bgLeft + 3f, baselineY, barNumberPaint)
                }
            }
        }

        return bitmap
    }

    private fun computeMeasureBBoxComparisons(
        rowMeasures: List<MusicSheetToVF.RenderedMeasure>,
        drawnGlyphBoxes: List<VexRenderingContext.DrawnGlyphBox>
    ): List<MeasureBBoxComparison> {
        if (rowMeasures.isEmpty()) return emptyList()

        val glyphByMeasure = drawnGlyphBoxes
            .filter { it.measureNumber != null }
            .groupBy { it.measureNumber!! }

        return rowMeasures.map { measure ->
            val barLeft = measure.staves.minOfOrNull { it.stave.x } ?: 0f
            val barTop = measure.staves.minOfOrNull { it.stave.getTopLineTopY() } ?: 0f
            val barRight = measure.staves.maxOfOrNull { it.stave.x + it.stave.width } ?: barLeft
            val barBottom = measure.staves.maxOfOrNull { it.stave.getBottomLineBottomY() } ?: barTop
            val barRect = RectF(barLeft, barTop, barRight, barBottom)

            val glyphs = glyphByMeasure[measure.measureNumber].orEmpty()
            val contentRect = if (glyphs.isEmpty()) {
                null
            } else {
                RectF(
                    glyphs.minOf { it.left },
                    glyphs.minOf { it.top },
                    glyphs.maxOf { it.right },
                    glyphs.maxOf { it.bottom }
                )
            }

            MeasureBBoxComparison(
                measureNumber = measure.measureNumber,
                barRect = barRect,
                contentRect = contentRect
            )
        }
    }

    private fun refineRowsUsingGlyphBBoxes(
        rows: List<List<MusicSheetToVF.RenderedMeasure>>,
        widthPx: Int,
        horizontalPadding: Float,
        systemWidth: Float
    ): List<RowRenderState> {
        if (rows.isEmpty()) return emptyList()

        val roughBottom = rows
            .flatten()
            .flatMap { it.staves }
            .maxOfOrNull { it.stave.getBottomLineBottomY() }
            ?: 800f
        val probeHeight = (roughBottom + 2400f).toInt().coerceAtLeast(2200)
        val probeBitmap = Bitmap.createBitmap(widthPx, probeHeight, Bitmap.Config.ARGB_8888)
        val probeCanvas = Canvas(probeBitmap)
        val probeCtx = VexRenderingContext().apply {
            canvas = probeCanvas
            debugCollectGlyphBoxes = true
        }

        val refinedStates = mutableListOf<RowRenderState>()

        for ((rowIndex, originalRow) in rows.withIndex()) {
            val baseRow = if (rowIndex == 0) originalRow else shiftRowByDelta(originalRow, 0f)
            val measuredGlyphBBoxes = measureRowGlyphBBoxes(
                rowMeasures = baseRow,
                rowY = baseRow.minOfOrNull { it.topY() } ?: 0f,
                horizontalPadding = horizontalPadding,
                systemWidth = systemWidth,
                ctx = probeCtx
            )

            val previous = refinedStates.lastOrNull()
            val targetGapPx = 2f * (
                baseRow
                    .flatMap { it.staves }
                    .maxOfOrNull { it.stave.spacingBetweenLines }
                    ?: 7f
            )

            val deltaY = if (previous == null || previous.glyphBBoxes.isEmpty() || measuredGlyphBBoxes.isEmpty()) {
                0f
            } else {
                computeRowDeltaForGlyphGap(
                    previous = previous.glyphBBoxes,
                    current = measuredGlyphBBoxes,
                    targetGapPx = targetGapPx
                )
            }

            if (deltaY == 0f) {
                refinedStates += RowRenderState(baseRow, measuredGlyphBBoxes)
            } else {
                val shiftedRow = shiftRowByDelta(baseRow, deltaY)
                val shiftedGlyphBoxes = measuredGlyphBBoxes.map { it.shifted(deltaY) }
                refinedStates += RowRenderState(shiftedRow, shiftedGlyphBoxes)
            }
        }

        probeBitmap.recycle()
        return refinedStates
    }

    private fun measureRowGlyphBBoxes(
        rowMeasures: List<MusicSheetToVF.RenderedMeasure>,
        rowY: Float,
        horizontalPadding: Float,
        systemWidth: Float,
        ctx: VexRenderingContext
    ): List<GlyphBBox> {
        val system = VFSystem(
            x = horizontalPadding,
            y = rowY,
            width = systemWidth
        )
        rowMeasures.forEach { system.addMeasure(it) }
        ctx.consumeDebugGlyphBoxes()
        system.draw(ctx)
        return computeSplitGlyphBBoxes(ctx.consumeDebugGlyphBoxes())
    }

    private fun computeRowDeltaForGlyphGap(
        previous: List<GlyphBBox>,
        current: List<GlyphBBox>,
        targetGapPx: Float
    ): Float {
        var requiredDelta = 0f
        val previousByStaff = previous.associateBy { it.staffNumber }
        val currentByStaff = current.associateBy { it.staffNumber }
        val sharedStaffs = previousByStaff.keys.intersect(currentByStaff.keys)

        if (sharedStaffs.isNotEmpty()) {
            sharedStaffs.forEach { staffNumber ->
                val prev = previousByStaff.getValue(staffNumber)
                val cur = currentByStaff.getValue(staffNumber)
                val minTop = prev.bottom + targetGapPx
                requiredDelta = maxOf(requiredDelta, minTop - cur.top)
            }
        } else {
            val prevBottom = previous.maxOfOrNull { it.bottom } ?: 0f
            val curTop = current.minOfOrNull { it.top } ?: 0f
            requiredDelta = maxOf(requiredDelta, (prevBottom + targetGapPx) - curTop)
        }

        return requiredDelta.coerceAtLeast(0f)
    }

    private fun shiftRowByDelta(
        row: List<MusicSheetToVF.RenderedMeasure>,
        deltaY: Float
    ): List<MusicSheetToVF.RenderedMeasure> {
        if (deltaY == 0f) return row

        return row.map { measure ->
            val shiftedStaves = measure.staves.map { staffRender ->
                val source = staffRender.stave
                val shifted = dev.pola.vexflow.elements.VFStave(
                    x = source.x,
                    y = source.y + deltaY,
                    width = source.width,
                    options = source.options
                ).apply {
                    lineThickness = source.lineThickness
                    clef = source.clef
                    keySignature = source.keySignature
                    timeSignature = source.timeSignature
                    startBarline = source.startBarline
                    endBarline = source.endBarline
                }
                staffRender.voices.forEach { voice ->
                    voice.setStave(shifted)
                    voice.tickables.forEach { note -> note.setStave(shifted) }
                }
                staffRender.copy(stave = shifted)
            }
            measure.copy(staves = shiftedStaves)
        }
    }

    private fun computeSplitContentBBoxes(rowMeasures: List<MusicSheetToVF.RenderedMeasure>): List<ContentBBox> {
        if (rowMeasures.isEmpty()) return emptyList()

        val renderedByStaff = linkedMapOf<Int, MutableList<MusicSheetToVF.RenderedStaff>>()
        rowMeasures.forEach { measure ->
            measure.staves.forEach { staff ->
                renderedByStaff.getOrPut(staff.staffNumber) { mutableListOf() }.add(staff)
            }
        }

        return renderedByStaff.entries.mapNotNull { (staffNumber, renderedStaves) ->
            var left = Float.MAX_VALUE
            var right = Float.MIN_VALUE
            var top = Float.MAX_VALUE
            var bottom = Float.MIN_VALUE

            renderedStaves.forEach { renderedStaff ->
                val stave = renderedStaff.stave
                left = minOf(left, stave.x)
                right = maxOf(right, stave.x + stave.width)

                var staffTop = stave.getTopLineTopY()
                var staffBottom = stave.getBottomLineBottomY()
                val spacing = stave.spacingBetweenLines

                renderedStaff.voices.forEach { voice ->
                    voice.tickables.forEach { note ->
                        val ys = note.getYs()
                        if (ys.isEmpty()) return@forEach

                        val noteTop = ys.minOrNull() ?: return@forEach
                        val noteBottom = ys.maxOrNull() ?: return@forEach
                        staffTop = minOf(staffTop, noteTop - spacing * 0.9f)
                        staffBottom = maxOf(staffBottom, noteBottom + spacing * 0.9f)

                        if (note.duration < dev.pola.vexflow.model.VFFraction.of(1, 1)) {
                            val stem = note.getStemExtents()
                            staffTop = minOf(staffTop, stem.baseY, stem.topY)
                            staffBottom = maxOf(staffBottom, stem.baseY, stem.topY)
                        }

                        if (note.keys.any { keyHasAccidental(it) }) {
                            staffTop = minOf(staffTop, noteTop - spacing * 2f)
                            staffBottom = maxOf(staffBottom, noteBottom + spacing * 2f)
                        }
                    }
                }

                top = minOf(top, staffTop)
                bottom = maxOf(bottom, staffBottom)
            }

            if (left == Float.MAX_VALUE || top == Float.MAX_VALUE || right == Float.MIN_VALUE || bottom == Float.MIN_VALUE) {
                null
            } else {
                ContentBBox(
                    staffNumber = staffNumber,
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom
                )
            }
        }
    }

    private fun computeSplitGlyphBBoxes(glyphBoxes: List<VexRenderingContext.DrawnGlyphBox>): List<GlyphBBox> {
        if (glyphBoxes.isEmpty()) return emptyList()

        return glyphBoxes
            .filter { it.staffNumber != null }
            .groupBy { it.staffNumber!! }
            .toSortedMap()
            .map { (staffNumber, boxes) ->
                GlyphBBox(
                    staffNumber = staffNumber,
                    left = boxes.minOf { it.left },
                    top = boxes.minOf { it.top },
                    right = boxes.maxOf { it.right },
                    bottom = boxes.maxOf { it.bottom }
                )
            }
    }

    private fun keyHasAccidental(key: String): Boolean {
        val pitch = key.substringBefore('/')
        if (pitch.length <= 1) return false
        val suffix = pitch.substring(1)
        return suffix == "#" || suffix == "b" || suffix == "n" || suffix == "##" || suffix == "bb"
    }

    private fun bboxPaletteColor(index: Int): Int {
        val palette = listOf(
            Color.rgb(214, 40, 40),
            Color.rgb(0, 109, 119),
            Color.rgb(255, 127, 80),
            Color.rgb(42, 157, 143),
            Color.rgb(77, 144, 142),
            Color.rgb(161, 97, 208)
        )
        return palette[index % palette.size]
    }

    private fun parseFirstSystemTargetFromEnv(default: Int): Int? {
        val raw = System.getenv("LILYPOND_FIRST_SYSTEM_MEASURES")?.trim().orEmpty()
        if (raw.isEmpty()) return default
        val parsed = raw.toIntOrNull() ?: return default
        return if (parsed <= 0) null else parsed
    }

    private fun parseSystemSpacingFromEnv(default: Float): Float {
        val raw = System.getenv("LILYPOND_SYSTEM_SPACING")?.trim().orEmpty()
        val parsed = raw.toFloatOrNull()
        return if (parsed != null && parsed >= 12f) parsed else default
    }
}
