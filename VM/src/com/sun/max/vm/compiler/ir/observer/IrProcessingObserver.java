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
package com.sun.max.vm.compiler.ir.observer;

import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.ir.*;

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
