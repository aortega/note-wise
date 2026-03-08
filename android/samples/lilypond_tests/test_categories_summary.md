# LilyPond MusicXML Test Categories Summary

## 

### 01a-Pitches-Pitches.xml
**Description:** All pitches from G to c’’’’ in ascending steps. First without
    accidentals, then with a sharp, and then with a flat accidental.
**XML File:** 01a-Pitches-Pitches.xml
**Image:** 

### 01b-Pitches-Intervals.xml
**Description:** All pitch intervals in ascending jump size.
**XML File:** 01b-Pitches-Intervals.xml
**Image:** 

### 01c-Pitches-NoVoiceElement.xml
**Description:** The <voice> element of notes is optional in MusicXML. Here, there
    is one note with lyrics, but without a voice assigned.
**XML File:** 01c-Pitches-NoVoiceElement.xml
**Image:** 

### 01d-Pitches-Microtones.xml
**Description:** Some microtones: c flat-and-a-half, d half-flat, e half-sharp,
    f sharp-and-a-half; once in the lower and once in the upper region of the
    staff.
**XML File:** 01d-Pitches-Microtones.xml
**Image:** 

### 01e-Pitches-ParenthesizedAccidentals.xml
**Description:** Accidentals have the attributes ‘cautionary’, ‘editorial’,
    ‘parenthesized’, and ‘bracketed’. The first two measures each have a
    cautionary accidental, an editorial, a cautionary with parentheses off, and
    an editorial and cautionary accidental. The next two measures each have a
    normal accidental, a bracketed, a parenthesized, and a bracketed and
    parenthesized accidental.
**XML File:** 01e-Pitches-ParenthesizedAccidentals.xml
**Image:** 

### 01f-Pitches-ParenthesizedMicrotoneAccidentals.xml
**Description:** Microtone accidentals can be cautionary or editorial. Each measure
    has a normal accidental, an editorial, a cautionary, and an editiorial and
    cautionary accidental.
**XML File:** 01f-Pitches-ParenthesizedMicrotoneAccidentals.xml
**Image:** 

### 02a-Rests-Durations.xml
**Description:** All different rest lengths: A two-bar multi-measure rest, a whole
    rest, a half rest, etc., until a 1024th-rest, then the same with dotted
    durations. The last bar is a full-measure rest with the attribute
    ‘measure="no"’.
**XML File:** 02a-Rests-Durations.xml
**Image:** 

### 02b-Rests-PitchedRests.xml
**Description:** Rests can have explicit pitches to position them vertically. In the
    first bar, the first rest has no explicit pitch and should use the default
    position, while the remaining rests are explicitly positioned at pitches E4,
    F5, A3, and C6. The second bar holds a full-measure rest at pitch G4 (within
    an alto clef).
**XML File:** 02b-Rests-PitchedRests.xml
**Image:** 

### 02c-Rests-MultiMeasureRests.xml
**Description:** Five multi-measure rests: 3 measures, 15 measures, 1 measure,
    12 measures, and finally 3 measures with the ‘use-symbols’ attribute set.
**XML File:** 02c-Rests-MultiMeasureRests.xml
**Image:** 

### 02d-Rests-Multimeasure-TimeSignatures.xml
**Description:** Multi-measure rests should always be converted into durations that
    are multiples of the time signature.
**XML File:** 02d-Rests-Multimeasure-TimeSignatures.xml
**Image:** 

### 02e-Rests-NoType.xml
**Description:** In some cases, a <rest> element misses the <type> child (this
    happens, for example, with voices in Finale, where you don’t manually insert
    a rest).
**XML File:** 02e-Rests-NoType.xml
**Image:** 

### 03a-Rhythm-Durations.xml
**Description:** All note durations, from long, brevis, whole, etc., until 1024th.
    First with their plain values, then dotted, and finally double-dotted.
**XML File:** 03a-Rhythm-Durations.xml
**Image:** 

### 03b-Rhythm-Backup.xml
**Description:** Two voices with a <backup> element that does not jump to the
    beginning of the measure for voice 2 but to the second beat. Voice 1 covers
    beats 1 and 2, and voice 2 covers beats 2 and 3. There is no rest or note
    for uncovered beats.
**XML File:** 03b-Rhythm-Backup.xml
**Image:** 

### 03c-Rhythm-DivisionChange.xml
**Description:** Although uncommon, the divisions of a quarter note can change
    somewhere in the middle of a MusicXML file. Here, the first half measure
    uses a division of 1, which then changes to 8 in the middle of the first
    measure and to 38 in the middle of the second measure.
**XML File:** 03c-Rhythm-DivisionChange.xml
**Image:** 

### 03d-Rhythm-DottedDurations-Factors.xml
**Description:** Several durations can be written with dots. For multi-measure rests
    it is also possible to have durations that cannot be expressed with dotted
    notes (like 5/8).
**XML File:** 03d-Rhythm-DottedDurations-Factors.xml
**Image:** 

### 03e-Rhythm-No-Divisions.xml
**Description:** No <divisions> element.
**XML File:** 03e-Rhythm-No-Divisions.xml
**Image:** 

### 03f-Rhythm-Forward.xml
**Description:** A voice with two <forward> elements, putting the first note on the
    third beat and the second note on the last 16th of the measure. There are no
    visible rests.
**XML File:** 03f-Rhythm-Forward.xml
**Image:** 

### 11a-TimeSignatures.xml
**Description:** Various time signatures: 2/2 (alla breve), 4/4 (C), 2/2, 3/2, 2/4,
    3/4, 4/4, 5/4, 3/8, 6/8, 12/8.
**XML File:** 11a-TimeSignatures.xml
**Image:** 

### 11b-TimeSignatures-NoTime.xml
**Description:** A score without a time signature at the beginning (but with <key>
    and <clef> elements). The second bar sets a 2/2 time with
    ‘print-object="no"’ for both staves, and the third bar sets a 4/4 time
    with ‘print-object="no"’ only for the lower staff.
