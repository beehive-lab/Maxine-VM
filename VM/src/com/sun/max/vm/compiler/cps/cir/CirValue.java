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
package com.sun.max.vm.compiler.cps.cir;

import com.sun.max.vm.compiler.cps.cir.transform.*;
import com.sun.max.vm.compiler.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Representation of runtime values in CIR.
 *
 * @author Bernd Mathiske
 */
public abstract class CirValue extends CirNode implements IrValue {

    private final Kind kind;

    public Kind kind() {
        return kind;
    }

    protected CirValue(Kind kind) {
        this.kind = kind;
    }

    public boolean isCategory1() {
        return kind().isCategory1();
    }

    public boolean isCategory2() {
        return kind().isCategory2();
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitValue(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitValue(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformValue(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateValue(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateValue(this);
    }

    /**
     * Determines if all but the last two arguments (i.e. the continuation values)
     * in a CIR argument array are constant.
     */
    public static boolean areConstant(CirValue[] arguments) {
        for (int i = 0; i < arguments.length - 2; i++) {
            if (!arguments[i].isConstant()) {
                return false;
            }
        }
        return true;
    }

    public boolean isConstant() {
        return false;
    }

    public final boolean isScalarConstant() {
        if (isConstant()) {
            if (kind() != Kind.REFERENCE) {
                return true;
            }
            final CirConstant constant = (CirConstant) this;
            return constant.value().isZero();
        }
        return false;
    }

    public boolean isResolvedOffset() {
        return false;
    }

    public boolean isResolvedFieldConstant() {
        return false;
    }

    public Value value() {
        throw new RuntimeException(this + " is not a value, but a " + getClass());
    }

    public static final class Undefined extends CirValue {
        private Undefined() {
            super(Kind.VOID);
        }

        @Override
        public void acceptVisitor(CirVisitor visitor) {
            visitor.visitUndefined(this);
        }

        @Override
        public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
            visitor.visitUndefined(this, scope);
        }

        @Override
        public String toString() {
            return "UNDEFINED";
        }
    }

    public static final Undefined UNDEFINED = new Undefined();
}
