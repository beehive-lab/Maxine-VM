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

import com.sun.c1x.alloc.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * This class encapsulates global information about the compilation of a particular method,
 * including a reference to the runtime, statistics about the compiled code, etc.
 *
 * @author Ben L. Titzer
 */
public class C1XCompilation {

    private static ThreadLocal<C1XCompilation> currentCompilation = new ThreadLocal<C1XCompilation>();

    public final C1XCompiler compiler;
    public final CiTarget target;
    public final RiRuntime runtime;
    public final RiMethod method;
    public final CiStatistics stats;
    public final int osrBCI;

    private boolean hasExceptionHandlers;
    private int nextID = 1;

    private FrameMap frameMap;
    private AbstractAssembler assembler;

    private IR hir;

    /**
     * Object used to generate the trace output that can be fed to the
     * <a href="https://c1visualizer.dev.java.net/">C1 Visualizer</a>.
     */
    private final CFGPrinter cfgPrinter;

    /**
     * Buffer that {@link #cfgPrinter} writes to. Using a buffer allows writing to the
     * relevant {@linkplain CFGPrinter#cfgFileStream() file} to be serialized so that
     * traces for independent compilations are not interleaved.
     */
    private ByteArrayOutputStream cfgPrinterBuffer;

    /**
     * Creates a new compilation for the specified method and runtime.
     *
     * @param compiler the compiler
     * @param target the target of the compilation, including architecture information
     * @param runtime the runtime implementation
     * @param method the method to be compiled
     * @param osrBCI the bytecode index for on-stack replacement, if requested
     */
    C1XCompilation(C1XCompiler compiler, CiTarget target, RiRuntime runtime, RiMethod method, int osrBCI) {
        this.compiler = compiler;
        this.target = target;
        this.runtime = runtime;
        this.method = method;
        this.osrBCI = osrBCI;
        this.stats = new CiStatistics();

        CFGPrinter cfgPrinter = null;
        if (C1XOptions.PrintCFGToFile && method != null && TTY.Filter.matches(C1XOptions.PrintFilter, method)) {
            cfgPrinterBuffer = new ByteArrayOutputStream();
            cfgPrinter = new CFGPrinter(cfgPrinterBuffer, target);
            cfgPrinter.printCompilation(method);
        }
        this.cfgPrinter = cfgPrinter;
    }

    /**
     * Creates a new compilation for the specified method and runtime.
     *
     * @param compiler the compiler
     * @param target the target of the compilation, including architecture information
     * @param runtime the runtime implementation
     * @param method the method to be compiled
     */
    public C1XCompilation(C1XCompiler compiler, CiTarget target, RiRuntime runtime, RiMethod method) {
        this(compiler, target, runtime, method, -1);
    }

    public IR hir() {
        return hir;
    }

    /**
     * Records that this compilation has exception handlers.
     */
    public void setHasExceptionHandlers() {
        hasExceptionHandlers = true;
    }

    /**
     * Checks whether this compilation is for an on-stack replacement.
     *
     * @return {@code true} if this compilation is for an on-stack replacement
     */
    public boolean isOsrCompilation() {
        return osrBCI >= 0;
    }

    /**
     * Gets the frame which describes the layout of the OSR interpreter frame for this method.
     *
     * @return the OSR frame
     */
    public RiOsrFrame getOsrFrame() {
        return runtime.getOsrFrame(method, osrBCI);
    }

    /**
     * Records an assumption made by this compilation that the specified type is a leaf class.
     *
     * @param type the type that is assumed to be a leaf class
     * @return {@code true} if the assumption was recorded and can be assumed; {@code false} otherwise
     */
    public boolean recordLeafTypeAssumption(RiType type) {
        return false;
    }

    /**
     * Records an assumption made by this compilation that the specified method is a leaf method.
     *
     * @param method the method that is assumed to be a leaf method
     * @return {@code true} if the assumption was recorded and can be assumed; {@code false} otherwise
     */
    public boolean recordLeafMethodAssumption(RiMethod method) {
        return false;
    }

    /**
     * Records an assumption that the specified type has no finalizable subclasses.
     *
     * @param receiverType the type that is assumed to have no finalizable subclasses
     * @return {@code true} if the assumption was recorded and can be assumed; {@code false} otherwise
     */
    public boolean recordNoFinalizableSubclassAssumption(RiType receiverType) {
        return false;
    }

    /**
     * Gets the {@code RiType} corresponding to {@code java.lang.Throwable}.
     *
     * @return the compiler interface type for {@link Throwable}
     */
    public RiType throwableType() {
        return runtime.getRiType(Throwable.class);
    }

    /**
     * Records an inlining decision not to inline an inlinable method.
     *
     * @param target the method that was not inlined
     * @param reason a description of the reason why the method was not inlined
     */
    public void recordInliningFailure(RiMethod target, String reason) {
        // TODO: record inlining failure
    }

