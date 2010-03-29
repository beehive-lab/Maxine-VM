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

import java.util.*;

import com.sun.c1x.lir.*;
import com.sun.c1x.ri.*;

/**
 * The {@code ExceptionHandler} class represents an exception handler for a Java bytecode method.
 *
 * @author Ben L. Titzer
 */
public class ExceptionHandler {

    public static final List<ExceptionHandler> ZERO_HANDLERS = Collections.emptyList();

    public final RiExceptionHandler handler;
    private BlockBegin entryBlock;
    private LIRList entryCode;
    private int entryCodeOffset;
    private int phiOperand;
    private int scopeCount;
    private int lirOpId;

    public ExceptionHandler(RiExceptionHandler handler) {
        this.handler = handler;
        this.entryCodeOffset = -1;
        this.phiOperand = -1;
        this.scopeCount = -1;
        this.lirOpId = -1;
    }

    public ExceptionHandler(ExceptionHandler other) {
        this.handler = other.handler;
        this.entryBlock = other.entryBlock;
        this.entryCode = other.entryCode;
        this.entryCodeOffset = other.entryCodeOffset;
        this.phiOperand = other.phiOperand;
        this.scopeCount = other.scopeCount;
        this.lirOpId = other.lirOpId;
    }

    @Override
    public String toString() {
        return "XHandler(Block=" + entryBlock.blockID + ")";
    }

    /**
     * Gets the compiler interface object that describes this exception handler,
     * including the bytecode ranges.
     * @return the compiler interface exception handler
     */
    public final RiExceptionHandler handler() {
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
     * @return {@code true} if this exception handler covers the specified bytecode
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
    public final int entryCodeOffset() {
        return entryCodeOffset;
    }

    public final int phiOperand() {
        return phiOperand;
    }

    public final void setEntryBlock(BlockBegin entry) {
        entryBlock = entry;
    }

    public final void setEntryCodeOffset(int pco) {
        entryCodeOffset = pco;
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

    public static boolean couldCatch(List<ExceptionHandler> exceptionHandlers, RiType klass, boolean typeIsExact) {
        // the type is unknown so be conservative
        if (!klass.isLoaded()) {
            return true;
        }

        for (int i = 0; i < exceptionHandlers.size(); i++) {
            ExceptionHandler handler = exceptionHandlers.get(i);
            if (handler.isCatchAll()) {
                // catch of ANY
                return true;
            }
            RiType handlerKlass = handler.handler.catchKlass();
            // if it's unknown it might be catchable
            if (!handlerKlass.isLoaded()) {
                return true;
            }
            // if the throw type is definitely a subtype of the catch type
            // then it can be caught.
            if (klass.isSubtypeOf(handlerKlass)) {
                return true;
            }
            if (!typeIsExact) {
                // If the type isn't exactly known then it can also be caught by
                // catch statements where the inexact type is a subtype of the
                // catch type.
                // given: foo extends bar extends Exception
                // throw bar can be caught by catch foo, catch bar, and catch
                // Exception, however it can't be caught by any handlers without
                // bar in its type hierarchy.
                if (handlerKlass.isSubtypeOf(klass)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int lirOpId() {
        return lirOpId;
    }

    public LIRList entryCode() {
        return entryCode;
    }

    public void setLirOpId(int throwingOpId) {
        lirOpId = throwingOpId;
    }

    public void setEntryCode(LIRList entryCode) {
        this.entryCode = entryCode;

    }
}
