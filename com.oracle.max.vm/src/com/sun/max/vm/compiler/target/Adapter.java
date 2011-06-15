/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target;

import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.stack.*;

/**
 * An adapter is a code stub interposing a call between two methods that have different calling conventions. The adapter
 * is called upon entry to the callee in the prologue specific to the {@linkplain CallEntryPoint entry point} used by
 * the caller. This allows a call between two methods that share the same calling convention to avoid an adapter
 * altogether.
 *
 * The adapter framework assumes there are exactly two calling conventions in use by the compilers in the VM. While the
 * details are platform specific, the {@linkplain CallEntryPoint#OPTIMIZED_ENTRY_POINT "OPT"} calling convention mostly
 * conforms to the C ABI of the underlying platform. The {@linkplain CallEntryPoint#BASELINE_ENTRY_POINT "baseline"} convention is
 * used by code that maintains an expression stack (much like an interpreter) and it uses two separate registers for a
 * frame pointer and a stack pointer. The frame pointer is used to access incoming arguments and local variables, and
 * the stack pointer is used to maintain the Java expression stack. All arguments to Java calls under the "baseline"
 * convention are passed via the Java expression stack.
 *
 * Return values are placed in a register under both conventions.
 */
public abstract class Adapter extends TargetMethod {

    /**
     * The type of adaptation performed by an adapter. Each enum value denotes an ordered pair of two
     * different calling conventions. The platform specific details of each calling convention
     * are documented by the platform-specific subclasses of {@link AdapterGenerator}.
     *
     */
    public enum Type {
        /**
         * Type of an adapter that interposes a call from code compiled with the baseline calling convention to
         * code compiled with the "OPT" calling convention.
         */
        BASELINE2OPT(BASELINE_ENTRY_POINT, OPTIMIZED_ENTRY_POINT),

        /**
         * Type of an adapter that interposes a call from code compiled with the "OPT" calling convention to
         * code compiled with the "baseline" calling convention.
         */
        OPT2BASELINE(OPTIMIZED_ENTRY_POINT, BASELINE_ENTRY_POINT);

        Type(CallEntryPoint caller, CallEntryPoint callee) {
            this.caller = caller;
            this.callee = callee;
        }

        /**
         * Denotes the calling convention of an adapter's caller.
         */
        public final CallEntryPoint caller;

        /**
         * Denotes the calling convention of an adapter's callee.
         */
        public final CallEntryPoint callee;
    }

    /**
     * The generator that produced this adapter.
     */
    @INSPECTED(deepCopied = false)
    public final AdapterGenerator generator;

    /**
     * Creates an adapter and installs it in the code manager.
     *
     * @param generator the generator that produced the adapter
     * @param description a textual description of the adapter
     * @param frameSize the size of the adapter frame
     * @param code the adapter code
     * @param callPosition TODO
     */
    public Adapter(AdapterGenerator generator, String description, int frameSize, byte[] code, int callPosition) {
        super(Adapter, description, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        this.setFrameSize(frameSize);
        this.generator = generator;

        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(0, 0, 0);
        targetBundleLayout.update(ArrayField.code, code.length);
        Code.allocate(targetBundleLayout, this);
        setData(null, null, code);
        setStopPositions(new int[] {callPosition}, NO_DIRECT_CALLEES, 1, 0);
    }

    public static final Object[] NO_DIRECT_CALLEES = {};

    /**
     * Gets the offset of the call to this in a method's prologue.
     */
    public abstract int callOffsetInPrologue();

    @Override
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
    }

    @Override
    public boolean isPatchableCallSite(Address callSite) {
        FatalError.unexpected("Adapter should never be patched");
        return false;
    }

    @Override
    public Address fixupCallSite(int callOffset, Address callEntryPoint) {
        throw FatalError.unexpected("Adapter should never be patched");
    }

    @Override
    public Address patchCallSite(int callOffset, Address callEntryPoint) {
        throw FatalError.unexpected("Adapter should never be patched");
    }

    @Override
    public Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass) {
        if (isTopFrame) {
            throw FatalError.unexpected("Exception occurred in frame adapter");
        }
        return Address.zero();
    }

    @Override
    public void traceDebugInfo(IndentWriter writer) {
    }

    @Override
    public void traceExceptionHandlers(IndentWriter writer) {
    }

    @Override
    public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
    }

    @Override
    public void catchException(Cursor current, Cursor callee, Throwable throwable) {
        // Exceptions do not occur in adapters
    }
}
