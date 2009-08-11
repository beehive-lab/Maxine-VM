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
import java.util.*;

import com.sun.c1x.alloc.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;

/**
 * The <code>Compilation</code> class encapsulates global information about the compilation of a particular method,
 * including a reference to the runtime, statistics about the compiled code, etc.
 *
 * @author Ben L. Titzer
 */
public class C1XCompilation {

    public final Target target;
    public final CiRuntime runtime;
    public final CiMethod method;
    public final int osrBCI;

    int maxSpills;
    boolean needsDebugInfo;
    boolean hasExceptionHandlers;
    boolean hasUnsafeAccess;
    Bailout bailout;

    private Instruction currentInstruction;

    private FrameMap frameMap;
    private AbstractAssembler assembler;

    private IR hir;

    private Instruction lastInstructionPrinted; // Debugging only
    private CFGPrinter cfgPrinter;

    private CodeOffsets codeOffsets;
    private List<ExceptionInfo> exceptionInfoList;
    public C1XCompiler compiler;

    /**
     * Creates a new compilation for the specified method and runtime.
     *
     * @param target the target of the compilation, including architecture information
     * @param runtime the runtime implementation
     * @param method the method to be compiled
     * @param osrBCI the bytecode index for on-stack replacement, if requested
     */
    C1XCompilation(C1XCompiler compiler, Target target, CiRuntime runtime, CiMethod method, int osrBCI) {
        this.compiler = compiler;
        this.target = target;
        this.runtime = runtime;
        this.method = method;
        this.osrBCI = osrBCI;
    }

    /**
     * Creates a new compilation for the specified method and runtime.
     *
     * @param target the target of the compilation, including architecture information
     * @param runtime the runtime implementation
     * @param method the method to be compiled
     * @param targetMethod the target method
     */
    public C1XCompilation(C1XCompiler compiler, Target target, CiRuntime runtime, CiMethod method) {
        this(compiler, target, runtime, method, -1);
    }

    public IR hir() {
        return hir;
    }

    /**
     * Gets the bailout condition if this compilation failed.
     *
     * @return the bailout condition
     */
    public Bailout bailout() {
        return bailout;
    }

    /**
     * Gets the root method being compiled.
     *
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
     *
     * @return <code>true</code> if this compilation is for an on-stack replacement
     */
    public boolean isOsrCompilation() {
        return osrBCI >= 0;
    }

    /**
     * Gets the frame which describes the layout of the OSR interpreter frame for this method.
     *
     * @return the OSR frame
     */
    public CiOsrFrame getOsrFrame() {
        return runtime.getOsrFrame(method, osrBCI);
    }

    /**
     * Records an assumption made by this compilation that the specified type is a leaf class.
     *
     * @param type the type that is assumed to be a leaf class
     * @return <code>true</code> if the assumption was recorded and can be assumed; <code>false</code> otherwise
     */
    public boolean recordLeafTypeAssumption(CiType type) {
        return false;
    }

    /**
     * Records an assumption made by this compilation that the specified method is a leaf method.
     *
     * @param method the method that is assumed to be a leaf method
     * @return <code>true</code> if the assumption was recorded and can be assumed; <code>false</code> otherwise
     */
    public boolean recordLeafMethodAssumption(CiMethod method) {
        return false;
    }

    /**
     * Records an assumption that the specified type has no finalizable subclasses.
     *
     * @param receiverType the type that is assumed to have no finalizable subclasses
     * @return <code>true</code> if the assumption was recorded and can be assumed; <code>false</code> otherwise
     */
    public boolean recordNoFinalizableSubclassAssumption(CiType receiverType) {
        return false;
    }

    /**
     * Gets the <code>CiType</code> corresponding to <code>java.lang.Throwable</code>.
     *
     * @return the compiler interface type for Throwable
     */
    public CiType throwableType() {
        return runtime.resolveType("java.lang.Throwable");
    }

