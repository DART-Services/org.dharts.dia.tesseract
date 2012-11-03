/* File: ImageAnalyzerFactory.java
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
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dharts.dia.tesseract.PublicTypes.OcrEngineMode;
import org.dharts.dia.tesseract.PublicTypes.PageSegMode;
import org.dharts.dia.tesseract.tess4j.LayoutHandle;
import org.dharts.dia.tesseract.tess4j.ResultHandle;
import org.dharts.dia.tesseract.tess4j.TesseractHandle;
import org.dharts.dia.tesseract.tess4j.TesseractHandle.InvalidStateException;

/**
 * Manages a connection to the underlying Tesseract implementation and creates 
 * <code>{@link ImageAnalyzer}</code> instances. An <code>ImageAnalyzerFactory</code and the 
 * analyzers that it creates should be confined to a single thread, however the use of multiple 
 * factories by different threads is supported. Multi-threaded applications should create a 
 * new <code>ImageAnalyzerFactory</code> for each thread.
 * 
 * @author Neal Audenaert
 */
public class ImageAnalyzerFactory {
    private static final Logger LOGGER = Logger.getLogger(ImageAnalyzerFactory.class);

    /**
     * Instantiates a new {@link ImageAnalyzerFactory} for the supplied data path.
     * 
     * @param datapath The path to use to find Tesseract training files.
     * @return The constructed {@link ImageAnalyzerFactory}
     * @throws IOException If there are problems accessing the supplied data path.
     * @throws TesseractException If there are problems initializing the factory.
     */
    public static ImageAnalyzerFactory createFactory(File datapath) throws IOException, TesseractException {
        return createFactory(datapath, "eng", OcrEngineMode.TESSERACT_ONLY);
    }
    
    /**
     * Instantiates a new {@link ImageAnalyzerFactory} for the supplied configuration.
     * 
     * @param datapath Sets the parent directory of tessdata.
     * @param language the language or languages used to recognize the supplied text. The 
     *      language is (usually) an <code>ISO 639-3</code> string. It may be in the form 
     *      <code>[~]<lang>[+[~]<lang>]*</code> indicating that multiple languages are to be 
     *      loaded. E.g., <code>hin+eng</code> will load Hindi and English.
     * 
     *      <p>
     *      Languages may specify internally that they want to be loaded with one or more other 
     *      languages, so the <code>~</code> sign is available to override that. E.g., 
     *      if <code>hin</code> were set to load <code>eng</code> by default, then 
     *      <code>hin+~eng</code> would force loading only <code>hin</code>. The number of  
     *      loaded languages is limited only by memory, with the caveat that loading additional 
     *      languages will impact both speed and accuracy, as there is more work to do to 
     *      decide on the applicable language, and there is more chance of hallucinating 
     *      incorrect words.
     * @param oem  the OCR engine mode to be used. @see {@link OcrEngineMode} for more details.
     * @return The constructed {@link ImageAnalyzerFactory}
     * @throws IOException If there are problems accessing the supplied data path.
     * @throws TesseractException If there are problems initializing the factory.
     */
    public static ImageAnalyzerFactory createFactory(
            File datapath, String language, OcrEngineMode oem) throws IOException, TesseractException {
        
        String pathStr;
        try {
            pathStr = datapath.getCanonicalPath();
        } catch (Exception ex) {
            throw new IOException("Could not construct canonical path", ex);
        }
        
        if (!datapath.isDirectory() || !datapath.canRead()) {
            throw new IOException("The datapath must be a readable directory." + pathStr);
        }

        pathStr = pathStr + "/";
        ImageAnalyzerFactory m = new ImageAnalyzerFactory(pathStr, language, oem);
        m.init();
        
        return m;
    }
    
    /* From the Tesseract documentation.
     * 
     * Instances are now mostly thread-safe and totally independent, but some  global 
     * parameters remain. Basically it is safe to use multiple TessBaseAPIs in different 
     * threads in parallel, UNLESS: you use <code>setVariable</code> on some of the Params 
     * in classify and textord.  If you do, then the effect will be to change it for all your
     * instances.
     * 
     * TODO make sure we understand what parameters are global and restrict access to those 
     *      parameters
     */

