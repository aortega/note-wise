package dev.pola.vexflow.core

import dev.pola.vexflow.model.VFFraction
import dev.pola.vexflow.model.VFStaveNote

/**
 * A tick context groups notes that occur at the same beat position.
 * Its x assignment propagates to all notes it contains.
 */
class VFTickContext(val tickID: Int) {

	private val tickablesByVoice: MutableMap<Int, VFStaveNote> = mutableMapOf()

	var x: Float = 0f
		set(value) {
			field = value
			for (note in tickablesByVoice.values) {
				note.x = value
			}
		}

	var width: Float = 0f
	var leftPx: Float = 0f
		private set
	var rightPx: Float = 0f
		private set

	fun addTickable(note: VFStaveNote, voiceIndex: Int = 0) {
		tickablesByVoice[voiceIndex] = note
		note.setTickContext(this)
	}

	fun getTickables(): List<VFStaveNote> = tickablesByVoice.values.toList()

	fun getTickablesByVoice(): Map<Int, VFStaveNote> = tickablesByVoice.toMap()

	fun getMaxDuration(): VFFraction =
		tickablesByVoice.values.maxOfOrNull { it.duration } ?: VFFraction.ZERO

	fun preFormat() {
		leftPx = tickablesByVoice.values.maxOfOrNull { it.getMetrics().totalLeftPx } ?: 0f
		rightPx = tickablesByVoice.values.maxOfOrNull { it.getMetrics().totalRightPx } ?: 0f
		width = leftPx + rightPx
	}
}
