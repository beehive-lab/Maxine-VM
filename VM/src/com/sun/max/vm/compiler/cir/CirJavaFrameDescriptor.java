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
package com.sun.max.vm.compiler.cir;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.runtime.*;

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
     * {@linkplain #classMethodActor() method} and {@linkplain #bytecodePosition() bytecode position} of the copy is
     * shared with this objects but the {@linkplain #locals() locals} and {@linkplain #stackSlots() stack} are not.
     */
    public CirJavaFrameDescriptor copy() {
        final CirJavaFrameDescriptor p = (parent() == null) ? null : parent().copy();
        return new CirJavaFrameDescriptor(p, classMethodActor, bytecodePosition, locals.clone(), stackSlots.clone());
    }

    /**
     * Extend this descriptor by an additional top level parent.
     *
     * @param extension the new top level parent
     * @return the extended descriptor
     */
    private CirJavaFrameDescriptor extended(CirJavaFrameDescriptor extension) {
        if (parent() == null) {
            return new CirJavaFrameDescriptor(extension.copy(), classMethodActor, bytecodePosition, locals.clone(), stackSlots.clone());
        }
        return new CirJavaFrameDescriptor(parent().extended(extension), classMethodActor, bytecodePosition, locals.clone(), stackSlots.clone());
    }

    private void propagateValue(CirValue value, CirBlock block, CirScopedBlockUpdating scopedBlockUpdating) {
        if (value instanceof CirVariable) {
            final CirVariable variable = (CirVariable) value;
            final CirClosure closure = block.closure();
            if (Arrays.contains(closure.parameters(), variable)) {
                return;
            }
            closure.setParameters(Arrays.append(closure.parameters(), variable));
            for (CirCall call : block.calls()) {
                call.setArguments(Arrays.append(call.arguments(), variable));
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
