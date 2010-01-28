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
package com.sun.max.vm.cps.jit;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Template based JIT compiler.
 *
 * @author Laurent Daynes
 */
public abstract class JitCompiler extends AbstractVMScheme implements RuntimeCompilerScheme {

    @HOSTED_ONLY
    private boolean isInitialized;

    protected JitCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.COMPILING) {
            init();
        }
    }

    @HOSTED_ONLY
    private void init() {
        synchronized (this) {
            if (!isInitialized) {
                targetGenerator().initialize();
                isInitialized = true;
            }
        }
    }

    protected abstract TemplateBasedTargetGenerator targetGenerator();

    public JitTargetMethod compile(ClassMethodActor classMethodActor) {
        if (MaxineVM.isHosted()) {
            init();
        }
        return (JitTargetMethod) targetGenerator().makeIrMethod(classMethodActor);
    }

    @HOSTED_ONLY
    public void gatherCalls(TargetMethod targetMethod, AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        try {
            final BytecodeVisitor bytecodeVisitor = new InvokedMethodRecorder(targetMethod.classMethodActor(), directCalls, virtualCalls, interfaceCalls);
            final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodeVisitor);
            bytecodeScanner.scan(targetMethod.classMethodActor());
        } catch (Throwable throwable) {
            ProgramError.unexpected("could not scan byte code", throwable);
        }
    }
}
