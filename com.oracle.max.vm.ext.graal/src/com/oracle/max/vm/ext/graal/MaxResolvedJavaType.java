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
import static com.sun.max.vm.type.KindEnum.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import com.oracle.graal.api.meta.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.reference.*;


public class MaxResolvedJavaType extends MaxJavaType implements ResolvedJavaType {

    private static MaxResolvedJavaType objectType;
    private static ClassActor objectClassActor;
    private static MaxResolvedJavaType wrappedWordType;
    private static MaxResolvedJavaType wordType;
    private static MaxResolvedJavaType pointerType;
    private static MaxResolvedJavaType accessorType;
    private static MaxResolvedJavaType referenceType;
    private static MaxResolvedJavaType codePointerType;
    private static MaxResolvedJavaType hubType;

    public static void init() {
        objectClassActor = ClassActor.fromJava(Object.class);
        objectType = MaxResolvedJavaType.get(objectClassActor);
        pointerType = MaxResolvedJavaType.get(ClassActor.fromJava(Pointer.class));
        accessorType = MaxResolvedJavaType.get(ClassActor.fromJava(Accessor.class));
        referenceType = MaxResolvedJavaType.get(ClassActor.fromJava(Reference.class));
        wordType = MaxResolvedJavaType.get(ClassActor.fromJava(Word.class));
        codePointerType = MaxResolvedJavaType.get(ClassActor.fromJava(CodePointer.class));
        wrappedWordType = MaxResolvedJavaType.get(ClassActor.fromJava(WordUtil.WrappedWord.class));
        hubType = MaxResolvedJavaType.get(ClassActor.fromJava(Hub.class));
    }

    protected MaxResolvedJavaType(RiResolvedType riResolvedType) {
        super(riResolvedType);
    }

    public static MaxResolvedJavaType get(RiResolvedType riResolvedType) {
        return (MaxResolvedJavaType) MaxJavaType.get(riResolvedType);
    }

    public static RiResolvedType getRiResolvedType(ResolvedJavaType resolvedJavaType) {
        return (RiResolvedType) MaxJavaType.getRiType(resolvedJavaType);
    }

    public static MaxResolvedJavaType getJavaLangObject() {
        assert objectType != null;
        return objectType;
    }

    public static MaxResolvedJavaType getPointerType() {
        assert pointerType != null;
        return pointerType;
    }

    public static MaxResolvedJavaType getAccessorType() {
        assert accessorType != null;
        return accessorType;
    }

    public static MaxResolvedJavaType getWordType() {
        assert wordType != null;
        return wordType;
    }

    public static MaxResolvedJavaType getReferenceType() {
        assert referenceType != null;
        return referenceType;
    }

    public static MaxResolvedJavaType getCodePointerType() {
        assert codePointerType != null;
        return codePointerType;
    }

    public static MaxResolvedJavaType getWrappedWordType() {
        assert wrappedWordType != null;
        return wrappedWordType;
    }

    public static MaxResolvedJavaType getHubType() {
        assert hubType != null;
        return hubType;
    }

    private RiResolvedType riResolvedType() {
        return (RiResolvedType) riType;
    }

    private static com.sun.cri.ri.RiType.Representation toCiRepresentation(Representation r) {
        // Checkstyle: stop
        switch (r) {
            case JavaClass: return com.sun.cri.ri.RiType.Representation.JavaClass;
            case ObjectHub: return com.sun.cri.ri.RiType.Representation.ObjectHub;
            default: return null;
        }
        // Checkstyle: resume
    }

