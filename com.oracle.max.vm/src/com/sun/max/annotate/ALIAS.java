/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.annotate;
import java.lang.annotation.*;
import java.util.concurrent.*;

import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Mechanism for referring to fields, methods and constructors otherwise inaccessible due to Java language access
 * control rules. This enables VM code to directly access a private field or invoke a private method in a JDK class
 * without using reflection. Aliases avoid the boxing/unboxing required by reflection and they type check an aliased
 * field access or method invocation statically.
 *
 * The idiom for using ALIAS is somewhat related to the {@link SUBSTITUTE} annotation, but reversed; both are often used
 * in combination. In both cases a separate class is used to declare the aliased or substituted methods. In the
 * substitution case occurrences of {@code this} actually refer to the instance of the class being substituted. In the
 * aliased case we pretend that the class declaring the aliased method is an instance of the aliasee in order to access
 * its fields or invoke its methods.
 *
 * For example, assume we want to create an instance of a class {@code Foo} which has a private constructor
 * that takes one {@code int} argument. To do this we declare a new class {@code FooAlias} that contains the following:
 *
 * <code>
 * final class FooAlias {
 *     ALIAS(declaringClass = Foo.class, name="<init>");
 *     private native void init(int arg);
 *
 *     INTRINSIC(UNSAFE_CAST)
 *     static native FooAlias asThis(Foo foo);
 *
 *     public static Foo createFoo(int arg) {
 *         final Foo foo = (Foo) Heap.createTuple(ClassActor.fromJava(Foo.class).dynamicHub());
 *         FooAlias thisFoo = asThis(foo);
 *         thisFoo.init(arg);
 *         return foo;
 *     }
 * }
 * </code>
 *
 * The idiomatic use of {@code native} serves merely to avoid providing a body for the annotated methods.
 *
 * The code for field access is similar; declare an {@code @ALIAS} annotated field in the class with the same
 * name as the field in the aliasee and then use {@code thisFoo.field}.
 *
 * A (current) restriction is that this annotation is only parsed by the VM during boot image generation
 * and so should only be used on boot image methods and fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface ALIAS {
    /**
     * The name of the aliased field or method.
     * If the default value is specified for this element, then the
     * name of the annotated field or method is used.
     */
    String name() default "";

    /**
     * The type descriptor of the aliased field or signature descriptor of the aliased method. If the default value is
     * specified for this element, then the descriptor of the annotated field or method is used.
     * Note that the
     *
     * @return a string in the format expected by {@link JavaTypeDescriptor#parseTypeDescriptor(String)} or
     *         {@link SignatureDescriptor#create(String)}.
     */
    String descriptor() default "";

    /**
     * Specifies the class in which the aliased field or method is declared.
     * If the default value is specified for this element, then a non-default
     * value must be given for the {@link #declaringClassName()} element.
     */
    Class declaringClass() default ALIAS.class;

    /**
     * Specifies the class in which the aliased field or method is declared.
     * This method is provided for cases where the declaring class
     * is not accessible (according to Java language access control rules)
     * in the scope of the alias method.
     *
     * If the default value is specified for this element, then a non-default
     * value must be given for the {@link #declaringClassName()} element.
     */
    String declaringClassName() default "";

    /**
     * Specifies the suffix of the declaring class name when it is an inner class.
     */
    String innerClass() default "";

    /**
     * Specifies if the aliased target must exist. This property is useful to handle differences
     * in JDK versions for private methods.
     */
    boolean optional() default false;

    public static class Static {

        /**
         * Map from method alias to aliased method.
         */
        private static final ConcurrentHashMap<MethodActor, MethodActor> aliasedMethods = new ConcurrentHashMap<MethodActor, MethodActor>();

        /**
         * Map from aliased field to aliased field.
         */
        private static final ConcurrentHashMap<FieldActor, FieldActor> aliasedFields = new ConcurrentHashMap<FieldActor, FieldActor>();

        /**
         * Gets the field aliased by a given field, if any.
         *
         * @param field a field that may be an alias (i.e. annotated with {@link ALIAS})
         * @return the field aliased by {@code field} or {@code null} if it is not an alias
         */
        public static FieldActor aliasedField(FieldActor field) {
            if (MaxineVM.isHosted()) {
                registerAliasedField(field);
            }
            FieldActor aliasedField = aliasedFields.get(field);
            assert aliasedField != null || field.getAnnotation(ALIAS.class) == null : "All aliased fields and methods must be registed at boot time";
            return aliasedField;
        }

        @HOSTED_ONLY
        private static void registerAliasedField(FieldActor field) {
            ALIAS alias = field.getAnnotation(ALIAS.class);
            if (alias != null) {
                FieldActor aliasedField = aliasedFields.get(field);
                if (aliasedField == null) {
                    Class holder;
                    try {
                        holder = declaringClass(alias);
                    } catch (NoClassDefFoundError ex) {
                        if (alias.optional()) {
                            aliasedFields.put(field, field);
                            return;
                        }
                        throw FatalError.unexpected("Could not find holder for alias " + field + " in " + alias.declaringClassName());
                    }

                    String name = alias.name();
                    if (name.isEmpty()) {
                        name = field.name();
                    }

                    TypeDescriptor type = alias.descriptor().isEmpty() ? field.descriptor() : JavaTypeDescriptor.parseTypeDescriptor(alias.descriptor());
                    aliasedField = ClassActor.fromJava(holder).findLocalFieldActor(SymbolTable.makeSymbol(name), type);
                    if (aliasedField == null) {
                        if (alias.optional()) {
                            aliasedFields.put(field, field);
                            return;
                        }
                        throw FatalError.unexpected("Could not find target for alias " + field + " in " + holder.getName());
                    }
                    if (!alias.descriptor().isEmpty()) {
                        if (!field.type().isAssignableFrom(aliasedField.type())) {
                            throw FatalError.unexpected("Type of alias " + field + " [" + field.type() + "] must be a subclass of aliased field's type [" + aliasedField.type() + "]");
                        }
                    }
                    assert aliasedField.isStatic() == field.isStatic() : "Alias " + field + " must be static if " + aliasedField + " is";
                    aliasedFields.put(field, aliasedField);
                }
            }
        }

        /**
         * Gets the method (or constructor) aliased by a given method (or constructor), if any.
         *
         * @param method a method that may be an alias (i.e. annotated with {@link ALIAS})
         * @return the method aliased by {@code method} or {@code null} if it is not an alias
         */
        public static MethodActor aliasedMethod(MethodActor method) {
            if (MaxineVM.isHosted()) {
                registerAliasedMethod(method);
                return aliasedMethods.get(method);
            } else {
                MethodActor aliasedMethod = aliasedMethods.get(method);
                assert aliasedMethod != null || Heap.isInBootImage(method) || method.getAnnotation(ALIAS.class) == null : "All aliased fields and methods must be registered at boot time";
                return aliasedMethod;
            }
        }

        @HOSTED_ONLY
        private static void registerAliasedMethod(MethodActor method) {
            ALIAS alias = method.getAnnotation(ALIAS.class);
            if (alias != null) {
                MethodActor aliasedMethod = aliasedMethods.get(method);
                if (aliasedMethod == null) {
                    Class holder;
                    try {
                        holder = declaringClass(alias);
                    } catch (NoClassDefFoundError ex) {
                        if (alias.optional()) {
                            aliasedMethods.put(method, method);
                            return;
                        }
                        throw FatalError.unexpected("Could not find holder for alias " + method + " in " + alias.declaringClassName());
                    }

                    String name = alias.name();
                    if (name.isEmpty()) {
                        name = method.name();
                    }

                    SignatureDescriptor sig = alias.descriptor().isEmpty() ? method.descriptor() : SignatureDescriptor.create(alias.descriptor());
                    aliasedMethod = ClassActor.fromJava(holder).findLocalMethodActor(SymbolTable.makeSymbol(name), sig);
                    if (aliasedMethod == null) {
                        if (alias.optional()) {
                            aliasedMethods.put(method, method);
                            return;
                        }
                        throw FatalError.unexpected("Could not find target for alias " + method + " in " + holder.getName());
                    }
                    if (!alias.descriptor().isEmpty()) {
                        if (!returnType(method).isAssignableFrom(returnType(aliasedMethod))) {
                            throw FatalError.unexpected("Return type of alias " + method + " [" + returnType(method) + "] must be a superclass of aliased method's return type [" + returnType(aliasedMethod) + "]");
                        }
                        ClassActor[] params = parameterTypes(aliasedMethod);
                        ClassActor[] aliasParams = parameterTypes(method);
                        if (params.length != aliasParams.length) {
                            throw FatalError.unexpected("Alias " + method + " must have same number of parameters as the aliased method");
                        }
                        for (int i = 0; i < params.length; ++i) {
                            if (!params[i].isAssignableFrom(aliasParams[i])) {
                                throw FatalError.unexpected("Parameter type " + i + " of alias " + method + " [" + aliasParams[i] + "] must be a subclass of aliased method's corresponding parameter type [" + params[i] + "]");
                            }
                        }
                    }
                    assert aliasedMethod.isStatic() == method.isStatic() : "Alias " + method + " must be static if " + aliasedMethod + " is";
                    aliasedMethods.put(method, aliasedMethod);
                }
            }
        }

        private static ClassActor returnType(MethodActor method) {
            return ClassActor.fromJava(method.descriptor().resolveReturnType(method.holder().classLoader));
        }

        private static ClassActor[] parameterTypes(MethodActor method) {
            Class[] parameterTypes = method.descriptor().resolveParameterTypes(method.holder().classLoader);
            ClassActor[] result = new ClassActor[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; ++i) {
                result[i] = ClassActor.fromJava(parameterTypes[i]);
            }
            return result;
        }

        public static boolean isAliased(FieldActor field) {
            return aliasedFields.containsValue(field);
        }

        public static boolean isAliased(MethodActor method) {
            return aliasedMethods.containsValue(method);
        }

        private static Class declaringClass(ALIAS alias) {
            Class holder;
            if (alias.declaringClass() == ALIAS.class) {
                assert !alias.declaringClassName().isEmpty();
                holder = Classes.forName(alias.declaringClassName(), false, ALIAS.class.getClassLoader());
            } else {
                assert alias.declaringClassName().isEmpty();
                holder = alias.declaringClass();
            }
            if (!alias.innerClass().isEmpty()) {
                holder = Classes.getInnerClass(holder, alias.innerClass());
            }
            return holder;
        }
    }
}
