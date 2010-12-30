/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * Performs copy propagation of variables in CIR so that
 *   ((lambda (y) E) x) is transformed to E[x/y].
 *
 * Only propagates to CirClosures; does not copy propagate to CirBlocks.
 *
 * @author Aziz Ghuloum
 */
public class CirCopyPropagation{
    IdentityHashMapping<CirVariable, CirValue> copies = new IdentityHashMapping<CirVariable, CirValue>();
    LinkedList queue = null;
    IdentitySet<CirBlock> seenBlocks = new IdentitySet<CirBlock>();

    public static void apply(CirClosure closure) {
        final CirCopyPropagation x = new CirCopyPropagation();
        x.enqueue(closure.body());
        x.runall();
    }

    private void runall() {
        while (queue != null) {
            final CirCall x = queue.head;
            queue = queue.rest;
            processCall(x);
        }

    }

    private void processCall(CirCall call) {
        CirJavaFrameDescriptor fd = call.javaFrameDescriptor();
        while (fd != null) {
            final CirValue[] local = fd.locals;
            for (int i = 0; i < local.length; i++) {
                local[i] = processValue(local[i]);
            }
            final CirValue[] stack = fd.stackSlots;
            for (int i = 0; i < stack.length; i++) {
                stack[i] = processValue(stack[i]);
            }
            fd = fd.parent();
        }
        final CirValue[] args = call.arguments();
        for (int i = 0; i < args.length; i++) {
            args[i] = processValue(args[i]);
        }
        final CirValue proc = processValue(call.procedure());
        call.setProcedure(proc);
        if (proc instanceof CirClosure) {
            copyPropagate(call);
        }
    }

    private void copyPropagate(CirCall call) {
        final CirClosure proc = (CirClosure) call.procedure();
        final CirValue[] args = call.arguments();
        final CirVariable[] params = proc.parameters();
        final CirValue[] subst = CirCall.newArguments(args.length);
        assert params.length == args.length;
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof CirContinuationVariable) {
                assert args[i] instanceof CirContinuation || args[i] instanceof CirContinuationVariable;
                subst[i] = null;
            } else {
                if (args[i] instanceof CirVariable || args[i] instanceof CirConstant) {
                    subst[i] = args[i];
                }
            }
        }

        int finalLength = params.length;
        for (int i = 0; i < params.length; i++) {
            if (subst[i] != null) {
                finalLength--;
                copies.put(params[i], subst[i]);
            }
        }

        if (finalLength != params.length) {
            if (finalLength == 0) {
                call.assign(proc.body());
            } else {
                final CirValue[] nargs = new CirValue[finalLength];
                final CirVariable[] nparams = new CirVariable[finalLength];
                int j = 0;
                for (int i = 0; i < params.length; i++) {
                    if (subst[i] == null) {
                        assert args[i] != null;
                        nargs[j] = args[i];
                        nparams[j] = params[i];
                        j++;
                    }
                }
                proc.setParameters(nparams);
                call.setArguments(nargs);
            }
        }
    }

    private CirValue processValue(CirValue val) {
        if (val instanceof CirVariable) {
            final CirValue v = copies.get((CirVariable) val);
            if (v == null) {
                return val;
            }
            return v;
        }
        if (val instanceof CirBlock) {
            final CirBlock b = (CirBlock) val;
            if (!seenBlocks.contains(b)) {
                seenBlocks.add(b);
                enqueue(b.closure().body());
            }
        } else if (val instanceof CirClosure) {
            enqueue(((CirClosure) val).body());
        }
        return val;
    }

    private void enqueue(CirCall call) {
        queue = new LinkedList(call, queue);
    }

    private static final class LinkedList {
        private final CirCall head;
        private final LinkedList rest;
        private LinkedList(CirCall x, LinkedList y) {
            head = x;
            rest = y;
        }
    }

}
