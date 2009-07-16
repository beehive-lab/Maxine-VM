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
package com.sun.c1x.lir;

import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;


/**
 * The <code>XHandler</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class XHandler {

    private CiExceptionHandler desc;

    BlockBegin entryBlock; // Entry block of xhandler
    LIRList entryCode; // LIR-operations that must be executed before jumping to entryBlock
    int entryPco; // pco where entryCode (or entryBlock if no entryCode) starts
    int phiOperand; // For resolving of phi functions at begin of entryBlock
    int scopeCount; // for filling ExceptionRangeEntry.scopeCount
    int lirOpId; // opId of the LIR-operation throwing to this handler

    // creation

    public XHandler(CiExceptionHandler desc) {
        this.desc = desc;
        entryBlock = null;
        entryCode = null;
        entryPco = -1;
        phiOperand = -1;
        scopeCount = -1;
        lirOpId = -1;
    }

    public XHandler(XHandler other) {
        this.desc = other.desc;
        this.entryBlock = other.entryBlock;
        this.entryCode = other.entryCode;
        this.entryPco = other.entryPco;
        this.phiOperand = other.phiOperand;
        this.scopeCount = other.scopeCount;
        this.lirOpId = other.lirOpId;
    }

    // accessors for data of ciExceptionHandler
    public int begBci() {
        return desc.startBCI();
    }

    public int endBci() {
        return desc.endBCI();
    }

    public int handlerBci() {
        return desc.handlerBCI();
    }

    public boolean isCatchAll() {
        return desc.isCatchAll();
    }

    public int catchType() {
        return desc.catchClassIndex();
    }

    public CiType catchKlass() {
        return desc.catchKlass();
    }

    public boolean covers(int bci) {
        return begBci() <= bci && bci < endBci();
    }

    // accessors for additional fields
    public BlockBegin entryBlock() {
        return entryBlock;
    }

    public LIRList entryCode() {
        return entryCode;
    }

    public int entryPco() {
        return entryPco;
    }

    public int phiOperand() {
        assert phiOperand != -1 : "not set";
        return phiOperand;
    }

    public int scopeCount() {
        assert scopeCount != -1 : "not set";
        return scopeCount;
    }

    public int lirOpId() {
        return lirOpId;
    }

    public void setEntryBlock(BlockBegin entryBlock) {
        assert entryBlock.isSet(BlockBegin.BlockFlag.ExceptionEntry) : "must be an exception handler entry";
        assert entryBlock.bci() == handlerBci() : "bci's must correspond";
        this.entryBlock = entryBlock;
    }

    public void setEntryCode(LIRList entryCode) {
        this.entryCode = entryCode;
    }

    public void setEntryPco(int entryPco) {
        this.entryPco = entryPco;
    }

    public void setPhiOperand(int phiOperand) {
        this.phiOperand = phiOperand;
    }

    public void setScopeCount(int scopeCount) {
        this.scopeCount = scopeCount;
    }

    public void setLirOpId(int lirOpId) {
        this.lirOpId = lirOpId;
    };

    public boolean equals(XHandler other) {
        assert (entryPco() != -1) && (other.entryPco() != -1) : "must have entryPco";
        if (entryPco() != other.entryPco()) {
            return false;
        }
        if (scopeCount() != other.scopeCount()) {
            return false;
        }
        if (desc != other.desc) {
            return false;
        }
        assert entryBlock() == other.entryBlock() : "entryBlock must be equal when entryPco is equal";
        return true;
    }
}
