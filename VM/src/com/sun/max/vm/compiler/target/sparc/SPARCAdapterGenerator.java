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
package com.sun.max.vm.compiler.target.sparc;

import com.sun.max.annotate.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.Adapter.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.type.*;

/**
 * Adapter generators for SPARC.
 *
 * @author Doug Simon
 */
public abstract class SPARCAdapterGenerator extends AdapterGenerator {

    static {
        if (VMConfiguration.target().needsAdapters()) {
            // Create and register the adapter generators
            new Jit2Opt();
            new Opt2Jit();
        }
    }

    public SPARCAdapterGenerator(Adapter.Type adapterType) {
        super(adapterType);
    }

    @Override
    public boolean advanceIfInPrologue(Cursor current) {
        if (inPrologue(current.ip(), current.targetMethod())) {
            FatalError.unimplemented();
        }
        return false;
    }

    /**
     * SPARC specific generator for {@link Type#JIT2OPT} adapters.
     */
    public static class Jit2Opt extends SPARCAdapterGenerator {

        /**
         * SPARC specific {@link Type#JIT2OPT} adapters.
         */
        public static class Jit2OptAdapter extends Adapter {

            Jit2OptAdapter(AdapterGenerator generator, String description, int frameSize, byte[] code, int callPosition) {
                super(generator, description, frameSize, code, callPosition);
            }

            /**
             * Computes the state of an adapter frame based on an execution point in this adapter.
             *
             * This complex computation is only necessary for the Inspector as that is the only context
             * in which the current execution point in an adapter can be anywhere except for in
             * the call back the callee being adapted. Hence the {@link HOSTED_ONLY} annotation.
             *
             * @param cursor a stack frame walker cursor denoting an execution point in this adapter
             * @return the amount by which {@code current.sp()} should be adjusted to obtain the slot holding the
             *         address to which the adapter will return. If the low bit of this value is set, then it means that
             *         the caller's RBP is obtained by reading the word at {@code current.fp()} otherwise it is equal to
             *         {@code current.fp()}.
             */
            @HOSTED_ONLY
            private int computeFrameState(Cursor cursor) {
                throw FatalError.unimplemented();
            }

            @Override
            public void advance(Cursor current) {
                throw FatalError.unimplemented();
            }

            @Override
            public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
                throw FatalError.unimplemented();
            }
        }

        /**
         * A specialization of an SPARC specific {@link Type#JIT2OPT} adapter that contains a reference map occupying 32 or less bits.
         */
        public static class Jit2OptAdapterWithRefMap extends Jit2OptAdapter {

            final int refMap;

            public Jit2OptAdapterWithRefMap(AdapterGenerator generator, String description, int refMap, int frameSize, byte[] code, int callPosition) {
                super(generator, description, frameSize, code, callPosition);
                this.refMap = refMap;
            }

