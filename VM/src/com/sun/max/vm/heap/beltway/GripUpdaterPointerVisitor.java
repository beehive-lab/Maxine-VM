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
package com.sun.max.vm.heap.beltway;

import com.sun.max.unsafe.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
/**
 * Generic grip updater visitor for beltway collectors.
 *
 * @author Laurent Daynes
 */
public class GripUpdaterPointerVisitor extends PointerIndexVisitor {

    final Action action;

    public GripUpdaterPointerVisitor(Action action) {
        this.action = action;
    }

    /**
     * Visit the grip at the specified address (pointer + wordIndex) and replace it if the action of this visitor returns
     * a different grip that the one initially seen.
     */
    @Override
    public void visit(Pointer pointer, int wordIndex) {
        final Grip oldGrip = pointer.getGrip(wordIndex);
        // Should we filter null grips here for perf reason ?
        // May have a separate  NullFilterPointerVisitor that do that, and
        // use a VM option to use it instead.
        final Grip newGrip = action.doAction(oldGrip);
        if (newGrip != oldGrip) {
            pointer.setGrip(wordIndex, newGrip);
        }
    }
}
