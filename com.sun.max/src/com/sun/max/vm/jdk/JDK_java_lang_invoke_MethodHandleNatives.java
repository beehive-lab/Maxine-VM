/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 */

package com.sun.max.vm.jdk;

import static com.sun.max.vm.classfile.constant.ConstantPool.ReferenceKind.*;
import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MemberName.*;
import static java.lang.invoke.MethodType.*;

import java.lang.invoke.*;
import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.methodhandle.*;
import com.sun.max.vm.methodhandle.MaxMethodHandles.*;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.type.*;

/**
 * java.lang.invoke.MethodHandleNatives Substitutions.
 *
 * see methodHandles.cpp in HotSpot.
 *
 */
@METHOD_SUBSTITUTIONS(className = "java.lang.invoke.MethodHandleNatives")
public final class JDK_java_lang_invoke_MethodHandleNatives {

    // these guys are defined in methodHandles.cpp and MethodHandleNatives.
    public static final int IS_METHOD = 0x00010000; // method (not constructor)
    public static final int IS_CONSTRUCTOR = 0x00020000; // constructor
    public static final int IS_INVOCABLE = IS_METHOD + IS_CONSTRUCTOR;
    public static final int IS_FIELD = 0x00040000; // field
    public static final int IS_TYPE = 0x00080000; // nested type
    public static final int ALL_KINDS = IS_METHOD | IS_CONSTRUCTOR | IS_FIELD | IS_TYPE;
    public static final int REFERENCE_KIND_SHIFT = 24; // refKind
    public static final int REFERENCE_KIND_MASK = 0x0F000000 >> REFERENCE_KIND_SHIFT;
    // The SEARCH_* bits are not for MN.flags but for the matchFlags argument of MHN.getMembers:
    static final int SEARCH_SUPERCLASSES = 0x00100000; // walk super classes
    static final int SEARCH_INTERFACES = 0x00200000; // walk implemented interfaces

    public static final int JVM_REF_getField         = REF_getField.classfileReferenceKind();
    public static final int JVM_REF_getStatic        = REF_getStatic.classfileReferenceKind();
    public static final int JVM_REF_putField         = REF_putField.classfileReferenceKind();
    public static final int JVM_REF_putStatic        = REF_putStatic.classfileReferenceKind();
    public static final int JVM_REF_invokeVirtual    = REF_invokeVirtual.classfileReferenceKind();
    public static final int JVM_REF_invokeStatic     = REF_invokeStatic.classfileReferenceKind();
    public static final int JVM_REF_invokeSpecial    = REF_invokeSpecial.classfileReferenceKind();
    public static final int JVM_REF_newInvokeSpecial = REF_newInvokeSpecial.classfileReferenceKind();
    public static final int JVM_REF_invokeInterface  = REF_invokeInterface.classfileReferenceKind();

    private JDK_java_lang_invoke_MethodHandleNatives() {
    }

    @SUBSTITUTE
    private static void registerNatives() {
    }

    @SUBSTITUTE
    private static int getConstant(int which) {
        return 0;
    }

    @SUBSTITUTE(optional = true)
    private static void init(MethodType type) {
    }

    /**
     * Entry point from the VM to the JDK requesting an adapter for invoking a method handle.
     *
     * @param callerClass - for access checks
     * @param refKind - the invoke kind
     * @param defc - targets defining class
     * @param name - name of target
     * @param type - Object representing the signature of the target
     * @param appendix - placeholder for returning an appendix argument.
     * @return
     */
    @ALIAS(declaringClassName = "java.lang.invoke.MethodHandleNatives", descriptor = "(Ljava/lang/Class;ILjava/lang/Class;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/invoke/MemberName;")
    public static native Object linkMethod(Class< ? > callerClass, int refKind, Class< ? > defc, String name, Object type, Object[] appendix);

    /**
     * Entry point from the VM to the JDK requesting an adapter for invoking a method handle.
     *
     * @param callerClass
     * @param refKind
     * @param defc
     * @param name
     * @param type
     * @return
     */
    @ALIAS(declaringClassName = "java.lang.invoke.MethodHandleNatives")
    public static native MethodHandle linkMethodHandleConstant(Class<?> callerClass, int refKind, Class<?> defc, String name, Object type);

