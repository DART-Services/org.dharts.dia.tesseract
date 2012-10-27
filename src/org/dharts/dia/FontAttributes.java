/* File: FontAttributes.java
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
package org.dharts.dia;

/**
 * Describes the font face and typographic details used for a particular segment of text. 
 * Instances of this class are immutable and should be constructed with the provided 
 * {@code Builder}.
 *  
 * @author Neal Audenaert
 */
public class FontAttributes {
    // TODO Allow nullable values. We need a way to say that a particular property is 
    //      mixed within a section.
    private final boolean bold;
    private final boolean italic;
    private final boolean underlined;
    private final boolean monospace;
    private final boolean serif;
    private final boolean smallcaps;
    private final int pointsize;
    private final int fontId;
    private final String fontName;

    private FontAttributes(boolean bold, 
                           boolean italic,
                           boolean underlined,
                           boolean monospace, 
                           boolean serif, 
                           boolean smallcaps, 
                           int pointsize, 
                           int fontId, 
                           String fontName) {
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.monospace = monospace;
        this.serif = serif;
        this.smallcaps = smallcaps;
        this.pointsize = pointsize;
        this.fontId = fontId;
        this.fontName = fontName;
    }
    
    /** @return <code>true</code> if the represented word is bold. */
    public boolean isBold() {
        return bold;
    }

    /** @return <code>true</code> if the represented word is italicized. */
    public boolean isItalic() {
        return italic;
    }

    /** @return <code>true</code> if the represented word is underlined. */
    public boolean isUnderlined() {
        return underlined;
    }

    /** @return <code>true</code> if the represented word is in a monospace font. */
    public boolean isMonospace() {
        return monospace;
    }

    /** @return <code>true</code> if the represented word is in a serif font. */
    public boolean isSerif() {
        return serif;
    }

    /** @return <code>true</code> if the represented word is in small caps. */
    public boolean isSmallcaps() {
        return smallcaps;
    }

    /** @return the pointsize of this word in printers points (1/72 inch.)
     */
    public int getPointsize() {
        return pointsize;
    }

    /** @return the identifier for the font used. */
    public int getFontId() {
        return fontId;
    }
    
    /** @return the name of the font used. */
    public String getFontName() {
        return fontName;
    }
    
    /**
     * Used to build the FontAttributes object. Once built, a FontAttribute instance should be
     * immutable.
     * 
     * @author Neal Audenaert
     */
    public static class Builder {
        
        private boolean bold = false;
        private boolean italic = false;
        private boolean underlined = false;
        private boolean monospace = false;
        private boolean serif = false;
        private boolean smallcaps = false;
        private int pointsize = Integer.MIN_VALUE;
        private int fontId = Integer.MIN_VALUE;
        private String fontName = "";
        
        public Builder() {
            
        }

        public Builder setIsBold(boolean flag) {
            bold = flag;
            return this;
        }

        public Builder setIsItalic(boolean flag) {
            italic = flag;
            return this;
        }

        public Builder setIsUnderline(boolean flag) {
            underlined = flag;
            return this;
        }

        public Builder setIsMonospace(boolean flag) {
            monospace = flag;
            return this;
        }

        public Builder setIsSerif(boolean flag) {
            serif = flag;
            return this;
        }

        public Builder setIsSmallcaps(boolean flag) {
            smallcaps = flag;
            return this;
        }

        public Builder setPointSize(int size) {
            pointsize = size;
            return this;
        }

        public Builder setFontId(int id) {
            fontId = id;
            return this;
        }
        
        public Builder setFontName(String name) {
            fontName = name;
            return this;
        }
        
        public FontAttributes build() {
            return new FontAttributes(bold, italic, underlined, monospace, serif, smallcaps, pointsize, fontId, fontName);
        }
    }
}