    //========================================================================================
    // MEMBER VARIABLES
    //========================================================================================
    
    // final TessAPI api;
    final TesseractHandle handle;
    
    private final String datapath;
    private final String language;
    private final OcrEngineMode oem;
    
    private volatile boolean destroyed = false;
    
    PageSegMode psm = PageSegMode.AUTO;
    Map<String, String> properties = new HashMap<String, String>();
    
    private ImageAnalyzerImpl analyzer = null;
    
    //========================================================================================
    // INITIALIZATION METHODS
    //========================================================================================
    
    private ImageAnalyzerFactory(String datapath, String language, OcrEngineMode oem) 
            throws TesseractException {
        this.datapath = datapath;
        this.language = language;
        this.oem = oem;
        
        handle = TesseractHandle.create();
        
//        try {
//            handle.setPageSegMode(PageSegMode.AUTO);
//        } catch (InvalidStateException ise) {
//            throw new TesseractException("Could not intialize connection to Tesseract", ise);
//        }
    }
    
    protected void finalize() {
        if (!destroyed) {
            LOGGER.warn("Failed to properly shut down ImageAnalyzerFActory. This may " +
            		"result in memory leaks and other problems. Forcing shutdown.");
            this.close();
        }
    }
    
    /**
     * Closes this ImageAnalyzerFactory. Factories must be shut down after use to reclaim 
     * memory and free up resources for other factories.
     */
    public void close() {
        destroyed = true;
        try {
            handle.close();
        } catch (InvalidStateException hce) {
            LOGGER.error("Attempt to close a handle that is not in a closable state.", hce);
        }
    }
    
    /** 
     * Indicates whether this factory has been closed and should no longer be used.
     * @return <tt>true</tt> if this factory has been closed.
     */
    public boolean isClosed() {
        return destroyed;
    }
    
    /**
     * Initializes the handle to the Tesseract engine.
     * 
     * @throws TesseractException if the attempt to initalize the engine was not successful.
     */
    private void init() throws TesseractException {
        checkDestroyed();
        
        handle.init(datapath, language, oem);
    }
    
    /** Throws an exception if the factory has been destroyed. */
    private void checkDestroyed() throws TesseractException {
        if (this.destroyed) {
            throw new TesseractException("Invalid Access: This ImageAnalyzerFactory has " +
                    "already been closed.");
        }
    }
    
    /** Throws an exception if there is a currently active analyzer. */
    private void checkAnalyzer() throws TesseractException {
        // check for any current analyzers and throw exception if found
        if (this.analyzer != null) {
            throw new TesseractException("Concurrent Access Exception: An analyzer is " +
                    "already in use for this ImageAnalyzerFactory. Be sure to call 'close'" +
                    "on any existing analyzers prior to creating another.");
        }
    }
    
    
    //========================================================================================
    // CONFIGURATION ACCESSORS AND MUTATORS 
    //========================================================================================
    
    /**
     * Returns the languages string used to initialize this mediator. If this was initialized
     * with "deu+hin" then that will be returned. If hin loaded eng automatically as well, 
     * then that will not be included in this list. To find the languages actually loaded use
     * <code>GetLoadedLanguagesAsVector</code>. 
     * 
     * @return The language used to initialize this mediator.
     */
    public String getInitLanguage() throws TesseractException {
        checkDestroyed();
        return this.language;      
    }
    
    /**
     * Returns the loaded languages as a set of <code>String</code>s. This will include all 
     * languages loaded as dependencies of other languages as well as those specified 
     * explicitly when the factory was created. 
     * 
     * @return A set containing all loaded languages.
     */
    public Collection<String> getLoadedLanguages() throws TesseractException {
        checkDestroyed();
        
        return handle.getLoadedLanguages();
    }
    
    //========================================================================================
    // CREATE & MANAGE ANALYZERS 
    //========================================================================================

