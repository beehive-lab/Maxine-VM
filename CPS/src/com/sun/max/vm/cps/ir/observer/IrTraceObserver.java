/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.observer;

import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.ir.*;

/**
 * This class implements an IR observer that prints a trace of a method's IR code before and after generation and
 * transformation.
 * <p>
 * To enable the IrTraceObserver, specify the following System properties:
 * <p>
 * <pre>
 *     -Dmax.ir.observers=IrTraceObserver
 * </pre>
 * <p>
 * To simplify tracing of all IR's for a particular method, the {@code "max.ir.trace"} property can be used as described
 * {@linkplain IrObserverConfiguration here}.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class IrTraceObserver extends IrObserverAdapter {

    protected static final IndentWriter out = IndentWriter.traceStreamWriter();

    private final int traceLevel;

    private final Class<? extends IrMethod> irMethodType;

    public static final String PROPERTY_TRACE_LEVEL = "max.ir.trace.level";

    protected static final int DEFAULT_TRACE_LEVEL = 3;

    public IrTraceObserver() {
        this(IrMethod.class);
    }

    public IrTraceObserver(Class<? extends IrMethod> irMethodType) {
        this.irMethodType = irMethodType;
        int traceLvl = 3;
        try {
            traceLvl = Integer.parseInt(System.getProperty(PROPERTY_TRACE_LEVEL, String.valueOf(traceLvl)));
        } catch (NumberFormatException e) {
            ProgramWarning.message("Value for system property \"" + PROPERTY_TRACE_LEVEL + "\" not a valid integer: " + System.getProperty(PROPERTY_TRACE_LEVEL));
        }
        this.traceLevel = traceLvl;
    }

    protected String traceString(IrMethod irMethod, String description) {
        return irMethod.getClass().getSimpleName() + "[" + irMethod.classMethodActor() + "] - " + description;
    }

    protected boolean hasLevel(int requiredLevel) {
        return traceLevel >= requiredLevel;
    }

    protected int allocationTraceLevel() {
        return DEFAULT_TRACE_LEVEL;
    }

    protected int afterGenerationTraceLevel() {
        return DEFAULT_TRACE_LEVEL;
    }

    protected int transformTraceLevel(Object transform) {
        return DEFAULT_TRACE_LEVEL;
    }

    @Override
    public void observeAllocation(IrMethod irMethod) {
        if (irMethodType.isAssignableFrom(irMethod.getClass())) {
            if (hasLevel(allocationTraceLevel())) {
                out.println(traceString(irMethod, "allocated"));
            }
        }
    }

    @Override
    public void observeBeforeGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        // For console tracing, the IR state before generation is typically not very interesting
    }

    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (irMethodType.isAssignableFrom(irMethod.getClass())) {
            if (hasLevel(afterGenerationTraceLevel())) {
                out.println(traceString(irMethod, "after generation"));
                out.println(irMethod.traceToString());
                out.flush();
            }
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethodType.isAssignableFrom(irMethod.getClass())) {
            if (hasLevel(transformTraceLevel(transform))) {
                out.println(traceString(irMethod, "before transformation: " + transform));
                out.println(irMethod.traceToString());
            }
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (irMethodType.isAssignableFrom(irMethod.getClass())) {
            if (hasLevel(transformTraceLevel(transform))) {
                out.println(traceString(irMethod, "after transformation: " + transform));
                out.println(irMethod.traceToString());
            }
        }
    }
}