    @Override
    public Constant getEncoding(Representation r) {
        CiConstant encoding = ((ClassActor) riResolvedType()).getEncoding(toCiRepresentation(r));
        return ConstantMap.toGraal(encoding);
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
        return (MaxResolvedJavaType.getRiResolvedType(other)).isSubtypeOf(riResolvedType());
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

    private MaxResolvedJavaType getSuperType() {
        if (isArray()) {
            ResolvedJavaType componentType = getComponentType();
            if (componentType.isPrimitive() || componentType == objectType) {
                return objectType;
            }
            return (MaxResolvedJavaType) ((MaxResolvedJavaType) componentType).getSuperType().getArrayClass();
        }
        return MaxResolvedJavaType.get(riResolvedType().superType());
    }

    @Override
    public ResolvedJavaType findLeastCommonAncestor(final ResolvedJavaType otherType) {
        final MaxResolvedJavaType thisType = this;
        return findLeastCommonAncestor(thisType, otherType);
        /*
        return Debug.scope("MaxResolvedJavaType.FindLeastCommonAncestor", new Callable<ResolvedJavaType>() {

            public ResolvedJavaType call() throws Exception {
                ResolvedJavaType result = findLeastCommonAncestor(thisType, otherType);
                Debug.log("findLeastCommonAncestor %s, %s -> %s", thisType, otherType, result);
                return result;                /*
            }

        });
        */
    }

    private static ResolvedJavaType findLeastCommonAncestor(MaxResolvedJavaType thisType, ResolvedJavaType otherTypeA) {
        MaxResolvedJavaType otherType = (MaxResolvedJavaType) otherTypeA;

        if (otherType.isPrimitive()) {
            return null;
        }

        if (MaxineVM.isHosted()) {
            if (thisType == wrappedWordType) {
                thisType = wordType;
            }
            if (otherType == wrappedWordType) {
                otherType = wordType;
            }

            com.sun.max.vm.type.KindEnum thisKind = ((ClassActor) thisType.riType).kind.asEnum;
            com.sun.max.vm.type.KindEnum otherKind = ((ClassActor) otherType.riType).kind.asEnum;

            // Word types occupy a parallel universe; they have their own hierarchy but they do not mix with other types
            if ((thisKind == WORD && otherKind != WORD) || (otherKind == WORD && thisKind != WORD)) {
                return null;
            }
            // Reference and CodePointer types are singletons and do not mix with anything else
            if (thisType == referenceType || otherType == referenceType) {
                if (thisType == otherType) {
                    return thisType;
                } else {
                    return null;
                }
            }
            if (thisType == codePointerType || otherType == codePointerType) {
                if (thisType == otherType) {
                    return thisType;
                } else {
                    return null;
                }
            }
        }

        if (thisType == objectType || otherType == objectType) {
            return objectType;
        }

        // now the normal algorithm looking up the class hierarchy, checking assignability
        MaxResolvedJavaType t1 = thisType;
        MaxResolvedJavaType t2 = otherType;
        while (true) {
            if (t1.isAssignableFrom(t2)) {
                return t1;
            }
            if (t2.isAssignableFrom(t1)) {
                return t2;
            }
            t1 = t1.getSuperType();
            t2 = t2.getSuperType();
        }
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
    public ResolvedJavaMethod resolveMethod(final ResolvedJavaMethod method) {
        final MaxResolvedJavaType thisType = this;
        return resolveMethod(thisType, method);
        /*
        return Debug.scope("MaxResolvedJavaType.ResolvedMethod", new Callable<ResolvedJavaMethod>() {

            public ResolvedJavaMethod call() throws Exception {
                ResolvedJavaMethod result = resolveMethod(thisType, method);
                Debug.log("resolveMethod %s -> %s", method, result);
                return result;
            }

        });
        */

    }

    private static ResolvedJavaMethod resolveMethod(MaxResolvedJavaType type, ResolvedJavaMethod method) {
        // It is not clear whether it is required that method is actually a member of this type.
        // Certainly, ConditionalElimination.node can call it (as of 8/2/13) when that is not the
        // case. Currently ClassActor.resolveMethodImpl expects this invariant, so we must
        // check explicitly. Also if we are dealing with interfaces, the implementation may not be
        // known and, again, resolveMethodImpl doesn't expect that.
        MaxResolvedJavaMethod maxMethod = (MaxResolvedJavaMethod) method;
        ClassActor classActor = (ClassActor) type.riType;
        MethodActor methodActor = (MethodActor) maxMethod.riMethod;
        VirtualMethodActor match = null;
        // name/descriptor are canonical, so identity comparison ok
        for (VirtualMethodActor vma : classActor.allVirtualMethodActors()) {
            if (vma.name == methodActor.name && vma.descriptor == methodActor.descriptor) {
                match = vma;
                break;
            }
        }
        if (match == null) {
            return null;
        }
        return MaxResolvedJavaMethod.get(type.riResolvedType().resolveMethodImpl((RiResolvedMethod) maxMethod.riMethod));
    }

    @Override
    public ResolvedJavaMethod findUniqueConcreteMethod(ResolvedJavaMethod method) {
        RiResolvedMethod riMethod = riResolvedType().uniqueConcreteMethod(MaxResolvedJavaMethod.getRiResolvedMethod(method));
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

    @Override
    public String toString() {
        return riType.toString();
    }

    @Override
    public String getSourceFileName() {
        ClassActor ca = (ClassActor) riType;
        return ca.sourceFileName;
    }

    @Override
    public URL getClassFilePath() {
        return null;
    }

    @Override
    public boolean isLocal() {
        return ((ClassActor) riType).toJava().isLocalClass();
    }

    @Override
    public boolean isMember() {
        return ((ClassActor) riType).toJava().isMemberClass();
    }

    @Override
    public ResolvedJavaType getEnclosingType() {
        Class<?> enclosingClass = ((ClassActor) riType).toJava().getEnclosingClass();
        if (enclosingClass == null) {
            return null;
        }
        return MaxResolvedJavaType.get(ClassActor.fromJava(enclosingClass));
    }

    @Override
    public ResolvedJavaMethod[] getDeclaredConstructors() {
        VirtualMethodActor[] v = ((ClassActor) riType).localVirtualMethodActors();
        ArrayList<ResolvedJavaMethod> list = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i].isConstructor()) {
                list.add(MaxResolvedJavaMethod.get(v[i]));
                count++;
            }
        }
        return list.toArray(new ResolvedJavaMethod[count]);
    }

    @Override
    public ResolvedJavaMethod[] getDeclaredMethods() {
        MethodActor[] v = ((ClassActor) riType).getLocalMethodActorsArray();
        ResolvedJavaMethod[] result = new ResolvedJavaMethod[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = MaxResolvedJavaMethod.get(v[i]);
        }
        return result;
    }

    @Override
    public Constant newArray(int length) {
        return Constant.forObject(Array.newInstance(((ClassActor) riType).toJava(), length));
    }

}
