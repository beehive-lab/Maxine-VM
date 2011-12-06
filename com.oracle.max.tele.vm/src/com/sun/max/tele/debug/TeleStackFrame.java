/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.TeleStackFrameWalker.TruncatedStackFrame;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Hierarchy of classes that act as wrappers for VM {@linkplain StackFrame stack frames}, with additional
 * contextual information added for the benefits of clients.  The hierarchy also includes the subclasses
 * {@link TruncatedFrame}, which is <em>synthetic</em>: it
 * corresponds to no VM frame type, but is rather used to as a marker by the stack walker for
 * communicating truncated stack walks.
 */
public abstract class TeleStackFrame<StackFrame_Type extends StackFrame> extends AbstractVmHolder implements MaxStackFrame {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of the memory region occupied by a {@linkplain MaxStackFrame stack frame} in the VM.
     * <br>
     * The parent of this region is the {@linkplain MaxStack stack} in which it is contained.
     * <br>
     * This region has no children (although it could if we chose to decompose it into slots).
     */
    private static final class StackFrameMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxStackFrame> {

        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();
        private final TeleStackFrame teleStackFrame;

        private StackFrameMemoryRegion(MaxVM vm, TeleStackFrame teleStackFrame, String regionName, Address start, long nBytes) {
            super(vm, regionName, start, nBytes);
            this.teleStackFrame = teleStackFrame;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            return teleStackFrame.stack().memoryRegion();
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxStackFrame owner() {
            return teleStackFrame;
        }

        public boolean isBootRegion() {
            return false;
        }
    }

    /**
     * Factory method for wrapping VM (and synthetic) stack frames with additional information, in a type
     * hierarchy that partially mirrors the types of frames.
     *
     * @param vm the VM
     * @param teleStack the stack containing the frame
     * @param position the position in the stack of the frame; position 0 is the top
     * @param stackFrame the frame to be wrapped
     * @return a newly created instance of {@link TeleStackFrame}
     */
    public static TeleStackFrame createFrame(TeleVM vm, TeleStack teleStack, int position, StackFrame stackFrame) {
        if (stackFrame instanceof VMStackFrame) {
            final VMStackFrame compiledStackFrame = (VMStackFrame) stackFrame;
            return new TeleVMFrame(vm, teleStack, position, compiledStackFrame);
        }
        if (stackFrame instanceof NativeStackFrame) {
            final NativeStackFrame nativeStackFrame = (NativeStackFrame) stackFrame;
            return new NativeFrame(vm, teleStack, position, nativeStackFrame);
        }
        if (stackFrame instanceof TruncatedStackFrame) {
            final TruncatedStackFrame errorStackFrame = (TruncatedStackFrame) stackFrame;
            return new TruncatedFrame(vm, teleStack, position, errorStackFrame);
        }
        TeleError.unexpected("Unknown stack frame kind");
        return null;
    }

    protected final StackFrame_Type stackFrame;
    private final TeleStack teleStack;
    private final int position;
    private final CodeLocation codeLocation;

    protected TeleStackFrame(TeleVM vm, TeleStack teleStack, int position, StackFrame_Type stackFrame) {
        super(vm);
        this.stackFrame = stackFrame;
        this.teleStack = teleStack;
        this.position = position;

        CodeLocation location = null;
        Pointer instructionPointer = stackFrame.ip;
        if (instructionPointer.isNotZero()) {
            final StackFrame callee = stackFrame.calleeFrame();
            if (callee == null) {
                // Top frame, not a call return so no adjustment.
                location = vm.codeLocationFactory().createMachineCodeLocation(instructionPointer, "top stack frame IP");
            } else {
                // A call frame; record the return location, the next to be executed upon return.
                // Add a platform-specific offset from the stored code address to the actual call return site.
                final TargetMethod calleeTargetMethod = callee.targetMethod();
                if (calleeTargetMethod != null) {
                    if (calleeTargetMethod.is(TrapStub)) {
                        // Special case, where the IP caused a trap; no adjustment.
                        location = vm.codeLocationFactory().createMachineCodeLocation(instructionPointer, "stack frame return");
                    }
                }
                if (location == null) {
                    // An ordinary call; apply a platform-specific adjustment to get the real return address.
                    final int offsetToReturnPC = platform().isa.offsetToReturnPC;
                    location = vm.codeLocationFactory().createMachineCodeLocation(instructionPointer.plus(offsetToReturnPC), "stack frame return");
                }
            }
        }
        this.codeLocation = location;
    }

    public final TeleObject representation() {
        // No distinguished object in VM runtime represents a stack frame.
        return null;
    }

    public final TeleStack stack() {
        return teleStack;
    }

    public final int position() {
        return position;
    }

    public final boolean isTop() {
        return position == 0;
    }

    public Pointer ip() {
        return stackFrame.ip;
    }

    public Pointer sp() {
        return stackFrame.sp;
    }

    public Pointer fp() {
        return stackFrame.fp;
    }

