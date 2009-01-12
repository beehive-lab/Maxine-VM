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
package com.sun.max.vm.compiler.cir.snippet;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.snippet.*;

/**
 * Eliminate all checkcasts referring to subtypes of Word.
 * These are gratuitously inserted by javac.
 * They would cause the target VM to crash.
 *
 * @author Bernd Mathiske
 */
public final class CirCheckCast extends CirSpecialSnippet {

    public CirCheckCast() {
        super(Snippet.CheckCast.SNIPPET);
    }

    private enum Parameter {
        classActor, object, normalContinuation, exceptionContinuation
    }

    @Override
    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        if (isConstantArgument(arguments, Parameter.classActor)) {
            final ClassActor classActor = (ClassActor) getConstantArgumentValue(arguments, Parameter.classActor).asObject();
            if (Word.class.isAssignableFrom(classActor.toJava())) {
                return true;
            }
        }
        return super.isFoldable(cirOptimizer, arguments);
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) {
        if (isConstantArgument(arguments, Parameter.classActor)) {
            final ClassActor classActor = (ClassActor) getConstantArgumentValue(arguments, Parameter.classActor).asObject();
            if (Word.class.isAssignableFrom(classActor.toJava())) {
                return new CirCall(getNormalContinuation(arguments), CirCall.NO_ARGUMENTS);
            }
        }
        return super.fold(cirOptimizer, arguments);
    }

}
