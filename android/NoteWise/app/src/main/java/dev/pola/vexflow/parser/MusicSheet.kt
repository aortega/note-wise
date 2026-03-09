package dev.pola.vexflow.parser

/**
 * Pure data model for parsed MusicXML content.
 */
data class MusicSheet(
    val title: String = "",
    val composer: String = "",
    val parts: List<Part> = emptyList()
)

data class Part(
    val id: String,
    val name: String = "",
    val measures: List<Measure> = emptyList()
)

data class Measure(
    val number: Int,
    val attributes: MeasureAttributes,
    val notes: List<NoteOrRest>,
    val tempoMarks: List<TempoMark> = emptyList(),
    val barlineLeft: String = "regular",
    val barlineRight: String = "regular"
)

data class MeasureAttributes(
    val divisions: Int = 1,
    val keyFifths: Int = 0,
    val keyMode: String = "major",
    val timeNumerator: Int = 4,
    val timeDenominator: Int = 4,
    val timeSymbol: String = "",
    val clefByStaff: Map<Int, String> = mapOf(1 to "treble")
)

fun MeasureAttributes.clefForStaff(staff: Int): String {
    return clefByStaff[staff] ?: clefByStaff[1] ?: "treble"
}

sealed class NoteOrRest {
    abstract val duration: Int
    abstract val voice: Int
    abstract val staff: Int
    abstract val isChordNote: Boolean
}

data class NoteData(
    val pitch: Pitch,
    override val duration: Int,
    override val voice: Int = 1,
    override val staff: Int = 1,
    val staffExplicit: Boolean = false,
    override val isChordNote: Boolean = false,
    val tieStart: Boolean = false,
    val tieEnd: Boolean = false,
    val slurStart: Boolean = false,
    val slurEnd: Boolean = false,
    val beamState: BeamState = BeamState.NONE,
    val accidental: String? = null,
    val notationType: String = "normal"
) : NoteOrRest()

data class RestData(
    override val duration: Int,
    override val voice: Int = 1,
    override val staff: Int = 1,
    val staffExplicit: Boolean = false,
    override val isChordNote: Boolean = false
) : NoteOrRest()

data class Pitch(
    val step: String,
    val octave: Int,
    val alter: Float = 0f
)

enum class BeamState { NONE, BEGIN, CONTINUE, END }

data class TempoMark(
    val beatUnit: String = "quarter",
    val bpm: Float,
    val divisions: Int = 0
)
