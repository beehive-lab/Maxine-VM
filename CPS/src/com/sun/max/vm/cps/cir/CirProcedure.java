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
package com.sun.max.vm.cps.cir;

import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Something that can be called with arguments.
 *
 * Note that in CIR no call ever returns, because we use CPS.
 * This is one reason why we do not call this class "Function".
 * The other is that everything can potentially have side-effects.
 *
 * @author Bernd Mathiske
 */
public abstract class CirProcedure extends CirValue {

    protected CirProcedure() {
        super(Kind.WORD);
    }

    /**
     * Gets the kinds of the parameters declared by this procedure. This does not include the normal and exception
     * continuation parameters.
     *
     * @return the kinds of the parameters declared by this procedure. A {@code null} value in the returned array
     *         indicates that either a {@link Kind#WORD} or {@link Kind#REFERENCE} argument is accepted for the
     *         corresponding parameter position.
     */
    public abstract Kind[] parameterKinds();

    public static boolean isConstantArgument(CirValue[] arguments, Enum index) {
        return arguments[index.ordinal()].isConstant();
    }

    private static Value getConstantArgumentValue(CirValue[] arguments, int index) {
        final CirConstant cirConstant = (CirConstant) arguments[index];
        return cirConstant.value();
    }

    public static Value getConstantArgumentValue(CirValue[] arguments, Enum index) {
        return getConstantArgumentValue(arguments, index.ordinal());
    }

    public static CirValue getNormalContinuation(CirValue[] arguments) {
        return arguments[arguments.length - 2];
    }

    public static CirValue getExceptionContinuation(CirValue[] arguments) {
        return arguments[arguments.length - 1];
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitProcedure(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitProcedure(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformProcedure(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateProcedure(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateProcedure(this);
    }
}
