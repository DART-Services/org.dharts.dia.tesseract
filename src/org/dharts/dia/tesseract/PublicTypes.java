/* File: PublicTypes.java
 * Created: 26 August 2012
 * Author: Neal Audenaert
 * 
 * Copyright 2012 Digital Archives, Research & Technology Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dharts.dia.tesseract;

import org.dharts.dia.tesseract.tess4j.TessAPI;

/**
 * Defines enums and other static constants to mirror those defined by 'publictypes.h' in the 
 * core Tesseract code. Comments and names have been adapted directly from the original source 
 * code. 
 * 
 * @author Neal Audenaert
 */
public interface PublicTypes {
    
    /**
     * When Tesseract/Cube is initialized we can choose to instantiate/load/run only the 
     * Tesseract part, only the Cube part or both along with the combiner. The preference of 
     * which engine to use is stored in <code>tessedit_ocr_engine_mode</code>.
     */
    enum OcrEngineMode {
        /** Run Tesseract only - fastest */
        TESSERACT_ONLY(TessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY),

        /** Run Cube only - better accuracy, but slower. */
        CUBE_ONLY(TessAPI.TessOcrEngineMode.OEM_CUBE_ONLY),

        /** Run both and combine results - best accuracy */
        TESSERACT_CUBE_COMBINED(TessAPI.TessOcrEngineMode.OEM_TESSERACT_CUBE_COMBINED),

        /**
         * Specify this mode when calling <code>init_*()</code>, to indicate that any of the 
         * above modes should be automatically inferred from the variables in the 
         * language-specific config, command-line configs, or if not specified in any of the 
         * above should be set to the default <code>OEM_TESSERACT_ONLY</code>.
         */
        DEFAULT(TessAPI.TessOcrEngineMode.OEM_DEFAULT);
        
        public final int value;
        OcrEngineMode(int ord) {
            value = ord;
        }
    }
    
    /**
     * Possible modes for page layout analysis. The values of these enums are kept in order 
     * of decreasing amount of layout analysis to be done, except for <code>OSD_ONLY</code>,
     * so that the inequality test macros below work.
     */
    enum PageSegMode {
        /** Orientation and script detection only. */
        OSD_ONLY(TessAPI.TessPageSegMode.PSM_OSD_ONLY),
        
        /** Automatic page segmentation with orientation and script detection. (OSD) */
        AUTO_OSD(TessAPI.TessPageSegMode.PSM_AUTO_OSD),

        /** Automatic page segmentation, but no OSD, or OCR.  */
        AUTO_ONLY(TessAPI.TessPageSegMode.PSM_AUTO_ONLY),

        /** Fully automatic page segmentation, but no OSD. */
        AUTO(TessAPI.TessPageSegMode.PSM_AUTO),

        /** Assume a single column of text of variable sizes. */
        SINGLE_COLUMN(TessAPI.TessPageSegMode.PSM_SINGLE_COLUMN),

        /** Assume a single uniform block of vertically aligned text. */
        SINGLE_BLOCK_VERT_TEXT(TessAPI.TessPageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT),

        /** Assume a single uniform block of text. (Default.) */
        SINGLE_BLOCK(TessAPI.TessPageSegMode.PSM_SINGLE_BLOCK),
        
        /** Treat the image as a single text line. */
        SINGLE_LINE(TessAPI.TessPageSegMode.PSM_SINGLE_LINE),
        
        /** Treat the image as a single word. */
        SINGLE_WORD(TessAPI.TessPageSegMode.PSM_SINGLE_WORD),
        
        /** Treat the image as a single word in a circle. */
        CIRCLE_WORD(TessAPI.TessPageSegMode.PSM_CIRCLE_WORD),

        /** Treat the image as a single character. */
        SINGLE_CHAR(TessAPI.TessPageSegMode.PSM_SINGLE_CHAR);

        public final int value;
        PageSegMode(int ord) {
            value = ord;
        }
        
        // TODO implement inequality test
    }
    
