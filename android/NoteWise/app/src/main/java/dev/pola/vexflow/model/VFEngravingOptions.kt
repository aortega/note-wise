package dev.pola.vexflow.model

/**
 * Engraving-level options that control visual notation style.
 */
data class VFEngravingOptions(
    val beamThickness: Float = VFMetrics.BEAM_THICKNESS,
    val beamSpacing: Float = VFMetrics.BEAM_SPACING,
    val maxBeamSlope: Float = 0.4f
)
