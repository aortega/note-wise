package dev.pola.vexflow.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import dev.pola.vexflow.core.VexRenderingContext
import dev.pola.vexflow.elements.VFLineBreaker
import dev.pola.vexflow.elements.VFRenderedRowSpacingRefiner
import dev.pola.vexflow.elements.VFSystem
import dev.pola.vexflow.parser.MusicSheetToVF

/**
 * Multi-row score renderer with automatic line breaking.
 */
class MultiStaveSheetMusicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        // Force onDraw invocation for bitmap-based visual tests and runtime rendering.
        setWillNotDraw(false)
    }

    private val renderingContext = VexRenderingContext()
    private var sourceMeasures: List<MusicSheetToVF.RenderedMeasure> = emptyList()
    private var systems: List<VFSystem> = emptyList()
    private var totalHeightPx: Float = 220f

    fun setMeasures(measures: List<MusicSheetToVF.RenderedMeasure>) {
        sourceMeasures = measures
        rebuildLayout(width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) {
            rebuildLayout(w)
        }
    }

    private fun rebuildLayout(viewWidth: Int) {
        if (sourceMeasures.isEmpty()) {
            systems = emptyList()
            totalHeightPx = 220f
            requestLayout()
            invalidate()
            return
        }

        if (viewWidth <= 0) {
            requestLayout()
            invalidate()
            return
        }

        val horizontalPadding = 0f
        val availableWidth = (viewWidth.toFloat() - horizontalPadding * 2f).coerceAtLeast(240f)
        val topY = 70f
        val systemSpacing = 0f

        val layout = VFLineBreaker.layout(
            measures = sourceMeasures,
            systemWidth = availableWidth,
            startX = horizontalPadding,
            startY = topY,
            systemSpacing = systemSpacing
        )

        val compactOnlyLayout = sourceMeasures.isNotEmpty() && sourceMeasures.all { isCompactMeasure(it) }
        val refinedRows = if (compactOnlyLayout) {
            layout.rows.map { row ->
                VFRenderedRowSpacingRefiner.RefinedRow(measures = row, glyphBounds = emptyList())
            }
        } else {
            VFRenderedRowSpacingRefiner.refineRows(
                rows = layout.rows,
                widthPx = viewWidth,
                horizontalPadding = horizontalPadding,
                systemWidth = availableWidth
            )
        }

        systems = refinedRows.map { rowState ->
            val rowMeasures = rowState.measures
            val rowY = rowMeasures.minOfOrNull { it.topY() } ?: topY
            VFSystem(
                x = horizontalPadding,
                y = rowY,
                width = availableWidth
            ).apply {
                rowMeasures.forEach { addMeasure(it) }
            }
        }

        val maxGlyphBottom = refinedRows
            .flatMap { it.glyphBounds }
            .maxOfOrNull { it.bottom }
            ?: 0f
        val maxStaffBottom = refinedRows
            .flatMap { it.measures }
            .flatMap { it.staves }
            .maxOfOrNull { it.stave.getBottomLineBottomY() }
            ?: (topY + 120f)
        val maxStaffSpacingPx = refinedRows
            .flatMap { it.measures }
            .flatMap { it.staves }
            .maxOfOrNull { it.stave.spacingBetweenLines }
            ?: 10f
        totalHeightPx = maxOf(maxGlyphBottom, maxStaffBottom) + maxStaffSpacingPx * 11f
        requestLayout()
        invalidate()
    }

    private fun isCompactMeasure(measure: MusicSheetToVF.RenderedMeasure): Boolean {
        val expectedTicks = measure.staves
            .flatMap { it.voices }
            .map { it.getExpectedTotalTicks().doubleValue }
        val smallestExpected = expectedTicks.minOrNull() ?: return false
        return smallestExpected <= 0.5
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderingContext.canvas = canvas
        systems.forEach { it.draw(renderingContext) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val resolvedWidth = resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec)
        val desiredHeight = totalHeightPx.toInt().coerceAtLeast(220)
        val resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }
}
