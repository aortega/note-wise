package dev.pola.vexflow.parser

import dev.pola.vexflow.core.VFVoice
import dev.pola.vexflow.elements.VFBarline
import dev.pola.vexflow.elements.VFBarlineType
import dev.pola.vexflow.elements.VFBeam
import dev.pola.vexflow.elements.VFClef
import dev.pola.vexflow.elements.VFAccidental
import dev.pola.vexflow.elements.VFKeySignature
import dev.pola.vexflow.elements.VFStave
import dev.pola.vexflow.elements.VFStaveOptions
import dev.pola.vexflow.elements.VFTimeSignature
import dev.pola.vexflow.elements.VFTie
import dev.pola.vexflow.elements.VFTieNotes
import dev.pola.vexflow.elements.VFMultiMeasureRest
import dev.pola.vexflow.model.VFMetrics
import dev.pola.vexflow.model.VFStaveNote
import dev.pola.vexflow.model.VFStaveNoteStruct

/**
 * Converts a MusicSheet to VFStave + VFVoice objects ready for rendering.
 */
object MusicSheetToVF {

    private const val GRAND_STAFF_GAP_PX = 88f
    // SMuFL defines 1em as 4 staff spaces.
    private const val SMUFL_EM_IN_STAFF_SPACES = 4f
    private const val INFER_MIN_RANGE_STEPS = 21

    data class RenderedStaff(
        val staffNumber: Int,
        val resolvedClefType: String,
        val stave: VFStave,
        val voices: List<VFVoice>,
        val beams: List<VFBeam>,
        val ties: List<VFTie>,
        val multiMeasureRest: VFMultiMeasureRest? = null
    )

    data class RenderedMeasure(
        val measureNumber: Int,
        val staves: List<RenderedStaff>
    ) {
        fun totalNoteCount(): Int = staves.sumOf { staff ->
            staff.voices.sumOf { voice -> voice.tickables.size }
        }

        fun noteStartOffset(): Float {
            val leftMost = staves.minOfOrNull { staff -> staff.stave.x } ?: return 0f
            return staves.maxOfOrNull { staff -> staff.stave.getNoteStartX() - leftMost } ?: 0f
        }

        fun topY(): Float = staves.minOfOrNull { it.stave.y } ?: 0f
    }

    fun convert(
        sheet: MusicSheet,
        startX: Float,
        startY: Float,
        staveWidth: Float,
        partIndex: Int = 0,
        showClef: Boolean = true,
        showKeySig: Boolean = true,
        showTimeSig: Boolean = true,
        staffLineSpacingPx: Float = VFMetrics.DEFAULT_LINE_SPACING
    ): List<RenderedMeasure> {
        if (sheet.parts.isEmpty()) return emptyList()

        val selectedPart = when {
            partIndex in sheet.parts.indices -> sheet.parts[partIndex]
            else -> sheet.parts.firstOrNull { it.measures.isNotEmpty() } ?: sheet.parts.first()
        }

        // If requested/default part is empty, fall back to first non-empty part for resilient rendering.
        val part = if (selectedPart.measures.isNotEmpty()) {
            selectedPart
        } else {
            sheet.parts.firstOrNull { it.measures.isNotEmpty() } ?: selectedPart
        }

        if (part.measures.isEmpty()) return emptyList()
        val staffResolver = buildStaffResolver(part)
        val hasExplicitStaffTags = part.measures
            .flatMap { it.notes }
            .any { note ->
                when (note) {
                    is NoteData -> note.staffExplicit
                    is RestData -> note.staffExplicit
                }
            }
        val openingBoundarySpacingSpaces = determineOpeningBoundarySpacingSpaces(
            part = part,
            staveWidth = staveWidth,
            staffLineSpacingPx = staffLineSpacingPx,
            showClef = showClef,
            showKeySig = showKeySig,
            showTimeSig = showTimeSig,
            staffResolver = staffResolver
        )
        var previousAttributes: MeasureAttributes? = null
        val seenVirtualStaffs = mutableSetOf<Int>()
        var previousMeasureStaffNumbers: Set<Int> = emptySet()

        val result = mutableListOf<RenderedMeasure>()
        var visualIndex = 0
        var skipRemaining = 0

        for (measure in part.measures) {
            if (skipRemaining > 0) {
                skipRemaining--
                continue
            }
            val attrs = measure.attributes
            val measureStaffNumbers = measure.notes.map(staffResolver).distinct().ifEmpty { listOf(1) }
            val firstAppearanceStaffs = measureStaffNumbers.filter { it !in seenVirtualStaffs }.toSet()
            val segmentStartStaffs = measureStaffNumbers.filter { it !in previousMeasureStaffNumbers }.toSet()
            seenVirtualStaffs.addAll(measureStaffNumbers)
            val showKeySigForMeasure =
                visualIndex == 0 || previousAttributes?.let {
                    it.keyFifths != attrs.keyFifths || it.keyMode != attrs.keyMode
                } ?: true
            val showTimeSigForMeasure =
                visualIndex == 0 || previousAttributes?.let {
                    it.timeNumerator != attrs.timeNumerator ||
                        it.timeDenominator != attrs.timeDenominator ||
                        it.timeSymbol != attrs.timeSymbol
                } ?: true

            result += convertMeasure(
                measure = measure,
                x = startX + visualIndex * staveWidth,
                y = startY,
                width = staveWidth,
                showClef = showClef,
                showKeySig = showKeySig && showKeySigForMeasure,
                showTimeSig = showTimeSig && showTimeSigForMeasure,
                previousAttributes = previousAttributes,
                staffLineSpacingPx = staffLineSpacingPx,
                openingBoundarySpacingSpaces = openingBoundarySpacingSpaces,
                staffResolver = staffResolver,
                firstAppearanceStaffs = firstAppearanceStaffs,
                segmentStartStaffs = segmentStartStaffs,
                inferVirtualClefs = !hasExplicitStaffTags
            ).also {
                previousAttributes = attrs
                previousMeasureStaffNumbers = measureStaffNumbers.toSet()
            }

            if (attrs.multipleRestCount > 1) {
                skipRemaining = attrs.multipleRestCount - 1
            }
            visualIndex++
        }
        return result
    }

