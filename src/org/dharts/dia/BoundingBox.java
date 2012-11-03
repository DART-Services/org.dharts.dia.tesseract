/* File: BoundingBox.java
 * Created: Nov 3, 2012
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
 * Defines the boundary of a particular feature on the page.
 *  
 * @author Neal Audenaert
 */
public class BoundingBox {
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;
    
    public BoundingBox(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    public boolean contains(BoundingBox box) {
        return false;
    }
    
    public boolean intersects(BoundingBox box) {
        return false;
    }

    public BoundingBox intersection(BoundingBox box) {
        return null;
    }
    
    public BoundingBox union(BoundingBox box) {
        return null;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bounding Box: (")
          .append(left).append(", ").append(top).append(") x (")
          .append(right).append(", ").append(bottom).append(")");
        return sb.toString();
    }
}