    /**
     * Records an inlining decision not to inline an inlinable method.
     *
     * @param target the method that was not inlined
     * @param reason a description of the reason why the method was not inlined
     */
    public void recordInliningFailure(CiMethod target, String reason) {
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
     * @param osrBCI the OSR bytecode index; <code>-1</code> if this is not an OSR
     * @return the block map for the specified method
     */
    public BlockMap getBlockMap(CiMethod method, int osrBCI) {
        // XXX: cache the block map for methods that are compiled or inlined often
        BlockMap map = new BlockMap(method, hir.numberOfBlocks());
        boolean isOsrCompilation = false;
        if (osrBCI >= 0) {
            map.addEntrypoint(osrBCI, BlockBegin.BlockFlag.OsrEntry);
            isOsrCompilation = true;
        }
        if (!map.build(!isOsrCompilation && C1XOptions.ComputeStoresInLoops)) {
            throw new Bailout("build of BlockMap failed for " + method);
        } else {
            if (C1XOptions.PrintCFGToFile) {
                CFGPrinter cfgPrinter = this.cfgPrinter();
                cfgPrinter.printCFG(method, map, method.codeSize(), "BlockListBuilder " + Util.format("%f %r %H.%n(%p)", method, true), false, false);
            }
        }
        map.cleanup();
        hir.incrementNumberOfBlocks(map.numberOfBlocks());
        return map;
    }

    /**
     * Returns the number of bytecodes inlined into the compilation.
     *
     * @return the number of bytecodes
     */
    public int totalInstructions() {
        return hir.totalInstructions();
    }

    /**
     * Updates the current instruction to a new value and returns the old one.
     *
     * @param instr
     *            the new current instruction
     * @return the old current instruction
     */
    public Instruction setCurrentInstruction(Instruction instr) {
        final Instruction previous = currentInstruction;
        currentInstruction = instr;
        return previous;
    }

    /**
     * Returns the current processed instruction. This method is used during HIR to LIR transformations.
     *
     * @return the current instruction
     */
    public Instruction currentInstruction() {
        return currentInstruction;
    }

    /**
     * Returns the frame map of this compilation.
     *
     * @return the frame map
     */
    public FrameMap frameMap() {
        return frameMap;
    }

    public void maybePrintCurrentInstruction() {
        if (currentInstruction != null && lastInstructionPrinted != currentInstruction) {
            lastInstructionPrinted = currentInstruction;
            currentInstruction.printLine();
        }
    }

    public CodeOffsets offsets() {
        if (codeOffsets == null) {
            codeOffsets = new CodeOffsets();
        }
        return codeOffsets;
    }

    public AbstractAssembler masm() {
        return assembler;
    }

    public void addExceptionHandlersForPco(int pcOffset, List<ExceptionHandler> exceptionHandlers) {
        if (C1XOptions.PrintExceptionHandlers && C1XOptions.Verbose) {
            TTY.println("  added exception scope for pco %d", pcOffset);
        }
        if (exceptionInfoList == null) {
            exceptionInfoList = new ArrayList<ExceptionInfo>();
        }
        exceptionInfoList.add(new ExceptionInfo(pcOffset, exceptionHandlers));
    }

    public DebugInformationRecorder debugInfoRecorder() {
        // TODO: Implement correctly, for now return skeleton class for code to work
        return new DebugInformationRecorder();
    }

    public boolean hasExceptionHandlers() {
        return hasExceptionHandlers;
    }

    public BlockBegin osrEntry() {
        throw Util.unimplemented();
    }

    public CiTargetMethod compile() {

        if (C1XOptions.PrintCompilation) {
            TTY.println();
            TTY.println("Compiling method: " + method.toString());
        }

        CiTargetMethod targetMethod = null;
        try {
            hir = new IR(this);
            hir.build();
            emitLIR();
            targetMethod = emitCode();
        } catch (Throwable t) {
            bailout = new Bailout("Unexpected exception while compiling: " + this.method(), t);
            throw bailout;
        }

        if (targetMethod != null) {
            targetMethod.totalInstructions = this.totalInstructions();
        }

        return targetMethod;
    }

    private void emitLIR() {
        if (C1XOptions.GenerateLIR) {
            frameMap = target.backend.newFrameMap(this, method, hir.topScope.numberOfLocks(), hir.topScope.maxStack());
            final LIRGenerator lirGenerator = target.backend.newLIRGenerator(this);
            for (BlockBegin begin : hir.linearScanOrder()) {
                lirGenerator.visitBlock(begin);
            }

            final RegisterAllocator registerAllocator = new LinearScan(this, hir, lirGenerator, frameMap());
            registerAllocator.allocate();

            CFGPrinter printer = cfgPrinter();
            if (printer != null) {
                printer.printCFG(hir.startBlock, "After generation of LIR", false, true);
            }
        }
    }

    private CiTargetMethod emitCode() {
        if (C1XOptions.GenerateLIR && C1XOptions.GenerateAssembly) {
            assembler = target.backend.newAssembler(compiler);
            final LIRAssembler lirAssembler = target.backend.newLIRAssembler(this);
            lirAssembler.emitCode(hir.linearScanOrder());

            // generate code or slow cases
            lirAssembler.emitSlowCaseStubs();

            // generate exception adapters
            lirAssembler.emitExceptionEntries(exceptionInfoList);

            lirAssembler.emitDeoptHandler();

            return assembler.finishTargetMethod(runtime, frameMap().framesize());
        }

        return null;
    }

    public CFGPrinter cfgPrinter() {
        if (C1XOptions.PrintCFGToFile && cfgPrinter == null) {
            OutputStream cfgFileStream = CFGPrinter.cfgFileStream();
            if (cfgFileStream != null) {
                cfgPrinter = new CFGPrinter(cfgFileStream);
                cfgPrinter.printCompilation(method);
            }
        }

        return cfgPrinter;
    }

    public boolean needsDebugInformation() {
        // TODO Check what to return here, for now do not collect debug information
        return false;
    }

    public void recordImplicitException(int offset, int offset2) {
        // TODO move to CiTargetMethod?

    }
}
