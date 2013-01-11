/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vm.ext.graal;

import static com.oracle.max.vm.ext.graal.MaxGraal.unimplemented;

import java.lang.annotation.*;

import com.oracle.graal.api.meta.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.holder.*;


public class MaxResolvedJavaType extends MaxJavaType implements ResolvedJavaType {

    protected MaxResolvedJavaType(RiResolvedType riResolvedType) {
        super(riResolvedType);
    }

    public static MaxResolvedJavaType get(RiResolvedType riResolvedType) {
        return (MaxResolvedJavaType) MaxJavaType.get(riResolvedType);
    }

    public static RiResolvedType get(ResolvedJavaType resolvedJavaType) {
        return (RiResolvedType) MaxJavaType.get(resolvedJavaType);
    }

    private RiResolvedType riResolvedType() {
        return (RiResolvedType) riType;
    }

    @Override
    public Constant getEncoding(Representation r) {
        unimplemented("MaxResolvedType.getEncoding");
        //return riResolvedType.getEncoding(r);
        return null;
    }

    @Override
    public boolean hasFinalizer() {
        return riResolvedType().hasFinalizer();
    }

    @Override
    public boolean hasFinalizableSubclass() {
        return riResolvedType().hasFinalizableSubclass();
    }

    @Override
    public boolean isInterface() {
        return riResolvedType().isInterface();
    }

    @Override
    public boolean isInstanceClass() {
        return riResolvedType().isInstanceClass();
    }

    @Override
    public boolean isArray() {
        return riResolvedType().isArrayClass();
    }

    @Override
    public boolean isPrimitive() {
        ClassActor ca = (ClassActor) riType;
        return ca.isPrimitiveClassActor();
    }

    @Override
    public int getModifiers() {
        return riResolvedType().accessFlags();
    }

    @Override
    public boolean isInitialized() {
        return riResolvedType().isInitialized();
    }

    @Override
    public void initialize() {
        unimplemented("MaxResolvedType.initialize");
    }

    @Override
    public boolean isAssignableFrom(ResolvedJavaType other) {
        return (MaxResolvedJavaType.get(other)).isSubtypeOf(riResolvedType());
    }

    @Override
    public boolean isInstance(Constant obj) {
        ClassActor classActor = (ClassActor) riType;
        return classActor.isInstance(obj.asObject());
    }

    @Override
    public ResolvedJavaType asExactType() {
        return MaxResolvedJavaType.get(riResolvedType().exactType());
    }

    @Override
    public ResolvedJavaType getSuperclass() {
        return MaxResolvedJavaType.get(riResolvedType().superType());
    }

    @Override
    public ResolvedJavaType[] getInterfaces() {
        unimplemented("MaxResolvedType.getInterfaces");
        return null;
    }

    @Override
    public ResolvedJavaType findLeastCommonAncestor(ResolvedJavaType otherType) {
        unimplemented("MaxResolvedType.findLeastCommonAncestor");
        return null;
    }

    @Override
    public ResolvedJavaType findUniqueConcreteSubtype() {
        RiResolvedType riResolvedType = riResolvedType().uniqueConcreteSubtype();
        if (riResolvedType == null) {
            return null;
        } else {
            return MaxResolvedJavaType.get(riResolvedType);
        }

    }

    @Override
    public ResolvedJavaType getArrayClass() {
        return (ResolvedJavaType) super.getArrayClass();
    }

    @Override
    public ResolvedJavaType getComponentType() {
        return (ResolvedJavaType) super.getComponentType();
    }

    @Override
    public ResolvedJavaMethod resolveMethod(ResolvedJavaMethod method) {
        MaxResolvedJavaMethod maxMethod = (MaxResolvedJavaMethod) method;
        return MaxResolvedJavaMethod.get(riResolvedType().resolveMethodImpl((RiResolvedMethod) maxMethod.riMethod));
    }

    @Override
    public ResolvedJavaMethod findUniqueConcreteMethod(ResolvedJavaMethod method) {
        RiResolvedMethod riMethod = riResolvedType().uniqueConcreteMethod(MaxResolvedJavaMethod.get(method));
        if (riMethod == null) {
            return null;
        } else {
            return MaxResolvedJavaMethod.get(riMethod);
        }

    }

    @Override
    public ResolvedJavaField[] getInstanceFields(boolean includeSuperclasses) {
        RiResolvedType riResolvedType = riResolvedType();
        ResolvedJavaField[] superClassJavaFields = null;
        if (includeSuperclasses) {
            RiResolvedType superType = riResolvedType.superType();
            if (superType != null) {
                superClassJavaFields = MaxResolvedJavaType.get(superType).getInstanceFields(true);
            }
        }
        RiResolvedField[] fields = riResolvedType.declaredFields();
        ResolvedJavaField[] javaFields = new ResolvedJavaField[fields.length + (superClassJavaFields == null ? 0 : superClassJavaFields.length)];
        int x = 0;
        if (superClassJavaFields != null) {
            x = superClassJavaFields.length;
            System.arraycopy(superClassJavaFields, 0, javaFields, 0, x);
        }
        for (int i = 0; i < fields.length; i++) {
            javaFields[x + i] = MaxResolvedJavaField.get(fields[i]);
        }
        return javaFields;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        unimplemented("MaxResolvedType.getAnnotation");
        return null;
    }

    @Override
    public ResolvedJavaField findInstanceFieldWithOffset(long offset) {
        ClassActor ca = (ClassActor) riType;
        return MaxResolvedJavaField.get(ca.findInstanceFieldActor((int) offset));
    }

}
