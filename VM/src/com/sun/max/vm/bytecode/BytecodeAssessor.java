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
package com.sun.max.vm.bytecode;

import com.sun.max.collect.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * @author Bernd Mathiske
 */
public final class BytecodeAssessor {

    private static final int _MAX_STRAIGHT_LINE_CODE_LENGTH = 20;

    private static boolean hasStraightLineCode(final ClassMethodActor classMethodActor, final Cons<ClassMethodActor> callers) {
        if (classMethodActor.isDeclaredNeverInline()) {
            return false;
        }
        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        if (codeAttribute == null) {
            return true;
        }
        if (classMethodActor.codeAttribute().code().length > _MAX_STRAIGHT_LINE_CODE_LENGTH) {
            return false;
        }
        if (Cons.containsIdentical(callers, classMethodActor)) {
            return false;
        }
        final ConstantPool constantPool = classMethodActor.codeAttribute().constantPool();
        final BytecodeScanner scanner =
            new BytecodeScanner(new BytecodeAdapter() {
                @Override
                public void opcodeDecoded() {
                    final Bytecode opcode = currentOpcode();
                    if (opcode.is(Bytecode.Flags.CONDITIONAL_BRANCH | Bytecode.Flags.UNCONDITIONAL_BRANCH | Bytecode.Flags.SWITCH)) {
                        bytecodeScanner().stop();
                    }
                }

                private void checkInvoke(int index) {
                    final ClassMethodRefConstant constant = constantPool.classMethodAt(index);
                    if (constant.isResolvableWithoutClassLoading(constantPool)) {
                        final ClassMethodActor callee = (ClassMethodActor) constant.resolve(constantPool, index);
                        if (!hasStraightLineCode(callee, new Cons<ClassMethodActor>(classMethodActor, callers))) {
                            bytecodeScanner().stop();
                        }
                    } else {
                        bytecodeScanner().stop();
                    }
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
        return hasStraightLineCode(classMethodActor, null);
    }

}