**XML File:** 11b-TimeSignatures-NoTime.xml
**Image:** 

### 11c-TimeSignatures-CompoundSimple.xml
**Description:** Compound time signatures with the same denominator: (3+2)/8 and
    (5+3+1)/4.
**XML File:** 11c-TimeSignatures-CompoundSimple.xml
**Image:** 

### 11d-TimeSignatures-CompoundMultiple.xml
**Description:** Compound time signatures with separate fractions displayed:
    3/8+2/8+3/4 and 5/2+1/8.
**XML File:** 11d-TimeSignatures-CompoundMultiple.xml
**Image:** 

### 11e-TimeSignatures-CompoundMixed.xml
**Description:** Compound time signatures of mixed type: (3+2)/8+3/4.
**XML File:** 11e-TimeSignatures-CompoundMixed.xml
**Image:** 

### 11f-TimeSignatures-SymbolMeaning.xml
**Description:** A time signature of 3/8 with the ‘symbol="cut"’ attribute and two
    ‘symbol="single-number"’ attributes with compound time signatures. Shall
    ‘symbol’ be ignored in this case?
**XML File:** 11f-TimeSignatures-SymbolMeaning.xml
**Image:** 

### 11g-TimeSignatures-SingleNumber.xml
**Description:** A time signature displayed as a single number.
**XML File:** 11g-TimeSignatures-SingleNumber.xml
**Image:** 

### 11h-TimeSignatures-SenzaMisura.xml
**Description:** A <senza-misura> time signature. The first bar has three eighth
    notes, the second bar contains a full-measure rest (indicated by setting
    ‘measure="yes"’ for the <rest> element) with a length of a dotted half
    note, and the third bar contains four eighth notes.
**XML File:** 11h-TimeSignatures-SenzaMisura.xml
**Image:** 

### 12a-Clefs.xml
**Description:** Various clefs: G, C, F, percussion, TAB, and ‘none’ (in
    measure 16). Some clefs are also shown with transposition and on other staff
    lines than their default; the displayed note is always C4.
**XML File:** 12a-Clefs.xml
**Image:** 

### 12b-Clefs-NoKeyOrClef.xml
**Description:** A score without a <key> or <clef> element (but with <time>). The
    default (4/4 in treble clef) should be used.
**XML File:** 12b-Clefs-NoKeyOrClef.xml
**Image:** 

### 13a-KeySignatures.xml
**Description:** Various key signatures: from 11 flats to 11 sharps. Each signature
    is shown twice, with one measure in major and the other measure in minor.
**XML File:** 13a-KeySignatures.xml
**Image:** 

### 13b-KeySignatures-ChurchModes.xml
**Description:** All different modes: ‘major’, ‘minor’, ‘ionian’, ‘dorian’,
    ‘phrygian’, ‘lydian’, ‘mixolydian’, ‘aeolian’, ‘locrian’, and ‘none’. All
    modes are given with two sharps.
**XML File:** 13b-KeySignatures-ChurchModes.xml
**Image:** 

### 13c-KeySignatures-NonTraditional.xml
**Description:** Non-traditional key signatures, where each alteration is separately
    given. The first signature is [f sharp, a flat, b flat]. The second one is
    [c flat-flat, g sharp-sharp, d flat, b sharp, f natural], with explicitly
    selected octaves for each alteration.
**XML File:** 13c-KeySignatures-NonTraditional.xml
**Image:** 

### 13d-KeySignatures-Microtones.xml
**Description:** Non-traditional key signatures with microtone alterations:
    [g flat-and-a-half, a flat, b half-flat, c natural, d half-sharp, e sharp,
    f sharp-and-a-half].
**XML File:** 13d-KeySignatures-Microtones.xml
**Image:** 

### 13e-KeySignatures-Cancel.xml
**Description:** Tests of key signature cancellation: in measure 2 it is positioned
    at the default location (which usually means that the cancellation comes
    immediately after the bar line), in measure 3 it uses the attribute
    ‘location="right"’ (i.e., the cancellation comes after the new key
    signature), in measure 4 it uses ‘location="before-barline"’, and in
    measure 5 it uses the attribute ‘location="left"’ (the cancellation comes
    before the new key signature).
**XML File:** 13e-KeySignatures-Cancel.xml
**Image:** 

### 13f-KeySignatures-Visible.xml
**Description:** Test the ‘print-object’ attribute of key signatures. The signature
    at the beginning of the second bar is a flat major and should be invisible;
    the following notes d flat, e flat, a flat, and b flat shouldn’t have a flat
    accidental.
**XML File:** 13f-KeySignatures-Visible.xml
**Image:** 

### 14a-StaffDetails-LineChanges.xml
**Description:** Testing staff line configurations and pitched notes. The number of
    staff lines can be modified by using the <staff-lines> child of the
    <staff-details> attribute. This can happen globally (the first staff has one
    line globally) or during the part at the beginning of a measure and even
    inside a measure (the second part has five lines initially, four at the
    beginning of the second measure, and three starting in the middle of the
    third measure). The fourth measure in the lower staff has five lines again
    but uses the ‘print-object="no"’ attribute in the <line-detail> element to
    suppress the second and fourth staff line.
**XML File:** 14a-StaffDetails-LineChanges.xml
**Image:** 

### 21a-Chord-Basic.xml
**Description:** A chord consisting of two quarter notes followed by a quarter rest.
**XML File:** 21a-Chord-Basic.xml
**Image:** 

### 21b-Chords-TwoNotes.xml
**Description:** Some subsequent (identical) two-note chords. In the second bar, the
    chords are tied (top note, bottom note, both notes). In the third bar, the
    unnatural directions of the ties are enforced with the ‘placement’ attribute
    (between chords one and two) and the ‘orientation’ attribute (between chords
    three and four)
**XML File:** 21b-Chords-TwoNotes.xml
**Image:** 

### 21c-Chords-ThreeNotesDuration.xml
**Description:** Some three-note chords, with various durations.
**XML File:** 21c-Chords-ThreeNotesDuration.xml
**Image:** 

