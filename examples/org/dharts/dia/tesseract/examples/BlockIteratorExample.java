/* File: BlockAnalysis.java
 * Created: Sep 19, 2012
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
package org.dharts.dia.tesseract.examples;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.dharts.dia.BoundingBox;
import org.dharts.dia.tesseract.BlockIterator;
import org.dharts.dia.tesseract.ImageAnalyzer;
import org.dharts.dia.tesseract.ImageAnalyzerFactory;
import org.dharts.dia.tesseract.LayoutIterator;
import org.dharts.dia.tesseract.PageBlock;
import org.dharts.dia.tesseract.LayoutIterator.Level;
import org.dharts.dia.tesseract.TesseractException;

/**
 * Given an image, analyze its structure and write a new image with boxes drawn around 
 * each block.
 * 
 * @author Neal Audenaert
 */
public class BlockIteratorExample {
    private static final String FILENAME = "res/examples/simple_poetry.png";
    private static final String OUT_FILENAME = "res/examples/output/block_iterator.jpg";
    
    static ImageAnalyzerFactory mediator = null;
    static ImageAnalyzer analyzer = null;
    static LayoutIterator layout = null;
    static BufferedImage image = null;
    static Graphics2D g = null;
    
    /**
     * Reads the input image and sets up the graphics object to write annotations on the
     * output image.
     * 
     * @param filename The image to load.
     * @throws IOException
     */
    private static void setupImage(String filename) throws IOException {
        image = ImageIO.read(new File(filename));
        g = image.createGraphics();
        g.setStroke(new BasicStroke(3.0f));
        g.setColor(Color.BLACK);
    }
    
    /** 
     * Initializes the required analyzer factory and image analyzer. These will be closed in
     * the cleanup method.
     * 
     * @throws IOException
     * @throws TesseractException
     */
    private static void setupAnalyzer() throws IOException, TesseractException {
        mediator = ImageAnalyzerFactory.createFactory(new File("."));
        analyzer = mediator.createImageAnalyzer(image);
    }
    
    private static void doAnalysis(String outfile, Level level) throws TesseractException, IOException {
        layout = analyzer.analyzeLayout();
        Iterator<PageBlock> iterator = new BlockIterator(layout);
        PageBlock block = null;
        while (iterator.hasNext()) {
            block = iterator.next();
            BoundingBox box = block.getBox();
            g.drawRect(box.left, box.top, box.right - box.left, box.bottom - box.top);
        }
        
        ImageIO.write(image, "jpg", new File(outfile));
    }
    
    private static void cleanup() {
        try {
            if (g != null) 
                g.dispose();
            
            if (layout != null) 
                layout.close();
            
            if (analyzer != null) 
                analyzer.close();
            
            if (mediator != null) 
                mediator.close();
            
            g = null;
            layout = null;
            analyzer = null;
            mediator = null;
        } catch (Exception ex) {
            throw new RuntimeException("Unrecoverable error trying to clean up Tesseract references", ex);
        }
    }
    
    public static void findBlocks(String filename, String outputname, Level level) {
        try  {
            setupImage(filename);
            setupAnalyzer();
            doAnalysis(outputname, level);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    public static void main(String[] args) {
        BlockIteratorExample.findBlocks(FILENAME, OUT_FILENAME, Level.BLOCK);
    }
}
