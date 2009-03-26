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
package com.sun.max.vm.stack.sparc;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.sparc.*;
import com.sun.max.vm.stack.*;

/**
 * Describes a stack frame for a method produced by the {@linkplain SPARCJitCompiler SPARC JIT compiler}.
 * The JIT compiler doesn't use register windows for JIT-compiled code. Instead, it re-uses the same register window
 * across successive jit-compiled code to jit-compiled code calls.
 * As for the AMD64 JIT compiler, it uses a frame and a stack pointer. The stack pointer is the native stack pointer defined by
 * the SPARC platform (i.e., %06). It is used as an operand stack (as defined in the JVM specification).
 * The frame pointer is a synthetic pointer (%l0) explicitly maintained by the SPARC JIT runtime.
 * The native frame pointer (%i6), the one defined by the ABI and know by the OS, points to the base of the register window,
 * and is not manipulated at all by JITed code.
 *
 * Frames of JITed code are synthetic in that the operating system only sees a single native stack frame, delimited by %i6 and %o6.
 * That native stack frame must follows the requirement of the SPARC ABI, namely, it must be at all times 16-byte aligned,
 * must have a area for saving the current register window at its base, and both the stack and frame pointer registers (%i6 and %o6) hold
 * a biased address. The consequence for the JIT frame is that the top frame always end with an
 *
 *
 * Because register windows aren't used, the JIT runtime explicitly saves the caller's context on the stack (i.e., its code pointer
 * and frame pointer).
 *
 * The layout of the stack is as follows:
 * <p>
 * <pre>
 *   Base  Index       Contents
 *   ----------------+--------------------------------+----------------              maximumSlotOffset() if P > 0
 *      [+R+(P*J)+1] | Java parameter 0               | Incoming
 *                   |     ...                        | Java
 *            [+R+1] | Java parameter (P-1)           | parameters
 *   ----------------+--------------------------------+----------------    ___
 *              [+R] | return address                 | Call                ^
 *            [+R-1] | caller's FP value              | save                |
 *            [+R-2] | caller's Literal Base          | area                |
 *                   +--------------------------------+----------------     |
 *                   |     ...                        | alignment           |
 *                   +--------------------------------+----------------     |        maximumSlotOffset() if P == 0
 *          [+(T-1)] | template spill slot (T-1)      | Template            |
 *                   |     ...                        | spill               |
 *              [+0] | template spill slot 0          | area            frameSize()
 *  FP (%l0)  ==>    +--------------------------------+----------------     |
 *              [-J] | Java non-parameter local 0     | Java                |
 *                   |     ...                        | non-parameters      |
 *          [-(L*J)] | Java non-parameter local (L-1) | locals              v
 *                   +--------------------------------+----------------    ---
 *      [-((L+1)*J)] | Java stack slot 0              | Java
 *                   |     ...                        | operand
 *      [-((L+S)*J)] | Java stack slot (S-1)          | stack
 *  TOS       ==>    +--------------------------------+----------------  lowestSlotOffset()
 *                   | outgoing register area         |
 *                   | (6 x 8 bytes = 96 bytes)       | Only for the
 *                   +--------------------------------+ top frame
 *                   | register window save area      |
 *                   | (16 x 8 bytes = 128 bytes)     |
 *  SP (%o6) + BIAS  +--------------------------------+----------------
 *
 *
 * where:
 *      P == Number of Java parameter slots
 *      L == Number of Java non-parameter local slots
 *      S == Number of Java operand stack slots  # (i.e. maxStack)
 *      T == Number of template spill slots
 *      R == Return address offset [ frameSize - sizeOfNonParameterLocals() ]
 *      J == Stack slots per JIT slot [ JIT_SLOT_SIZE / Word.size() ]
 *
 * </pre>
 * The parameters portion of the stack frame is set up by the caller.
 * The frame size counts only those slots that are allocated on the stack by the callee, upon method entry, namely,
 * the size for the call save area, the locals that aren't argument, the Java stack, and the template spill slots.
 *
 *
 * @author Doug Simon
 * @author Laurent Daynes
 */
public class SPARCJitStackFrameLayout extends JitStackFrameLayout {
    /**
     * Size of the area in the Optimized to Jited code frame adapter used to set floating point register to immediate floating point value.
     * We make it 16 bytes area to make adapter frame 16-byte aligned.
     * @see {@link #OFFSET_TO_FLOATING_POINT_TEMP_AREA}
     */
    public static final int FLOATING_POINT_TEMP_AREA_SIZE = 2 * Word.size();

    /**
     * SPARC instruction set doesn't include instruction for setting a floating-point register to a literal value.
     * Instead, one must set a memory location and load it using a ld or ldd instruction. Alternatively, one may
     * set an immediate word in a integer register, store it in memory, and load it in a floating point register.
     * The JIT use the latter approach for setting floating point (single or double precision) "constant".
     * Each opt->jit adapter frame has an 8 bytes slot reserved in the adapter frame for this purpose.
     * This slot can be accessed by every JITed method using the native frame pointer %i6, which is otherwise unused
     * by JITed code.
     */
    public static final int OFFSET_TO_FLOATING_POINT_TEMP_AREA = STACK_BIAS.SPARC_V9.stackBias() - FLOATING_POINT_TEMP_AREA_SIZE;

    /**
     * Size of the call save area, in number of stack slots. All method invocations to JIT'ed code push a return address
     * (1 stack slot) and the JIT prologue saves the caller's frame pointer (1 stack slot).
     * FIXME: and the literal base of the caller (1 stack slot) -- NOT TRUE ANYMORE.
     */
    public static final int CALL_SAVE_AREA_SLOTS = 2;
    public static final int CALL_SAVE_AREA_SIZE = CALL_SAVE_AREA_SLOTS * STACK_SLOT_SIZE;