### 21d-Chords-SchubertStabatMater.xml
**Description:** There are chords in the second measure, after several ornaments in
    the first measure and a ‘p’ at the beginning of the second measure.
**XML File:** 21d-Chords-SchubertStabatMater.xml
**Image:** 

### 21e-Chords-PickupMeasures.xml
**Description:** A test for proper chord detection after a pickup measure (i.e., the
    first beat of measure 1 is not aligned with a multiple of the time
    signature).
**XML File:** 21e-Chords-PickupMeasures.xml
**Image:** 

### 21f-Chord-ElementInBetween.xml
**Description:** Between the individual notes of a chord there can be <direction>
    elements, which already belong to the next <note> element after the current
    one. The segno and the piano sign should be attached to the rest after the
    chord.
**XML File:** 21f-Chord-ElementInBetween.xml
**Image:** 

### 21g-Chords-Tremolos.xml
**Description:** Different tremolos on different chord notes. The tremolo on the
    last chord is of type ‘unmeasured’.
**XML File:** 21g-Chords-Tremolos.xml
**Image:** 

### 21h-Chord-Accidentals.xml
**Description:** A chord with normal, cautionary, and editorial accidentals (from
    bottom to top).
**XML File:** 21h-Chord-Accidentals.xml
**Image:** 

### 22a-Noteheads.xml
**Description:** Different note styles, using the <notehead> element. First, each
    note head style is printed with four quarter notes, two with filled heads,
    two with unfilled heads, where first the stem is up and then the stem is
    down.
**XML File:** 22a-Noteheads.xml
**Image:** 

### 22b-Staff-Notestyles.xml
**Description:** Staff-connected note styles: slash notation, hidden notes (with and
    without hidden staff lines).
**XML File:** 22b-Staff-Notestyles.xml
**Image:** 

### 22c-Noteheads-Chords.xml
**Description:** Different note styles for individual notes inside a chord, using
    the <notehead> element.
**XML File:** 22c-Noteheads-Chords.xml
**Image:** 

### 22d-Parenthesized-Noteheads.xml
**Description:** Parenthesized note heads. A normal parenthesized note, a
    parenthesized note with an ‘x’ note head, a three-note chord with the middle
    note parenthesized, a three-note chord with all notes parenthesized, a
    normal quarter rest in parentheses, and a pitched quarter rest in
    parentheses.
**XML File:** 22d-Parenthesized-Noteheads.xml
**Image:** 

### 23a-Tuplets.xml
**Description:** Some tuplets (3:2, 3:2, 3:2, 4:2, 4:1, 7:3, 6:2) with the default
    tuplet bracket displaying the number of actual notes played. The second
    tuplet does not have the ‘number’ attribute set.
**XML File:** 23a-Tuplets.xml
**Image:** 

### 23b-Tuplets-Styles.xml
**Description:** Different tuplet styles: default, none, x:y, x:y-note, and
    x-note:y-note; each with bracket, slur, and without bracket. Finally,
    non-standard 4:3 and 17:2 tuplets are given in the last measure.
**XML File:** 23b-Tuplets-Styles.xml
**Image:** 

### 23c-Tuplet-Display-NonStandard.xml
**Description:** Displaying tuplet note types that might not coincide with the
    displayed note. The tuplets in measure 1 derive the type from the note, the
    tuplets in measure 2 use the <normal-type and <normal-dot> children of the
      <time-modification> element, and the remaining tuplets use the
      <tuplet-type> and <tuplet-dot> children of the <tuplet> element.
**XML File:** 23c-Tuplet-Display-NonStandard.xml
**Image:** 

### 23d-Tuplets-Nested.xml
**Description:** Tuplets can be nested. The first bar contains a 5:2 tuplet (with
    16th notes) in the middle of a 3:2 tuple (with eighth notes). The second bar
    has a 5:2 tuplet at the beginning and at the end (with the tuplet number
    forced below) of a 3:2 tuple (with the bracket forced above). The third bar
    changes the <divisions> value and contains a triplet with eighths on the
    last beat of another triplet with quarter notes.
**XML File:** 23d-Tuplets-Nested.xml
**Image:** 

### 23e-Tuplets-Tremolo.xml
**Description:** Tremolo tuplets. The first bar contains normal eighth triplets with
    staccato points, the second bar holds three tremolo tuplets, the third bar
    holds a sextuple followed by a triplet, the fourth bar contains a sextuple
    (starting on the second beat) with a ‘fp’ sign, and the fifth bar is
    identical to the third bar.
**XML File:** 23e-Tuplets-Tremolo.xml
**Image:** 

### 23f-Tuplets-DurationButNoBracket.xml
**Description:** Tuplets without brackets, using only <time-modification>. The upper
    staff contains two quarters followed by a quarter triplet. The lower staff
    holds two eighths, an eighths triplet, four 16th notes, and a 16th
    sextuplet.
**XML File:** 23f-Tuplets-DurationButNoBracket.xml
**Image:** 

### 24a-GraceNotes.xml
**Description:** Different kinds of grace notes. First measure: single 1/16 grace
    note, two beamed 1/16 grace notes, 1/16 appoggiatura, 1/8 appoggiatura.
    Second measure: slashed single 1/16 grace note, two beamed 1/16 grace notes
    (with both notes marked as slashed), 1/16 acciaccatura, 1/16 grace note
    (without slash) right before the bar line. Third measure: no grace note
    before chord, 1/4 grace note with sharp, two 1/4 grace notes with flats,
    1/16 slashed grace note with natural before a quarter rest.
**XML File:** 24a-GraceNotes.xml
**Image:** 

### 24b-ChordAsGraceNote.xml
**Description:** Chords as grace notes. The last (unslashed and beamed) grace group
    consists of two chords with one tie between the two grace chords and another
    tie between the last grace chord and the main chord.
**XML File:** 24b-ChordAsGraceNote.xml
**Image:** 

### 24c-GraceNote-MeasureEnd.xml
**Description:** A grace note that appears at the measure end (without any
    ‘steal-from-*’ attribute set). Some applications need to convert this into
    an after-grace.
