/* File: ResultsHandle.java
 * Created: Oct 27, 2012
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
package org.dharts.dia.tesseract.tess4j;

import java.nio.IntBuffer;

import org.dharts.dia.tesseract.tess4j.TessAPI.TessResultIterator;


import static org.dharts.dia.tesseract.tess4j.TesseractHandle.toBoolean;

/**
 * Implements {@link BasePageHandle} for use with OCR results.
 */
public class ResultHandle extends BasePageHandle<TessResultIterator> {
    
    /**
     * Creates a new {@link ResultHandle}. This is intended to be called by 
     * {@link TesseractHandle}.
     *  
     * @param ctx The initial context for this handle. 
     * @param handle The low-level handle to the Tesseract result iterator.
     * @return The created {@link ResultHandle} 
     */
    static ResultHandle create(HandleContext<TessResultIterator> ctx, TessResultIterator handle) {
        return new ResultHandle(ctx, handle);
    }
    
    /**
     * Copies a {@link ResultHandle}. 
     * @param handle The handle to copy.
     * @return A copy of the supplied handle. 
     */
    public static ResultHandle copy(BasePageHandle<TessResultIterator> handle) {
        TessResultIterator newHandle = handle.context.copy(handle.iterator);
        return new ResultHandle(handle.context, newHandle);
    }
    
    private ResultHandle(HandleContext<TessResultIterator> ctx, TessResultIterator handle) {
        super(ctx, handle);
    }

    /**
     * Returns the recognized text for the current object at the given level.
     * 
     * @param level The level in the page hierarchy to evaluate.
     * @return The recognized text of the current object.
     * 
     * @throws IllegalStateException if the handle has been closed
     */
    public String getUTF8Text(int level) {
        // FIXME This returns a string and will result in a memory leak
        synchronized (this) {
            checkClosed();
            return context.getAPI().TessResultIteratorGetUTF8Text(iterator, level);
        }
    }

    /** 
     * Returns the mean confidence of the current object at the given level. The number 
     * should be interpreted as a percent probability. (0.0f-100.0f)
     * 
     * @param level The level in the page hierarchy to evaluate.
     * @return The mean confidence of the current object at the given level.
     * 
     * @throws IllegalStateException if the handle has been closed
     */
    public final float getConfidence(int level) {
        synchronized (this) {
            checkClosed();
            return context.getAPI().TessResultIteratorConfidence(iterator, level);
        }
    }

    /**
     * @param isBold
     * @param isItalic
     * @param isUnderlined
     * @param isMonospace
     * @param isSerif
     * @param isSmallcaps
     * @param pointsize
     * @param fontId
     * @return The name of the font family associated with the current word. This may be
     *      {@code null}
     *      
     * @throws IllegalStateException if the handle has been closed
     */
    public String getWordFontAttributes(IntBuffer isBold, 
                                        IntBuffer isItalic,
                                        IntBuffer isUnderlined, 
                                        IntBuffer isMonospace, 
                                        IntBuffer isSerif,
                                        IntBuffer isSmallcaps, 
                                        IntBuffer pointsize, 
                                        IntBuffer fontId) {
        
       // NOTE Unlike other methods that return a String, the underlying Tessearact API 
       //      documents that this String points to an internal table that will be managed 
       //      in the same lifespan as the iterator itself. Therefore, this string does not
       //      need to be deleted and will not result in a memory leak.
        synchronized (this) {
            checkClosed();
            return context.getAPI().TessResultIteratorWordFontAttributes(iterator, 
                    isBold, isItalic, isUnderlined, isMonospace, isSerif, isSmallcaps, pointsize, fontId);
        }
    }

    /**
     * @return {@code true} if the current word 
     * 
     * @throws IllegalStateException if the handle has been closed
     * @throws IllegalArgumentException If Tesseract returns an invalid boolean value
     */
    public boolean isDictionaryWord() {
        synchronized(this) {
            checkClosed();
            int value = context.getAPI().TessResultIteratorWordIsFromDictionary(iterator);
            return toBoolean(value);
        }
    }

    /**
     * @return {@code true} if the current word is numeric.
     * 
     * @throws IllegalStateException if the handle has been closed
     * @throws IllegalArgumentException If Tesseract returns an invalid boolean value
     */
    public boolean isNumeric() {
        synchronized(this) {
            checkClosed();
            int value = context.getAPI().TessResultIteratorWordIsNumeric(iterator);
            return toBoolean(value);
        }
    }

    /**
     * @return {@code true} if the current symbol is a subscript. .
     * 
     * @throws IllegalStateException if the handle has been closed
     * @throws IllegalArgumentException If Tesseract returns an invalid boolean value
     */
    public boolean isSubscript() {
        synchronized(this) {
            checkClosed();
            int value = context.getAPI().TessResultIteratorSymbolIsSubscript(iterator);
            return toBoolean(value);
        }
    }
    
    /**
     * @return {@code true} if the current symbol is a superscript.
     * 
     * @throws IllegalStateException if the handle has been closed
     * @throws IllegalArgumentException If Tesseract returns an invalid boolean value
     */
    public boolean isSuperscript() {
        synchronized(this) {
            checkClosed();
            int value = context.getAPI().TessResultIteratorSymbolIsSuperscript(iterator);
            return toBoolean(value);
        }
    }
    
    /**
     * @return {@code true} if the current symbol is a dropcat.
     * 
     * @throws IllegalStateException if the handle has been closed
     * @throws IllegalArgumentException If Tesseract returns an invalid boolean value
     */
    public boolean isDropcap() {
        synchronized(this) {
            checkClosed();
            int value = context.getAPI().TessResultIteratorSymbolIsDropcap(iterator);
            return toBoolean(value);
        }
    }
}
