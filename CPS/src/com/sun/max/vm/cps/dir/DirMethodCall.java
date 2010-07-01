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
package com.sun.max.vm.cps.dir;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.cps.dir.transform.*;

/**
 * @author Bernd Mathiske
 */
public class DirMethodCall extends DirCall<DirValue> {

    private final DirValue method;

    public DirValue method() {
        return method;
    }

    private final boolean isNative;

    /**
     * Determines if this is a call to a native function. Note, this does not mean a call to a native method, but the
     * call inside a native method's stub to the actual native code. This will be the translation of the
     * {@link Bytecodes#JNICALL} instruction.
     */
    public boolean isNative() {
        return isNative;
    }

    public DirMethodCall(DirVariable result, DirValue method, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
        super(result, arguments, catchBlock, javaFrameDescriptor);
        this.method = method;
        this.isNative = isNativeCall;
    }

    @Override
    protected DirValue procedure() {
        return method;
    }

    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ method.hashCodeForBlock();
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitMethodCall(this);
    }

    @Override
    public String toString() {
        if (isNative) {
            return super.toString() + " <native function call>";
        }
        return super.toString();
    }
}
