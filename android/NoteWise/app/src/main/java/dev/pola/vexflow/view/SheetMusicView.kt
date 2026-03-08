package dev.pola.vexflow.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import dev.pola.vexflow.core.VFFormatter
import dev.pola.vexflow.core.VFFormatterOptions
import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFStave

/**
 * Android View host for rendering a stave and voices using the VexFlow Kotlin core.
 */
class SheetMusicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val renderingContext = VexRenderingContext()
    private val formatter = VFFormatter(VFFormatterOptions(minWidth = 10f))

    private var stave: VFStave? = null
    private var voices: List<VFVoice> = emptyList()

    fun setVoices(newVoices: List<VFVoice>, newStave: VFStave) {
        stave = newStave
        voices = newVoices
        voices.forEach { voice ->
            voice.setStave(newStave)
            voice.preFormat()
        }
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sv = stave ?: return

        renderingContext.canvas = canvas
        sv.draw(renderingContext)

        if (voices.isNotEmpty()) {
            val startX = sv.getNoteStartX()
            val justify = (sv.width - (startX - sv.x)).coerceAtLeast(0f)
            formatter.formatAndDrawVoices(
                voices = voices,
                stave = sv,
                ctx = renderingContext,
                justifyWidth = justify
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = stave?.let { sv ->
            (sv.getBottomLineBottomY() + sv.spacingBetweenLines * 5f).toInt()
        } ?: 240

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}
