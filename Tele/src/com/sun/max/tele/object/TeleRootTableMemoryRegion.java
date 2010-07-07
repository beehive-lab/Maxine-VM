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

import java.lang.management.*;

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * Access to an instance of {@link RootTableMemoryRegion} in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleRootTableMemoryRegion extends TeleRuntimeMemoryRegion{

    private static final int TRACE_VALUE = 2;

    long wordsUsed = 0;

    private final Object localStatsPrinter = new Object() {

        private long previousWordsUsedCount = 0;

        @Override
        public String toString() {
            final long wordsUsedCount = wordsUsed;
            final long newWordsUsedCount =  wordsUsedCount - previousWordsUsedCount;
            final StringBuilder msg = new StringBuilder();
            msg.append(", #wordsUsed=(").append(wordsUsedCount);
            msg.append(",new=").append(newWordsUsedCount).append(")");
            previousWordsUsedCount = wordsUsedCount;
            return msg.toString();
        }
    };

    public TeleRootTableMemoryRegion(TeleVM vm, Reference rootTableMemoryRegionReference) {
        super(vm, rootTableMemoryRegionReference);
    }

    @Override
    protected void updateObjectCache(StatsPrinter statsPrinter) {
        super.updateObjectCache(statsPrinter);
        try {
            wordsUsed = vm().teleFields().RootTableMemoryRegion_wordsUsed.readLong(getReference());
        } catch (DataIOError dataIOError) {
            // No update; data read failed for some reason other than VM availability
            ProgramWarning.message("TeleLinearAllocationMemoryRegion dataIOError:");
            dataIOError.printStackTrace();
            // TODO (mlvdv)  replace this with a more general mechanism for responding to VM unavailable
        }
        statsPrinter.addStat(localStatsPrinter);
    }

    @Override
    public MemoryUsage getUsage() {
        return new MemoryUsage(-1, vm().wordSize().toLong() * wordsUsed, getRegionSize().toLong(), -1);
    }

}
