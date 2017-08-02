/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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

package com.sun.max.vm.methodhandle;

import static com.oracle.max.cri.intrinsics.IntrinsicIDs.*;
import static com.sun.max.vm.actor.Actor.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.UNSAFE_CAST;
import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MethodHandleNatives.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.methodhandle.MaxMethodHandles.MethodHandleIntrinsicID.*;

import java.lang.invoke.*;
import java.util.concurrent.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.type.*;

/**
 * Various JSR292 implementation static methods, most of which have analogues in the
 * HotSpot JVM found in methodHandles.cpp, linkResolver.cpp and systemDictionary.cpp.
 * Resolution and linking of MethodHandles.
 */
public final class MaxMethodHandles {

    private static final int REFERENCE_KIND_SHIFT      = 24; // refKind

    /**
     * Following the HotSpot -- cache synthetic method handle intrinsic methods
     * keyed on signature and intrinsic type.
     */
    private static final ConcurrentHashMap<MethodHandleIntrinsicKey, MethodActor>
    IntrinsicMap = new ConcurrentHashMap<>();
    /**
     * Intrinsics for the MethodHandle VM invocation semantics.
     */
    public enum MethodHandleIntrinsicID {
        // TODO check these
        InvokeGeneric("invoke", null),
        InvokeBasic("invokeBasic", INVOKEBASIC),
        LinkToVirtual("linkToVirtual", LINKTOVIRTUAL),
        LinkToStatic("linkToStatic", LINKTOSTATIC),
        LinkToSpecial("linkToSpecial", LINKTOSPECIAL),
        LinkToInterface("linkToInterface", null),
        None(null, null);


        MethodHandleIntrinsicID(String name, String intrinsic) {
            this.name = name;
            this.intrinsic = intrinsic;
        }
        final String name;
        final String intrinsic;

        /**
         * Return the intrinsic ID given the method name a la HotSpot.
         * @param name
         * @return
         */
        public static MethodHandleIntrinsicID fromName(String name) {
            if (name == null || name.equals("")) {
                return None;
            }

            /*
             * InvokeGeneric is what its called in HotSpot. There is no
             * reason for explicitly mapping invokeExact to invokeGeneric other
             * than to align the naming with the HotSpot source to make it easier to follow.
             */
            if (name.equals("invokeExact")) {
                return InvokeGeneric;
            }

            for (MethodHandleIntrinsicID s : values()) {
                if (s.name == name) {
                    return s;
                }
            }

            return None;
        }

        public static boolean isSignaturePolymorphicIntrinsic(MethodHandleIntrinsicID name) {
            return name != InvokeGeneric;
        }

        /**
         * Return true if the argument is a signature polymorphic static linkTo* method,
         * and false otherwise.
         * @param name
         * @return
         */
        public static boolean isSignaturePolymorphicStatic(MethodHandleIntrinsicID name) {
            return name == LinkToVirtual || name == LinkToStatic || name == LinkToSpecial || name == LinkToInterface;
        }
    }


    private static MethodActor resolveInterfaceMethod(ClassActor classActor, String name,
                                                      MethodType type, Class<?> caller, boolean checkAccess)
    {
        assert(classActor.isInterface());

        MethodActor ma;
        ma = classActor.findClassMethodActor(SymbolTable.makeSymbol(name), SignatureDescriptor.create(type.toMethodDescriptorString()));

        if (ma == null) {
            throw new NoSuchMethodError(classActor + "." + name + "(" + type + ")");
        }

        if (checkAccess) {
            //TODO access checks
        }
        return ma;
    }
    /**
     *
     * from linkResolver.cpp resolve_method().
     *
     * @param classActor
     * @param name
     * @param type
     * @param caller
     * @param checkAccess
     * @return
     */
    public static MethodActor resolveMethod (ClassActor classActor, String name,
                    MethodType type, Class <?> caller, boolean checkAccess)
    {
        Trace.begin(1, "MethodHandles.resolveMethod()");
        Trace.line(1, "classActor="   + classActor + ":" + classActor.hashCode() +
                        ", name="         + name +
                        ", type="         + type);
        // 1. verify class is not an interface
        if (classActor.isInterface()) {
            throw new IncompatibleClassChangeError("Interface found where class expected -->" + classActor.javaClass().getName());
        }

        // 2. lookup method in its resolved class and super classes
        MethodActor ma = classActor.
                        findClassMethodActor(SymbolTable.makeSymbol(name), SignatureDescriptor.create(type.toMethodDescriptorString()));

        // 3. search interfaces
        if (ma == null) {

//            ma = resolveInterfaceMethod(classActor, name, type, caller, checkAccess);

            // 4. JSR 292 support.
            if (ma == null) {
                ma = lookupPolymorphicMethod(classActor, name, type, caller, new Object[1]);
            }
        }

        if (ma == null) {
            throw new NoSuchMethodError(classActor.javaClass().getName() + "." + name + "(" + type + ")");
        }

        if (ma.isAbstract() && !classActor.isAbstract()) {
            throw new AbstractMethodError();
        }
        Trace.line(1, "resolved method=" + ma);
        Trace.end(1, "MethodHandles.resolveMethod()");
        // TODO: access checks.
        return ma;
    }


