/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Representation of compilation in the VM:  a method, stub, adapter, or other routine.
 * Much of the information is derived by delegation to
 * a surrogate for the corresponding instance of {@link TargetMethod}
 * in the VM.
 */
public final class TeleCompilation extends AbstractVmHolder implements MaxCompilation {

    /**
     * Description of a compiled code region allocated in a code cache.
     * <br>
     * The parent of this region is the {@link MaxCodeCacheRegion} in which it is created.
     * <br>
     * This region has no children, unless we decide later to subdivide and model the parts separately.
     *
     */
    private static final class CompiledCodeMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxCompilation> {

        private static final List<MaxEntityMemoryRegion<? extends MaxEntity>> EMPTY = Collections.emptyList();

        private final TeleCompilation owner;
        private final boolean isBootCode;
        private final VmCodeCacheAccess codeCache;

        private CompiledCodeMemoryRegion(TeleVM vm, TeleCompilation owner, TeleTargetMethod teleTargetMethod, VmCodeCacheAccess codeCache, boolean isBootCode) {
            super(vm, teleTargetMethod);
            this.owner = owner;
            this.isBootCode = isBootCode;
            this.codeCache = codeCache;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Evaluate this lazily, since this isn't known until the code's memory
            // region is actually allocated.
            return codeCache.findCodeCacheRegion(start()).memoryRegion();
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

    private final TeleTargetMethod teleTargetMethod;
    private CodeLocation codeStartLocation = null;
    private final CompiledCodeMemoryRegion compiledCodeMemoryRegion;

    /**
     * Creates an object that describes a region of VM memory used to hold a single compiled method.
     *
     * @param vm the VM
     * @param teleTargetMethod surrogate for the compilation in the VM
     * @param codeCache the owner of all cached code in the VM
     * @param isBootCode is this code in the boot region?
     */
    public TeleCompilation(TeleVM vm, TeleTargetMethod teleTargetMethod, VmCodeCacheAccess codeCache, boolean isBootCode) {
        super(vm);
        this.teleTargetMethod = teleTargetMethod;
        this.compiledCodeMemoryRegion = new CompiledCodeMemoryRegion(vm, this, teleTargetMethod, codeCache, isBootCode);
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

    public MaxMachineCodeInfo getMachineCodeInfo() {
        return teleTargetMethod.getMachineCodeInfo();
    }

    public int codeVersion() {
        return teleTargetMethod.codeVersion();
    }

    public Address getCodeStart() {
        return teleTargetMethod.getCodeStart();
    }

    public CodeLocation getCodeStartLocation() {
        if (codeStartLocation == null) {
            try {
                codeStartLocation = codeLocations().createMachineCodeLocation(getCodeStart(), "start location in code");
            } catch (InvalidCodeAddressException e) {
            }
        }
        return codeStartLocation;
    }

    public Address getCallEntryPoint() {
        return teleTargetMethod.callEntryPoint();
    }

    public CodeLocation getCallEntryLocation() {
        try {
            return codeLocations().createMachineCodeLocation(getCallEntryPoint(), "Code entry");
        } catch (InvalidCodeAddressException e) {
        }
        return null;
    }

    public boolean isCodeLive() {
        return !teleTargetMethod.isCodeEvicted();
    }

    public String shortDesignator() {
        return teleTargetMethod.shortDesignator();
    }

    public String longDesignator() {
        return teleTargetMethod.longDesignator();
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

    public boolean isValidCodeLocation(Address address) throws IllegalArgumentException {
        return teleTargetMethod.isValidCodeLocation(address);
    }

    /**
     * Returns the local surrogate for the {@link TargetMethod} that the
     * VM uses to represent a method compilation.
     */
    public TeleTargetMethod teleTargetMethod() {
        return teleTargetMethod;
    }

    public VMFrameLayout frameLayout() {
        TargetMethod targetMethod = teleTargetMethod.targetMethod();
        if (targetMethod != null) {
            return targetMethod.frameLayout();
        }
        return null;
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
