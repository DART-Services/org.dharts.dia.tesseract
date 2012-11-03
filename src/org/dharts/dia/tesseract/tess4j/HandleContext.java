/* File: LayoutHandleContext.java
 * Created: Oct 27, 2012
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

import java.util.HashSet;
import java.util.Set;

import org.dharts.dia.tesseract.tess4j.TessAPI.TessPageIterator;

/**
 * Implements the context for cloning and releasing multiple handles to the same underlying
 * set of analysis/recognition results from Tesseract. A context is constructed the first time
 * page handle is created and is then shared between all cloned instances of that handle. 
 * 
 * <p>The {@link #copy(TessPageIterator)} method of this class provides the only correct 
 * mechanism for creating a copy or clone of an existing page iterator. This is called 
 * internally by the static  {@code clone} methods of the {@link LayoutHandle} and 
 * {@link ResultHandle} classes.
 * 
 * <p>The {@link #release(TessPageIterator)} method is called when a {@link BasePageHandle} 
 * is disposed. Once all iterators that have been created via this context have been released, 
 * the context itself is released. 
 * 
 * <p>Concrete implementations of a {@code HandleContext} are intended to be instantiated by 
 * the {@link TesseractHandle} when it constructs a new {@link BasePageHandle}. This allows 
 * the {@link TesseractHandle} to provide access to its internal state management mechanisms 
 * so that it can enable/prevent the creation of new {@link BasePageHandle}s while previously
 * created handles are still in use. 
 *
 * @see TesseractHandle
 * @see BasePageHandle
 * 
 * @param <T> The type of iterator handle this context operates on.
 */
abstract class HandleContext<T extends TessPageIterator> {

    // @GaurdedBy(this)
    protected final Set<T> handles = new HashSet<T>();
    
    /**
     * Creates a handle context given an initial {@link TessPageIterator}.
     * @param base The initial reference to the iterator handle that references the analysis 
     *      and/or recognition results. 
     */
    HandleContext(T base) {
        handles.add(base);
    }
    
    /**
     * Copies an existing iteration handle.
     *  
     * @param handle The iteration handle to be cloned
     * @return A cloned instance of the supplied iteration handle.
     * @throws IllegalArgumentException If the handle to be cloned is not part of this context.
     */
    T copy(T handle) {
        T result = null;
        
        synchronized(this) {
            if (!this.handles.contains(handle)) {
                throw new IllegalArgumentException("Could not copy the supplied iterator. It is not part of this context");
            }
            
            result = doCopy(handle);
            this.handles.add(result);
        }
        
        return result;
    }
    
    /**
     * Releases the supplied handle
     * @param handle The iteration handle to be released
     * @return {@code true} if the handle was released.
     */
    boolean release(T handle) {
        // TODO: evaluate exception handling. What if doClose throws an exception?
        //       Should we throw an IllegalArgumentException if the handle is not found?
        boolean modified = false;
        int remaining = -1;
        synchronized (this) {
            if (this.handles.remove(handle)) {
                doClose(handle);
                remaining = this.handles.size();
                modified = true;
            }
        }
        
        // perform the release operation outside of the synchronize block to reduce
        // chances for deadlock. 
        if (modified && remaining == 0) 
            release();
                
        return modified;
    }

    /** 
     * @return a reference to the Tesseract API instance to be used in conjunction with 
     *      this context. 
     */
    protected abstract TessAPI getAPI();
    
    /**
     * Implemented by subclasses to perform the copy operation for the specific type of 
     * iterator represented by this context (layout analysis or recognition results). Called 
     * by {@link #copy(TessPageIterator)}. 
     *  
     * @param handle The {@link TessPageIterator} to copy.
     * @return The cloned instance.
     */
    protected abstract T doCopy(T handle);
    
    /**
     * Implemented by subclasses to perform the close operation for the specific type of 
     * iterator represented by this context (layout analysis or recognition results). Called 
     * by {@link #release(TessPageIterator)}. 
     *  
     * @param handle The {@link TessPageIterator} to close.
     */
    protected abstract void doClose(T handle);
 
    /**
     * Called once all iteration handles managed by this context have been released. Should 
     * be implemented to notify the {@link TesseractHandle} that instantiated this context 
     * that all references to a particular analysis or recognition result have been released
     * and it is safe to invoke a method on the Tesseract API that will clear the analysis 
     * result (e.g., setting a new image or region to analyze). 
     */
    protected abstract void release();
}