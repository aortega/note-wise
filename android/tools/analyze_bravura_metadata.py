#!/usr/bin/env python3
"""
Script to analyze the Bravura metadata.json file and extract glyphBBoxes information.
This helps understand the structure without loading the entire file at once.
"""

import json
import sys

def analyze_json_structure(file_path):
    """Analyze the top-level structure of the JSON file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            # Load the entire file (might be large but necessary for analysis)
            data = json.load(f)
        
        print("=== TOP-LEVEL KEYS ===")
        for key in data.keys():
            print(f"- {key}")
        
        print(f"\n=== TOTAL TOP-LEVEL KEYS: {len(data)} ===")
        
        # Look for glyphBBoxes specifically
        if 'glyphBBoxes' in data:
            print(f"\n=== glyphBBoxes SECTION FOUND ===")
            glyph_bboxes = data['glyphBBoxes']
            print(f"Type: {type(glyph_bboxes)}")
            
            if isinstance(glyph_bboxes, dict):
                print(f"Number of glyphs: {len(glyph_bboxes)}")
                print(f"\n=== FIRST 5 GLYPH ENTRIES ===")
                for i, (glyph_name, bbox) in enumerate(list(glyph_bboxes.items())[:5]):
                    print(f"{i+1}. {glyph_name}: {bbox}")
                
                # Look for specific glyphs we're using
                target_glyphs = ['gClef', 'noteheadBlack', 'accidentalSharp', 'timeSig4']
                print(f"\n=== TARGET GLYPHS ===")
                for glyph in target_glyphs:
                    if glyph in glyph_bboxes:
                        print(f"{glyph}: {glyph_bboxes[glyph]}")
                    else:
                        print(f"{glyph}: NOT FOUND")
                        
            elif isinstance(glyph_bboxes, list):
                print(f"List length: {len(glyph_bboxes)}")
                if glyph_bboxes:
                    print(f"First entry: {glyph_bboxes[0]}")
        else:
            print(f"\n=== glyphBBoxes SECTION NOT FOUND ===")
            print("Available keys that might contain bounding box info:")
            bbox_related_keys = [k for k in data.keys() if 'bbox' in k.lower() or 'bound' in k.lower()]
            for key in bbox_related_keys:
                print(f"- {key}")
    
    except json.JSONDecodeError as e:
        print(f"JSON decode error: {e}")
    except FileNotFoundError:
        print(f"File not found: {file_path}")
    except Exception as e:
        print(f"Error: {e}")

def extract_glyph_bboxes(file_path, output_file=None):
    """Extract just the glyphBBoxes section we need."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        if 'glyphBBoxes' in data:
            glyph_bboxes = data['glyphBBoxes']
            
            # Extract only the glyphs we're currently using or anticipate using soon.
            target_glyphs = {
                'gClef': None,
                'fClef': None,
                'cClef': None,
                'noteheadBlack': None,  # Often corresponds to noteHeadQuarter in SMuFL naming
                'noteHeadQuarter': None,
                'noteHeadHalf': None,
                'noteHeadWhole': None,
                'noteheadWhole': None,  # Try different naming convention
                'noteheadWholeRest': None,  # Might be named differently
                'accidentalSharp': None,
                'accidentalFlat': None,
                'accidentalNatural': None,
                'accidentalDoubleSharp': None,
                'accidentalDoubleFlat': None,
                'timeSig0': None,
                'timeSig1': None,
                'timeSig2': None,
                'timeSig3': None,
                'timeSig4': None,
                'timeSig5': None,
                'timeSig6': None,
                'timeSig7': None,
                'timeSig8': None,
                'timeSig9': None,
                'timeSigCommon': None,
                'timeSigCutCommon': None
            }
            
            found_glyphs = {}
            for glyph_name in target_glyphs.keys():
                if glyph_name in glyph_bboxes:
                    found_glyphs[glyph_name] = glyph_bboxes[glyph_name]
                # Also check for common variations
                elif glyph_name.lower() in glyph_bboxes:
                    found_glyphs[glyph_name] = glyph_bboxes[glyph_name.lower()]
                elif glyph_name.upper() in glyph_bboxes:
                    found_glyphs[glyph_name] = glyph_bboxes[glyph_name.upper()]
            
            print(f"\n=== EXTRACTED GLYPH BOUNDING BOXES ===")
            for glyph_name, bbox in found_glyphs.items():
                print(f"{glyph_name}: {bbox}")
            
            if output_file:
                with open(output_file, 'w', encoding='utf-8') as f:
                    json.dump(found_glyphs, f, indent=2)
                print(f"\nSaved extracted data to: {output_file}")
            
            return found_glyphs
        else:
            print("glyphBBoxes section not found")
            return None
            
    except Exception as e:
        print(f"Error extracting glyphBBoxes: {e}")
        return None

if __name__ == "__main__":
    metadata_file = "bravura/redist/bravura_metadata.json"
    
    print("Analyzing Bravura metadata structure...")
    analyze_json_structure(metadata_file)
    
    print(f"\n{'='*50}")
    print("Extracting relevant glyph bounding boxes...")
    extracted_bboxes = extract_glyph_bboxes(metadata_file, "extracted_glyph_bboxes.json")
    
    if extracted_bboxes:
        print(f"\nSuccessfully extracted {len(extracted_bboxes)} glyph bounding boxes")
    else:
        print("Failed to extract glyph bounding boxes")
