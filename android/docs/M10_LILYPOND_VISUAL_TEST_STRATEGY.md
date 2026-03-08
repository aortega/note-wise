# M10 LilyPond Visual Rendering Strategy

## Purpose

This runbook defines how to validate NoteWise M10 rendering using LilyPond regression fixtures with an alphaTab render proxy and deterministic NoteWise golden tests.

Scope: Multi-measure layout and engraving stability only (M10).

## Inputs

- XML fixtures: `android/samples/lilypond_tests/xml_files/`
- LilyPond references: `android/samples/lilypond_tests/images/`
- Fixture metadata: `android/samples/lilypond_tests/test_metadata.json`

## Output Artifacts

For each fixture and width profile, produce:

- `lilypond_reference.png`
- `alphatab_proxy.png`
- `notewise_actual.png`
- `notewise_vs_proxy.diff.png` (optional for triage)
- `notewise_vs_golden.diff.png` (required on failure)

Recommended storage:

- `android/NoteWise/app/src/test/resources/visual-goldens/lilypond/<fixture>/<width>/`
- `android/NoteWise/app/build/reports/visual-tests/lilypond/<fixture>/<width>/`

## Width Profiles

Run all fixture comparisons at these fixed widths:

- `420`
- `720`
- `1080`
- `1440`

## Normalization Rules

Apply identical normalization before comparing outputs:

- White background
- Deterministic scale and DPI
- Bravura for NoteWise notation glyphs
- Suppress headers/footers and non-notation text when possible
- Normalize geometry:
  - pad to common size, or
  - center-crop all images to shared bounds

Do not skip normalization. Unnormalized comparisons are for debugging only.

## alphaTab Proxy Generation

Use alphaTab visual test harness settings equivalent to:

- Skia rendering engine
- `highDpiFactor = 1`
- lazy loading disabled

Save settings snapshot with each generated proxy set so rendering is reproducible.

## Tier Plan

### Tier 1 (required for M10 closure)

- `11a-TimeSignatures.xml`
- `12a-Clefs.xml`
- `13a-KeySignatures.xml`
- `21a-Chord-Basic.xml`
- `03b-Rhythm-Backup.xml`

### Tier 2 (after Tier 1 is green)

- `01a-Pitches-Pitches.xml`
- `02a-Rests-Durations.xml`
- `03c-Rhythm-DivisionChange.xml`
- `13e-KeySignatures-Cancel.xml`

## Pass/Fail Policy

Automated pass criteria:

- NoteWise golden tests pass in non-update mode
- No structural rule regressions (cross-staff leakage, repeated signatures, tie discontinuity)

Manual pass criteria:

- If LilyPond/Bravura font-size or layout differences remain after normalization, reviewer confirms differences are style-only
- Reviewer records fixture IDs and verdict in `android/docs/AGENT_DECISIONS.md`

Fail conditions:

- Semantic engraving mismatch (wrong accidental/key/time placement, tie/beam semantics, staff assignment)
- System break artifacts causing note collisions/overlaps
- Unexplained geometry drift across width profiles

## Suggested Workflow

1. Select Tier 1 fixtures and map XML->LilyPond image via metadata.
2. Generate alphaTab proxy images for the same fixtures and widths.
3. Render NoteWise outputs for the same fixtures and widths.
4. Normalize all images and run automated diffs.
5. Review mismatches manually and classify as semantic vs style-only.
6. Update goldens only for accepted outputs.
7. Record decisions and unresolved deltas in `AGENT_DECISIONS.md` and `AGENT_ISSUES.md`.

## M10 Closure Evidence

Before marking M10 done, include:

- Tier 1 results table with pass/fail per fixture/width
- Links/paths to proxy and NoteWise artifacts
- Manual sign-off note for any style-only accepted deltas
- Command log showing non-update test pass