**XML File:** 24c-GraceNote-MeasureEnd.xml
**Image:** 

### 24d-AfterGrace.xml
**Description:** Some grace notes and after-graces indicated by
    ‘steal-time-previous’ (for the first grace note) and ‘steal-time-following’
    (for the second one). The remaining grace notes have no such attribute.
**XML File:** 24d-AfterGrace.xml
**Image:** 

### 24e-GraceNote-StaffChange.xml
**Description:** Grace notes on a different staff than the actual notes.
**XML File:** 24e-GraceNote-StaffChange.xml
**Image:** 

### 24f-GraceNote-Slur.xml
**Description:** A grace note with a slur to the actual note. The <grace> element
    has no ‘slash’ attribute; since MusicXML does not provide a default value it
    is up to the application to interpret the grace note as an acciaccatura
    (with slash) or an appoggiatura (without slash).
**XML File:** 24f-GraceNote-Slur.xml
**Image:** 

### 24g-GraceNote-Dynamics.xml
**Description:** Grace notes in combination with dynamics. The ‘f’ sign is located
    on the first grace note (using a <direction> element), followed by a
    diminuendo wedge, and the ‘p’ sign is on the main beat (again using
    <direction>).
**XML File:** 24g-GraceNote-Dynamics.xml
**Image:** 

### 24h-GraceNote-Simultaneous.xml
**Description:** Simultaneous grace notes and grace note groups of different length
    starting a part or a voice.
**XML File:** 24h-GraceNote-Simultaneous.xml
**Image:** 

### 31a-Directions.xml
**Description:** All <direction> elements defined in MusicXML. The lyrics for each
    note describes the direction element assigned to that note. Not marked with
    lyrics is a <scordatura> element at the very beginning.
**XML File:** 31a-Directions.xml
**Image:** 

### 31b-Directions-Order.xml
**Description:** Using <offset> it is possible to make successive <direction>
    elements look like being concatenated. However, it is a bad idea in general
    to do that because it makes the rendering dependent on a program’s score
    formatting.
**XML File:** 31b-Directions-Order.xml
**Image:** 

### 31c-MetronomeMarks.xml
**Description:** Tempo markings (every third quarter): ‘quarter.=100’,
    ‘quarter..=half.’, ‘(quarter.=half..)’, ‘(quarter.=77)’.
**XML File:** 31c-MetronomeMarks.xml
**Image:** 

### 31d-Directions-Compounds.xml
**Description:** This tests various combinations of <direction> children. The lyrics
    for each note describe the compound elements assigned to that note.
**XML File:** 31d-Directions-Compounds.xml
**Image:** 

### 31f-Direction-Multiline-Compounds.xml
**Description:** A test that checks how newlines in <words> elements are supported.
**XML File:** 31f-Direction-Multiline-Compounds.xml
**Image:** 

### 32a-Notations.xml
**Description:** Most <notation> elements defined in MusicXML. The lyrics show the
    notation assigned to each note.
**XML File:** 32a-Notations.xml
**Image:** 

### 32b-Articulations-Texts.xml
**Description:** Text markup with different CSS font sizes, weights, horizontal
    positions (using ‘default-x’), and vertical positions (using ‘default-y’),
    seven in total. The four markups below the staff are positioned immediately
    before <measure> elements; they should be thus associated with the following
    bar line. One markup is also drawn in red.
**XML File:** 32b-Articulations-Texts.xml
**Image:** 

### 32c-MultipleNotationChildren.xml
**Description:** It should not make any difference whether two articulations are
    given inside two different <notations> elements, inside two different
    <articulations> children of the same <notations> element, or inside the same
    <articulations> element. Thus, all three notes should have a staccato and an
    accent.
**XML File:** 32c-MultipleNotationChildren.xml
**Image:** 

### 32d-Arpeggio.xml
**Description:** Different arpeggio kinds and directions.
**XML File:** 32d-Arpeggio.xml
**Image:** 

### 33a-Spanners.xml
**Description:** Several spanners as defined in MusicXML: tuplet, slur (solid,
    dashed), wedge (cresc, dim), trill with accidental mark and wavy-line (with
    another accidental mark on the second beat), single-note trill spanner,
    octave-shift (8va,15mb), bracket (solid down/down, dashed down/down, solid
    none/down, dashed none/up, solid none/none), dashes, glissando (wavy), slide
    (solid), grouping, two-note tremolo, hammer-on, pull-off, pedal line (down,
    change, up), pedal text (down, up).
**XML File:** 33a-Spanners.xml
**Image:** 

### 33b-Spanners-Tie.xml
**Description:** Two whole notes with a tie inbetween.
**XML File:** 33b-Spanners-Tie.xml
**Image:** 

### 33c-Spanners-Slurs.xml
**Description:** A note can be the end of one slur and the start of a new slur.
    Also, in MusicXML, nested slurs are possible like in the second measure
    where one slur goes over all four notes, and another slur goes from the
    second to the third note.
**XML File:** 33c-Spanners-Slurs.xml
**Image:** 

### 33da-Spanners-OctaveShifts-before.xml
**Description:** All types of octave shifts (15ma on the third eighth note, 15mb on
    the fourth and fifth, 8va on the sixth and seventh, and 8vb on the last two
    16th notes). This test file positions <octave-shift type="stop"> before
    the associated note, as expected in MusicXML import of Finale, for example.
    Consequently, it contains ‘Finale’ as the <software> tag.
**XML File:** 33da-Spanners-OctaveShifts-before.xml
**Image:** 

### 33db-Spanners-OctaveShifts-after.xml
**Description:** All types of octave shifts (15ma on the third eighth note, 15mb on
    the fourth and fifth, 8va on the sixth and seventh, and 8vb on the last two
    16th notes). This test file positions <octave-shift type="stop"> after the
    associated note, as expected in MusicXML import of MuseScore, for example.
    Consequently, it contains ‘MuseScore’ as the <software> tag.
