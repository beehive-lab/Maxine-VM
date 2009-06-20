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
package com.sun.max.vm.compiler.eir.allocate.linearscan;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

/**
 * Parent interval that represent a certain original variable. When intervals are split, they are still organized in
 * this data structure.
 *
 * @author Thomas Wuerthinger
 */
public final class ParentInterval {

    private AppendableSequence<Interval> children;
    private EirVariable slotVariable;
    private boolean spillSlotDefined;

    public ParentInterval() {
        children = new ArrayListSequence<Interval>(3);
    }

    public boolean spillSlotDefined() {
        return spillSlotDefined;
    }

    public void setSpillSlotDefined(boolean b) {
        spillSlotDefined = b;
    }

    public Sequence<Interval> children() {
        return children;
    }

    public String detailedToString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("(");
        for (Interval i : children()) {
            sb.append(i.detailedToString());
            sb.append("; ");
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("(");
        for (Interval i : children()) {
            sb.append(i.toString());
            sb.append("; ");
        }

        sb.append(")");
        return sb.toString();
    }

    public Interval createChild(EirVariable variable) {
        final Interval result = new Interval(this, variable);
        variable.setInterval(result);
        children.append(result);
        return result;
    }

    public Interval previousChild(Interval current) {

        // Children may be completely unsorted
        Interval prev = null;
        for (Interval i : children) {
            if (i != current && i.getLastRangeEnd() <= current.getFirstRangeStart()) {
                if (prev == null || i.getLastRangeEnd() > prev.getLastRangeEnd()) {
                    prev = i;
                }
            }
        }

        assert prev.getLastRangeEnd() <= current.getFirstRangeStart();

        return prev;
    }

    public Interval getChildAt(int endNumber) {

        for (Interval i : children) {
            if (i.covers(endNumber)) {
                return i;
            }
        }

        // TODO (tw): Check why this is needed!
        for (Interval i : children) {
            if (i.coversEndInclusive(endNumber)) {
                return i;
            }
        }

        return null;
    }

    public boolean hasSlotVariable() {
        return slotVariable != null;
    }

    public EirVariable slotVariable(EirMethodGeneration generation) {

        if (slotVariable == null) {
            final Kind kind = children.first().variable().kind();

            for (Interval i : children) {
                assert i.variable().kind() == kind;
                if (i.variable().location() != null && i.variable().location().asStackSlot() != null) {
                    slotVariable = i.variable();
                }
            }

            if (slotVariable == null) {
                slotVariable = generation.createEirVariable(kind);
                slotVariable.fixLocation(generation.allocateSpillStackSlot());
            }
        }

        return slotVariable;
    }
}
