/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir;

import com.sun.max.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * Java frame descriptors carrying CIR location information.
 *
 * @see JavaFrameDescriptor
 *
 * @author Bernd Mathiske
 */
public final class CirJavaFrameDescriptor extends JavaFrameDescriptor<CirValue> {

    public CirJavaFrameDescriptor(CirJavaFrameDescriptor parent, ClassMethodActor classMethodActor, int bytecodePosition, CirValue[] locals, CirValue[] stackSlots) {
        super(parent, classMethodActor, bytecodePosition, locals, stackSlots);
    }

    public CirJavaFrameDescriptor(ClassMethodActor classMethodActor, int bytecodePosition, CirValue[] locals, CirValue[] stackSlots) {
        this(null, classMethodActor, bytecodePosition, locals, stackSlots);
    }

    @Override
    public CirJavaFrameDescriptor parent() {
        return (CirJavaFrameDescriptor) super.parent();
    }

    /**
     * Creates and returns a copy of this Java frame descriptor. The {@linkplain #parent() parent},
     * {@linkplain #classMethodActor() method} and {@linkplain #bci() bytecode position} of the copy is
     * shared with this objects but the {@linkplain #locals() locals} and {@linkplain #stackSlots() stack} are not.
     */
    public CirJavaFrameDescriptor copy() {
        final CirJavaFrameDescriptor p = (parent() == null) ? null : parent().copy();
        return new CirJavaFrameDescriptor(p, classMethodActor, bci, locals.clone(), stackSlots.clone());
    }

    /**
     * Extend this descriptor by an additional top level parent.
     *
     * @param extension the new top level parent
     * @return the extended descriptor
     */
    private CirJavaFrameDescriptor extended(CirJavaFrameDescriptor extension) {
        if (parent() == null) {
            return new CirJavaFrameDescriptor(extension.copy(), classMethodActor, bci, locals.clone(), stackSlots.clone());
        }
        return new CirJavaFrameDescriptor(parent().extended(extension), classMethodActor, bci, locals.clone(), stackSlots.clone());
    }

    private void propagateValue(CirValue value, CirBlock block, CirScopedBlockUpdating scopedBlockUpdating) {
        if (value instanceof CirVariable) {
            final CirVariable variable = (CirVariable) value;
            final CirClosure closure = block.closure();
            if (Utils.indexOfIdentical(closure.parameters(), variable) >= 0) {
                return;
            }
            closure.setParameters(Utils.concat(closure.parameters(), variable));
            for (CirCall call : block.calls()) {
                call.setArguments(Utils.concat(call.arguments(), variable));
            }
            for (CirCall call : block.calls()) {
                final CirBlock scope = scopedBlockUpdating.scope(call);
                if (scope != null) {
                    propagateValue(value, scope, scopedBlockUpdating);
                }
            }
        }
    }

    private void propagateVariables(CirBlock block, CirScopedBlockUpdating scopedBlockUpdating) {
        CirJavaFrameDescriptor javaFrameDescriptor = this;
        do {
            for (CirValue value : javaFrameDescriptor.locals) {
                propagateValue(value, block, scopedBlockUpdating);
            }
            for (CirValue value : javaFrameDescriptor.stackSlots) {
                propagateValue(value, block, scopedBlockUpdating);
            }
            javaFrameDescriptor = javaFrameDescriptor.parent();
        } while (javaFrameDescriptor != null);
    }

    /**
     * Extend every Java frame descriptor in the closure by this descriptor as top-most parent.
     * This represents inlining by one additional level, with this descriptor denoting the outermost caller.
     */
    public void pushInto(CirClosure closure) {
        final CirScopedBlockUpdating scopedBlockUpdating = new CirScopedBlockUpdating(closure.body());
        scopedBlockUpdating.run();

        final CirTraversal traversal = new CirTraversal(closure.body()) {
            @Override
            public void visitCall(CirCall call) {
                if (call.javaFrameDescriptor() != null) {
                    call.setJavaFrameDescriptor(call.javaFrameDescriptor().extended(CirJavaFrameDescriptor.this));
                    final CirBlock scope = scopedBlockUpdating.scope(call);
                    if (scope != null) {
                        propagateVariables(scope, scopedBlockUpdating);
                    }
                }
                super.visitCall(call);
            }
        };
        traversal.run();
    }
}
