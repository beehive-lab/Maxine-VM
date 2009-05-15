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
package com.sun.max.vm.compiler.dir;

import com.sun.max.vm.compiler.dir.transform.*;

/**
 * Safepoint instruction with Java frame descriptor.
 *
 * @author Bernd Mathiske
 */
public final class DirSafepoint extends DirInstruction {

    private final DirJavaFrameDescriptor _javaFrameDescriptor;

    public DirJavaFrameDescriptor javaFrameDescriptor() {
        return _javaFrameDescriptor;
    }

    public DirSafepoint(DirJavaFrameDescriptor javaFrameDescriptor) {
        _javaFrameDescriptor = javaFrameDescriptor;
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirSafepoint) {
            final DirSafepoint dirSafepoint = (DirSafepoint) other;
            if (_javaFrameDescriptor == null) {
                return dirSafepoint._javaFrameDescriptor == null;
            }
            return _javaFrameDescriptor.equals(dirSafepoint._javaFrameDescriptor);
        }
        return false;
    }

    @Override
    public String toString() {
        return "safepoint " + _javaFrameDescriptor;
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitSafepoint(this);
    }
}
