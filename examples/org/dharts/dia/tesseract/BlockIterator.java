/* File: BlockIterator.java
 * Created: Sep 22, 2012
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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.dharts.dia.tesseract.LayoutIterator;
import org.dharts.dia.tesseract.LayoutIterator.BoundingBox;
import org.dharts.dia.tesseract.LayoutIterator.Level;
import org.dharts.dia.tesseract.TesseractException;

/**
 * @author Neal Audenaert
 */
public class BlockIterator implements Iterator<PageBlock> {
    private static final Logger LOGGER = Logger.getLogger(BlockIterator.class);
    
    private final LayoutIterator iterator;
    
    private PageBlock nextBlock = null;
    private boolean endOfPage = false;
    
    public BlockIterator(LayoutIterator iterator) {
        // TODO clone this iterator, release it when the block iterator is done or passes out 
        //      of scope
        this.iterator = iterator;
        initNextItem(false);
    }
    
    protected void finalize() {
        close();
    }
    
    private void close() {
        // TODO release the cloned iterator - must be safe for multiple calls in a 
        //      parallel environment
    }
    
    private void initNextItem(boolean advance) {
        if (this.endOfPage) {
            nextBlock = null;
            return;
        }
        
        if (advance) {
            if (!iterator.next(Level.BLOCK)) {
                this.endOfPage = true;
                return;
            }
        }

        try {
            BoundingBox box = iterator.getBoundingBox(Level.BLOCK);
            if (box == null) {
                initNextItem(true);
            }
            
            nextBlock = new PageBlock(iterator.getBlockType(), iterator.getOrientation(), box);
        } catch (TesseractException te) {
            LOGGER.error("Could not initialize iterator.", te);
            this.nextBlock = null;
        }
    }
    
    @Override
    public boolean hasNext() {
        return nextBlock != null;
    }

    @Override
    public PageBlock next() {
        if (nextBlock == null) {
            throw new NoSuchElementException();
        }
        PageBlock next = this.nextBlock;
        initNextItem(true);
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove blocks from the page model");
    }

}