    private fun convertMeasure(
        measure: Measure,
        x: Float,
        y: Float,
        width: Float,
        showClef: Boolean,
        showKeySig: Boolean,
        showTimeSig: Boolean,
        previousAttributes: MeasureAttributes?,
        staffLineSpacingPx: Float,
        openingBoundarySpacingSpaces: Float,
        staffResolver: (NoteOrRest) -> Int,
        firstAppearanceStaffs: Set<Int>,
        segmentStartStaffs: Set<Int>,
        inferVirtualClefs: Boolean
    ): RenderedMeasure {
        val attrs = measure.attributes

        val staffNumbers = measure.notes.map(staffResolver).distinct().sorted().ifEmpty { listOf(1) }
        val chordGroups = buildChordGroups(measure.notes)
        val measureDurationDivisions = measureDurationInDivisions(attrs)

        val renderedStaves = staffNumbers.mapIndexed { staffIndex, staffNumber ->
            val clefForStaff = resolvedClefForStaff(
                attrs = attrs,
                staffNumber = staffNumber,
                inferVirtualClefs = inferVirtualClefs
            )
            val showClefForStaff = showClef && (
                staffNumber in firstAppearanceStaffs ||
                (inferVirtualClefs && staffNumber in segmentStartStaffs) ||
                previousAttributes == null ||
                    previousAttributes.clefForStaff(staffNumber) != attrs.clefForStaff(staffNumber)
                )

            val stave = VFStave(
                x = x,
                y = y + staffIndex * GRAND_STAFF_GAP_PX,
                width = width,
                options = VFStaveOptions(
                    spacingBetweenLinesPx = staffLineSpacingPx,
                    openingBoundarySpacingSpaces = openingBoundarySpacingSpaces
                )
            ).apply {
                if (showClefForStaff) {
                    clef = VFClef(clefForStaff, "default", null).apply {
                        sizePx = staffLineSpacingPx * SMUFL_EM_IN_STAFF_SPACES
                    }
                }
                if (showKeySig && staffIndex == 0) {
                    keySignature = VFKeySignature(fifthsToKeySpec(attrs.keyFifths, attrs.keyMode))
                }
                if (showTimeSig && staffIndex == 0) {
                    timeSignature = VFTimeSignature(attrs.timeSymbol.ifEmpty { "${attrs.timeNumerator}/${attrs.timeDenominator}" }).apply {
                        sizePx = staffLineSpacingPx * SMUFL_EM_IN_STAFF_SPACES
                    }
                }
                startBarline = VFBarline(mapBarlineStyle(measure.barlineLeft))
                endBarline = VFBarline(mapBarlineStyle(measure.barlineRight))
            }

            val staffGroups = chordGroups.filter { staffResolver(it.primary) == staffNumber }
            val voiceNumbers = staffGroups.map { it.primary.voice }.distinct().sorted()
            val voices = mutableListOf<VFVoice>()
            val beams = mutableListOf<VFBeam>()
            val ties = mutableListOf<VFTie>()

            for (voiceNum in voiceNumbers) {
                val groups = staffGroups.filter { it.primary.voice == voiceNum }
                val vfNotes = mutableListOf<VFStaveNote>()

                val activeTieByPitch = mutableMapOf<String, VFStaveNote>()
                val beamRun = mutableListOf<VFStaveNote>()

                for (group in groups) {
                    val measureRestCount = (group.primary as? RestData)
                        ?.takeIf { isFullMeasureRest(it, groups.size, measureDurationDivisions) }
                        ?.let { 1 }

                    val vfNote = buildVFNote(
                        group = group,
                        divisions = attrs.divisions,
                        keyFifths = attrs.keyFifths,
                        staffLineSpacingPx = staffLineSpacingPx,
                        staffResolver = staffResolver,
                        measureRestCount = measureRestCount
                    )
                    vfNotes.add(vfNote)

                    val primary = group.primary
                    if (primary is NoteData) {
                        val key = pitchToKey(primary.pitch, primary.accidental, attrs.keyFifths)

                        if (primary.tieEnd) {
                            activeTieByPitch[key]?.let { start ->
                                ties += VFTie(VFTieNotes(firstNote = start, lastNote = vfNote))
                                activeTieByPitch.remove(key)
                            }
                        }
                        if (primary.tieStart) {
                            activeTieByPitch[key] = vfNote
                        }

                        when (primary.beamState) {
                            BeamState.BEGIN -> {
                                beamRun.clear()
                                beamRun += vfNote
                            }
                            BeamState.CONTINUE -> beamRun += vfNote
                            BeamState.END -> {
                                beamRun += vfNote
                                if (beamRun.size >= 2) {
                                    beams += VFBeam(beamRun.toList())
                                }
                                beamRun.clear()
                            }
                            BeamState.NONE -> {
                                if (beamRun.size >= 2) {
                                    beams += VFBeam(beamRun.toList())
                                }
                                beamRun.clear()
                            }
                        }
                    }
                }

                if (beamRun.size >= 2) {
                    beams += VFBeam(beamRun.toList())
                    beamRun.clear()
                }

                val vfVoice = VFVoice("${attrs.timeNumerator}/${attrs.timeDenominator}")
                vfVoice.addTickables(vfNotes)
                voices += vfVoice
            }

            RenderedStaff(
                staffNumber = staffNumber,
                resolvedClefType = clefForStaff,
                stave = stave,
                voices = voices,
                beams = beams,
                ties = ties,
                multiMeasureRest = if (attrs.multipleRestCount > 1) {
                    VFMultiMeasureRest(count = attrs.multipleRestCount, staffLineSpacingPx = staffLineSpacingPx)
                } else null
            )
        }

        return RenderedMeasure(
            measureNumber = measure.number,
            staves = renderedStaves
        )
    }

