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

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
/**
 * This interface specifies the interface between a dynamic compiler
 * and the rest of the virtual machine, including methods to create
 * entries for vtables, compile methods, and stack walking.
 *
 * @author Laurent Daynes
 * @author Ben L. Titzer
 */
public interface DynamicCompilerScheme extends VMScheme {

    /**
     * Gets the initial value for a virtual method table for the specified table index and method actor. This may be a
     * compiled version of the method if it exists, or could be an entrypoint to a trampoline method.
     *
     * @param vTableIndex the virtual method table index for the method
     * @param virtualMethodActor the method actor
     * @return a word representing a valid entrypoint to code for the specified table index and method
     *
     * @see com.sun.max.vm.trampoline.TrampolineGenerator
     */
    Word createInitialVTableEntry(int vTableIndex, VirtualMethodActor virtualMethodActor);

    /**
     * Gets the initial value for an interface method table for the specified table index and method actor. This may be
     * a compiled version of the method if it exists, or could be an entrypoint to a trampoline method.
     *
     * @param iIndex the interface method table index for the method
     * @param virtualMethodActor the method actor
     * @return a word representing a valid entrypoint to code for the specified table index and method
     *
     * @see com.sun.max.vm.trampoline.TrampolineGenerator
     */
    Word createInitialITableEntry(int iIndex, VirtualMethodActor virtualMethodActor);

    /**
     * Compiles a method to an internal representation (typically a {@link TargetMethod}).
     *
     * @param classMethodActor the method to compile
     * @return a reference to the IR method created by this compiler for the specified method
     */
    IrMethod compile(ClassMethodActor classMethodActor, CompilationDirective compilationDirective);

    /**
     * Returns a list of the internal IR generators that this compiler uses, e.g. for instrumentation.
     *
     * @return a sequence of IR generators internal to this compiler
     */
    Sequence<IrGenerator> irGenerators();

    /**
     * Analyzes the target method that this compiler produced to build a call graph. This method appends the direct
     * calls (i.e. static and special calls), the virtual calls, and the interface calls to the appendable sequences
     * supplied.
     *
     * @param targetMethod the target method to analyze
     * @param directCalls a sequence of the direct calls to which this method should append
     * @param virtualCalls a sequence of virtual calls to which this method should append
     * @param interfaceCalls a sequence of interface calls to which this method should append
     */
    @PROTOTYPE_ONLY
    void gatherCalls(TargetMethod targetMethod, AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls);

    /**
     * Returns the number of compilations completed by this compiler.
     * @return the number of compilations completed
     */
    long numberOfCompilations(); // TODO: move uses of this method to CompilationScheme.isCompiling()

    /**
     * Method to initialize the dynamic compiler.
     */
    @PROTOTYPE_ONLY
    void initializeForJitCompilations(); // TODO: move to using the basic initialize(Phase) method?

    /**
     * Walks a frame for a target method that was produced by this compiler.
     *
     * @param stackFrameWalker the stack frame walker object
     * @param isTopFrame {@code true} if this frame is the top frame; {@code false} otherwise
     * @param targetMethod the target method corresponding to this stack frame
     * @param purpose the purpose of this stack walk
     * @param context the context for the stack walk
     * @return whether stack walking may continue after executing this method
     */
    boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, Purpose purpose, Object context); // TODO: why is the compiler involved in stack walking at all?

    StackUnwindingContext makeStackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable);

}
