/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Denotes a class (the "substitutor") that provides an alternative implementation
 * (a method annotated by {@link SUBSTITUTE}) for at least one method in another
 * class (the {@link #g "substitutee"}).
 *
 * @see SUBSTITUTE
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface METHOD_SUBSTITUTIONS {

    /**
     * Specifies the substitutee class.
     *
     * If the default value is specified for this element, then a non-default
     * value must be given for the {@link #className()} element.
     */
    Class value() default METHOD_SUBSTITUTIONS.class;

    /**
     * Specifies the substitutee class.
     * This method is provided for cases where the substitutee class
     * is not accessible (according to Java language access control rules).
     *
     * If the default value is specified for this element, then a non-default
     * value must be given for the {@link #value()} element.
     */
    String className() default "";

    /**
     * Specifies the suffix of the substitutee class name when it is an inner class.
     */
    String innerClass() default "";

    public static final class Static {
        private Static() {
        }

        /**
         * A map from a method that is substituted to the method that substitutes it.
         */
        private static final HashMap<ClassMethodActor, ClassMethodActor> originalToSubstitute = new HashMap<ClassMethodActor, ClassMethodActor>();

        /**
         * The converse mapping.
         */
        private static final HashMap<ClassMethodActor, ClassMethodActor> substituteToOriginal = new HashMap<ClassMethodActor, ClassMethodActor>();

        /**
         * @param substitutee a class that has one or more methods to be substituted
         * @param substitutor a class that provides substitute implementations for one or more methods in
         *            {@code substitutee}
         */
        @HOSTED_ONLY
        private static void register(Class substitutee, Class substitutor) {
            boolean substitutionFound = false;
            for (Method substituteMethod : substitutor.getDeclaredMethods()) {
                final SUBSTITUTE substituteAnnotation = substituteMethod.getAnnotation(SUBSTITUTE.class);
                if (substituteAnnotation != null) {
                    substitutionFound = true;
                    String substituteName = substituteAnnotation.value();
                    if (substituteName.length() == 0) {
                        substituteName = substituteMethod.getName();
                    }
                    boolean conditional = substituteAnnotation.optional();
                    boolean isConstructor = substituteAnnotation.constructor();

                    final ClassMethodActor originalMethodActor;
                    Constructor constructor = null;
                    Method originalMethod = null;

                    if (isConstructor) {
                        constructor = findConstructor(substitutee, SignatureDescriptor.fromJava(substituteMethod));
                        if (constructor == null) {
                            if (conditional) {
                                Trace.line(1, "Substitutee for " + substituteMethod + " not found - skipping");
                                continue;
                            }
                            throw ProgramError.unexpected("could not find unconditional substitutee constructor in " + substitutee + " substituted by " + substituteMethod);
                        }
                        try {
                            originalMethodActor = (ClassMethodActor) MethodActor.fromJavaConstructor(constructor);
                        } catch (NoSuchMethodError e) {
                            if (conditional) {
                                continue;
                            }
                            throw e;
                        }
                    } else {
                        originalMethod = findMethod(substitutee, substituteName, SignatureDescriptor.fromJava(substituteMethod));
                        if (originalMethod == null) {
                            if (conditional) {
                                Trace.line(1, "Substitutee for " + substituteMethod + " not found - skipping");
                                continue;
                            }
                            ProgramError.unexpected("could not find unconditional substitutee method in " + substitutee + " substituted by " + substituteMethod);
                        }

                        try {
                            originalMethodActor = ClassMethodActor.fromJava(originalMethod);
                        } catch (NoSuchMethodError e) {
                            if (conditional) {
                                continue;
                            }
                            throw e;
                        }
                    }

                    ClassMethodActor substituteMethodActor = ClassMethodActor.fromJava(substituteMethod);
                    if (originalToSubstitute.put(originalMethodActor, substituteMethodActor) != null) {
                        ProgramError.unexpected("a substitute has already been registered for " + (isConstructor ? constructor : originalMethod));
                    }
                    if (substituteToOriginal.put(substituteMethodActor, originalMethodActor) != null) {
                        ProgramError.unexpected("only one original method per substitute allowed - " + substituteMethod);
                    }
                    Trace.line(2, "Substituted " + originalMethodActor.format("%h.%n(%p)"));
                    Trace.line(2, "       with " + substituteMethodActor.format("%h.%n(%p)"));
                    originalMethodActor.setFlagsFromSubstitute(substituteMethodActor);
                    //MaxineVM.registerImageMethod(originalMethodActor); // TODO: loosen this requirement
                } else {
                    // Any other method in the substitutor class must be either inlined or static.
                    if (substituteMethod.getAnnotation(INLINE.class) == null &&
                        substituteMethod.getAnnotation(ALIAS.class) == null &&
                        substituteMethod.getAnnotation(INTRINSIC.class) == null &&
                        !Modifier.isStatic(substituteMethod.getModifiers())) {
                        ProgramError.unexpected("method without @" + SUBSTITUTE.class.getSimpleName() + " annotation in " + substitutor + " must be static, have @" + INTRINSIC.class.getSimpleName() +
                                        "(UNSAFE_CAST) or @" + INLINE.class.getSimpleName() + " annotation: " + substituteMethod);
                    }
                }
            }
            ProgramError.check(substitutionFound, "no method with " + SUBSTITUTE.class.getSimpleName() + " annotation found in " + substitutor);
        }

        @HOSTED_ONLY
        public static void processAnnotationInfo(METHOD_SUBSTITUTIONS annotation, ClassActor substitutor) {

            // These two checks make it impossible for method substitutions holders to have (non-alias) instance fields.
            // A substitute non-static method could never access such a field given that the receiver is
            // cast (via UNSAFE_CAST) to be an instance of the substitutee.
            ProgramError.check(substitutor.superClassActor.typeDescriptor == JavaTypeDescriptor.OBJECT, "method substitution class must directly subclass java.lang.Object");
            for (FieldActor field : substitutor.localInstanceFieldActors()) {
                if (field.getAnnotation(ALIAS.class) == null) {
                    throw FatalError.unexpected("method substitution class cannot declare any non-aliased instance fields");
                }
            }

            Class holder;
            if (annotation.value() != METHOD_SUBSTITUTIONS.class) {
                assert annotation.className().isEmpty();
                holder = annotation.value();
            } else {
                assert !annotation.className().isEmpty();
                holder = Classes.forName(annotation.className(), false, substitutor.classLoader);
            }

            if (!annotation.innerClass().isEmpty()) {
                holder = Classes.getInnerClass(holder, annotation.innerClass());
            }
            register(holder, substitutor.toJava());
        }

        @HOSTED_ONLY
        public static Method findMethod(Class declaringClass, String name, SignatureDescriptor signatureDescriptor) {
            for (Method javaMethod : declaringClass.getDeclaredMethods()) {
                if (javaMethod.getName().equals(name)) {
                    if (signatureDescriptor.parametersEqual(SignatureDescriptor.fromJava(javaMethod))) {
                        return javaMethod;
                    }
                }
            }
            return null;
        }

        @HOSTED_ONLY
        public static Constructor findConstructor(Class declaringClass, SignatureDescriptor signatureDescriptor) {
            for (Constructor constructor : declaringClass.getDeclaredConstructors()) {
                if (signatureDescriptor.parametersEqual(SignatureDescriptor.fromJava(constructor))) {
                    return constructor;
                }
            }
            return null;
        }

        /**
         * Searches for a substitute implementation for a given method.
         *
         * @param originalMethod
         * @return a substitute implementation for {@code javaMethod} or null if no substitution is found
         */
        public static ClassMethodActor findSubstituteFor(ClassMethodActor originalMethod) {
            return originalToSubstitute.get(originalMethod);
        }

        /**
         * Searches for the method that is substituted by the given method.
         */
        public static ClassMethodActor findOriginal(ClassMethodActor substituteMethod) {
            return substituteToOriginal.get(substituteMethod);
        }
    }
}