    private fun resolvedClefForStaff(
        attrs: MeasureAttributes,
        staffNumber: Int,
        inferVirtualClefs: Boolean
    ): String {
        if (attrs.clefByStaff.containsKey(staffNumber)) {
            return attrs.clefForStaff(staffNumber)
        }
        if (!inferVirtualClefs) {
            return attrs.clefForStaff(staffNumber)
        }
        return if (staffNumber == 1) "treble" else "bass"
    }

    private fun determineOpeningBoundarySpacingSpaces(
        part: Part,
        staveWidth: Float,
        staffLineSpacingPx: Float,
        showClef: Boolean,
        showKeySig: Boolean,
        showTimeSig: Boolean,
        staffResolver: (NoteOrRest) -> Int
    ): Float {
        val firstMeasure = part.measures.firstOrNull() ?: return 1f
        val attrs = firstMeasure.attributes
        val staffNumber = firstMeasure.notes.firstOrNull()?.let(staffResolver) ?: 1

        val clefWidth = if (showClef) {
            val clef = VFClef(attrs.clefForStaff(staffNumber), "default", null).apply {
                sizePx = staffLineSpacingPx * SMUFL_EM_IN_STAFF_SPACES
            }
            runCatching { clef.widthForStaffSpacing(staffLineSpacingPx) }
                .getOrElse { clef.width }
        } else {
            0f
        }

        val keyWidth = if (showKeySig) {
            val key = VFKeySignature(fifthsToKeySpec(attrs.keyFifths, attrs.keyMode))
            runCatching { key.widthForStaffSpacing(staffLineSpacingPx) }
                .getOrElse { key.accidentalCount * staffLineSpacingPx * 0.8f }
        } else {
            0f
        }

        val timeWidth = if (showTimeSig) {
            val time = VFTimeSignature(attrs.timeSymbol.ifEmpty { "${attrs.timeNumerator}/${attrs.timeDenominator}" }).apply {
                sizePx = staffLineSpacingPx * SMUFL_EM_IN_STAFF_SPACES
            }
            runCatching { time.widthForStaffSpacing(staffLineSpacingPx) }
                .getOrElse { time.width }
        } else {
            0f
        }

        val openingPaddingAtOneScale =
            (staffLineSpacingPx * VFMetrics.STAVE_LEFT_PADDING_SPACES) +
                (if (showClef) VFMetrics.clefPaddingPx(staffLineSpacingPx) else 0f) +
                (if (showKeySig) VFMetrics.keySignaturePaddingPx(staffLineSpacingPx) else 0f) +
                (if (showTimeSig) VFMetrics.timeSignaturePaddingPx(staffLineSpacingPx) else 0f)
        val openingWidthAtOneSpace =
            openingPaddingAtOneScale + clefWidth + keyWidth + timeWidth
        val openingWidthAtHalfSpace =
            (openingPaddingAtOneScale * 0.5f) + clefWidth + keyWidth + timeWidth

        val usableOneSpace = staveWidth - openingWidthAtOneSpace
        val usableHalfSpace = staveWidth - openingWidthAtHalfSpace
        val minNoteField = staffLineSpacingPx * 5f

        return when {
            usableOneSpace >= minNoteField -> 1f
            usableHalfSpace >= minNoteField -> 0.5f
            else -> 0.5f
        }
    }

