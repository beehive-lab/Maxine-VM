/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.BytecodeIntrinsifier.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * The Maxine-specific part of {@linkplain BytecodeIntrinsifier intrinsification}.
 */
public class Intrinsifier extends IntrinsifierClient {

    final ClassMethodActor compilee;
    final CodeAttribute codeAttribute;
    final ConstantPool cp;
    public boolean unsafe;
    public int flags;

    /**
     * Creates an {@link Intrinsifier} instance to process the bytecode of a single method.
     *
     * @param compilee the method to be processed
     * @param codeAttribute the code of the method to be processed
     */
    public Intrinsifier(ClassMethodActor compilee, CodeAttribute codeAttribute) {
        this.cp = codeAttribute.cp;
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
     * @return {@code true} if the code is determined to be unsafe
     */
    public boolean run() {
        try {
            flags = new BytecodeIntrinsifier(this, compilee, codeAttribute.code(), null, codeAttribute.exceptionHandlerBCIs(), codeAttribute.maxStack, initLocals()).run();
            return unsafe;
        } catch (Throwable e) {
            String msg = String.format("Error while intrinsifying " + compilee + "%n" + CodeAttributePrinter.toString(codeAttribute));
            throw (InternalError) new InternalError(msg).initCause(e);
        }
    }

    /**
     * Determines if a given opcode is unsafe (i.e. cannot be compiled by the baseline compiler).
     *
     * @param opcode an opcode to test
     */
    private static boolean isUnsafe(int opcode, int intrinsic) {
        switch (opcode) {
            // Checkstyle: stop
            case READREG            :
            case WRITEREG           :
            case PAUSE              :
            case ADD_SP             :
            case FLUSHW             :
            case ALLOCA             :
            case STACKHANDLE        :
            case JNICALL            :
            case TEMPLATE_CALL      :
            case ICMP               :
            case WCMP               : return true;
            case INFOPOINT: {
                if ((intrinsic & UNCOMMON_TRAP) == UNCOMMON_TRAP) {
                    // An uncommon trap is OK to baseline-compile
                    return false;
                }
                return true;
            }
            // Checkstyle: resume
        }
        return false;
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
                if (intrinsic == UNSAFE_CAST || intrinsic == TEMPLATE_CALL || intrinsic == STACKHANDLE) {
                    bi.intrinsify(intrinsic, cpi);
                } else if (intrinsic != 0) {
                    int opcode = intrinsic & 0xff;
                    if (!unsafe) {
                        unsafe = isUnsafe(opcode, intrinsic);
                    }
                    int operand = (intrinsic >> 8) & 0xffff;
                    bi.intrinsify(opcode, operand);
                } else {
                    if (!unsafe) {
                        // The semantics of @INLINE and @FOLD are not implemented by the baseline compiler.
                        unsafe = (method.flags() & (Actor.FOLD | Actor.INLINE)) != 0;
                    }
                    if (holderIsWord && !isStatic) {
                        // Cannot dispatch dynamically on Word types
                        bi.intrinsify(INVOKESPECIAL, cpi);
                    }
                }
            } catch (NoClassDefFoundError e) {
                // This is almost always due to process code guarded with MaxineVM.isHosted().
                // The compiler will dead-code eliminate such code before it is ever compiled.
            } catch (NoSuchMethodError e) {
                // This is almost always due to process code guarded with MaxineVM.isHosted().
                // The compiler will dead-code eliminate such code before it is ever compiled.
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
