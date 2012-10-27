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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.dharts.dia.tesseract.LayoutIterator.BoundingBox;
import org.dharts.dia.tesseract.LayoutIterator.Level;
import org.junit.Test;

import static org.junit.Assert.*;

public class MediatorTests {
    
    File tessDataPath = new File(".");
    File simpleImageFile = new File("res/testing/simple.png");
    File poetryImageFile = new File("res/testing/simple_poetry.png");
    
    @Test
    public void testCreateMediator() throws IOException, TesseractException {
        // Note that you need to compile this with an x86 version of Java, not the 64 bit version
        assertTrue("Data directory does not exist: " + tessDataPath.getAbsolutePath(), tessDataPath.exists());
        assertTrue("Data directory cannot be read: " + tessDataPath.getAbsolutePath(), tessDataPath.canRead());
        
        ImageAnalyzerFactory mediator = ImageAnalyzerFactory.createFactory(tessDataPath);
        assertNotNull("Failed to create mediator", mediator);
        
        mediator.close();
    }
    
    @Test
    public void testAnalyzer() throws IOException, TesseractException {

        ImageAnalyzerFactory mediator = null;
        try {
            mediator = ImageAnalyzerFactory.createFactory(tessDataPath);
            
            BufferedImage image = ImageIO.read(simpleImageFile);
            ImageAnalyzer analyzer = mediator.createImageAnalyzer(image);
            
            assertNotNull("Failed to create analyzer", analyzer);
            
            analyzer.close();
        } finally {
            if (mediator != null)
                mediator.close();
        }
    }
    
    @Test
    public void testPageIterator() throws IOException, TesseractException {
        ImageAnalyzerFactory mediator = null;
        try {
            mediator = ImageAnalyzerFactory.createFactory(new File("."));
            
            BufferedImage image = ImageIO.read(poetryImageFile);
            ImageAnalyzer analyzer = mediator.createImageAnalyzer(image);
            LayoutIterator iterator = analyzer.analyzeLayout();
            assertNotNull("Failed to create iterator", iterator);

            int ct = 0;
            do {
                BoundingBox box = iterator.getBoundingBox(Level.TEXTLINE);
                ct++;
                System.out.println(ct + " :: " + (box.right - box.left));
            } while (iterator.next(Level.TEXTLINE));
            
            iterator.close();
            analyzer.close();
        } finally {
            if (mediator != null)
                mediator.close();
        }
    }
}
