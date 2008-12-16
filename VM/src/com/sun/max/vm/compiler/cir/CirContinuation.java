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

import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * A closure that is regarded as a continuation. Note that
 * {@link com.sun.max.vm.compiler.cir.optimize.CirConstantBlockArgumentsPropagation} depends on CirContinuations
 * being distinguishable from CirClosures.
 *
 * <p>
 * Continuations always have zero or one parameters. The single parameter
 * will hold the "return value" from the call which is being continued.
 * <p>
 * In a {@link CirPrinter trace}, a continuation is printed as
 * cont[] . { body }
 *
 * @author Bernd Mathiske
 */
public final class CirContinuation extends CirClosure {

    public CirContinuation(CirVariable parameter) {
        super(null);
        setParameters(parameter);
    }

    public CirContinuation() {
        super(null);
        setParameters();
    }

    @Override
    public String toString() {
        return "<CirContinuation>";
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitContinuation(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitContinuation(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformContinuation(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateContinuation(this);
    }

    @Override
    public boolean acceptPredicate(CirPredicate predicate) {
        return predicate.evaluateContinuation(this);
    }

}
