/* Copyright @ 2010 Quan Nguyen
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
 * 
 * This work has been adapted by Neal Audenaert (2012). Modifications consist chiefly  
 * of code reorganization and changes to the JavaDocs. For more detail on the original  
 * work, please see the Tess4J project (http://tess4j.sourceforge.net/).
 */
package org.dharts.dia.tesseract;

/**
 * An exception occurred when working with Tesseract. As a historical note, this was 
 * originally part of the Tess4J library but has been moved into this package so that 
 * it can be part of the public API of the DHARTS Tesseract wrapper (Tess4J is used 
 * internally, but is not intended for public access).
 * 
 * @author Quan Nguyen
 */
public class TesseractException extends Exception {
    // TODO we need to move this (or some variant of this) back into the Tess4J code base
    //      and make it extend a better top-level exception. This will allow us to make the
    //      constructors package private. In general, we need to think through the exception
    //      handling strategy in more detail.
    
    public TesseractException() {
        super();
    }

    public TesseractException(String message) {
        super(message);
    }

    public TesseractException(Throwable cause) {
        super(cause);
    }

    public TesseractException(String message, Throwable cause) {
        super(message, cause);
    }
}

