/* File: TestContext.java
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * @author Neal Audenaert
 *
 */
public class TestContext {
    final static File tessDataPath = new File(".");
    final static File simpleImageFile = new File("res/testing/simple.png");
    final static File poetryImageFile = new File("res/testing/simple_poetry.png");
    
    static ImageAnalyzerFactory getImageAnaylzerFactory() throws IOException, TesseractException {
        return ImageAnalyzerFactory.createFactory(tessDataPath);
    }
    
    static ImageAnalyzer getImageAnalyzer(ImageAnalyzerFactory factory, File f) throws TesseractException, IOException {
        BufferedImage image = ImageIO.read(f);
        return factory.createImageAnalyzer(image);
    }
}
