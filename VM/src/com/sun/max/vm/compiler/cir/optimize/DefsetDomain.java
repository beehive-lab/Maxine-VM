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
package com.sun.max.vm.compiler.cir.optimize;

import java.util.*;

import com.sun.max.vm.compiler.cir.*;
/**
 * Typeset defines the abstract value domain for ClassTypeAnalysis Its internal representation is a set of TypeElements.
 *
 * @see TypesetElement
 *
 * @author Yi Guo
 */
public final class DefsetDomain extends AbstractValueDomain<DefsetDomain.Defset> {
    public static final DefsetDomain singleton = new DefsetDomain();
    private final Defset _top;
    private final Defset _bottom;

    private DefsetDomain() {
        _top = new Defset();
        _bottom = new Defset();
    }


    @Override
    public Defset fromConstant(CirConstant c) {
        final Defset s = new Defset();
        s._set.add(c);
        return s;
    }

    public Defset fromNode(CirNode node) {
        final Defset s = new Defset();
        s._set.add(node);
        return s;
    }

    @Override
    public Defset getBottom() {
        return _bottom;
    }

    @Override
    public Defset getTop() {
        return _top;
    }

    public final class Defset extends AbstractValue<Defset> {
        private final Set<CirNode> _set;
        private Defset() {
            _set = new HashSet<CirNode>();
        }
        @Override
        boolean isTop() {
            return this == _top;
        }
        @Override
        boolean isBottom() {
            return this == _bottom;
        }
        @Override
        public Defset meetNontrivial(Defset b) {
            final Defset a = new Defset();
            a._set.addAll(this._set);
            a._set.addAll(b._set);
            return a;
        }
        @Override
        public boolean eqNontrivial(Defset b) {
            return _set.equals(b._set);
        }

        public Set<CirNode> getSet() {
            return _set;
        }

        @Override
        public String toString() {
            if (this == _top) {
                return "TOP";
            }
            if (isBottom()) {
                return "BOTTOM";
            }
            final StringBuilder retString = new StringBuilder();
            for (CirNode node : _set) {
                if (retString.length() > 0) {
                    retString.append(", ");
                }
                retString.append(node);
                if (node instanceof CirCall) {
                    retString.append(((CirCall) node).procedure());
                }
            }
            return retString.toString();
        }
        @Override
        boolean lessOrEqualNontrivial(Defset v) {
            return _set.containsAll(v._set);
        }
        @Override
        Defset[] createArray(int length) {
            return new Defset[length];
        }
    }
}
