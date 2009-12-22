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

package com.sun.max.vm.compiler.cps.cir.optimize;

import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An abstract data domain whose purpose is distinguishing between
 * null and nonnull (initialized) values.  Illustrated below
 * <pre>
 *                TOP
 *               /   \
 *              /     \
 *            NULL   INIT
 *              \     /
 *               \   /
 *               BOTTOM
 * </pre>
 * @author Aziz Ghuloum
 */
public final class InitializedDomain extends AbstractValueDomain<InitializedDomain.Set> {

    @Override
    public Set fromConstant(CirConstant c) {
        final Value v = c.value();
        if (v.kind() == Kind.REFERENCE) {
            final Object o = v.asObject();
            if (o == null) {
                return NULL;
            }
            return INITIALIZED;
        }
        return TOP;
    }

    @Override
    public Set getBottom() {
        return BOTTOM;
    }

    @Override
    public Set getTop() {
        return TOP;
    }

    public Set getInitialized() {
        return INITIALIZED;
    }

    public static final class Set extends AbstractValue<Set> {

        private final String name;

        private Set(String name) {
            this.name = name;
        }

        @Override
        boolean eqNontrivial(Set v) {
            return this == v;
        }

        @Override
        boolean isBottom() {
            return this == BOTTOM;
        }

        @Override
        boolean isTop() {
            return this == TOP;
        }

        boolean isInitialized() {
            return this == INITIALIZED;
        }

        boolean isNotInitialized() {
            return this == NULL;
        }

        @Override
        boolean lessOrEqualNontrivial(Set v) {
            return false;
        }

        @Override
        Set meetNontrivial(Set v) {
            return BOTTOM;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        Set[] createArray(int length) {
            return new Set[length];
        }
    }

    private static final Set TOP         = new Set("TOP-AV");
    private static final Set BOTTOM      = new Set("BOT-AV"); /* may or may not be initialized */
    private static final Set INITIALIZED = new Set("INIT-AV");
    private static final Set NULL        = new Set("NULL-AN");

    public static final InitializedDomain DOMAIN = new InitializedDomain();
}
