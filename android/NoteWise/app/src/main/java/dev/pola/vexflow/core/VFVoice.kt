package dev.pola.vexflow.core

import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.model.VFFraction
import dev.pola.vexflow.model.VFStaveNote

/**
 * A voice is a time-ordered sequence of notes that should fill a measure.
 */
class VFVoice(timeSpec: String = "4/4") {

    private val expectedTotalTicks: VFFraction = parseTimeSpec(timeSpec)

    val tickables: MutableList<VFStaveNote> = mutableListOf()

    private var stave: VFStave? = null

    fun addTickable(note: VFStaveNote) {
        tickables.add(note)
    }

    fun addTickables(notes: List<VFStaveNote>) {
        tickables.addAll(notes)
    }

    fun clear() {
        tickables.clear()
    }

    fun setStave(stave: VFStave) {
        this.stave = stave
        tickables.forEach { it.setStave(stave) }
    }

    fun getStave(): VFStave? = stave

    fun getExpectedTotalTicks(): VFFraction = expectedTotalTicks

    fun getTotalTicks(): VFFraction =
        tickables.fold(VFFraction.ZERO) { acc, note -> acc + note.duration }

    fun getResolutionMultiplier(): Int = 4096

    fun preFormat() {
        val sv = stave ?: return
        tickables.forEach { it.setStave(sv) }
    }

    fun draw(ctx: VexRenderingContext) {
        tickables.forEach { it.draw(ctx) }
    }

    companion object {
        fun parseTimeSpec(spec: String): VFFraction {
            val parts = spec.split("/")
            val n = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 4
            val d = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 4
            return VFFraction.of(n, d)
        }
    }
}
