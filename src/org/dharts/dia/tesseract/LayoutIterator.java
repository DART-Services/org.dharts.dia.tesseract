/* File: PageIterator.java
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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.dharts.dia.tesseract.PublicTypes.Orientation;
import org.dharts.dia.tesseract.PublicTypes.PolyBlockType;
import org.dharts.dia.tesseract.PublicTypes.TextlineOrder;
import org.dharts.dia.tesseract.PublicTypes.WritingDirection;
import org.dharts.dia.tesseract.handles.ReleasableContext;
import org.dharts.dia.tesseract.handles.TesseractHandle;
import org.dharts.dia.tesseract.tess4j.TessAPI;

/**
 * Iterates over the recognized structures (not text) in a page or region of interest. This
 * class provides access only to the page layout structure and not to its textual content.
 * For access to the actual OCR results, @see {@link RecognitionResultsIterator}.
 * 
 * <p>
 * In the current incarnation, most of the methods are parameterized by the <code>Level</code> 
 * (e.g., block, paragraph, line, word, symbol) that the iterator should operate at. Future 
 * work will migrate the page structure to a well-defined API and provide iterators that behave 
 * consistently with Java <code>Iterator</code>s. Note that this will result in significant 
 * changes to this API. 
 * 
 * <p>
 * At most one <code>LayoutIterator</code> and <code>RecognitionResultsIterator</code> can 
 * be used at any given time. Consequently, it's required that any <code>LayoutIterator</code>  
 * in use be closed using the {@link #close()} method prior to performing another analysis or 
 * recognition request. If there is a <code>LayoutIterator</code> open, method calls that would 
 * create a new one will throw an exception. 
 * 
 * <p>
 * This constraint is due to the fact that, under the hood, Tesseract's implementations of 
 * the results iterator (<code>PageIterator</code> and <code>ResultsIterator</code>) point
 * to data held in the <code>TessBaseAPI</code> class. If this data is changed (e.g., 
 * by setting a new image to recognize or by re-running recognition of the current image), the
 * open iterator will be in an inconsistent state. Its behavior in this case is not documented
 * and may in unexpected results or errors. Following the pattern of concurrent modification 
 * to iterators and collections, requiring that iterators be explicitly closed provides a  
 * fail-fast strategy in the case of incorrect use rather than failing (possibly silently) at 
 * some arbitrary point in the future.   
 *  
 * <h2>Coordinate system</h2>
 * <p>
 * Integer coordinates are at the cracks between the pixels.
 * 
 * <p>
 * The top-left corner of the top-left pixel in the image is at (0,0). The bottom-right corner 
 * of the bottom-right pixel in the image is at (width, height).
 * 
 * <p>
 * Every bounding box goes from the top-left of the top-left contained pixel to the 
 * bottom-right of the bottom-right contained pixel, so the bounding box of the single 
 * top-left pixel in the image is: (0,0)->(1,1).
 * 
 * <p>
 * If an image rectangle has been set in the API, then returned coordinates relate to the 
 * original (full) image, rather than the rectangle.
 * 
 * 
 * @see {@link RecognitionResultsIterator}
 * @see {@link ImageAnalyzer#analyzeLayout()}
 * @see {@link ImageAnalyzer#analyzeLayout(java.awt.Rectangle)}
 * @author Neal Audenaert
 */
public class LayoutIterator {
    private static final Logger LOGGER = Logger.getLogger(LayoutIterator.class); 
    
    /**
     * The elements of the page hierarchy. Used to enable functions that operate on each 
     * level without having to have 5x as many functions.
     */
    public enum Level {
        /** Block of text/image/separator line. */
        BLOCK(TessAPI.TessPageIteratorLevel.RIL_BLOCK),     
        
        /** Paragraph within a block. */
        PARA(TessAPI.TessPageIteratorLevel.RIL_PARA),
        
        /** Line within a paragraph. */
        TEXTLINE(TessAPI.TessPageIteratorLevel.RIL_TEXTLINE),
        
        /** Word within a textline. */
        WORD(TessAPI.TessPageIteratorLevel.RIL_WORD),
        
        /** Symbol/character within a word. */
        SYMBOL(TessAPI.TessPageIteratorLevel.RIL_SYMBOL);
        
        public final int value;
        Level(int ord) {
            value = ord;
        }
    }
    
    private final TessAPI.TessPageIterator iterator;
    protected final ReleasableContext context;
    private final List<CloseListener<LayoutIterator>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new <code>LayoutIterator</code>
     * 
     * @param analyzer The analyzer responsible for creating this. 
     * @param iterator A pointer to be used to reference the corresponding object in 
     *      the C++ code.
     */
    public LayoutIterator(ReleasableContext context, TessAPI.TessPageIterator iterator) {
        // FIXME make this package scoped. It should accept a PageIteratorHandle (to be 
        //       returned by TesseractHandle 
        this.context = context;
        this.iterator = iterator;
    }

