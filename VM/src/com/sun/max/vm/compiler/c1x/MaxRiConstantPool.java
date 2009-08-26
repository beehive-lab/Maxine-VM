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

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.C1XMetrics;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * The <code>MaxRiConstantPool</code> class implements a constant pool for
 * the compiler interface, including support for looking up constants during
 * compilation, as well as runtime support for resolving constants in
 * the compiled code.
 * <p>
 * Note that all methods that begin with "<code>resolve</code>" are runtime calls
 * that actually perform resolution, and therefore have side effects. Calls to
 * these methods are inserted into the compiled code for unresolved constant
 * pool constants by the compiler.
 * <p>
 * All methods that begin with "<code>lookup</code>" are side-effect free. They
 * will only look up constant pool constants that are already resolved. They
 * are used by the compiler in looking up constants during compilation.
 *
 * @author Ben L. Titzer
 */
public class MaxRiConstantPool implements RiConstantPool {
    final MaxRiRuntime runtime;
    public final ConstantPool constantPool;
    final HashMap<SignatureDescriptor, MaxRiSignature> signatures = new HashMap<SignatureDescriptor, MaxRiSignature>();

    /**
     * Creates a new constant pool inside of the specified runtime for the specified constant pool.
     * @param runtime the runtime implementation
     * @param constantPool the actual constant pool contents
     */
    MaxRiConstantPool(MaxRiRuntime runtime, ConstantPool constantPool) {
        this.runtime = runtime;
        this.constantPool = constantPool;
    }

     // TODO: check for incompatible class changes in all resolution and lookup methods

    /**
     * Resolves a field reference for a getfield operation at runtime, and makes the
     * necessary runtime checks for getfield on the specified field.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField resolveGetField(char cpi) {
        return resolveField(cpi);
    }

    /**
     * Resolves a field reference for a putfield operation at runtime, and makes the
     * necessary runtime checks for putfield on the specified field.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField resolvePutField(char cpi) {
        return resolveField(cpi);
    }

    /**
     * Resolves a field reference for a getstatic operation at runtime, and makes the
     * necessary runtime checks for getstatic on the specified field.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField resolveGetStatic(char cpi) {
        return resolveField(cpi);
    }

    /**
     * Resolves a field reference for a putstatic operation at runtime, and makes the
     * necessary runtime checks for putstatic on the specified field.
     * (a call to this method is inserted into compiled code by the compiler)
     * @param cpi the constant pool index of the field reference
     * @return the compiler interface field resolved at that index
     */
    public RiField resolvePutStatic(char cpi) {
        return resolveField(cpi);
    }

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

    private MaxRiField resolveField(char cpi) {
        return canonicalRiField(constantPool.fieldAt(cpi).resolve(constantPool, cpi));
    }

    private MaxRiMethod resolveMethod(char cpi) {
        return canonicalRiMethod(constantPool.methodAt(cpi).resolve(constantPool, cpi));
    }

    /**
     * Resolves a type at runtime and makes the necessary access checks.
     * @param cpi the constant pool index of the type constant
     * @return the compiler interface type resolved at that index
     */
    public RiType resolveType(char cpi) {
        return canonicalRiType(constantPool.classAt(cpi).resolve(constantPool, cpi));
    }

    /**
     * Resolves a string constant at runtime.
     * @param cpi the constant pool index of the string constant
     * @return the string object resolved at that index
     */
    public String resolveString(char cpi) {
        return constantPool.stringAt(cpi);
    }