    public final MaxCodeLocation codeLocation() {
        return codeLocation;
    }

    public boolean isSameFrame(MaxStackFrame maxStackFrame) {
        if (maxStackFrame instanceof TeleStackFrame) {
            // By default, delegate definition of "same" to the wrapped frames.
            final TeleStackFrame otherStackFrame = (TeleStackFrame) maxStackFrame;
            return this.stackFrame.isSameFrame(otherStackFrame.stackFrame);
        }
        return false;
    }

    @Override
    public String toString() {
        return Integer.toString(position) + ":  " + entityName();
    }

    static final class TeleVMFrame extends TeleStackFrame<VMStackFrame> implements MaxStackFrame.Compiled {

        private final StackFrameMemoryRegion stackFrameMemoryRegion;
        private final String entityDescription;
        private TeleCompilation teleCompiledCode;


        protected TeleVMFrame(TeleVM vm, TeleStack teleStack, int position, VMStackFrame compiledStackFrame) {
            super(vm, teleStack, position, compiledStackFrame);
            final String description = teleStack.thread().entityName() + " frame(" + position() + ")";
            this.stackFrameMemoryRegion = new StackFrameMemoryRegion(vm, this, description, stackFrame.slotBase(), layout().maximumSlotOffset());
            this.entityDescription = "Stack frame " + position() + " in the " + vm().entityName() + " for " + teleStack.thread().entityName();
            this.teleCompiledCode = vm.machineCode().findCompilation(stackFrame.ip);
        }

        public String entityName() {
            return "<" + stackFrame.getClass().getSimpleName() + "> " + stackFrame.toString();
        }

        public String entityDescription() {
            return entityDescription;
        }

        public MaxEntityMemoryRegion<MaxStackFrame> memoryRegion() {
            return stackFrameMemoryRegion;
        }

        public boolean contains(Address address) {
            return stackFrameMemoryRegion.contains(address);
        }

        public MaxMachineCodeRoutine machineCode() {
            return compilation();
        }

        public TeleCompilation compilation() {
            if (teleCompiledCode == null) {
                teleCompiledCode = vm().machineCode().findCompilation(stackFrame.ip);
            }
            return teleCompiledCode;
        }

        public VMFrameLayout layout() {
            return stackFrame.layout;
        }

        public Pointer slotBase() {
            return stackFrame.slotBase();
        }

        public int biasedFPOffset(int offset) {
            return stackFrame.biasedFPOffset(Offset.fromInt(offset)).toInt();
        }

        public StackBias bias() {
            return stackFrame.bias();
        }

        public String sourceVariableName(int slot) {
            final TeleCompilation compilation = compilation();
            return compilation == null ? null : compilation.sourceVariableName(this, slot);
        }

    }

    static final class NativeFrame extends TeleStackFrame<NativeStackFrame> implements MaxStackFrame.Native {

        private MaxNativeFunction nativeFunction = null;

        private NativeFrame(TeleVM vm, TeleStack teleStack, int position, NativeStackFrame nativeStackFrame) {
            super(vm, teleStack, position, nativeStackFrame);
        }

        public String entityName() {
            return "Frame: " + stackFrame.toString();
        }

        public String entityDescription() {
            return "A stack frame discovered running unknown native code together with the " + vm().entityName();
        }

        public MaxEntityMemoryRegion<MaxStackFrame> memoryRegion() {
            // Don't know enough
            return null;
        }

        public boolean contains(Address address) {
            return false;
        }

        public TeleCompilation compilation() {
            return null;
        }

        @Override
        public MaxMachineCodeRoutine machineCode() {
            if (nativeFunction == null) {
                nativeFunction = vm().machineCode().findNativeFunction(stackFrame.ip);
            }
            return nativeFunction;
        }

    }

    static final class TruncatedFrame extends TeleStackFrame<TruncatedStackFrame> implements MaxStackFrame.Truncated {

        private TruncatedFrame(TeleVM vm, TeleStack teleStack, int position, TruncatedStackFrame pseudoStackFrame) {
            super(vm, teleStack, position, pseudoStackFrame);
        }

        public String entityName() {
            return stackFrame.toString();
        }

        public String entityDescription() {
            return "A frame denoting a truncated stack walk the " + vm().entityName();
        }

        public MaxEntityMemoryRegion<MaxStackFrame> memoryRegion() {
            return null;
        }

        public boolean contains(Address address) {
            return false;
        }

        @Override
        public Pointer ip() {
            return Pointer.zero();
        }

        @Override
        public Pointer sp() {
            return Pointer.zero();
        }

        @Override
        public Pointer fp() {
            return Pointer.zero();
        }

        public MaxMachineCodeRoutine machineCode() {
            return null;
        }

        public TeleCompilation compilation() {
            return null;
        }

        public Throwable error() {
            return stackFrame.error;
        }

        public int omitted() {
            return stackFrame.omitted;
        }

    }

}
