/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x;

import java.io.*;

import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiOsrFrame;
import com.sun.c1x.ci.CiRuntime;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.graph.BlockMap;
import com.sun.c1x.graph.GraphBuilder;
import com.sun.c1x.ir.BlockBegin;
import com.sun.c1x.ir.IRScope;
import com.sun.c1x.util.*;
import com.sun.c1x.target.Target;

/**
 * The <code>Compilation</code> class encapsulates global information about the compilation
 * of a particular method, including a reference to the runtime, statistics about the compiled code,
 * etc.
 *
 * @author Ben L. Titzer
 */
public class C1XCompilation {

    public final Target target;
    public final CiRuntime runtime;
    public final CiMethod method;
    final int osrBCI;

    BlockBegin start;
    int maxSpills;
    boolean needsDebugInfo;
    boolean hasExceptionHandlers;
    boolean hasFpuCode;
    boolean hasUnsafeAccess;
    Bailout bailout;

    int totalBlocks = 1;
    int totalInstructions;

    /**
     * Creates a new compilation for the specified method and runtime.
     * @param target the target of the compilation, including architecture information
     * @param runtime the runtime implementation
     * @param method the method to be compiled
     * @param osrBCI the bytecode index for on-stack replacement, if requested
     */
    public C1XCompilation(Target target, CiRuntime runtime, CiMethod method, int osrBCI) {
        this.target = target;
        this.runtime = runtime;
        this.method = method;
        this.osrBCI = osrBCI;
    }

    /**
     * Creates a new compilation for the specified method and runtime.
     * @param target the target of the compilation, including architecture information
     * @param runtime the runtime implementation
     * @param method the method to be compiled
     */
    public C1XCompilation(Target target, CiRuntime runtime, CiMethod method) {
        this.target = target;
        this.runtime = runtime;
        this.method = method;
        this.osrBCI = -1;
    }

    /**
     * Performs the compilation, producing the start block.
     */
    public BlockBegin startBlock() {
        try {
            if (start == null && bailout == null) {
                CFGPrinter cfgPrinter = null;
                if (C1XOptions.PrintCFGToFile) {
                    OutputStream cfgFileStream = CFGPrinter.cfgFileStream();
                    if (cfgFileStream != null) {
                        cfgPrinter = new CFGPrinter(cfgFileStream);
                        cfgPrinter.printCompilation(method());
                    }
                }

                final GraphBuilder builder = new GraphBuilder(this, new IRScope(this, null, 0, method, osrBCI));
                start = builder.start();
                totalInstructions = builder.instructionCount();

                if (C1XOptions.PrintCFGToFile && cfgPrinter != null) {
                    cfgPrinter.printCFG(start, "After Generation of HIR", true, false);
                }
            }
        } catch (Bailout b) {
            bailout = b;
        } catch (Throwable t) {
            bailout = new Bailout("Unexpected exception while compiling: " + this.method(), t);
        }
        return start;
    }

    /**
     * Gets the bailout condition if this compilation failed.
     * @return the bailout condition
     */
    public Bailout bailout() {
        return bailout;
    }

    /**
     * Gets the root method being compiled.
     * @return the method being compiled
     */
    public CiMethod method() {
        return method;
    }

    /**
     * Records that this compilation has exception handlers.
     */
    public void setHasExceptionHandlers() {
        hasExceptionHandlers = true;
    }

    /**
     * Checks whether this compilation is for an on-stack replacement.
     * @return <code>true</code> if this compilation is for an on-stack replacement
     */
    public boolean isOsrCompilation() {
        return osrBCI >= 0;
    }

    /**
     * Gets the bytecode index for on-stack replacement, if this compilation is for an OSR.
     * @return the bytecode index
     */
    public int osrBCI() {
        return osrBCI;
    }

    /**
     * Gets the frame which describes the layout of the OSR interpreter frame for this method.
     * @return the OSR frame
     */
    public CiOsrFrame getOsrFrame() {
        return runtime.getOsrFrame(method, osrBCI);
    }

    /**
     * Records an assumption made by this compilation that the specified type is a leaf class.
     * @param type the type that is assumed to be a leaf class
     * @return <code>true</code> if the assumption was recorded and can be assumed; <code>false</code> otherwise
     */
    public boolean recordLeafTypeAssumption(CiType type) {
        return false;
    }

    /**
     * Records an assumption made by this compilation that the specified method is a leaf method.
     * @param method the method that is assumed to be a leaf method
     * @return <code>true</code> if the assumption was recorded and can be assumed; <code>false</code> otherwise
     */
    public boolean recordLeafMethodAssumption(CiMethod method) {
        return false;
    }

    /**
     * Records an assumption that the specified type has no finalizable subclasses.
     * @param receiverType the type that is assumed to have no finalizable subclasses
     * @return <code>true</code> if the assumption was recorded and can be assumed; <code>false</code> otherwise
     */
    public boolean recordNoFinalizableSubclassAssumption(CiType receiverType) {
        return false;
    }

    /**
     * Gets the <code>CiType</code> corresponding to <code>java.lang.Throwable</code>.
     * @return the compiler interface type for Throwable
     */
    public CiType throwableType() {
        return runtime.resolveType("java.lang.Throwable");
    }

    /**
     * Records an inlining decision not to inline an inlinable method.
     * @param target the method that was not inlined
     * @param reason a description of the reason why the method was not inlined
     */
    public void recordInliningFailure(CiMethod target, String reason) {
        // TODO: record inlining failure
    }

    /**
     * Converts this compilation to a string.
     * @return a string representation of this compilation
     */
    @Override
    public String toString() {
        if (isOsrCompilation()) {
            return "osr-compile @ " + osrBCI + ": " + method;
        }
        return "compile: " + method;
    }

    /**
     * Builds the block map for the specified method.
     * @param method the method for which to build the block map
     * @param osrBCI the OSR bytecode index; <code>-1</code> if this is not an OSR
     * @return the block map for the specified method
     */
    public BlockMap getBlockMap(CiMethod method, int osrBCI) {
        // XXX: cache the block map for methods that are compiled or inlined often
        BlockMap map = new BlockMap(method, totalBlocks);
        boolean isOsrCompilation = false;
        if (osrBCI >= 0) {
            map.addEntrypoint(osrBCI, BlockBegin.BlockFlag.OsrEntry);
            isOsrCompilation = true;
        }
        if (!map.build(!isOsrCompilation && C1XOptions.ComputeStoresInLoops)) {
            throw new Bailout("build of BlockMap failed for " + method);
        } else {
            if (C1XOptions.PrintCFGToFile) {
                OutputStream cfgFileStream = CFGPrinter.cfgFileStream();
                if (cfgFileStream != null) {
                    CFGPrinter cfgPrinter = new CFGPrinter(cfgFileStream);
                    cfgPrinter.printCFG(map, method.codeSize(), "BlockListBuilder " + Util.format("%f %r %H.%n(%p)", method, true), false, false);
                }
            }
        }
        map.cleanup();
        totalBlocks += map.numberOfBlocks();
        return map;
    }

    /**
     * Returns the number of bytecodes inlined into the compilation.
     * @return the number of bytecodes
     */
    public int totalInstructions() {
        return totalInstructions;
    }

    public int nextBlockNumber() {
        return totalBlocks++;
    }
}
