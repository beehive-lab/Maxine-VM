/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import com.sun.c1x.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * The {@code MaxRiConstantPool} class implements a constant pool for
 * the compiler interface, including support for looking up constants during
 * compilation, as well as runtime support for resolving constants in
 * the compiled code.
 * <p>
 * Note that all methods that begin with "{@code resolve}" are runtime calls
 * that actually perform resolution, and therefore have side effects. Calls to
 * these methods are inserted into the compiled code for unresolved constant
 * pool constants by the compiler.
 * <p>
 * All methods that begin with "{@code lookup}" are side-effect free. They
 * will only look up constant pool constants that are already resolved. They
 * are used by the compiler in looking up constants during compilation.
 *
 * @author Ben L. Titzer
 */
public class MaxRiConstantPool implements RiConstantPool {
    public final ConstantPool constantPool;

    /**
     * Creates a new constant pool inside of the specified runtime for the specified constant pool.
     * @param constantPool the actual constant pool contents
     */
    MaxRiConstantPool(ConstantPool constantPool) {
        this.constantPool = constantPool;
    }

     // TODO: check for incompatible class changes in all resolution and lookup methods

    /**
     * Resolves a method reference for an invokevirtual at runtime, and makes the
     * necessary runtime checks for invokevirtual on the specified method.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod resolveInvokeVirtual(char cpi) {
        return resolveMethod(cpi);
    }

    /**
     * Resolves a method reference for an invokespecial at runtime, and makes the
     * necessary runtime checks for invokespecial on the specified method.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod resolveInvokeSpecial(char cpi) {
        return resolveMethod(cpi);
    }

    /**
     * Resolves a method reference for an invokeinterface at runtime, and makes the
     * necessary runtime checks for invokeinterface on the specified method.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod resolveInvokeInterface(char cpi) {
        return resolveMethod(cpi);
    }

    /**
     * Resolves a method reference for an invokestatic at runtime, and makes the
     * necessary runtime checks for invokestatic on the specified method.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod resolveInvokeStatic(char cpi) {
        return resolveMethod(cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField lookupGetField(char cpi) {
        return fieldFrom(constantPool.fieldAt(cpi), cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField lookupPutField(char cpi) {
        return fieldFrom(constantPool.fieldAt(cpi), cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField lookupGetStatic(char cpi) {
        return fieldFrom(constantPool.fieldAt(cpi), cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField lookupPutStatic(char cpi) {
        return fieldFrom(constantPool.fieldAt(cpi), cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod lookupInvokeVirtual(char cpi) {
        return methodFrom(constantPool.methodAt(cpi), cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod lookupInvokeSpecial(char cpi) {
        return methodFrom(constantPool.methodAt(cpi), cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod lookupInvokeInterface(char cpi) {
        return methodFrom(constantPool.methodAt(cpi), cpi);
    }

    /**
     *
     * @param cpi the constant pool index of the method reference
     * @return the compiler interface method resolved at that index
     */
    public RiMethod lookupInvokeStatic(char cpi) {
        return methodFrom(constantPool.methodAt(cpi), cpi);
    }

    public RiMethod lookupMethod(char cpi) {
        return methodFrom(constantPool.methodAt(cpi), cpi);
    }

    private RiMethod resolveMethod(char cpi) {
        return constantPool.methodAt(cpi).resolve(constantPool, cpi);
    }

    /**
     * Resolves a type at runtime and makes the necessary access checks.
     * @param cpi the constant pool index of the type constant
     * @return the compiler interface type resolved at that index
     */
    public RiType resolveType(char cpi) {
        return constantPool.classAt(cpi).resolve(constantPool, cpi);
    }

    /**
     * Looks up a type at the specified constant pool index, without performing
     * any resolution for unresolved types.
     * @param cpi the constant pool index
     * @return the compiler interface type at that index
     */
    public RiType lookupType(char cpi) {
        return typeFrom(constantPool.classAt(cpi), cpi);
    }

    public RiSignature lookupSignature(char cpi) {
        return SignatureDescriptor.create(constantPool.utf8At(cpi).string);
    }

    /**
     * Looks up a constant at the specified index, without performing any resolution.
     * @param cpi the constant pool index
     * @return the compiler interface constant at that index
     */
    public Object lookupConstant(char cpi) {
        switch (constantPool.tagAt(cpi)) {
            case CLASS: {
                RiType type = typeFrom(constantPool.classAt(cpi), cpi);
                if (type.isResolved()) {
                    return CiConstant.forObject(type.javaClass());
                }
                return type;
            }
            case INTEGER: {
                return CiConstant.forInt(constantPool.intAt(cpi));
            }
            case FLOAT: {
                return CiConstant.forFloat(constantPool.floatAt(cpi));
            }
            case STRING: {
                return CiConstant.forObject(constantPool.stringAt(cpi));
            }
            case LONG: {
                return CiConstant.forLong(constantPool.longAt(cpi));
            }
            case DOUBLE: {
                return CiConstant.forDouble(constantPool.doubleAt(cpi));
            }
            default:
                throw ProgramError.unexpected("unknown constant type");
        }
    }

    private RiField fieldFrom(FieldRefConstant constant, int cpi) {
        if (constant.isResolved() || attemptResolution(constant)) {
            // the resolution can occur without side effects
            try {
                return constant.resolve(constantPool, cpi);
            } catch (HostOnlyFieldError hostOnlyFieldError) {
                // Treat as unresolved
            }
        }
        RiType holder = TypeDescriptor.toRiType(constant.holder(constantPool), constantPool.holder());
        RiType type = TypeDescriptor.toRiType(constant.type(constantPool), constantPool.holder());
        String name = constant.name(constantPool).string;
        return new MaxUnresolvedField(constantPool, cpi, holder, name, type);
    }

    private RiMethod methodFrom(MethodRefConstant constant, int cpi) {
        if (constant.isResolved() || attemptResolution(constant)) {
            // the resolution can occur without side effects
            try {
                MethodActor methodActor = constant.resolve(constantPool, cpi);
                if (methodActor instanceof ClassMethodActor && ((ClassMethodActor) methodActor).isDeclaredFoldable()) {
                    C1XIntrinsic.registerFoldableMethod(methodActor, methodActor.toJava());
                }
                return methodActor;
            } catch (HostOnlyMethodError hostOnlyMethodError) {
                // Treat as unresolved
            }
        }
        RiType holder = TypeDescriptor.toRiType(constant.holder(constantPool), constantPool.holder());
        RiSignature signature = constant.signature(constantPool);
        String name = constant.name(constantPool).string;

        return new MaxUnresolvedMethod(constantPool, cpi, holder, name, signature);
    }

    private RiType typeFrom(ClassConstant constant, int cpi) {
        if (constant.isResolved() || attemptResolution(constant)) {
            // the resolution can occur without side effects
            return constant.resolve(constantPool, cpi);
        }
        return new MaxUnresolvedType(constant.typeDescriptor(), constantPool, cpi);
    }

    private boolean attemptResolution(ResolvableConstant constant) {
        if (C1XOptions.NormalCPEResolution) {
            C1XMetrics.ResolveCPEAttempts++;
            return constant.isResolvableWithoutClassLoading(constantPool);
        }
        return false;
    }

    public CiConstant encoding() {
        return CiConstant.forObject(this.constantPool);
    }
}