    /**
     * From linkResolver.cpp lookup_polymorphic_method().
     *
     * @param classActor
     * @param name
     * @param signature
     * @param caller
     * @param appendix
     * @return
     */
    public static MethodActor lookupPolymorphicMethod
    (ClassActor classActor, String name, SignatureDescriptor signature, Class< ? > caller, Object [] appendix)
    {
        Trace.line(1, "MaxMethodHandles.lookupPolymorphicMethod() : " +
                        "classActor: " + classActor +
                        ", name: " + name +
                        ", signatureDescriptor: " + signature +
                        ", caller: " + caller +
                        ", appendix: " + appendix);
        return lookupPolymorphicMethod (classActor, name, MethodType.fromMethodDescriptorString(signature.asString(), null),
                        caller, appendix);

    }

    /**
     *
     * @param classActor
     * @param name
     * @param type
     * @param caller
     * @return
     */
    public static MethodActor lookupPolymorphicMethod(ClassActor classActor, String name, MethodType type, Class< ? > caller, Object [] appendix) {
        Trace.begin(1, "MethodHandles.lookupPolymorphicMethod()");
        MethodHandleIntrinsicID iid = fromName(name);
        MethodActor actor = null;
        Trace.line(1, "iid=" + iid + ", class=" + classActor.javaClass().getName() + ", type=" + type);
        if (classActor.javaClass().equals(MethodHandle.class) && iid != None) {

            if (isSignaturePolymorphicIntrinsic(iid)) {
                Trace.line(1, "isSignaturePolymorphicIntrinsic=" + iid);
                /*
                 * convert all wrapped primitives to primitives, and erase all other references to Object in
                 * accordance with the MethodHandle type system. Preserve the last argument if its a static linkTo*
                 * intrinsic.
                 */
                MethodType basicType = basicMethodType(type, isSignaturePolymorphicStatic(iid));
                actor = maybeMakeMethodHandleIntrinsic(classActor, iid, basicType);

            }
            else if (iid == InvokeGeneric) {
                Trace.line(1, "InvokeGeneric, calling JDK to spin adapter.");
                Object memberName = JDK_java_lang_invoke_MethodHandleNatives.
                                linkMethod(classActor.javaClass(), JVM_REF_invokeVirtual, MethodHandle.class, name, type, appendix);
                Trace.line(1, "linkMethod returned memberName: " + memberName + ", appendix: " + appendix[0]);
                if (memberName != null) {
                    VMTarget vmtarget = VMTarget.fromMemberName(memberName);
                    actor = (MethodActor)vmtarget.getVmTarget();
                    assert(actor != null);
                }
            }
        }
        // TODO access checks



        Trace.line(1, "MethodActor => " + actor);
        Trace.end(1, "MethodHandles.lookupPolymorphicMethod()");
        return actor;
    }


    /**
     *
     * @param classActor
     * @param iid
     * @param type
     * @return
     */
    private static MethodActor maybeMakeMethodHandleIntrinsic (ClassActor classActor, MethodHandleIntrinsicID iid, MethodType type) {
        MethodHandleIntrinsicKey key = new MethodHandleIntrinsicKey(type, iid);
        MethodActor method;
        if (IntrinsicMap.containsKey(key)) {
            method = IntrinsicMap.get(key);
        } else {
            Trace.line(1, "Making new intrinsic method");
            method = makeMethodHandleIntrinsic(classActor, iid, type);
            IntrinsicMap.put(key, method);
            method.setHolder(classActor);

        }
        return method;
    }


    /**
     * from methodOopDesc::make_method_handle_intrinsic
     *
     * @return
     */
    private static MethodActor makeMethodHandleIntrinsic
    (ClassActor mhClassActor, MethodHandleIntrinsicID iid, MethodType type)
    {
        Trace.begin(1, "MethodHandles.makeMethodHandleIntrinsic()");

        Utf8Constant name = SymbolTable.makeSymbol(iid.name);

        int static_flag = MethodHandleIntrinsicID.isSignaturePolymorphicStatic(iid) ? ACC_STATIC : 0;
        MethodActor methodActor;
        ConstantPoolEditor cpe = new ConstantPool(mhClassActor.constantPool().classLoader()).edit();
        cpe.append(PoolConstantFactory.makeUtf8Constant(iid.name));
        cpe.append(PoolConstantFactory.makeUtf8Constant(type.toString()));
        cpe.release();
        CodeAttribute codeAttribute =
                        new CodeAttribute(
                                        cpe.pool(),
                                        new byte[0],
                                        (char)0,
                                        (char)0,
                                        CodeAttribute.NO_EXCEPTION_HANDLER_TABLE,
                                        LineNumberTable.EMPTY,
                                        LocalVariableTable.EMPTY,
                                        null);
        if (static_flag != 0) {
            Trace.line(1, "Making StaticMethodActor: " + name);
            methodActor = new StaticMethodActor(
                            name,
                            SignatureDescriptor.create(type.returnType(), type.parameterArray()),
                            ACC_SYNTHETIC | ACC_FINAL |/* ACC_NATIVE |*/ static_flag,
                            codeAttribute,
                            iid.intrinsic);
        }
        else {
            Trace.line(1, "Making VirtualMethodActor: " + name);
            methodActor = new VirtualMethodActor(

                            name,
                            SignatureDescriptor.create(type.returnType(), type.parameterArray()),
                            ACC_SYNTHETIC | ACC_FINAL,
                            codeAttribute,
                            iid.intrinsic);
            /* Virtual dispatch is done on the vtable index of the target, buried
             * in the method handles member name.
             */
            ((VirtualMethodActor)methodActor).setVTableIndex(VirtualMethodActor.NONVIRTUAL_VTABLE_INDEX);

            methodActor.setHolder(mhClassActor);
            if (iid == InvokeBasic) {
                ((ClassMethodActor)methodActor).compiledState = new Compilations(null, vm().stubs.invokeBasic());
            }
        }


        Trace.line(1, "method=>" + methodActor);
        Trace.end(1, "MethodHandles.makeMethodHandleIntrinsic()");

        return methodActor;
    }

