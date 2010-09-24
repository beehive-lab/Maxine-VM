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
package com.sun.max.vm.layout.hosted;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;

/**
 * @author Bernd Mathiske
 */
public class HostedHybridLayout extends HostedWordArrayLayout implements HybridLayout {

    @Override
    public Layout.Category category() {
        return Layout.Category.HYBRID;
    }

    @Override
    public boolean isHybridLayout() {
        return true;
    }

    @Override
    public boolean isArrayLayout() {
        return false;
    }

    public int firstAvailableWordArrayIndex(Size initialCellSize) {
        return 0;
    }

    private final HostedTupleLayout tupleLayout;

    public HostedHybridLayout(GripScheme gripScheme) {
        super(gripScheme);
        tupleLayout = new HostedTupleLayout(gripScheme);
    }

    public Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors) {
        return tupleLayout.layoutFields(superClassActor, fieldActors);
    }

    @INLINE
    public int getFieldOffsetInCell(FieldActor fieldActor) {
        return tupleLayout.getFieldOffsetInCell(fieldActor);
    }
}
