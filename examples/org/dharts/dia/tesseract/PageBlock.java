/* File: PageBlock.java
 * Created: Sep 22, 2012
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

import org.dharts.dia.BoundingBox;
import org.dharts.dia.tesseract.LayoutIterator.BlockOrientation;
import org.dharts.dia.tesseract.LayoutIterator.Level;
import org.dharts.dia.tesseract.PublicTypes.PolyBlockType;

public class PageBlock extends PageItem {
    private final PolyBlockType type;
    private final BlockOrientation orientation;

    PageBlock(PolyBlockType type, BlockOrientation orientation, BoundingBox box) {
        super(Level.BLOCK, box);
        this.type = type;
        this.orientation = orientation;
    }

    /**
     * @return the type
     */
    public PolyBlockType getType() {
        return type;
    }

    /**
     * @return the orientation
     */
    public BlockOrientation getOrientation() {
        return orientation;
    }
}