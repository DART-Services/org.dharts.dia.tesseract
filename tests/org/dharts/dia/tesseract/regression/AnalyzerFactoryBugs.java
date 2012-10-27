/* File: AnalyzerFactoryBugs.java
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
package org.dharts.dia.tesseract.regression;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.dharts.dia.tesseract.ImageAnalyzer;
import org.dharts.dia.tesseract.ImageAnalyzerFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Neal Audenaert
 *
 */
public class AnalyzerFactoryBugs {
    private File dataDir = new File(".");
    private File testImageFile = new File("images/visualpage/300allday-18.png");
    
    private ImageAnalyzerFactory factory = null;
    private ImageAnalyzer analyzer = null;
    
    protected void tearDown() {
        // cleanup. This shouldn't be needed, but we'll check
        if (analyzer != null) {
            try {
                analyzer.close();
            } catch (Exception ex) {
                assertFalse("Failed to close open analyzer: " + ex.getMessage(), true);
            }
        }
        
        if ((factory != null) && !factory.isClosed()) {
            try {
                factory.close();
            } catch (Exception ex) {
                assertFalse("Failed to close non-null factory: " + ex.getMessage(), true);
            }
        }
    }
    
    /**
     * Tests for https://github.com/DART-Services/org.dharts.dia.tesseract/issues/10
     * 
     * <p>Calling an ImageAnalyzerFactory after it has been closed results in a seg fault 
     * because the Java layer calls into the underlying C++ with a reference to a pointer 
     * that is no longer valid.
     * 
     * <p>This should generate an Exception and not invoke the C++ layer.
     */
    @Test
    public void testOpenCloseUseAnalyzer() {
        BufferedImage image = null;
        
        // Open and immediately close the image analyzer factory.
        // This should be just fine
        try {
            factory = ImageAnalyzerFactory.createFactory(dataDir);
            factory.close();
        } catch (Exception ex) {
            assertFalse("Could not open and imediately close the analyzer factory: " + ex.getMessage(), true);
        }
            
        // sanity checks on the image file
        assertTrue("Test image does not exist", testImageFile.exists());
        assertTrue("Test image is not readable", testImageFile.canRead());
        assertTrue("Test image is not a file", testImageFile.isFile());
        
        try {
            image = ImageIO.read(testImageFile);
            assertNotNull("Could not load test image", image);
        } catch (IOException ex) {
            assertFalse("Failed to load test image: " + ex.getMessage(), true);
        }
            
        try {
            // This should throw an exception. In the buggy behavior, this is causing a 
            // seg fault because the factory attempts to create the analyzer even though 
            // the factory has been closed.
            analyzer = factory.createImageAnalyzer(image);
            assertFalse("Was able to construct an ImageAnalyzer from a closed factory", true);
        } catch (Exception ex) {
            // this is the desired behavior.
            analyzer = null;
        }
        
        // we succeeded an have already shut this down
        factory = null;
    }

}