    private final int _numberOfTemplateSlots;

    public SPARCJitStackFrameLayout(TargetMethod targetMethod) {
        super(targetMethod.classMethodActor());
        final int frameSlots = targetMethod.frameSize() / STACK_SLOT_SIZE;
        // The extra JIT_SLOT_SIZE is the saving area for the literal base.
        final int nonTemplateSlots = CALL_SAVE_AREA_SLOTS + sizeOfLocalArea() / STACK_SLOT_SIZE;
        _numberOfTemplateSlots = frameSlots - nonTemplateSlots;
        assert targetMethod.frameSize() == frameSize();
    }
    public SPARCJitStackFrameLayout(ClassMethodActor classMethodActor, int numberOfTemplateSlots) {
        super(classMethodActor);
        _numberOfTemplateSlots = numberOfTemplateSlots;
    }

    @Override
    public int frameSize() {
        return offsetToTopOfFrameFromFramePointer() + sizeOfLocalArea();
    }

    public int offsetToTopOfFrameFromFramePointer() {
        final int unalignedSize = sizeOfTemplateSlots() + CALL_SAVE_AREA_SIZE;
        return JIT_ABI.alignFrameSize(unalignedSize);
    }

    @Override
    public int frameReferenceMapOffset() {
        return lowestSlotOffset();
    }

    @Override
    public int frameReferenceMapSize() {
        return ByteArrayBitMap.computeBitMapSize((maximumSlotOffset() - lowestSlotOffset()) / STACK_SLOT_SIZE);
    }


    /**
     * Size of the local area below the frame pointer. It includes the non parameter locals + the saving area
     * for the literal base pointer (a single JIT_SLOT_SIZE).
     * @return
     */
    private int sizeOfLocalArea() {
        return sizeOfNonParameterLocals() + JIT_SLOT_SIZE;
    }

    private int numberOfLocalAreaSlot() {
        return numberOfNonParameterSlots() + 1;
    }

    @Override
    public int localVariableOffset(int localVariableIndex) {
        if (isParameter(localVariableIndex)) {
            // The slot index is at a positive offset from FP.
            //
            // | <-------local area -----> |
            // | non-parameter locals | LB | template slots | caller FP | return address | parameters |
            // | <------------------------------ frameSize() --------------------------> |
            //                             ^ FP                                          ^ parameterStart
            // LB: literal base saving area.

            // Local variables are accessed via FP, not SP.
            final int parameterStart = frameSize() - sizeOfLocalArea();
            return parameterStart + JIT_SLOT_SIZE * (_numberOfParameterSlots - 1 - localVariableIndex);
        }
        // The slot index is at a negative offset from FP.
        // We need to count one extra-slot for LB
        //
        // | non-parameter locals | LB | template slots | caller FP | return address | parameters |
        //       ^ slot index          ^ FP
        final int slotIndex = _numberOfParameterSlots - 2 - localVariableIndex;
        return slotIndex * JIT_SLOT_SIZE;
    }

    private int framePointerOffsetToRefMapIndex(int offset) {
        // | operand slots | non-parameter locals | LB | template slots | call save area | return address | parameters |
        //       ^ operand offset (wrt. FP)            ^ FP                                               ^ local offset (wrt. FP)
        // <-------- frame pointer bias --------------->
        final int framePointerBias = sizeOfOperandStack() + sizeOfLocalArea();
        return (offset + framePointerBias) / STACK_SLOT_SIZE;
    }

    @Override
    public int localVariableReferenceMapIndex(int localVariableIndex) {
        return framePointerOffsetToRefMapIndex(localVariableOffset(localVariableIndex));
    }

    @Override
    public int numberOfTemplateSlots() {
        return _numberOfTemplateSlots;
    }

    @Override
    public int maximumSlotOffset() {
        if (_numberOfParameterSlots == 0) {
            // if there are no parameters, return the offset to the end of the last template slot
            return sizeOfLocalArea() + _numberOfTemplateSlots * STACK_SLOT_SIZE;
        }
        // return the end of the first parameter
        return localVariableOffset(0) + JIT_SLOT_SIZE;
    }

    @Override
    public int lowestSlotOffset() {
        return operandStackOffset(_numberOfOperandStackSlots - 1);
    }

    @Override
    public int operandStackOffset(int operandStackIndex) {
        return 0 - ((numberOfLocalAreaSlot() + operandStackIndex + 1) * JIT_SLOT_SIZE);
    }

    @Override
    public int operandStackReferenceMapIndex(int operandStackIndex) {
        return framePointerOffsetToRefMapIndex(operandStackOffset(operandStackIndex));
    }


    @Override
    public boolean isReturnAddressPushedByCall() {
        return false;
    }

    /**
     * Offset to the slot where the return address is stored, relative to the stack pointer.
     * @return
     */
    public int returnAddressOffset() {
        // return frameSize() - sizeOfNonParameterLocals();
        return frameSize() - STACK_SLOT_SIZE;
    }

    /**
     * Offset to the slot where the caller's FP is stored, relative to the stack pointer.
     * @return
     */
    public int callersFPOffset() {
        return returnAddressOffset() - STACK_SLOT_SIZE;
    }

    @Override
    public JitSlots slots() {
        return new JitSlots() {
            @Override
            protected String nameOfSlot(int offset) {
                final int templateSlotIndex = offset / STACK_SLOT_SIZE;
                if (templateSlotIndex >= 0 && templateSlotIndex < _numberOfTemplateSlots) {
                    return "template slot " + templateSlotIndex;

                }
                if (offset == returnAddressOffset()) {
                    return "return address";
                }
                if (offset == callersFPOffset()) {
                    return "caller's FP";
                }
                return super.nameOfSlot(offset);
            }
        };
    }
}
