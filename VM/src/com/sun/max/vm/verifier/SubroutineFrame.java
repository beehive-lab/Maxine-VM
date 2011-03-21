/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import com.sun.max.vm.verifier.types.*;

/**
 * An object for encapsulating the linkage of nested subroutine calls.
 *
 * @author Doug Simon
 */
public class SubroutineFrame {

    /**
     * Shared object used to denote the result of merging two or more subroutines.
     */
    public static final Subroutine MERGED_SUBROUTINE = SUBROUTINE;

    /**
     * Constant denoting the top level caller of all subroutines which is not a subroutine itself.
     */
    public static final SubroutineCall TOP = new SubroutineCall(null, null, null);

    final Subroutine subroutine;
    int depth;
    SubroutineFrame parent;

    public SubroutineFrame(Subroutine subroutine, SubroutineFrame parent) {
        assert (subroutine != null && parent != null) || TOP == null;
        this.subroutine = subroutine;
        this.parent = parent;
        this.depth = parent == null ? 0 : 1 + parent.depth;
    }

    /**
     * Determines if this subroutine context or any of its callers is for a given subroutine.
     */
    public boolean contains(Subroutine subroutine) {
        if (subroutine == this.subroutine) {
            return true;
        }
        return parent == null ? false : parent.contains(subroutine);
    }

    public SubroutineFrame parent() {
        return parent;
    }

    public void reparent(SubroutineFrame newParent) {
        assert newParent != null;
        this.parent = newParent;
        this.depth = 1 + newParent.depth;
    }

    public SubroutineFrame[] ancestors() {
        SubroutineFrame[] result = new SubroutineFrame[depth + 1];
        int i = depth + 1;
        SubroutineFrame sf = this;
        while (sf != null) {
            result[--i] = sf;
            sf = sf.parent;
        }
        assert result[0] == TOP;
        return result;
    }

    /**
     * Merges this context with another. If the result of the merge is identical to this context, then this context object is returned.
     */
    public SubroutineFrame merge(SubroutineFrame subroutineFrame) {
        assert depth == subroutineFrame.depth;
        if (subroutineFrame == this) {
            return this;
        }
        final SubroutineFrame mergedParent = parent.merge(subroutineFrame.parent());
        if (subroutine != subroutineFrame.subroutine) {
            if (subroutine == MERGED_SUBROUTINE && mergedParent == parent) {
                return this;
            }
            return new SubroutineFrame(MERGED_SUBROUTINE, mergedParent);
        }
        if (mergedParent == parent) {
            return this;
        }
        return new SubroutineFrame(subroutine, mergedParent);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(subroutine == null ? "method-entry-frame" : subroutine.toString());
        if (parent != null) {
            sb.append("\n    called from ").append(parent.toString());
        }
        return sb.toString();
    }
}