    private data class ChordGroup(
        val primary: NoteOrRest,
        val chordNotes: List<NoteData> = emptyList()
    )

    private fun buildStaffResolver(part: Part): (NoteOrRest) -> Int {
        val allNotes = part.measures.flatMap { it.notes }
        val hasExplicitStaffTags = allNotes.any { note ->
            when (note) {
                is NoteData -> note.staffExplicit
                is RestData -> note.staffExplicit
            }
        }
        if (hasExplicitStaffTags) {
            return { note -> note.staff }
        }

        // Only infer a virtual grand staff when the MusicXML attributes actually
        // declare multiple clef/staff contexts. Wide single-staff pitch-range
        // exercises (e.g. 01a-Pitches-Pitches.xml) should remain one staff.
        val hasMultiStaffClefDeclarations = part.measures.any { measure ->
            measure.attributes.clefByStaff.keys.any { it > 1 }
        }
        if (!hasMultiStaffClefDeclarations) {
            return { note -> note.staff }
        }

        val pitched = allNotes.filterIsInstance<NoteData>()
        if (pitched.isEmpty()) {
            return { note -> note.staff }
        }

        val steps = pitched.map { absoluteStep(it.pitch) }
        val minStep = steps.minOrNull() ?: return { note -> note.staff }
        val maxStep = steps.maxOrNull() ?: return { note -> note.staff }
        val range = maxStep - minStep
        if (range < INFER_MIN_RANGE_STEPS) {
            return { note -> note.staff }
        }

        val splitStep = (minStep + maxStep) / 2
        return { note ->
            when (note) {
                is NoteData -> {
                    // Use a stable two-staff split across the part range to avoid
                    // per-octave bucket jumps that can split a single measure unexpectedly.
                    if (absoluteStep(note.pitch) <= splitStep) 1 else 2
                }
                is RestData -> 1
            }
        }
    }

