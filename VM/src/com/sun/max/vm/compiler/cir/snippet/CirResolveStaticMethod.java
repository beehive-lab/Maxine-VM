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
/*VCSID=4d18985b-50b4-4aa1-b9d3-13415fb97216*/
package com.sun.max.vm.compiler.cir.snippet;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.runtime.*;

/**
 * We override here to retrieve a CIR method instead of a target method
 * and to resolve builtins.
 *
 * @author Bernd Mathiske
 */
public final class CirResolveStaticMethod extends CirSpecialSnippet {

    public CirResolveStaticMethod() {
        super(ResolutionSnippet.ResolveStaticMethod.SNIPPET);
    }

    private enum Parameter {
        guard, normalContinuation, exceptionContinuation;
    }

    @Override
    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) {
        final EntrypointResolutionGuard guard = (EntrypointResolutionGuard) getConstantArgumentValue(arguments, Parameter.guard).asObject();
        final ConstantPool constantPool = guard.constantPool();
        final StaticMethodActor selectedMethod = ResolutionSnippet.ResolveStaticMethod.quasiFold(constantPool, guard.constantPoolIndex());
        return new CirCall(getNormalContinuation(arguments), builtinOrMethod(selectedMethod, cirOptimizer.cirGenerator()));
    }
}
