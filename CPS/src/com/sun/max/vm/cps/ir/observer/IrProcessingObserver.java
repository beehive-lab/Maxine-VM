/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * This class implements an IR observer that using indentation to show the relationship between the phases of IR
 * processing.
 * <p>
 * To enable the IrProcessingObserver, specify the following System properties:
 * <p>
 * <pre>
 *     -Dmax.ir.observers=IrProcessingObserver
 * </pre>
 *
 * @author Doug Simon
 */
public class IrProcessingObserver extends IrObserverAdapter {

    protected static final IndentWriter out = IndentWriter.traceStreamWriter();

    private final int traceLevel;

    public static final String PROPERTY_TRACE_LEVEL = "max.ir.trace.level";

    protected static final int DEFAULT_TRACE_LEVEL = 3;

    public IrProcessingObserver() {
        int traceLvl = 3;
        try {
            traceLvl = Integer.parseInt(System.getProperty(PROPERTY_TRACE_LEVEL, String.valueOf(traceLvl)));
        } catch (NumberFormatException e) {
            ProgramWarning.message("Value for system property \"" + PROPERTY_TRACE_LEVEL + "\" not a valid integer: " + System.getProperty(PROPERTY_TRACE_LEVEL));
        }
        this.traceLevel = traceLvl;
    }

    protected String methodName(IrMethod irMethod) {
        return irMethod.getClass().getSimpleName() + "[" + irMethod.name() + "]";
    }

    protected String traceString(IrMethod irMethod, String description) {
        return description + ": " + irMethod.name();
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
        if (hasLevel(allocationTraceLevel())) {
            out.println("ALLOCATED " + methodName(irMethod));
        }
    }

    @Override
    public void observeBeforeGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (hasLevel(afterGenerationTraceLevel())) {
            out.println(methodName(irMethod) + " BEGIN generation");
            out.indent();
            out.flush();
        }
    }

    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (hasLevel(afterGenerationTraceLevel())) {
            out.outdent();
            out.println(methodName(irMethod) + "   END generation");
            out.flush();
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (hasLevel(transformTraceLevel(transform))) {
            out.println(methodName(irMethod) + " BEGIN " + ": " + transform);
            out.indent();
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (hasLevel(transformTraceLevel(transform))) {
            out.outdent();
            out.println(methodName(irMethod) + "   END " + ": " + transform);
        }
    }
}
