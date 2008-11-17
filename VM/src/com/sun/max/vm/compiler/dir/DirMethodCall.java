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
 * @author Bernd Mathiske
 */
public class DirMethodCall extends DirCall {

    private final DirValue _method;

    public DirValue method() {
        return _method;
    }

    private final boolean _isNative;

    /**
     * @return whether the callee is a native method
     */
    public boolean isNative() {
        return _isNative;
    }

    private final DirJavaFrameDescriptor _javaFrameDescriptor;

    public DirJavaFrameDescriptor javaFrameDescriptor() {
        return _javaFrameDescriptor;
    }

    public DirMethodCall(DirVariable result, DirValue method, DirValue[] arguments, DirCatchBlock catchBlock, boolean isNativeCall, DirJavaFrameDescriptor javaFrameDescriptor) {
        super(result, arguments, catchBlock);
        _method = method;
        _isNative = isNativeCall;
        _javaFrameDescriptor = javaFrameDescriptor;
    }


    @Override
    public int hashCodeForBlock() {
        return super.hashCodeForBlock() ^ _method.hashCodeForBlock();
    }

    @Override
    public boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirMethodCall) {
            final DirMethodCall dirMethodCall = (DirMethodCall) other;
            if (!_method.equals(dirMethodCall._method)) {
                return false;
            }
            assert arguments().length == dirMethodCall.arguments().length : "two calls to the same method with different number of args: this = " + this + " other = " + other;
            for (int i = 0; i < arguments().length; i++) {
                if (!arguments()[i].equals(dirMethodCall.arguments()[i])) {
                    return false;
                }
            }
            if (catchBlock() == null) {
                if (dirMethodCall.catchBlock() != null) {
                    return false;
                }
            } else {
                if (dirMethodCall.catchBlock() == null) {
                    return false;
                }
                if (!catchBlock().isEquivalentTo(dirMethodCall.catchBlock(), dirBlockEquivalence)) {
                    return false;
                }
            }
            if (_javaFrameDescriptor == null) {
                if (dirMethodCall._javaFrameDescriptor != null) {
                    return false;
                }
            } else {
                if (dirMethodCall._javaFrameDescriptor == null) {
                    return false;
                }
                if (!_javaFrameDescriptor.equals(dirMethodCall._javaFrameDescriptor)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String arguments = "";
        String separator = "";
        for (int i = 0; i < arguments().length; i++) {
            arguments += separator + arguments()[i];
            separator = " ";
        }
        String s = method().toString() + "(" + arguments + ")";
        if (result() != null) {
            s = result().toString() + " := " +  s;
        }
        if (catchBlock() != null) {
            s += " -> #" + catchBlock().serial();
        }
        return s;
    }

    @Override
    public void acceptVisitor(DirVisitor visitor) {
        visitor.visitMethodCall(this);
    }
}