    /**
     * Converts this compilation to a string.
     *
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
     *
     * @param method the method for which to build the block map
     * @param osrBCI the OSR bytecode index; {@code -1} if this is not an OSR
     * @return the block map for the specified method
     */
    public BlockMap getBlockMap(RiMethod method, int osrBCI) {
        // PERF: cache the block map for methods that are compiled or inlined often
        BlockMap map = new BlockMap(method, hir.numberOfBlocks());
        boolean isOsrCompilation = false;
        if (osrBCI >= 0) {
            map.addEntrypoint(osrBCI, BlockBegin.BlockFlag.OsrEntry);
            isOsrCompilation = true;
        }
        if (!map.build(!isOsrCompilation && C1XOptions.PhiLoopStores)) {
            throw new CiBailout("build of BlockMap failed for " + method);
        } else {
            if (cfgPrinter() != null) {
                cfgPrinter().printCFG(method, map, method.code().length, "BlockListBuilder " + CiUtil.format("%f %r %H.%n(%p)", method, true), false, false);
            }
        }
        map.cleanup();
        stats.byteCount += map.numberOfBytes();
        stats.blockCount += map.numberOfBlocks();
        return map;
    }

    /**
     * Returns the frame map of this compilation.
     * @return the frame map
     */
    public FrameMap frameMap() {
        return frameMap;
    }

    public AbstractAssembler masm() {
        if (assembler == null) {
            assembler = compiler.backend.newAssembler();
            assembler.setFrameSize(frameMap.frameSize());
        }
        return assembler;
    }

    public boolean hasExceptionHandlers() {
        return hasExceptionHandlers;
    }

    public CiResult compile() {
        CiTargetMethod targetMethod;
        TTY.Filter filter = new TTY.Filter(C1XOptions.PrintFilter, method);
        try {

            emitHIR();
            emitLIR();
            targetMethod = emitCode();

            if (C1XOptions.PrintMetrics) {
                C1XMetrics.BytecodesCompiled += method.code().length;
            }
        } catch (CiBailout b) {
            return new CiResult(null, b, stats);
        } catch (Throwable t) {
            return new CiResult(null, new CiBailout("Exception while compiling: " + method, t), stats);
        } finally {
            filter.remove();
            flushCfgPrinterToFile();
        }

        return new CiResult(targetMethod, null, stats);
    }

    private void flushCfgPrinterToFile() {
        if (cfgPrinter != null) {
            cfgPrinter.flush();
            OutputStream cfgFileStream = CFGPrinter.cfgFileStream();
            if (cfgFileStream != null) {
                synchronized (cfgFileStream) {
                    try {
                        cfgFileStream.write(cfgPrinterBuffer.toByteArray());
                    } catch (IOException e) {
                        TTY.println("WARNING: Error writing CFGPrinter output for %s to disk: %s", method, e);
                    }
                }
            }
        }
    }

    public IR emitHIR() {
        setCurrent(this);

        if (C1XOptions.PrintCompilation) {
            TTY.println();
            TTY.println("Compiling method: " + method.toString());
        }
        hir = new IR(this);
        hir.build();
        return hir;
    }

    public void initFrameMap(int numberOfLocks) {
        frameMap = this.compiler.backend.newFrameMap(method, numberOfLocks);
    }

    private void emitLIR() {
        if (C1XOptions.GenLIR) {
            if (C1XOptions.PrintTimers) {
                C1XTimers.LIR_CREATE.start();
            }

            initFrameMap(hir.topScope.numberOfLocks());

            final LIRGenerator lirGenerator = compiler.backend.newLIRGenerator(this);
            for (BlockBegin begin : hir.linearScanOrder()) {
                lirGenerator.doBlock(begin);
            }

            if (C1XOptions.PrintTimers) {
                C1XTimers.LIR_CREATE.stop();
            }

            new LinearScan(this, hir, lirGenerator, frameMap()).allocate();
        }
    }

    private CiTargetMethod emitCode() {
        if (C1XOptions.GenLIR && C1XOptions.GenCode) {
            final LIRAssembler lirAssembler = compiler.backend.newLIRAssembler(this);
            lirAssembler.emitCode(hir.linearScanOrder());

            // generate code or slow cases
            lirAssembler.emitLocalStubs();

            // generate exception adapters
            lirAssembler.emitExceptionEntries();

            CiTargetMethod targetMethod = masm().finishTargetMethod(method, runtime, -1);

            if (cfgPrinter() != null) {
                cfgPrinter().printCFG(hir.startBlock, "After code generation", false, true);
                cfgPrinter().printMachineCode(runtime.disassemble(targetMethod));
            }

            if (C1XOptions.PrintTimers) {
                C1XTimers.CODE_CREATE.stop();
            }
            return targetMethod;
        }

        return null;
    }

    public CFGPrinter cfgPrinter() {
        return cfgPrinter;
    }

    public boolean needsDebugInformation() {
        return false;
    }

    public int nextID() {
        return nextID++;
    }

    public static C1XCompilation current() {
        return currentCompilation.get();
    }

    private static void setCurrent(C1XCompilation compilation) {
        currentCompilation.set(compilation);
    }
}
