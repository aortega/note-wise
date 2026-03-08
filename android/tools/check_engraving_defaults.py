#!/usr/bin/env python3
"""
Script to check engraving defaults for clef positioning information.
"""

import json

def main():
    # Load the metadata file
    with open('bravura/redist/bravura_metadata.json', 'r') as f:
        data = json.load(f)
    
    print("=== ENGRAVING DEFAULTS ===")
    if 'engravingDefaults' in data:
        engraving = data['engravingDefaults']
        print(f"Type: {type(engraving)}")
        print(f"Number of defaults: {len(engraving)}")
        
        # Look for clef-related defaults
        clef_defaults = {}
        for key, value in engraving.items():
            if 'clef' in key.lower() or key in ['gClef', 'fClef', 'cClef']:
                clef_defaults[key] = value
        
        if clef_defaults:
            print(f"\n=== CLEF-RELATED ENGRAVING DEFAULTS ===")
            for key, value in clef_defaults.items():
                print(f"{key}: {value}")
        else:
            print("No clef-related engraving defaults found")
        
        # Show staff positioning defaults
        print(f"\n=== STAFF POSITIONING DEFAULTS ===")
        staff_defaults = {}
        for key, value in engraving.items():
            if any(term in key.lower() for term in ['staff', 'line', 'position', 'offset']):
                staff_defaults[key] = value
        
        for key, value in list(staff_defaults.items())[:10]:  # Show first 10
            print(f"{key}: {value}")
        
        # Show all defaults to understand structure
        print(f"\n=== ALL ENGRAVING DEFAULTS (first 10) ===")
        for i, (key, value) in enumerate(list(engraving.items())[:10]):
            print(f"{i+1}. {key}: {value}")
    
    else:
        print("engravingDefaults section not found")

if __name__ == "__main__":
    main()
