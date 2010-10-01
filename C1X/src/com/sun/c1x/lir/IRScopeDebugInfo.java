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
import com.sun.cri.ci.*;

/**
 * The {@code IRScopeDebugInfo} class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class IRScopeDebugInfo {

    public final IRScope scope;
    public final int bci;
    public final List<CiValue> locals;
    public final List<CiValue> expressions;
    public final List<CiValue> monitors;
    public final IRScopeDebugInfo caller;

    public IRScopeDebugInfo(IRScope scope, int bci, List<CiValue> locals, List<CiValue> expressions, List<CiValue> monitors, IRScopeDebugInfo caller) {
        this.scope = scope;
        this.locals = locals;
        this.bci = bci;
        this.expressions = expressions;
        this.monitors = monitors;
        this.caller = caller;
    }

    public void print() {
        System.out.println("IRScopeDebugInfo " + scope + " @" + bci);
        if (locals != null) {
            System.out.print(" locals: ");
            for (CiValue v : locals) {
                System.out.print(v + " ");
            }
            System.out.println();
        }
        if (expressions != null) {
            System.out.print(" expressions: ");
            for (CiValue v : expressions) {
                System.out.print(v + " ");
            }
            System.out.println();
        }
        if (monitors != null) {
            System.out.print(" monitors: ");
            for (CiValue v : monitors) {
                System.out.print(v + " ");
            }
            System.out.println();
        }
        System.out.println(" caller: " + caller);
    }
}
