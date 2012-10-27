/* File: PageItem.java
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

import java.util.UUID;

import org.dharts.dia.tesseract.LayoutIterator.BoundingBox;
import org.dharts.dia.tesseract.LayoutIterator.Level;

public class PageItem {
    // TODO add notion of parent item
    private final UUID uuid = UUID.randomUUID();
    private final Level level;
    private final BoundingBox box;

    PageItem(Level level, BoundingBox box) {
        this.level = level;
        this.box = box;
    }

    /**
     * @return the uuid
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return the box
     */
    public BoundingBox getBox() {
        return box;
    }
}