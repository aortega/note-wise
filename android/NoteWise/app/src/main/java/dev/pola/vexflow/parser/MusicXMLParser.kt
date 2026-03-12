package dev.pola.vexflow.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

class MusicXMLParseException(message: String) : Exception(message)

/**
 * Parses MusicXML (.xml or .mxl) files into a MusicSheet model.
 */
class MusicXMLParser {

    fun parse(stream: InputStream): MusicSheet {
        val xmlStream = decompressIfNeeded(stream)
        return parseXml(xmlStream)
    }

    private fun decompressIfNeeded(stream: InputStream): InputStream {
        val buffered = stream.buffered()
        buffered.mark(2)
        val header = ByteArray(2)
        buffered.read(header)
        buffered.reset()
        return if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
            extractRootfileFromMxl(buffered)
        } else {
            buffered
        }
    }

    private fun extractRootfileFromMxl(stream: InputStream): InputStream {
        val zip = ZipInputStream(stream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".xml") && !entry.name.startsWith("META-INF")) {
                return zip.readBytes().inputStream()
            }
            entry = zip.nextEntry
        }
        throw MusicXMLParseException("No MusicXML content found in MXL archive")
    }

    private fun parseXml(stream: InputStream): MusicSheet {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var title = ""
        var composer = ""
        val partNames = linkedMapOf<String, String>()
        val parts = mutableMapOf<String, MutableList<Measure>>()
        var currentScorePartId: String? = null

        var event = parser.next()
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "work-title" -> title = parser.nextText()
                    "creator" -> {
                        if (parser.getAttributeValue(null, "type") == "composer") {
                            composer = parser.nextText()
                        }
                    }
                    "score-part" -> {
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        currentScorePartId = id
                        partNames[id] = ""
                    }
                    "part-name" -> {
                        val name = parser.nextText()
                        val id = currentScorePartId
                        if (id != null) {
                            partNames[id] = name
                        }
                    }
                    "part" -> {
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        parts[id] = parsePart(parser)
                        partNames.putIfAbsent(id, "")
                    }
                }
            }
            event = parser.next()
        }

        val sheetParts = partNames.keys.map { id ->
            Part(id = id, name = partNames[id] ?: "", measures = parts[id] ?: emptyList())
        }

        return MusicSheet(title = title, composer = composer, parts = sheetParts)
    }

    private fun parsePart(parser: XmlPullParser): MutableList<Measure> {
        val measures = mutableListOf<Measure>()
        var attrs = MeasureAttributes()

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "part")) {
            if (event == XmlPullParser.START_TAG && parser.name == "measure") {
                val number = parser.getAttributeValue(null, "number")?.toIntOrNull() ?: (measures.size + 1)
                val parsed = parseMeasure(parser, number, attrs)
                measures.add(parsed.first)
                attrs = parsed.second
            }
            event = parser.next()
        }

        return measures
    }

    private fun parseMeasure(
        parser: XmlPullParser,
        number: Int,
        previousAttributes: MeasureAttributes
    ): Pair<Measure, MeasureAttributes> {
        var attributes = previousAttributes.copy(multipleRestCount = 0)
        val notes = mutableListOf<NoteOrRest>()
        val tempoMarks = mutableListOf<TempoMark>()
        var barlineLeft = "regular"
        var barlineRight = "regular"

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "measure")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "attributes" -> attributes = parseAttributes(parser, attributes)
                    "note" -> notes.add(parseNote(parser))
                    "direction" -> parseDirection(parser)?.let { tempoMarks.add(it) }
                    "barline" -> {
                        val location = parser.getAttributeValue(null, "location") ?: "right"
                        val style = parseBarlineStyle(parser)
                        if (location == "left") barlineLeft = style else barlineRight = style
                    }
                }
            }
            event = parser.next()
        }

        return Measure(
            number = number,
            attributes = attributes,
            notes = notes,
            tempoMarks = tempoMarks,
            barlineLeft = barlineLeft,
            barlineRight = barlineRight
        ) to attributes
    }

    private fun parseAttributes(parser: XmlPullParser, previous: MeasureAttributes): MeasureAttributes {
        var divisions = previous.divisions
        var keyFifths = previous.keyFifths
        var keyMode = previous.keyMode
        var timeNumerator = previous.timeNumerator
        var timeDenominator = previous.timeDenominator
        var timeSymbol = previous.timeSymbol
        val clefByStaff = previous.clefByStaff.toMutableMap()
        var multipleRestCount = 0

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "attributes")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "divisions" -> divisions = parser.nextText().trim().toIntOrNull() ?: divisions
                    "fifths" -> keyFifths = parser.nextText().trim().toIntOrNull() ?: keyFifths
                    "mode" -> keyMode = parser.nextText().trim().ifEmpty { keyMode }
                    "time" -> {
                        val symbol = parser.getAttributeValue(null, "symbol") ?: ""
                        timeSymbol = when (symbol.lowercase()) {
                            "common" -> "C"
                            "cut" -> "C|"
                            else -> ""
                        }
                        // child <beats> and <beat-type> are processed by the outer loop
                    }
                    "beats" -> timeNumerator = parser.nextText().trim().toIntOrNull() ?: timeNumerator
                    "beat-type" -> timeDenominator = parser.nextText().trim().toIntOrNull() ?: timeDenominator
                    "clef" -> {
                        val number = parser.getAttributeValue(null, "number")?.toIntOrNull() ?: 1
                        val parsedClef = parseClef(parser)
                        clefByStaff[number] = parsedClef
                    }
                    "measure-style" -> {
                        var msEvent = parser.next()
                        while (!(msEvent == XmlPullParser.END_TAG && parser.name == "measure-style")) {
                            if (msEvent == XmlPullParser.START_TAG && parser.name == "multiple-rest") {
                                multipleRestCount = parser.nextText().trim().toIntOrNull() ?: 0
                            }
                            msEvent = parser.next()
                        }
                    }
                }
            }
            event = parser.next()
        }

        // Validate the visual symbol against actual beats: "common" = 4/4, "cut" = 2/2.
        // If the symbol doesn't match the actual time, fall back to numeric display.
        if (timeSymbol == "C" && !(timeNumerator == 4 && timeDenominator == 4)) timeSymbol = ""
        if (timeSymbol == "C|" && !(timeNumerator == 2 && timeDenominator == 2)) timeSymbol = ""

        return MeasureAttributes(
            divisions = divisions,
            keyFifths = keyFifths,
            keyMode = keyMode,
            timeNumerator = timeNumerator,
            timeDenominator = timeDenominator,
            timeSymbol = timeSymbol,
            clefByStaff = clefByStaff,
            multipleRestCount = multipleRestCount
        )
    }

    private fun parseClef(parser: XmlPullParser): String {
        var clef = "treble"
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "clef")) {
            if (event == XmlPullParser.START_TAG && parser.name == "sign") {
                clef = when (parser.nextText().trim().uppercase()) {
                    "G" -> "treble"
                    "F" -> "bass"
                    "C" -> "alto"
                    else -> clef
                }
            }
            event = parser.next()
        }
        return clef
    }

    private fun parseNote(parser: XmlPullParser): NoteOrRest {
        var step = "C"
        var octave = 4
        var alter = 0f
        var duration = 1
        var voice = 1
        var staff = 1
        var staffExplicit = false
        var isRest = false
        var isChord = false
        var tieStart = false
        var tieEnd = false
        var slurStart = false
        var slurEnd = false
        var beamState = BeamState.NONE
        var accidental: String? = null
        var accidentalCautionary = false
        var accidentalEditorial = false
        var accidentalParenthesized = false
        var accidentalBracketed = false
        var restMeasure: Boolean? = null
        var restDisplayStep: String? = null
        var restDisplayOctave: Int? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "note")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "step" -> step = parser.nextText().trim()
                    "octave" -> octave = parser.nextText().trim().toIntOrNull() ?: 4
                    "display-step" -> restDisplayStep = parser.nextText().trim().uppercase().ifEmpty { null }
                    "display-octave" -> restDisplayOctave = parser.nextText().trim().toIntOrNull()
                    "alter" -> alter = parser.nextText().trim().toFloatOrNull() ?: 0f
                    "duration" -> duration = parser.nextText().trim().toIntOrNull() ?: 1
                    "voice" -> voice = parser.nextText().trim().toIntOrNull() ?: 1
                    "staff" -> {
                        staff = parser.nextText().trim().toIntOrNull() ?: 1
                        staffExplicit = true
                    }
                    "rest" -> {
                        isRest = true
                        restMeasure = parseYesNo(parser.getAttributeValue(null, "measure"))
                    }
                    "chord" -> isChord = true
                    "tie" -> {
                        when (parser.getAttributeValue(null, "type")) {
                            "start" -> tieStart = true
                            "stop" -> tieEnd = true
                        }
                    }
                    "beam" -> {
                        if (parser.getAttributeValue(null, "number") == "1") {
                            beamState = when (parser.nextText().trim()) {
                                "begin" -> BeamState.BEGIN
                                "continue" -> BeamState.CONTINUE
                                "end" -> BeamState.END
                                else -> BeamState.NONE
                            }
                        }
                    }
                    "accidental" -> {
                        val cautionaryAttr = parser.getAttributeValue(null, "cautionary")
                        val editorialAttr = parser.getAttributeValue(null, "editorial")
                        val parenthesesAttr =
                            parser.getAttributeValue(null, "parentheses")
                                ?: parser.getAttributeValue(null, "parenthesized")
                        val bracketAttr = parser.getAttributeValue(null, "bracket")

                        accidentalCautionary = parseYesNo(cautionaryAttr) == true
                        accidentalEditorial = parseYesNo(editorialAttr) == true
                        val explicitParenthesized = parseYesNo(parenthesesAttr)
                        val explicitBracketed = parseYesNo(bracketAttr)
                        // Default behavior: cautionary -> parentheses, editorial -> brackets
                        accidentalBracketed = explicitBracketed ?: accidentalEditorial
                        // Brackets suppress parentheses: only render parentheses if not bracketed
                        accidentalParenthesized = if (accidentalBracketed) false else (explicitParenthesized ?: accidentalCautionary)

                        accidental = when (parser.nextText().trim()) {
                            "sharp" -> "#"
                            "flat" -> "b"
                            "natural" -> "n"
                            "double-sharp" -> "##"
                            "flat-flat" -> "bb"
                            "quarter-flat" -> "qb"
                            "quarter-sharp" -> "qs"
                            "three-quarters-flat" -> "db"
                            "three-quarters-sharp" -> "#t"
                            else -> null
                        }
                    }
                    "slur" -> {
                        when (parser.getAttributeValue(null, "type")) {
                            "start" -> slurStart = true
                            "stop" -> slurEnd = true
                        }
                    }
                }
            }
            event = parser.next()
        }

        return if (isRest) {
            RestData(
                duration = duration,
                voice = voice,
                staff = staff,
                staffExplicit = staffExplicit,
                isChordNote = isChord,
                measureRest = restMeasure,
                displayStep = restDisplayStep,
                displayOctave = restDisplayOctave
            )
        } else {
            NoteData(
                pitch = Pitch(step = step, octave = octave, alter = alter),
                duration = duration,
                voice = voice,
                staff = staff,
                staffExplicit = staffExplicit,
                isChordNote = isChord,
                tieStart = tieStart,
                tieEnd = tieEnd,
                slurStart = slurStart,
                slurEnd = slurEnd,
                beamState = beamState,
                accidental = accidental,
                accidentalCautionary = accidentalCautionary,
                accidentalEditorial = accidentalEditorial,
                accidentalParenthesized = accidentalParenthesized,
                accidentalBracketed = accidentalBracketed
            )
        }
    }

    private fun parseYesNo(raw: String?): Boolean? {
        return when (raw?.trim()?.lowercase()) {
            "yes" -> true
            "no" -> false
            else -> null
        }
    }

    private fun parseDirection(parser: XmlPullParser): TempoMark? {
        var bpm: Float? = null
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "direction")) {
            if (event == XmlPullParser.START_TAG && parser.name == "sound") {
                bpm = parser.getAttributeValue(null, "tempo")?.toFloatOrNull()
            }
            event = parser.next()
        }
        return bpm?.let { TempoMark(bpm = it) }
    }

    private fun parseBarlineStyle(parser: XmlPullParser): String {
        var style = "regular"
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "barline")) {
            if (event == XmlPullParser.START_TAG && parser.name == "bar-style") {
                style = parser.nextText().trim()
            }
            event = parser.next()
        }
        return style
    }
}
