package dev.pola.vexflow.elements

import android.graphics.PointF
import dev.pola.vexflow.core.VFFormatter
import dev.pola.vexflow.core.VFFormatterOptions
import dev.pola.vexflow.core.VFVoiceGroup
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.model.VFGlyphBoundingBox
import dev.pola.vexflow.model.VFGlyphBoundingBoxManager
import dev.pola.vexflow.model.VFTables
import dev.pola.vexflow.parser.MusicSheetToVF

/**
 * A horizontal row of measures (a score system).
 */
class VFSystem(
    val x: Float,
    val y: Float,
    val width: Float,
    val options: VFStaveOptions = VFStaveOptions()
) {
    private val measures = mutableListOf<MusicSheetToVF.RenderedMeasure>()
    private val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))

    private data class GrandStaffBraceGlyph(
        val codepoint: Int,
        val glyphName: String
    )

    private data class GrandStaffBraceLayout(
        val glyph: GrandStaffBraceGlyph,
        val scaledBounds: VFGlyphBoundingBox,
        val sizePx: Float
    )

    companion object {
        private const val SMUFL_EM_IN_STAFF_SPACES = 4f
        private const val GRAND_STAFF_BRACE_RIGHT_GAP_SPACES = 0.25f
        private const val GRAND_STAFF_BRACE_EXTRA_LEFT_PADDING_PX = 2f

        private var globalRemainingDebugNotesToDrawInFirstMeasures: Int? = null

        fun resetGlobalDebugFirstStaffNoteBudget(limit: Int?) {
            globalRemainingDebugNotesToDrawInFirstMeasures = limit
        }

        fun grandStaffBraceReservedInsetPx(spanHeightPx: Float, staffSpacing: Float): Float {
            val layout = computeGrandStaffBraceLayout(spanHeightPx, staffSpacing) ?: return staffSpacing * 1.8f
            return layout.scaledBounds.width +
                (staffSpacing * GRAND_STAFF_BRACE_RIGHT_GAP_SPACES) +
                GRAND_STAFF_BRACE_EXTRA_LEFT_PADDING_PX
        }

        fun grandStaffBraceReservedInsetPx(top: VFStave, bottom: VFStave): Float {
            val spanHeightPx = bottom.getBottomLineBottomY() - top.getTopLineTopY()
            val staffSpacing = maxOf(top.spacingBetweenLines, bottom.spacingBetweenLines)
            return grandStaffBraceReservedInsetPx(spanHeightPx, staffSpacing)
        }

        private fun computeGrandStaffBraceLayout(
            spanHeightPx: Float,
            staffSpacing: Float
        ): GrandStaffBraceLayout? {
            val glyph = selectGrandStaffBraceGlyph(spanHeightPx, staffSpacing)
            val raw = runCatching { VFGlyphBoundingBoxManager.get(glyph.glyphName) }.getOrNull()
                ?: fallbackBraceBoundingBox(glyph.glyphName)
                ?: return null
            val sizePx = (spanHeightPx * SMUFL_EM_IN_STAFF_SPACES / raw.height)
                .coerceAtLeast(staffSpacing * SMUFL_EM_IN_STAFF_SPACES)
            val scaledBounds = raw.scaled(sizePx / SMUFL_EM_IN_STAFF_SPACES)
            return GrandStaffBraceLayout(
                glyph = glyph,
                scaledBounds = scaledBounds,
                sizePx = sizePx
            )
        }

        private fun selectGrandStaffBraceGlyph(
            spanHeightPx: Float,
            staffSpacing: Float
        ): GrandStaffBraceGlyph {
            val staffHeightsCovered = spanHeightPx / (staffSpacing * SMUFL_EM_IN_STAFF_SPACES)
            return when {
                staffHeightsCovered >= 4.5f -> GrandStaffBraceGlyph(VFTables.GLYPH_BRACE_FLAT, "braceFlat")
                staffHeightsCovered >= 3.25f -> GrandStaffBraceGlyph(VFTables.GLYPH_BRACE_LARGER, "braceLarger")
                staffHeightsCovered >= 2.0f -> GrandStaffBraceGlyph(VFTables.GLYPH_BRACE_LARGE, "braceLarge")
                staffHeightsCovered >= 1.25f -> GrandStaffBraceGlyph(VFTables.GLYPH_BRACE_SMALL, "braceSmall")
                else -> GrandStaffBraceGlyph(VFTables.GLYPH_BRACE, "brace")
            }
        }

        private fun fallbackBraceBoundingBox(glyphName: String): VFGlyphBoundingBox? {
            val (southwestX, northeastX) = when (glyphName) {
                "brace" -> 0.008f to 0.328f
                "braceSmall" -> 0f to 0.412f
                "braceLarge" -> 0f to 0.268f
                "braceLarger" -> 0f to 0.24f
                "braceFlat" -> 0f to 0.224f
                else -> return null
            }
            return VFGlyphBoundingBox(
                northeast = PointF(northeastX, 3.988f),
                southwest = PointF(southwestX, 0f)
            )
        }
    }

    fun addMeasure(measure: MusicSheetToVF.RenderedMeasure) {
        measures += measure
    }

    fun draw(ctx: VexRenderingContext) {
        measures.forEachIndexed { measureIndex, rendered ->
            val sharedVoiceGroups = rendered.staves
                .filter {
                    it.multiMeasureRest == null &&
                        it.voices.isNotEmpty() &&
                        !isCenteredMeasureRestOnly(it)
                }
                .map { VFVoiceGroup(stave = it.stave, voices = it.voices) }
            val sharedTimingStaffNumbers = sharedVoiceGroups.map { group ->
                rendered.staves.first { it.stave === group.stave }.staffNumber
            }.toSet()
            val useSharedTimingGrid = sharedVoiceGroups.size > 1
            if (useSharedTimingGrid) {
                val sharedStartX = rendered.staves.maxOfOrNull { it.stave.getNoteStartX() } ?: 0f
                val sharedBarRight = rendered.staves.minOfOrNull { it.stave.x + it.stave.width } ?: sharedStartX
                val sharedJustify = (sharedBarRight - sharedStartX).coerceAtLeast(0f)
                formatter.formatVoiceGroups(
                    groups = sharedVoiceGroups,
                    startX = sharedStartX,
                    justifyWidth = sharedJustify,
                    referenceStave = rendered.staves.first().stave
                )
            }

            ctx.debugGlyphMeasureNumber = rendered.measureNumber
            rendered.staves.forEach { staffRender ->
                val stave = staffRender.stave
                ctx.debugGlyphStaffNumber = staffRender.staffNumber
                stave.draw(ctx)
                var drewNotesForStaff = false

                if (staffRender.multiMeasureRest != null) {
                    staffRender.multiMeasureRest.draw(stave, ctx)
                } else if (staffRender.voices.isNotEmpty()) {
                    if (!useSharedTimingGrid || staffRender.staffNumber !in sharedTimingStaffNumbers) {
                        val startX = stave.getNoteStartX()
                        val justify = (stave.width - (startX - stave.x)).coerceAtLeast(0f)
                        formatter.formatVoices(
                            voices = staffRender.voices,
                            stave = stave,
                            startX = startX,
                            justifyWidth = justify
                        )
                    }

                    when (debugRenderMode(rendered, staffRender)) {
                        DebugRenderMode.NORMAL -> {
                            staffRender.voices.forEach { it.draw(ctx) }
                            drewNotesForStaff = true
                        }
                        DebugRenderMode.FIRST_MEASURES_ONLY -> {
                            val remaining = globalRemainingDebugNotesToDrawInFirstMeasures ?: 0
                            if (remaining > 0) {
                                val allowed = staffRender.voices.flatMap { it.tickables }.take(remaining)
                                allowed.forEach { it.draw(ctx) }
                                globalRemainingDebugNotesToDrawInFirstMeasures = (remaining - allowed.size).coerceAtLeast(0)
                                drewNotesForStaff = allowed.isNotEmpty()
                            }
                        }
                        DebugRenderMode.SKIP -> {
                            // Intentionally suppress staff 1 notes outside the first debug measures.
                        }
                    }
                }

                if (drewNotesForStaff) {
                    staffRender.beams.forEach { it.draw(ctx, stave) }
                    staffRender.ties.forEach { it.draw(ctx) }
                }
                ctx.debugGlyphStaffNumber = null
            }

            if (rendered.staves.size > 1) {
                drawStaffGroupBarlines(
                    ctx,
                    rendered,
                    drawStart = measureIndex == 0
                )
            }
            ctx.debugGlyphMeasureNumber = null
        }
    }

    private fun isCenteredMeasureRestOnly(staffRender: MusicSheetToVF.RenderedStaff): Boolean {
        return staffRender.voices.isNotEmpty() &&
            staffRender.voices.all { voice ->
                voice.tickables.isNotEmpty() && voice.tickables.all { it.measureRestCount != null }
            }
    }

    private enum class DebugRenderMode {
        NORMAL,
        FIRST_MEASURES_ONLY,
        SKIP
    }

    private fun debugRenderMode(
        rendered: MusicSheetToVF.RenderedMeasure,
        staffRender: MusicSheetToVF.RenderedStaff
    ): DebugRenderMode {
        val debugEnabled = System.getenv("LILYPOND_DEBUG_LAYOUT")?.equals("true", ignoreCase = true) == true
        if (!debugEnabled) return DebugRenderMode.NORMAL
        if (globalRemainingDebugNotesToDrawInFirstMeasures == null) return DebugRenderMode.NORMAL
        if (staffRender.staffNumber != 1) return DebugRenderMode.NORMAL
        return if (rendered.measureNumber <= 2) DebugRenderMode.FIRST_MEASURES_ONLY else DebugRenderMode.SKIP
    }

    private fun drawStaffGroupBarlines(
        ctx: VexRenderingContext,
        rendered: MusicSheetToVF.RenderedMeasure,
        drawStart: Boolean
    ) {
        val top = rendered.staves.firstOrNull()?.stave ?: return
        val bottom = rendered.staves.lastOrNull()?.stave ?: return

        val yTop = top.getTopLineTopY()
        val yBottom = bottom.getBottomLineBottomY()

        if (drawStart) {
            drawGrandStaffBrace(ctx, top, bottom)
            top.startBarline?.drawSpan(ctx, yTop, yBottom, top.x)
        }
        top.endBarline?.drawSpan(ctx, yTop, yBottom, top.x + top.width)
    }

    private fun drawGrandStaffBrace(
        ctx: VexRenderingContext,
        top: VFStave,
        bottom: VFStave
    ) {
        val spacing = maxOf(top.spacingBetweenLines, bottom.spacingBetweenLines)
        val yTop = top.getTopLineTopY()
        val yBottom = bottom.getBottomLineBottomY()
        val layout = computeGrandStaffBraceLayout(yBottom - yTop, spacing) ?: return
        val desiredRightX = top.x - (spacing * GRAND_STAFF_BRACE_RIGHT_GAP_SPACES)
        val originX = desiredRightX - layout.scaledBounds.northeast.x
        val originY = yTop + layout.scaledBounds.northeast.y
        ctx.drawSmuflGlyph(layout.glyph.codepoint, originX, originY, layout.sizePx)
    }
}
