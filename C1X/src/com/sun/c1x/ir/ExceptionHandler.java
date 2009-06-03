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
package com.sun.c1x.ir;

import com.sun.c1x.ci.CiExceptionHandler;
import com.sun.c1x.lir.LIRInstr;

import java.util.List;

/**
 * The <code>ExceptionHandler</code> class represents an exception handler for a Java bytecode method.
 *
 * @author Ben L. Titzer
 */
public class ExceptionHandler {
    private final CiExceptionHandler _handler;
    private BlockBegin _entryBlock;
    private List<LIRInstr> _entryCode;
    private int _entryPCO;
    private int _phiOperand;
    private int _scopeCount;

    public ExceptionHandler(CiExceptionHandler handler) {
        _handler = handler;
        _entryPCO = -1;
        _phiOperand = -1;
        _scopeCount = -1;
    }

    public ExceptionHandler(ExceptionHandler other) {
        _handler = other._handler;
        _entryBlock = other._entryBlock;
        _entryCode = other._entryCode;
        _entryPCO = other._entryPCO;
        _phiOperand = other._phiOperand;
        _scopeCount = other._scopeCount;
    }

    /**
     * Gets the compiler interface object that describes this exception handler,
     * including the bytecode ranges.
     * @return the compiler interface exception handler
     */
    public final CiExceptionHandler handler() {
        return _handler;
    }

    /**
     * Gets the bytecode index of the handler (catch block).
     * @return the bytecode index of the handler
     */
    public int handlerBCI() {
        return _handler.catchBCI();
    }

    /**
     * Utility method to check if this exception handler covers the specified bytecode index.
     * @param bci the bytecode index to check
     * @return <code>true</code> if this exception handler covers the specified bytecode
     */
    public final boolean covers(int bci) {
        return _handler.startBCI() <= bci && bci < _handler.endBCI();
    }

    /**
     * Gets the entry block for this exception handler.
     * @return the entry block
     */
    public final BlockBegin entryBlock() {
        return _entryBlock;
    }

    /**
     * Gets the PC offset of the handler entrypoint, which is used by
     * the runtime to forward exception points to their catch sites.
     * @return the pc offset of the handler entrypoint
     */
    public final int entryPCO() {
        return _entryPCO;
    }

    public final int phiOperand() {
        return _phiOperand;
    }

    public final int scopeCount() {
        return _scopeCount;
    }

    public final void setEntryBlock(BlockBegin entry) {
        _entryBlock = entry;
    }

    public final void setEntryPCO(int pco) {
        _entryPCO = pco;
    }

    public final void setPhiOperand(int phi) {
        _phiOperand = phi;
    }

    public final void setScopeCount(int count) {
        _scopeCount = count;
    }

    public boolean isCatchAll() {
        return _handler.catchClassIndex() == 0;
    }
}
