package dev.pola.vexflow.elements

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.parser.MusicSheetToVF

/**
 * Refines row-to-row vertical spacing using bounds from actual rendered draw operations.
 * This keeps runtime and visual-test layout aligned and avoids relying only on heuristic
 * note/stem extents when staff-owned glyphs like clefs, time signatures, flags, beams,
 * ties, and barlines affect the visual content box.
 */
object VFRenderedRowSpacingRefiner {

    private const val MIN_TOP_PADDING_PX = 6f

    data class StaffGlyphBounds(
        val staffNumber: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        fun rect(): RectF = RectF(left, top, right, bottom)

        fun shifted(deltaY: Float): StaffGlyphBounds =
            copy(top = top + deltaY, bottom = bottom + deltaY)
    }

    data class RefinedRow(
        val measures: List<MusicSheetToVF.RenderedMeasure>,
        val glyphBounds: List<StaffGlyphBounds>
    )

    fun refineRows(
        rows: List<List<MusicSheetToVF.RenderedMeasure>>,
        widthPx: Int,
        horizontalPadding: Float,
        systemWidth: Float
    ): List<RefinedRow> {
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

        return try {
            val refinedStates = mutableListOf<RefinedRow>()

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

                val deltaY = if (previous == null || previous.glyphBounds.isEmpty() || measuredGlyphBBoxes.isEmpty()) {
                    0f
                } else {
                    computeRowDeltaForGlyphGap(
                        previous = previous.glyphBounds,
                        current = measuredGlyphBBoxes,
                        targetGapPx = targetGapPx
                    )
                }

                if (deltaY == 0f) {
                    refinedStates += RefinedRow(baseRow, measuredGlyphBBoxes)
                } else {
                    val shiftedRow = shiftRowByDelta(baseRow, deltaY)
                    val shiftedGlyphBoxes = measuredGlyphBBoxes.map { it.shifted(deltaY) }
                    refinedStates += RefinedRow(shiftedRow, shiftedGlyphBoxes)
                }
            }

            normalizeTopPadding(refinedStates)
        } finally {
            probeBitmap.recycle()
        }
    }

    private fun normalizeTopPadding(rows: List<RefinedRow>): List<RefinedRow> {
        if (rows.isEmpty()) return rows

        val minGlyphTop = rows
            .flatMap { it.glyphBounds }
            .minOfOrNull { it.top }

        val minStaffTop = rows
            .flatMap { it.measures }
            .flatMap { it.staves }
            .minOfOrNull { it.stave.getTopLineTopY() }

        val minObservedTop = when {
            minGlyphTop != null && minStaffTop != null -> minOf(minGlyphTop, minStaffTop)
            minGlyphTop != null -> minGlyphTop
            minStaffTop != null -> minStaffTop
            else -> MIN_TOP_PADDING_PX
        }

        val deltaY = (MIN_TOP_PADDING_PX - minObservedTop).coerceAtLeast(0f)
        if (deltaY <= 0f) return rows

        return rows.map { row ->
            RefinedRow(
                measures = shiftRowByDelta(row.measures, deltaY),
                glyphBounds = row.glyphBounds.map { it.shifted(deltaY) }
            )
        }
    }

    private fun measureRowGlyphBBoxes(
        rowMeasures: List<MusicSheetToVF.RenderedMeasure>,
        rowY: Float,
        horizontalPadding: Float,
        systemWidth: Float,
        ctx: VexRenderingContext
    ): List<StaffGlyphBounds> {
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
        previous: List<StaffGlyphBounds>,
        current: List<StaffGlyphBounds>,
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

    private fun computeSplitGlyphBBoxes(glyphBoxes: List<VexRenderingContext.DrawnGlyphBox>): List<StaffGlyphBounds> {
        if (glyphBoxes.isEmpty()) return emptyList()

        return glyphBoxes
            .filter { it.staffNumber != null }
            .groupBy { it.staffNumber!! }
            .toSortedMap()
            .map { (staffNumber, boxes) ->
                StaffGlyphBounds(
                    staffNumber = staffNumber,
                    left = boxes.minOf { it.left },
                    top = boxes.minOf { it.top },
                    right = boxes.maxOf { it.right },
                    bottom = boxes.maxOf { it.bottom }
                )
            }
    }

    private fun shiftRowByDelta(
        row: List<MusicSheetToVF.RenderedMeasure>,
        deltaY: Float
    ): List<MusicSheetToVF.RenderedMeasure> {
        if (deltaY == 0f) return row

        return row.map { measure ->
            val shiftedStaves = measure.staves.map { staffRender ->
                val source = staffRender.stave
                val shifted = VFStave(
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
}