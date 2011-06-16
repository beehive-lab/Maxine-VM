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
package com.sun.max.tele.method;

import java.io.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Representation of compilation in the VM:  a method, stub, adapter, or other routine.
 * Much of the information is derived by delegation to
 * a surrogate for the corresponding instance of {@link TargetMethod}
 * in the VM.
 */
public final class TeleCompilation extends AbstractTeleVMHolder implements MaxCompilation {

    /**
     * Description of a compiled code region allocated in a code cache.
     * <br>
     * The parent of this region is the {@link MaxCompiledCodeRegion} in which it is created.
     * <br>
     * This region has no children, unless we decide later to subdivide and model the parts separately.
     *
     */
    private static final class CompiledCodeMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxCompilation> {

        private static final List<MaxEntityMemoryRegion<? extends MaxEntity>> EMPTY = Collections.emptyList();

        private final TeleCompilation owner;
        private final boolean isBootCode;
        private final TeleCodeCache teleCodeCache;

        private CompiledCodeMemoryRegion(TeleVM teleVM, TeleCompilation owner, TeleTargetMethod teleTargetMethod, TeleCodeCache teleCodeCache, boolean isBootCode) {
            super(teleVM, teleTargetMethod);
            this.owner = owner;
            this.isBootCode = isBootCode;
            this.teleCodeCache = teleCodeCache;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Evaluate this lazily, since this isn't known until the code's memory
            // region is actually allocated.
            return teleCodeCache.findCompiledCodeRegion(start()).memoryRegion();
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxCompilation owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return isBootCode;
        }
    }

    /**
     * Adapter for bytecode scanning that only knows the constant pool
     * index argument of the last method invocation instruction scanned.
     */
    private static final class MethodRefIndexFinder extends BytecodeAdapter  {
        int methodRefIndex = -1;

        public MethodRefIndexFinder reset() {
            methodRefIndex = -1;
            return this;
        }

        @Override
        protected void invokestatic(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            methodRefIndex = index;
        }

        public int methodRefIndex() {
            return methodRefIndex;
        }
    }

    private final TeleTargetMethod teleTargetMethod;
    private CodeLocation codeStartLocation = null;
    private final CompiledCodeMemoryRegion compiledCodeMemoryRegion;
    private List<MachineCodeLocation> instructionLocations;

    /**
     * Creates an object that describes a region of VM memory used to hold a single compiled method.
     *
     * @param teleVM the VM
     * @param teleTargetMethod surrogate for the compilation in the VM
     * @param teleCodeCache the owner of all cached code in the VM
     * @param isBootCode is this code in the boot region?
     */
    public TeleCompilation(TeleVM teleVM, TeleTargetMethod teleTargetMethod, TeleCodeCache teleCodeCache, boolean isBootCode) {
        super(teleVM);
        this.teleTargetMethod = teleTargetMethod;
        this.compiledCodeMemoryRegion = new CompiledCodeMemoryRegion(teleVM, this, teleTargetMethod, teleCodeCache, isBootCode);
    }

    public String entityName() {
        final ClassMethodActor classMethodActor = classMethodActor();
        if (classMethodActor != null) {
            return "Code " + classMethodActor.simpleName();
        }
        return teleTargetMethod.getRegionName();
    }

    public String entityDescription() {
        final ClassMethodActor classMethodActor = teleTargetMethod.classMethodActor();
        final String description = teleTargetMethod.getClass().getSimpleName() + " for ";
        if (classMethodActor != null) {
            return description + classMethodActor.simpleName();
        }
        return description + teleTargetMethod.classActorForObjectType().simpleName();
    }

    public MaxEntityMemoryRegion<MaxCompilation> memoryRegion() {
        return compiledCodeMemoryRegion;
    }

    public boolean contains(Address address) {
        return compiledCodeMemoryRegion.contains(address);
    }

    public TeleObject representation() {
        return teleTargetMethod;
    }

    public InstructionMap getInstructionMap() {
        return teleTargetMethod.getInstructionMap();
    }

    public int vmCodeGeneration() {
        return teleTargetMethod.vmCodeGenerationCount();
    }

    public Address getCodeStart() {
        return teleTargetMethod.getCodeStart();
    }

    public CodeLocation getCodeStartLocation() {
        final Address codeStart = getCodeStart();
        if (codeStartLocation == null && codeStart != null) {
            codeStartLocation = codeManager().createMachineCodeLocation(codeStart, "start location in code");
        }
        return codeStartLocation;
    }

    public Address getCallEntryPoint() {
        return teleTargetMethod.callEntryPoint();
    }

    public CodeLocation getCallEntryLocation() {
        final Address callEntryPoint = getCallEntryPoint();
        if (callEntryPoint.isZero()) {
            return null;
        }
        return codeManager().createMachineCodeLocation(callEntryPoint, "Code entry");
    }

    public int compilationIndex() {
        return teleTargetMethod.compilationIndex();
    }

    public TeleClassMethodActor getTeleClassMethodActor() {
        return teleTargetMethod.getTeleClassMethodActor();
    }

    public ClassMethodActor classMethodActor() {
        final TeleClassMethodActor teleClassMethodActor = getTeleClassMethodActor();
        if (teleClassMethodActor != null) {
            return teleClassMethodActor.classMethodActor();
        }
        return null;
    }

    public ClassActor classActorForObjectType() {
        return teleTargetMethod.classActorForObjectType();
    }

    public TeleTargetMethod teleTargetMethod() {
        return teleTargetMethod;
    }

    /**
     * Gets the name of the source variable corresponding to a stack slot, if any.
     *
     * @param slot a stack slot
     * @return the Java source name for the frame slot, null if not available.
     */
    public String sourceVariableName(MaxStackFrame.Compiled javaStackFrame, int slot) {
        return teleTargetMethod.sourceVariableName(javaStackFrame, slot);
    }

    public void writeSummary(PrintStream printStream) {
        teleTargetMethod.writeSummary(printStream);
    }
}
