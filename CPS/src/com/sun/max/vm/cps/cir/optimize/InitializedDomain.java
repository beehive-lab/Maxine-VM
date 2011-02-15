/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.vm.cps.cir.*;
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
        if (v.kind().isReference) {
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
