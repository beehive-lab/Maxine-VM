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
/*VCSID=da17b60d-4c5c-4074-9f28-6c112e137ef4*/
package com.sun.max.vm.compiler.cir.snippet;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.snippet.*;

/**
 * CIR snippets that specialize the generic CirSnippet class
 * and therefore have a different constructor.
 *
 * @author Bernd Mathiske
 */
public abstract class CirSpecialSnippet extends CirSnippet {

    protected CirSpecialSnippet(Snippet snippet) {
        super(snippet);
        register();
    }

    protected CirCall createExceptionCall(Throwable throwable, CirValue[] arguments) {
        return CirRoutine.Static.createExceptionCall(throwable, arguments);
    }

    protected CirProcedure builtinOrMethod(ClassMethodActor classMethodActor, CirGenerator cirGenerator) {
        if (classMethodActor.isBuiltin()) {
            return CirBuiltin.get(Builtin.get(classMethodActor));
        }
        return cirGenerator.createIrMethod(classMethodActor);
    }
}