    /**
     * Closes this iterator. Instances must be closed prior to creating a new analyzer.
     */
    public void close() {
        context.release();
        for (CloseListener<LayoutIterator> ears: listeners) {
            ears.closed(this);
        }
        
        listeners.clear();
    }

    public ListenerRegistration onClose(final CloseListener<LayoutIterator> ears) {
        listeners.add(ears);
        
        return new ListenerRegistration() {
            
            @Override
            public void unregister() {
                LayoutIterator.this.listeners.remove(ears);
            }
        };
    }

    /**
     * Moves the iterator to point to the start of the page to begin an iteration. Note that,
     * after construction this method has already been called, so it is not necessary to call
     * it prior to working with an iterator. It can, however, be used to reset the iterator
     * after processing has begun, if desired.
     */
    void begin() {
        context.getAPI().TessPageIteratorBegin(iterator);
    }

    /**
     * Moves to the start of the next object in reading order at the given level in the page 
     * hierarchy and returns <code>false</code> if the end of the page was reached.
     * 
     * <p>
     * NOTE that <code>{@link Level.SYMBOL}</code> will skip non-text blocks, but all other 
     * level values will visit each non-text block once. Think of non text blocks as containing 
     * a single paragraph, with a single line, with a single imaginary word.
     * 
     * <p>
     * Calls to <code>next</code> with different levels may be freely intermixed.
     * This function iterates words in right-to-left scripts correctly, if the appropriate 
     * language has been loaded into Tesseract.
     *
     * @param level The level in the page hierarchy for the next element to be retrieved.
     * @return <code>false</code> if the end of the page was reached, <code>true</code> if 
     *      the iterator can be called again (at this level). 
     */
    public boolean next(Level level) {
        int atEnd = context.getAPI().TessPageIteratorNext(iterator, level.ordinal());
        
        try {
            return TesseractHandle.toBoolean(atEnd);
        } catch (TesseractException e) {
            LOGGER.error("Invalid boolean supplied.", e);
            return false;
        }
    }

