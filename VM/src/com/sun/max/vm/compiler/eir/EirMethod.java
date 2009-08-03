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
package com.sun.max.vm.compiler.eir;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class EirMethod extends AbstractIrMethod {

    public boolean isTemplate() {
        return classMethodActor().isTemplate();
    }

    public boolean isTrampoline() {
        return classMethodActor() instanceof TrampolineMethodActor;
    }

    private final EirABI abi;

    public EirABI abi() {
        return abi;
    }

    public EirMethod(ClassMethodActor classMethodActor, EirABI eirABI) {
        super(classMethodActor);
        abi = eirABI;
    }

    protected EirMethod(ClassMethodActor classMethodActor, EirABIsScheme eirABIsScheme) {
        super(classMethodActor);
        abi = eirABIsScheme.getABIFor(classMethodActor);
    }

    /**
     * Locations where this method retrieves its parameters.
     */
    private EirLocation[] parameterLocations;

    public EirLocation[] parameterLocations() {
        return parameterLocations;
    }

    public void setParameterLocations(EirLocation[] parameterLocations) {
        this.parameterLocations = parameterLocations;
    }

    /**
     * Locations where this method's callers set up the method's parameters.
     */
    private EirLocation[] argumentLocations;

    public EirLocation[] argumentLocations() {
        return argumentLocations;
    }

    public void setArgumentLocations(EirLocation[] argumentLocations) {
        this.argumentLocations = argumentLocations;
    }

    private EirLocation resultLocation;

    public EirLocation resultLocation() {
        return resultLocation;
    }

    protected void setResultLocation(EirLocation resultLocation) {
        this.resultLocation = resultLocation;
    }

    private int frameSize;

    /**
     * Gets the size of the stack frame used for the local variables in
     * the method being generated. The stack pointer is decremented/incremented
     * by this amount when entering/leaving the method.
     */
    public int frameSize() {
        assert isGenerated();
        return frameSize;
    }

    /**
     * @see #frameSize()
     */
    public void setFrameSize(int numberOfBytes) {
        frameSize = numberOfBytes;

        if (frameSize < Word.size()) {
            // To support deoptimization we need at least one word besides the return address on the stack
            // @see Deoptimizer
            frameSize = Word.size();
        }
    }

    public void emit(EirTargetEmitter emitter) {
        for (EirBlock block : blocks()) {
            block.emit(emitter);
        }
    }

    private IndexedSequence<EirBlock> blocks;

    public IndexedSequence<EirBlock> blocks() {
        return blocks;
    }

    private EirLiteralPool literalPool;

    public EirLiteralPool literalPool() {
        return literalPool;
    }

    public void setGenerated(IndexedSequence<EirBlock> blocks, EirLiteralPool literalPool, EirLocation[] parameterLocations,
                             final EirLocation resultLocation, int frameSize) {
        this.blocks = blocks;
        this.literalPool = literalPool;
        setParameterLocations(parameterLocations);
        setArgumentLocations(parameterLocations); // FIXME: this should be different for SPARC. An extra parameter is needed...
        setResultLocation(resultLocation);
        setFrameSize(frameSize);
    }

    public boolean isGenerated() {
        return blocks != null;
    }

    public String traceToString() {
        final CharArrayWriter charArrayWriter = new CharArrayWriter();
        final IndentWriter writer = new IndentWriter(charArrayWriter);
        writer.println("EirCompiledMethod: " + classMethodActor().holder() + "." + name() + "(" + ((parameterLocations() == null) ? null : Arrays.toString(parameterLocations(), ", ")) + ") -> " + resultLocation());
        if (isGenerated()) {
            writer.indent();
            for (EirBlock block : blocks) {
                block.printTo(writer);
            }

            if (!literalPool.referenceLiterals().isEmpty()) {
                writer.println("reference literals: ");
                writer.indent();
                for (EirLiteral literal : literalPool.referenceLiterals()) {
                    writer.println(literal.toString());
                }
                writer.outdent();
            }
            if (!literalPool.scalarLiterals().isEmpty()) {
                writer.println("scalar literals: ");
                writer.indent();
                for (EirLiteral literal : literalPool.scalarLiterals()) {
                    writer.println(literal.toString());
                }
                writer.outdent();
            }
        }
        return charArrayWriter.toString();
    }


    @Override
    public Class<? extends IrTraceObserver> irTraceObserverType() {
        return EirTraceObserver.class;
    }

    @Override
    public void cleanup() {
        for (EirBlock block : blocks) {
            block.cleanup();
        }
    }

    public void cleanupAfterEmitting() {
        for (EirBlock block : blocks) {
            block.cleanupAfterEmitting();
        }
    }

    public Kind resultKind() {
        return classMethodActor().resultKind();
    }
}
