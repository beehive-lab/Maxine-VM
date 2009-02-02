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
package com.sun.max.tele.object;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.interpret.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * Singleton manages the creation and maintenance of instances of {@link TeleObject}, each of which is a
 * canonical surrogate for a heap object in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class TeleObjectFactory extends AbstractTeleVMHolder{

    private static final int TRACE_VALUE = 2;

    private static TeleObjectFactory _teleObjectFactory;

    /**
     * @return the singleton manager for instances of {@link TeleObject}.
     */
    public static TeleObjectFactory make(TeleVM teleVM) {
        if (_teleObjectFactory == null) {
            _teleObjectFactory = new TeleObjectFactory(teleVM);
        }
        return _teleObjectFactory;
    }

    // TODO (mlvdv)  TeleObject weak references

    /**
     * Map: Reference to {@link Object}s in the {@link TeleVM} --> canonical local {@link TeleObject} that represents the
     * object in the {@link TeleVM}. Relies on References being canonical and GC-safe.
     */
    private  final GrowableMapping<Reference, TeleObject> _referenceToTeleObject = HashMapping.createIdentityMapping();

    /**
     * Map: OID --> {@link TeleObject}.
     */
    private final GrowableMapping<Long, TeleObject> _oidToTeleObject = HashMapping.createEqualityMapping();

    /**
     * Constructors for specific classes of tuple objects in the heap in the {@teleVM}.
     * The most specific class that matches a particular {@link TeleObject} will
     * be used, in an emulation of virtual method dispatch.
     */
    private final Map<Class, Constructor> _classToTeleTupleObjectConstructor = new HashMap<Class, Constructor>();

    private TeleObjectFactory(TeleVM teleVM) {
        super(teleVM);
        Trace.begin(1, tracePrefix() + "initializing");
        final long startTimeMillis = System.currentTimeMillis();
        // Representation for all tuple objects not otherwise mentioned
        _classToTeleTupleObjectConstructor.put(Object.class, getConstructor(TeleTupleObject.class));
        // Some common Java classes
        _classToTeleTupleObjectConstructor.put(String.class, getConstructor(TeleString.class));
        _classToTeleTupleObjectConstructor.put(Enum.class, getConstructor(TeleEnum.class));
        _classToTeleTupleObjectConstructor.put(ClassLoader.class, getConstructor(TeleClassLoader.class));
        // Maxine Actors
        _classToTeleTupleObjectConstructor.put(FieldActor.class, getConstructor(TeleFieldActor.class));
        _classToTeleTupleObjectConstructor.put(VirtualMethodActor.class, getConstructor(TeleVirtualMethodActor.class));
        _classToTeleTupleObjectConstructor.put(StaticMethodActor.class, getConstructor(TeleStaticMethodActor.class));
        _classToTeleTupleObjectConstructor.put(InterfaceMethodActor.class, getConstructor(TeleInterfaceMethodActor.class));
        _classToTeleTupleObjectConstructor.put(InterfaceActor.class, getConstructor(TeleInterfaceActor.class));
        _classToTeleTupleObjectConstructor.put(VmThread.class, getConstructor(TeleVmThread.class));
        _classToTeleTupleObjectConstructor.put(PrimitiveClassActor.class, getConstructor(TelePrimitiveClassActor.class));
        _classToTeleTupleObjectConstructor.put(ArrayClassActor.class, getConstructor(TeleArrayClassActor.class));
        _classToTeleTupleObjectConstructor.put(ReferenceClassActor.class, getConstructor(TeleReferenceClassActor.class));
        // Maxine code management
        _classToTeleTupleObjectConstructor.put(JitTargetMethod.class, getConstructor(TeleJitTargetMethod.class));
        _classToTeleTupleObjectConstructor.put(OptimizedTargetMethod.class, getConstructor(TeleOptimizedTargetMethod.class));
        _classToTeleTupleObjectConstructor.put(RuntimeStub.class, getConstructor(TeleRuntimeStub.class));
        _classToTeleTupleObjectConstructor.put(CodeRegion.class, getConstructor(TeleCodeRegion.class));
        _classToTeleTupleObjectConstructor.put(CodeManager.class, getConstructor(TeleCodeManager.class));
        _classToTeleTupleObjectConstructor.put(RuntimeMemoryRegion.class, getConstructor(TeleRuntimeMemoryRegion.class));
        _classToTeleTupleObjectConstructor.put(InterpretedTargetMethod.class, getConstructor(TeleInterpretedTargetMethod.class));
        // Other Maxine support
        _classToTeleTupleObjectConstructor.put(Kind.class, getConstructor(TeleKind.class));
        _classToTeleTupleObjectConstructor.put(ObjectReferenceValue.class, getConstructor(TeleObjectReferenceValue.class));
        _classToTeleTupleObjectConstructor.put(Builtin.class, getConstructor(TeleBuiltin.class));
        // ConstantPool and PoolConstants
        _classToTeleTupleObjectConstructor.put(ConstantPool.class, getConstructor(TeleConstantPool.class));
        _classToTeleTupleObjectConstructor.put(CodeAttribute.class, getConstructor(TeleCodeAttribute.class));
        _classToTeleTupleObjectConstructor.put(PoolConstant.class, getConstructor(TelePoolConstant.class));
        _classToTeleTupleObjectConstructor.put(Utf8Constant.class, getConstructor(TeleUtf8Constant.class));
        _classToTeleTupleObjectConstructor.put(StringConstant.class, getConstructor(TeleStringConstant.class));
        _classToTeleTupleObjectConstructor.put(ClassConstant.Resolved.class, getConstructor(TeleClassConstant.Resolved.class));
        _classToTeleTupleObjectConstructor.put(ClassConstant.Unresolved.class, getConstructor(TeleClassConstant.Unresolved.class));
        _classToTeleTupleObjectConstructor.put(FieldRefConstant.Resolved.class, getConstructor(TeleFieldRefConstant.Resolved.class));
        _classToTeleTupleObjectConstructor.put(FieldRefConstant.Unresolved.class, getConstructor(TeleFieldRefConstant.Unresolved.class));
        _classToTeleTupleObjectConstructor.put(FieldRefConstant.UnresolvedIndices.class, getConstructor(TeleFieldRefConstant.UnresolvedIndices.class));
        _classToTeleTupleObjectConstructor.put(ClassMethodRefConstant.Resolved.class, getConstructor(TeleClassMethodRefConstant.Resolved.class));
        _classToTeleTupleObjectConstructor.put(ClassMethodRefConstant.Unresolved.class, getConstructor(TeleClassMethodRefConstant.Unresolved.class));
        _classToTeleTupleObjectConstructor.put(ClassMethodRefConstant.UnresolvedIndices.class, getConstructor(TeleClassMethodRefConstant.UnresolvedIndices.class));
        _classToTeleTupleObjectConstructor.put(InterfaceMethodRefConstant.Resolved.class, getConstructor(TeleInterfaceMethodRefConstant.Resolved.class));
        _classToTeleTupleObjectConstructor.put(InterfaceMethodRefConstant.Unresolved.class, getConstructor(TeleInterfaceMethodRefConstant.Unresolved.class));
        _classToTeleTupleObjectConstructor.put(InterfaceMethodRefConstant.UnresolvedIndices.class, getConstructor(TeleInterfaceMethodRefConstant.UnresolvedIndices.class));
        // Java language objects
        _classToTeleTupleObjectConstructor.put(Class.class, getConstructor(TeleClass.class));
        _classToTeleTupleObjectConstructor.put(Constructor.class, getConstructor(TeleConstructor.class));
        _classToTeleTupleObjectConstructor.put(Field.class, getConstructor(TeleField.class));
        _classToTeleTupleObjectConstructor.put(Method.class, getConstructor(TeleMethod.class));
        _classToTeleTupleObjectConstructor.put(TypeDescriptor.class, getConstructor(TeleTypeDescriptor.class));
        _classToTeleTupleObjectConstructor.put(SignatureDescriptor.class, getConstructor(TeleSignatureDescriptor.class));

        Trace.end(1, tracePrefix() + "initializing", startTimeMillis);
    }

    private Constructor getConstructor(Class clazz) {
        return Classes.getDeclaredConstructor(clazz, TeleVM.class, Reference.class);
    }

    /**
     * Factory method for canonical {@link TeleObject} surrogate for heap objects in the {@link teleVM}. Special subclasses are
     * created for Maxine implementation objects of special interest, and for other objects for which special treatment
     * is desired.
     *
     * Returns null for the distinguished zero {@link Reference}.
     *
     * Care is taken to avoid I/O with the {@link TeleVM} during synchronized
     * access to the canonicalization map.  There is a small exception
     * to this for {@link TeleTargetMethod}.
     * @param reference a Java object in the {@link TeleVM}
     *
     * @return canonical local surrogate for the object
     */
    public TeleObject make(Reference reference) {
        assert reference != null;
        if (reference.isZero()) {
            return null;
        }
        TeleObject teleObject = null;
        synchronized (_referenceToTeleObject) {
            teleObject = _referenceToTeleObject.get(reference);
        }
        if (teleObject != null) {
            return teleObject;
        }
        // Keep all the {@link TeleVM} traffic outside of synchronization.
        if (!teleVM().isValidOrigin(reference.toOrigin())) {
            return null;
        }

        final Reference hubReference = teleVM().wordToReference(teleVM().layoutScheme().generalLayout().readHubReferenceAsWord(reference));
        final Reference classActorReference = teleVM().fields().Hub_classActor.readReference(hubReference);
        final ClassActor classActor = teleVM().makeClassActor(classActorReference);

        // Must check for the static tuple case first; it doesn't follow the usual rules
        final Reference hubhubReference = teleVM().wordToReference(teleVM().layoutScheme().generalLayout().readHubReferenceAsWord(hubReference));
        final Reference hubClassActorReference = teleVM().fields().Hub_classActor.readReference(hubhubReference);
        final ClassActor hubClassActor = teleVM().makeClassActor(hubClassActorReference);
        final Class hubJavaClass = hubClassActor.toJava();  // the class of this object's hub
        if (StaticHub.class.isAssignableFrom(hubJavaClass)) {
            //teleObject = new TeleStaticTuple(teleVM(), reference);       ?????????
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {
                    teleObject = new TeleStaticTuple(teleVM(), reference);
                }
            }
        } else if (classActor.isArrayClassActor()) {
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {
                    teleObject = new TeleArrayObject(teleVM(), reference);
                }
            }
        } else if (classActor.isHybridClassActor()) {
            final Class javaClass = classActor.toJava();
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {
                    if (DynamicHub.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleDynamicHub(teleVM(), reference);
                    } else if (StaticHub.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleStaticHub(teleVM(), reference);
                    } else {
                        Problem.error("invalid hybrid implementation type");
                    }
                }
            }
        } else if (classActor.isTupleClassActor()) {
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {
                    final Constructor constructor = lookupTeleTupleObjectConstructor(classActor);
                    try {
                        teleObject = (TeleObject) constructor.newInstance(teleVM(), reference);
                    } catch (InstantiationException e) {
                        throw ProgramError.unexpected();
                    } catch (IllegalAccessException e) {
                        throw ProgramError.unexpected();
                    } catch (InvocationTargetException e) {
                        throw ProgramError.unexpected();
                    }
                }
            }
        } else {
            Problem.error("invalid object implementation type");
        }

        _oidToTeleObject.put(teleObject.getOID(), teleObject);
        assert _oidToTeleObject.containsKey(teleObject.getOID());

        _referenceToTeleObject.put(reference, teleObject);
        return teleObject;
    }

    private Constructor lookupTeleTupleObjectConstructor(ClassActor classActor) {
        Class javaClass = classActor.toJava();
        while (javaClass != null) {
            final Constructor constructor = _classToTeleTupleObjectConstructor.get(javaClass);
            if (constructor != null) {
                return constructor;
            }
            javaClass = javaClass.getSuperclass();
        }
        ProgramError.unexpected("TeleObjectFactory failed to find constructor for class" + classActor.toJava());
        return null;
    }

    /**
     * @return the {@link TeleObject} with specified OID.
     */
    public TeleObject lookupObject(long id) {
        return _oidToTeleObject.get(id);
    }

    private int _previousTeleObjectCount = 0;

    public void refresh(long processEpoch) {
        Trace.begin(TRACE_VALUE, tracePrefix() + "refreshing");
        final long startTimeMillis = System.currentTimeMillis();
        for (TeleObject teleObject : _referenceToTeleObject.values()) {
            teleObject.refresh(processEpoch);
        }
        final int currentTeleObjectCount = _referenceToTeleObject.length();
        final StringBuilder sb = new StringBuilder(100);
        sb.append(tracePrefix());
        sb.append("refreshing, count=").append(Integer.toString(currentTeleObjectCount));
        sb.append("  new=").append(Integer.toString(currentTeleObjectCount - _previousTeleObjectCount));
        Trace.end(TRACE_VALUE, sb.toString(), startTimeMillis);
        _previousTeleObjectCount = currentTeleObjectCount;
    }

}
