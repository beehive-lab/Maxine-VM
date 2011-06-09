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
package com.sun.max.tele.debug;

import java.io.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.method.CodeLocation.*;

/**
 * Access to breakpoint creation and management in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleBreakpointManager extends AbstractTeleVMHolder implements MaxBreakpointManager {

    private final TeleBytecodeBreakpoint.BytecodeBreakpointManager bytecodeBreakpointManager;
    private final TeleTargetBreakpoint.TargetBreakpointManager targetBreakpointManager;

    // Thread-safe, immutable list.  Will be read many, many more times than will change.
    private volatile List<MaxBreakpoint> breakpointCache = Collections.emptyList();

    public TeleBreakpointManager(TeleVM teleVM, TeleBytecodeBreakpoint.BytecodeBreakpointManager bytecodeBreakpointManager) {
        super(teleVM);
        this.bytecodeBreakpointManager = bytecodeBreakpointManager;
        this.targetBreakpointManager = teleVM.teleProcess().targetBreakpointManager();
        updateBreakpointCache();
        addListener(new MaxBreakpointListener() {

            public void breakpointsChanged() {
                updateBreakpointCache();
            }
        });
    }

    public void addListener(MaxBreakpointListener listener) {
        targetBreakpointManager.addListener(listener);
        bytecodeBreakpointManager.addListener(listener);
    }

    public void removeListener(MaxBreakpointListener listener) {
        targetBreakpointManager.removeListener(listener);
        bytecodeBreakpointManager.removeListener(listener);
    }

    public TeleBreakpoint makeBreakpoint(MaxCodeLocation maxCodeLocation) throws MaxVMBusyException {
        final CodeLocation codeLocation = (CodeLocation) maxCodeLocation;
        if (maxCodeLocation.hasAddress()) {
            return targetBreakpointManager.makeClientBreakpoint(codeLocation);
        }
        return bytecodeBreakpointManager.makeClientBreakpoint(codeLocation);
    }

    public  TeleBreakpoint findBreakpoint(MaxCodeLocation maxCodeLocation) {
        if (maxCodeLocation instanceof MachineCodeLocation) {
            final MachineCodeLocation compiledCodeLocation = (MachineCodeLocation) maxCodeLocation;
            return targetBreakpointManager.findClientBreakpoint(compiledCodeLocation);
        }
        final BytecodeLocation methodCodeLocation = (BytecodeLocation) maxCodeLocation;
        return bytecodeBreakpointManager.findClientBreakpoint(methodCodeLocation);
    }

    public List<MaxBreakpoint> breakpoints() {
        return breakpointCache;
    }

    public void writeSummary(PrintStream printStream) {
        vm().teleProcess().targetBreakpointManager().writeSummaryToStream(printStream);
        bytecodeBreakpointManager.writeSummaryToStream(printStream);
    }

    public TeleTargetBreakpoint makeTransientTargetBreakpoint(MaxCodeLocation maxCodeLocation) throws MaxVMBusyException {
        final CodeLocation codeLocation = (CodeLocation) maxCodeLocation;
        return targetBreakpointManager.makeTransientBreakpoint(codeLocation);
    }

    /**
     * Recomputes the immutable list cache of all client breakpoints.
     */
    private void updateBreakpointCache() {
        final List<MaxBreakpoint> newBreakpointsCache = new  ArrayList<MaxBreakpoint>(targetBreakpointManager.clientBreakpoints());
        for (MaxBreakpoint breakpoint : bytecodeBreakpointManager.clientBreakpoints()) {
            newBreakpointsCache.add(breakpoint);
        }
        breakpointCache = Collections.unmodifiableList(newBreakpointsCache);
    }

}
