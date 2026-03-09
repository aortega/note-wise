# Future Improvements

## ML-Assisted Engraving

### Motivation

Traditional engraving rules (as described in the [LilyPond essay on automated engraving](https://lilypond.org/doc/v2.24/Documentation/essay/automated-engraving#getting-things-right))
are a mix of hard constraints and aesthetic heuristics with context-dependent exceptions.
Our current formatter encodes a subset of these rules by hand. A learned model could
capture the full distribution of professional engraving decisions without requiring
every rule to be explicitly programmed.

### Core idea: LilyPond as a free labeler

We already have a pipeline that renders MusicXML through LilyPond and compares the
output pixel-for-pixel. The same pipeline can be extended to **extract note x-positions
from LilyPond SVG output**, creating unlimited labeled training data at zero annotation cost.

```
MusicXML corpus
    → LilyPond SVG render
    → SVG coordinate parser  (build this)
    → (input features, target x-positions) pairs
    → train regression model
    → deploy in VFFormatter replacing hand-coded spacing logic
```

### Scoped sub-problems (ranked by feasibility)

| Problem | Model type | Training signal |
|---|---|---|
| Note x-spacing within a measure | MLP / small transformer | LilyPond SVG note coords |
| System line-break decisions | Sequence classifier (LSTM) | LilyPond break positions |
| Beam slant selection | Binary classifier | LilyPond beam angles |
| Vertical staff spacing | Regression | LilyPond staff Y coords |
| Accidental column stacking | Constraint + ML hybrid | LilyPond accidental coords |

### Starting experiment: measure-level spacing regression

**Input vector per note (within a measure):**
- Duration (rational number, e.g. 0.25 for quarter)
- Accidental type (one-hot: none / flat / sharp / double-flat / double-sharp / natural)
- Pitch class relative to staff center (integer semitones)
- Position index within measure (0..N-1)
- Total note count in measure
- Available note-area width in staff-spaces

**Output:**
- x-offset from note-area left edge, normalized to [0, 1]

**Dataset source:**
- W3C MusicXML test suite (already in repo)
- OpenScore / MuseScore public corpus for breadth

**Baseline:** current `applyFourQuarterGridIfApplicable` centering formula.
The model should outperform it for measures with mixed durations, many accidentals,
or unusual note counts.

### Key engineering prerequisite

Build a **LilyPond SVG → note coordinate parser** that maps each `<use>` glyph in
the SVG back to its MusicXML note identity. This is the one non-trivial build step;
everything else follows from it.

### Relationship to current architecture

The trained model would slot in as a drop-in replacement for `VFFormatter.formatVoices`.
During inference it runs on-device (ONNX or TFLite, <1 MB model size for the regression
scope). The existing visual-golden test suite automatically validates that the ML output
is at least as good as the rule-based baseline.
