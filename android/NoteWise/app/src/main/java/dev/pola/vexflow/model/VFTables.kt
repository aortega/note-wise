package dev.pola.vexflow.model

/**
 * SMuFL Unicode codepoints for Bravura glyphs used in rendering.
 */
object VFTables {

    const val GLYPH_G_CLEF: Int = 0xE050
    const val GLYPH_F_CLEF: Int = 0xE062
    const val GLYPH_C_CLEF: Int = 0xE05C
    const val GLYPH_PERCUSSION_CLEF: Int = 0xE069

    const val GLYPH_NOTE_HEAD_WHOLE: Int = 0xE0A2
    const val GLYPH_NOTE_HEAD_HALF: Int = 0xE0A3
    const val GLYPH_NOTE_HEAD_QUARTER: Int = 0xE0A4
    const val GLYPH_NOTE_HEAD_DOUBLE_WHOLE: Int = 0xE0A1

    const val GLYPH_ACCIDENTAL_SHARP: Int = 0xE262
    const val GLYPH_ACCIDENTAL_FLAT: Int = 0xE260
    const val GLYPH_ACCIDENTAL_NATURAL: Int = 0xE261
    const val GLYPH_ACCIDENTAL_DOUBLE_SHARP: Int = 0xE263
    const val GLYPH_ACCIDENTAL_DOUBLE_FLAT: Int = 0xE264
    const val GLYPH_ACCIDENTAL_PARENS_LEFT: Int = 0xE26A
    const val GLYPH_ACCIDENTAL_PARENS_RIGHT: Int = 0xE26B
    const val GLYPH_ACCIDENTAL_BRACKET_LEFT: Int = 0xE26C
    const val GLYPH_ACCIDENTAL_BRACKET_RIGHT: Int = 0xE26D
    // Microtone accidentals (SMuFL)
    // Use non-arrow 24-EDO Stein/Zimmermann glyphs for quarter-tone accidentals.
    const val GLYPH_ACCIDENTAL_QUARTER_FLAT: Int = 0xE280
    const val GLYPH_ACCIDENTAL_QUARTER_SHARP: Int = 0xE282
    const val GLYPH_ACCIDENTAL_HALF_SHARP: Int = 0xE282
    const val GLYPH_ACCIDENTAL_THREE_QUARTER_FLAT: Int = 0xE281
    const val GLYPH_ACCIDENTAL_THREE_QUARTER_SHARP: Int = 0xE283

    const val GLYPH_REST_WHOLE: Int = 0xE4E3
    const val GLYPH_REST_HALF: Int = 0xE4E4
    const val GLYPH_REST_QUARTER: Int = 0xE4E5
    const val GLYPH_REST_8TH: Int = 0xE4E6
    const val GLYPH_REST_16TH: Int = 0xE4E7
    const val GLYPH_REST_32ND: Int = 0xE4E8
    const val GLYPH_REST_64TH: Int = 0xE4E9
    const val GLYPH_REST_128TH: Int = 0xE4EA
    const val GLYPH_REST_256TH: Int = 0xE4EB
    const val GLYPH_REST_512TH: Int = 0xE4EC
    const val GLYPH_REST_1024TH: Int = 0xE4ED

    const val GLYPH_FLAG_8TH_UP: Int = 0xE240
    const val GLYPH_FLAG_8TH_DOWN: Int = 0xE241
    const val GLYPH_FLAG_16TH_UP: Int = 0xE242
    const val GLYPH_FLAG_16TH_DOWN: Int = 0xE243
    const val GLYPH_FLAG_32ND_UP: Int = 0xE244
    const val GLYPH_FLAG_32ND_DOWN: Int = 0xE245

    const val GLYPH_TIME_SIG_0: Int = 0xE080
    const val GLYPH_TIME_SIG_1: Int = 0xE081
    const val GLYPH_TIME_SIG_2: Int = 0xE082
    const val GLYPH_TIME_SIG_3: Int = 0xE083
    const val GLYPH_TIME_SIG_4: Int = 0xE084
    const val GLYPH_TIME_SIG_5: Int = 0xE085
    const val GLYPH_TIME_SIG_6: Int = 0xE086
    const val GLYPH_TIME_SIG_7: Int = 0xE087
    const val GLYPH_TIME_SIG_8: Int = 0xE088
    const val GLYPH_TIME_SIG_9: Int = 0xE089
    const val GLYPH_TIME_SIG_COMMON: Int = 0xE08A
    const val GLYPH_TIME_SIG_CUT: Int = 0xE08B

    const val GLYPH_AUGMENTATION_DOT: Int = 0xE1E7

    fun codepointToString(codepoint: Int): String = String(Character.toChars(codepoint))

    fun timeSigDigit(digit: Int): Int {
        require(digit in 0..9) { "Digit must be 0-9" }
        return GLYPH_TIME_SIG_0 + digit
    }
}
