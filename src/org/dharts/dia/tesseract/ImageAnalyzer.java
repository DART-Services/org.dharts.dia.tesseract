/* File: ImageAnalyzer.java
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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Used to retrieve the recognition results from a single image. This class can be used to 
 * perform both layout analysis (using <code>{@link #analyzeLayout()}</code>) both character
 * recognition (using <code>{@link #recognize()}</code>). Both analysis and recognition 
 * results can be performed on either the entire image or a select region of interest using 
 * the single parameter version of these methods. The analyzer can be called multiple times
 * (e.g., to perform layout analysis and then OCR on specific page regions), but the created
 * <code>LayoutIterator</code>s must be closed prior to performing the next analysis step. 
 * 
 * <p>
 * @see {@link LayoutIterator}
 * @see {@link RecognitionResultsIterator}
 * @author Neal Audenaert
 */
public interface ImageAnalyzer {

    /** 
     * Closes this analyzer. This method must be invoked prior to creating another 
     * analyzer with the same <code>ImageAnalyzerFactory</code>. After closing the analyzer,
     * all other methods will fail.
     * 
     * @throws TesseractException
     */
    public void close() throws TesseractException;
    
    /**
     * Returns a reference to the image being processed.
     * @return a reference to the image being processed.
     */
    public BufferedImage getImage();

    /**
     * Runs layout analysis over the entire image and returns a reading order iterator over 
     * the results. Note that any previously created <code>LayoutIterator</code> or 
     * <code>RecognitionResultIterator</code> must be closed prior to calling this method.
     *  
     * @return A reading-order iterator over the layout analysis results. Must be closed.
     * @throws TesseractException If there are errors analyzing the image. This is likely to 
     *      be caused by code that has failed to close a previously created iterator.
     */
    public LayoutIterator analyzeLayout() throws TesseractException;
    
    /**
     * Runs layout analysis over a specific region of interest and returns a reading order 
     * iterator over the results. Note that any previously created <code>LayoutIterator</code> 
     * or <code>RecognitionResultIterator</code> must be closed prior to calling this method.
     *  
     * @param roi A rectangle indicating the region of the image to be analyzed.
     * @return A reading-order iterator over the layout analysis results. Must be closed.
     * @throws TesseractException If there are errors analyzing the image. This is likely to 
     *      be caused by code that has failed to close a previously created iterator.
     */
    public LayoutIterator analyzeLayout(Rectangle roi) throws TesseractException;
    
    /**
     * Runs layout analysis over the entire image and returns a reading order iterator over 
     * the results. Note that any previously created <code>LayoutIterator</code> or 
     * <code>RecognitionResultIterator</code> must be closed prior to calling this method.
     *  
     * @return A reading-order iterator over the layout analysis results. Must be closed.
     * @throws TesseractException If there are errors analyzing the image. This is likely to 
     *      be caused by code that has failed to close a previously created iterator.
     */
    public RecognitionResultsIterator recognize() throws TesseractException;
    
    /**
     * Runs character recognition over a specific region of interest and returns a reading order 
     * iterator over the results. Note that any previously created <code>LayoutIterator</code> 
     * or <code>RecognitionResultIterator</code> must be closed prior to calling this method.
     *  
     * @param roi A rectangle indicating the region of the image to be recognized.
     * @return A reading-order iterator over the recognition results. Must be closed.
     * @throws TesseractException If there are errors processing the image. This is likely to 
     *      be caused by code that has failed to close a previously created iterator.
     */
    public RecognitionResultsIterator recognize(Rectangle rect) throws TesseractException;
}