**XML File:** 33db-Spanners-OctaveShifts-after.xml
**Image:** 

### 33e-Spanners-OctaveShifts-InvalidSize.xml
**Description:** Invalid <octave-shifts> values: 27 down for the second note, and
    11 up for the third note.
**XML File:** 33e-Spanners-OctaveShifts-InvalidSize.xml
**Image:** 

### 33f-Trill-EndingOnGraceNote.xml
**Description:** A trill spanner that spans a grace note and ends on an after-grace
    note at the end of the measure.
**XML File:** 33f-Trill-EndingOnGraceNote.xml
**Image:** 

### 33g-Slur-ChordedNotes.xml
**Description:** Slurs on chorded notes. The upper slur connects the first and third
    chord; for both the start and end, the <slur> element is attached not to the
    first note of the chord but to the second one (tagged with <chord>). The
    lower slur connects the chord on the second beat and the note on fourth beat
    and is attached in the normal way.
**XML File:** 33g-Slur-ChordedNotes.xml
**Image:** 

### 33h-Spanners-Glissando.xml
**Description:** All different types of glissando defined in MusicXML. The first two
    and a half measures contain <glissando> elements with various ‘line-type’
    attributes, the rest contains <slide> elements (also with various
    ‘line-type’ attributes).
**XML File:** 33h-Spanners-Glissando.xml
**Image:** 

### 33i-Ties-NotEnded.xml
**Description:** Several ties that have their end tag missing. The end position of
    the tie starting at position A is at position C. The tie starting at
    position C has no end tag at all. The tie starting at position D is ended at
    position E.
**XML File:** 33i-Ties-NotEnded.xml
**Image:** 

### 33j-Beams-Tremolos.xml
**Description:** Tests for double-note tremolo beams. The first bar shows a
    half-note tremolo (one beam, two strokes) followed by a dotted quarter-note
    tremolo with chords (three strokes). The second bar shows a half-note
    triplet with three tremolos (no beams, three strokes) followed by three
    beamed eighths-note chords with a tremolo (two strokes) between the second
    and third chord.
**XML File:** 33j-Beams-Tremolos.xml
**Image:** 

### 34a-Print-Object-Spanners.xml
**Description:** Test various spanner elements (mostly from <notations>) starting
    from a <note> object with ‘print-object’ set to ‘no’, then test spanners
    ending with such a note object: beam, tuplet, slur, trill + wavy-line,
    glissando (wavy), slide (solid), two-note tremolo, hammer-on, pull-off.
    Spanners starting from an invisible object should be suppressed.
**XML File:** 34a-Print-Object-Spanners.xml
**Image:** 

### 34b-Colors.xml
**Description:** Colors. The elements in the first bar have the ‘color’ attribute
    set to red for <note>, <notehead>, <stem>, <dot>, and <accidental>,
    respectively.
**XML File:** 34b-Colors.xml
**Image:** 

### 34c-Font-Size.xml
**Description:** Font sizes. The elements in the first bar have the ‘font-size’
    attribute set to a larger value for <note>, <notehead>, <trill-mark>, <dot>,
    and <accidental>, respectively.
**XML File:** 34c-Font-Size.xml
**Image:** 

### 41a-MultiParts-Partorder.xml
**Description:** A piece with four parts named ‘P0’, ‘P1’, ‘P2’, and ‘P3’ (in that
    order).
**XML File:** 41a-MultiParts-Partorder.xml
**Image:** 

### 41b-MultiParts-MoreThan10.xml
**Description:** A piece with 20 parts (called ‘P0’ to ‘P19’) using a small global
    font size to check whether an application supports that many parts and
    whether they are correctly sorted.
**XML File:** 41b-MultiParts-MoreThan10.xml
**Image:** 

### 41c-StaffGroups.xml
**Description:** A huge orchestra score with 25 parts and different kinds of nested,
    bracketed groups, using quite a small staff size. Each part/group is
    assigned a name and an abbreviation (if necessary) to be shown before the
    staff. Also, most groups show unbroken bar lines, while the bar lines are
    broken between the groups.
**XML File:** 41c-StaffGroups.xml
**Image:** 

### 41d-StaffGroups-Nested.xml
**Description:** Two properly nested part groups: One group (with a bracket) goes
    from staff 2 to 4, and another group (with a brace) goes from staff 3 to 4.
**XML File:** 41d-StaffGroups-Nested.xml
**Image:** 

### 41e-StaffGroups-InstrumentNames-Linebroken.xml
**Description:** The <part-name> and <part-abbreviation> fields don’t have an
    ‘xml:space’ attribute, making the interpretation of whitespace in the
    element content implementation-dependent.
**XML File:** 41e-StaffGroups-InstrumentNames-Linebroken.xml
**Image:** 

### 41f-StaffGroups-Overlapping.xml
**Description:** MusicXML allows for overlapping part groups, but many applications
    do not support that, requiring that they are properly nested instead. In
    this test, ‘Group 1’ (with a bracket) goes from staff 1 to 4, and ‘Group 2’
    (also with a bracket) goes from staff 3 to 5.
**XML File:** 41f-StaffGroups-Overlapping.xml
**Image:** 

### 41g-StaffGroups-NestingOrder.xml
**Description:** The horizontal order of nested group delimiters (brackets, braces,
    etc.) is unspecified in MusicXML; however, it can be controlled by the
    ‘default-x’ attribute of <group-symbol> in case the application’s default
    positioning produces unwanted results (and the application actually supports
    this attribute).
**XML File:** 41g-StaffGroups-NestingOrder.xml
**Image:** 

### 41h-TooManyParts.xml
**Description:** This piece has two more <part> elements than the <part-list>
    section contains. One can either convert all the parts present but not
    listed in the part list, or simply not import or ignore them.
**XML File:** 41h-TooManyParts.xml
**Image:** 

### 41i-PartNameDisplay-Override.xml
**Description:** MusicXML allows <part-name> and <part-name-display> in the
    <score-part> element. If <part-name-display> is given, it overrides
    <part-name> for display.
