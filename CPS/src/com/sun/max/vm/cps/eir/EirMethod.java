/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir;

import java.io.*;

import com.sun.max.*;
import com.sun.max.io.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class EirMethod extends AbstractIrMethod {

    public final EirABI abi;
    private int frameSize;
    private EirLiteralPool literalPool;
    private EirBlock[] blocks;

    /**
     * Locations where this method retrieves its parameters.
     */
    private EirLocation[] parameterLocations;

    /**
     * Locations where this method's callers set up the method's parameters.
     */
    private EirLocation[] argumentLocations;

    /**
     * Location where this method writes its return value.
     */
    private EirLocation resultLocation;

    /**
     * The total size of the blocks allocated in the frame via the {@link StackAllocate} builtin.
     */
    private int stackBlocksSize;

    public boolean isTemplate() {
        return classMethodActor().isTemplate();
    }

    public EirMethod(ClassMethodActor classMethodActor, EirABI eirABI) {
        super(classMethodActor);
        abi = eirABI;
    }

    protected EirMethod(ClassMethodActor classMethodActor, EirABIsScheme eirABIsScheme) {
        this(classMethodActor, eirABIsScheme.getABIFor(classMethodActor));
    }

    public EirLocation[] parameterLocations() {
        return parameterLocations;
    }

    public void setParameterLocations(EirLocation[] parameterLocations) {
        this.parameterLocations = parameterLocations;
    }

    public EirLocation[] argumentLocations() {
        return argumentLocations;
    }

    public void setArgumentLocations(EirLocation[] argumentLocations) {
        this.argumentLocations = argumentLocations;
    }

    public EirLocation resultLocation() {
        return resultLocation;
    }

    protected void setResultLocation(EirLocation resultLocation) {
        this.resultLocation = resultLocation;
    }

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
    }

    /**
     * Gets the total size of the blocks allocated in the frame via the {@link StackAllocate} builtin.
     * The value returned by {@link #frameSize()} includes this amount.
     */
    public int stackBlocksSize() {
        return stackBlocksSize;
    }

    public void emit(EirTargetEmitter emitter) {
        for (EirBlock block : blocks()) {
            block.emit(emitter);
        }
    }

    public EirBlock[] blocks() {
        return blocks;
    }

    public EirLiteralPool literalPool() {
        return literalPool;
    }

    public void setGenerated(EirBlock[] blocks, EirLiteralPool literalPool, EirLocation[] parameterLocations,
                             final EirLocation resultLocation, int frameSize, int stackBlocksSize) {
        this.blocks = blocks;
        this.literalPool = literalPool;
        this.stackBlocksSize = stackBlocksSize;
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
        writer.println("EirCompiledMethod: " + classMethodActor().holder() + "." + name() + "(" + ((parameterLocations() == null) ? null : Utils.toString(parameterLocations(), ", ")) + ") -> " + resultLocation());
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
