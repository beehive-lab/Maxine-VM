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

import java.util.*;

import com.sun.max.vm.compiler.dir.transform.*;

/**
 * A call to a builtin or a method.
 *
 * @author Bernd Mathiske
 */
public abstract class DirCall<Procedure_Type> extends DirInstruction {

    private final DirVariable _result;
    private final DirValue[] _arguments;
    private DirCatchBlock _catchBlock;
    private final DirJavaFrameDescriptor _javaFrameDescriptor;

    /**
     * @param result the variable which will hold the result of the callee execution
     * @param arguments arguments for the callee
     * @param catchBlock the block where execution continues in case of an exception thrown by the callee
     */
    public DirCall(DirVariable result, DirValue[] arguments, DirCatchBlock catchBlock, DirJavaFrameDescriptor javaFrameDescriptor) {
        _result = result;
        _arguments = arguments;
        _catchBlock = catchBlock;
        _javaFrameDescriptor = javaFrameDescriptor;
    }

    public DirVariable result() {
        return _result;
    }

    public DirValue[] arguments() {
        return _arguments;
    }

    public void setArgument(int index, DirValue value) {
        assert value != null;
        _arguments[index] = value;
    }

    @Override
    public DirCatchBlock catchBlock() {
        return _catchBlock;
    }

    public DirJavaFrameDescriptor javaFrameDescriptor() {
        return _javaFrameDescriptor;
    }

    @Override
    public void substituteBlocks(Map<DirBlock, DirBlock> blockMap) {
        final DirBlock block = blockMap.get(_catchBlock);
        if (block != null && block != _catchBlock) {
            _catchBlock = (DirCatchBlock) block;
        }
    }

    protected abstract Procedure_Type procedure();

    @Override
    public final boolean isEquivalentTo(DirInstruction other, DirBlockEquivalence dirBlockEquivalence) {
        if (other instanceof DirCall) {
            final DirCall dirCall = (DirCall) other;
            if (!procedure().equals(dirCall.procedure())) {
                return false;
            }
            assert arguments().length == dirCall.arguments().length : "two calls to the same method or builtin with different number of args: this = " + this + " other = " + other;
            for (int i = 0; i < arguments().length; i++) {
                if (!arguments()[i].equals(dirCall.arguments()[i])) {
                    return false;
                }
            }
            if (catchBlock() == null) {
                if (dirCall.catchBlock() != null) {
                    return false;
                }
            } else {
                if (dirCall.catchBlock() == null) {
                    return false;
                }
                if (!catchBlock().isEquivalentTo(dirCall.catchBlock(), dirBlockEquivalence)) {
                    return false;
                }
            }
            if (_javaFrameDescriptor == null) {
                if (dirCall._javaFrameDescriptor != null) {
                    return false;
                }
            } else {
                if (dirCall._javaFrameDescriptor == null) {
                    return false;
                }
                if (!_javaFrameDescriptor.equals(dirCall._javaFrameDescriptor)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public final String toString() {
        String arguments = "";
        String separator = "";
        for (int i = 0; i < arguments().length; i++) {
            arguments += separator + arguments()[i];
            separator = " ";
        }
        String s = procedure().toString() + "(" + arguments + ")";
        if (result() != null) {
            s = result().toString() + " := " +  s;
        }
        if (catchBlock() != null) {
            s += " -> #" + catchBlock().serial();
        }
        if (_javaFrameDescriptor != null) {
            s += " " + _javaFrameDescriptor.toString();
        }
        return s;
    }


}
