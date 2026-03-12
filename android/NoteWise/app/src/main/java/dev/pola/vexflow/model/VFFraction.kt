package dev.pola.vexflow.model

/**
 * Immutable rational number. Used to represent note durations and beat positions.
 * Always stored in lowest terms (GCD-reduced). Denominator is always positive;
 * the sign lives in the numerator.
 */
data class VFFraction(val numerator: Int, val denominator: Int) : Comparable<VFFraction> {

    init {
        require(denominator != 0) { "Denominator must not be zero" }
    }

    companion object {
        val ZERO = VFFraction(0, 1)
        val ONE = VFFraction(1, 1)

        /** Create a reduced fraction. Normalises sign to numerator. */
        fun of(numerator: Int, denominator: Int): VFFraction {
            require(denominator != 0) { "Denominator must not be zero" }
            if (numerator == 0) return ZERO
            val sign = if (denominator < 0) -1 else 1
            val g = gcd(kotlin.math.abs(numerator), kotlin.math.abs(denominator))
            return VFFraction(sign * numerator / g, kotlin.math.abs(denominator) / g)
        }

        /** Parse a VexFlow duration string into a VFFraction. Returns null on unknown input. */
        fun fromDurationString(duration: String): VFFraction? {
            val token = duration.trim()
            val core = if (token.endsWith("r")) token.dropLast(1) else token
            val dotted = core.endsWith("d")
            val base = if (dotted) core.dropLast(1) else core
            val frac = when (base) {
                "w", "1" -> of(1, 1)
                "h", "2" -> of(1, 2)
                "q", "4" -> of(1, 4)
                "8" -> of(1, 8)
                "16" -> of(1, 16)
                "32" -> of(1, 32)
                "64" -> of(1, 64)
                "128" -> of(1, 128)
                "256" -> of(1, 256)
                "512" -> of(1, 512)
                "1024" -> of(1, 1024)
                else -> return null
            }
            return if (dotted) frac + frac * of(1, 2) else frac
        }

        private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    }

    operator fun plus(other: VFFraction): VFFraction =
        of(
            numerator * other.denominator + other.numerator * denominator,
            denominator * other.denominator
        )

    operator fun minus(other: VFFraction): VFFraction =
        of(
            numerator * other.denominator - other.numerator * denominator,
            denominator * other.denominator
        )

    operator fun times(other: VFFraction): VFFraction =
        of(numerator * other.numerator, denominator * other.denominator)

    operator fun div(other: VFFraction): VFFraction {
        require(other.numerator != 0) { "Division by zero fraction" }
        return of(numerator * other.denominator, denominator * other.numerator)
    }

    override fun compareTo(other: VFFraction): Int =
        (numerator.toLong() * other.denominator).compareTo(other.numerator.toLong() * denominator)

    val doubleValue: Double get() = numerator.toDouble() / denominator
    val floatValue: Float get() = numerator.toFloat() / denominator

    override fun toString(): String = "$numerator/$denominator"
}
