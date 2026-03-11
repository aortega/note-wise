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
        if (applyEqualBeatsGridIfApplicable(contexts, stave, startX, justifyWidth)) {
            return
        }
        assignXPositions(contexts, startX, justifyWidth, stave.spacingBetweenLines)
    }

    /**
     * Applies alphaTab-style equal-spring anchors when every beat in the measure has the
     * same duration (e.g. all quarter notes in 2/4, 3/4, or 4/4 time).  The first beat
     * is anchored one pre-spring width past [startX]; the remaining width is split into
     * N equal springs, one per beat.
     */
    private fun applyEqualBeatsGridIfApplicable(
        contexts: List<VFTickContext>,
        stave: VFStave,
        startX: Float,
        justifyWidth: Float
    ): Boolean {
        if (justifyWidth <= 0f || contexts.isEmpty()) return false
        val ordered = contexts.sortedBy { it.tickID }
        val firstDuration = ordered.first().getMaxDuration()
        if (!ordered.all { it.getMaxDuration() == firstDuration }) return false

        val barRight = stave.x + stave.width
        val referenceLeft = startX
        // Mirror alphaTab's equal-beat bar layout:
        // 1. The first beat's on-time anchor sits one pre-spring width after the modifier block.
        // 2. The remaining usable width is split into N equal springs (one per beat).
        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f
        val available = (barRight - referenceLeft - rightSafety).coerceAtLeast(0f)
        val firstPreSpringWidth = ordered.first().leftPx
        val springWidth = ((available - firstPreSpringWidth) / ordered.size.toFloat()).coerceAtLeast(0f)

        var onTimeX = firstPreSpringWidth
        ordered.forEach { ctx ->
            ctx.x = referenceLeft + onTimeX
            onTimeX += springWidth
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
