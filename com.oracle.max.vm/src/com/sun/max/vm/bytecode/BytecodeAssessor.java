/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 */
public final class BytecodeAssessor {

    private static final int MAX_STRAIGHT_LINE_CODE_LENGTH = 20;

    private static boolean hasStraightLineCode(final ClassMethodActor classMethodActor, final List<ClassMethodActor> callers) {
        if (classMethodActor.isDeclaredNeverInline()) {
            return false;
        }
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        if (codeAttribute == null) {
            return true;
        }
        if (classMethodActor.codeAttribute().code().length > MAX_STRAIGHT_LINE_CODE_LENGTH) {
            return false;
        }
        if (Utils.indexOfIdentical(callers, classMethodActor) != -1) {
            return false;
        }
        final ConstantPool constantPool = classMethodActor.codeAttribute().cp;
        final BytecodeScanner scanner =
            new BytecodeScanner(new BytecodeAdapter() {
                @Override
                public void opcodeDecoded() {
                    final int opcode = currentOpcode();
                    if (Bytecodes.isBlockEnd(opcode)) {
                        bytecodeScanner().stop();
                    }
                }

                private void checkInvoke(int index) {
                    final ClassMethodRefConstant constant = constantPool.classMethodAt(index);
                    if (constant.isResolvableWithoutClassLoading(constantPool)) {
                        try {
                            final ClassMethodActor callee = (ClassMethodActor) constant.resolve(constantPool, index);
                            LinkedList<ClassMethodActor> innerCallers = new LinkedList<ClassMethodActor>(callers);
                            innerCallers.addFirst(classMethodActor);
                            if (hasStraightLineCode(callee, innerCallers)) {
                                return;
                            }
                        } catch (NoSuchMethodError noSuchMethodError) {
                        }
                    }
                    bytecodeScanner().stop();
                }

                @Override
                public void invokestatic(int index) {
                    checkInvoke(index);
                }

                @Override
                public void invokespecial(int index) {
                    checkInvoke(index);
                }

                @Override
                public void invokevirtual(int index) {
                    bytecodeScanner().stop();
                }

                @Override
                public void invokeinterface(int index, int count) {
                    bytecodeScanner().stop();
                }
            });
        scanner.scan(classMethodActor);
        return !scanner.wasStopped();
    }

    /**
     * Return true if this dynamic method is a simple accessor (i.e., its body follows the pattern aload_0, getfield, return).
     */
    public static boolean hasSmallStraightlineCode(ClassMethodActor classMethodActor) {
        List<ClassMethodActor> callers = Collections.emptyList();
        return hasStraightLineCode(classMethodActor, callers);
    }

}

