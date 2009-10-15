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

import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * The <code>CodeEmitInfo</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class CodeEmitInfo {

    public IRScopeDebugInfo scopeDebugInfo;
    private final IRScope scope;
    private List<ExceptionHandler> exceptionHandlers;
    public OopMap oopMap;
    private final ValueStack stack; // used by deoptimization (contains also monitors
    private final int bci;

    public CodeEmitInfo(int bci, ValueStack state, List<ExceptionHandler> exceptionHandlers) {
        this.scope = state.scope();
        this.bci = bci;
        this.scopeDebugInfo = null;
        this.oopMap = null;
        this.stack = state;
        this.exceptionHandlers = exceptionHandlers;
    }

    // make a copy
    private CodeEmitInfo(CodeEmitInfo info) {
        this.scope = info.scope;
        this.exceptionHandlers = null;
        this.bci = info.bci;
        this.scopeDebugInfo = null;
        this.oopMap = null;
        stack = info.stack;

        // deep copy of exception handlers
        if (info.exceptionHandlers != null) {
            exceptionHandlers = new ArrayList<ExceptionHandler>();
            for (ExceptionHandler h : info.exceptionHandlers) {
                exceptionHandlers.add(new ExceptionHandler(h));
            }
        }
    }

    public CodeEmitInfo copy() {
        return new CodeEmitInfo(this);
    }

    FrameMap frameMap() {
        return scope.compilation().frameMap();
    }

    /**
     * Gets the scopeDebugInfo of this class.
     *
     * @return the scopeDebugInfo
     */
    public IRScopeDebugInfo scopeDebugInfo() {
        return scopeDebugInfo;
    }

    // accessors
    public OopMap oopMap() {
        return oopMap;
    }

    public void setOopMap(OopMap oopMap) {
        this.oopMap = oopMap;
    }

    public IRScope scope() {
        return scope;
    }

    public List<ExceptionHandler> exceptionHandlers() {
        return exceptionHandlers;
    }

    public ValueStack stack() {
        return stack;
    }

    public int bci() {
        return bci;
    }

    public void addRegisterOop(LIROperand opr) {
        assert oopMap != null :  "oop map must already exist";
        assert opr.isSingleCpu() :  "should not call otherwise";

        CiLocation name = frameMap().regname(opr);
        oopMap.setOop(name);
    }

    public void recordDebugInfo(DebugInformationRecorder recorder, int pcOffset) {

        // TODO: (tw) Check where to generate the oopMap!
        if (oopMap == null) {
            return;
        }

        // record the safepoint before recording the debug info for enclosing scopes
        recorder.addSafepoint(pcOffset, oopMap.deepCopy());
        if (scopeDebugInfo != null) {
            scopeDebugInfo.recordDebugInfo(recorder, pcOffset);
        }
        recorder.endSafepoint(pcOffset);
    }
}