**XML File:** 41i-PartNameDisplay-Override.xml
**Image:** 

### 41j-PartNameDisplay-Multiple-DisplayText-Children.xml
**Description:** This score has multiple <display-text> elements in its
    <part-name-display> block: the word ‘Player’ printed in red with a large
    font size, the word ‘One’ printed in blue with a small, italic font size.
**XML File:** 41j-PartNameDisplay-Multiple-DisplayText-Children.xml
**Image:** 

### 41k-PartName-Print.xml
**Description:** The <part-name-display> and <part-abbreviation-display> elements
    can also be children of the <print> element; the former is used at the
    beginning of the first bar, and the latter at the beginning of the second
    bar.
**XML File:** 41k-PartName-Print.xml
**Image:** 

### 41l-GroupNameDisplay-Override.xml
**Description:** If <group-name-display> is given, it overrides <group-name> for
    display.
**XML File:** 41l-GroupNameDisplay-Override.xml
**Image:** 

### 42a-MultiVoice-TwoVoicesOnStaff-Lyrics.xml
**Description:** Two voices share one staff. To each voice some lyrics is assigned
    (with the lyrics of voice one positioned above the staff). In the last bar,
    the second voice is empty, thus the full rest should be placed on the
    staff’s default position.
**XML File:** 42a-MultiVoice-TwoVoicesOnStaff-Lyrics.xml
**Image:** 

### 42b-MultiVoice-MidMeasureClefChange.xml
**Description:** A part with three voices; two on the upper staff and one on the
    lower staff. There are two clef changes in the upper staff, one in the
    middle of measure 1 and one at the end. The third voice (i.e., the second
    voice in the upper staff) has a length of only three eights, starting at the
    fourth eighth of the first measure.
**XML File:** 42b-MultiVoice-MidMeasureClefChange.xml
**Image:** 

### 43a-PianoStaff.xml
**Description:** A simple piano staff, i.e., two voices, each on a separate staff.
**XML File:** 43a-PianoStaff.xml
**Image:** 

### 43b-MultiStaff-DifferentKeys.xml
**Description:** A piano staff with different keys and clefs for each of its staves.
    The keys and clefs for both staves are given at the very beginning of the
    measure.
**XML File:** 43b-MultiStaff-DifferentKeys.xml
**Image:** 

### 43c-MultiStaff-DifferentKeysAfterBackup.xml
**Description:** A piano staff with different keys and clefs for each of its staves.
    The key and clef for the second staff is given only after a <backup>
    element, just before the first note of the second staff, but after the whole
    measure for staff one has been output.
**XML File:** 43c-MultiStaff-DifferentKeysAfterBackup.xml
**Image:** 

### 43d-MultiStaff-StaffChange.xml
**Description:** Staff changes in a piano staff. In the first measure, the voice
    from the second staff has some notes on the first staff. In the second
    measure, second eighth, the voice from the second staff has a chord on the
    first staff.
**XML File:** 43d-MultiStaff-StaffChange.xml
**Image:** 

### 43e-Multistaff-ClefDynamics.xml
**Description:** A piano staff with dynamics and clef changes, where each element
    (‘ffff’, a wedge, and clef changes) applies only to one voice or one staff,
    respectively.
**XML File:** 43e-Multistaff-ClefDynamics.xml
**Image:** 

### 43f-MultiStaff-Lyrics.xml
**Description:** Two voices of a single part on two staves, with lyrics. The lyrics
    of voice one is positioned above the staff.
**XML File:** 43f-MultiStaff-Lyrics.xml
**Image:** 

### 43g-MultiStaff-PartSymbol.xml
**Description:** In this four-staves part, the <part-symbol> element spans up a
    ‘square’ staff group delimiter between staves 2 and 3.
**XML File:** 43g-MultiStaff-PartSymbol.xml
**Image:** 

### 45a-SimpleRepeat.xml
**Description:** A simple, repeated measure (to be played five times), with an
    implicit start at the beginning.
**XML File:** 45a-SimpleRepeat.xml
**Image:** 

### 45b-RepeatWithAlternatives.xml
**Description:** A simple repeat with two alternative endings (volta brackets).
**XML File:** 45b-RepeatWithAlternatives.xml
**Image:** 

### 45c-SimpleRepeat-Nested.xml
**Description:** Repeats can also be nested. The inner repeat spans from bar 2 to
    bar bar 3 (to be played five times). The outer repeat spans implicitly from
    the beginning to bar 7 (to be played one time, i.e., it doesn’t get
    repeated).
**XML File:** 45c-SimpleRepeat-Nested.xml
**Image:** 

### 45d-Repeats-MultipleEndings.xml
**Description:** Multiple alternative endings. The first alternative starts and ends
    at bar 2; the second continues until bar 5, the third, fifth, and seventh
    until bar 9, the fourth and sixth until bar 10, and the eighth is
    discontinued after one bar.
**XML File:** 45d-Repeats-MultipleEndings.xml
**Image:** 

### 45e-Repeats-Combination.xml
**Description:** A series of repeat elements.
**XML File:** 45e-Repeats-Combination.xml
**Image:** 

### 45f-Repeats-InvalidEndings.xml
**Description:** A stress test with a combination of <repeat> and <ending> elements
    that don’t make sense. The displayed result depends on the sanitizing
    possibilities of the application that handles the input.
**XML File:** 45f-Repeats-InvalidEndings.xml
**Image:** 

### 45g-Repeats-NotEnded.xml
**Description:** A forward-repeating bar line without an ending repeat bar.
**XML File:** 45g-Repeats-NotEnded.xml
**Image:** 

### 45h-Repeats-Partial.xml
**Description:** A repeat starting and ending at a partial bar. The style of the
    back-to-back bar line is ’heavy-heavy’ (using the MusicXML encoding as
    exported by Finale).
**XML File:** 45h-Repeats-Partial.xml
**Image:** 