    /**
     * Returns true if the Class argument is a sub word primitive class or false otherwise.
     * @param c
     * @return
     */
    private static boolean isSubWordType(Class<?> c) {
        return (c == boolean.class || c == short.class || c == char.class || c == byte.class);
    }

    /**
     * Returns true if the Class argument conforms to the legal MethodHandle type system
     * i.e. one of VLIFJD where L == Object reference only
     */
    private static boolean isBasic(Class<?> c) {
        return (c == int.class || c == float.class || c == double.class || c == long.class || c == Object.class || c == void.class) ;
    }

    private static boolean isBasicType(MethodType type, boolean ignoreLastArg) {
        if (! isBasic(type.returnType())) {
            Trace.line(1, "Return type: " + type.returnType() + " is not basic");
            return false;
        }
        Class <?> [] params = type.parameterArray();
        int end = ignoreLastArg ? params.length - 1 : params.length;

        for (int i = 0; i < end ; i++) {
            if (!isBasic(params[i])) {
                Trace.line(1, "Parameter " + params[i] + " is not basic");
                return false;
            }
        }
        return true;
    }

    /**
     * Canonicalize the Class argument to conform to the MethodHandle type system.
     * All wrapped primitives should be unwrapped prior to calling canonicalize.
     *
     * @param c
     * @return
     */
    private static Class<?> canonicalize(Class<?> c) {

        if (isSubWordType(c)) {
            return int.class;
        }
        else if (c.isPrimitive()) {
            return c;
        }
        return Object.class;
    }

    /**
     *
     * @param type
     * @param keepLastArg
     * @return
     */
    private static MethodType basicMethodType(MethodType type, boolean keepLastArg) {

        Trace.begin(1, "MaxMethodHandles:basicMethodType");
        Trace.line(1, "type=" + type + ", keepLastArg=" + keepLastArg);

        if (isBasicType(type, keepLastArg)) {
            Trace.line(1, "MethodType: " + type + " is already basic.");
            Trace.end(1, "MaxMethodHandles:basicMethodType");
            return type;
        }

        if (type.hasWrappers()) {
            Trace.line(1, "Unwrapping wrapped types.");
            type = type.unwrap();
        }

        Class<?> rtype = type.returnType();
        Class<?> [] ptype = type.parameterArray();
        int end = ptype.length;

        if (keepLastArg) {
            end--;
        }
        for (int i = 0; i < end; i++) {
            ptype[i] = canonicalize (ptype[i]);
        }
        MethodType newType = MethodType.methodType(rtype, ptype);
        Trace.line(1, "Created new methodType =>" + newType);
        Trace.end(1, "MaxMethodHandles:basicMethodType");
        return newType;
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native MaxMethodHandles asThis(Object o);

    @ALIAS(declaringClass=java.lang.invoke.MethodHandle.class, descriptor="Ljava/lang/invoke/LambdaForm;")
    private Object form;

    @ALIAS(declaringClassName="java.lang.invoke.LambdaForm", descriptor="Ljava/lang/invoke/MemberName;")
    private Object vmentry;

    /**
     * Extract the MethodActor from a method handle to handle an invokeBasic
     * call, i.e. navigate
     * MH->form->vmentry->MethodActor (injected)
     * @param mh
     * @return The MethodActor
     */
    public static ClassMethodActor getInvokerForInvokeBasic(Object mh) {
        Trace.begin(1, "MaxMethodHandles.getInvokerForInvokeBasic:");
        Object lambdaForm = asThis(mh).form;
        Object memberName = asThis(lambdaForm).vmentry;
        VMTarget target = VMTarget.fromMemberName(memberName);
        assert(target != null);
        Trace.end(1, "MaxMethodHandles.getInvokerForInvokeBasic");
        return UnsafeCast.asClassMethodActor(target.getVmTarget());
    }

}
