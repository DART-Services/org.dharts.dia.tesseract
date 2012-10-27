/* File: InvalidParameterException.java
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

import java.util.Map;

/**
 * Indicates that one or more configuration parameters were invalid.
 * 
 * @author Neal Audenaert
 */
public class InvalidParameterException extends TesseractException {
    public final Map<String, String> badValues;
    
    public InvalidParameterException(String msg, Map<String, String> badValues) {
        super(msg);
        this.badValues = badValues;
    }
    
    @Override
    public String getMessage() {
        String msg = super.getMessage();
        for (String name : badValues.keySet()) {
            msg += "\n\t" + name + " = " + badValues.get(name); 
        }
        
        return msg;
    }

    @Override
    public String getLocalizedMessage() {
        String msg = super.getLocalizedMessage();
        for (String name : badValues.keySet()) {
            msg += "\n\t" + name + " = " + badValues.get(name); 
        }
        
        return msg;
    }
}