### 45i-Repeats-Nested.xml
**Description:** A repeat with two alternative endings. In the first one (enclosing
    bar 2 to bar 4), there is a nested repeat enclosing bar 3. In the second one
    (from bar 5 to bar 6), there is another nested repeat starting also at bar 5
    but ending already at the same bar.
**XML File:** 45i-Repeats-Nested.xml
**Image:** 

### 45j-SimpleRepeat-Start.xml
**Description:** A simple, repeated measure with an explicit start at the beginning.
**XML File:** 45j-SimpleRepeat-Start.xml
**Image:** 

### 45k-Repeats-Chords-FiguredBass.xml
**Description:** A repeat with volta brackets, also showing chord names and figured
    bass. As a specialty, the repeat at the end of measure 2 is encoded as two
    successive <barline> elements, holding a <repeat> child in the first and an
    <ending> child in the second element.
**XML File:** 45k-Repeats-Chords-FiguredBass.xml
**Image:** 

### 46a-Barlines.xml
**Description:** Different types of (non-repeat) bar lines: default (no setting),
    regular, dotted, dashed, heavy, light-light, light-heavy, heavy-light,
    heavy-heavy, tick, short, none.
**XML File:** 46a-Barlines.xml
**Image:** 

### 46b-MidmeasureBarline.xml
**Description:** Bar lines can appear at mid-measure positions (having a
    ‘location="middle"’ attribute), without using an implicit measure.
**XML File:** 46b-MidmeasureBarline.xml
**Image:** 

### 46c-Midmeasure-Clef.xml
**Description:** A clef change in the middle of a measure, using either an implicit
    measure (as done for the second half of measure 2) with an <attributes>
    element at its beginning, or placing an <attributes> element at the middle
    of the measure (as done in measure 3).
**XML File:** 46c-Midmeasure-Clef.xml
**Image:** 

### 46d-PickupMeasure-ImplicitMeasures.xml
**Description:** A combination of unusual measures.
**XML File:** 46d-PickupMeasure-ImplicitMeasures.xml
**Image:** 

### 46e-PickupMeasure-SecondVoiceStartsLater.xml
**Description:** Voice 2 (consisting of a single quarter note) should start at the
    second beat of the first full measure.
**XML File:** 46e-PickupMeasure-SecondVoiceStartsLater.xml
**Image:** 

### 46f-IncompleteMeasures.xml
**Description:** Measures can contain less notes than the time signature says. At
    the very beginning this is usually handled as an upbeat (even if the first
    measure is not tagged as measure 0 or the ‘implicit’ attribute is missing).
**XML File:** 46f-IncompleteMeasures.xml
**Image:** 

### 46g-PickupMeasure-Chordnames-FiguredBass.xml
**Description:** A pickup measure with chord names and figured bass.
**XML File:** 46g-PickupMeasure-Chordnames-FiguredBass.xml
**Image:** 

### 46h-Barline-Fermata.xml
**Description:** Fermatas over bar lines using <fermata> as a child of <barline>.
    The bar line between measure 1 and 2 has one red-colored fermata on the top,
    the bar line between measure 2 and 3 has two fermatas, one above and one
    below (the latter one with a ‘type="inverted"’ attribute and in larger
    size).
**XML File:** 46h-Barline-Fermata.xml
**Image:** 

### 51a-Header-Credits.xml
**Description:** Check multiple <credit> elements with various <credit-type>
    children (also testing the case without such a child and having two). There
    is also a <defaults> block to properly set up (quite small) page dimensions
    and a ‘tenth’ value, making the ‘default-x’ and ‘default-y’ attributes of
    <credit-words> children meaningful.
**XML File:** 51a-Header-Credits.xml
**Image:** 

