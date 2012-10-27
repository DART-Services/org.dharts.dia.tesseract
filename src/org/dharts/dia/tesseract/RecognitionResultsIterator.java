/* File: ResultIterator.java
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

import java.nio.IntBuffer;

import org.apache.log4j.Logger;
import org.dharts.dia.tesseract.handles.ReleasableContext;
import org.dharts.dia.tesseract.handles.TesseractHandle;
import org.dharts.dia.tesseract.tess4j.TessAPI;

// FIXME Wrap TessAPI in lower level handle

/**
 * Class to iterate over tesseract results, providing access to all levels of the page 
 * hierarchy, without including any tesseract headers or having to handle any 
 * tesseract structures.
 * 
 * WARNING! This class points to data held within the TessBaseAPI class, and therefore can 
 * only be used while the TessBaseAPI class still exists and has not been subjected to a 
 * call of Init, SetImage, Recognize, Clear, End DetectOS, or anything else that changes 
 * the internal PAGE_RES. See apitypes.h for the definition of PageIteratorLevel. See also 
 * base class PageIterator, which contains the bulk of the interface. LTRResultIterator adds 
 * text-specific methods for access to OCR output.
 * 
 * @author Neal Audenaert
 */
public class RecognitionResultsIterator extends LayoutIterator {
    private static final Logger LOGGER = Logger.getLogger(RecognitionResultsIterator.class);

    
    // FIXME The API docs have been taken more or less directly from the underlying C++ 
    //       code. This API needs some significant clean up as it will result in memory leaks.
    //
    //       Also, the choice of having a single type of iterator to iterate over different 
    //       levels in the page hierarchy needs to be re-worked. In particular, the behavior 
    //       of methods like getFontAttributes() changes depending on they level at which the 
    //       iterator is being used (even though the iterator itself doesn't maintain any 
    //       state about how it is being used). 
    //
    //       This class appears represents word-level functionality (validate this)
    
    private final TessAPI.TessResultIterator iterator;

    public RecognitionResultsIterator(ReleasableContext context, TessAPI.TessResultIterator iterator) {
        // FIXME make this package scoped. It should accept a ResultIteratorHandle (to be 
        //       returned by TesseractHandle 
        super(context, iterator);
        this.iterator = iterator;
    }

//    @Override
//    public TessAPI.TessResultIterator clone() {
//        return context.getAPI().TessResultIteratorCopy(iterator);
//    }

    /**
     * Returns the recognized text for the current object at the given level.
     * 
     * @param level The level in the page hierarchy to evaluate.
     * @return The recognized text of the current object.
     */
    public final String getUTF8Text(Level level) {
        // FIXME This returns a string and will result in a memory leak
        return context.getAPI().TessResultIteratorGetUTF8Text(iterator, level.value);
    }

    /** 
     * Returns the mean confidence of the current object at the given level. The number 
     * should be interpreted as a percent probability. (0.0f-100.0f)
     * 
     * @param level The level in the page hierarchy to evaluate.
     * @return The mean confidence of the current object at the given level.
     */
    public final float getConfidence(Level level) {
        return context.getAPI().TessResultIteratorConfidence(iterator, level.value);
    }

    // ============= Functions that refer to words only ============
    
    /**
     * Returns the font attributes of the current word. If iterating at a higher level 
     * object than words, eg textlines, then this will return the attributes of the first 
     * word in that textline. 
     * 
     * @return the font attributes of the current word.
     */
    public final FontAttributes getWordFontAttributes() {
        IntBuffer isBold = IntBuffer.allocate(1);
        IntBuffer isItalic = IntBuffer.allocate(1);
        IntBuffer isUnderlined = IntBuffer.allocate(1);
        IntBuffer isMonospace = IntBuffer.allocate(1);
        IntBuffer isSerif = IntBuffer.allocate(1);
        IntBuffer isSmallcaps = IntBuffer.allocate(1);
        IntBuffer pointsize = IntBuffer.allocate(1);
        IntBuffer fontId = IntBuffer.allocate(1);

        // NOTE Unlike other methods that return a String, the underlying Tessearact API 
        //      documents that this String points to an internal table that will be managed 
        //      in the same lifespan as the iterator itself. Therefore, this string does not
        //      need to be deleted and will not result in a memory leak.
        String fontName = context.getAPI().TessResultIteratorWordFontAttributes(iterator, 
                isBold, isItalic, isUnderlined, isMonospace, isSerif, isSmallcaps, pointsize, fontId);

        FontAttributeBuilder builder = new FontAttributeBuilder();
        try {
            builder.setIsBold(TesseractHandle.toBoolean(isBold.get()))
                   .setIsItalic(TesseractHandle.toBoolean(isItalic.get()))
                   .setIsUnderline(TesseractHandle.toBoolean(isUnderlined.get()))
                   .setIsMonospace(TesseractHandle.toBoolean(isMonospace.get()))
                   .setIsSerif(TesseractHandle.toBoolean(isSerif.get()))
                   .setIsSmallcaps(TesseractHandle.toBoolean(isSmallcaps.get()))
                   .setPointSize(pointsize.get())
                   .setFontId(fontId.get())
                   .setFontName(fontName);
        } catch (Exception ex) {
            LOGGER.error("Internal Error: Could not retrieve font attributed.", ex);
            throw new RuntimeException(ex);
        }

        return builder.attrs;
    }

