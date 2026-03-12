package dev.pola.vexflow.core

import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.model.VFMetrics

data class VFFormatterOptions(
    val minWidth: Float = 10f
)

data class VFVoiceGroup(
    val stave: VFStave,
    val voices: List<VFVoice>
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
        formatVoiceGroups(
            groups = listOf(VFVoiceGroup(stave = stave, voices = voices)),
            startX = startX,
            justifyWidth = justifyWidth,
            referenceStave = stave
        )
    }

    fun formatVoiceGroups(
        groups: List<VFVoiceGroup>,
        startX: Float,
        justifyWidth: Float = 0f,
        referenceStave: VFStave? = null
    ) {
        val nonEmptyGroups = groups.filter { it.voices.isNotEmpty() }
        if (nonEmptyGroups.isEmpty()) return
        val resolvedReferenceStave = referenceStave ?: nonEmptyGroups.firstOrNull()?.stave ?: return

        nonEmptyGroups.forEach { group ->
            group.voices.forEach { voice ->
                if (voice.getStave() !== group.stave) {
                    voice.setStave(group.stave)
                } else {
                    voice.preFormat()
                }
            }
        }

        val contexts = collectTickContextsForGroups(nonEmptyGroups)
        contexts.forEach { it.preFormat() }
        if (applyCenteredFullMeasureRestIfApplicable(contexts, resolvedReferenceStave, startX)) {
            return
        }
        if (applyEqualBeatsGridIfApplicable(contexts, resolvedReferenceStave, startX, justifyWidth)) {
            return
        }
        val sharedSpacing = nonEmptyGroups.maxOfOrNull { it.stave.spacingBetweenLines }
            ?: resolvedReferenceStave.spacingBetweenLines
        assignXPositions(contexts, startX, justifyWidth, sharedSpacing, resolvedReferenceStave)
    }

    private fun applyCenteredFullMeasureRestIfApplicable(
        contexts: List<VFTickContext>,
        stave: VFStave,
        startX: Float
    ): Boolean {
        if (contexts.size != 1) return false
        val only = contexts.first()
        val tickables = only.getTickables()
        if (tickables.isEmpty()) return false
        if (!tickables.all { it.measureRestCount != null }) return false

        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f
        val noteAreaEnd = (stave.x + stave.width - rightSafety).coerceAtLeast(startX)
        only.x = (startX + noteAreaEnd) / 2f
        return true
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

        enforceMinimumContextSpacing(ordered, minGap = 0f)
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
            appendVoiceToContextMap(contextMap, voice, voiceIndex)
        }

        return contextMap.values.toList()
    }

    private fun collectTickContextsForGroups(groups: List<VFVoiceGroup>): List<VFTickContext> {
        val contextMap = sortedMapOf<Int, VFTickContext>()
        var voiceIndex = 0
        groups.forEach { group ->
            group.voices.forEach { voice ->
                appendVoiceToContextMap(contextMap, voice, voiceIndex)
                voiceIndex++
            }
        }

        return contextMap.values.toList()
    }

    private fun appendVoiceToContextMap(
        contextMap: MutableMap<Int, VFTickContext>,
        voice: VFVoice,
        voiceIndex: Int
    ) {
        var beatTick = 0
        val resolution = voice.getResolutionMultiplier()

        for (note in voice.tickables) {
            val ctx = contextMap.getOrPut(beatTick) { VFTickContext(beatTick) }
            ctx.addTickable(note, voiceIndex)

            val durationTicks = (note.duration.doubleValue * resolution).toInt().coerceAtLeast(1)
            beatTick += durationTicks
        }
    }

    private fun assignXPositions(
        contexts: List<VFTickContext>,
        startX: Float,
        justifyWidth: Float,
        staffSpacing: Float,
        stave: VFStave
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

        val minGap = if (contexts.all { ctx -> ctx.getTickables().all { it.isRest } }) 0f else options.minWidth
        enforceMinimumContextSpacing(contexts, minGap)
        fitRestOnlyContextsWithinMeasure(contexts, stave, startX)
    }

    private fun fitRestOnlyContextsWithinMeasure(
        contexts: List<VFTickContext>,
        stave: VFStave,
        startX: Float
    ) {
        if (contexts.isEmpty()) return
        if (!contexts.all { ctx -> ctx.getTickables().all { it.isRest } }) return

        val ordered = contexts.sortedBy { it.tickID }
        val rightSafety = (stave.endBarline?.leftExtentPx() ?: 0f) + 2f
        val noteAreaStart = startX
        val noteAreaEnd = (stave.x + stave.width - rightSafety).coerceAtLeast(noteAreaStart)
        if (noteAreaEnd <= noteAreaStart) return

        val currentLeft = ordered.minOf { it.x - it.leftPx }
        val currentRight = ordered.maxOf { it.x + it.rightPx }
        if (currentLeft >= noteAreaStart && currentRight <= noteAreaEnd) return

        val packedXs = FloatArray(ordered.size)
        packedXs[0] = noteAreaStart + ordered.first().leftPx

        val minRequiredGaps = FloatArray((ordered.size - 1).coerceAtLeast(0))
        for (i in 0 until ordered.lastIndex) {
            minRequiredGaps[i] = ordered[i].rightPx + ordered[i + 1].leftPx
            packedXs[i + 1] = packedXs[i] + minRequiredGaps[i]
        }

        val packedRight = packedXs.last() + ordered.last().rightPx
        if (packedRight >= noteAreaEnd) {
            ordered.forEachIndexed { index, ctx -> ctx.x = packedXs[index] }
            return
        }

        val shiftToPackedStart = packedXs.first() - ordered.first().x
        val desiredExtraGaps = FloatArray((ordered.size - 1).coerceAtLeast(0))
        var totalDesiredExtra = 0f
        for (i in 0 until ordered.lastIndex) {
            val shiftedCurrent = ordered[i].x + shiftToPackedStart
            val shiftedNext = ordered[i + 1].x + shiftToPackedStart
            val rhythmicGap = shiftedNext - shiftedCurrent
            val extra = (rhythmicGap - minRequiredGaps[i]).coerceAtLeast(0f)
            desiredExtraGaps[i] = extra
            totalDesiredExtra += extra
        }

        if (totalDesiredExtra <= 0f) {
            ordered.forEachIndexed { index, ctx -> ctx.x = packedXs[index] }
            return
        }

        val availableExtra = (noteAreaEnd - packedRight).coerceAtLeast(0f)
        val compression = (availableExtra / totalDesiredExtra).coerceIn(0f, 1f)

        var currentX = packedXs.first()
        ordered.first().x = currentX
        for (i in 1 until ordered.size) {
            currentX += minRequiredGaps[i - 1] + desiredExtraGaps[i - 1] * compression
            ordered[i].x = currentX
        }

        val finalRight = ordered.last().x + ordered.last().rightPx
        if (finalRight > noteAreaEnd) {
            val overflow = finalRight - noteAreaEnd
            ordered.forEach { it.x -= overflow }
        }

        val finalLeft = ordered.first().x - ordered.first().leftPx
        if (finalLeft < noteAreaStart) {
            val underflow = noteAreaStart - finalLeft
            ordered.forEach { it.x += underflow }
        }
    }

    private fun enforceMinimumContextSpacing(
        contexts: List<VFTickContext>,
        minGap: Float = options.minWidth
    ) {
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
