/* File: TesseractHandle.java
 * Created: Sep 23, 2012
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
package org.dharts.dia.tesseract.tess4j;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.dharts.dia.tesseract.InvalidParameterException;
import org.dharts.dia.tesseract.PublicTypes;
import org.dharts.dia.tesseract.TesseractException;
import org.dharts.dia.tesseract.tess4j.TessAPI.TessBaseAPI;
import org.dharts.dia.tesseract.tess4j.TessAPI.TessPageIterator;
import org.dharts.dia.tesseract.tess4j.TessAPI.TessResultIterator;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
// FIXME remove external dependencies

public class TesseractHandle {
    
    public static enum State {
        UNINITIALIZED, INITIALIZED, IMAGE_SET, ANALYZING, RECOGNIZING, CLOSED;
    }
    
    
    /** 
     * A collection of the active states for a handle, that is, the handle has been initialized 
     * and/or is being used to perform some analysis task.
     */
    public static final Collection<State> ACTIVE_STATES = 
            Collections.unmodifiableCollection(Arrays.asList(State.INITIALIZED, State.IMAGE_SET, State.ANALYZING, State.RECOGNIZING));
    public static final Collection<State> CLOSABLE_STATES = 
            Collections.unmodifiableCollection(Arrays.asList(State.UNINITIALIZED, State.INITIALIZED, State.IMAGE_SET));
    
    /**
     * Converts integer values returned by the underlying API into Java booleans.
     * 
     * @param value The value to convert.
     * @return <tt>true</tt> if value equals <tt>TessAPI.TRUE</tt>, <tt>false</tt> if
     *      it equals <tt>TessAPI.FALSE</tt>
     * @throws IllegalArgumentException If the supplied value is any value other than 
     *      <tt>TessAPI.TRUE</tt> or <tt>TessAPI.FALSE</tt>.
     */
    public static boolean toBoolean(int value) {
        boolean result;
        if (value == TessAPI.TRUE) {
            result = true;
        } else if (value == TessAPI.FALSE) {
            result = false;
        } else {
            throw new IllegalArgumentException("Invlid boolean value. Expected " + 
                    TessAPI.TRUE  + " or " + TessAPI.FALSE + ". Got " + value);
        }
        
        return result;
    }
    
    public static TesseractHandle create() {
        return new TesseractHandle(TessAPI.INSTANCE);
    }
    
    private final TessBaseAPI handle;
    private final TessAPI api;
    
    private volatile String language = null;        // the language used to initialize this 
