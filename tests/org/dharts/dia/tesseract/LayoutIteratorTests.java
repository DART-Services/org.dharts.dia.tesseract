/* File: LayoutIteratorTests.java
 * Created: Nov 3, 2012
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.awt.Rectangle;
import java.io.IOException;

import org.dharts.dia.BoundingBox;
import org.dharts.dia.tesseract.LayoutIterator.Level;
import org.junit.Test;

/**
 * @author Neal Audenaert
 *
 */
public class LayoutIteratorTests {
    @Test
    public void testPageIteratorCloning() throws IOException, TesseractException {
        ImageAnalyzerFactory factory = TestContext.getImageAnaylzerFactory();
        try {
            ImageAnalyzer analyzer = TestContext.getImageAnalyzer(factory, TestContext.poetryImageFile);
            
            LayoutIterator blockIterator = analyzer.analyzeLayout();
            LayoutIterator lineIterator = blockIterator.copy();
            assertNotNull("Failed to create iterator", blockIterator);

            int blocks = 0;
            do {
                @SuppressWarnings("unused")
                BoundingBox box = blockIterator.getBoundingBox(Level.BLOCK);
                blocks++;
            } while (blockIterator.next(Level.BLOCK));
            
            assertEquals("Unexpected number of blocks", 3, blocks);
            blockIterator.close();
            
            int lines = 0;
            do {
                @SuppressWarnings("unused")
                BoundingBox box = lineIterator.getBoundingBox(Level.TEXTLINE);
                lines++;
            } while (lineIterator.next(Level.TEXTLINE));
            
            assertEquals("Unexpected number of lines", 22, lines);
            
            lineIterator.close();
            analyzer.close();
        } finally {
            if (factory != null)
                factory.close();
        }
    }
    
    /**
     * After closing an iterator, all methods should throw exceptions.
     * @throws IOException
     * @throws TesseractException
     */
    @Test
    public void testMethodsPostClose() throws IOException, TesseractException {
        // HACK: this will be impossible to maintain. Need a better way to identify and 
        //       test methods that should fail (possibly reflection)
        ImageAnalyzerFactory factory = TestContext.getImageAnaylzerFactory();
        try {
            ImageAnalyzer analyzer = TestContext.getImageAnalyzer(factory, TestContext.poetryImageFile);
            LayoutIterator blockIterator = analyzer.analyzeLayout();
            blockIterator.close();
            
            try {
                blockIterator.begin();
                assertFalse("begin called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.copy();
                assertFalse("copy called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.close();
                assertFalse("close called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.getBaseline(Level.BLOCK);
                assertFalse("getBaseline called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.getBlockType();
                assertFalse("getBlockType called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.getOrientation();
                assertFalse("getOrientation called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.isAtBeginningOf(Level.BLOCK);
                assertFalse("isAtBeginningOf called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.isAtFinalElement(Level.WORD, Level.BLOCK);
                assertFalse("isAtFinalElement called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            try {
                blockIterator.next(Level.BLOCK);
                assertFalse("next called after iterator was closed. Should have thrown an exception", true);
            } catch (Exception ex) { /* expected */ }
            
            analyzer.close();
        } finally {
            if (factory != null)
                factory.close();
        }
    }

    /**
     * After closing an iterator, all methods should throw exceptions.
     * @throws IOException
     * @throws TesseractException
     */
    @Test
    public void testDuplicateAnalyzerMethods() throws IOException, TesseractException {
        // HACK: this will be impossible to maintain. Need a better way to identify and 
        //       test methods that should fail (possibly reflection)
        ImageAnalyzerFactory factory = TestContext.getImageAnaylzerFactory();
        try {
            ImageAnalyzer analyzer = TestContext.getImageAnalyzer(factory, TestContext.poetryImageFile);
            LayoutIterator blockIterator = analyzer.analyzeLayout();
            
            // try to create other iterators and manipulate factory while iterator is open
            LayoutIterator badIterator;
            try {
                badIterator = analyzer.analyzeLayout();
                assertFalse("Was able to create duplicate iterator. Should have thrown an exception", true);
                badIterator.close();
            } catch (Exception ex) { /* expected */ }
            
            try {
                badIterator = analyzer.analyzeLayout(new Rectangle(50, 50));
                assertFalse("Was able to create duplicate iterator. Should have thrown an exception", true);
                badIterator.close();
            } catch (Exception ex) { /* expected */ }
            
            try {
                badIterator = analyzer.recognize();
                assertFalse("Was able to create duplicate iterator. Should have thrown an exception", true);
                badIterator.close();
            } catch (Exception ex) { /* expected */ }
            
            try {
                badIterator = analyzer.recognize(new Rectangle(50, 50));
                assertFalse("Was able to create duplicate iterator. Should have thrown an exception", true);
                badIterator.close();
            } catch (Exception ex) { /* expected */ }
            
            blockIterator.close();
            analyzer.close();
        } finally {
            if (factory != null)
                factory.close();
        }
    }
}