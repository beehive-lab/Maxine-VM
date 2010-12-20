/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
