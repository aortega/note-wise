# OpenSheetMusicDisplay: Technical Architecture and MusicXML Processing Pipeline

## Table of Contents
1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Core Components](#core-components)
4. [MusicXML Processing Pipeline](#musicxml-processing-pipeline)
5. [Data Models](#data-models)
6. [Rendering Pipeline](#rendering-pipeline)
7. [Performance Optimizations](#performance-optimizations)
8. [Extension Points](#extension-points)
9. [Configuration and Customization](#configuration-and-customization)

## Overview

OpenSheetMusicDisplay (OSMD) is a TypeScript/JavaScript library for rendering MusicXML notation in web browsers. It provides a comprehensive pipeline from MusicXML input to visual output, supporting complex musical notation, interactive features, and multiple rendering backends.

### Key Features
- Full MusicXML 3.1 support
- Compressed MusicXML (.mxl) handling
- Multiple rendering backends (SVG, Canvas)
- Interactive notation with cursor support
- Extensible plugin architecture
- High-quality typography using SMUFL fonts

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    OpenSheetMusicDisplay                      │
│                        (Main API)                           │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                Music Sheet Reader                            │
│            (XML Parsing & Data Model)                       │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              Music Sheet Calculator                          │
│           (Layout & Positioning)                            │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│            Graphical Music Sheet                            │
│         (Graphical Object Hierarchy)                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│           VexFlow Music Sheet Drawer                        │
│            (Visual Rendering)                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              VexFlow Backend                                │
│            (SVG/Canvas Output)                              │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. OpenSheetMusicDisplay (Main Class)

**Location**: `src/OpenSheetMusicDisplay/OpenSheetMusicDisplay.ts`

**Responsibilities**:
- Main API entry point
- File loading and format detection
- Backend management (SVG/Canvas)
- Rendering coordination
- User interaction handling

**Key Methods**:
```typescript
load(content: string | Document | Blob): Promise<{}>
render(): void
setOptions(options: IOSMDOptions): void
```

### 2. MusicSheetReader

**Location**: `src/MusicalScore/ScoreIO/MusicSheetReader.ts`

**Responsibilities**:
- XML parsing and validation
- Data model creation
- Instrument and part management
- Measure-by-measure processing

**Key Classes**:
- `MusicSheetReader`: Main parsing coordinator
- `InstrumentReader`: Part-specific parsing
- `IXmlElement`: XML element wrapper

### 3. MusicSheetCalculator

**Location**: `src/MusicalScore/Graphical/MusicSheetCalculator.ts`

**Responsibilities**:
- Layout calculations
- Position determination
- Collision detection
- System and page formatting

**Key Subclasses**:
- `VexFlowMusicSheetCalculator`: VexFlow-specific implementation

### 4. VexFlowMusicSheetDrawer

**Location**: `src/MusicalScore/Graphical/VexFlow/VexFlowMusicSheetDrawer.ts`

**Responsibilities**:
- Graphical object to VexFlow conversion
- Visual rendering coordination
- Backend interaction
- Drawing optimization

## MusicXML Processing Pipeline

### Stage 1: Input Handling

```typescript
// File format detection and loading
public load(content: string | Document | Blob): Promise<{}> {
    // 1. Handle different input types
    if (content instanceof Blob) {
        // Check if MXL (compressed) or XML
        const mxlFile: MXLFile = new MXLFile(content);
        return mxlFile.tryUnzip().then(() => {
            if (mxlFile.unzipSuccessful) {
                return mxlFile.getXmlString().then((xmlString) => {
                    return self.load(xmlString);
                });
            }
        });
    }
    
    // 2. Parse XML string
    const parser: DOMParser = new DOMParser();
    const xmlDocument: Document = parser.parseFromString(content, "application/xml");
    
    // 3. Validate MusicXML structure
    const scorePartwiseElement: Element = xmlDocument.querySelector("score-partwise");
    
    // 4. Create wrapper for easier navigation
    const score: IXmlElement = new IXmlElement(scorePartwiseElement);
    
    // 5. Process with MusicSheetReader
    const reader: MusicSheetReader = new MusicSheetReader([temposCalculator], this.rules);
    this.sheet = reader.createMusicSheet(score, tempTitle);
}
```

### Stage 2: XML Parsing and Data Model Creation

#### MXL Decompression
```typescript
// MXL file handling
export class MXLHelper {
    public static async jszipToXMLstring(zip: JSZip): Promise<string> {
        // 1. Read container.xml
        let container: string = await zip.file("META-INF/container.xml").async("text");
        
        // 2. Parse container to find root file
        const parser: DOMParser = new DOMParser();
        const doc: Document = parser.parseFromString(container, "text/xml");
        const rootFile: string = doc.getElementsByTagName("rootfile")[0].getAttribute("full-path");
        
        // 3. Extract main MusicXML file
        const xmlText: string = await zip.file(rootFile).async("text");
        return xmlText;
    }
}
```

#### XML Element Navigation
```typescript
export class IXmlElement {
    constructor(elem: Element) {
        this.elem = elem;
        this.name = elem.nodeName.toLowerCase();
        this.hasAttributes = elem.hasAttributes();
        this.hasElements = elem.hasChildNodes();
    }
    
    public element(elementName: string): IXmlElement {
        // Get first child element with given name
    }
    
    public elements(nodeName?: string): IXmlElement[] {
        // Get all child elements with given name
    }
    
    public attribute(attributeName: string): IXmlAttribute {
        // Get attribute value
    }
}
```

### Stage 3: Musical Data Model Creation

#### Instrument and Part Processing
```typescript
private createInstrumentGroups(entryList: IXmlElement[]): { [_: string]: Instrument } {
    const instrumentDict: { [_: string]: Instrument } = {};
    
    for (const node of entryList) {
        if (node.name === "score-part") {
            const instrIdString: string = node.attribute("id").value;
            const instrument: Instrument = new Instrument(instrumentId, instrIdString, this.musicSheet, currentGroup);
            
            // Parse part-name, part-abbreviation
            // Parse score-instrument and midi-instrument
            // Handle sub-instruments and MIDI mappings
            
            instrumentDict[instrIdString] = instrument;
        }
    }
    
    return instrumentDict;
}
```

#### Measure Processing
```typescript
private _createMusicSheet(root: IXmlElement, path: string): MusicSheet {
    // 1. Parse metadata (title, composer, etc.)
    this.pushSheetLabels(root, path);
    
    // 2. Create instruments from part-list
    const partlistNode: IXmlElement = root.element("part-list");
    const partList: IXmlElement[] = partlistNode.elements();
    const instrumentDict: { [_: string]: Instrument } = this.createInstrumentGroups(partList);
    
    // 3. Process measures
    const partInst: IXmlElement[] = root.elements("part");
    this.initializeReading(partList, partInst, instrumentReaders);
    
    // 4. Measure-by-measure processing loop
    let couldReadMeasure: boolean = true;
    while (couldReadMeasure) {
        this.currentMeasure = new SourceMeasure(this.completeNumberOfStaves, this.musicSheet.Rules);
        
        for (const instrumentReader of instrumentReaders) {
            couldReadMeasure = couldReadMeasure && instrumentReader.readNextXmlMeasure(
                this.currentMeasure, this.currentFraction, octavePlusOneEncoding);
        }
        
        if (couldReadMeasure) {
            this.musicSheet.addMeasure(this.currentMeasure);
            this.setSourceMeasureDuration(instrumentReaders, sourceMeasureCounter);
            this.currentMeasure.AbsoluteTimestamp = this.currentFraction.clone();
            this.currentFraction.Add(this.currentMeasure.Duration);
        }
    }
}
```

### Stage 4: Graphical Object Creation

#### Layout Calculation
```typescript
public prepareGraphicalMusicSheet(): void {
    // 1. Initialize graphical objects
    this.graphicalMusicSheet.Initialize();
    
    // 2. Create accidental calculators for each staff
    const accidentalCalculators: AccidentalCalculator[] = this.createAccidentalCalculators();
    
    // 3. Process each source measure
    for (let idx: number = 0; idx < musicSheet.SourceMeasures.length; idx++) {
        const sourceMeasure: SourceMeasure = musicSheet.SourceMeasures[idx];
        
        // Create graphical measures for this source measure
        const graphicalMeasures: GraphicalMeasure[] = this.createGraphicalMeasuresForSourceMeasure(
            sourceMeasure, accidentalCalculators, lyricWords, openOctaveShifts, activeClefs);
        
        measureList.push(graphicalMeasures);
    }
    
    // 4. Handle staff entries and vertical containers
    this.handleStaffEntries(staffIsPercussionArray);
    this.calculateVerticalContainersList();
    this.setIndicesToVerticalGraphicalContainers();
}
```

#### Voice and Note Processing
```typescript
protected handleVoiceEntry(voiceEntry: VoiceEntry, graphicalStaffEntry: GraphicalStaffEntry,
                           accidentalCalculator: AccidentalCalculator, openLyricWords: LyricWord[],
                           activeClef: ClefInstruction, openTuplets: Tuplet[], openBeams: Beam[],
                           octaveShiftValue: OctaveEnum, staffIndex: number): OctaveEnum {
    
    // 1. Create graphical voice entry
    const gve: GraphicalVoiceEntry = graphicalStaffEntry.findOrCreateGraphicalVoiceEntry(voiceEntry);
    
    // 2. Process each note
    for (let idx: number = 0; idx < voiceEntry.Notes.length; idx++) {
        const note: Note = voiceEntry.Notes[idx];
        
        let graphicalNote: GraphicalNote;
        if (voiceEntry.IsGrace) {
            graphicalNote = MusicSheetCalculator.symbolFactory.createGraceNote(note, gve, activeClef, this.rules, octaveShiftValue);
        } else {
            graphicalNote = MusicSheetCalculator.symbolFactory.createNote(note, gve, activeClef, octaveShiftValue, this.rules, undefined);
        }
        
        // 3. Check for accidentals
        if (note.Pitch) {
            this.checkNoteForAccidental(graphicalNote, accidentalCalculator, activeClef, octaveShiftValue);
        }
        
        // 4. Handle beams and tuplets
        if (note.NoteBeam !== undefined && note.PrintObject) {
            this.handleBeam(graphicalNote, note.NoteBeam, openBeams);
        }
        
        if (note.NoteTuplet !== undefined && note.PrintObject) {
            this.handleTuplet(graphicalNote, note.NoteTuplet, openTuplets);
        }
        
        graphicalStaffEntry.addGraphicalNoteToListAtCorrectYPosition(gve, graphicalNote);
    }
}
```

## Data Models

### Source Data Models

#### MusicSheet
```typescript
export class MusicSheet {
    public Title: Label;
    public Composer: Label;
    public Subtitle: Label;
    public Lyricist: Label;
    public Copyright: Label;
    public Instruments: Instrument[];
    public InstrumentalGroups: InstrumentalGroup[];
    public SourceMeasures: SourceMeasure[];
    public Rules: EngravingRules;
    public MeasureWidthFactor: number;
}
```

#### SourceMeasure
```typescript
export class SourceMeasure {
    public MeasureNumber: number;
    public Duration: Fraction;
    public AbsoluteTimestamp: Fraction;
    public VerticalSourceStaffEntryContainers: VerticalSourceStaffEntryContainer[];
    public FirstInstructionsStaffEntries: SourceStaffEntry[];
    public LastInstructionsStaffEntries: SourceStaffEntry[];
    public StaffLinkedExpressions: MultiExpression[][];
    public TempoExpressions: MultiTempoExpression[];
    public FirstRepetitionInstructions: RepetitionInstruction[];
    public ImplicitMeasure: boolean;
}
```

#### VoiceEntry
```typescript
export class VoiceEntry {
    public Timestamp: Fraction;
    public Notes: Note[];
    public Articulations: Articulation[];
    public TechnicalInstructions: TechnicalInstruction[];
    public LyricsEntries: Dictionary<string, LyricsEntry>;
    public OrnamentContainer: OrnamentContainer;
    public StemDirection: StemDirectionType;
    public WantedStemDirection: StemDirectionType;
}
```

### Graphical Data Models

#### GraphicalMusicSheet
```typescript
export class GraphicalMusicSheet {
    public ParentMusicSheet: MusicSheet;
    public Title: GraphicalLabel;
    public Subtitle: GraphicalLabel;
    public Composer: GraphicalLabel;
    public Lyricist: GraphicalLabel;
    public Copyright: GraphicalLabel;
    public MusicPages: GraphicalMusicPage[];
    public MeasureList: GraphicalMeasure[][];
    public VerticalGraphicalStaffEntryContainers: VerticalGraphicalStaffEntryContainer[];
}
```

#### GraphicalMeasure
```typescript
export abstract class GraphicalMeasure extends GraphicalObject {
    public parentSourceMeasure: SourceMeasure;
    public staffEntries: GraphicalStaffEntry[];
    public beginInstructionsWidth: number;
    public endInstructionsWidth: number;
    public minimumStaffEntriesWidth: number;
    public hasOnlyRests: boolean;
    public hasError: boolean;
}
```

#### GraphicalNote
```typescript
export class GraphicalNote extends GraphicalObject {
    public sourceNote: Note;
    public PositionAndShape: BoundingBox;
    public Accidental: GraphicalAccidental;
    public DotPositions: number[];
    public Flag: GraphicalFlag;
    public Stem: GraphicalStem;
    public Notehead: GraphicalNotehead;
}
```

## Rendering Pipeline

### Stage 5: VexFlow Conversion

#### VexFlowMusicSheetDrawer
```typescript
export class VexFlowMusicSheetDrawer extends MusicSheetDrawer {
    private backend: VexFlowBackend;
    private backends: VexFlowBackend[] = [];
    private zoom: number = 1.0;
    
    public drawSheet(graphicalMusicSheet: GraphicalMusicSheet): void {
        // 1. Configure VexFlow defaults
        (Vex.Flow as any).STAVE_LINE_THICKNESS = this.rules.StaffLineWidth * unitInPixels;
        (Vex.Flow as any).STEM_WIDTH = this.rules.StemWidth * unitInPixels;
        (Vex.Flow as any).DEFAULT_NOTATION_FONT_SCALE = this.rules.VexFlowDefaultNotationFontScale;
        
        // 2. Process each page
        for (const graphicalMusicPage of graphicalMusicSheet.MusicPages) {
            const backend: VexFlowBackend = this.backends[page.PageNumber - 1];
            backend.graphicalMusicPage = graphicalMusicPage;
            backend.scale(this.zoom);
        }
        
        // 3. Delegate to base class for actual drawing
        super.drawSheet(graphicalMusicSheet);
    }
}
```

#### Measure Rendering
```typescript
protected drawMeasure(measure: VexFlowMeasure): void {
    // 1. Set absolute coordinates
    measure.setAbsoluteCoordinates(
        measure.PositionAndShape.AbsolutePosition.x * unitInPixels,
        measure.PositionAndShape.AbsolutePosition.y * unitInPixels
    );
    
    // 2. Draw the measure using VexFlow
    try {
        measure.draw(this.backend.getContext());
    } catch (ex) {
        log.warn("VexFlowMusicSheetDrawer.drawMeasure", ex);
    }
    
    // 3. Draw additional elements (buzz rolls, etc.)
    for (const staffEntry of measure.staffEntries) {
        this.drawStaffEntry(staffEntry);
        this.drawBuzzRolls(staffEntry, newBuzzRollId);
    }
}
```

### Stage 6: Backend Rendering

#### SVG Backend
```typescript
export class SvgVexFlowBackend extends VexFlowBackend {
    public renderText(height: number, fontStyle: FontStyles, font: string, text: string,
                     fontHeightInPixel: number, position: PointF2D, color: string, fontFamily: string): Node {
        const textElement: SVGTextElement = document.createElementNS("http://www.w3.org/2000/svg", "text");
        textElement.setAttribute("x", position.x.toString());
        textElement.setAttribute("y", position.y.toString());
        textElement.setAttribute("font-size", fontHeightInPixel.toString());
        textElement.setAttribute("font-family", fontFamily);
        textElement.setAttribute("fill", color);
        textElement.textContent = text;
        
        this.container.appendChild(textElement);
        return textElement;
    }
}
```

#### Canvas Backend
```typescript
export class CanvasVexFlowBackend extends VexFlowBackend {
    public renderText(height: number, fontStyle: FontStyles, font: string, text: string,
                     fontHeightInPixel: number, position: PointF2D, color: string, fontFamily: string): Node {
        const ctx: CanvasRenderingContext2D = this.getContext() as CanvasRenderingContext2D;
        ctx.font = `${fontStyle} ${fontHeightInPixel}px ${fontFamily}`;
        ctx.fillStyle = color;
        ctx.fillText(text, position.x, position.y);
        
        // Return dummy node for consistency
        return document.createElement("div");
    }
}
```

## Performance Optimizations

### 1. Sky/Bottom Line Calculation
```typescript
export class SkyBottomLineCalculator {
    private skyLine: number[] = [];
    private bottomLine: number[] = [];
    
    public updateSkyLineInRange(startX: number, endX: number, value: number): void {
        const startIndex: number = Math.floor(startX * this.samplingUnit);
        const endIndex: number = Math.ceil(endX * this.samplingUnit);
        
        for (let i: number = startIndex; i <= endIndex; i++) {
            if (i < this.skyLine.length) {
                this.skyLine[i] = Math.min(this.skyLine[i], value);
            }
        }
    }
}
```

### 2. Batch Processing
```typescript
// Batch calculation for multiple measures
public calculateSkyBottomLines(): void {
    for (const musicSystem of this.musicSystems) {
        for (const staffLine of musicSystem.StaffLines) {
            const calculator: SkyBottomLineCalculator = staffLine.SkyBottomLineCalculator;
            
            // Process all measures in batch
            for (const measure of staffLine.Measures) {
                for (const staffEntry of measure.staffEntries) {
                    calculator.updateFromStaffEntry(staffEntry);
                }
            }
        }
    }
}
```

### 3. Lazy Loading
```typescript
// Only create graphical objects when needed
public getGraphicalMeasureFromSourceMeasureAndIndex(sourceMeasure: SourceMeasure, staffIndex: number): GraphicalMeasure {
    const measureListIndex: number = sourceMeasure.measureListIndex;
    
    if (!this.MeasureList[measureListIndex] || !this.MeasureList[measureListIndex][staffIndex]) {
        // Create on demand
        this.MeasureList[measureListIndex] = this.MeasureList[measureListIndex] || [];
        this.MeasureList[measureListIndex][staffIndex] = this.createGraphicalMeasure(sourceMeasure, staffIndex);
    }
    
    return this.MeasureList[measureListIndex][staffIndex];
}
```

## Extension Points

### 1. Symbol Factory
```typescript
export interface IGraphicalSymbolFactory {
    createNote(note: Note, gve: GraphicalVoiceEntry, activeClef: ClefInstruction, 
               octaveShiftValue: OctaveEnum, rules: EngravingRules, stemDirection?: StemDirectionType): GraphicalNote;
    createGraceNote(note: Note, gve: GraphicalVoiceEntry, activeClef: ClefInstruction, 
                     octaveShiftValue: OctaveEnum, rules: EngravingRules): GraphicalNote;
    createStaffEntry(sourceStaffEntry: SourceStaffEntry, measure: GraphicalMeasure): GraphicalStaffEntry;
}
```

### 2. Reader Plugins
```typescript
export interface IVoiceMeasureReadPlugin {
    getName(): string;
    readVoiceMeasure(measure: IXmlElement, sourceMeasure: SourceMeasure, 
                     voice: Voice, currentTimestamp: Fraction): boolean;
}
```

### 3. After-Sheet Modules
```typescript
export interface IAfterSheetReadingModule {
    calculate(musicSheet: MusicSheet): void;
}
```

## Configuration and Customization

### EngravingRules
```typescript
export class EngravingRules {
    // Layout settings
    public StaffHeight: number = 10.0;
    public StaffLineWidth: number = 0.15;
    public StemWidth: number = 0.15;
    
    // Spacing settings
    public MinimumDistance: number = 0.5;
    public RhythmRightMargin: number = 0.2;
    public LyricsYMarginToBottomLine: number = 0.2;
    
    // Rendering settings
    public DefaultColorMusic: string = "#000000";
    public DefaultFontFamily: string = "Times New Roman";
    public DefaultFontStyle: FontStyles = FontStyles.Regular;
    
    // VexFlow specific
    public VexFlowDefaultNotationFontScale: number = 39;
    public VexFlowDefaultTabFontScale: number = 39;
    public DefaultVexFlowNoteFont: string = "bravura";
}
```

### OSMDOptions
```typescript
export interface IOSMDOptions {
    // Backend options
    backend?: string; // 'svg' | 'canvas'
    autoResize?: boolean;
    
    // Rendering options
    drawTitle?: boolean;
    drawComposer?: boolean;
    drawLyrics?: boolean;
    drawMeasureNumbers?: boolean;
    
    // Layout options
    followCursor?: boolean;
    coloringMode?: ColoringModes;
    coloringEnabled?: boolean;
    
    // Advanced options
    pageFormat?: string;
    zoom?: number;
    defaultColorMusic?: string;
}
```

## Error Handling and Validation

### XML Validation
```typescript
private _createMusicSheet(root: IXmlElement, path: string): MusicSheet {
    try {
        // Validate root element
        if (!root) {
            throw new MusicSheetReadingException("Undefined root element");
        }
        
        const partlistNode: IXmlElement = root.element("part-list");
        if (!partlistNode) {
            throw new MusicSheetReadingException("Undefined partListNode");
        }
        
        // Continue processing...
        
    } catch (e) {
        log.error("MusicSheetReader.CreateMusicSheet", e);
        return undefined;
    }
}
```

### Rendering Error Recovery
```typescript
protected drawMeasure(measure: VexFlowMeasure): void {
    try {
        measure.draw(this.backend.getContext());
    } catch (ex) {
        log.warn("VexFlowMusicSheetDrawer.drawMeasure", ex);
        // Continue with next measure instead of failing completely
    }
}
```

## Memory Management

### Object Cleanup
```typescript
public clear(): void {
    for (const backend of this.backends) {
        backend.clear();
    }
    
    // Clear graphical objects
    this.graphicalMusicSheet = undefined;
    this.sheet = undefined;
}

protected clearRecreatedObjects(): void {
    // Clear ties that will be recalculated
    for (const staffEntry of this.staffEntriesWithGraphicalTies) {
        staffEntry.GraphicalTies.length = 0;
    }
    this.staffEntriesWithGraphicalTies.length = 0;
}
```

This technical architecture provides a robust foundation for music notation rendering, with clear separation of concerns, extensibility points, and performance optimizations suitable for complex musical scores.
