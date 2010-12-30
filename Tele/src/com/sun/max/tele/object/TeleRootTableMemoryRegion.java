/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.lang.management.*;

import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
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
            TeleWarning.message("TeleLinearAllocationMemoryRegion dataIOError:", dataIOError);
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