    /**
     * Analyzers must be explicitly released prior to creating a new analyzer. This should 
     * be called only by the <code>{@link ImageAnalyzerImpl#close()}</code> method.
     * 
     * <p>
     * The underlying implementation is provided by method calls on a single object that must 
     * be executed in a particular sequence. This Java API explicitly enforces this execution 
     * order by requiring that there can be only one <code>ImageAnalyzer</code> in use at any
     * given time. Rather than failing in an undetermined way at some future point in time if
     * a client tries to use two different <code>ImageAnalyzer</code>s simultaneously, we 
     * adopt a fail fast policy in which attempts to create an analyzer without closing the 
     * previous one will throw an exception.
     *   
     * @param analyzer The analyzer to release.
     */
    void release(ImageAnalyzerImpl analyzer) {
        this.analyzer = null;
    }
    
    /**
     * Creates an <code>ImageAnalyzer</code> to perforrm OCR and/or page layout analysis on
     * the underlying image. After a call to this method, the supplied image may be modified
     * or destroyed. The returned analyzer must be explicitly closed prior to processing 
     * another image. This will use the last page configuration data that was explicitly 
     * provided. 
     * 
     * <p>
     * The underlying Tesseract implementation of <code>setImage</code> clears all recognition 
     * results, and sets the rectangle to the full image. This has the side effect of 
     * invalidating any previously created <code>ImageAnalyzer</code> instances. However, 
     * since the <code>ImageAnalyzer</code> is not explicitly accessible via the Tessearact API, 
     * the Java instance, and any <code>PageIterator</code> currently in use would continue to 
     * "work" but would refer to the new image and other internal data structures. 
     * 
     * <p>
     * Rather than failing in an undetermined way at some future point in time if
     * a client tries to use two different <code>ImageAnalyzer</code>s simultaneously, we 
     * adopt a fail fast policy in which attempts to create an analyzer without closing the 
     * previous one will throw an exception.
     * 
     * @param image The image to be processed.
     * @param pcd Page configuration data that will govern how the page is processed 
     * @return An analyzer to be used to perform the OCR or layout analysis of the provide 
     *      image. This object must be explicitly closed prior to creating another analyzer.
     * @throws TesseractException
     */
    public ImageAnalyzer createImageAnalyzer(BufferedImage image, PageConfigurationData pcd) 
            throws TesseractException {
        // HACK: pcd will be cumulative. That is, parameters that are explicitly set will 
        //       override existing parameters, but this will not clear any values that have
        //       been previously set. For example, if a new configuration is supplied that 
        //       specifies the page segmentation mode, but does not specify source resolution,
        //       the last supplied source resolution will continue to be used. This could 
        //       result in surprising behavior.
        
        // One solution is to create expose a mutable PageConfigurationData instance for 
        // each factory that contains all configuration variables that have been set. 
        checkDestroyed();
        checkAnalyzer();
        
        // HACK: perform pre and post configuration. Need to find a better way to achieve this
        pcd.configure(this);
        ImageAnalyzer analyzer = createImageAnalyzer(image);
        pcd.configure(this, analyzer);
        return analyzer;
    }
    
    /**
     * Creates an <code>ImageAnalyzer</code> to perforrm OCR and/or page layout analysis on
     * the underlying image. After a call to this method, the supplied image may be modified
     * or destroyed. The returned analyzer must be explicitly closed prior to processing 
     * another image. This will use the last page configuration data that was explicitly 
     * provided. 
     * 
     * <p>
     * The underlying Tesseract implementation of <code>setImage</code> clears all recognition 
     * results, and sets the rectangle to the full image. This has the side effect of 
     * invalidating any previously created <code>ImageAnalyzer</code> instances. However, 
     * since the <code>ImageAnalyzer</code> is not explicitly accessible via the Tessearact API, 
     * the Java instance, and any <code>PageIterator</code> currently in use would continue to 
     * "work" but would refer to the new image and other internal data structures. 
     * 
     * <p>
     * Rather than failing in an undetermined way at some future point in time if
     * a client tries to use two different <code>ImageAnalyzer</code>s simultaneously, we 
     * adopt a fail fast policy in which attempts to create an analyzer without closing the 
     * previous one will throw an exception.
     * 
     * @param image The image to be processed.
     * @return An analyzer to be used to perform the OCR or layout analysis of the provide 
     *      image. This object must be explicitly closed prior to creating another analyzer.
     * @throws TesseractException
     */
    public ImageAnalyzer createImageAnalyzer(BufferedImage image) throws TesseractException {
        checkDestroyed();
        checkAnalyzer();
        
        this.analyzer = new ImageAnalyzerImpl(image);
        try {
            this.analyzer.init();
        } catch (InvalidParameterException ipe) {
            try {
                this.analyzer.close();
            } catch (Exception ex) {
                this.analyzer = null;   
                // TODO Log this error
            }
            
            throw ipe;
        }
        
        return this.analyzer;
    }
    
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
    public class ImageAnalyzerImpl implements ImageAnalyzer {