    /**
     * Possible types for a POLY_BLOCK or ColPartition.
     */
    enum PolyBlockType {
        /** Type is not yet known. Keep as the first element. */
        UNKNOWN(TessAPI.TessPolyBlockType.PT_UNKNOWN),
        
        /** Text that lives inside a column. */
        FLOWING_TEXT(TessAPI.TessPolyBlockType.PT_FLOWING_TEXT),
        
        /** Text that spans more than one column. */
        HEADING_TEXT(TessAPI.TessPolyBlockType.PT_HEADING_TEXT),
        
        /** Text that is in a cross-column pull-out region. */
        PULLOUT_TEXT(TessAPI.TessPolyBlockType.PT_PULLOUT_TEXT),
        
        /** Partition belonging to an equation region. */
//        EQUATION(TessAPI.TessPolyBlockType.PT_EQUATION),
        
        /* Partition has inline equation. Not yet supported by Tess4j. */
//        INLINE_EQUATION(TessAPI.TessPolyBlockType.PT_INLINE_EQUATION),
        
        /* Partition belonging to a table region. Not yet supported by Tess4j. */
        TABLE(TessAPI.TessPolyBlockType.PT_TABLE),
        
        /** Text-line runs vertically. */
        VERTICAL_TEXT(TessAPI.TessPolyBlockType.PT_VERTICAL_TEXT),
        
        /** Text that belongs to an image. */
        CAPTION_TEXT(TessAPI.TessPolyBlockType.PT_CAPTION_TEXT),
        
        /** Image that lives inside a column. */
        FLOWING_IMAGE(TessAPI.TessPolyBlockType.PT_FLOWING_IMAGE),
        
        /** Image that spans more than one column. */
        HEADING_IMAGE(TessAPI.TessPolyBlockType.PT_HEADING_IMAGE),  
        
        /** Image that is in a cross-column pull-out region. */
        PULLOUT_IMAGE(TessAPI.TessPolyBlockType.PT_PULLOUT_IMAGE),
        
        /** Horizontal Line. */
        HORZ_LINE(TessAPI.TessPolyBlockType.PT_HORZ_LINE),      
        
        /** Vertical Line. */
        VERT_LINE(TessAPI.TessPolyBlockType.PT_VERT_LINE),      
        
        /** Lies outside of any column. */
        NOISE(TessAPI.TessPolyBlockType.PT_NOISE);

        public final int value;
        PolyBlockType(int ord) {
            value = ord;
        }
        
        /** Given that there are several different block types that denote textual content, 
         *  this tests to see if a supplied block type is one of those types. */
        public static boolean isText(PolyBlockType type) {
            return type == FLOWING_TEXT || 
                   type == HEADING_TEXT ||
                   type == PULLOUT_TEXT ||
                   type == VERTICAL_TEXT || 
                   type == CAPTION_TEXT;
        }

        /** Given that there are several different block types that denote image content, 
         *  this tests to see if a supplied block type is one of those types. */
        public static boolean isImage(PolyBlockType type) {
            return type == FLOWING_IMAGE || 
                    type == HEADING_IMAGE ||
                    type == PULLOUT_IMAGE;
        }
    } 
    
