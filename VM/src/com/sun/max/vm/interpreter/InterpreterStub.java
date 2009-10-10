/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.interpreter;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 *
 * @author Paul Caprioli
 */
public abstract class InterpreterStub extends TargetMethod {

    public InterpreterStub(ClassMethodActor classMethodActor, InterpreterStubCompiler compilerScheme, TargetABI abi) {
        super("InterpStub:" + classMethodActor, compilerScheme, abi);
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        FatalError.unimplemented();
    }

    @Override
    @HOSTED_ONLY
    public void gatherCalls(AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        FatalError.unimplemented();
    }

    @Override
    public void patchCallSite(int callOffset, Word callEntryPoint) {
        FatalError.unimplemented();
    }

    @Override
    public Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass) {
        return Address.zero();
    }
}
