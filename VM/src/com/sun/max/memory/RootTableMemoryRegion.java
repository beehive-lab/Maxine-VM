/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.memory;

import java.lang.management.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;


/**
 * A runtime allocated region of memory used to hold roots; usage is
 * counted in number of words allocated as of the most recent GC.
 *
 * @author Michael Van De Vanter
 */
public final class RootTableMemoryRegion extends MemoryRegion {

    public RootTableMemoryRegion(String description) {
        super(description);
    }

    /**
     * The number of words in the table that are used, current
     * as of most recent GC.
     */
    @INSPECTED
    public volatile long wordsUsed = 0;

    /**
     * Records in this region the number of words that are actually being used in the table.
     */
    public void setWordsUsed(long wordsUsed) {
        this.wordsUsed = wordsUsed;
    }

    /**
     * Gets the number of words used, current as of the most recent GC.
     */
    public long wordsUsed() {
        return wordsUsed;
    }

    @Override
    public MemoryUsage getUsage() {
        final long sizeAsLong = size.toLong();
        return new MemoryUsage(sizeAsLong, wordsUsed * Word.size(), sizeAsLong, sizeAsLong);
    }

}
