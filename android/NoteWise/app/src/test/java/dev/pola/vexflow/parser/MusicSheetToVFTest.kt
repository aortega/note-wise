package dev.pola.vexflow.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSheetToVFTest {

    private fun sampleMeasure(): Measure {
        return Measure(
            number = 1,
            attributes = MeasureAttributes(
                divisions = 1,
                keyFifths = 0,
                keyMode = "major",
                timeNumerator = 4,
                timeDenominator = 4,
                clefByStaff = mapOf(1 to "treble")
            ),
            notes = listOf(
                NoteData(pitch = Pitch("C", 4), duration = 1, voice = 1),
                NoteData(pitch = Pitch("D", 4), duration = 1, voice = 1),
                RestData(duration = 1, voice = 1),
                NoteData(pitch = Pitch("E", 4), duration = 1, voice = 1)
            )
        )
    }

    @Test
    fun `convert creates one rendered measure with one voice`() {
        val sheet = MusicSheet(
            title = "Unit",
            parts = listOf(Part(id = "P1", name = "Piano", measures = listOf(sampleMeasure())))
        )

        val rendered = MusicSheetToVF.convert(sheet, startX = 20f, startY = 80f, staveWidth = 400f)
        val staff = rendered[0].staves.first()

        assertEquals(1, rendered.size)
        assertEquals(1, rendered[0].staves.size)
        assertEquals(1, staff.voices.size)
        assertEquals(4, staff.voices[0].tickables.size)
        assertEquals("4", staff.voices[0].tickables[0].durationString)
        assertEquals("4r", staff.voices[0].tickables[2].durationString)
        assertEquals(listOf("b/4"), staff.voices[0].tickables[2].keys)
    }

    @Test
    fun `pitched rest uses explicit display position key`() {
        val measure = Measure(
            number = 1,
            attributes = MeasureAttributes(divisions = 1),
            notes = listOf(RestData(duration = 1, voice = 1, displayStep = "E", displayOctave = 4))
        )
        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))

        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)
        val rest = rendered[0].staves.first().voices[0].tickables[0]

        assertEquals(listOf("e/4"), rest.keys)
    }

    @Test
    fun `full measure rest uses line four default and shows count one`() {
        val measure = Measure(
            number = 1,
            attributes = MeasureAttributes(divisions = 512, timeNumerator = 2, timeDenominator = 2),
            notes = listOf(RestData(duration = 2048, voice = 1))
        )
        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))

        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)
        val rest = rendered[0].staves.first().voices[0].tickables[0]

        assertEquals(listOf("d/5"), rest.keys)
        assertEquals(1, rest.measureRestCount)
    }

    @Test
    fun `measure no full rest still uses full measure engraving when duration fills bar`() {
        val measure = Measure(
            number = 1,
            attributes = MeasureAttributes(divisions = 512, timeNumerator = 3, timeDenominator = 2),
            notes = listOf(RestData(duration = 3072, voice = 1, measureRest = false))
        )
        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))

        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)
        val rest = rendered[0].staves.first().voices[0].tickables[0]

        assertEquals(listOf("d/5"), rest.keys)
        assertEquals(1, rest.measureRestCount)
    }

    @Test
    fun `tiny rest durations keep increasing speed tokens`() {
        val measure = Measure(
            number = 1,
            attributes = MeasureAttributes(divisions = 512, timeNumerator = 2, timeDenominator = 2),
            notes = listOf(
                RestData(duration = 32, voice = 1),
                RestData(duration = 16, voice = 1),
                RestData(duration = 8, voice = 1),
                RestData(duration = 4, voice = 1),
                RestData(duration = 2, voice = 1),
                RestData(duration = 2, voice = 1)
            )
        )
        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))

        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)
        val durations = rendered[0].staves.first().voices[0].tickables.map { it.durationString }

        assertEquals(listOf("64r", "128r", "256r", "512r", "1024r", "1024r"), durations)
    }

    @Test
    fun `chord notes collapse into one VFStaveNote with multiple keys`() {
        val measure = Measure(
            number = 1,
            attributes = MeasureAttributes(divisions = 1),
            notes = listOf(
                NoteData(pitch = Pitch("C", 4), duration = 1, voice = 1),
                NoteData(pitch = Pitch("E", 4), duration = 1, voice = 1, isChordNote = true),
                NoteData(pitch = Pitch("G", 4), duration = 1, voice = 1, isChordNote = true)
            )
        )
        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))

        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)
        val note = rendered[0].staves.first().voices[0].tickables[0]

        assertEquals(1, rendered[0].staves.first().voices[0].tickables.size)
        assertEquals(3, note.keys.size)
        assertTrue(note.keys.contains("c/4"))
        assertTrue(note.keys.contains("e/4"))
        assertTrue(note.keys.contains("g/4"))
    }

    @Test
    fun `unchanged measure attributes do not repeat clef key and time signatures`() {
        val attrs = MeasureAttributes(
            divisions = 1,
            keyFifths = 2,
            keyMode = "major",
            timeNumerator = 4,
            timeDenominator = 4,
            clefByStaff = mapOf(1 to "treble", 2 to "bass")
        )

        val measure1 = Measure(
            number = 1,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("C", 5), duration = 4, voice = 1, staff = 1),
                NoteData(pitch = Pitch("C", 3), duration = 4, voice = 1, staff = 2)
            )
        )
        val measure2 = Measure(
            number = 2,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("D", 5), duration = 4, voice = 1, staff = 1),
                NoteData(pitch = Pitch("D", 3), duration = 4, voice = 1, staff = 2)
            )
        )

        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure1, measure2))))
        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)

        val first = rendered[0]
        val second = rendered[1]

        assertEquals(2, first.staves.size)
        assertEquals(2, second.staves.size)

        assertTrue(first.staves[0].stave.clef != null)
        assertTrue(first.staves[0].stave.keySignature != null)
        assertTrue(first.staves[0].stave.timeSignature != null)
        assertTrue(first.staves[1].stave.clef != null)
        assertTrue(first.staves[1].stave.keySignature != null)
        assertTrue(first.staves[1].stave.timeSignature != null)

        assertTrue(second.staves[0].stave.clef == null)
        assertTrue(second.staves[0].stave.keySignature == null)
        assertTrue(second.staves[0].stave.timeSignature == null)
        assertTrue(second.staves[1].stave.clef == null)
        assertTrue(second.staves[1].stave.keySignature == null)
        assertTrue(second.staves[1].stave.timeSignature == null)
    }

    @Test
    fun `missing staff in explicit piano part defaults to treble and leaves bass empty`() {
        val attrs = MeasureAttributes(
            divisions = 1,
            keyFifths = 0,
            keyMode = "major",
            timeNumerator = 4,
            timeDenominator = 4,
            timeSymbol = "C",
            clefByStaff = mapOf(1 to "treble", 2 to "bass")
        )

        val measure0 = Measure(
            number = 0,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("C", 5), duration = 1, voice = 1, staff = 1, staffExplicit = true),
                RestData(duration = 1, voice = 2, staff = 2, staffExplicit = true)
            )
        )
        val measure1 = Measure(
            number = 1,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("A", 4), duration = 4, voice = 1, staff = 1, staffExplicit = true),
                NoteData(pitch = Pitch("E", 3), duration = 4, voice = 2)
            )
        )

        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure0, measure1))))
        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)

        assertEquals(listOf(1, 2), rendered[0].staves.map { it.staffNumber })
        assertEquals(listOf(1, 2), rendered[1].staves.map { it.staffNumber })
        assertTrue(rendered[0].staves.all { it.stave.timeSignature != null })

        val trebleMeasure1 = rendered[1].staves.first { it.staffNumber == 1 }
        val bassMeasure1 = rendered[1].staves.first { it.staffNumber == 2 }

        assertEquals(2, trebleMeasure1.voices.size)
        assertTrue(
            trebleMeasure1.voices.any { voice -> voice.tickables.any { it.keys == listOf("e/3") } }
        )

        assertTrue(bassMeasure1.voices.isEmpty())
    }

    @Test
    fun `declared grand staff keeps both staves even when one staff is empty`() {
        val attrs = MeasureAttributes(
            divisions = 1,
            timeNumerator = 4,
            timeDenominator = 4,
            clefByStaff = mapOf(1 to "treble", 2 to "bass")
        )
        val measure = Measure(
            number = 1,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("C", 5), duration = 4, voice = 1, staff = 1, staffExplicit = true)
            )
        )

        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))
        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)

        assertEquals(listOf(1, 2), rendered[0].staves.map { it.staffNumber })
        val emptyCompanion = rendered[0].staves.first { it.staffNumber == 2 }
        assertTrue(emptyCompanion.voices.isEmpty())
    }

    @Test
    fun `cross-staff chord notes are not merged into primary staff noteheads`() {
        val measure = Measure(
            number = 1,
            attributes = MeasureAttributes(divisions = 1, clefByStaff = mapOf(1 to "treble", 2 to "bass")),
            notes = listOf(
                NoteData(pitch = Pitch("C", 4), duration = 1, voice = 1, staff = 1, staffExplicit = true),
                NoteData(pitch = Pitch("E", 3), duration = 1, voice = 1, staff = 2, staffExplicit = true, isChordNote = true),
                NoteData(pitch = Pitch("G", 3), duration = 1, voice = 1, staff = 2, staffExplicit = true)
            )
        )
        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))

        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 300f)
        assertEquals(1, rendered.size)
        assertEquals(2, rendered[0].staves.size)

        val trebleFirstNote = rendered[0].staves.first { it.staffNumber == 1 }.voices[0].tickables[0]
        val bassFirstNote = rendered[0].staves.first { it.staffNumber == 2 }.voices[0].tickables[0]

        assertEquals(listOf("c/4"), trebleFirstNote.keys)
        assertEquals(listOf("g/3"), bassFirstNote.keys)
    }

    @Test
    fun `wide pitch range without explicit staff stays single staff`() {
        val measure = Measure(
            number = 1,
            attributes = MeasureAttributes(
                divisions = 1,
                timeNumerator = 4,
                timeDenominator = 4,
                clefByStaff = mapOf(1 to "treble")
            ),
            notes = listOf(
                NoteData(pitch = Pitch("G", 2), duration = 1, voice = 1),
                NoteData(pitch = Pitch("C", 3), duration = 1, voice = 1),
                NoteData(pitch = Pitch("C", 6), duration = 1, voice = 1),
                NoteData(pitch = Pitch("C", 7), duration = 1, voice = 1)
            )
        )
        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure))))

        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 420f)

        assertEquals(1, rendered.size)
        assertEquals(1, rendered[0].staves.size)
        assertEquals(1, rendered[0].staves[0].staffNumber)
    }

    @Test
    fun `virtual staff segment starts show clef`() {
        val attrs = MeasureAttributes(
            divisions = 1,
            timeNumerator = 4,
            timeDenominator = 4,
            clefByStaff = mapOf(1 to "treble", 2 to "bass")
        )
        val measure1 = Measure(
            number = 1,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("C", 7), duration = 4, voice = 1)
            )
        )
        val measure2 = Measure(
            number = 2,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("G", 2), duration = 4, voice = 1)
            )
        )
        val measure3 = Measure(
            number = 3,
            attributes = attrs,
            notes = listOf(
                NoteData(pitch = Pitch("B", 6), duration = 4, voice = 1)
            )
        )

        val sheet = MusicSheet(parts = listOf(Part(id = "P1", measures = listOf(measure1, measure2, measure3))))
        val rendered = MusicSheetToVF.convert(sheet, startX = 0f, startY = 0f, staveWidth = 420f)

        assertEquals(3, rendered.size)
        assertTrue(rendered[0].staves.any { it.stave.clef != null })
        assertTrue(rendered[1].staves.any { it.stave.clef != null })
        assertTrue(rendered[2].staves.any { it.stave.clef != null })
    }
}
