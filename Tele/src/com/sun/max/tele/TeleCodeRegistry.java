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
package com.sun.max.tele;

import java.io.*;

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;

/**
 * Manages a cache of information about target routines (methods and native routines) in the {@link CodeManager} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public class TeleCodeRegistry extends AbstractTeleVMHolder {

    public TeleCodeRegistry(TeleVM teleVM) {
        super(teleVM);
        Trace.begin(1, tracePrefix() + " initializing");
        final long startTimeMillis = System.currentTimeMillis();
        TeleObject.copyStaticFields(teleVM, JniFunctionWrapper.class);
        Trace.end(1, tracePrefix() + " initializing", startTimeMillis);
    }

    private final SortedMemoryRegionList<TargetCodeRegion> targetCodeRegions = new SortedMemoryRegionList<TargetCodeRegion>();

    /**
     * Adds a {@link TeleTargetRoutine} entry to the code registry, indexed by code address. Should only be called from
     * a constructor of a {@link TeleTargetRoutine} subclass.
     *
     * @param teleTargetRoutine the tele target routine whose {@linkplain TeleTargetRoutine#targetCodeRegion() code
     *            region} is to be added to this registry
     * @throws IllegalArgumentException when the memory region of {@code teleTargetRoutine} overlaps one already in this registry.
     */
    public synchronized void add(TeleTargetRoutine teleTargetRoutine) {
        targetCodeRegions.add(teleTargetRoutine.targetCodeRegion());
    }

    /**
     * Gets the TeleTargetRoutine in this registry that contains a given address inm the {@link TeleVM}.
     *
     * @param <TeleTargetRoutine_Type> the type of the requested TeleTargetRoutine
     * @param teleTargetRoutineType the {@link Class} instance representing {@code TeleTargetRoutine_Type}
     * @param address the look up address
     * @return the tele target routine of type {@code TeleTargetRoutine_Type} in this registry that contains {@code
     *         address} or null if no such tele target routine of the requested type exists
     */
    public synchronized <TeleTargetRoutine_Type extends TeleTargetRoutine> TeleTargetRoutine_Type get(Class<TeleTargetRoutine_Type> teleTargetRoutineType, Address address) {
        final TargetCodeRegion targetCodeRegion = targetCodeRegions.find(address);
        if (targetCodeRegion != null) {
            final TeleTargetRoutine teleTargetRoutine = targetCodeRegion.teleTargetRoutine();
            if (teleTargetRoutineType.isInstance(teleTargetRoutine)) {
                return teleTargetRoutineType.cast(teleTargetRoutine);
            }
        }
        return null;
    }

    /**
     * All local {@link TargetCodeRegion} objects representing all known Java {@link TargetMethod}s and native routines
     * in target VM, arranged for lookup by address.
     */
    public Iterable<TargetCodeRegion> targetCodeRegions() {
        return targetCodeRegions;
    }

    public void writeSummaryToStream(PrintStream printStream) {
        Address lastEndAddress = null;
        for (TargetCodeRegion targetCodeRegion : targetCodeRegions) {
            final TeleTargetRoutine teleTargetRoutine = targetCodeRegion.teleTargetRoutine();
            final String name = teleTargetRoutine.teleRoutine().getUniqueName();
            if (lastEndAddress != null && !lastEndAddress.equals(targetCodeRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + targetCodeRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = targetCodeRegion.end();
            printStream.println(targetCodeRegion.start().toHexString() + "--" + targetCodeRegion.end().minus(1).toHexString() + ":  " + name);

        }
    }
}
