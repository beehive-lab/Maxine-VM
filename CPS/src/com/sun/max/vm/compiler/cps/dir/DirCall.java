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
package com.sun.max.vm.compiler.cps.dir;

import java.util.*;

import com.sun.max.vm.compiler.cps.dir.transform.*;

/**
 * A call to a builtin or a method.
 *
 * @author Bernd Mathiske
 */
public abstract class DirCall<Procedure_Type> extends DirInstruction {

    private final DirVariable result;
    private final DirValue[] arguments;
    private DirCatchBlock catchBlock;
    private final DirJavaFrameDescriptor javaFrameDescriptor;

    /**
     * @param result the variable which will hold the result of the callee execution
     * @param arguments arguments for the callee
     * @param catchBlock the block where execution continues in case of an exception thrown by the callee
     */
    public DirCall(DirVariable result, DirValue[] arguments, DirCatchBlock catchBlock, DirJavaFrameDescriptor javaFrameDescriptor) {
        this.result = result;
        this.arguments = arguments;
        this.catchBlock = catchBlock;
        this.javaFrameDescriptor = javaFrameDescriptor;
    }

    public DirVariable result() {
        return result;
    }

    public DirValue[] arguments() {
        return arguments;
    }

    public void setArgument(int index, DirValue value) {
        assert value != null;
        arguments[index] = value;
    }

    @Override
    public DirCatchBlock catchBlock() {
        return catchBlock;
    }

    public DirJavaFrameDescriptor javaFrameDescriptor() {
        return javaFrameDescriptor;
    }

    @Override
    public void substituteBlocks(Map<DirBlock, DirBlock> blockMap) {
        final DirBlock block = blockMap.get(catchBlock);
        if (block != null && block != catchBlock) {
            catchBlock = (DirCatchBlock) block;
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
            if (javaFrameDescriptor == null) {
                if (dirCall.javaFrameDescriptor != null) {
                    return false;
                }
            } else {
                if (dirCall.javaFrameDescriptor == null) {
                    return false;
                }
                if (!javaFrameDescriptor.equals(dirCall.javaFrameDescriptor)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String argumentsString = "";
        String separator = "";
        for (int i = 0; i < arguments().length; i++) {
            argumentsString += separator + arguments()[i];
            separator = " ";
        }
        String s = procedure().toString() + "(" + argumentsString + ")";
        if (result() != null) {
            s = result().toString() + " := " +  s;
        }
        if (catchBlock() != null) {
            s += " -> #" + catchBlock().serial();
        }
        if (javaFrameDescriptor != null) {
            s += " " + javaFrameDescriptor.toString();
        }
        return s;
    }

}
