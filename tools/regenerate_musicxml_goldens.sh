#!/usr/bin/env bash
# Regenerates the W3C MusicXML Test Suite golden PNGs for NoteWise visual tests.
#
# Must be run from the repo root:
#   bash tools/regenerate_musicxml_goldens.sh
#
# What it does:
#  1. Runs the alphaTab MusicXMLTestSuite visual tests (padding=[7,0,7,0], no footer)
#  2. Accepts the new renders as the updated goldens
#  3. Copies the 150 PNGs to android/NoteWise/.../visual-goldens/musicxml-suite/
#  4. Updates approval_manifest.json with the new heights
#
# Requirements: node >= 20 and npm must be on your PATH.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AT_PKG="$REPO_ROOT/android/reference/alphaTab-develop/packages/alphatab"
AT_VISUAL_DIR="$AT_PKG/test-data/visual-tests/musicxml-testsuite"
GOLDEN_DIR="$REPO_ROOT/android/NoteWise/app/src/test/resources/visual-goldens/musicxml-suite"
MANIFEST="$GOLDEN_DIR/approval_manifest.json"

echo "=== Step 1: Run MusicXMLTestSuite visual tests ==="
(
  cd "$AT_PKG"
  npm test -- --grep "MusicXMLTestSuiteTests" --timeout 60000
) || true   # tests are expected to fail (new renders != old goldens)

echo ""
echo "=== Step 2: Accept new renders as goldens ==="
(
  cd "$AT_PKG"
  npm run test-accept-reference
)

echo ""
echo "=== Step 3: Copy PNGs to visual-goldens/musicxml-suite/ ==="
mkdir -p "$GOLDEN_DIR"
COPIED=0
for src_png in "$AT_VISUAL_DIR"/*.png; do
  stem="$(basename "$src_png" .png)"
  # Convert alphaTab stem (01a-Pitches-Pitches) to golden stem (01a_pitches_pitches)
  golden_stem="${stem//-/_}"
  golden_stem="${golden_stem,,}"   # lowercase

  # Detect width from the PNG IHDR chunk
  width=$(python3 -c "
import struct, sys
with open('$src_png', 'rb') as f:
    f.read(8); f.read(4); f.read(4); d = f.read(13)
    print(struct.unpack('>I', d[:4])[0])
")
  dest_png="$GOLDEN_DIR/${golden_stem}_${width}.png"
  cp "$src_png" "$dest_png"
  COPIED=$((COPIED + 1))
done
echo "Copied $COPIED PNGs."

echo ""
echo "=== Step 4: Update approval_manifest.json ==="
python3 "$REPO_ROOT/tools/update_musicxml_manifest.py" \
  --golden-dir "$GOLDEN_DIR" \
  --visual-dir "$AT_VISUAL_DIR" \
  --manifest "$MANIFEST"

echo ""
echo "Done. Run the Kotlin visual tests to verify:"
echo "  ./gradlew :app:test --tests 'dev.pola.notewise.visual.MusicXMLSuiteVisualTest'"
