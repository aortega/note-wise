package dev.pola.vexflow.core

import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.model.VFMetrics

data class VFFormatterOptions(
    val minWidth: Float = 10f
)

/**
 * Assigns x positions to notes by grouping them into beat-aligned tick contexts.
 */
class VFFormatter(private val options: VFFormatterOptions = VFFormatterOptions()) {

    fun formatAndDraw(
        voice: VFVoice,
        stave: VFStave,
        ctx: VexRenderingContext,
        startX: Float
    ) {
        formatVoices(listOf(voice), stave, startX, stave.width - (startX - stave.x))
        voice.draw(ctx)
    }

    fun formatVoices(
        voices: List<VFVoice>,
        stave: VFStave,
        startX: Float,
        justifyWidth: Float = 0f
    ) {
        voices.forEach {
            if (it.getStave() == null) {
                it.setStave(stave)
            }
            it.preFormat()
        }

        val contexts = collectTickContexts(voices)
        contexts.forEach { it.preFormat() }
        if (applyFourQuarterGridIfApplicable(contexts, stave, startX, justifyWidth)) {
            return
        }
        assignXPositions(contexts, startX, justifyWidth, stave.spacingBetweenLines)
    }

    private fun applyFourQuarterGridIfApplicable(
        contexts: List<VFTickContext>,
        stave: VFStave,
        startX: Float,
        justifyWidth: Float
    ): Boolean {
        if (justifyWidth <= 0f || contexts.size != 4) return false
        val ordered = contexts.sortedBy { it.tickID }
        val quarter = dev.pola.vexflow.model.VFFraction.of(1, 4)
        if (!ordered.all { it.getMaxDuration() == quarter }) return false

        val barRight = stave.x + stave.width
        // Start the chain after signature modifiers so sequence reads left-to-right in note space.
        val referenceLeft = startX
        val noteheadWidths = ordered.map { (it.rightPx * 2f).coerceAtLeast(0f) }
        val accidentalExtraLeft = ordered.map { (it.leftPx - it.rightPx).coerceAtLeast(0f) }
        val totalNoteVisualWidth = noteheadWidths.sum() + accidentalExtraLeft.sum()
        val gap = ((barRight - referenceLeft) - totalNoteVisualWidth) / 5f
        val separation = gap.coerceAtLeast(0f)

        var cursor = referenceLeft + separation
        ordered.forEachIndexed { index, ctx ->
            val centerX = cursor + accidentalExtraLeft[index] + (noteheadWidths[index] / 2f)
            ctx.x = centerX
            cursor += accidentalExtraLeft[index] + noteheadWidths[index] + separation
        }
        return true
    }

    fun formatAndDrawVoices(
        voices: List<VFVoice>,
        stave: VFStave,
        ctx: VexRenderingContext,
        justifyWidth: Float = 0f
    ) {
        val startX = stave.getNoteStartX()
        formatVoices(voices, stave, startX, justifyWidth)
        voices.forEach { it.draw(ctx) }
    }

    private fun collectTickContexts(voices: List<VFVoice>): List<VFTickContext> {
        val contextMap = sortedMapOf<Int, VFTickContext>()

        for ((voiceIndex, voice) in voices.withIndex()) {
            var beatTick = 0
            val resolution = voice.getResolutionMultiplier()

            for (note in voice.tickables) {
                val ctx = contextMap.getOrPut(beatTick) { VFTickContext(beatTick) }
                ctx.addTickable(note, voiceIndex)

                val durationTicks = (note.duration.doubleValue * resolution).toInt()
                beatTick += durationTicks
            }
        }

        return contextMap.values.toList()
    }

    private fun assignXPositions(
        contexts: List<VFTickContext>,
        startX: Float,
        justifyWidth: Float,
        staffSpacing: Float
    ) {
        if (contexts.isEmpty()) return

        val totalWeight = contexts.sumOf { it.getMaxDuration().doubleValue }
        if (totalWeight <= 0.0) return

        var cumWeight = 0.0
        for ((index, ctx) in contexts.withIndex()) {
            val proportion = if (justifyWidth > 0f) cumWeight / totalWeight else 0.0
            val baseX = startX + (proportion * justifyWidth).toFloat()
            val leadingOffset = if (index == 0) {
                ctx.leftPx + VFMetrics.signatureToNotesGapPx(staffSpacing)
            } else {
                ctx.leftPx
            }
            ctx.x = baseX + leadingOffset
            cumWeight += ctx.getMaxDuration().doubleValue
        }

        val minGap = options.minWidth
        for (i in 1 until contexts.size) {
            val prev = contexts[i - 1]
            val curr = contexts[i]
            val minX = prev.x + prev.rightPx + curr.leftPx + minGap
            if (curr.x < minX) {
                curr.x = minX
            }
        }
    }
}