### 51b-Header-Quotes.xml
**Description:** Header fields and part names can contain double quotes ("). This
    test checks some fields (<movement-title>, <creator type="composer">,
    <rights>, <software>, <part-name>) whether they are converted or imported
    without problems (i.e., whether they are correctly escaped when converting).
**XML File:** 51b-Header-Quotes.xml
**Image:** 

### 51c-MultipleMetadata.xml
**Description:** Some metadata elements can occur multiple times. This test file
    contains the elements <creator> (type ‘composer’), <rights>, <software>, and
    <encoding-date> twice.
**XML File:** 51c-MultipleMetadata.xml
**Image:** 

### 51d-EmptyTitle.xml
**Description:** A piece with empty (but existing) <work-title> and <work-number>
    elements. <movement-title> is non-empty but <movement-number> is empty, too.
    It makes sense to use <movement-title> as the title in this situation.
**XML File:** 51d-EmptyTitle.xml
**Image:** 

### 52a-PageLayout.xml
**Description:** Several page layout settings: paper size (16cm×9cm), page margins
    (left / right = 3cm / 2cm, top / bottom = 0.5cm / 0.7cm), system margins
    (left / right = 1.5cm / 2cm), distances (top-system / system-system = 4cm /
    3cm), and different fonts.
**XML File:** 52a-PageLayout.xml
**Image:** 

### 52b-Breaks.xml
**Description:** A system break (after measure 1) and a page break (after measure 2)
    using <print> elements.
**XML File:** 52b-Breaks.xml
**Image:** 

### 61a-Lyrics.xml
**Description:** Some notes with simple lyrics: Syllables, notes without a syllable,
    syllable spanners.
**XML File:** 61a-Lyrics.xml
**Image:** 

### 61b-MultipleLyrics.xml
**Description:** Multiple (simple) lyrics. The order of the exported stanzas is
    relevant (identified by the number attribute in this test case)
**XML File:** 61b-MultipleLyrics.xml
**Image:** 

### 61c-Lyrics-Pianostaff.xml
**Description:** Lyrics assigned to the voices of a piano staff containing two
    simple staves. Each staff is assigned exactly one lyrics line.
**XML File:** 61c-Lyrics-Pianostaff.xml
**Image:** 

### 61d-Lyrics-Melisma.xml
**Description:** How to treat lyrics and slurred notes. Normally, a slurred group of
    notes is assigned only one lyrics syllable.
**XML File:** 61d-Lyrics-Melisma.xml
**Image:** 

### 61e-Lyrics-Chords.xml
**Description:** Assigning lyrics to chorded notes.
**XML File:** 61e-Lyrics-Chords.xml
**Image:** 

### 61f-Lyrics-GracedNotes.xml
**Description:** Grace notes shall not mess up the lyrics, and they shall not be
    assigned to a syllable.
**XML File:** 61f-Lyrics-GracedNotes.xml
**Image:** 

### 61g-Lyrics-NameNumber.xml
**Description:** A lyrics syllable can have both a number and a name attribute. The
    question is: What should be used to put syllables of the same voice
    together. This example uses different number/name combinations to check how
    different applications handle this unspecified case (The advice on the
    MusicXML mailing list was "there is no correct way, each application can do
    what it thinks is best").
**XML File:** 61g-Lyrics-NameNumber.xml
**Image:** 

### 61h-Lyrics-BeamsMelismata.xml
**Description:** Beaming or slurs can indicate melismata for lyrics. Also make sure
    that notes without an explicit syllable are treated as if they were part of
    a melisma.
**XML File:** 61h-Lyrics-BeamsMelismata.xml
**Image:** 

### 61i-Lyrics-Chords.xml
**Description:** Each note of a chord can have some lyrics attached. In this case,
    each note of the chord has lyrics of the form "Lyrics [123]" attached,
    where each lyrics has a different number attribute to distinguish them.
    These syllables should be imported into three different stanzas and the
    timing should be correct.
**XML File:** 61i-Lyrics-Chords.xml
**Image:** 

### 61j-Lyrics-Elisions.xml
**Description:** Multiple lyrics syllables assigned to a single note are implemented
    either using a space in the lyrics’ <text> element or by using <elision>.
**XML File:** 61j-Lyrics-Elisions.xml
**Image:** 

### 61k-Lyrics-SpannersExtenders.xml
**Description:** Lyrics spanners: continued syllables and extenders, possibly
    spanning multiple notes. The intermediate notes do not have any <lyric>
    element.
**XML File:** 61k-Lyrics-SpannersExtenders.xml
**Image:** 

### 71a-Chordnames.xml
**Description:** A normal staff with several (complex) chord names displayed.
**XML File:** 71a-Chordnames.xml
**Image:** 

### 71c-ChordsFrets.xml
**Description:** A staff with chord names and some fretboards shown. The fretboards
    can have an arbitrary number of frets/strings, can start at an arbitrary
    fret and can even contain fingering information.
**XML File:** 71c-ChordsFrets.xml
**Image:** 

### 71d-ChordsFrets-Multistaff.xml
**Description:** Chords and fretboards assigned to the voices in a multi-voice,
    multi-staff part. There should be fret diagrams above each of the two
    staves.
**XML File:** 71d-ChordsFrets-Multistaff.xml
**Image:** 

### 71e-TabStaves.xml
**Description:** Some tablature staves, with explicit fingering information and
    different string tunings given in the MusicXML file.
**XML File:** 71e-TabStaves.xml
**Image:** 

### 71f-AllChordTypes.xml
**Description:** All chord types defined in MusicXML. The staff will only contain
    one c’ note (NO chord) for all of them, but the chord names should be
    properly printed.
**XML File:** 71f-AllChordTypes.xml
**Image:** 

### 71g-MultipleChordnames.xml
**Description:** There can be multiple subsequent harmony elements, indicating a
    harmony change during a note
**XML File:** 71g-MultipleChordnames.xml
**Image:** 

### 72a-TransposingInstruments.xml
**Description:** Transposing instruments: Trumpet in Bb, Horn in Eb, Piano; All of
    them show the C major scale (the trumpet with 2 sharp, the horn with 3
    sharp).
**XML File:** 72a-TransposingInstruments.xml
**Image:** 

### 72b-TransposingInstruments-Full.xml
**Description:** Various transposition. Each part plays a c’’, just displayed in
    different display pitches. The second-to-last staff uses a transposition
    where the displayed c’ is an actual f’’’ concert pitch. The final staff is
    an untransposed instrument.
**XML File:** 72b-TransposingInstruments-Full.xml
**Image:** 

### 72c-TransposingInstruments-Change.xml
**Description:** An instrument change from one transposition (Clarinet in Eb) to
    another transposing instrument (Clarinet in Bb). The displayed instrument
    name should also be updated.
**XML File:** 72c-TransposingInstruments-Change.xml
**Image:** 

### 73a-Percussion.xml
**Description:** Three types of percussion staves: A five-line staff with bass clef
    for Timpani, a five-line staff with percussion clef, and a one-line
    percussion staff with only unpitched notes.
**XML File:** 73a-Percussion.xml
**Image:** 

### 74a-FiguredBass.xml
**Description:** Some figured bass containing alterated figures, bracketed figures
    and slashed figures.
**XML File:** 74a-FiguredBass.xml
**Image:** 

### 75a-AccordionRegistrations.xml
**Description:** All possible accordion registrations.
**XML File:** 75a-AccordionRegistrations.xml
**Image:** 

### 90a-Compressed-MusicXML.mxl
**Description:** A compressed MusicXML file, containing a simple MusicXML score and
    the corresponding .pdf output for reference.
**XML File:** 90a-Compressed-MusicXML.mxl
**Image:** 

### 99a-Sibelius5-IgnoreBeaming.xml
**Description:** Dolet 3 for Sibelius (5.1) did not print out any closing beam tags,
    only starting and continuing beam tags. For such files, one either needs to
    ignore all beaming information or close all beams
**XML File:** 99a-Sibelius5-IgnoreBeaming.xml
**Image:** 

### 99b-Lyrics-BeamsMelismata-IgnoreBeams.xml
**Description:** If we properly ignore all beaming information from the Dolet 3 for
    Sibelius export file, make sure that the lyrics syllables are still assigned
    to the correct notes.
**XML File:** 99b-Lyrics-BeamsMelismata-IgnoreBeams.xml
**Image:** 

