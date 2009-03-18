/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.b.c;

import java.util.*;

import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;

public class NameAnalysis {

    private Map<CirVariable, CirVariable> _boundName = new HashMap<CirVariable, CirVariable>();
    boolean _complete;

    public class Transform extends CirVisitor {

        @Override
        public void visitCall(CirCall call) {
            final CirValue op = call.procedure();
            if (op instanceof CirClosure && !(op instanceof CirContinuation)) {
                for (int i = 0; i < call.arguments().length; i++) {
                    if (call.arguments()[i] instanceof CirVariable) {
                        final CirVariable newVar = ((CirClosure) op).parameters()[i];
                        final CirVariable oldVar = (CirVariable) call.arguments()[i];
                        if (!_boundName.containsKey(oldVar)) {
                            _boundName.put(oldVar, oldVar);
                        }
                        final CirVariable oldName = _boundName.get(oldVar);
                        if (_boundName.get(newVar) != oldName) {
                            _boundName.put(newVar, oldName);
                            _complete = false;
                        }
                    }
                }
            }
        }
    }

    public CirVariable get(CirVariable v) {
        return _boundName.get(v);
    }

    public void apply(CirClosure closure) {
        final Transform visitor = new Transform();
        _complete = false;
        while (!_complete) {
            _complete = true;
            CirVisitingTraversal.apply(closure, visitor);
        }
    }
}
