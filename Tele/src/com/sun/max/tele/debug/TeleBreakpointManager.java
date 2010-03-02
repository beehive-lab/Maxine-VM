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
package com.sun.max.tele.debug;

import java.io.*;

import com.sun.max.collect.*;
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

    // Thread-safe, immutable list.  Will be read many, many more times than they will change.
    private volatile IterableWithLength<MaxBreakpoint> breakpointCache = Sequence.Static.empty(MaxBreakpoint.class);

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

    public IterableWithLength<MaxBreakpoint> breakpoints() {
        return breakpointCache;
    }

    public void writeSummary(PrintStream printStream) {
        teleVM().teleProcess().targetBreakpointManager().writeSummaryToStream(printStream);
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
        final VariableSequence<MaxBreakpoint> newBreakpointsCache = new  VectorSequence<MaxBreakpoint>(targetBreakpointManager.clientBreakpoints());
        for (MaxBreakpoint breakpoint : bytecodeBreakpointManager.clientBreakpoints()) {
            newBreakpointsCache.append(breakpoint);
        }
        breakpointCache = newBreakpointsCache;
    }

}
