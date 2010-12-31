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
package com.sun.max.vm.cps.dir;

import java.util.*;

import com.sun.max.vm.cps.dir.transform.*;

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