    private fun absoluteStep(pitch: Pitch): Int {
        val diatonicStep = when (pitch.step.uppercase()) {
            "C" -> 0
            "D" -> 1
            "E" -> 2
            "F" -> 3
            "G" -> 4
            "A" -> 5
            "B" -> 6
            else -> 0
        }
        return pitch.octave * 7 + diatonicStep
    }

    private fun buildChordGroups(notes: List<NoteOrRest>): List<ChordGroup> {
        val result = mutableListOf<ChordGroup>()
        val pendingChordNotes = mutableListOf<NoteData>()

        for (note in notes) {
            if (note.isChordNote && note is NoteData) {
                pendingChordNotes += note
            } else {
                if (result.isNotEmpty() && pendingChordNotes.isNotEmpty()) {
                    val last = result.removeAt(result.lastIndex)
                    result += last.copy(chordNotes = pendingChordNotes.toList())
                    pendingChordNotes.clear()
                }
                result += ChordGroup(note)
            }
        }

        if (result.isNotEmpty() && pendingChordNotes.isNotEmpty()) {
            val last = result.removeAt(result.lastIndex)
            result += last.copy(chordNotes = pendingChordNotes.toList())
        }

        return result
    }

    private fun buildVFNote(
        group: ChordGroup,
        divisions: Int,
        keyFifths: Int,
        staffLineSpacingPx: Float,
        staffResolver: (NoteOrRest) -> Int,
        measureRestCount: Int? = null
    ): VFStaveNote {
        val primary = group.primary
        val duration = durationToVF(primary.duration, divisions) + if (primary is RestData) "r" else ""

        val keyedNotes = when (primary) {
            is RestData -> emptyList()
            is NoteData -> {
                val chordNotes = group.chordNotes.filter { staffResolver(it) == staffResolver(primary) }
                listOf(primary) + chordNotes
            }
        }

        val keys = when (primary) {
            is RestData -> {
                val explicitRestKey = primary.displayStep?.let { step ->
                    primary.displayOctave?.let { octave -> "${step.lowercase()}/$octave" }
                }
                val defaultRestKey = if (measureRestCount != null) "d/5" else "b/4"
                listOf(explicitRestKey ?: defaultRestKey)
            }
            is NoteData -> {
                keyedNotes.map { pitchToKey(it.pitch, it.accidental, keyFifths) }
            }
        }

        val accidentalDisplayOptions = keyedNotes.map { note ->
            if (note.accidental == null) {
                null
            } else {
                VFAccidental.DisplayOptions(
                    cautionary = note.accidentalCautionary,
                    editorial = note.accidentalEditorial,
                    parenthesized = note.accidentalParenthesized,
                    bracketed = note.accidentalBracketed
                )
            }
        }

        return VFStaveNote(
            VFStaveNoteStruct(
                keys = keys,
                duration = duration,
                glyphFontScale = staffLineSpacingPx * SMUFL_EM_IN_STAFF_SPACES,
                accidentalDisplayOptions = accidentalDisplayOptions,
                measureRestCount = measureRestCount
            )
        )
    }

    private fun isFullMeasureRest(
        rest: RestData,
        groupsInVoice: Int,
        measureDurationDivisions: Int
    ): Boolean {
        if (rest.measureRest == true) return true
        if (groupsInVoice != 1) return false
        if (measureDurationDivisions <= 0) return false
        return rest.duration == measureDurationDivisions
    }

    private fun measureDurationInDivisions(attrs: MeasureAttributes): Int {
        val denominator = attrs.timeDenominator.toLong().coerceAtLeast(1L)
        val numerator = attrs.divisions.toLong() * 4L * attrs.timeNumerator.toLong()
        return ((numerator + (denominator / 2L)) / denominator).toInt().coerceAtLeast(1)
    }

    fun pitchToKey(pitch: Pitch, explicitAccidental: String?, keyFifths: Int): String {
        val acc = explicitAccidental ?: inferredAccidentalForPitch(pitch, keyFifths)
        return "${pitch.step.lowercase()}$acc/${pitch.octave}"
    }

