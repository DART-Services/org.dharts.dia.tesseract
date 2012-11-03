/* File: LayoutHandle.java
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

import static org.dharts.dia.tesseract.tess4j.TesseractHandle.toBoolean;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.dharts.dia.tesseract.tess4j.TessAPI.TessPageIterator;
import org.dharts.dia.tesseract.tess4j.TessAPI.TessResultIterator;

/**
 * Wrapper for the low-level handles used to iterate over page structure and recognition 
 * results. This class and its subclasses clean up the portion of the API defined by 
 * {@link TessAPI} related to analysis and recognitions results (i.e. methods of the form 
 * {@code TessPageIterator*} and {@code TessResultIterator*}) and guard the state of these 
 * handles to prevent segmentation faults that result from illegal access, for example, when 
 * trying to use a handle that has already been closed. 
 *   
 * <p>Since Tesseract allows only one set of analysis/recognition results to be active at 
 * any given time, instances of {@link BasePageHandle} must be disposed after use. 
 * {@link TesseractHandle} is responsible for monitoring the {@link BasePageHandle}s that it 
 * creates (with the help of {@link HandleContext} and will not permit operations that modify
 * Tesseract's interally stored analysis/recognition results while a {@link BasePageHandle} 
 * is in use.
 * 
 * <p>While a {@link TesseractHandle} may only have one set of layout analysis or recognition 
 * results at any time, a client may obtain handles to multiple iterators over those results 
 * by cloning an existing handle.  
 * 
 * @see TesseractHandle#analyseLayout()
 * @see TesseractHandle#recognize()
 * @see HandleContext
 * 
 * @param <T> The type of iteration handle this class operates on. Must be either 
 *      {@link TessPageIterator} (for layout analysis results) or {@link TessResultIterator} 
 *      for OCR recognition results.
 *      
 * @author Neal Audenaert
 */
public class BasePageHandle<T extends TessPageIterator> {
    // TODO need smarter locking strategy. I need to ensure that the handle isn't closed while
    //      executing methods, but I don't need to prevent the concurrent execution of methods

    protected final HandleContext<T> context;
    protected final T iterator;
    private boolean closed = false;
    
    protected BasePageHandle(HandleContext<T> ctx, T handle) {
        this.context = ctx;
        this.iterator = handle;
    }

    /**
     * Disposes this handle. Once all handles to the same set of analysis/recognition results
     * have been disposed, the {@link TesseractHandle} will permit operations that change 
     * its internal representation of the current analysis/recognition results. 
     */
    public void dispose() {
        synchronized (this) {
            checkClosed();
            this.context.release(iterator);
            closed = true;
        }
    }
    
    /**
     * @throws {@link IllegalStateException} If this handle has been closed.
     */
    protected void checkClosed() {
        if (this.closed)
            throw new IllegalStateException("Layout handle has already been closed.");
    }
    
    /**
     * Moves the iterator to point to the start of the page to begin an iteration. Note that,
     * after construction this method has already been called, so it is not necessary to call
     * it prior to working with an iterator. It can, however, be used to reset the iterator
     * after processing has begun, if desired.
     * 
     * @throws IllegalStateException if this handle has been closed.
     */
    public void begin() {
        synchronized (this) {
            checkClosed();
            context.getAPI().TessPageIteratorBegin(iterator);
        }
    }

    /**
     * Moves to the start of the next object in reading order at the given level in the page 
     * hierarchy and returns {@code false} if the end of the page was reached.
     * 
     * <p>
     * NOTE that, at the symbol level, this will skip non-text blocks, but all other 
     * level values will visit each non-text block once. Think of non text blocks as containing 
     * a single paragraph, with a single line, with a single imaginary word.
     * 
     * <p>
     * Calls to {@code next} with different levels may be freely intermixed. This function 
     * iterates words in right-to-left scripts correctly, if the appropriate language has 
     * been loaded into Tesseract.
     *
     * @param level The level in the page hierarchy for the next element to be retrieved.
     * @return {@code false} if the end of the page was reached, {@code true} if 
     *      the iterator can be called again (at this level). 
     *      
     * @throws IllegalStateException if this handle has been closed.
     * @throws IllegalArgumentException if Tesseract returns a bad response value.
     */
    public boolean next(int level) {
        synchronized (this) {
            checkClosed();
            int atEnd = context.getAPI().TessPageIteratorNext(iterator, level);
            return toBoolean(atEnd);
        }
    }

    /**
     * Returns {@code true} if the iterator is at the start of an object at the given level.
     *
     * <p>
     * For instance, suppose an iterator it is pointed to the first symbol of the
     * first word of the third line of the second paragraph of the first block in
     * a page, then:
     * 
     * <pre>
     *   it.IsAtBeginningOf(BLOCK) = false
     *   it.IsAtBeginningOf(PARA) = false
     *   it.IsAtBeginningOf(TEXTLINE) = true
     *   it.IsAtBeginningOf(WORD) = true
     *   it.IsAtBeginningOf(SYMBOL) = true
     * </pre>
     * 
     * @param level The level of the page hierarchy to consider when evaluating whether or 
     *      not this iterator is at the beginning.
     * @return {@code true} if this iterator is at the start of an object at the given
     *      level of the page hierarchy. 
     *      
     * @throws IllegalStateException if this handle has been closed.
     * @throws IllegalArgumentException if Tesseract returns a bad response value.
     */
    public boolean isAtBeginningOf(int level) {
        synchronized (this) {
            checkClosed();
            int result = context.getAPI().TessPageIteratorIsAtBeginningOf(iterator, level);
            return toBoolean(result);
        }
    }

