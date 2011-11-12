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
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * The singleton manager for representations of machine code locations, both VM method
 * compilations and blocks of external native code about which less is known.
 */
public final class VmMachineCodeAccess extends AbstractVmHolder implements MaxMachineCode, TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private static VmMachineCodeAccess vmMachineCodeAccess;

    public static VmMachineCodeAccess make(TeleVM vm) {
        if (vmMachineCodeAccess == null) {
            vmMachineCodeAccess = new VmMachineCodeAccess(vm);
        }
        return vmMachineCodeAccess;
    }

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

    private final String entityName = "Machine Code";

    private final String entityDescription;

    private final ExternalMachineCodeAccess externalMachineCodeAccess;

    /**
     * A collection of {@link TeleTargetMethod} instances that
     * have been created for inspection (and registered here) before having been
     * allocated memory in the VM.  The registration of these will be completed during the
     * next update cycle after their location and size become known.
     */
    private final Set<TeleTargetMethod> unallocatedTeleTargetMethods = new HashSet<TeleTargetMethod>();

    private final Object statsPrinter = new Object() {
        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            // TODO (mlvdv) add some stats?
            return msg.toString();
        }
    };

    public VmMachineCodeAccess(TeleVM vm) {
        super(vm);
        final TimedTrace tracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        tracer.begin();
        this.entityDescription = "Remote code pointer creation and management for the " + vm.entityName();
        this.externalMachineCodeAccess = new ExternalMachineCodeAccess(vm);
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");
        tracer.end(statsPrinter);
    }

    /** {@inheritDoc}
     * <p>
     * Updates the representation of every <strong>method compilation</strong> surrogate
     * (represented as instances of subclasses of {@link TeleTargetMethod},
     * in case any of the information in the VM's representation has changed since the previous update.  This should not be
     * attempted until all information about allocated regions that might contain objects has been updated.
     */
    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            updateTracer.begin();
            assert vm().lockHeldByCurrentThread();
            for (TeleTargetMethod teleTargetMethod : unallocatedTeleTargetMethods) {
                if (!teleTargetMethod.getRegionStart().isZero() && teleTargetMethod.getRegionNBytes() != 0) {
                    // The compilation has been allocated memory in the VM since the last time we looked; complete its registration.
                    unallocatedTeleTargetMethods.remove(teleTargetMethod);
                    registerCompilation(teleTargetMethod);
                }
            }
            for (VmCodeCacheRegion region : codeCache().vmCodeCacheRegions()) {
                region.updateCache(epoch);
            }
            lastUpdateEpoch = epoch;
            updateTracer.end(null);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch);
        }
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxMachineCode> memoryRegion() {
        // This represents no memory allocation; code resides in regions managed by the code cache.
        return null;
    }

    public boolean contains(Address address) {
        // There's no real notion of containing an address here.
        return false;
    }

    public TeleObject representation() {
        // No distinguished object in VM runtime represents this.
        return null;
    }

    public MaxMachineCodeRoutine<? extends MaxMachineCodeRoutine> findMachineCode(Address address) {
        TeleCompilation compilation = findCompilation(address);
        return (compilation != null) ? compilation : findExternalCode(address);
    }

    /**
     * Get the method compilation, if any, whose code cache allocation includes
     * a given address in the VM, whether or not there is target code at the
     * specific location.
     *
     * @param address memory location in the VM
     * @return a  method compilation whose code cache allocation includes the address, null if none
     */
    private TeleCompilation findCompilationByAllocaton(Address address) {
        TeleCompilation teleCompilation = null;
        for (VmCodeCacheRegion codeCacheRegion : codeCache().vmCodeCacheRegions()) {
            teleCompilation = codeCacheRegion.findCompilation(address);
            if (teleCompilation != null) {
                break;
            }
        }
        if (teleCompilation == null) {
            // Not a known method compilation.
            if (!codeCache().contains(address)) {
                // The address is not in the code cache.
                return null;
            }
            // Not a known method compilation, but in a code cache region.
            // Use the interpreter to see if the code manager in the VM knows about it.
            try {
                final Reference targetMethodReference = vm().methods().Code_codePointerToTargetMethod.interpret(new WordValue(address)).asReference();
                // Possible that the address points to an unallocated area of a code region.
                if (targetMethodReference != null && !targetMethodReference.isZero()) {
                    objects().makeTeleObject(targetMethodReference);  // Constructor will register the compiled method if successful
                }
            } catch (MaxVMBusyException maxVMBusyException) {
            } catch (TeleInterpreterException e) {
                // This sometimes happens when the VM process terminates; ignore in those cases
                if (vm().state().processState() != MaxProcessState.TERMINATED) {
                    throw TeleError.unexpected(e);
                }
            }
            // If a new method was discovered, the cache will now know about it
            for (VmCodeCacheRegion codeCacheRegion : codeCache().vmCodeCacheRegions()) {
                teleCompilation = codeCacheRegion.findCompilation(address);
                if (teleCompilation != null) {
                    break;
                }
            }
        }
        return teleCompilation;
    }

    public TeleCompilation findCompilation(Address address) {
        TeleCompilation teleCompilation = findCompilationByAllocaton(address);
        if (teleCompilation != null && teleCompilation.isValidCodeLocation(address)) {
            return teleCompilation;
        }
        return null;
    }

    public List<MaxCompilation> compilations(TeleClassMethodActor teleClassMethodActor) {
        final List<MaxCompilation> compilations = new ArrayList<MaxCompilation>(teleClassMethodActor.compilationCount());
        for (TeleTargetMethod teleTargetMethod : teleClassMethodActor.compilations()) {
            compilations.add(findCompilationByAllocaton(teleTargetMethod.getRegionStart()));
        }
        return Collections.unmodifiableList(compilations);
    }

    public TeleCompilation latestCompilation(TeleClassMethodActor teleClassMethodActor) throws MaxVMBusyException {
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            final TeleTargetMethod teleTargetMethod = teleClassMethodActor.getCurrentCompilation();
            return teleTargetMethod == null ? null : findCompilation(teleTargetMethod.getRegionStart());
        } finally {
            vm().unlock();
        }
    }

    public TeleExternalCodeRoutine registerExternalCode(Address codeStart, long nBytes, String name) throws MaxVMBusyException, IllegalArgumentException, MaxInvalidAddressException {
        return externalMachineCodeAccess.registerExternalCode(codeStart, nBytes, name);
    }

    public MaxExternalCodeRoutine findExternalCode(Address address) {
        return externalMachineCodeAccess.findExternalCode(address);
    }

    /**
     * Adds a {@link MaxCompilation} entry to the code registry, indexed by code address.
     * This should only be called from a constructor of a {@link TeleTargetMethod} subclass.
     *
     * @param teleTargetMethod the compiled method whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the memory region of {@link teleTargetMethod} overlaps one already in this registry.
     */
    public void registerCompilation(TeleTargetMethod teleTargetMethod) {
        if (teleTargetMethod.getRegionStart().isZero() || teleTargetMethod.getRegionNBytes() == 0) {
            // The compilation is being constructed, which is to say it exists in the VM but has not
            // had an allocation of memory assigned to it.
            unallocatedTeleTargetMethods.add(teleTargetMethod);
            TeleWarning.message(tracePrefix() + " unallocated TargetMethod registered");
        } else {
            // Find the code cache region in which the compilation has been allocated, and add it to
            // the registry we keep for that code region.
            final VmCodeCacheRegion codeCacheRegion = vm().codeCache().findCodeCacheRegion(teleTargetMethod.getRegionStart());
            assert codeCacheRegion != null;
            teleTargetMethod.setCodeCacheRegion(codeCacheRegion);
            codeCacheRegion.register(teleTargetMethod);
        }
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        int compilationCount = 0;
        int loadedCompilationCount = 0;
        for (VmCodeCacheRegion codeCacheRegion : codeCache().vmCodeCacheRegions()) {
            compilationCount += codeCacheRegion.compilationCount();
            loadedCompilationCount += codeCacheRegion.loadedCompilationCount();
        }
        printStream.print(indentation + "Total compilations registered: " + formatter.format(compilationCount));
        if (!unallocatedTeleTargetMethods.isEmpty()) {
            printStream.print(" (" + formatter.format(unallocatedTeleTargetMethods.size()) + " unallocated");
        }
        printStream.print(" (code loaded: " + formatter.format(loadedCompilationCount) + ")\n");
        printStream.println(indentation + "By region:");
        for (VmCodeCacheRegion codeCacheRegion : codeCache().vmCodeCacheRegions()) {
            final StringBuffer sb = new StringBuffer();
            sb.append(codeCacheRegion.entityName() + ": ");
            sb.append("Compilations=" + formatter.format(codeCacheRegion.compilationCount()));
            sb.append(", code loaded=" + formatter.format(codeCacheRegion.loadedCompilationCount()));
            printStream.println(indentation + "    " + sb.toString());
        }
        externalMachineCodeAccess.printSessionStats(printStream, indent, verbose);
    }

    public void writeSummary(PrintStream printStream) {
        for (VmCodeCacheRegion codeCacheRegion : codeCache().vmCodeCacheRegions()) {
            codeCacheRegion.writeSummary(printStream);
        }
        externalMachineCodeAccess.writeSummary(printStream);
    }

}
