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
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * Access to compiled code in the VM.
 * <br>
 * Much of the important state in the VM about the code cache is contained
 * in a singleton heap object, an instance of {@linkplain com.sun.max.vm.code.CodeManager CodeManager}.
 * <br>
 * The VM's cache allocates memory in one or more memory regions.  At the moment,
 * the VM allocates only one dynamic code region.
 *
 * @see com.sun.max.vm.code.CodeManager
 * @author Michael Van De Vanter
 */
public final class TeleCodeCache extends AbstractTeleVMHolder implements TeleVMCache, MaxCodeCache {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final String entityName = "Code Cache";
    private final String entityDescription;

    /**
     * The object in the VM that manages cached compiled code.
     */
    private TeleCodeManager teleCodeManager;

    /**
     * Contains all the compiled code we know about,
     * organized for lookup by address.
     */
    private final CodeRegistry codeRegistry;

    private final String bootCodeRegionName;
    private TeleCompiledCodeRegion bootCodeRegion = null;
    private TeleCompiledCodeRegion dynamicCodeRegion = null;

    /**
     * Unmodifiable list of all regions in which compiled code is stored.
     */
    private List<MaxCompiledCodeRegion> compiledCodeRegions = Collections.emptyList();

    /**
     * Creates the object that models the cache of compiled code in the VM.
     * <br>
     * A subsequent call to {@link #initialize()} is required before this object is functional.
     */
    public TeleCodeCache(TeleVM vm) {
        super(vm);
        assert vm().lockHeldByCurrentThread();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.codeRegistry = new CodeRegistry(vm);
        this.entityDescription = "Storage managment in the " + vm().entityName() + " for method compilations";
        this.bootCodeRegionName = vm().getString(vm().teleFields().Code_CODE_BOOT_NAME.readReference(vm()));
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");
        tracer.end(null);
    }

    /**
     * Check whether the code cache is prepared to start registering entries, in particular
     * whether it knows about the code regions in the VM.
     *
     * @return whether the code cache is fully initialized and operational.
     */
    public boolean isInitialized() {
        return bootCodeRegion != null;
    }

    /**
     * Completes the initialization of this object:  identifies the VM's code regions, and preloads
     * the cache of information about method compilations in the boot code region.
     */
    public void initialize(long epoch) {
        assert vm().lockHeldByCurrentThread();
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " initializing");
        tracer.begin();

        teleCodeManager = (TeleCodeManager) heap().makeTeleObject(vm().teleFields().Code_codeManager.readReference(vm()));
        bootCodeRegion = new TeleCompiledCodeRegion(vm(), teleCodeManager.teleBootCodeRegion(), true);
        dynamicCodeRegion = new TeleCompiledCodeRegion(vm(), teleCodeManager.teleRuntimeCodeRegion(), false);