    /**
     * Resolves a class constant at runtime and makes the necessary access checks.
     * @param cpi the constant pool index
     * @return the class object for the class at that index
     */
    public Class<?> resolveClass(char cpi) {
        return constantPool.classAt(cpi).resolve(constantPool, cpi).mirror();
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

    /**
     * Looks up a constant at the specified index, without performing any resolution.
     * @param cpi the constant pool index
     * @return the compiler interface constant at that index
     */
    public Object lookupConstant(char cpi) {
        switch (constantPool.tagAt(cpi)) {
            case CLASS: {
                MaxRiType type = typeFrom(constantPool.classAt(cpi), cpi);
                if (type.isLoaded()) {
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

    private MaxRiField fieldFrom(FieldRefConstant constant, int cpi) {
        if (constant instanceof FieldRefConstant.Resolved) {
            // already resolved
            return canonicalRiField(((FieldRefConstant.Resolved) constant).fieldActor());
        } else if (attemptResolution(constant)) {
            // the resolution can occur without side effects
            return canonicalRiField(constant.resolve(constantPool, cpi));
        }
        return new MaxRiField(this, constant); // unresolved
    }

    private MaxRiMethod methodFrom(MethodRefConstant constant, int cpi) {
        if (constant instanceof ClassMethodRefConstant.Resolved) {
            // already resolved
            return canonicalRiMethod(((ClassMethodRefConstant.Resolved) constant).methodActor());
        } else if (constant instanceof InterfaceMethodRefConstant.Resolved) {
            // already resolved
            return canonicalRiMethod(((InterfaceMethodRefConstant.Resolved) constant).methodActor());
        } else if (attemptResolution(constant)) {
            // the resolution can occur without side effects
            return canonicalRiMethod(constant.resolve(constantPool, cpi));
        }
        return new MaxRiMethod(this, constant); // unresolved
    }

    private MaxRiType typeFrom(ClassConstant constant, int cpi) {
        if (constant instanceof ClassConstant.Resolved) {
            // already resolved
            return canonicalRiType(((ClassConstant.Resolved) constant).classActor);
        } else if (attemptResolution(constant)) {
            // the resolution can occur without side effects
            return canonicalRiType(constant.resolve(constantPool, cpi));
        }
        return new MaxRiType(this, constant); // unresolved
    }

    private boolean attemptResolution(ResolvableConstant constant) {
        if (C1XOptions.AggressivelyResolveCPEs) {
            C1XMetrics.ResolveCPEAttempts++;
            return constant.isResolvableWithoutClassLoading(constantPool);
        }
        return false;
    }

    /**
     * Canonicalizes resolved <code>MaxRiType</code> instances (per runtime), so
     * that the same <code>MaxRiType</code> instance is always returned for the
     * same <code>ClassActor</code>.
     * @param classActor the class actor for which to get the canonical type
     * @return the canonical compiler interface type for the class actor
     */
    public MaxRiType canonicalRiType(ClassActor classActor) {
        final MaxRiType type = new MaxRiType(this, classActor);
        synchronized (runtime) {
            // all resolved types are canonicalized per runtime instance
            final MaxRiType previous = runtime.types.get(type);
            if (previous == null) {
                runtime.types.put(type, type);
                return type;
            }
            return previous;
        }
    }

    /**
     * Canonicalizes resolved <code>MaxRiMethod</code> instances (per runtime), so
     * that the same <code>MaxRiMethod</code> instance is always returned for the
     * same <code>MethodActor</code>.
     * @param methodActor the mehtod actor for which to get the canonical type
     * @return the canonical compiler interface method for the method actor
     */
    public MaxRiMethod canonicalRiMethod(MethodActor methodActor) {
        final MaxRiMethod method = new MaxRiMethod(this, methodActor);
        synchronized (runtime) {
            // all resolved methods are canonicalized per runtime instance
            final MaxRiMethod previous = runtime.methods.get(method);
            if (previous == null) {
                runtime.methods.put(method, method);
                return method;
            }
            return previous;
        }
    }

    /**
     * Canonicalizes resolved <code>MaxRiFielde</code> instances (per runtime), so
     * that the same <code>MaxRiField</code> instance is always returned for the
     * same <code>FieldActor</code>.
     * @param fieldActor the field actor for which to get the canonical type
     * @return the canonical compiler interface field for the field actor
     */
    public MaxRiField canonicalRiField(FieldActor fieldActor) {
        final MaxRiField field = new MaxRiField(this, fieldActor);
        synchronized (runtime) {
            // all resolved field are canonicalized per runtime instance
            final MaxRiField previous = runtime.fields.get(field);
            if (previous == null) {
                runtime.fields.put(field, field);
                return field;
            }
            return previous;
        }
    }

    /**
     * Caches the compiler interface signature objects (per constant pool), to
     * reduce the amount of decoding done for repeated uses of the same signature.
     * @param descriptor the signature descriptor.
     * @return the cached compiler interface signature object
     */
    public synchronized MaxRiSignature cacheSignature(SignatureDescriptor descriptor) {
        MaxRiSignature signature = signatures.get(descriptor);
        if (signature == null) {
            signature = new MaxRiSignature(this, descriptor);
            signatures.put(descriptor, signature);
        }
        return signature;
    }

    /**
     * Creates new a new compiler interface exception handler.
     * @param startBCI the start bytecode index of the protected range
     * @param endBCI the end bytecode index of the protected range
     * @param catchBCI the bytecode index of the handler block
     * @param classCPI the index into the constant pool for the catch class
     * @return a compiler interface exception handler object
     */
    public RiExceptionHandler newExceptionHandler(int startBCI, int endBCI, int catchBCI, int classCPI) {
        return new MaxRiExceptionHandler(startBCI, endBCI, catchBCI, classCPI, (classCPI == 0) ? null : resolveType((char) classCPI));
    }

    @Override
    public CiConstant encoding() {
        return CiConstant.forObject(this.constantPool);
    }
}
