package dev.pola.vexflow.model

/**
 * Global layout constants. All values are in screen pixels.
 */
object VFMetrics {
    // Match alphaTab opening spacing more closely: 6px initial inset and
    // 0.5-space pre-beat gaps at the default 9px staff spacing.
    const val CLEF_PADDING_SPACES: Float = 0.5f
    const val KEY_SIGNATURE_PADDING_SPACES: Float = 0.5f
    const val TIME_SIGNATURE_PADDING_SPACES: Float = 0.5f
    // Left inset before the first modifier, expressed in staff-space units.
    const val STAVE_LEFT_PADDING_SPACES: Float = 0.6666667f
    const val SIGNATURE_TO_NOTES_GAP_SPACES: Float = 0f
    const val STAVE_END_PADDING: Float = 10f
    const val DEFAULT_LINE_SPACING: Float = 10f
    const val STEM_HEIGHT_SPACES: Float = 3.5f
    const val STEM_THICKNESS_TO_SPACING: Float = 0.12f
    const val STEM_ATTACH_INSET_TO_HEAD_WIDTH: Float = 0.14f
    const val FLAG_ATTACH_OFFSET_SPACES: Float = 0.5f
    const val LEDGER_LINE_WIDTH_FACTOR: Float = 1.5f
    const val BEAM_THICKNESS: Float = 4f
    const val BEAM_SPACING: Float = 6f

    fun clefPaddingPx(staffSpacing: Float): Float = staffSpacing * CLEF_PADDING_SPACES

    fun keySignaturePaddingPx(staffSpacing: Float): Float =
        staffSpacing * KEY_SIGNATURE_PADDING_SPACES

    fun timeSignaturePaddingPx(staffSpacing: Float): Float =
        staffSpacing * TIME_SIGNATURE_PADDING_SPACES

    fun signatureToNotesGapPx(staffSpacing: Float): Float =
        staffSpacing * SIGNATURE_TO_NOTES_GAP_SPACES
}
