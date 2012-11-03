/* File: MediatorTests.java
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.dharts.dia.BoundingBox;
import org.dharts.dia.tesseract.LayoutIterator.Level;
import org.junit.Test;

public class MediatorTests {
    
    ImageAnalyzerFactory getImageAnaylzerFactory() throws IOException, TesseractException {
        return ImageAnalyzerFactory.createFactory(TestContext.tessDataPath);
    }
    
    @Test
    public void testCreateMediator() throws IOException, TesseractException {
        // Note that you need to compile this with an x86 version of Java, not the 64 bit version
        assertTrue("Data directory does not exist: " + TestContext.tessDataPath.getAbsolutePath(), TestContext.tessDataPath.exists());
        assertTrue("Data directory cannot be read: " + TestContext.tessDataPath.getAbsolutePath(), TestContext.tessDataPath.canRead());
        
        ImageAnalyzerFactory mediator = TestContext.getImageAnaylzerFactory();
        assertNotNull("Failed to create mediator", mediator);
        
        mediator.close();
    }
    
    @Test
    public void testAnalyzer() throws IOException, TesseractException {

        ImageAnalyzerFactory factory = TestContext.getImageAnaylzerFactory();
        try {
            ImageAnalyzer analyzer = TestContext.getImageAnalyzer(factory, TestContext.simpleImageFile);
            
            assertNotNull("Failed to create analyzer", analyzer);
            
            analyzer.close();
        } finally {
            if (factory != null)
                factory.close();
        }
    }
    
    @Test
    public void testPageIterator() throws IOException, TesseractException {
        ImageAnalyzerFactory factory = TestContext.getImageAnaylzerFactory();
        try {
            ImageAnalyzer analyzer = TestContext.getImageAnalyzer(factory, TestContext.poetryImageFile);
            
            LayoutIterator iterator = analyzer.analyzeLayout();
            assertNotNull("Failed to create iterator", iterator);

            do {
                @SuppressWarnings("unused")
                BoundingBox box = iterator.getBoundingBox(Level.TEXTLINE);
            } while (iterator.next(Level.TEXTLINE));
            
            iterator.close();
            analyzer.close();
        } finally {
            if (factory != null)
                factory.close();
        }
    }
}
