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
package com.sun.max.vm.compiler;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.trampoline.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AbstractCompiler extends AbstractVMScheme implements CompilerScheme {

    public AbstractCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public abstract IrGenerator irGenerator();

    @Override
    public void initialize(Phase phase) {
        super.initialize(phase);

        if (phase == Phase.PROTOTYPING || phase == Phase.STARTING) {
            IrObserverConfiguration.attach(irGenerators());
        }
    }

    @Override
    public void finalize(Phase phase) {
        super.finalize(phase);

        if (phase == Phase.RUNNING) {
            for (IrGenerator generator : irGenerators()) {
                generator.notifyAfterFinish();
            }
        }
    }

    public long numberOfCompilations() {
        return irGenerator().numberOfCompilations();
    }

    @PROTOTYPE_ONLY
    public void createBuiltins(PackageLoader packageLoader) {
        packageLoader.loadAndInitializeAll(Builtin.class);
        Builtin.initialize();
    }

    @PROTOTYPE_ONLY
    public void createSnippets(PackageLoader packageLoader) {
        packageLoader.loadAndInitializeAll(Snippet.class);
        packageLoader.loadAndInitializeAll(HotpathSnippet.class);
        packageLoader.loadAndInitializeAll(DynamicTrampoline.class);
    }

    @PROTOTYPE_ONLY
    private boolean areSnippetsCompiled = false;

    @PROTOTYPE_ONLY
    public boolean areSnippetsCompiled() {
        return areSnippetsCompiled;
    }

    @PROTOTYPE_ONLY
    public void compileSnippets() {
        areSnippetsCompiled = true;
        ClassActor.DEFERRABLE_QUEUE_2.runAll();
    }

    public Word createInitialVTableEntry(int index, VirtualMethodActor dynamicMethodActor) {
        // This default implementation is for testing purposes only:
        assert MaxineVM.isPrototyping();
        // During IR interpretation we can map this token back to a specific IR method representation:
        return MethodID.fromMethodActor(dynamicMethodActor);
    }

    public Word createInitialITableEntry(int index, VirtualMethodActor dynamicMethodActor) {
        // This default implementation is for testing purposes only:
        assert MaxineVM.isPrototyping();
        // During IR interpretation we can map this token back to a specific IR method representation:
        return MethodID.fromMethodActor(dynamicMethodActor);
    }

    public void staticTrampoline() {
        throw new UnsupportedOperationException();
    }

    public final IrMethod compile(ClassMethodActor classMethodActor, CompilationDirective compilationDirective) {
        return irGenerator().makeIrMethod(classMethodActor);
    }

    @PROTOTYPE_ONLY
    public void gatherCalls(TargetMethod targetMethod, AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        throw new UnsupportedOperationException();
    }

    @PROTOTYPE_ONLY
    public void initializeForJitCompilations() {
    }

    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, Purpose purpose, Object context) {
        throw new UnsupportedOperationException();
    }

    public Pointer namedVariablesBasePointer(Pointer stackPointer, Pointer framePointer) {
        throw new UnsupportedOperationException();
    }

    public StackUnwindingContext makeStackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable) {
        throw new UnsupportedOperationException();
    }

    public boolean isBuiltinImplemented(Builtin builtin) {
        return true;
    }
}
