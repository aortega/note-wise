# Visual Goldens

Golden PNG snapshots for Robolectric visual regression tests.

## Update Workflow

1. Run tests with updates enabled:
   - `UPDATE_VISUAL_GOLDENS=true ./gradlew :app:testDebugUnitTest --tests "*GrandStaffVisualTest*"`
2. Review generated images under `app/src/test/resources/visual-goldens/`.
3. Re-run tests without update mode:
   - `./gradlew :app:testDebugUnitTest --tests "*GrandStaffVisualTest*"`

## LilyPond Tier-1 Workflow

1. Generate/refresh LilyPond Tier-1 goldens:
   - `UPDATE_VISUAL_GOLDENS=true ./gradlew :app:testDebugUnitTest --tests "*LilyPondTier1VisualTest*"`
2. Optional multi-width run (comma-separated widths):
   - `LILYPOND_VISUAL_WIDTHS=420,720,1080,1440 UPDATE_VISUAL_GOLDENS=true ./gradlew :app:testDebugUnitTest --tests "*LilyPondTier1VisualTest*"`
3. Validate without updates:
   - `./gradlew :app:testDebugUnitTest --tests "*LilyPondTier1VisualTest*"`

Current bootstrap LilyPond baseline set (non-empty verified):
- `11a-TimeSignatures.xml`
- `12aa-Clefs_Pitch_Traditional.xml`
- `13a-KeySignatures.xml`

Temporarily excluded while parser/layout support is expanded:
- `03b-Rhythm-Backup.xml`
- `21a-Chord-Basic.xml`

On mismatch, candidate outputs are written to:
- `app/build/reports/visual-tests/*.new.png`
- `app/build/reports/visual-tests/*.diff.png`

## Progressive Approval Workflow (Fixture-By-Fixture)

Use this when you want to manually approve/reject each fixture before promoting a golden.

1. Initialize manifest:
   - `python3 tools/lilypond_progressive_golden_workflow.py init`
2. Show next fixture and run hint:
   - `python3 tools/lilypond_progressive_golden_workflow.py next`
3. Run only that fixture (example command printed by `next`):
   - `cd android/NoteWise && LILYPOND_FIXTURES="11a-TimeSignatures.xml" LILYPOND_VISUAL_WIDTHS="720" ./gradlew :app:testDebugUnitTest --tests "*LilyPondTier1VisualTest*"`
    - `cd android/NoteWise && LILYPOND_FIXTURES="11a-TimeSignatures.xml" LILYPOND_RELAX_SANITY="true" LILYPOND_VISUAL_WIDTHS="720" ./gradlew :app:testDebugUnitTest --tests "*LilyPondTier1VisualTest*"`
    - `LILYPOND_RELAX_SANITY=true` is recommended for manual progressive review so a `.new.png` candidate is generated even when the render is still visibly incorrect.
4. Review candidate image(s) under:
   - `app/build/reports/visual-tests/lilypond/tier1/`
5. Approve or reject:
   - `python3 tools/lilypond_progressive_golden_workflow.py approve 11a-TimeSignatures.xml`
   - `python3 tools/lilypond_progressive_golden_workflow.py reject 11a-TimeSignatures.xml --note "reason"`
6. Check current state:
   - `python3 tools/lilypond_progressive_golden_workflow.py status`

The manifest is stored at:
- `android/NoteWise/app/src/test/resources/visual-goldens/lilypond/tier1/approval_manifest.json`
