/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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

package com.sun.max.vm.bytecode.graft;

import com.sun.c1x.bytecode.*;
import com.sun.c1x.bytecode.BytecodeIntrinsifier.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * The Maxine-specific part of {@linkplain BytecodeIntrinsifier intrinsification}.
 *
 * @author Doug Simon
 */
public class Intrinsics extends IntrinsifierClient {

    final ClassMethodActor compilee;
    final CodeAttribute codeAttribute;
    final ConstantPool cp;
    public boolean unsafe;
    public boolean extended;

    /**
     * Creates an {@link Intrinsics} instance to process the bytecode of a single method.
     *
     * @param compilee the method to be processed
     * @param codeAttribute the code of the method to be processed
     */
    public Intrinsics(ClassMethodActor compilee, CodeAttribute codeAttribute) {
        this.cp = codeAttribute.constantPool;
        this.compilee = compilee;
        this.codeAttribute = codeAttribute;
        unsafe = compilee.isUnsafe();
    }

    /**
     * Creates the map denoting which local variables of the method being intrinsified
     * initially hold {@link Word} values based its signature.
     */
    private boolean[] initLocals() {
        boolean[] locals = new boolean[codeAttribute.maxLocals];
        int i = 0;
        if (!compilee.isStatic()) {
            locals[i++] = compilee.holder().kind.isWord;
        }
        SignatureDescriptor sig = compilee.descriptor();
        for (int j = 0; j < sig.numberOfParameters(); j++) {
            Kind kind = sig.parameterDescriptorAt(j).toKind();
            if (kind.isWord) {
                locals[i] = true;
            }
            i += kind.stackSlots;
        }
        return locals;
    }

    /**
     * Processes the given code to intrinsify it.
     *
     * @return {@code} if the code is determined to be unsafe (i.e. cannot be compiled by the JIT compiler)
     */
    public boolean run() {
        try {
            extended = new BytecodeIntrinsifier(this, compilee, codeAttribute.code(), null, codeAttribute.exceptionHandlerPositions(), codeAttribute.maxStack, initLocals()).run();
            return unsafe;
        } catch (Throwable e) {
            String msg = String.format("Error while intrinsifying " + compilee + "%n" + CodeAttributePrinter.toString(codeAttribute));
            throw (InternalError) new InternalError(msg).initCause(e);
        }
    }

    /**
     * Determines if a given opcode is unsafe (i.e. cannot be compiled by the JIT compiler).
     *
     * @param opcode an opcode to test
     */
    private static boolean isUnsafe(int opcode) {
        switch (opcode) {
            // Checkstyle: stop
            case Bytecodes.READREG            :
            case Bytecodes.WRITEREG           :
            case Bytecodes.SAFEPOINT          :
            case Bytecodes.PAUSE              :
            case Bytecodes.ADD_SP             :
            case Bytecodes.READ_PC            :
            case Bytecodes.FLUSHW             :
            case Bytecodes.ALLOCA             :
            case Bytecodes.STACKADDR          :
            case Bytecodes.JNICALL            :
            case Bytecodes.CALL               :
            case Bytecodes.ICMP               :
            case Bytecodes.WCMP               :
            case Bytecodes.RET                :
            case Bytecodes.JSR_W              :
            case Bytecodes.JSR                : return true;
            // Checkstyle: resume
        }
        return false;
    }

    public static char toUnsafeOperand(Kind kind) {
        switch (kind.asEnum) {
            // Checkstyle: stop
            case INT:        return 'i';
            case BOOLEAN:    return 'z';
            case BYTE:       return 'b';
            case CHAR:       return 'c';
            case DOUBLE:     return 'd';
            case FLOAT:      return 'f';
            case LONG:       return 'l';
            case REFERENCE:  return 'a';
            case SHORT:      return 's';
            case WORD:       return 'w';
            // Checkstyle: resume
            default:
                throw new IllegalArgumentException("Unknown UNSAFE_CAST type char operand: " + kind);
        }
    }

    public static Kind toUnsafeCastOperand(char typeChar) {
        switch (typeChar) {
            // Checkstyle: stop
            case 'i': return Kind.INT;
            case 'z': return Kind.BOOLEAN;
            case 'b': return Kind.BYTE;
            case 'c': return Kind.CHAR;
            case 'd': return Kind.DOUBLE;
            case 'f': return Kind.FLOAT;
            case 'l': return Kind.LONG;
            case 'a': return Kind.REFERENCE;
            case 's': return Kind.SHORT;
            case 'w': return Kind.WORD;
            // Checkstyle: resume
            default:
                throw new IllegalArgumentException("Unknown UNSAFE_CAST type char operand: " + typeChar);
        }
    }

