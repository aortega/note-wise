package dev.pola.notewise

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import dev.pola.vexflow.core.VFFormatter
import dev.pola.vexflow.core.VFFormatterOptions
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFBarline
import dev.pola.vexflow.elements.VFBarlineType
import dev.pola.vexflow.elements.VFBeam
import dev.pola.vexflow.elements.VFClef
import dev.pola.vexflow.elements.VFKeySignature
import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.elements.VFStaveOptions
import dev.pola.vexflow.elements.VFTie
import dev.pola.vexflow.elements.VFTieNotes
import dev.pola.vexflow.elements.VFTimeSignature
import dev.pola.vexflow.model.VFEngravingOptions
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct

/**
 * Smoke view for visual validation of M5 engraving elements.
 */
class RendererSmokeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val vexContext = VexRenderingContext()
    private val renderScale = 2.0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        vexContext.canvas = canvas
        vexContext.debugDrawGlyphAnchors = true

        val staveLeft = 40f
        val staveTop = 120f
        val staveWidth = (width - 80f).coerceAtLeast(240f)

        val stave = VFStave(
            x = staveLeft,
            y = staveTop,
            width = staveWidth,
            options = VFStaveOptions(
                spacingBetweenLinesPx = 10f * renderScale,
                engravingOptions = VFEngravingOptions(
                    beamThickness = 3.5f * renderScale,
                    beamSpacing = 5f * renderScale,
                    maxBeamSlope = 0.35f
                )
            )
        )
        stave.clef = VFClef("treble").apply {
            sizePx = 40f * renderScale
        }
        stave.keySignature = VFKeySignature("D").apply {
            sizePx = 30f * renderScale
        }
        stave.timeSignature = VFTimeSignature("4/4").apply {
            sizePx = 40f * renderScale
        }
        stave.startBarline = VFBarline(VFBarlineType.SINGLE)
        stave.endBarline = VFBarline(VFBarlineType.END)
        stave.lineThickness = 2f * renderScale
        stave.draw(vexContext)

        val voice = VFVoice("4/4")
        voice.setStave(stave)
        val notes = listOf("c/4", "c/4", "d/4", "e/4").map { key ->
            VFStaveNote(
                VFStaveNoteStruct(
                    keys = listOf(key),
                    duration = "8",
                    glyphFontScale = 40f * renderScale
                )
            )
        }
        voice.addTickables(notes)

        val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f * renderScale))
        val startX = stave.getNoteStartX()
        val justifyWidth = (stave.width - (startX - stave.x) - 20f).coerceAtLeast(0f)
        formatter.formatVoices(listOf(voice), stave, startX, justifyWidth)

        // Mark notes as beamed before note draw so per-note flags/stems are skipped.
        val beam = VFBeam(notes)
        voice.draw(vexContext)

        beam.draw(vexContext, stave)
        VFTie(VFTieNotes(firstNote = notes[0], lastNote = notes[1])).draw(vexContext)
    }
}