    /**
     * Returns whether the iterator is positioned at the last element in a given level. (e.g. 
     * the last word in a line, the last line in a block). Given the following text: 
     *
     * <pre>
     *     Here's some two-paragraph example
     *   text.  It starts off innocuously
     *   enough but quickly turns bizarre.
     *     The author inserts a cornucopia
     *   of words to guard against confused
     *   references.
     * </pre>
     * 
     * <p>
     * Now take an iterator it pointed to the start of "bizarre." The following will hold:
     * <pre>
     *  it.isAtFinalElement(PARA, SYMBOL) = false
     *  it.isAtFinalElement(PARA, WORD) = true
     *  it.isAtFinalElement(BLOCK, WORD) = false
     * </pre>
     *
     * @param level Specifies the level in the page hierarchy with respect to which the current
     *      element should be evaluated. For example, in the case that we want to know if the 
     *      current word is the last word in a paragraph, this should be the value for paragraph. 
     * @param element The type of the element. This specifies how to identify the current 
     *      element to be evaluated. For example, in the case that we want to know if the  
     *      current word is the last word in a paragraph, this should be the value for word. 
     * @return {@code true} if the current element of the specified type is the last in 
     *      the indicate page structure; {@code false} otherwise.
     *      
     * @throws IllegalStateException if this handle has been closed.
     * @throws IllegalArgumentException if Tesseract returns a bad response value.
     */
    public boolean isAtFinalElement(int level, int element) {
        synchronized (this) {
            checkClosed();
            int result = context.getAPI().TessPageIteratorIsAtFinalElement(iterator, level, element);
            return toBoolean(result);
        }
    }

    /**
     * @param level The for which the current bounding box should be returned. 
     * @param left Buffer for the left hand side of the bounding box 
     * @param top Buffer for the top of the bounding box
     * @param right Buffer for the right hand side of the bounding box
     * @param bottom Buffer for the bottom of the bounding box
     * 
     * @return {@code true} if the bounding box was successfully obtained, {@code false} it 
     *      no bounding box could be obtained for this level of the hierarchy.
     *      
     * @throws IllegalStateException if this handle has been closed.
     * @throws IllegalArgumentException if Tesseract returns a bad response value.
     */
    public boolean getBoundingBox(int level, IntBuffer left, IntBuffer top, IntBuffer right, IntBuffer bottom) {
        synchronized (this) {
            checkClosed();
            int exists = context.getAPI().TessPageIteratorBoundingBox(
                    iterator, level, left, top, right, bottom);
            return toBoolean(exists);
        }
    }

    /**
     * @return the type of the current block. 
     *      
     * @throws IllegalStateException if this handle has been closed.
     */
    public int getBlockType() {
        synchronized (this) {
            checkClosed();
            return context.getAPI().TessPageIteratorBlockType(iterator);
        }
    }

    /**
     * @param level The level in the hierarchy for which the baseline should be obtained.
     * @param x1 The x coordinate of the first end-point of the baseline.
     * @param y1 The y coordinate of the first end-point of the baseline.
     * @param x2 The x coordinate of the second end-point of the baseline.
     * @param y2 The y coordinate of the second end-point of the baseline.
     * 
     * @return {@code true} if the baseline was successfully obtained, {@code false} if 
     *      it could be obtained for this level of the hierarchy.
     *      
     * @throws IllegalStateException if this handle has been closed.
     * @throws IllegalArgumentException if Tesseract returns a bad response value.
     */
    public boolean getBaseline(int level, IntBuffer x1, IntBuffer y1, IntBuffer x2, IntBuffer y2) {
    
        synchronized (this) {
            checkClosed();
            int exists = context.getAPI().TessPageIteratorBaseline(
                    iterator, level, x1, y1, x2, y2);
            return toBoolean(exists);
        }
    }

    /**
     * Retrieves orientation for the block the iterator points to. Unlike other methods to 
     * query the page layout details, this does not take a {@code level} argument as it
     * always operates at the top-level of the page hierarchy (blocks).
     * 
     * @param orientationBuf A buffer to be filled with the value for the orientation.
     * @param writingDirectionBuf A buffer to be filled with the value for the writing direction.
     * @param textlineOrderBuf A buffer to be filed with the textline order.
     * @param deskewAngleBuf A buffer to be filled with the angle of rotation in radians needed  
     *      to deskew the image
     */
    public void getOrientation(IntBuffer orientationBuf, IntBuffer writingDirectionBuf,
            IntBuffer textlineOrderBuf, FloatBuffer deskewAngleBuf) {
        
        synchronized (this) {
            checkClosed();
            context.getAPI().TessPageIteratorOrientation(iterator, 
                    orientationBuf, writingDirectionBuf, textlineOrderBuf, deskewAngleBuf);
        }
    }
}
