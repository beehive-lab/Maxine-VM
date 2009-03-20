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
package com.sun.max.vm.compiler.cir;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.cir.transform.*;

/**
 * Super class of all CIR data types.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class CirNode implements Cloneable {

    protected CirNode() {
        _id = _serial++;
    }

    private static int _serial = 0;

    /**
     * This is used to give all CIR nodes a deterministic hash code so that hash-based
     * data structures using CirNodes as a key will always have the same layout. This
     * makes compiler bugs *much* easier to reproduce and fix.
     *
     * Unfortunately, it cannot be {@code final} as it needs to be set in {@link #clone()}.
     */
    @CONSTANT
    private int _id;

    public int id() {
        return _id;
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
        return _id;
    }

    @Override
    public Object clone() {
        try {
            final CirNode result = (CirNode) super.clone();
            result._id = _serial++;
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

    private static final boolean _printingIds = false;

    public static boolean printingIds() {
        return _printingIds;
    }

    public void trace(int level) {
        if (Trace.hasLevel(level)) {
            Trace.line(level, traceToString(false));
        }
    }

    public final String traceToString(boolean showBir) {
        return traceToString(showBir, _printingIds, Integer.MAX_VALUE);
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
