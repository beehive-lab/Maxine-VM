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
package com.sun.max.vm.cps.cir;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.transform.*;

/**
 * Super class of all CIR data types.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class CirNode implements Cloneable {

    protected CirNode() {
        id = serial++;
    }

    private static int serial = 0;

    /**
     * This is used to give all CIR nodes a deterministic hash code so that hash-based
     * data structures using CirNodes as a key will always have the same layout. This
     * makes compiler bugs *much* easier to reproduce and fix.
     *
     * Unfortunately, it cannot be {@code final} as it needs to be set in {@link #clone()}.
     */
    @CONSTANT
    private int id;

    public int id() {
        return id;
    }

    public boolean equals(Object other, CirVariableRenaming renaming) {
        return equals(other);
    }

    @Override
    public boolean equals(Object other) {
        return other == this;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public Object clone() {
        try {
            final CirNode result = (CirNode) super.clone();
            result.id = serial++;
            return result;
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw ProgramError.unexpected("could not clone CirNode: " + this, cloneNotSupportedException);
        }
    }

    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitNode(this);
    }

    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitNode(this, scope);
    }

    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformNode(this);
    }

    public boolean acceptUpdate(CirUpdate update) {
        return update.updateNode(this);
    }

    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateNode(this);
    }

    private static final boolean printingIds = false;

    public static boolean printingIds() {
        return printingIds;
    }

    public void trace(int level) {
        if (Trace.hasLevel(level)) {
            Trace.line(level, traceToString(false));
        }
    }

    public final String traceToString(boolean showBir) {
        return traceToString(showBir, printingIds, Integer.MAX_VALUE);
    }

    /**
     * @see CirPrinter#CirPrinter(PrintStream, CirNode, boolean, int)
     */
    public final String traceToString(boolean showBir, boolean printIds, int nesting) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        acceptVisitor(new CirPrinter(new PrintStream(byteArrayOutputStream), this, printIds, nesting));
        return byteArrayOutputStream.toString();
    }
}