    private fun inferredAccidentalForPitch(pitch: Pitch, keyFifths: Int): String {
        val expectedAlter = expectedAlterForStep(pitch.step, keyFifths).toFloat()
        // If the actual alter matches the expected alter, no accidental needed
        if (Math.abs(pitch.alter - expectedAlter) < 0.01f) return ""
        
        // Map fractional alter values to accidental symbols (supports microtones)
        return when {
            // Microtone accidentals
            Math.abs(pitch.alter - (-1.5f)) < 0.01f -> "db"      // three-quarter-flat
            Math.abs(pitch.alter - (-1.0f)) < 0.01f -> "b"       // flat
            Math.abs(pitch.alter - (-0.5f)) < 0.01f -> "qb"      // quarter-flat / half-flat
            Math.abs(pitch.alter - 0.0f) < 0.01f -> "n"          // natural
            Math.abs(pitch.alter - 0.5f) < 0.01f -> "qs"         // quarter-sharp / half-sharp
            Math.abs(pitch.alter - 1.0f) < 0.01f -> "#"          // sharp
            Math.abs(pitch.alter - 1.5f) < 0.01f -> "#t"         // three-quarter-sharp
            Math.abs(pitch.alter - (-2.0f)) < 0.01f -> "bb"      // double-flat
            Math.abs(pitch.alter - 2.0f) < 0.01f -> "##"         // double-sharp
            else -> ""
        }
    }

    private fun expectedAlterForStep(step: String, keyFifths: Int): Int {
        if (keyFifths == 0) return 0
        val sharpOrder = listOf("F", "C", "G", "D", "A", "E", "B")
        val flatOrder = listOf("B", "E", "A", "D", "G", "C", "F")
        val order = if (keyFifths > 0) sharpOrder else flatOrder
        val direction = if (keyFifths > 0) 1 else -1

        val target = step.uppercase()
        var alter = 0
        repeat(kotlin.math.abs(keyFifths)) { idx ->
            if (order[idx % order.size] == target) {
                alter += direction
            }
        }
        return alter
    }

    fun durationToVF(divisionDuration: Int, divisions: Int): String {
        val quarterUnits = divisionDuration.toDouble() / divisions.toDouble().coerceAtLeast(1.0)
        val candidates = listOf(
            "1" to 4.0,
            "2d" to 3.0,
            "2" to 2.0,
            "4d" to 1.5,
            "4" to 1.0,
            "8d" to 0.75,
            "8" to 0.5,
            "16d" to 0.375,
            "16" to 0.25,
            "32d" to 0.1875,
            "32" to 0.125,
            "64d" to 0.09375,
            "64" to 0.0625,
            "128d" to 0.046875,
            "128" to 0.03125,
            "256d" to 0.0234375,
            "256" to 0.015625,
            "512d" to 0.01171875,
            "512" to 0.0078125,
            "1024d" to 0.005859375,
            "1024" to 0.00390625
        )

        if (quarterUnits <= candidates.last().second) {
            return "1024"
        }

        return candidates.minByOrNull { (_, value) -> kotlin.math.abs(value - quarterUnits) }?.first ?: "1024"
    }

    fun fifthsToKeySpec(fifths: Int, mode: String): String {
        if (kotlin.math.abs(fifths) > 7) {
            return "FIFTHS$fifths"
        }

        val majorKeys = mapOf(
            0 to "C", 1 to "G", 2 to "D", 3 to "A", 4 to "E", 5 to "B", 6 to "F#", 7 to "C#",
            -1 to "F", -2 to "Bb", -3 to "Eb", -4 to "Ab", -5 to "Db", -6 to "Gb", -7 to "Cb"
        )
        val minorKeys = mapOf(
            0 to "Am", 1 to "Em", 2 to "Bm", 3 to "F#m", 4 to "C#m", 5 to "G#m",
            -1 to "Dm", -2 to "Gm", -3 to "Cm", -4 to "Fm", -5 to "Bbm", -6 to "Ebm", -7 to "Abm"
        )
        return if (mode.lowercase() == "minor") minorKeys[fifths] ?: "Am" else majorKeys[fifths] ?: "C"
    }

    private fun mapBarlineStyle(style: String): VFBarlineType {
        return when (style.lowercase()) {
            "light-light" -> VFBarlineType.DOUBLE
            "light-heavy", "heavy" -> VFBarlineType.END
            else -> VFBarlineType.SINGLE
        }
    }
}
