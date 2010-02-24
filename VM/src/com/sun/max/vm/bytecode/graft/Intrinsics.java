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

import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.bytecode.*;
import com.sun.c1x.bytecode.BytecodeExtender.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 *
 *
 * @author Doug Simon
 */
public class Intrinsics extends Intrinsifier {

    final ClassMethodActor compilee;
    final CodeAttribute codeAttribute;
    final ConstantPool cp;

    public Intrinsics(ClassMethodActor compilee, CodeAttribute codeAttribute) {
        this.cp = codeAttribute.constantPool;
        this.compilee = compilee;
        this.codeAttribute = codeAttribute;
    }

    private static final HashMap<MethodActor, Integer> intrinsicsMap = new HashMap<MethodActor, Integer>();

    @HOSTED_ONLY
    public static boolean isUnsafeCast(Method method) {
        final INTRINSIC intrinsic = method.getAnnotation(INTRINSIC.class);
        if (intrinsic == null) {
            return false;
        }
        return intrinsic.value() == Bytecodes.UNSAFE_CAST;
    }

    @HOSTED_ONLY
    public static synchronized void register() {
        for (ClassActor classActor : ClassRegistry.BOOT_CLASS_REGISTRY) {
            for (ClassMethodActor classMethodActor : classActor.localStaticMethodActors()) {
                registerMethod(classMethodActor);
            }
            for (ClassMethodActor classMethodActor : classActor.localVirtualMethodActors()) {
                registerMethod(classMethodActor);
            }
        }
    }

    @HOSTED_ONLY
    private static void registerMethod(ClassMethodActor classMethodActor) {
        final INTRINSIC intrinsic = classMethodActor.getAnnotation(INTRINSIC.class);
        if (intrinsic != null) {
            Integer oldValue = intrinsicsMap.put(classMethodActor, intrinsic.value());
            assert oldValue == null;
        }
    }

    public void run() {
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
        try {
            BytecodeExtender bce = new BytecodeExtender(this, compilee, codeAttribute.code(), codeAttribute.exceptionHandlerPositions(), codeAttribute.maxStack, locals);
            bce.run();
        } catch (Throwable e) {
            String dis = "\n\n" + CodeAttributePrinter.toString(codeAttribute);
            Throwable error = new InternalError("Error while intrinsifying " + compilee + dis).initCause(e);
            error.printStackTrace();
            new BytecodeExtender(this, compilee, codeAttribute.code(), codeAttribute.exceptionHandlerPositions(), codeAttribute.maxStack, locals).run();
            throw (InternalError) error;
        }
    }

    private void intrinsify(BytecodeExtender bce, int cpi, boolean isStatic) {
        MethodRefConstant constant = cp.methodAt(cpi);
        TypeDescriptor holder = constant.holder(cp);
        boolean holderIsWord = holder.toKind().isWord;
        if (holderIsWord || constant.isResolvableWithoutClassLoading(cp)) {
            try {
                MethodActor method = constant.resolve(cp, cpi);
                Integer intrinsic = intrinsicsMap.get(method);
                if (intrinsic != null) {
                    int opcode = intrinsic & 0xff;
                    assert Bytecodes.isExtension(opcode);
                    int operand = Bytecodes.isOpcode3(intrinsic) ? (intrinsic >> 8) & 0xff : cpi;
                    bce.intrinsify(opcode, operand);
                } else if (holderIsWord && !isStatic) {
                    // Cannot dispatch dynamically on Word types
                    bce.intrinsify(Bytecodes.INVOKESPECIAL, cpi);

                }
            } catch (HostOnlyClassError e) {
            } catch (HostOnlyMethodError e) {
            }
        }

        SignatureDescriptor sig = constant.signature(cp);
        invoke(bce, isStatic, sig);
    }

    private void invoke(BytecodeExtender bce, boolean isStatic, SignatureDescriptor sig) {
        for (int i = sig.numberOfParameters() - 1; i >= 0; --i) {
            Kind kind = sig.parameterDescriptorAt(i).toKind();
            bce.pop(kind.stackSlots);
        }

        if (!isStatic) {
            bce.pop(1);
        }
        Kind resultKind = sig.resultKind();
        if (resultKind != Kind.VOID) {
            if (resultKind.isCategory1) {
                bce.push1(resultKind.isWord);
            } else {
                bce.push2();
            }
        }
    }

    @Override
    public void invokeinterface(BytecodeExtender bce, int cpi) {
        intrinsify(bce, cpi, false);
    }

    @Override
    public void invokespecial(BytecodeExtender bce, int cpi) {
        intrinsify(bce, cpi, false);
    }

    @Override
    public void invokestatic(BytecodeExtender bce, int cpi) {
        intrinsify(bce, cpi, true);
    }

    @Override
    public void invokevirtual(BytecodeExtender bce, int cpi) {
        intrinsify(bce, cpi, false);
    }

    @Override
    public void jnicall(BytecodeExtender bce, int cpi) {
        SignatureDescriptor sig = SignatureDescriptor.create(cp.utf8At(cpi, "native function descriptor"));
        invoke(bce, true, sig);
    }

    @Override
    public void getfield(BytecodeExtender bce, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        bce.pop1();
        if (kind.isCategory1) {
            bce.push1(kind.isWord);
        } else {
            bce.push2();
        }
    }

    @Override
    public void getstatic(BytecodeExtender bce, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        if (kind.isCategory1) {
            bce.push1(kind.isWord);
        } else {
            bce.push2();
        }
    }

    @Override
    public void putfield(BytecodeExtender bce, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        bce.pop(kind.stackSlots);
        bce.pop1();
    }

    @Override
    public void putstatic(BytecodeExtender bce, int cpi) {
        FieldRefConstant constant = cp.fieldAt(cpi);
        Kind kind = constant.type(cp).toKind();
        bce.pop(kind.stackSlots);
    }

}
