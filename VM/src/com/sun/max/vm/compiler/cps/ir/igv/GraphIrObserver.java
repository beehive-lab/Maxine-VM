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
package com.sun.max.vm.compiler.cps.ir.igv;

import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cps.ir.*;
import com.sun.max.vm.compiler.cps.ir.observer.*;

/**
 * Transmits a graph of the IR to the Ideal Graph Visualizer.
 * Usage example:
 * -Dmax.ir.observer=com.sun.max.vm.compiler.ir.igv.GraphIrObserver
 * -Dmax.ir.igv.filename=graph.xml
 * The actual generation of the graphs for every method is done by the {@link GraphIrMethodVisitor}
 *
 * @author Thomas Wuerthinger
 */
public class GraphIrObserver extends IrObserverAdapter {

    /**
     * Default output file name. Can be overwritten using the system property max.ir.igv.filename
     */
    public static final String DEFAULT_FILENAME = "graph.xml";

    private final GraphWriter.Document document = GraphWriter.createDocument();
    private final VariableMapping<IrMethod, GraphWriter.Group> groupMapping = new ChainedHashMapping<IrMethod, GraphWriter.Group>();
    private int nestingLevel;

    private void begin(IrMethod irMethod) {
        assert !groupMapping.containsKey(irMethod);
        final GraphWriter.Group group = document.createGroup(irMethod.classMethodActor().format("%H.%n(%p)"));
        groupMapping.put(irMethod, group);
        final IrMethodVisitor methodVisitor = new InitializeGroupIrMethodVisitor(group);
        IrMethodVisitor.Static.visit(irMethod, methodVisitor);
        assert groupMapping.containsKey(irMethod);
    }

    private void process(IrMethod irMethod, Object context, String stateName) {
        assert groupMapping.containsKey(irMethod) : "Method must be in group mapping " + irMethod + ", " + irMethod.getClass();
        final GraphWriter.Group group = groupMapping.get(irMethod);
        final IrMethodVisitor methodVisitor = new GraphIrMethodVisitor(group, context, stateName);
        IrMethodVisitor.Static.visit(irMethod, methodVisitor);
    }

    private void setInputMethodObject(final GraphWriter.Group group, final IrMethod irMethod) {
        final byte[] bytecodes = irMethod.classMethodActor().codeAttribute().code();
        final BytecodeBlock bytecodeBlock = new BytecodeBlock(bytecodes, 0, bytecodes.length - 1);
        String bytecodesAsString = BytecodePrinter.toString(irMethod.classMethodActor().codeAttribute().constantPool, bytecodeBlock);
        bytecodesAsString = bytecodesAsString.replace(":", "");
        final String methodName = irMethod.classMethodActor().format("%H.%n(%p)");
        final String shortName = irMethod.classMethodActor().simpleName();
        group.createMethod(-1, methodName, shortName, bytecodesAsString);
    }

    private void finish(IrMethod irMethod) {
        assert groupMapping.containsKey(irMethod) : "Method must be in group mapping";

        final GraphWriter.Group group = groupMapping.get(irMethod);
        setInputMethodObject(group, irMethod);
        groupMapping.remove(irMethod);

        // Do not include groups with no graphs
        if (group.getGraphs().length() == 0) {
            document.removeGroup(group);
        }

        assert !groupMapping.containsKey(irMethod) : "Method must no longer be in group mapping";

    }

    private boolean methodOK(IrMethod method) {
        return !method.isNative();
    }

    /**
     * Captures the first state of the given method.
     * Must be called before {@link observeAfterGeneration} and {@link observerAfterTransformation} is called for the given method.
     */
    @Override
    public void observeBeforeGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (methodOK(irMethod)) {
            begin(irMethod);
        }
    }

    /**
     * Captures the final state of the given method. Can only be called after {@link observeBeforeGeneration} was called for the given method.
     */
    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (methodOK(irMethod)) {
            process(irMethod, null, "After Generation");
            finish(irMethod);
        }
    }

    private String nestingPrefix() {
        if (nestingLevel == 0) {
            return "";
        }
        final char[] prefix = new char[nestingLevel];
        Arrays.fill(prefix, '+');
        return new String(prefix);
    }

    /**
     * Captures the state of the given method before a transformation. Can only be called after
     * {@link #observeBeforeGeneration} and before {@link #observeAfterGeneration} was called for the given method.
     */
    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (methodOK(irMethod)) {
            process(irMethod, context, nestingPrefix() + "BEFORE: " + transform);
        }
        nestingLevel++;
    }

    /**
     * Captures the state of the given method after a transformation. Can only be called after
     * {@link #observeBeforeGeneration} and before {@link #observeAfterGeneration} was called for the given method.
     */
    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        nestingLevel--;
        if (methodOK(irMethod)) {
            process(irMethod, context, nestingPrefix() + "AFTER: " + transform);
        }
    }

    /**
     * Exports the traced graphs to a file using the XML format.
     */
    @Override
    public void finish() {
        final String fileName = DEFAULT_FILENAME;

        try {
            final FileWriter fileWriter = new FileWriter(fileName);
            final GraphWriter graphWriter = new GraphWriter(fileWriter);
            graphWriter.write(document);
            graphWriter.close();
            Trace.line(2, document.getGroups().length() + " groups of graphs written to file " + fileName);
        } catch (IOException e) {
            ProgramError.unexpected("Error while writing to file " + fileName, e);
        }
    }
}
