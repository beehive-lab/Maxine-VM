/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A specialization of the VM's stack walker for remote use.
 *
 * @see StackFrameWalker
 */
public final class TeleStackFrameWalker extends StackFrameWalker {

    /**
     * A specialized implementation of the stack walker's cursor that
     * can access information about the code cache remotely.
     */
    private static final class TeleStackFrameCursor extends StackFrameCursor {

        protected TeleStackFrameCursor(StackFrameWalker walker, TeleVM vm) {
            super(walker, new TeleNativeOrVmIP(vm));
        }
    }

    private final TeleVM vm;
    private final TeleThreadLocalsArea teleEnabledVmThreadLocalValues;
    private final TeleNativeThread teleNativeThread;

    private final Pointer cpuInstructionPointer;
    private final Pointer cpuStackPointer;
    private final Pointer cpuFramePointer;

    public TeleStackFrameWalker(TeleVM vm, TeleNativeThread teleNativeThread) {
        super();
        // Replace the two cursors use by the walker with inspector-specific ones
        this.current = new TeleStackFrameCursor(this, vm);
        this.callee = new TeleStackFrameCursor(this, vm);
        this.vm = vm;
        this.teleNativeThread = teleNativeThread;
        this.teleEnabledVmThreadLocalValues = teleNativeThread.localsBlock().tlaFor(SafepointPoll.State.ENABLED);
        final TeleRegisterSet registers = teleNativeThread.registers();
        this.cpuInstructionPointer = registers.instructionPointer();
        this.cpuStackPointer = registers.stackPointer();
        this.cpuFramePointer = registers.framePointer();
    }

    /**
     * A pseudo frame informing the stack walker client of a condition
     * that prevented a complete stack walk.
     */
    public static class TruncatedStackFrame extends StackFrame {

        /**
         * Denotes the error causing the stack walk to stop. If {@code null}, the stack walk
         * was stopped due to the frame limit specified to {@link TeleStackFrameWalker#frames(int)}
         * being hit.
         */
        public final Throwable error;

        /**
         * The number of frames above the frame limit. This will be -1 if an error stopped the stack walk.
         */
        public final int omitted;

        TruncatedStackFrame(StackFrame callee, Throwable error) {
            super(callee, Pointer.zero(), Pointer.zero(), Pointer.zero());
            this.error = error;
            this.omitted = -1;
        }

        TruncatedStackFrame(StackFrame callee, int omitted) {
            super(callee, Pointer.zero(), Pointer.zero(), Pointer.zero());
            this.error = null;
            this.omitted = omitted;
        }

        @Override
        public boolean isSameFrame(StackFrame stackFrame) {
            if (stackFrame instanceof TruncatedStackFrame) {
                final TruncatedStackFrame other = (TruncatedStackFrame) stackFrame;
                final StackFrame calleeFrame = calleeFrame();
                if (calleeFrame == null) {
                    return other.calleeFrame() == null;
                }
                return calleeFrame.isSameFrame(other.calleeFrame());
            }
            return false;
        }

        @Override
        public String toString() {
            if (error == null) {
                return "<truncated: omitted " + omitted + " frames>";
            }
            return "<error: " + error + ">";
        }
    }

    /**
     * If the stack walk is stopped due to an exception or {@code maxDepth} being reached,
     * a {@link TruncatedStackFrame} is appended the end of the returned list.
     *
     * @param maxDepth
     * @return
     */
    public List<StackFrame> frames(final int maxDepth) {
        if (maxDepth <= 0) {
            return Collections.emptyList();
        }
        final ArrayList<StackFrame> frames = new ArrayList<StackFrame>();
        try {
            final boolean[] hitMaxDepth = {false};
            final StackFrameVisitor visitor = new StackFrameVisitor() {
                @Override
                public boolean visitFrame(StackFrame stackFrame) {
                    frames.add(stackFrame);
                    if (frames.size() == maxDepth) {
                        hitMaxDepth[0] = true;
                        return false;
                    }
                    return true;
                }
            };
            inspect(cpuInstructionPointer, cpuStackPointer, cpuFramePointer, visitor);
            if (hitMaxDepth[0]) {
                StackFrame last = frames.get(frames.size() - 1);
                class Counter extends RawStackFrameVisitor {
                    int count;
                    @Override
                    public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
                        count++;
                        return true;
                    }
                }
                Counter c = new Counter();
                if (maxDepth != 1) {
                    inspect(last.ip, last.sp, last.fp, c);
                }
                frames.add(new TruncatedStackFrame(last, c.count));
            }

        } catch (Throwable e) {
            e.printStackTrace();
            final StackFrame parentFrame = frames.isEmpty() ? null : frames.get(frames.size() - 1);
            frames.add(new TruncatedStackFrame(parentFrame, e));
        }
        return frames;
    }

    @Override
    public TargetMethod targetMethodFor(Pointer instructionPointer) {
        final TeleCompilation compiledCode = vm.codeCache().findCompiledCode(instructionPointer);
        if (compiledCode != null) {
            return compiledCode.teleTargetMethod().targetMethod();
        }
        return null;
    }

    @Override
    public byte readByte(Address address, int offset) {
        return vm.memory().readByte(address, offset);
    }

    @Override
    public Word readWord(Address address, int offset) {
        return vm.memory().readWord(address, offset);
    }

    @Override
    public int readInt(Address address, int offset) {
        return vm.memory().readInt(address, offset);
    }

    @Override
    public Pointer readPointer(VmThreadLocal local) {
        return teleEnabledVmThreadLocalValues == null ? Pointer.zero() : teleEnabledVmThreadLocalValues.getWord(local).asPointer();
    }
}