    /**
     * <pre>
     *  +------------------+  Orientation Example:
     *  | 1 Aaaa Aaaa Aaaa |  ====================
     *  | Aaa aa aaa aa    |  To left is a diagram of some (1) English and
     *  | aaaaaa A aa aaa. |  (2) Chinese text and a (3) photo credit.
     *  |                2 |
     *  |   #######  c c C |  Upright Latin characters are represented as A and a.
     *  |   #######  c c c |  '<' represents a latin character rotated
     *  | < #######  c c c |      anti-clockwise 90 degrees.
     *  | < #######  c   c |
     *  | < #######  .   c |  Upright Chinese characters are represented C and c.
     *  | 3 #######      c |
     *  +------------------+  NOTA BENE: enum values here should match goodoc.proto
     * </pre>
     *  
     * <p>
     * If you orient your head so that "up" aligns with Orientation,
     * then the characters will appear "right side up" and readable.
     *
     * <p>
     * In the example above, both the English and Chinese paragraphs are oriented
     * so their "up" is the top of the page (page up).  The photo credit is read
     * with one's head turned leftward ("up" is to page left).
     *
     * <p>
     * The values of this enum match the convention of Tesseract's osdetect.h
     */
    enum Orientation {
        UP(TessAPI.TessOrientation.ORIENTATION_PAGE_UP),
        RIGHT(TessAPI.TessOrientation.ORIENTATION_PAGE_RIGHT),
        DOWN(TessAPI.TessOrientation.ORIENTATION_PAGE_DOWN),
        LEFT(TessAPI.TessOrientation.ORIENTATION_PAGE_LEFT);
        

        public final int value;
        Orientation(int ord) {
            value = ord;
        }
    }

    /**
     * The grapheme clusters within a line of text are laid out logically in this direction, 
     * judged when looking at the text line rotated so that its Orientation is "page up".
     *
     * <p>
     * For English text, the writing direction is left-to-right.  For the Chinese text in the 
     * above example, the writing direction is top-to-bottom.
     */
    enum WritingDirection {
        LEFT_TO_RIGHT(TessAPI.TessWritingDirection.WRITING_DIRECTION_LEFT_TO_RIGHT),
        RIGHT_TO_LEFT(TessAPI.TessWritingDirection.WRITING_DIRECTION_RIGHT_TO_LEFT),
        TOP_TO_BOTTOM(TessAPI.TessWritingDirection.WRITING_DIRECTION_TOP_TO_BOTTOM);

        public final int value;
        WritingDirection(int ord) {
            value = ord;
        }
    }

    /**
     * The text lines are read in the given sequence.
     *
     * <p>
     * In English, the order is top-to-bottom. In Chinese, vertical text lines are read 
     * right-to-left.  Mongolian is written in vertical columns top to bottom like Chinese, 
     * but the lines order left-to right.
     *
     * <p>
     * Note that only some combinations make sense.  For example,
     * WRITING_DIRECTION_LEFT_TO_RIGHT implies TEXTLINE_ORDER_TOP_TO_BOTTOM
     */
    enum TextlineOrder {
        LEFT_TO_RIGHT(TessAPI.TessTextlineOrder.TEXTLINE_ORDER_LEFT_TO_RIGHT),
        RIGHT_TO_LEFT(TessAPI.TessTextlineOrder.TEXTLINE_ORDER_RIGHT_TO_LEFT),
        TOP_TO_BOTTOM(TessAPI.TessTextlineOrder.TEXTLINE_ORDER_TOP_TO_BOTTOM);

        public final int value;
        TextlineOrder(int ord) {
            value = ord;
        }
    }

    /**
     * 
     */
    enum ParagraphJustification {

        /** 
         * The alignment is not clearly one of the other options. This could happen, for 
         * example, if there are only one or two lines of text or the text looks like 
         * source code or poetry.
         * <p>
         * NOTE: Fully justified paragraphs (text aligned to both left and right margins
         *       margins) are marked by Tesseract with JUSTIFICATION_LEFT if their text
         *       is written with a left-to-right script and with JUSTIFICATION_RIGHT if
         *       their text is written in a right-to-left script.
         * <p>
         * For text read in vertical lines, "Left" is wherever the starting reading 
         * position is.
         */
        JUSTIFICATION_UNKNOWN,

        /**
         * Each line, except possibly the first, is flush to the same left tab stop.
         */
        JUSTIFICATION_LEFT,

        /**
         * The text lines of the paragraph are centered about a line going
         * down through their middle of the text lines.
         */
        JUSTIFICATION_CENTER,

        /**
         * Each line, except possibly the first, is flush to the same right tab stop.
         */
        JUSTIFICATION_RIGHT
    }
}