//    private volatile PublicTypes.PageSegMode psm = null;    // The current page segmentation mode
    
    private volatile State state = State.UNINITIALIZED;
    
    /**
     * 
     */
    TesseractHandle(TessAPI api) {
        this.api = api;
        this.handle = api.TessBaseAPICreate();
        
        // TODO test is null?
    }
    
    public synchronized void close() throws InvalidStateException {
        requireState(CLOSABLE_STATES);
        
        api.TessBaseAPIDelete(handle);
        state = State.CLOSED;
    }
 
    private void requireState(State state) throws InvalidStateException {
        requireState(Collections.singleton(state));
    }
    
    private void requireState(Collection<State> states) throws InvalidStateException {
        boolean found = false;
        for (State s: states) {
            if (this.state == s) {
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new InvalidStateException(this.state, states);
        }
    }
    
    //========================================================================================
    // VARIABLES AND CONFIGURATION
    //========================================================================================
    
    /**
     * Helper method for <code>{@link #getVariable(String, Object)}</code> that throws an 
     * exception if the variable could not be retrieved.
     *     
     * @param errCode The returned error code.
     * @param name The name of the variable to be retrieved. 
     * @throws InvalidParameterException If the errCode indicates that the variable could not be 
     *      identified.
     * @throws TesseractException if the supplied errCode is not a valid boolean value (0 or 1). 
     */
    private void checkGetVariableReturnValue(int errCode, String name) throws InvalidParameterException, TesseractException {
        if (!toBoolean(errCode)) {
            throw new InvalidParameterException("Could not retrieve the requested parameter", Collections.singletonMap(name, ""));
        }
    }
    
    /**
     * Set the value of an internal "parameter." Supply the name of the parameter and the 
     * value as a string, just as you would in a config file. Returns false if the name lookup 
     * failed. 
     * 
     * <p>For example, <code>setVariable("tessedit_char_blacklist", "xyz");</code> to ignore x,
     * y and z. Or <code>setVariable("classify_bln_numeric_mode", "1");</code> to set 
     * numeric-only mode.
     * 
     * <p>
     * FIXME: These notes seem contradictory 
     * <code>setVariable</code> may be used before <code>Init</code>, but settings will revert 
     * to defaults on <code>End()</code>.
     * 
     * <p>Note: Must be called after <code>init()</code>. Only works for non-init variables 
     * (init variables should be passed to <code>init()</code>).
     * @throws InvalidParameterException 
     * @throws TesseractException 
     */
    public void setVariable(String name, String value) 
            throws TesseractException, InvalidParameterException {
        requireState(State.INITIALIZED);    // may allow during analysis as well, but probably not
        
        int rv = api.TessBaseAPISetVariable(handle, name, value);
        if (!toBoolean(rv)) {
            Map<String, String> map = Collections.singletonMap(name, value);
            throw new InvalidParameterException("Could not set the requested parameter", map);
        }
    }
    
    /**
     * Indicates whether this handle has the specified variable. Specifically, 
     * <code>{@link #getVariable(String, Object)}</code> will return a valid value if and only
     * if this returns <code>true</code>.
     *  
     * @param name The name of the variable to retrieve.
     * @param type The expected type of the variable to retrieve.
     * @return <code>true</code> if <code>{@link #getVariable(String, Object)}</code> will 
     *      return a valid variable for this value, <code>false</code> otherwise.
     */
    public <T> boolean hasVariable(String name, T type) {
        // TODO do we need to require a state here?
        try {
            getVariable(name, type);
            return true;
        } catch (TesseractException ex) {
            return false;
        }
    }
    
    /**
     * Retrieves the indicated variable as an instance of the requested type. Allowable types
     * are <code>{@link java.lang.Integer}</code>, <code>{@link java.lang.Double}</code>, 
     * <code>{@link java.lang.Boolean}</code> and <code>{@link java.lang.String}</code>. If 
     * there is no value for the variable of the specified type, this will throw an
     * <code>{@link InvalidParameterException}</code>. 
     * 
     * <p> 
     * Note that this will return <code>null</code> only if the set value for the indicated 
     * variable is actually <code>null</code>.
     *   
     * @param name The variable whose value should be returned
     * @param type  The expected type of the variable. This must be one of the following:
     *      <ul>
     *        <li><code>{@link java.lang.Integer}</code></li>
     *        <li><code>{@link java.lang.Double}</code></li>
     *        <li><code>{@link java.lang.Boolean}</code></li>
     *        <li><code>{@link java.lang.String}</code>
     *      </ul>
     * @return The value of the request parameter
     * @throws TesseractException If there is an internal error accessing Tesseract
     * @throws InvalidParameterException If the requested variable is not valid for the 
     *      requested type. 
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String name, T type) throws InvalidParameterException, TesseractException {
        requireState(State.INITIALIZED);
        
        int rv = 0;
        if (type instanceof Integer) {
            IntBuffer value = IntBuffer.allocate(1);
            rv = api.TessBaseAPIGetIntVariable(handle, name, value);
            checkGetVariableReturnValue(rv, name);
            return (T)new Integer(value.get());
        } else if (type instanceof Boolean) {
            IntBuffer value = IntBuffer.allocate(1);
            rv = api.TessBaseAPIGetBoolVariable(handle, name, value);
            checkGetVariableReturnValue(rv, name);
            
            return (T)Boolean.valueOf(toBoolean(value.get()));
        } else if (type instanceof Double) {
            DoubleBuffer value = DoubleBuffer.allocate(1);
            rv = api.TessBaseAPIGetDoubleVariable(handle, name, value);
            checkGetVariableReturnValue(rv, name);
            
            return (T)new Double(value.get());
        } else if (type instanceof String) {
            String result = api.TessBaseAPIGetStringVariable(handle, name);
            if (result == null) {
                Map<String, String> map = Collections.singletonMap(name, "");
                throw new InvalidParameterException("Could not retrieve the requested parameter", map);
            }
            
            return (T)result;
        } else {
            Map<String, String> map = Collections.singletonMap(name, "");
            throw new InvalidParameterException(
                    "Could not get variable. Unsupported type: " + type.getClass().getName() + 
                    ".  Excepted Integer, Double, Boolean, or String", map);
        }
    }
    
    /**
     * Print Tesseract parameters to the given file.
     *  
     * <p>
     * Note: This must be called after the handle has been initialized. Until then, variables 
     * cannot be set or accessed.
     * 
     * <p>This method may be called only when the handle is in the initialized state.
     * 
     * @param filename The name of the file to print the variables to.
     * @throws HandleClosedException If this handle has been closed.
     * @throws TesseractException If this handle has not been initialized.
     */
    public void printVariables(String filename) throws HandleClosedException, TesseractException {
        requireState(State.INITIALIZED);
        
        this.api.TessBaseAPIPrintVariables(handle, filename);
    }

    /**
     * Returns the languages loaded by the last to an initialization method, including those 
     * loaded as dependencies of other loaded languages.
     * 
     * <p>This method may be called only when the handle is in an active state.
     * 
     * @return An unmodifiable collection containing the loaded languages
     * @throws TesseractException If the handle is not in an initialized state.
     */
    public Collection<String> getLoadedLanguages() throws TesseractException {
        requireState(ACTIVE_STATES);     
        
        PointerByReference ref = this.api.TessBaseAPIGetLoadedLanguagesAsVector(handle);
        // TODO TEST ME. Not sure that this works, shouldn't (I assume) delete the 
        //               returned values, but may need to delete the array?
        
        Pointer p = ref.getPointer();
        String[] values = p.getStringArray(0);
        Collection<String> languages = new HashSet<String>(values.length);
        for (String value: values) {
            languages.add(value);
        }
        
        return languages;
    }
    
    /**
     * Returns the languages used to initialize this handle as a string. This will return the 
     * language string as it was supplied to this <code>TesseractHandle</code> during 
     * initialization.
     *     
     * <p>This method may be called only when the handle is in an active state.
     * 
     * @return The languages this handle has been initialized for. 
     * @throws TesseractException If the handle has been closed or has not been initialized.
     */
    public String getInitLanguages() throws TesseractException {
        requireState(ACTIVE_STATES);     
        
        // we could call into the native code, but it will be faster to just cache this value
        return this.language;
    }

    /**
     * Read a config file containing a set of name/value pairs. By default this will search in
     * <code>tessdata/configs</code>, and  <code>tessdata/tessconfigs</code> relative to the 
     * root data directory that was supplied during initialization. Absolute file paths may 
     * also be supplied.
     *   
     * <p>This method may be called only when the handle is in an initialized state.
     * 
     * @param filename The relative or absolute path to the config file.
     * @param initOnly Unknown TODO figure out what this value is
     * @throws InvalidStateException 
     */
    public void readConfigFile(String filename, int initOnly) throws InvalidStateException {
        requireState(State.INITIALIZED);
        
        this.api.TessBaseAPIReadConfigFile(handle, filename, initOnly);
    }

    /**
     * Sets the page segmentation mode to be used by this handle.
     * 
     * <p>This method may be called only when the handle is in an initialized state.
     * 
     * @param mode The segmentation mode to use.
     * @throws InvalidStateException
     */
    public void setPageSegMode(PublicTypes.PageSegMode mode) throws InvalidStateException {
        requireState(State.INITIALIZED);
        
        this.api.TessBaseAPISetPageSegMode(handle, mode.value);
    }

    /**
     * Returns the page segmentation mode in use by this handle.
     * 
     * <p>This method may be called only when the handle is in an active state.
     * 
     * @return
     * @throws TesseractException If the handle 
     */
    public PublicTypes.PageSegMode getPageSegMode() throws TesseractException {
        requireState(ACTIVE_STATES);
        
        int mode = this.api.TessBaseAPIGetPageSegMode(handle);
        return PublicTypes.PageSegMode.valueOf(mode); 
    }

    //========================================================================================
    // INITIALIZATION
    //========================================================================================
    
    /**
     * 
     * <p>This method must be called when the handle is in the <code>State.UNINITIALIZED</code>
     * state and changes the state of this handle to <code>State.INITIALIZED</code>.
     * 
     */
    public void init() {
        // TODO Auto-generated method stub
        this.api.TessBaseAPIInitForAnalysePage(handle);
        
    }

    /**
     * 
     * <p>This method must be called when the handle is in the <code>State.UNINITIALIZED</code>
     * state and changes the state of this handle to <code>State.INITIALIZED</code>.
     * 
     * @param datapath
     * @param language
     * @param oem
     * @param configs
     * @param configs_size
     * @throws HandleClosedException If this handle has been closed.
     * @throws TesseractException
     */
    public void init(String datapath, String language, PublicTypes.OcrEngineMode oem, 
            PointerByReference configs, int configs_size) 
            throws TesseractException {
        requireState(State.UNINITIALIZED);
        
        int success = this.api.TessBaseAPIInit1(handle, datapath, language, oem.value, configs, configs_size);
        try {
            if (!toBoolean(success)) {
                throw new TesseractException("Failed to initialize this handle.");
            }
            this.state = State.INITIALIZED;
        } catch (Exception ex) {
            throw new TesseractException("Failed to initialize this handle: Tesseract returned an invalid resposne code.", ex);
        }
    }

    /**
     * 
     * <p>This method must be called when the handle is in the <code>State.UNINITIALIZED</code>
     * state and changes the state of this handle to <code>State.INITIALIZED</code>.
     * 
     * @param datapath The parent of the <code>tessdata</code> directory.
     * @param language The language or languages to be used for recognition.
     * @param oem The OCR engine mode to use. 
     * 
     * @throws TesseractException
     */
    public void init(String datapath, String language, PublicTypes.OcrEngineMode oem) 
            throws TesseractException {
        requireState(State.UNINITIALIZED);
        
        int success = this.api.TessBaseAPIInit2(handle, datapath, language, oem.value);
        if (success != 0) {
            throw new TesseractException("Failed to initialize this handle.");
        }
        
        this.state = State.INITIALIZED;
    }

    /**
     * 
     * <p>This method must be called when the handle is in the <code>State.UNINITIALIZED</code>
     * state and changes the state of this handle to <code>State.INITIALIZED</code>.
     * 
     * @param datapath
     * @param language
     * @throws HandleClosedException If this handle has been closed.
     * @throws TesseractException
     */
    public void init(String datapath, String language) throws TesseractException {
        requireState(State.UNINITIALIZED);
        
        int success = this.api.TessBaseAPIInit3(handle, datapath, language);
        try {
            if (!toBoolean(success)) {
                throw new TesseractException("Failed to initialize this handle.");
            }
        } catch (Exception ex) {
            throw new TesseractException("Failed to initialize this handle: Tesseract returned an invalid resposne code.", ex);
        }
    }
    
    //========================================================================================
    // ANALYSIS & RECOGNITION
    //========================================================================================
    
    /**
     * 
     * @param image
     * @return
     * @throws InvalidStateException
     */
    public String rect(BufferedImage image) throws InvalidStateException {
        requireState(State.INITIALIZED);
        
        // FIXME this results in a memory leak since the returned string is required to be deleted
        // TODO presumably this doesn't result in a state change?
        ImageStats stats = new ImageStats(image);
        return this.api.TessBaseAPIRect(handle, stats.imagedata, stats.bytesPerPixel, stats.bytesPerLine, 
                0, 0, stats.width, stats.height);
        
    }
    
    public String rect(BufferedImage image, Rectangle rectangle) throws InvalidStateException {
        requireState(State.INITIALIZED);
        
        // FIXME this results in a memory leak since the returned string is required to be deleted
        ImageStats stats = new ImageStats(image);
        return this.api.TessBaseAPIRect(handle, stats.imagedata, stats.bytesPerPixel, stats.bytesPerLine, 
                rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    // NOT YET SUPPORTED
    // TessBaseAPIClearAdaptiveClassifier
    // TessBaseAPIRecognizeForChopTest
    
    /* (non-Javadoc)
     * @see org.dharts.dia.tesseract.tess4j.TessAPI#TessBaseAPISetImage(org.dharts.dia.tesseract.tess4j.TessAPI.TessBaseAPI, java.nio.ByteBuffer, int, int, int, int)
     */
    public void setImage(BufferedImage image) throws InvalidStateException {
        requireState(State.INITIALIZED);
        
        ImageStats stats = new ImageStats(image);
        this.api.TessBaseAPISetImage(handle, stats.imagedata, stats.width, stats.height, 
                stats.bytesPerPixel, stats.bytesPerLine);
        
        state = State.IMAGE_SET;
    }

    /* (non-Javadoc)
     * @see org.dharts.dia.tesseract.tess4j.TessAPI#TessBaseAPISetSourceResolution(org.dharts.dia.tesseract.tess4j.TessAPI.TessBaseAPI, int)
     */
    public void setSourceResolution(int ppi) throws InvalidStateException, InvalidParameterException {
        requireState(State.IMAGE_SET);
        
        if (ppi < 0) {
            throw new InvalidParameterException("Invalid pixels per inch. Must be non-negative", 
                    Collections.singletonMap("source resolution", Integer.toString(ppi)));
        }
        
        api.TessBaseAPISetSourceResolution(handle, ppi);
    }

    /* (non-Javadoc)
     * @see org.dharts.dia.tesseract.tess4j.TessAPI#TessBaseAPISetRectangle(org.dharts.dia.tesseract.tess4j.TessAPI.TessBaseAPI, int, int, int, int)
     */
    public void setRectangle(Rectangle rect) throws InvalidStateException {
        requireState(State.IMAGE_SET);
        
        api.TessBaseAPISetRectangle(handle, rect.x, rect.y, rect.width, rect.height);
    }

    /* (non-Javadoc)
     * @see org.dharts.dia.tesseract.tess4j.TessAPI#TessBaseAPIAnalyseLayout(org.dharts.dia.tesseract.tess4j.TessAPI.TessBaseAPI)
     */
    public LayoutHandle analyseLayout() throws InvalidStateException {
        // FIXME NOT THREAD SAFE (applies to most methods in class)
        requireState(State.IMAGE_SET);
        
        TessPageIterator piHandle = api.TessBaseAPIAnalyseLayout(handle);
        HandleContext<TessPageIterator> context = new LayoutHandleContext(piHandle);
                
        this.state = State.ANALYZING;
        return LayoutHandle.create(context, piHandle);
    }
//
//    /* (non-Javadoc)
//     * @see org.dharts.dia.tesseract.tess4j.TessAPI#TessBaseAPIRecognize(org.dharts.dia.tesseract.tess4j.TessAPI.TessBaseAPI, org.dharts.dia.tesseract.tess4j.TessAPI.ETEXT_DESC)
//     */
//    @Override
//    public int TessBaseAPIRecognize(TesseractHandle handle, ETEXT_DESC monitor) {
//        // TODO Auto-generated method stub
//        return 0;
//    }
//
//
    /* (non-Javadoc)
     * @see org.dharts.dia.tesseract.tess4j.TessAPI#TessBaseAPIGetIterator(org.dharts.dia.tesseract.tess4j.TessAPI.TessBaseAPI)
     */
    public ResultHandle recognize() throws InvalidStateException {
        // FIXME NOT THREAD SAFE
        requireState(State.IMAGE_SET);
        
        TessResultIterator riHandle = api.TessBaseAPIGetIterator(handle);
        HandleContext<TessResultIterator> context = new ResultHandleContext(riHandle);
        
        this.state = State.ANALYZING;
        return ResultHandle.create(context, riHandle);
    }

    
    /**
     * Lightweight helper class to extract data and dimensional parameters from a 
     * <code>BufferedImage</code>. Instances are immutable. 
     * 
     * @author Neal Audenaert
     */
    private static class ImageStats {
        private final ByteBuffer imagedata;
        private final int width;
        private final int height;
        private final int bytesPerPixel;
        private final int bytesPerLine;
        
        ImageStats(BufferedImage image) {
            this.imagedata = ImageIOHelper.convertImageData(image);
            this.width = image.getWidth();
            this.height = image.getHeight(); 
            
            int bpp = image.getColorModel().getPixelSize();
            this.bytesPerPixel = bpp / 8;                              // bytes per pixel
            this.bytesPerLine = (int) Math.ceil(width * bpp / 8.0);    // bytes per line
        }
    }
    
    //========================================================================================
    // EXCEPTION CLASSES
    //========================================================================================
    
    class LayoutHandleContext extends HandleContext<TessPageIterator> {

        LayoutHandleContext(TessPageIterator base) {
            super(base);
        }
        
        protected TessPageIterator doCopy(TessPageIterator handle) {
            return api.TessPageIteratorCopy(handle);
        }
        
        protected void doClose(TessPageIterator handle) {
            api.TessPageIteratorDelete(handle);
        }
        
        protected TessAPI getAPI() {
            return api;
        }
        
        protected void release() {
            TesseractHandle.this.state = State.IMAGE_SET;
        }
    }
    
    class ResultHandleContext extends HandleContext<TessResultIterator> {
        
        ResultHandleContext(TessResultIterator base) {
            super(base);
        }
        
        protected TessResultIterator doCopy(TessResultIterator handle) {
            // TODO guard state
            return api.TessResultIteratorCopy(handle);
        }
        
        protected void doClose(TessResultIterator handle) {
            // TODO guard state
            api.TessResultIteratorDelete(handle);
        }
        
        protected TessAPI getAPI() {
            return api;
        }
        
        protected void release() {
            TesseractHandle.this.state = State.IMAGE_SET;
        }
    }
    
    // FIXME replace these with runtime exceptions: IllegalStateException
    public static class HandleClosedException extends TesseractException {
        
    }
    
    public static class InvalidStateException extends TesseractException {
        
        /**
         * 
         */
        public InvalidStateException(State state, Collection<State> expectedStates) {
            // TODO Auto-generated constructor stub
        }
    }
}