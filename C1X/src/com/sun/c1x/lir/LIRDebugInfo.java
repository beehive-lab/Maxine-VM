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

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;

/**
 * This class represents debugging and deoptimization information attached to a LIR instruction.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class LIRDebugInfo {

    public abstract static class ValueLocator {
        public abstract CiValue getLocation(Value value);
    }

    public final FrameState state;
    public final List<ExceptionHandler> exceptionHandlers;
    public CiDebugInfo debugInfo;

    public LIRDebugInfo(FrameState state, List<ExceptionHandler> exceptionHandlers) {
        assert state != null;
        this.state = state;
        this.exceptionHandlers = exceptionHandlers;
    }

    private LIRDebugInfo(LIRDebugInfo info) {
        this.state = info.state;

        // deep copy of exception handlers
        if (info.exceptionHandlers != null) {
            this.exceptionHandlers = new ArrayList<ExceptionHandler>();
            for (ExceptionHandler h : info.exceptionHandlers) {
                this.exceptionHandlers.add(new ExceptionHandler(h));
            }
        } else {
            this.exceptionHandlers = null;
        }
    }

    public LIRDebugInfo copy() {
        return new LIRDebugInfo(this);
    }

    public void setOop(CiValue location, C1XCompilation compilation) {
        CiTarget target = compilation.target;
        assert debugInfo != null : "debug info not allocated yet";
        if (location.isAddress()) {
            CiAddress stackLocation = (CiAddress) location;
            assert stackLocation.index.isIllegal();
            if (stackLocation.base == CiRegister.Frame.asValue()) {
                int offset = stackLocation.displacement;
                assert offset % target.wordSize == 0 : "must be aligned";
                int stackMapIndex = offset / target.wordSize;
                setBit(debugInfo.frameRefMap, stackMapIndex);
            }
        } else if (location.isStackSlot()) {
            CiStackSlot stackSlot = (CiStackSlot) location;
            assert !stackSlot.inCallerFrame();
            assert target.spillSlotSize == target.wordSize;
            setBit(debugInfo.frameRefMap, stackSlot.index());
        } else {
            assert location.isRegister() : "objects can only be in a register";
            CiRegisterValue registerLocation = (CiRegisterValue) location;
            int encoding = registerLocation.reg.encoding;
            assert encoding >= 0 : "object cannot be in non-object register " + registerLocation.reg;
            assert encoding < target.arch.registerReferenceMapBitCount;
            setBit(debugInfo.registerRefMap, encoding);
        }
    }

    public CiDebugInfo debugInfo() {
        assert debugInfo != null : "debug info not allocated yet";
        return debugInfo;
    }

    public boolean hasDebugInfo() {
        return debugInfo != null;
    }

    private void setBit(byte[] array, int bit) {
        boolean wasSet = CiUtil.setBit(array, bit);
        assert !wasSet : "Ref map entry " + bit + " is already set.";
    }

    @Override
    public String toString() {
        return state.toString();
    }
}
