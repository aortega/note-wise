#!/usr/bin/env python3
"""
Script to extract comprehensive metadata from LilyPond MusicXML regression tests
including descriptions, categories, and test groupings for each test file.
"""

import requests
from bs4 import BeautifulSoup
import json
import re
from pathlib import Path

def extract_lilypond_metadata():
    """Extract comprehensive metadata from LilyPond MusicXML regression tests"""
    
    base_url = "https://lilypond.org/doc/v2.25/input/regression/musicxml/"
    collated_url = base_url + "collated-files.html"
    
    print(f"Fetching {collated_url}...")
    
    try:
        response = requests.get(collated_url)
        response.raise_for_status()
    except requests.RequestException as e:
        print(f"Error fetching the collated files page: {e}")
        return
    
    soup = BeautifulSoup(response.text, 'html.parser')
    
    tests = []
    current_category = ""
    current_category_description = ""
    
    # Find all h3 headings (categories)
    for h3 in soup.find_all('h3', class_='unnumberedsec'):
        if h3.get('id'):
            category_text = h3.get_text().strip()
            if '...' in category_text:
                current_category = category_text.split('...')[0].strip()
                current_category_description = category_text
            else:
                current_category = category_text
                current_category_description = ""
    
    # Find all h4 headings (individual tests)
    for h4 in soup.find_all('h4', class_='subheading'):
        test_title = h4.get_text().strip()
        
        # Find the description paragraph that follows
        description = ""
        next_element = h4.find_next_sibling()
        if next_element and next_element.name == 'p':
            description = next_element.get_text().strip()
        
        # Find the XML link
        xml_link = h4.find_next('a')
        if xml_link and xml_link.get('href'):
            xml_filename = xml_link.get_text().strip()
            xml_url = base_url + xml_link.get('href')
            
            # Find the image link
            img_link = h4.find_next('a').find('img') if h4.find_next('a') else None
            image_filename = ""
            image_url = ""
            if img_link and img_link.get('src'):
                image_filename = img_link.get('src').split('/')[-1]
                image_url = base_url + img_link.get('src')
            
            test_info = {
                'title': test_title,
                'description': description,
                'category': current_category,
                'category_description': current_category_description,
                'xml_url': xml_url,
                'xml_filename': xml_filename,
                'image_url': image_url,
                'image_filename': image_filename,
                'test_number': extract_test_number(test_title)
            }
            
            tests.append(test_info)
    
    # Sort by test number
    tests.sort(key=lambda x: x['test_number'])
    
    # Save comprehensive metadata
    metadata_path = Path("lilypond_tests/comprehensive_test_metadata.json")
    metadata_path.parent.mkdir(exist_ok=True)
    
    with open(metadata_path, 'w', encoding='utf-8') as f:
        json.dump(tests, f, indent=2, ensure_ascii=False)
    
    print(f"✅ Extracted metadata for {len(tests)} tests")
    print(f"📁 Saved to: {metadata_path}")
    
    # Create categorized summary
    create_categorized_summary(tests)
    
    return tests

def extract_test_number(title):
    """Extract test number from title like '61a-Lyrics.xml'"""
    match = re.match(r'(\d+)[a-z]', title)
    return int(match.group(1)) if match else 999

def create_categorized_summary(tests):
    """Create a categorized summary of all tests"""
    
    categories = {}
    for test in tests:
        category = test['category']
        if category not in categories:
            categories[category] = []
        categories[category].append(test)
    
    summary_path = Path("lilypond_tests/test_categories_summary.md")
    with open(summary_path, 'w', encoding='utf-8') as f:
        f.write("# LilyPond MusicXML Test Categories Summary\n\n")
        
        for category, test_list in sorted(categories.items()):
            f.write(f"## {category}\n\n")
            if category != test_list[0]['category_description']:
                f.write(f"{test_list[0]['category_description']}\n\n")
            
            for test in test_list:
                f.write(f"### {test['title']}\n")
                f.write(f"**Description:** {test['description']}\n")
                f.write(f"**XML File:** {test['xml_filename']}\n")
                f.write(f"**Image:** {test['image_filename']}\n\n")
    
    print(f"📄 Created categorized summary: {summary_path}")

def update_android_test_runner():
    """Update the Android test runner with proper test metadata"""
    
    metadata_path = Path("lilypond_tests/comprehensive_test_metadata.json")
    if not metadata_path.exists():
        print("❌ Metadata file not found. Run extract_lilypond_metadata() first.")
        return
    
    with open(metadata_path, 'r', encoding='utf-8') as f:
        tests = json.load(f)
    
    # Update test constants in Android test runner
    android_test_path = Path("TrackPlay/app/src/test/java/dev/pola/vexflow/LilyPondTestRunner.kt")
    if not android_test_path.exists():
        print("❌ Android test runner not found.")
        return
    
    # Read existing test runner
    with open(android_test_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extract unique test prefixes for categories
    test_prefixes = {}
    for test in tests:
        prefix = test['title'].split('-')[0]
        if prefix not in test_prefixes:
            test_prefixes[prefix] = []
        test_prefixes[prefix].append(test)
    
    # Generate new test constants
    constants_lines = []
    for prefix, test_list in sorted(test_prefixes.items()):
        constant_name = prefix.upper() + "_TESTS"
        test_titles = [f'"{t["title"]}"' for t in test_list]
        constants_lines.append(f'        private val {constant_name} = listOf({", ".join(test_titles)})')
    
    # Update the companion object
    companion_start = content.find('companion object {')
    companion_end = content.find('\n    }', companion_start)
    
    if companion_start != -1 and companion_end != -1:
        before_constants = content[:companion_start + len('companion object {')]
        after_constants = content[companion_end:]
        
        new_constants = '\n'.join(constants_lines) + '\n'
        
        updated_content = before_constants + new_constants + after_constants
        
        with open(android_test_path, 'w', encoding='utf-8') as f:
            f.write(updated_content)
        
        print(f"✅ Updated Android test runner with {len(test_prefixes)} test categories")
    else:
        print("❌ Could not find companion object in Android test runner")

if __name__ == "__main__":
    extract_lilypond_metadata()
    update_android_test_runner()
