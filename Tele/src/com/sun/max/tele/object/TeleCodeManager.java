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
import com.sun.max.vm.code.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for the singleton {@link CodeManager} in the VM.
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

    private TeleCodeRegion teleBootCodeRegion = null;

    /**
     * Surrogates for the runtime (dynamic) code region created by the {@link CodeManager} in the VM.
     * <br>
     * Assume that the region is created at startup, and that its identity doesn't change, just the
     * address when memory is allocated for it.
     */
    private TeleCodeRegion teleRuntimeCodeRegion = null;

    TeleCodeManager(TeleVM teleVM, Reference codeManagerReference) {
        super(teleVM, codeManagerReference);
    }

    /**
     * Lazy initialization; try to keep data reading out of constructor.
     */
    private void initialize() {
        if (teleBootCodeRegion == null) {
            Trace.begin(TRACE_VALUE, tracePrefix() + "initializing");
            final long startTimeMillis = System.currentTimeMillis();
            final Reference bootCodeRegionReference = vm().teleFields().Code_bootCodeRegion.readReference(vm());
            teleBootCodeRegion = (TeleCodeRegion) vm().makeTeleObject(bootCodeRegionReference);

            final Reference runtimeCodeRegionReference = vm().teleFields().CodeManager_runtimeCodeRegion.readReference(vm());
            teleRuntimeCodeRegion = (TeleCodeRegion) vm().makeTeleObject(runtimeCodeRegionReference);

            teleBootCodeRegion.refresh();
            teleRuntimeCodeRegion.refresh();

            Trace.end(TRACE_VALUE, tracePrefix() + "initializing, contains BootCodeRegion and RuntimeCodeRegion", startTimeMillis);
        }
    }

    @Override
    public void refresh() {
        // We aren't caching anything yet, other than the identity of the code regions (see above);
    }

    /**
     * @return surrogate for the special {@link CodeRegion} in the {@link BootImage} of the VM.
     */
    public TeleCodeRegion teleBootCodeRegion() {
        initialize();
        return teleBootCodeRegion;
    }

    /**
     * @return surrogates for the special Runtime {@link CodeRegion} of the VM.
     */
    public TeleCodeRegion teleRuntimeCodeRegion() {
        initialize();
        return teleRuntimeCodeRegion;
    }

}