    /**
     * The JVM wants a pointer to a MethodType.  Oblige it by finding or creating one.
     */
    @ALIAS(declaringClassName = "java.lang.invoke.MethodHandleNatives")
    public static native MethodType findMethodHandleType(Class<?> rtype, Class<?>[] ptypes);

    /**
     * The JVM is linking an invokedynamic instruction.  Create a reified call site for it.
     */
    @ALIAS(declaringClassName = "java.lang.invoke.MethodHandleNatives",
            descriptor = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/invoke/MemberName;")
    public static native Object linkCallSite(Object callerObj, Object bootstrapMethodObj, Object nameObj,
                                             Object typeObj, Object staticArguments, Object[] appendixResult);

    /**
     * For a field MemberName, return the offset of the field in its holder.
     *
     * @param memberName
     * @return
     */
    @SUBSTITUTE(value = "objectFieldOffset", signatureDescriptor = "(Ljava/lang/invoke/MemberName;)J")
    static long objectFieldOffset(Object memberName) {
        Trace.begin(1, "MHN:objectFieldOffset");
        Trace.line(1, "memberName=" + memberName + ", id=" + System.identityHashCode(memberName));
        VMTarget t = VMTarget.fromMemberName(memberName);
        assert t != null;
        int offset = t.getVMindex();
        Trace.line(1, "Got offset of =>" + (long) offset);
        Trace.end(1, "MHN:objectFieldOffset");
        return offset;
    }

    /**
     * For a field MemberName, return the offset of the field in its holder.
     *
     * @param memberName
     * @return
     */
    @SUBSTITUTE(value = "staticFieldOffset", signatureDescriptor = "(Ljava/lang/invoke/MemberName;)J")
    static long staticFieldOffset(Object memberName) {
        Trace.begin(1, "MHN:staticFieldOffset");
        Trace.line(1, "memberName=" + memberName + ", id=" + System.identityHashCode(memberName));
        VMTarget t = VMTarget.fromMemberName(memberName);
        assert t != null;
        int offset = t.getVMindex();
        Trace.line(1, "Got offset of =>" + (long) offset);
        Trace.end(1, "MHN:staticFieldOffset");
        return offset;
    }

    @SUBSTITUTE(value = "staticFieldBase", signatureDescriptor = "(Ljava/lang/invoke/MemberName;)Ljava/lang/Object;")
    static Object staticFieldBase(Object memberName) {
        Trace.begin(1, "MHN:staticFieldBase");
        VMTarget t = VMTarget.fromMemberName(memberName);
        FieldActor fa = t.asFieldActor();
        Trace.line(1, "Got FieldActor=" + fa);
        Object o = fa.holder().staticTuple();
        Trace.end(1, "MHN:staticFieldBase return=>" + o);
        return o;
    }

    /**
     * Return true if the refKind is a field.
     *
     * @param refKind
     * @return
     */
    private static boolean ref_kind_is_field(int refKind) {
        return refKind <= JVM_REF_putStatic;
    }

    /**
     * Return true if the refKind is a getter.
     *
     * @param refKind
     * @return
     */
    private static boolean ref_kind_is_getter(int refKind) {
        return refKind <= JVM_REF_getStatic;
    }

    /**
     * Return true if the refKind is a setter.
     *
     * @param refKind
     * @return
     */
    private static boolean ref_kind_is_setter(int refKind) {
        return ref_kind_is_field(refKind) && !ref_kind_is_getter(refKind);
    }

    /**
     * Returns a 2 element array containing the vmindex and vmtarget from a MemberName.
     *
     * see methodHandles.cpp: MHN_getMemberVMInfo
     *
     * @param memberName
     * @return
     */
    @SUBSTITUTE(value = "getMemberVMInfo", signatureDescriptor = "(Ljava/lang/invoke/MemberName;)Ljava/lang/invoke/Object;")
    static Object getMemberVMInfo(Object memberName) {
        Trace.begin(1, "MHN.getMemberVMInfo memberName id=" + System.identityHashCode(memberName));
        if (memberName == null) {
            Trace.end(1, "MHN.getMemberVMInfo: MemberName is null");
            return null;
        }
        VMTarget t = VMTarget.fromMemberName(memberName);

        if (t.isField()) {
            Trace.line(1, "Is Field");
            FieldActor fa = t.asFieldActor();
            init_field_MemberName(memberName, fa, fa.accessFlags());
        } else if (t.isMethod()) {
            ClassMethodActor cma = t.asClassMethodActor();
            Trace.line(1, "Is " + (cma.isConstructor() ? "constructor" : "method"));
            init_method_MemberName(memberName, cma, !cma.isConstructor(), null);

        } else {
            // TODO: Implement me
            throw new RuntimeException("Implement Me");
        }
        Trace.end(1, "MHN.getMemberVMInfo");
        return new Object[] {new Long(t.getVMindex()), t.getVmTarget()};
    }

    /**
     * Called from JDK to resolve the target method and inject the relevant linkage into the member name argument.
     *
     * see: methodHandles.cpp MHN_resolve_Mem() / resolve_MemberName()
     *
     * @param o - The MemberName
     * @param klass - the class containing the target method.
     * @return
     */
    @SUBSTITUTE(value = "resolve", signatureDescriptor = "(Ljava/lang/invoke/MemberName;Ljava/lang/Class;)Ljava/lang/invoke/MemberName;")
    static Object resolve(Object o, Class klass) {
        Trace.begin(1, "MHN.resolve(MemberName, Class)");

        String name = asMemberName(o).name;
        Object typeObject = asMemberName(o).type;
        Class< ? > clazz = asMemberName(o).clazz;
        ClassActor classActor = ClassActor.fromJava(clazz);
        MethodType type = lookupMethodType(typeObject, true);
        SignatureDescriptor mnSig = SignatureDescriptor.create(type.toMethodDescriptorString());
        VMTarget vmtarget = VMTarget.fromMemberName(o);
        Object resolution = asMemberName(o).resolution;

        MethodActor ma = null;
        int flags = asMemberName(o).flags;
        int refKind = (flags >> REFERENCE_KIND_SHIFT) & REFERENCE_KIND_MASK;

        Trace.line(1, "MemberName name=" + name + ", type=" + mnSig + ", clazz=" + clazz.getName() + ", flags=" + flags + ", resolution=" + resolution + ", vmtarget=" + vmtarget + ", refKind=" +
                        refKind);

        MethodHandleIntrinsicID mhInvokeID = MethodHandleIntrinsicID.None;

        if ((flags & ALL_KINDS) == IS_METHOD && (clazz == MethodHandle.class) &&
                        (refKind == JVM_REF_invokeVirtual || refKind == JVM_REF_invokeSpecial || refKind == JVM_REF_invokeStatic)) {
            MethodHandleIntrinsicID iid = MethodHandleIntrinsicID.fromName(classActor, name);
            if (iid != MethodHandleIntrinsicID.None &&
                            ((refKind == JVM_REF_invokeStatic) == MethodHandleIntrinsicID.isSignaturePolymorphicStatic(iid))) {
                mhInvokeID = iid;
                Trace.line(1, "mhInvokeID=" + mhInvokeID);
            }
        }

        boolean doDispatch = true;
        switch (flags & ALL_KINDS) {
            case IS_METHOD:
                if (refKind == JVM_REF_invokeStatic) {
                    Trace.line(1, "Resolving static method.");
                    ma = MaxMethodHandles.resolveMethod(classActor, name, type, klass, true);
                    if (!ma.isStatic()) {
                        throw new IncompatibleClassChangeError(name + " is not static");
                    }
                } else if (refKind == JVM_REF_invokeInterface) {
                    Trace.line(1, "Resolving interface method.");
                    ma = MaxMethodHandles.resolveInterfaceMethod(classActor, name, type, klass, true);
                } else if (mhInvokeID != MethodHandleIntrinsicID.None) {
                    Trace.line(1, "Resolving handle call.");
                    assert !MethodHandleIntrinsicID.isSignaturePolymorphicStatic(mhInvokeID);
                    ma = MaxMethodHandles.lookupPolymorphicMethod(classActor, name, type, klass, new Object[1]);
                } else if (refKind == JVM_REF_invokeSpecial) {
                    Trace.line(1, "Resolving special.");
                    ma = MaxMethodHandles.resolveMethod(classActor, name, type, klass, true);
                    doDispatch = false;
                    if (ma.isInitializer() && ma.holder().equals(klass)) {
                        throw new NoSuchMethodError(klass + " method " + name + " " + type + " not found");
                    }
                    if (ma.isStatic()) {
                        throw new IncompatibleClassChangeError("Expecting non static method " + klass + " method " + name + " " + type);
                    }

                    if (ma.isAbstract()) {
                        throw new AbstractMethodError("Expecting non abstract method " + klass + " method " + name + " " + type);
                    }
                    // ma.setStatic();
                } else if (refKind == JVM_REF_invokeVirtual) {
                    Trace.line(1, "Resolving virtual method.");
                    ma = MaxMethodHandles.resolveMethod(classActor, name, type, klass, true);
                    if (ma.isStatic()) {
                        throw new IncompatibleClassChangeError(name + " cannot be static");
                    }
                } else {
                    throw ProgramError.unexpected("refKind=" + refKind);
                }
                Trace.line(1, "Resolved => " + ma);
                init_method_MemberName(o, ma, !ma.canBeStaticallyBound() && doDispatch, clazz);
                break;
            case IS_CONSTRUCTOR:
                Trace.line(1, "Got constructor.");
                ma = MaxMethodHandles.resolveMethod(classActor, name, type, klass, true);
                init_method_MemberName(o, ma, !ma.canBeStaticallyBound() && doDispatch, clazz);
                break;
            case IS_FIELD:
                Trace.line(1, "Got field.");
                TypeDescriptor td = lookupFieldType(typeObject);
                Trace.line(1, "typeObject=" + typeObject + ", td=" + td);
                FieldActor fa = classActor.findFieldActor(SymbolTable.makeSymbol(name), td);
                Trace.line(1, "fieldActor=" + fa);
                int xflags = fa.accessFlags();
                boolean isSetter = ref_kind_is_setter(refKind);
                init_field_MemberName(o, fa, xflags, isSetter);
                break;
            default:
                throw ProgramError.unknownCase("XX");
        }

        Trace.end(1, "MHN.resolve()");
        return o;
    }

    /**
     * Initialise a field MemberName and plant the vmtarget / vmindex.
     *
     * see methodHandles.cpp init_field_MemberName
     *
     * @param mname
     * @param fieldActor
     * @param accessFlags
     * @return
     */
    private static /* MemberName */ Object init_field_MemberName(Object mname, FieldActor fieldActor, int accessFlags) {
        return init_field_MemberName(mname, fieldActor, accessFlags, false);
    }

    /**
     * Initialise a field MemberName and plant the vmtarget / vmindex.
     *
     * see methodHandles.cpp init_field_MemberName
     *
     * @param mname
     * @param fieldActor
     * @param accessFlags
     * @param isSetter
     * @return
     */
    private static /* MemberName */ Object init_field_MemberName(Object mname, FieldActor fieldActor, int accessFlags, boolean isSetter) {

        accessFlags |= IS_FIELD | ((fieldActor.isStatic() ? JVM_REF_getStatic : JVM_REF_getField) << REFERENCE_KIND_SHIFT);
        if (isSetter) {
            accessFlags += (JVM_REF_putField - JVM_REF_getField) << REFERENCE_KIND_SHIFT;
        }
        asMemberName(mname).flags = accessFlags;
        if (asMemberName(mname).name == null) {
            asMemberName(mname).name = fieldActor.name();
        }

        if (asMemberName(mname).type == null) {
            asMemberName(mname).type = fieldActor.type().javaClass();
        }

        if (asMemberName(mname).clazz == null) {
            asMemberName(mname).clazz = fieldActor.holder().javaClass();
        }

        VMTarget vmTarget = VMTarget.create(mname);
        vmTarget.setVmTarget(fieldActor);
        vmTarget.setVMindex(fieldActor.offset());
        Trace.line(1, "MHN.init_field_MemberName, field=" + fieldActor + ", flags=" + accessFlags + ", setter=" + isSetter);
        return mname;
    }

    /**
     * Decode a MemberName.type field and return an appropriate Maxine descriptor..
     *
     * @param type
     * @return
     */
    private static TypeDescriptor lookupFieldType(Object type) {
        Trace.begin(1, "MHN.lookupFieldType");
        TypeDescriptor td = null;

        if (type instanceof Class) {
            Trace.line(1, "class type=" + type);
            td = JavaTypeDescriptor.forJavaClass((Class) type);
        } else if (type instanceof String) {
            Trace.line(1, "String type=" + type);
            td = JavaTypeDescriptor.getDescriptorForJavaString((String) type);
        } else {
            throw ProgramError.unexpected("unexpected type: " + type);
        }
        Trace.end(1, "MHN.lookupFieldType td=" + td);
        return td;
    }

    /**
     * The HotSpot implementation of this method returns a symbol type wrapping the string which represents the
     * signature. Currently the Maxine downstream methods are expecting a MethodType.
     *
     * TODO convert to Maxine SignatureDescriptor.
     *
     * @param type
     * @param intern
     * @return
     */
    private static MethodType lookupMethodType(Object type, boolean intern) {
        MethodType mt = null;
        Trace.begin(1, "MHN.lookupMethodType");
        if (type instanceof MethodType) {
            mt = (MethodType) type;
        } else if (type instanceof Class) {
            mt = MethodType.methodType((Class) type);
        } else if (type instanceof String) {
            mt = MethodType.fromMethodDescriptorString((String) type, BootClassLoader.BOOT_CLASS_LOADER);
        } else {
            throw ProgramError.unexpected("unkown type");
        }
        Trace.line(1, "Got methodType=" + type);
        Trace.end(1, "MHN.lookupMethodType");
        return mt;
    }

    /**
     * Initialise a method MemberName and plant the vmindex / vmtarget. see methodHandles.cpp : init_method_MemberName()
     *
     * @param memberName
     * @param method
     * @param doDispatch
     */
    private static Object init_method_MemberName(Object memberName, Method method, boolean doDispatch, Class klazz) {
        return init_method_MemberName(memberName, MethodActor.fromJava(method), doDispatch, klazz);
    }

    /**
     * Initialise a constructor MemberName and plant the vmindex / vmtarget. see methodHandles.cpp : init_method_MemberName()
     *
     * @param memberName
     * @param constructor
     * @param doDispatch
     */
    private static Object init_method_MemberName(Object memberName, Constructor constructor, boolean doDispatch, Class klazz) {
        return init_method_MemberName(memberName, MethodActor.fromJavaConstructor(constructor), doDispatch, klazz);
    }

    /**
     * Initialise a method MemberName and plant the vmindex / vmtarget.
     *
     * MethodHandles::init_method_MemberName (methodHandles.cpp)
     *
     * @param memberName
     * @param method
     */
    public static Object init_method_MemberName(Object memberName, MethodActor methodActor, boolean doDispatch, Class
            resolvedClass) {
        Trace.begin(1, "MHN.init_method_MemberName: methodActor=" + methodActor + ", doDispatch=" + doDispatch);
        int xflags = methodActor.accessFlags();

        ClassActor holderActor = methodActor.holder();
        Class holder = holderActor.javaClass();

        int vmindex = VirtualMethodActor.NONVIRTUAL_VTABLE_INDEX;
        if (resolvedClass == null) {
            resolvedClass = holder;
        }

        if (methodActor.isInitializer()) {
            Trace.line(1, "is initializer.");
            xflags |= IS_CONSTRUCTOR | (JVM_REF_invokeSpecial << REFERENCE_KIND_SHIFT);
        } else if (methodActor.isStatic()) {
            Trace.line(1, "is static.");
            xflags |= IS_METHOD | (JVM_REF_invokeStatic << REFERENCE_KIND_SHIFT);
        } else if (resolvedClass.isInterface() && holder.isInterface()) {
            Trace.line(1, "is interface.");
            xflags |= IS_METHOD | (JVM_REF_invokeInterface << REFERENCE_KIND_SHIFT);
        } else if (!holder.equals(resolvedClass) && holder.isInterface()) {
            Trace.line(1, "is Miranda method");
            xflags |= IS_METHOD | (JVM_REF_invokeInterface << REFERENCE_KIND_SHIFT);
        } else if (!doDispatch || methodActor.canBeStaticallyBound()) {
            Trace.line(1, "method can be statically bound");
            xflags |= IS_METHOD | (JVM_REF_invokeSpecial << REFERENCE_KIND_SHIFT);
        } else {
            Trace.line(1, "is virtual.");
            xflags |= IS_METHOD | (JVM_REF_invokeVirtual << REFERENCE_KIND_SHIFT);
            vmindex = ((VirtualMethodActor) methodActor).vTableIndex();
        }
        Trace.line(1, "Injecting MethodActor=>" + methodActor + " into MemberName, flags=" + xflags);
        asMemberName(memberName).flags = xflags;
        asMemberName(memberName).clazz = holder;
        VMTarget vmtarget = VMTarget.create(memberName);
        vmtarget.setVmTarget(methodActor);
        vmtarget.setVMindex(vmindex);
        Trace.end(1, "MHN.init_method_MemberName flags=" + xflags + ", holder=" + holder + ", memberName" + memberName + ", vmtarget=" + vmtarget.getVmTarget());
        return memberName;
    }

    /**
     * Called from the JDK to initialise a MemberName using the supplied Object ref argument.
     *
     * methodHandles.cpp: MHN_init_Mem
     *
     * @param memberName
     * @param ref
     */
    @SUBSTITUTE(value = "init", signatureDescriptor = "(Ljava/lang/invoke/MemberName;Ljava/lang/Object;)V")
    static void init(Object memberName, Object ref) {

        Trace.begin(1, "MHN.init(): ref=" + ref + ", memberName id=" + System.identityHashCode(memberName));

        if (memberName == null) {
            throw ProgramError.unexpected("memberName is null");
        }
        if (ref == null) {
            throw ProgramError.unexpected("Object ref is null");
        }

        if (ref instanceof Field) {
            Trace.line(1, "Got Field");
            Field f = (Field) ref;
            Class c = f.getDeclaringClass();
            Utf8Constant name = SymbolTable.makeSymbol(f.getName());
            TypeDescriptor type = JavaTypeDescriptor.forJavaClass(f.getType());
            FieldActor fa = ClassActor.fromJava(c).findFieldActor(name, type);
            int flags = fa.accessFlags();
            init_field_MemberName(memberName, fa, flags);
        } else if (ref instanceof Method) {
            Trace.line(1, "Got Method");
            init_method_MemberName(memberName, (Method) ref, true, ref.getClass());
        } else if (ref instanceof Constructor) {
            Trace.line(1, "Got Constructor");
            init_method_MemberName(memberName, (Constructor) ref, true, ref.getClass());
        } else if (ref.getClass().equals(MemberName_Class)) {
            Trace.line(1, "Got MemberName");
            // TODO: Implement me
            throw new RuntimeException("Implement me");
        } else {
            throw FatalError.unimplemented();
        }

        int flags = asMemberName(memberName).flags;
        Class< ? > defc = asMemberName(memberName).clazz;
        String name = asMemberName(memberName).name;
        Object type = asMemberName(memberName).type;
        Object resolution = asMemberName(memberName).resolution;
        VMTarget vmtarget = VMTarget.fromMemberName(memberName);
        Trace.end(1, "MHN.init (memberName): defc=" + defc + ", name=" + name + ", type=" + type + ", flags=" + flags + ", resolution=" + resolution + ", vmtarget=" + vmtarget);

    }

    /**
     * Fill out MemberName fields (name, clazz, type, ...) from an injected MethodActor.
     *
     * methodHandles.cpp MHN_expand_Mem()
     *
     * @param memberName
     */
    @SUBSTITUTE(value = "expand", signatureDescriptor = "(Ljava/lang/invoke/MemberName;)V")
    static void expand(Object memberName) {
        Trace.begin(1, "MHN.expand");
        int flags = asMemberName(memberName).flags;
        Class< ? > defc = asMemberName(memberName).clazz;
        String name = asMemberName(memberName).name;
        Object type = asMemberName(memberName).type;
        VMTarget v = VMTarget.fromMemberName(memberName);
        Trace.line(1, "expand (memberName): defc=" + defc + ", name=" + name + ", type=" + type + ", flags=" + flags + ", vmtarget=" + v.getVmTarget());

        if (v == null) {
            throw new IllegalArgumentException("nothing to expand");
        }
        if (defc != null && name != null && type != null) {
            return;
        }

        switch (flags & ALL_KINDS) {
            case IS_METHOD:
            case IS_CONSTRUCTOR:
                assert v.isMethod() : "vmtarget must be a method.";
                ClassMethodActor cma = v.asClassMethodActor();
                if (defc == null) {
                    asMemberName(memberName).clazz = cma.holder().toJava();
                }
                if (name == null) {
                    asMemberName(memberName).name = cma.name();
                }
                if (type == null) {
                    asMemberName(memberName).type = cma.signature().asString();
                }
                break;
            case IS_FIELD:
                // TODO: Implement me
                throw new RuntimeException("Got Field -- implement me");
            default:
                throw new InternalError("unrecognized MemberName format");
        }
        Trace.end(1, "MHN.expand clazz=" + asMemberName(memberName).clazz + ", name=" + asMemberName(memberName).name + ", type=" + asMemberName(memberName).type);
    }

}
