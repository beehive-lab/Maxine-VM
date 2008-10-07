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
/*VCSID=a062cc65-eac2-489f-a064-89291ba02cec*/
package com.sun.max.vm.compiler.cir.optimize;

import com.sun.max.vm.compiler.cir.*;

/**
 * The meet-semilattice abstract class for dataflow analysis.
 *
 * @author Yi Guo
 */
public abstract class AbstractValueDomain<T extends AbstractValue<T>> {
    public final T meet(T a, T b) {
        if (a == b) {
            return a;
        }
        if (a.isBottom() || b.isBottom()) {
            return getBottom();
        }
        if (a.isTop()) {
            return b;
        }
        if (b.isTop()) {
            return a;
        }
        return a.meetNontrivial(b);
    }

    public final boolean equal(T a, T b) {
        if (a == b) {
            return true;
        }
        if (a.isTop() || a.isBottom() || b.isTop() || b.isBottom()) {
            return false;
        }
        return a.eqNontrivial(b);
    }

    public final boolean lessOrEqual(T a, T b) {
        if (a.isBottom()) {
            return true;
        }
        if (a.isTop()) {
            return b.isTop();
        }
        if (b.isBottom()) {
            return false;
        }
        if (b.isTop()) {
            return true;
        }
        if (a == b) {
            return true;
        }
        return a.lessOrEqualNontrivial(b);
    }

    public abstract T fromConstant(CirConstant c);

    public abstract T getTop();
    public abstract T getBottom();
}
