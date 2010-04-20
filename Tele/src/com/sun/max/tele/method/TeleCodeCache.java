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

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.reference.*;

/**
 * Access to compiled code in the VM.
 * <br>
 * Much of the important state
 * about the code cache is contained in a singleton heap object, an instance
 * of {@link CodeManger}.
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
        final Reference nameReference = vm().teleFields().Code_CODE_BOOT_NAME.readReference(vm());
        bootCodeRegionName = vm().getString(nameReference);
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

    public MaxCompiledCodeRegion findCompiledCodeRegion(Address address) {
        for (MaxCompiledCodeRegion region : compiledCodeRegions) {
            if (region.memoryRegion().contains(address)) {
                return region;
            }
        }
        return null;
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

}
