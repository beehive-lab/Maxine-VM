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

import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * This class represents debugging and deoptimization information attached to a LIR instruction.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class LIRDebugInfo {

    public final ValueStack stack;
    public final int bci;
    public final List<ExceptionHandler> exceptionHandlers;

    public IRScopeDebugInfo scopeDebugInfo;
    public OopMap oopMap;

    public LIRDebugInfo(ValueStack state, int bci, List<ExceptionHandler> exceptionHandlers) {
        this.bci = bci;
        this.scopeDebugInfo = null;
        this.oopMap = null;
        this.stack = state;
        this.exceptionHandlers = exceptionHandlers;
    }

    // make a copy
    private LIRDebugInfo(LIRDebugInfo info) {
        this.bci = info.bci;
        this.scopeDebugInfo = null;
        this.oopMap = null;
        this.stack = info.stack;

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

    public void recordDebugInfo(DebugInformationRecorder recorder, int pcOffset) {
        // TODO: (tw) Check where to generate the oopMap!
    }
}
