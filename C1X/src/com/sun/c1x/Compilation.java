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

import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiOsrFrame;
import com.sun.c1x.ci.CiRuntime;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.graph.BlockMap;
import com.sun.c1x.ir.BlockBegin;

/**
 * The <code>Compilation</code> class encapsulates global information about the compilation
 * of a particular method, including a reference to the runtime, statistics about the compiled code,
 * etc.
 *
 * @author Ben L. Titzer
 */
public class Compilation {

    final CiRuntime _runtime;
    final CiMethod _method;
    final int _osrBCI;

    Object _hir; // TODO: define IR class
    int _maxSpills;
    Object _frameMap; // TODO: define FrameMap class
    Object _masm; // TODO: define MacroAssembler
    boolean _needsDebugInfo;
    boolean _hasExceptionHandlers;
    boolean _hasFpuCode;
    boolean _hasUnsafeAccess;
    Bailout _bailout;

    int _totalBlocks;
    int _totalBytecodes;

    /**
     * Creates a new compilation for the specified method and runtime.
     * @param runtime the runtime implementation
     * @param method the method to be compiled
     * @param osrBCI the bytecode index for on-stack replacement, if requested
     */
    public Compilation(CiRuntime runtime, CiMethod method, int osrBCI) {
        _runtime = runtime;
        _method = method;
        _osrBCI = osrBCI;
    }

    /**
     * Gets the root method being compiled.
     * @return the method being compiled
     */
    public CiMethod method() {
        return _method;
    }

    /**
     * Gets the runtime for this compilation.
     * @return the runtime
     */
    public CiRuntime runtime() {
        return _runtime;
    }

    /**
     * Records that this compilation has exception handlers.
     */
    public void setHasExceptionHandlers() {
        _hasExceptionHandlers = true;
    }

    /**
     * Checks whether this compilation is for an on-stack replacement.
     * @return <code>true</code> if this compilation is for an on-stack replacement
     */
    public boolean isOsrCompilation() {
        return _osrBCI >= 0;
    }

    /**
     * Gets the bytecode index for on-stack replacement, if this compilation is for an OSR.
     * @return the bytecode index
     */
    public int osrBCI() {
        return _osrBCI;
    }

    /**
     * Gets the frame which describes the layout of the OSR interpreter frame for this method.
     * @return the OSR frame
     */
    public CiOsrFrame getOsrFrame() {
        return _runtime.getOsrFrame(_method, _osrBCI);
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
        return _runtime.resolveType("java.lang.Throwable");
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
     * Records an inlining decision to successfully inline a method.
     * @param target the method inlined
     */
    public void recordInlining(CiMethod target) {
        // TODO: record inlining success
    }

    /**
     * Builds the block map for the specified method.
     * @param method the method for which to build the block map
     * @param osrBCI the OSR bytecode index; <code>-1</code> if this is not an OSR
     * @return the block map for the specified method
     */
    public BlockMap getBlockMap(CiMethod method, int osrBCI) {
        // XXX: cache the block map for methods that are compiled or inlined often
        BlockMap map = new BlockMap(method, _totalBlocks);
        boolean isOsrCompilation = false;
        if (osrBCI >= 0) {
            map.addEntrypoint(osrBCI, BlockBegin.BlockFlag.OsrEntry);
            isOsrCompilation = true;
        }
        if (!map.build(!isOsrCompilation && C1XOptions.ComputeStoresInLoops)) {
            throw new Bailout("build of BlockMap failed");
        }
        map.cleanup();
        _totalBlocks += map.numberOfBlocks();
        return map;
    }
}
