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


/**
 * The <code>IRScopeDebugInfo</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class IRScopeDebugInfo {

    private final IRScope scope;
    private final int bci;
    private final List<CiValue> locals;
    private final List<CiValue> expressions;
    private final List<CiLocation> monitors;
    private final IRScopeDebugInfo caller;

    public IRScopeDebugInfo(IRScope scope, int bci, List<CiValue> locals, List<CiValue> expressions, List<CiLocation> monitors, IRScopeDebugInfo caller) {
        this.scope = scope;
        this.locals = locals;
        this.bci = bci;
        this.expressions = expressions;
        this.monitors = monitors;
        this.caller = caller;
    }

    public IRScope scope() {
        return scope;
    }

    public int bci() {
        return bci;
    }

    public List<CiValue> locals() {
        return locals;
    }

    public List<CiValue> expressions() {
        return expressions;
    }

    public List<CiLocation> monitors() {
        return monitors;
    }

    public IRScopeDebugInfo caller() {
        return caller;
    }

    public void recordDebugInfo(DebugInformationRecorder recorder, int pcOffset) {
        // TODO : Need to define the implementation of DebugInformationRecorder
    }
}