    /**
     * Returns true if the iterator is at the start of an object at the given
     * level.
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
     * @return <code>true</code> if this iterator is at the start of an object at the given
     *      level of the page hierarchy. 
     */
    public boolean isAtBeginningOf(Level level) {
        int result = context.getAPI().TessPageIteratorIsAtBeginningOf(iterator, level.ordinal());
        
        try {
            return TesseractHandle.toBoolean(result);
        } catch (TesseractException e) {
            LOGGER.error("Invalid boolean supplied.", e);
            return false;
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
     *      element should be evaluated. For example, in the case that we want to know 
     *      if the current word is the last word in a paragraph, this should be 
     *      <code>{@link Level.PARA}</code>.
     * @param element The type of the element. This specifies how to identify the current 
     *      element to be evaluated. For example, in the case that we want to know 
     *      if the current word is the last word in a paragraph, this should be 
     *      <code>{@link Level.WORD}</code>.
     * @return <code>true</code> if the current element of the specified type is the last in 
     *      the indicate page structure; <code>false</code> otherwise.
     */
    public boolean isAtFinalElement(Level level, Level element) {
        int result = context.getAPI().TessPageIteratorIsAtFinalElement(iterator, level.value, element.value);
        
        try {
            return TesseractHandle.toBoolean(result);
        } catch (TesseractException e) {
            LOGGER.error("Invalid boolean supplied.", e);
            return false;
        }
    }

    //=========================================================================================
    // ACCESSING DATA
    //=========================================================================================

    /**
     * Returns the bounding rectangle of the current object at the given level.
     * See comment on coordinate system above.
     * 
     * @param level The level in the page hierarchy to retrieve the bounding box for.
     * @return The bounding box of the current object at the given level.
     * @throws TesseractException If there is no such object at the current position.
     */
    public BoundingBox getBoundingBox(Level level) throws TesseractException {
        IntBuffer left = IntBuffer.allocate(1);
        IntBuffer top = IntBuffer.allocate(1);
        IntBuffer right = IntBuffer.allocate(1);
        IntBuffer bottom = IntBuffer.allocate(1);
        
        int exists = context.getAPI().TessPageIteratorBoundingBox(iterator, level.value,
                            left, top, right, bottom);
        
        if (!TesseractHandle.toBoolean(exists)) {
            return null; // HACK: for testing
//            throw new TesseractException("There is no current element at this level of " +
//            		"the page hierarchy (" + level + ").");
        }
        
        return new BoundingBox(left.get(), top.get(), right.get(), bottom.get());
    }
    
    /** @return the type of the current block. */
    public PolyBlockType getBlockType() {
        int type = context.getAPI().TessPageIteratorBlockType(iterator);
        
        for (PolyBlockType pbt: PolyBlockType.values()) {
            if (type == pbt.value) {
                return pbt;
            }
        }
        
        LOGGER.warn("Unrecognized PolyBlockType: " + type);
        return PolyBlockType.UNKNOWN;
    }
    
    /**
     * Returns the baseline of the current object at the given level. The baseline is the 
     * line that passes through (x1, y1) and (x2, y2). Note that with vertical text, baselines 
     * may be vertical.
     * 
     * @param level The level in the page hierarchy to retrieve the baseline for.
     * @return the baseline of the current object at the given level
     * @throws TesseractException If no baseline exists at the current position
     */
    public Baseline getBaseline(Level level) throws TesseractException {
        // FIXME this doesn't seem to work reliably for Blocks
        IntBuffer x1 = IntBuffer.allocate(1);
        IntBuffer y1 = IntBuffer.allocate(1);
        IntBuffer x2 = IntBuffer.allocate(1);
        IntBuffer y2 = IntBuffer.allocate(1);
        
        int exists = context.getAPI().TessPageIteratorBaseline(iterator, level.value, x1, y1, x2, y2);
        
        if (!TesseractHandle.toBoolean(exists)) {
            throw new TesseractException("There is no current element at this level of " +
                    "the page hierarchy (" + level + ").");
        }
        
        return new Baseline(x1.get(), y1.get(), x2.get(), y2.get());
    }
    
    /** 
     * Returns orientation for the block the iterator points to. Unlike other methods to 
     * query the page layout details, this does not take a <code>level</code> argument as it
     * always operates at the top-level of the page hierarchy, the 
     * <code>{@link Level.BLOCK}</code>.
     * 
     * @return The orientation for the block the iterator points to.
     */
    public BlockOrientation getOrientation() {
        int value;
        IntBuffer orientationBuf = IntBuffer.allocate(1);
        IntBuffer writingDirectionBuf = IntBuffer.allocate(1);
        IntBuffer textlineOrderBuf = IntBuffer.allocate(1);
        FloatBuffer deskewAngleBuf = FloatBuffer.allocate(1);
        
        context.getAPI().TessPageIteratorOrientation(iterator, 
                orientationBuf, writingDirectionBuf, textlineOrderBuf, deskewAngleBuf);
        
        value = orientationBuf.get();
        Orientation orientation = Orientation.UP;
        for (Orientation o: Orientation.values()) {
            if (value == o.value) {
                orientation = o;
                break;
            }
        }
        
        value = writingDirectionBuf.get();
        WritingDirection direction = WritingDirection.LEFT_TO_RIGHT;
        for (WritingDirection d: WritingDirection.values()) {
            if (value == d.value) {
                direction = d;
                break;
            }
        }
        
        value = textlineOrderBuf.get();
        TextlineOrder order = TextlineOrder.TOP_TO_BOTTOM;
        for (TextlineOrder o: TextlineOrder.values()) {
            if (value == o.value) {
                order = o;
                break;
            }
        }
        
        return new BlockOrientation(orientation, direction, order, deskewAngleBuf.get());
    }
    
    // TODO the following methods of Tesseract's API are not currently supported by Tess4J
    //      BoundingBoxInternal
    //      Empty
    //      GetBinaryImage
    //      GetImage
    //      ParagraphInfo
    //  
    
    /**
     * Defines the boundary of a particular feature on the page.
     *  
     * @author Neal Audenaert
     */
    public static class BoundingBox {
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;
        
        private BoundingBox(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        
        public boolean contains(BoundingBox box) {
            return false;
        }
        
        public boolean intersects(BoundingBox box) {
            return false;
        }

        public BoundingBox intersection(BoundingBox box) {
            return null;
        }
        
        public BoundingBox union(BoundingBox box) {
            return null;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Bounding Box: (")
              .append(left).append(", ").append(top).append(") x (")
              .append(right).append(", ").append(bottom).append(")");
            return sb.toString();
        }
    }

    /**
     * Defines the baseline of a particular feature on the page. The baseline is the 
     * line that passes through (x1, y1) and (x2, y2). Note that with vertical text, baselines 
     * may be vertical.
     *  
     * @author Neal Audenaert
     */
    public static class Baseline {
        public final int x1;
        public final int y1;
        public final int x2;
        public final int y2;
        
        public Baseline(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Baseline: (")
              .append(x1).append(", ").append(y1).append(") x (")
              .append(x2).append(", ").append(y2).append(")");
            return sb.toString();
        }
    }
    
    /**
     * Defines the block orientation of a particular feature on the page, including the 
     * orientation of the block, the writing direction within the block, the order in which
     * lines of text are read and the skew angle within the block in radians.
     * 
     * @author Neal Audenaert
     */
    public static class BlockOrientation {
        public final Orientation orientation;
        public final WritingDirection writingDirection;
        public final TextlineOrder textlineOrder;
        
        /** The number of radians required to rotate the block anti-clockwise for it to be 
         *  level. <code>-PI / 4 <= deskewAngl <= PI / 4</code>. This is calculated after 
         *  the block is rotated so that the text orientation is upright. */
        public final float deskewAnge;
        
        private BlockOrientation(Orientation orientation, WritingDirection writingDirection, 
                                 TextlineOrder order, float angle) {
            this.orientation = orientation;
            this.writingDirection = writingDirection;
            this.textlineOrder = order;
            this.deskewAnge = angle;
        }
    }

}