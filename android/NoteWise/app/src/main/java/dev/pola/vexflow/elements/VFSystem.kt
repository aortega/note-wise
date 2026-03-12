package dev.pola.vexflow.elements

import dev.pola.vexflow.core.VFFormatter
import dev.pola.vexflow.core.VFFormatterOptions
import dev.pola.vexflow.core.VexRenderingContext
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
    companion object {
        private var globalRemainingDebugNotesToDrawInFirstMeasures: Int? = null

        fun resetGlobalDebugFirstStaffNoteBudget(limit: Int?) {
            globalRemainingDebugNotesToDrawInFirstMeasures = limit
        }
    }

    fun addMeasure(measure: MusicSheetToVF.RenderedMeasure) {
        measures += measure
    }

    fun draw(ctx: VexRenderingContext) {
        for (rendered in measures) {
            ctx.debugGlyphMeasureNumber = rendered.measureNumber
            rendered.staves.forEach { staffRender ->
                val stave = staffRender.stave
                ctx.debugGlyphStaffNumber = staffRender.staffNumber
                stave.draw(ctx)
                var drewNotesForStaff = false

                if (staffRender.multiMeasureRest != null) {
                    staffRender.multiMeasureRest.draw(stave, ctx)
                } else if (staffRender.voices.isNotEmpty()) {
                    val startX = stave.getNoteStartX()
                    val justify = (stave.width - (startX - stave.x)).coerceAtLeast(0f)
                    formatter.formatVoices(
                        voices = staffRender.voices,
                        stave = stave,
                        startX = startX,
                        justifyWidth = justify
                    )

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
                drawStaffGroupBracket(ctx, rendered)
            }
            ctx.debugGlyphMeasureNumber = null
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

    private fun drawStaffGroupBracket(
        ctx: VexRenderingContext,
        rendered: MusicSheetToVF.RenderedMeasure
    ) {
        val top = rendered.staves.firstOrNull()?.stave ?: return
        val bottom = rendered.staves.lastOrNull()?.stave ?: return

        val xLeft = top.x + 2f
        val yTop = top.getTopLineTopY()
        val yBottom = bottom.getBottomLineBottomY()

        ctx.lineWidth = 2f
        ctx.beginPath()
        ctx.moveTo(xLeft, yTop)
        ctx.lineTo(xLeft, yBottom)
        ctx.stroke()

        val xRight = top.x + top.width - 2f
        ctx.beginPath()
        ctx.moveTo(xRight, yTop)
        ctx.lineTo(xRight, yBottom)
        ctx.stroke()
    }
}