    /**
     * Attempts to intrinsify a method invocation.
     *
     * @param bi the intrinsification engine
     * @param cpi the constant pool index of the invocation to process
     * @param isStatic specifies if the invocation is to a static method
     */
    private void intrinsify(BytecodeIntrinsifier bi, int cpi, boolean isStatic) {
        MethodRefConstant constant = cp.methodAt(cpi);
        TypeDescriptor holder = constant.holder(cp);
        SignatureDescriptor sig = constant.signature(cp);
        boolean holderIsWord = holder.toKind().isWord;
        if (holderIsWord || constant.isResolvableWithoutClassLoading(cp)) {
            try {
                MethodActor method = constant.resolve(cp, cpi);
                int intrinsic = method.intrinsic();
                if (intrinsic == Bytecodes.UNSAFE_CAST) {
                    Kind fromKind = isStatic ? sig.parameterDescriptorAt(0).toKind() : holder.toKind();
                    Kind toKind = sig.resultKind();
                    char fromChar = toUnsafeOperand(fromKind);
                    char toChar = toUnsafeOperand(toKind);
                    bi.intrinsify(Bytecodes.UNSAFE_CAST, fromChar << 8 | toChar);
                } else if (intrinsic == Bytecodes.CALL) {
                    bi.intrinsify(intrinsic, cpi);
                } else if (intrinsic != 0) {
                    int opcode = intrinsic & 0xff;
                    if (!unsafe) {
                        unsafe = isUnsafe(opcode);
                    }
                    assert !Bytecodes.isStandard(opcode);
                    int operand = (intrinsic >> 8) & 0xffff;
                    bi.intrinsify(opcode, operand);
                } else {
                    if (!unsafe) {
                        unsafe = (method.flags() & (Actor.FOLD | Actor.INLINE)) != 0;
                    }
                    if (holderIsWord && !isStatic) {
                        // Cannot dispatch dynamically on Word types
                        bi.intrinsify(Bytecodes.INVOKESPECIAL, cpi);
                    }
                }
            } catch (HostOnlyClassError e) {
            } catch (HostOnlyMethodError e) {
            }
        }

        invoke(bi, isStatic, sig);
    }

    private void invoke(BytecodeIntrinsifier bi, boolean isStatic, SignatureDescriptor sig) {
        for (int i = sig.numberOfParameters() - 1; i >= 0; --i) {
            Kind kind = sig.parameterDescriptorAt(i).toKind();
            bi.pop(kind.stackSlots);
        }

        if (!isStatic) {
            bi.pop(1);
        }
        Kind resultKind = sig.resultKind();
        if (resultKind != Kind.VOID) {
            if (resultKind.isCategory1) {
                bi.push1(resultKind.isWord);
            } else {
                bi.push2();
            }
        }
    }

    @Override
    public void invokeinterface(BytecodeIntrinsifier bi, int cpi) {
        intrinsify(bi, cpi, false);
    }

    @Override
    public void invokespecial(BytecodeIntrinsifier bi, int cpi) {
        intrinsify(bi, cpi, false);
    }

    @Override
    public void invokestatic(BytecodeIntrinsifier bi, int cpi) {
        intrinsify(bi, cpi, true);
    }

    @Override
    public void invokevirtual(BytecodeIntrinsifier bi, int cpi) {
        intrinsify(bi, cpi, false);
    }

    @Override
    public void jnicall(BytecodeIntrinsifier bi, int cpi) {
        SignatureDescriptor sig = SignatureDescriptor.create(cp.utf8At(cpi, "native function descriptor"));
        invoke(bi, true, sig);
    }

    @Override
    public void getfield(BytecodeIntrinsifier bi, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        bi.pop1();
        if (kind.isCategory1) {
            bi.push1(kind.isWord);
        } else {
            bi.push2();
        }
    }

    @Override
    public void getstatic(BytecodeIntrinsifier bi, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        if (kind.isCategory1) {
            bi.push1(kind.isWord);
        } else {
            bi.push2();
        }
    }

    @Override
    public void putfield(BytecodeIntrinsifier bi, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        bi.pop(kind.stackSlots);
        bi.pop1();
    }

    @Override
    public void putstatic(BytecodeIntrinsifier bi, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        bi.pop(kind.stackSlots);
    }
}