        private final Map<String, String> properties;
        private final BufferedImage image;
        
        // The iterator currently in use (if any)
        private LayoutIterator iterator = null;
        private boolean closed = false;
        
        //========================================================================================
        // CONSTRUCTORS AND INITIALIZATION METHOS
        //========================================================================================
        
        /**
         * Constructs a new ImageAnalyzer
         * 
         * @param factory The factory that owns this instance. 
         * @param image The image being analyzed.
         */
        private ImageAnalyzerImpl(BufferedImage image) {
            this.image = image;
            this.properties = Collections.unmodifiableMap(
                    new HashMap<String, String>(ImageAnalyzerFactory.this.properties));
            
        }
        
        private void init() throws InvalidParameterException, TesseractException {
            // update the page segmentation mode
            TesseractHandle handle = ImageAnalyzerFactory.this.handle;
            handle.setPageSegMode(psm);
            
            try {
                // update any configurations variables
                for (String name : properties.keySet()) {
                    handle.setVariable(name, properties.get(name));
                }
        
                handle.setImage(image);
            } catch (InvalidStateException ise) {
                throw new TesseractException("Could not set image." , ise);
            }
        }
        
        @Override
        public void close() throws TesseractException {
            // TODO this should clear the set image on the underlying API. Need to figure out
            //      how to do that.
            checkIterator();
            ImageAnalyzerFactory.this.release(this);
            closed = true;
        }
        
        private void checkIterator() throws TesseractException {
            if (iterator != null) {
                throw new TesseractException("Concurrent Access Exception: There is an " +
                        "outstanding <code>PageIterator</code> for this analyzer. Please " +
                        "ensure that you close the iterators before ");
            }
        }

        private void checkClosed() throws TesseractException {
            if (this.closed) {
                throw new TesseractException("Attempt to use an image analyzer that has already been closed.");
            }
        }
        
        //========================================================================================
        //
        //========================================================================================
        
        /** 
         * Stores a reference to the active iterator and adds a close listener.
         * @param iterator
         * @return
         */
        private <X extends LayoutIterator> X wrapIterator(final X iterator) {
            // TODO support cloning
            this.iterator = iterator;
            iterator.onClose(new CloseListener<LayoutIterator>() {
                
                @Override
                public void closed(LayoutIterator handle) {
                    if (ImageAnalyzerImpl.this.iterator == handle)
                        ImageAnalyzerImpl.this.iterator = null;
                }
            });
            
            return iterator;
        }
        
        @Override
        public BufferedImage getImage() {
            return this.image;
        }
        
        @Override
        public LayoutIterator analyzeLayout() throws TesseractException {
            checkIterator();  // FIXME state should be managed by handle now
            checkClosed();
            
            LayoutHandle layoutHandle = handle.analyseLayout();
            return wrapIterator(new LayoutIterator(layoutHandle));
        }
        
        @Override
        public LayoutIterator analyzeLayout(Rectangle rect) throws TesseractException {
            checkIterator();
            checkClosed();
            
            handle.setRectangle(rect);
            LayoutHandle layoutHandle = handle.analyseLayout();
            return wrapIterator(new LayoutIterator(layoutHandle));
        }
        
        @Override
        public RecognitionResultsIterator recognize() throws TesseractException {
            checkIterator();
            checkClosed();
            
            ResultHandle resultHandle = handle.recognize();
            return wrapIterator(new RecognitionResultsIterator(resultHandle));
        }
        
        @Override
        public RecognitionResultsIterator recognize(Rectangle rect) throws TesseractException {
            checkIterator();
            checkClosed();
            
            handle.setRectangle(rect);
            ResultHandle resultHandle = handle.recognize();
            return wrapIterator(new RecognitionResultsIterator(resultHandle));
        }
    }
}
