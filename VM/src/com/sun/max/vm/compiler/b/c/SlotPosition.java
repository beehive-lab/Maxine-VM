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
/*VCSID=65e4fd62-5739-4a85-87e5-1f219e5c6de0*/
package com.sun.max.vm.compiler.b.c;

import com.sun.max.vm.type.*;

/**
 * Position in a Java frame or stack.
 * 
 * If in the sense of the JVM spec, the same slot is occupied by items of different type, we instead reserve a separate
 * slot for each type. This disambiguation simplifies stack merging at basic block boundaries.
 * 
 * @author Bernd Mathiske
 */
class SlotPosition {

    private final Kind _kind;
    private final int _slotIndex;

    SlotPosition(Kind kind, int slotIndex) {
        _kind = kind.toStackKind();
        _slotIndex = slotIndex;
    }

    public Kind getKind() {
        return _kind;
    }

    public int getSlot() {
        return _slotIndex;
    }

    @Override
    public int hashCode() {
        return (_kind.hashCode() ^ _slotIndex) + _slotIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SlotPosition)) {
            return false;
        }
        final SlotPosition position = (SlotPosition) other;
        return _kind == position._kind && _slotIndex == position._slotIndex;
    }

}
