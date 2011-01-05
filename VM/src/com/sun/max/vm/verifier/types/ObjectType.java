/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.verifier.types;

import java.io.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
 * @author David Liu
 * @author Doug Simon
 */
public class ObjectType extends ReferenceType {

    private final TypeDescriptor typeDescriptor;
    final Verifier verifier;

    /**
     * Only called by {@link NullType#NullType()}.
     */
    ObjectType() {
        assert getClass() == NullType.class;
        typeDescriptor = null;
        verifier = null;
    }

    public ObjectType(TypeDescriptor typeDescriptor, Verifier verifier) {
        assert verifier != null || this instanceof ResolvedType;
        assert typeDescriptor != null;
        assert !JavaTypeDescriptor.isPrimitive(typeDescriptor);
        this.typeDescriptor = typeDescriptor;
        this.verifier = verifier;
    }

    @Override
    public TypeDescriptor typeDescriptor() {
        return typeDescriptor;
    }

    @Override
    public boolean isAssignableFromDifferentType(VerificationType from) {
        // Any object class is assignable from null
        if (from == NULL) {
            return true;
        }

        if (!(from instanceof ObjectType)) {
            return false;
        }

        if (this == OBJECT) {
            return true;
        }

        final ClassActor thisActor = resolve();
        if (thisActor.isInterface()) {
            // Interfaces (including java.lang.Cloneable and java.io.Serializable) are treated as java.lang.Object
            return true;
        }

        final ObjectType fromObject = (ObjectType) from;
        if (fromObject == OBJECT) {
            return false;
        }

        final ClassActor fromActor = fromObject.resolve();
        return thisActor.isAssignableFrom(fromActor);
    }

    @Override
    protected VerificationType mergeWithDifferentType(VerificationType from) {
        // value is null (uninitialized) or is not a reference type, return bogus.
        if (!(from instanceof ObjectType)) {
            return TOP;
        }

        // "this" is null type (a subtype of all other reference types) return 'from'.
        if (this == NULL) {
            return from;
        }

        // "this" is java.lang.Object, java.lang.Cloneable or java.lang.Serializable, return this.
        if (this == OBJECT || this == CLONEABLE || this == SERIALIZABLE) {
            return this;
        }

        // Array and object type merged to java.lang.Object.
        if (from.isArray()) {
            return OBJECT;
        }

        final ObjectType fromObject = (ObjectType) from;
        // value is null reference, return this type.
        if (fromObject == NULL) {
            return this;
        }

        final ClassActor classActor = resolve();
        if (from == OBJECT) {
            return from;
        }

        // Now both are non-array object types. Neither is java.lang.Object or null type.
        ClassActor fromClassActor = fromObject.resolve();

        // Treat interfaces as if they were java.lang.Object.
        if (classActor.isInterface()) {
            return OBJECT;
        }
        if (fromClassActor.isInterface()) {
            return OBJECT;
        }

        if (fromClassActor.kind != classActor.kind) {
            // Only happens when exactly one of the type is a Word type
            return TOP;
        }

        ClassActor fromSuperClassActor;

        // Find out whether the classes are deeper in the class tree by moving both
        // toward the root, and see who gets there first.
        ClassActor thisSuperClassActor = classActor.superClassActor;
        fromSuperClassActor = fromClassActor.superClassActor;
        while ((thisSuperClassActor != null) && (fromSuperClassActor != null)) {
            // If either hits the other when going up looking for a parent, then return the parent.
            if (fromSuperClassActor.equals(classActor)) {
                return this;
            }
            if (thisSuperClassActor.equals(fromClassActor)) {
                return from;
            }
            thisSuperClassActor = thisSuperClassActor.superClassActor;
            fromSuperClassActor = fromSuperClassActor.superClassActor;
        }

        // At most one of the following two while clauses will be executed.
        // Bring the deeper of thisClass and fromClass to the depth of the shallower one.
        ClassActor thisClassActor = classActor;
        while (thisSuperClassActor != null) {
            // thisClass is deeper
            thisClassActor = thisClassActor.superClassActor;
            thisSuperClassActor = thisSuperClassActor.superClassActor;
        }
        while (fromSuperClassActor != null) {
            // fromClass is deeper
            fromClassActor = fromClassActor.superClassActor;
            fromSuperClassActor = fromSuperClassActor.superClassActor;
        }

        // Walk both up, maintaining equal depth, until a join is found.
        // We know that we'll always find one.
        while (!thisClassActor.equals(fromClassActor)) {
            thisClassActor = thisClassActor.superClassActor;
            fromClassActor = fromClassActor.superClassActor;
        }

        if (thisClassActor.equals(ClassRegistry.OBJECT)) {
            return OBJECT;
        }

        return verifier.getObjectType(thisClassActor.typeDescriptor);
    }

    public ClassActor resolve() {
        return verifier.resolve(typeDescriptor());
    }

    @Override
    public int classfileTag() {
        return ITEM_Object;
    }

    @Override
    public void writeInfo(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeShort(constantPoolEditor.indexOf(PoolConstantFactory.createClassConstant(typeDescriptor), true));
    }

    @Override
    public String toString() {
        return typeDescriptor.toJavaString();
    }
}
