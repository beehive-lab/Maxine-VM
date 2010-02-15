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
public class TeleBreakpointFactory extends AbstractTeleVMHolder implements MaxBreakpointFactory {

    private final TeleBytecodeBreakpoint.Factory bytecodeBreakpointFactory;
    private final TeleTargetBreakpoint.Factory targetBreakpointFactory;

    // Thread-safe, immutable list.  Will be read many, many more times than they will change.
    private volatile IterableWithLength<MaxBreakpoint> breakpointCache = Sequence.Static.empty(MaxBreakpoint.class);

    public TeleBreakpointFactory(TeleVM teleVM, TeleBytecodeBreakpoint.Factory bytecodeBreakpointFactory) {
        super(teleVM);
        this.bytecodeBreakpointFactory = bytecodeBreakpointFactory;
        this.targetBreakpointFactory = teleVM.teleProcess().targetBreakpointFactory();
        updateBreakpointCache();
        addListener(new MaxBreakpointListener() {

            public void breakpointsChanged() {
                updateBreakpointCache();
            }
        });
    }

    public void addListener(MaxBreakpointListener listener) {
        targetBreakpointFactory.addListener(listener);
        bytecodeBreakpointFactory.addListener(listener);
    }

    public void removeListener(MaxBreakpointListener listener) {
        targetBreakpointFactory.removeListener(listener);
        bytecodeBreakpointFactory.removeListener(listener);
    }

    public TeleBreakpoint makeBreakpoint(MaxCodeLocation maxCodeLocation) throws MaxVMBusyException {
        final CodeLocation codeLocation = (CodeLocation) maxCodeLocation;
        if (maxCodeLocation.hasAddress()) {
            return targetBreakpointFactory.makeClientBreakpoint(codeLocation);
        }
        return bytecodeBreakpointFactory.makeClientBreakpoint(codeLocation);
    }

    public  TeleBreakpoint findBreakpoint(MaxCodeLocation maxCodeLocation) {
        if (maxCodeLocation instanceof MachineCodeLocation) {
            final MachineCodeLocation compiledCodeLocation = (MachineCodeLocation) maxCodeLocation;
            return targetBreakpointFactory.findClientBreakpoint(compiledCodeLocation);
        }
        final BytecodeLocation methodCodeLocation = (BytecodeLocation) maxCodeLocation;
        return bytecodeBreakpointFactory.findClientBreakpoint(methodCodeLocation);
    }

    public IterableWithLength<MaxBreakpoint> breakpoints() {
        return breakpointCache;
    }

    public void writeSummaryToStream(PrintStream printStream) {
        teleVM().teleProcess().targetBreakpointFactory().writeSummaryToStream(printStream);
        bytecodeBreakpointFactory.writeSummaryToStream(printStream);
    }

    public TeleTargetBreakpoint makeTransientTargetBreakpoint(MaxCodeLocation maxCodeLocation) throws MaxVMBusyException {
        final CodeLocation codeLocation = (CodeLocation) maxCodeLocation;
        return targetBreakpointFactory.makeTransientBreakpoint(codeLocation);
    }

    /**
     * Recomputes the immutable list cache of all client breakpoints.
     */
    private void updateBreakpointCache() {
        final VariableSequence<MaxBreakpoint> newBreakpointsCache = new  VectorSequence<MaxBreakpoint>(targetBreakpointFactory.clientBreakpoints());
        for (MaxBreakpoint breakpoint : bytecodeBreakpointFactory.clientBreakpoints()) {
            newBreakpointsCache.append(breakpoint);
        }
        breakpointCache = newBreakpointsCache;
    }

}
