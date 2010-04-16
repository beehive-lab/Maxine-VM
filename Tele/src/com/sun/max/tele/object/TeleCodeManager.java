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
package com.sun.max.tele.object;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for the singleton {@link CodeManager} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public final class TeleCodeManager extends TeleTupleObject {

    private static final int TRACE_VALUE = 1;

    @Override
    protected String  tracePrefix() {
        return "[TeleCodeManager] ";
    }

    private static TeleCodeManager teleCodeManager;

    public static TeleCodeManager make(TeleVM teleVM) {
        if (teleCodeManager ==  null) {
            teleCodeManager = (TeleCodeManager) teleVM.makeTeleObject(teleVM.teleFields().Code_codeManager.readReference(teleVM));
            teleCodeManager.initialize();
        }
        return teleCodeManager;
    }

    private TeleCodeRegion teleBootCodeRegion = null;

    /**
     * Surrogates for each of the code regions created by the {@link CodeManager} in the {@link TeleVM}.
     * Assume that the regions are all created at startup, and that their identity doesn't change, just their
     * address as they have memory allocated for them.
     */
    private TeleCodeRegion teleRuntimeCodeRegion = null;

    TeleCodeManager(TeleVM teleVM, Reference codeManagerReference) {
        super(teleVM, codeManagerReference);
    }

    /**
     * Lazy initialization; try to keep data reading out of constructor.
     */
    private void initialize() {
        Trace.begin(TRACE_VALUE, tracePrefix() + "initializing");
        final long startTimeMillis = System.currentTimeMillis();
        final Reference bootCodeRegionReference = teleVM().teleFields().Code_bootCodeRegion.readReference(teleVM());
        teleBootCodeRegion = (TeleCodeRegion) teleVM().makeTeleObject(bootCodeRegionReference);

        final Reference runtimeCodeRegionReference = teleVM().teleFields().CodeManager_runtimeCodeRegion.readReference(teleVM());
        teleRuntimeCodeRegion = (TeleCodeRegion) teleVM().makeTeleObject(runtimeCodeRegionReference);

        teleBootCodeRegion.refresh(0);
        teleRuntimeCodeRegion.refresh(0);

        // Pre-load the class method actors for all target methods in the boot heap.
        // This just shifts the perceptible delay when bringing up the "View target code..." dialog
        // into the general Inspector start up delay.
        for (TeleTargetMethod teleTargetMethod : teleBootCodeRegion.teleTargetMethods()) {
            teleTargetMethod.classMethodActor();
        }

        Trace.end(TRACE_VALUE, tracePrefix() + "initializing, contains BootCodeRegion and RuntimeCodeRegion", startTimeMillis);
    }

    @Override
    public void refresh(long processEpoch) {
        // We aren't caching anything yet, other than the identity of the code regions (see above);
    }

    /**
     * @return surrogate for the special {@link CodeRegion} in the {@link BootImage} of the {@link TeleVM}.
     */
    public TeleCodeRegion teleBootCodeRegion() {
        return teleBootCodeRegion;
    }

    /**
     * @return surrogates for the special Runtime {@link CodeRegion} of the {@link TeleVM}.
     */
    public TeleCodeRegion teleRuntimeCodeRegion() {
        return teleRuntimeCodeRegion;
    }

    /**
     * @return the allocated {@link CodeRegion} in the {@link TeleVM} that contains the address,
     * possibly the boot code region; null if none.
     */
    public TeleCodeRegion findCodeRegion(Address address) {
        if (teleBootCodeRegion.contains(address)) {
            return teleBootCodeRegion;
        }
        if (teleRuntimeCodeRegion.contains(address)) {
            return teleRuntimeCodeRegion;
        }
        return null;
    }

}