        List<MaxCompiledCodeRegion> regions = new ArrayList<MaxCompiledCodeRegion>(2);
        regions.add(bootCodeRegion);
        regions.add(dynamicCodeRegion);
        compiledCodeRegions = Collections.unmodifiableList(regions);
        lastUpdateEpoch = epoch;
        tracer.end(null);
    }

    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            assert vm().lockHeldByCurrentThread();
            codeRegistry.updateCache(epoch);
            bootCodeRegion.updateCache(epoch);
            dynamicCodeRegion.updateCache(epoch);
            lastUpdateEpoch = epoch;
            updateTracer.end(null);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch-" + epoch + ": " + this);
        }
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxCodeCache> memoryRegion() {
        // The code cache has no memory allocation of its own, but
        // rather owns one or more code regions that have memory
        // allocated from the OS.
        return null;
    }

    public boolean contains(Address address) {
        return findCompiledCodeRegion(address) != null;
    }

    public TeleCompiledCodeRegion bootCodeRegion() {
        return bootCodeRegion;
    }

    public List<MaxCompiledCodeRegion> compiledCodeRegions() {
        return compiledCodeRegions;
    }

    public TeleCompiledCodeRegion findCompiledCodeRegion(Address address) {
        if (bootCodeRegion.memoryRegion().contains(address)) {
            return bootCodeRegion;
        }
        if (dynamicCodeRegion.memoryRegion().contains(address)) {
            return dynamicCodeRegion;
        }
        return null;
    }

    public MaxMachineCode< ? extends MaxMachineCode> findMachineCode(Address address) {
        TeleExternalCode externalCode = codeRegistry.getExternalCode(address);
        if (externalCode != null) {
            return externalCode;
        }
        return findCompiledCode(address);
    }

    public TeleCompiledCode findCompiledCode(Address address) {
        TeleCompiledCode teleCompiledCode = codeRegistry.getCompiledCode(address);
        if (teleCompiledCode == null) {
            // Not a known Java method.
            if (!contains(address)) {
                // The address is not in a method code allocation region; no use looking further.
                return null;
            }
            // Not a known compiled method, and not some other kind of known target code, but in a code region
            // Use the interpreter to see if the code manager in the VM knows about it.
            try {
                final Reference targetMethodReference = vm().teleMethods().Code_codePointerToTargetMethod.interpret(new WordValue(address)).asReference();
                // Possible that the address points to an unallocated area of a code region.
                if (targetMethodReference != null && !targetMethodReference.isZero()) {
                    heap().makeTeleObject(targetMethodReference);  // Constructor will register the compiled method if successful
                }
            } catch (MaxVMBusyException maxVMBusyException) {
            } catch (TeleInterpreterException e) {
                throw TeleError.unexpected(e);
            }
            // If a new method was discovered, then it will have been added to the registry.
            teleCompiledCode = codeRegistry.getCompiledCode(address);
        }
        return teleCompiledCode;
    }

    public List<MaxCompiledCode> compilations(TeleClassMethodActor teleClassMethodActor) {
        final List<MaxCompiledCode> compilations = new ArrayList<MaxCompiledCode>(teleClassMethodActor.numberOfCompilations());
        for (TeleTargetMethod teleTargetMethod : teleClassMethodActor.compilations()) {
            compilations.add(findCompiledCode(teleTargetMethod.getRegionStart()));
        }
        return Collections.unmodifiableList(compilations);
    }

    public TeleCompiledCode latestCompilation(TeleClassMethodActor teleClassMethodActor) throws MaxVMBusyException {
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            final TeleTargetMethod teleTargetMethod = teleClassMethodActor.getCurrentCompilation();
            return teleTargetMethod == null ? null : findCompiledCode(teleTargetMethod.getRegionStart());
        } finally {
            vm().unlock();
        }
    }

    public MaxExternalCode findExternalCode(Address address) {
        return codeRegistry.getExternalCode(address);
    }

    // TODO (mlvdv) fix
    public TeleExternalCode createExternalCode(Address codeStart, long nBytes, String name) throws MaxVMBusyException, MaxInvalidAddressException {
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            TeleExternalCode create = TeleExternalCode.create(vm(), codeStart, nBytes, name);
            return create;
        } finally {
            vm().unlock();
        }
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        printStream.print(indentation + "Compilations registered: " + formatter.format(codeRegistry.size()) + "\n");
        if (bootCodeRegion != null) {
            printStream.print(indentation + "Registered from " + bootCodeRegion.entityName() + ": " + formatter.format(bootCodeRegion.compilationCount())
                            + " (loaded: " + formatter.format(bootCodeRegion.loadedCompilationCount()) + ")\n");
        }
        if (dynamicCodeRegion != null) {
            printStream.print(indentation + "Registerd from " + dynamicCodeRegion.entityName() + ": " + formatter.format(dynamicCodeRegion.compilationCount())
                            + " (loaded: " + formatter.format(dynamicCodeRegion.loadedCompilationCount()) + ")\n");
        }
    }

    public void writeSummary(PrintStream printStream) {
        codeRegistry.writeSummary(printStream);
    }

    /**
     * Gets the name used by the VM to identify the distinguished
     * boot code region, determined by static inspection of the field
     * that holds the value in the VM.
     *
     * @return the name assigned to the VM's boot code memory region
     * @see Code
     */
    public String bootCodeRegionName() {
        return bootCodeRegionName;
    }

    /**
     * Adds a region of native code to the coded registry, indexed by code address.
     *
     * @param teleExternalCode
     * @throws IllegalArgumentException when the memory region of {@link teleTargetMethod} overlaps one already in this registry.
     */
    public void register(TeleExternalCode teleExternalCode) {
        codeRegistry.add(teleExternalCode);

    }

    /**
     * Adds a {@link MaxCompiledCode} entry to the code registry, indexed by code address.
     * This should only be called from a constructor of a {@link TeleTargetMethod} subclass.
     *
     * @param teleTargetMethod the compiled method whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the memory region of {@link teleTargetMethod} overlaps one already in this registry.
     */
    public void register(TeleTargetMethod teleTargetMethod) {
        final TeleCompiledCodeRegion teleCompiledCodeRegion = findCompiledCodeRegion(teleTargetMethod.getRegionStart());
        codeRegistry.add(new TeleCompiledCode(vm(), teleTargetMethod, this, teleCompiledCodeRegion == bootCodeRegion));
    }

}
