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

import com.sun.c1x.ci.*;
import com.sun.c1x.lir.*;

import java.util.*;

/**
 * The <code>ExceptionHandler</code> class represents an exception handler for a Java bytecode method.
 *
 * @author Ben L. Titzer
 */
public class ExceptionHandler {

    public static final List<ExceptionHandler> ZERO_HANDLERS = Collections.emptyList();

    private final CiExceptionHandler handler;
    private BlockBegin entryBlock;
    private LIRList entryCode;
    private int entryPCO;
    private int phiOperand;
    private int scopeCount;

    public ExceptionHandler(CiExceptionHandler handler) {
        this.handler = handler;
        this.entryPCO = -1;
        this.phiOperand = -1;
        this.scopeCount = -1;
    }

    public ExceptionHandler(ExceptionHandler other) {
        this.handler = other.handler;
        this.entryBlock = other.entryBlock;
        this.entryCode = other.entryCode;
        this.entryPCO = other.entryPCO;
        this.phiOperand = other.phiOperand;
        this.scopeCount = other.scopeCount;
    }

    /**
     * Gets the compiler interface object that describes this exception handler,
     * including the bytecode ranges.
     * @return the compiler interface exception handler
     */
    public final CiExceptionHandler handler() {
        return handler;
    }

    /**
     * Gets the bytecode index of the handler (catch block).
     * @return the bytecode index of the handler
     */
    public int handlerBCI() {
        return handler.handlerBCI();
    }

    /**
     * Utility method to check if this exception handler covers the specified bytecode index.
     * @param bci the bytecode index to check
     * @return <code>true</code> if this exception handler covers the specified bytecode
     */
    public final boolean covers(int bci) {
        return handler.startBCI() <= bci && bci < handler.endBCI();
    }

    /**
     * Gets the entry block for this exception handler.
     * @return the entry block
     */
    public final BlockBegin entryBlock() {
        return entryBlock;
    }

    /**
     * Gets the PC offset of the handler entrypoint, which is used by
     * the runtime to forward exception points to their catch sites.
     * @return the pc offset of the handler entrypoint
     */
    public final int entryPCO() {
        return entryPCO;
    }

    public final int phiOperand() {
        return phiOperand;
    }

    public final int scopeCount() {
        return scopeCount;
    }

    public final void setEntryBlock(BlockBegin entry) {
        entryBlock = entry;
    }

    public final void setEntryPCO(int pco) {
        entryPCO = pco;
    }

    public final void setPhiOperand(int phi) {
        phiOperand = phi;
    }

    public final void setScopeCount(int count) {
        scopeCount = count;
    }

    public boolean isCatchAll() {
        return handler.catchClassIndex() == 0;
    }

    public static boolean couldCatch(List<ExceptionHandler> exceptionHandlers, CiType throwKlass, boolean typeIsExact) {
        // TODO Port implementation
        return false;
    }

    public int lirOpId() {
        // TODO Auto-generated method stub
        return 0;
    }

    public LIRList entryCode() {
        return entryCode;
    }

    public void setLirOpId(int throwingOpId) {
        // TODO Auto-generated method stub

    }

    public void setEntryCode(LIRList entryCode2) {
        // TODO Auto-generated method stub

    }
}
