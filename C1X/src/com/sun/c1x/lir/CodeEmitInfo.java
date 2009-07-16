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
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * The <code>CodeEmitInfo</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class CodeEmitInfo {

    private IRScopeDebugInfo scopeDebugInfo;
    private IRScope scope;
    private List<ExceptionHandler> exceptionHandlers;
    private OopMap oopMap;
    private ValueStack stack; // used by deoptimization (contains also monitors
    private int bci;
    private CodeEmitInfo next;
    private int id;

    public CodeEmitInfo(CodeEmitInfo info) {
        this(info, false);
    }

    // use scope from ValueStack
    public CodeEmitInfo(int bci, ValueStack stack, List<ExceptionHandler> exceptionHandlers) {
        this.scope = stack.scope();
        this.bci = bci;
        this.scopeDebugInfo = null;
        this.oopMap = null;
        this.stack = stack;
        this.exceptionHandlers = exceptionHandlers;
        this.next = null;
        this.id = -1;
        assert this.stack != null : "must be non null";
        assert bci == C1XOptions.InvocationEntryBci || Bytecodes.isDefined(scope().method().javaCodeAtBci(bci)) : "make sure bci points at a real bytecode";
    }

    // used by natives
    public CodeEmitInfo(IRScope scope, int bci) {
        this.scope = scope;
        this.bci = bci;
        this.oopMap = null;
        this.scopeDebugInfo = null;
        this.stack = null;
        this.exceptionHandlers = null;
        this.next = null;
        this.id = -1;
    }

    // make a copy
    public CodeEmitInfo(CodeEmitInfo info, boolean lockStackOnly) {
        this.scope = info.scope;
        this.exceptionHandlers = null;
        this.bci = info.bci;
        this.scopeDebugInfo = null;
        this.oopMap = null;

        if (lockStackOnly) {
            if (info.stack != null) {
                stack = info.stack.copyLocks();
            } else {
                stack = null;
            }
        } else {
            stack = info.stack;
        }

        // deep copy of exception handlers
        if (info.exceptionHandlers != null) {
            exceptionHandlers = new ArrayList<ExceptionHandler>();
            exceptionHandlers.addAll(info.exceptionHandlers);
        }
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

    public IRScope scope() {
        return scope;
    }

    public CiMethod method() {
        return scope.method();
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
    }

    public void recordDebugInfo(DebugInformationRecorder recorder, int pcOffset) {
    }

    public CodeEmitInfo next() {
        return next;
    }

    public void setNext(CodeEmitInfo next) {
        this.next = next;
    }

    public int id() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
