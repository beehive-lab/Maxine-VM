/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>Base</code> instruction represents the entry block of the procedure that has
 * both the standard entry and the OSR entry as successors.
 *
 * @author Ben L. Titzer
 */
public class Base extends BlockEnd {

    /**
     * Constructs a new Base instruction.
     * @param standardEntry the standard entrypoint block
     * @param osrEntry the OSR entrypoint block
     */
    public Base(BlockBegin standardEntry, BlockBegin osrEntry) {
        super(ValueType.ILLEGAL_TYPE, null, false);
        assert osrEntry == null || osrEntry.isOsrEntry();
        assert standardEntry.isStandardEntry();
        if (osrEntry != null) {
            successors.add(osrEntry);
        }
        successors.add(standardEntry);
        // TODO: Check why we need this?!
        this.pin();
    }

    /**
     * Gets the standard entrypoint block.
     * @return the standard entrypoint block
     */
    public BlockBegin standardEntry() {
        return defaultSuccessor();
    }

    /**
     * Gets the OSR entrypoint block, if it exists.
     * @return the OSR entrypoint bock, if it exists; <code>null</code> otherwise
     */
    public BlockBegin osrEntry() {
        return successors.size() < 2 ? null : successors.get(0);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitBase(this);
    }
}
