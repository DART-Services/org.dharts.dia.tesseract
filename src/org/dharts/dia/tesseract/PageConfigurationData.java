/* File: PageConfigurationData.java
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.dharts.dia.tesseract.PublicTypes.PageSegMode;

/**
 * Specifies configuration data to be used when processing a page image.  
 * 
 * @see {@link PageConfigurationDataBuilder} Used to build <code>PageConfigurationData</code>
 *      instances.
 * @see {@link ImageAnalyzerFactory#createImageAnalyzer(java.awt.image.BufferedImage, PageConfigurationData)}
 * @author Neal Audenaert
 */
public class PageConfigurationData {
    
    private final PageSegMode psm;
    private final Map<String, String> properties;
    private final Integer ppi;
    
    private PageConfigurationData(PageSegMode psm, Integer ppi, 
            Map<String, String> properties) {
        this.psm = psm;
        this.properties = Collections.unmodifiableMap(
                            new HashMap<String, String>(properties));
        this.ppi = ppi;
    }
    
    void configure(ImageAnalyzerFactory mediator) throws TesseractException {
        // update the page segmentation mode
        mediator.api.TessBaseAPISetPageSegMode(mediator.handle, psm.value);
        
        // update any configurations variables
        int success = Integer.MIN_VALUE;
        Map<String, String> errors = new HashMap<String, String>();
        try {
            for (String name : properties.keySet()) {
                String value = properties.get(name);
                success = mediator.api.TessBaseAPISetVariable(mediator.handle, name, value); 
                if (!ImageAnalyzerFactory.toBoolean(success)) {
                    errors.put(name, properties.get(name));
                }
            }
        } catch (TesseractException e) {
            throw new RuntimeException("Invalid response from Tesseract, expected " +
                    "boolean valued integer (0 or 1) but got " + success, e);
        }
        
        if (!errors.isEmpty()) {
            throw new InvalidParameterException(
                    "Could not create analyzer. Invalid variable settings: ", errors);
        }
    }
    
    void configure(ImageAnalyzerFactory factory, ImageAnalyzer analyzer) {
        // source resolution needs to be specified after the image is set
        if (ppi != null) {
            factory.api.TessBaseAPISetSourceResolution(factory.handle, ppi);
        }
    }
    
    /**
     * Used to create instances of the immutable <code>PageConfigurationData</code> data
     * vehicle. 
     *  
     * @author Neal Audenaert
     */
    public static final class PageConfigurationDataBuilder {
        private PageSegMode psm = PageSegMode.AUTO_OSD;
        private Map<String, String> properties = new HashMap<String, String>();
        private Integer ppi = null;
        private PageConfigurationData pcd;
        
        /**
         * Sets the page segmentation mode to be used by an image analyzer.
         * 
         * @param mode
         * @return A reference to this builder to facilitate fluid programming.
         */
        public PageConfigurationDataBuilder setPageSegmentationMode(PageSegMode mode) {
            this.psm = mode;
            return this;
        }
       
        /**
         * Sets the resolution of the image in pixels per inch.
         * 
         * @param ppi The page resolution.
         * @return A reference to this builder to facilitate fluid programming.
         */
        public PageConfigurationDataBuilder setPageResolution(int ppi) {
            this.ppi = ppi;
            return this;
        }
        
        /**
         * Sets the value of a Tesseract configuration variable.
         * 
         * @param name The name of the variable to set.
         * @param value The value for this variable.
         * @return A reference to this object to aid in setting multiple variables in a single
         *      statement. 
         */
        public PageConfigurationDataBuilder setVariable(String name, String value) {
            // TODO find and document a list of these variables and their valid values
            // TODO create an OO way of representing these properties.
            
            this.properties.put(name, value);
            return this;
        }
        
        /**
         * Builds the <code>PageConfigurationData</code> instance.
         * @return the <code>PageConfigurationData</code> instance.
         */
        public PageConfigurationData build() {
            // TODO examine thread safety. Fail if this has already been built.
            
            if (pcd == null) {
                pcd = new PageConfigurationData(psm, ppi, properties);
            }
            
            return pcd;
        }
    }
}