            @Override
            public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
                preparer.tracePrepareReferenceMap(this, 0, current.sp(), "frame");
                int frameSlotIndex = preparer.referenceMapBitIndex(current.sp());
                int byteIndex = 0;
                int refMap = this.refMap;
                for (int i = 0; i < 4; i++) {
                    final byte refMapByte = (byte) (refMap & 0xff);
                    preparer.traceReferenceMapByteBefore(byteIndex, refMapByte, "Frame");
                    preparer.setBits(frameSlotIndex, refMapByte);
                    preparer.traceReferenceMapByteAfter(current.sp(), frameSlotIndex, refMapByte);
                    refMap >>>= Bytes.WIDTH;
                    frameSlotIndex += Bytes.WIDTH;
                    byteIndex++;
                }
            }
        }

        /**
         * A specialization of an SPARC specific {@link Type#JIT2OPT} adapter that contains a reference map occupying more than 32 bits.
         */
        public static class Jit2OptAdapterWithBigRefMap extends Jit2OptAdapter {

            final byte[] refMap;

            public Jit2OptAdapterWithBigRefMap(AdapterGenerator generator, String description, byte[] refMap, int frameSize, byte[] code, int callPosition) {
                super(generator, description, frameSize, code, callPosition);
                this.refMap = refMap;
            }

            @Override
            public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
                preparer.tracePrepareReferenceMap(this, 0, current.sp(), "frame");
                int frameSlotIndex = preparer.referenceMapBitIndex(current.sp());
                int byteIndex = 0;
                for (int i = 0; i < refMap.length; i++) {
                    final byte frameReferenceMapByte = refMap[byteIndex];
                    preparer.traceReferenceMapByteBefore(byteIndex, frameReferenceMapByte, "Frame");
                    preparer.setBits(frameSlotIndex, frameReferenceMapByte);
                    preparer.traceReferenceMapByteAfter(current.sp(), frameSlotIndex, frameReferenceMapByte);
                    frameSlotIndex += Bytes.WIDTH;
                    byteIndex++;
                }
            }
        }

        /**
         * Frame layout for an SPARC specific {@link Type#JIT2OPT} adapter frame.
         * <pre>
         *
         *          +------------------------+     ---
         *          |        OPT arg N       |      ^
         *          +------------------------+      |
         *          :                        :  frame size
         *          +------------------------+      |
         *   %sp--> |        OPT arg R       |      v
         *          +------------------------+     ---
         *
         *   N == number of args - 1
         *   R == number of register args
         * </pre>
         */
        public static class Jit2OptAdapterFrameLayout extends AdapterStackFrameLayout {

            public Jit2OptAdapterFrameLayout(int frameSize) {
                super(frameSize, true);
            }

            @Override
            public int frameReferenceMapOffset() {
                return 0;
            }

            @Override
            public int frameReferenceMapSize() {
                return ByteArrayBitMap.computeBitMapSize(Unsigned.idiv(frameSize(), STACK_SLOT_SIZE));
            }

            @Override
            public Slots slots() {
                return new Slots() {
                    @Override
                    protected String nameOfSlot(int offset) {
                        throw FatalError.unimplemented();
                    }
                };
            }
        }

        private static final int PROLOGUE_SIZE = 8;

        public Jit2Opt() {
            super(Adapter.Type.JIT2OPT);
        }

        @Override
        public int prologueSizeForCallee(ClassMethodActor callee) {
            return PROLOGUE_SIZE;
        }

        /**
         * The prologue for a method with a JIT2OPT adapter has a call to the adapter
         * at the {@link CallEntryPoint#JIT_ENTRY_POINT}. The body of the method starts at the
         * {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}. The assembler code is as follows:
         * <pre>
         *     // TODO
         * </pre>
         */
        @Override
        protected int emitPrologue(Object out, Adapter adapter) {
            throw FatalError.unimplemented();
        }

        @Override
        public void linkAdapterCallInPrologue(TargetMethod targetMethod, Adapter adapter) {
            targetMethod.patchCallSite(0, adapter.codeStart());
        }

        /**
         * Creates a JIT2OPT adapter.
         *
         * @see Jit2OptAdapterFrameLayout
         */
        @Override
        protected Adapter create(Sig sig) {
            throw FatalError.unimplemented();
        }

        // Checkstyle: stop
        @Override
        protected void adapt(SPARCAssembler asm, Kind kind, GPR reg, int offset32) {
            throw FatalError.unimplemented();
        }

        @Override
        protected void adapt(SPARCAssembler asm, Kind kind, FPR reg, int offset32) {
            throw FatalError.unimplemented();
        }
        // Checkstyle: resume

        @Override
        public void adapt(SPARCAssembler asm, Kind kind, int optStackOffset32, int jitStackOffset32, int adapterFrameSize) {
            throw FatalError.unimplemented();
        }
    }

    /**
     * SPARC specific generator for {@link Type#OPT2JIT} adapters.
     */
    static class Opt2Jit extends SPARCAdapterGenerator {

        /**
         * SPARC specific {@link Type#OPT2JIT} adapter.
         */
        static final class Opt2JitAdapter extends Adapter {

            Opt2JitAdapter(AdapterGenerator generator, String description, int frameSize, byte[] code, int callPosition) {
                super(generator, description, frameSize, code, callPosition);
            }

            /**
             * Computes the amount by which the stack pointer in a given {@linkplain Cursor cursor}
             * should be adjusted to obtain the location on the stack holding the RIP to which
             * the adapter will return.
             *
             * This complex computation is only necessary for the Inspector as that is the only context
             * in which the current execution point in an adapter can be anywhere except for in
             * the call to the body of the callee being adapted. Hence the {@link HOSTED_ONLY} annotation.
             *
             * @param cursor a stack frame walker cursor denoting an execution point in this adapter
             */
            @HOSTED_ONLY
            private int computeRipAdjustment(Cursor cursor) {
                throw FatalError.unimplemented();
            }

            @Override
            public void advance(Cursor cursor) {
                throw FatalError.unimplemented();
            }

            @Override
            public boolean acceptStackFrameVisitor(Cursor cursor, StackFrameVisitor visitor) {
                throw FatalError.unimplemented();
            }
        }

        /**
         * Frame layout for an SPARC specific {@link Type#OPT2JIT} adapter frame.
         * <pre>
         *
         *          +------------------------+     ---
         *          |       JIT arg 0        |      ^
         *          +------------------------+      |
         *          :                        :  frame size
         *          +------------------------+      |
         *   RSP--> |       JIT arg N        |      v
         *          +------------------------+     ---
         *
         *   N == number of args - 1
         * </pre>
         */
        public static class Opt2JitAdapterFrameLayout extends AdapterStackFrameLayout {

            public Opt2JitAdapterFrameLayout(int frameSize) {
                super(frameSize, true);
            }

            @Override
            public Slots slots() {
                return new Slots() {
                    @Override
                    protected String nameOfSlot(int offset) {
                        throw FatalError.unimplemented();
                    }
                };
            }
        }

        Opt2Jit() {
            super(Adapter.Type.OPT2JIT);
        }

        @Override
        public int prologueSizeForCallee(ClassMethodActor callee) {
            throw FatalError.unimplemented();
        }

        /**
         * The prologue for a method with an OPT2JIT adapter has a call to the adapter
         * at the {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}. The code at the
         * {@link CallEntryPoint#JIT_ENTRY_POINT} has a jump over this call to the
         * body of the method. The assembler code is as follows:
         * <pre>
         *     // TODO
         * </pre>
         * In the case where there is no adaptation required (i.e. a parameterless call to a static method),
         * the assembler code in the prologue is a series of {@code nop}s up to the {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}
         * which is where the method body starts. This means that a JIT caller will fall through the {@code nop}s
         * to the method body.
         */
        @Override
        protected int emitPrologue(Object out, Adapter adapter) {
            throw FatalError.unimplemented();
        }

        @Override
        public void linkAdapterCallInPrologue(TargetMethod targetMethod, Adapter adapter) {
            targetMethod.patchCallSite(8, adapter.codeStart());
        }

        /**
         * Creates an OPT2JIT adapter.
         *
         * @see Opt2JitAdapterFrameLayout
         */
        @Override
        protected Adapter create(Sig sig) {
            throw FatalError.unimplemented();
        }

        @Override
        protected void adapt(SPARCAssembler asm, Kind kind, GPR reg, int offset32) {
            throw FatalError.unimplemented();
        }

        @Override
        protected void adapt(SPARCAssembler asm, Kind kind, FPR reg, int offset32) {
            throw FatalError.unimplemented();
        }

        @Override
        public void adapt(SPARCAssembler asm, Kind kind, int optStackOffset32, int jitStackOffset32, int adapterFrameSize) {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Emits code to copy an incoming argument from ABI-specified location of the caller
     * to the ABI-specified location of the callee.
     *
     * @param asm assembler used to emit code
     * @param kind the kind of the argument
     * @param optArg the location of the argument according to the OPT convention
     * @param jitStackOffset32 the 32-bit offset of the argument on the stack according to the JIT convention
     * @param adapterFrameSize the size of the adapter frame
     * @return the reference map index of the reference slot on the adapter frame corresponding to the argument or -1
     */
    protected int adaptArgument(SPARCAssembler asm, Kind kind, TargetLocation optArg, int jitStackOffset32, int adapterFrameSize) {
        if (optArg instanceof TargetLocation.IntegerRegister) {
            adapt(asm, kind, GPR.GLOBAL_SYMBOLIZER.fromValue(((TargetLocation.IntegerRegister) optArg).index()), jitStackOffset32);
        } else if (optArg instanceof TargetLocation.FloatingPointRegister) {
            adapt(asm, kind, FPR.fromValue(((TargetLocation.FloatingPointRegister) optArg).index()), jitStackOffset32);
        } else if (optArg instanceof TargetLocation.ParameterStackSlot) {
            int optStackOffset32 = ((TargetLocation.ParameterStackSlot) optArg).index() * OPT_SLOT_SIZE;
            adapt(asm, kind, optStackOffset32, jitStackOffset32, adapterFrameSize);
            if (kind == Kind.REFERENCE) {
                return optStackOffset32 / Word.size();
            }
        } else {
            throw FatalError.unexpected("Unadaptable parameter location type: " + optArg.getClass().getSimpleName());
        }
        return -1;
    }

    protected abstract void adapt(SPARCAssembler asm, Kind kind, int optStackOffset32, int jitStackOffset32, int adapterFrameSize);
    protected abstract void adapt(SPARCAssembler asm, Kind kind, FPR reg, int offset32);
    protected abstract void adapt(SPARCAssembler asm, Kind kind, GPR reg, int offset32);
}
