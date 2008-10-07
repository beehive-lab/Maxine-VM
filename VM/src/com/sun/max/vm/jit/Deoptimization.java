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
/*VCSID=884687bf-1524-4c81-9d87-0fd23d690288*/
package com.sun.max.vm.jit;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public abstract class Deoptimization implements StackFrameVisitor, TargetLocationVisitor {
    private StackFrame _sourceFrame;

    protected StackFrame sourceFrame() {
        return _sourceFrame;
    }

    private StackFrame _parentFrame;

    protected StackFrame parentFrame() {
        return _parentFrame;
    }

    private boolean _isParentFrameOptimized = true;

    private final RandomAccessWordBuffer _buffer = new RandomAccessWordBuffer();

    protected RandomAccessWordBuffer buffer() {
        return _buffer;
    }

    protected Deoptimization() {
    }

    public boolean visitFrame(StackFrame stackFrame) {
        if (stackFrame.isTopFrame()) {
            _sourceFrame = stackFrame;
            return true;
        }
        if (stackFrame.isAdapter()) {
            _isParentFrameOptimized = false;
            return true;
        }
        _parentFrame = stackFrame;
        return false;
    }

    protected abstract void createJitFrame(TargetJavaFrameDescriptor targetJavaFrameDescriptor, Deoptimizer.ReferenceOccurrences referenceOccurrences);

    protected abstract void createAdapterFrame(TargetJavaFrameDescriptor targetJavaFrameDescriptor);

    protected abstract void fixCallChain();

    void createJitFrames(TargetJavaFrameDescriptor targetJavaFrameDescriptor) {
        TargetJavaFrameDescriptor d = targetJavaFrameDescriptor;
        createJitFrame(d, (Deoptimizer.ReferenceOccurrences) VmThreadLocal.DEOPTIMIZER_REFERENCE_OCCURRENCES.getVariableReference().toJava());
        while (true) {
            final TargetJavaFrameDescriptor next = targetJavaFrameDescriptor.parent();
            if (next == null) {
                break;
            }
            d = next;
            createJitFrame(d, Deoptimizer.ReferenceOccurrences.SAFEPOINT);
        }
        if (_isParentFrameOptimized) {
            final ClassMethodActor classMethodActor = d.bytecodeLocation().classMethodActor();
            if (!(classMethodActor.isStatic() && classMethodActor.descriptor().hasNoParameters())) {
                createAdapterFrame(d);
            }
        }
        fixCallChain();
    }

    protected abstract void patchExecutionContext();

}
