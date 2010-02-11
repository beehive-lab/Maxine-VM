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
package com.sun.max.vm.cps.jit.amd64;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.thread.*;

/**
 * Template-based implementation of JIT compiler for AMD64.
 *
 * @author Laurent Daynes
 * @author Ben L. Titzer
 * @see AMD64JitStackFrameLayout
 */
public class AMD64JitCompiler extends JitCompiler {

    private final AMD64TemplateBasedTargetGenerator targetGenerator;

    @HOSTED_ONLY
    public AMD64JitCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        targetGenerator = new AMD64TemplateBasedTargetGenerator(this);
    }

    @HOSTED_ONLY
    public AMD64JitCompiler(VMConfiguration vmConfiguration, TemplateTable templateTable) {
        this(vmConfiguration);
        targetGenerator.initializeTemplateTable(templateTable);
    }

    @Override
    public <Type extends TargetMethod> Class<Type> compiledType() {
        Class<Class<Type>> type = null;
        return StaticLoophole.cast(type, AMD64JitTargetMethod.class);
    }

    @Override
    protected TemplateBasedTargetGenerator targetGenerator() {
        return targetGenerator;
    }

    public TemplateTable peekTemplateTable() {
        return targetGenerator.templateTable();
    }

    private static final byte ENTER = (byte) 0xC8;
    private static final byte LEAVE = (byte) 0xC9;
    private static final byte POP_RBP = (byte) 0x5D;

    private static final byte RET = (byte) 0xC3;
    private static final byte RET2 = (byte) 0xC2;

    /**
     * Offset to the last instruction of the prologue from the JIT entry point. The prologue comprises two instructions,
     * the first one of which is enter (fixed size, 4 bytes long).
     */
    public static final int OFFSET_TO_LAST_PROLOGUE_INSTRUCTION = 4;

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted()) {
            unwindMethod = ClassActor.fromJava(AMD64JitCompiler.class).findLocalClassMethodActor(SymbolTable.makeSymbol("unwind"), null);
        }
    }

    private static ClassMethodActor unwindMethod;

    private static int unwindFrameSize = -1;

    @NEVER_INLINE
    private static int getUnwindFrameSize() {
        if (unwindFrameSize == -1) {
            unwindFrameSize = CompilationScheme.Static.getCurrentTargetMethod(unwindMethod).frameSize();
        }
        return unwindFrameSize;
    }

    /**
     * This method must be compiled such that it uses neither the stack or frame pointer implicitly. Doing so might
     * conflict with any code restoring these registers before returning to the dispatcher of the exception. The
     * critical state of the registers before the RET instruction is:
     * <ul>
     * <li>RSP must be one word less than the stack pointer of the handler frame that is the target of the unwinding</li>
     * <li>The value at [RSP] must be address of the handler code</li>
     * </ul>
     * <p>
     *
     * @param catchAddress the address of the handler code (actually the dispatcher code)
     * @param stackPointer the stack pointer denoting the frame of the handler to which the stack is unwound upon
     *            returning from this method
     * @param framePointer
     */
    @NEVER_INLINE
    public static void unwind(Throwable throwable, Address catchAddress, Pointer stackPointer, Pointer framePointer) {
        int unwindFrameSize = getUnwindFrameSize();

        // Put the exception where the exception handler expects to find it
        VmThreadLocal.EXCEPTION_OBJECT.setVariableReference(Reference.fromJava(throwable));

        if (throwable instanceof StackOverflowError) {
            // This complete call-chain must be inlined down to the native call
            // so that no further stack banging instructions
            // are executed before execution jumps to the catch handler.
            VirtualMemory.protectPages(VmThread.current().stackYellowZone(), VmThread.STACK_YELLOW_ZONE_PAGES);
        }
        // Push 'catchAddress' to the handler's stack frame and update RSP to point to the pushed value.
        // When the RET instruction is executed, the pushed 'catchAddress' will be popped from the stack
        // and the stack will be in the correct state for the handler.
        Pointer returnAddressPointer = stackPointer.minus(Word.size());
        returnAddressPointer.setWord(catchAddress);

        VMRegister.setCpuStackPointer(returnAddressPointer.minus(unwindFrameSize));
        VMRegister.setCpuFramePointer(framePointer);
    }
}