    /** @return <code>true</code> if the current word was found in a dictionary. */
    public final boolean isDictionaryWord() {
        int value = context.getAPI().TessResultIteratorWordIsFromDictionary(iterator);
        
        try {
            return TesseractHandle.toBoolean(value);
        } catch (Exception ex) {
            LOGGER.error("Internal Error: Invalid boolean value returned by isDictionaryWord.", ex);
            throw new RuntimeException(ex);
        }
    }
    
    /** @return <code>true</code> if the current word is numeric. */
    public final boolean isNumeric() {
        int value = context.getAPI().TessResultIteratorWordIsNumeric(iterator);
        try {
            return TesseractHandle.toBoolean(value);
        } catch (Exception ex) {
            LOGGER.error("Internal Error: Invalid boolean value returned by isNumeric.", ex);
            throw new RuntimeException(ex);
        }
    }

    //=======================================================================================
    // Functions that refer to symbols only
    //=======================================================================================
    
    // If iterating at a higher level object than symbols, eg words, these methods
    // will return the attributes of the first symbol in that word.
    
    /** @return <code>true</code> if the current symbol is a subscript. */
    public final boolean isSubscript() {
        int value = context.getAPI().TessResultIteratorSymbolIsSubscript(iterator);
        
        try {
            return TesseractHandle.toBoolean(value);
        } catch (Exception ex) {
            LOGGER.error("Internal Error: Invalid boolean value returned by isSubscript.", ex);
            throw new RuntimeException(ex);
        }
    }

    /** @return <code>true</code> if the current symbol is a superscript */
    public final boolean isSuperscript() {
        int value = context.getAPI().TessResultIteratorSymbolIsSuperscript(iterator);
        
        try {
            return TesseractHandle.toBoolean(value);
        } catch (Exception ex) {
            LOGGER.error("Internal Error: Invalid boolean value returned by isSuperscript.", ex);
            throw new RuntimeException(ex);
        }
    }

    /** @return <code>true</code> if the current symbol is a dropcap. */
    public final boolean isDropcap() {
        int value = context.getAPI().TessResultIteratorSymbolIsDropcap(iterator);

        try {
            return TesseractHandle.toBoolean(value);
        } catch (Exception ex) {
            LOGGER.error("Internal Error: Invalid boolean value returned by isDropcap.", ex);
            throw new RuntimeException(ex);
        }
    }

    // TODO not implemented in Tess4J API
    //      WordRecognitionLanguage
    //      WordDirection
    //      HasBlamerInfo
    //      GetParamsTrainingBundle
    //      GetBlamerDebug
    //      GetBlamerMisadaptionDebug
    //      WordTruthUTF8Text
    //      WordLattice
    //
    // Also, No hooks for the ChoiceIterator
    
    //========================================================================================
    // INNER CLASSES
    //========================================================================================
    
    
    /**
     * Used to build the FontAttributes object. Once built, a FontAttribute instance should be
     * immutable.
     * 
     * @author Neal Audenaert
     */
    private static class FontAttributeBuilder {
        FontAttributes attrs = new FontAttributes();

        public FontAttributeBuilder setIsBold(boolean flag) {
            attrs.bold = flag;
            return this;
        }

        public FontAttributeBuilder setIsItalic(boolean flag) {
            attrs.italic = flag;
            return this;
        }

        public FontAttributeBuilder setIsUnderline(boolean flag) {
            attrs.underlined = flag;
            return this;
        }

        public FontAttributeBuilder setIsMonospace(boolean flag) {
            attrs.monospace = flag;
            return this;
        }

        public FontAttributeBuilder setIsSerif(boolean flag) {
            attrs.serif = flag;
            return this;
        }

        public FontAttributeBuilder setIsSmallcaps(boolean flag) {
            attrs.smallcaps = flag;
            return this;
        }

        public FontAttributeBuilder setPointSize(int size) {
            attrs.pointsize = size;
            return this;
        }

        public FontAttributeBuilder setFontId(int id) {
            attrs.fontId = id;
            return this;
        }
        
        public FontAttributeBuilder setFontName(String name) {
            attrs.fontName = name ;
            return this;
        }
    }

    /**
     * Describes the details of the font face used for a particular word.
     *  
     * @author Neal Audenaert
     */
    public static class FontAttributes {
        // FIXME make this really immutable, 
        // NOTE: This is implicitly immutable after it has been built since there are 
        //       no accessible mutators.
        private boolean bold;
        private boolean italic;
        private boolean underlined;
        private boolean monospace;
        private boolean serif;
        private boolean smallcaps;
        private int pointsize;
        private int fontId;
        private String fontName;

        /** @return <code>true</code> if the represented word is bold. */
        public boolean isBold() {
            return bold;
        }

        /** @return <code>true</code> if the represented word is italicized. */
        public boolean isItalic() {
            return italic;
        }

        /** @return <code>true</code> if the represented word is underlined. */
        public boolean isUnderlined() {
            return underlined;
        }

        /** @return <code>true</code> if the represented word is in a monospace font. */
        public boolean isMonospace() {
            return monospace;
        }

        /** @return <code>true</code> if the represented word is in a serif font. */
        public boolean isSerif() {
            return serif;
        }

        /** @return <code>true</code> if the represented word is in small caps. */
        public boolean isSmallcaps() {
            return smallcaps;
        }

        /** @return the pointsize of this word in printers points (1/72 inch.)
         */
        public int getPointsize() {
            return pointsize;
        }

        /** @return the identifier for the font used. */
        public int getFontId() {
            return fontId;
        }
        
        /** @return the name of the font used. */
        public String getFontName() {
            return fontName;
        }
    }
}
