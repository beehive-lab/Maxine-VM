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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.verifier.types.VerificationType.*;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.types.*;

/**
 *
 * @author Doug Simon
 */
public class Verifier implements VerificationRegistry {

    private final ConstantPool _constantPool;

    private final Map<TypeDescriptor, ObjectType> _objectTypes;

    private final IntHashMap<UninitializedNewType> _uninitializedNewTypes;

    private IntHashMap<Subroutine> _subroutines;

    private boolean _verbose;

    public Verifier(ConstantPool constantPool) {
        _constantPool = constantPool;
        _objectTypes = new HashMap<TypeDescriptor, ObjectType>();
        _uninitializedNewTypes = new IntHashMap<UninitializedNewType>();

        for (ObjectType objectType : PREDEFINED_OBJECT_TYPES) {
            _objectTypes.put(objectType.typeDescriptor(), objectType);
        }
    }

    public ObjectType getObjectType(TypeDescriptor typeDescriptor) {
        if (JavaTypeDescriptor.isPrimitive(typeDescriptor)) {
            return null;
        }
        ObjectType objectType = _objectTypes.get(typeDescriptor);
        if (objectType == null) {
            objectType = JavaTypeDescriptor.isArray(typeDescriptor) ? new ArrayType(typeDescriptor, this) : new ObjectType(typeDescriptor, this);
            _objectTypes.put(typeDescriptor, objectType);
        }
        return objectType;
    }

    public UninitializedNewType getUninitializedNewType(int position) {
        UninitializedNewType uninitializedNewType = _uninitializedNewTypes.get(position);
        if (uninitializedNewType == null) {
            uninitializedNewType = new UninitializedNewType(position);
            _uninitializedNewTypes.put(position, uninitializedNewType);
        }
        return uninitializedNewType;
    }

    public int clearSubroutines() {
        if (_subroutines != null) {
            final int count = _subroutines.count();
            _subroutines = null;
            return count;
        }
        return 0;
    }

    public Subroutine getSubroutine(int entryPosition, int maxLocals) {
        if (_subroutines == null) {
            _subroutines = new IntHashMap<Subroutine>();
        }
        Subroutine subroutine = _subroutines.get(entryPosition);
        if (subroutine == null) {
            subroutine = new Subroutine(entryPosition, maxLocals);
            _subroutines.put(entryPosition, subroutine);
        }
        return subroutine;
    }

    public VerificationType getVerificationType(TypeDescriptor typeDescriptor) {
        return VerificationType.getVerificationType(typeDescriptor, this);
    }

    public ConstantPool constantPool() {
        return _constantPool;
    }

    public static ClassVerifier verifierFor(ClassActor classActor) {
        final int majorVersion = classActor.majorVersion();
        if (majorVersion >= 50) {
            final boolean failOverToOldVerifier = majorVersion == 50;
            return new TypeCheckingVerifier(classActor, failOverToOldVerifier);
        }
        return new TypeInferencingVerifier(classActor);
    }

    /**
     * Resolves a given TypeDescriptor to a class actor.
     */
    public ClassActor resolve(TypeDescriptor type) {
        return ClassActor.fromJava(type.resolveType(constantPool().classLoader()));
    }

    public boolean verbose() {
        return _verbose;
    }

    public void setVerbose(boolean flag) {
        _verbose = flag;
    }
}
