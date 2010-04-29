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
package com.sun.max.tele.method;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * Access to compiled code in the VM.
 * <br>
 * Much of the important state
 * about the code cache is contained in a singleton heap object, an instance
 * of {@link CodeManger}.
 * <br>
 * The cache allocates memory in one or more memory regions.  At the moment,
 * the VM allocates only one dynamic code region.
 *
 * @see com.sun.max.vm.code.CodeManager
 * @author Michael Van De Vanter
 */
public final class TeleCodeCache extends AbstractTeleVMHolder implements MaxCodeCache {

    private static final int TRACE_VALUE = 2;

    private final String entityName = "Code Cache";
    private final String entityDescription;

    /**
     * The object in the VM that manages cached compiled code.
     */
    private TeleCodeManager teleCodeManager;

    private CodeRegistry codeRegistry;

    private final String bootCodeRegionName;
    private TeleCompiledCodeRegion bootCodeRegion = null;
    private TeleCompiledCodeRegion dynamicCodeRegion = null;
    private VariableSequence<MaxCompiledCodeRegion> compiledCodeRegions = new ArrayListSequence<MaxCompiledCodeRegion>(2);

    /**
     * Creates the objects that models the cache of compiled code in the VM.
     * <br>
     * A subsequent call to {@link #initialize()} is required before this object is functional.
     */
    public TeleCodeCache(TeleVM teleVM) {
        super(teleVM);
        entityDescription = "Storage managment in the " + vm().entityName() + " for method compilations";
        if (!teleVM.tryLock()) {
            ProgramError.unexpected("Unable to initialize code cache from VM");
        }
        try {
            final Reference nameReference = vm().teleFields().Code_CODE_BOOT_NAME.readReference(vm());
            bootCodeRegionName = vm().getString(nameReference);
        } finally {
            teleVM.unlock();
        }
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
     * Completes the initialization of this object:  identifies the VM's code regions, and pre-loads
     * the cache of information about method compilations in the boot code region.
     */
    public void initialize() {
        Trace.begin(1, tracePrefix() + " initializing");
        final long startTimeMillis = System.currentTimeMillis();
        int count = 0;

        teleCodeManager = (TeleCodeManager) vm().makeTeleObject(vm().teleFields().Code_codeManager.readReference(vm()));
        bootCodeRegion = new TeleCompiledCodeRegion(vm(), teleCodeManager.teleBootCodeRegion(), true);
        dynamicCodeRegion = new TeleCompiledCodeRegion(vm(), teleCodeManager.teleRuntimeCodeRegion(), false);

        compiledCodeRegions.append(bootCodeRegion);
        compiledCodeRegions.append(dynamicCodeRegion);

        for (TeleTargetMethod teleTargetMethod : bootCodeRegion().teleTargetMethods()) {
            teleTargetMethod.classMethodActor();
            count++;
        }
        Trace.end(1, tracePrefix() + " initializing (" + count + " methods in boot code region pre-loaded)", startTimeMillis);
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

    public MaxCompiledCodeRegion bootCodeRegion() {
        return bootCodeRegion;
    }

    public IndexedSequence<MaxCompiledCodeRegion> compiledCodeRegions() {
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

    // TODO (mlvdv) sort out comments here
    /**
     * Gets a {@code TeleTargetMethod} instance representing the {@link TargetMethod} in the tele VM that contains a
     * given instruction pointer. If the instruction pointer's address does not lie within a target method, then null is returned.
     * If the instruction pointer is within a target method but there is no {@code TeleTargetMethod} instance existing
     * for it in the {@linkplain TeleCodeCache tele code cache}, then a new instance is created and returned.
     *
     * @param address an instruction pointer in the tele VM's address space
     * @return {@code TeleTargetMethod} instance representing the {@code TargetMethod} containing {@code
     *         instructionPointer} or null if there is no {@code TargetMethod} containing {@code instructionPointer}
     */
    public TeleTargetMethod makeTeleTargetMethod(Address address) {
        ProgramError.check(!address.isZero());
        if (!vm().isBootImageRelocated()) {
            return null;
        }
        TeleTargetMethod teleTargetMethod = findTeleTargetRoutine(TeleTargetMethod.class, address);
        if (teleTargetMethod == null
                        && findTeleTargetRoutine(MaxCompiledCode.class, address) == null
                        && contains(address)) {
            // Not a known java target method, and not some other kind of known target code, but in a code region
            // See if the code manager in the VM knows about it.
            try {
                final Reference targetMethodReference = vm().teleMethods().Code_codePointerToTargetMethod.interpret(new WordValue(address)).asReference();
                // Possible that the address points to an unallocated area of a code region.
                if (targetMethodReference != null && !targetMethodReference.isZero()) {
                    teleTargetMethod = (TeleTargetMethod) vm().makeTeleObject(targetMethodReference);  // Constructor will add to register.
                }
            } catch (TeleInterpreterException e) {
                throw ProgramError.unexpected(e);
            }
        }
        return teleTargetMethod;
    }

    public TeleCompiledNativeCode createTeleNativeTargetRoutine(Address codeStart, Size codeSize, String name) {
        return TeleCompiledNativeCode.create(vm(), codeStart, codeSize, name);
    }

    @Deprecated
    public <TeleTargetRoutine_Type extends MaxCompiledCode> TeleTargetRoutine_Type findTeleTargetRoutine(Class<TeleTargetRoutine_Type> teleTargetRoutineType, Address address) {
        return codeRegistry().get(teleTargetRoutineType, address);
    }

    public void writeSummary(PrintStream printStream) {
        codeRegistry().writeSummary(printStream);
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

    private synchronized CodeRegistry codeRegistry() {
        if (codeRegistry == null) {
            codeRegistry = new CodeRegistry(vm());
        }
        return codeRegistry;
    }

    /**
     * Adds an entry to the code registry, indexed by code address, representing a body
     * of compiled (native) code about which little is known.
     *
     * @param teleCompiledNativeCode a body of native code
     */
    public void register(TeleCompiledNativeCode teleCompiledNativeCode) {
        codeRegistry().add(teleCompiledNativeCode);
    }

    /**
     * Adds a {@link MaxCompiledCode} entry to the code registry, indexed by code address. Should only be called from
     * a constructor of a {@link TeleTargetMethod} subclass.
     *
     * @param maxCompiledCode the compiled code whose {@linkplain MaxCompiledCode#memoryRegion() code
     *            region} is to be added to this registry
     * @throws IllegalArgumentException when the memory region of {@link MaxCompiledCode} overlaps one already in this registry.
     */
    public void register(TeleTargetMethod teleTargetMethod) {
        final TeleCompiledCodeRegion teleCompiledCodeRegion = findCompiledCodeRegion(teleTargetMethod.callEntryPoint());
        ProgramWarning.check(teleCompiledCodeRegion != null, "Can't locate code region for method");
        codeRegistry().add(new TeleCompiledMethod(vm(), teleTargetMethod, teleCompiledCodeRegion, teleCompiledCodeRegion == bootCodeRegion));
    }

}
