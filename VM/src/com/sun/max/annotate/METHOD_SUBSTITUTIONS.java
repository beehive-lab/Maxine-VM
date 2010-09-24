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
package com.sun.max.annotate;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * Denotes a class (the "substitutor") that provides an alternative implementation
 * (a method annotated by {@link SUBSTITUTE}) for at least one method in another
 * class (the {@link #g "substitutee"}).
 *
 * @see SUBSTITUTE
 *
 * @author Bernd Mathiske
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface METHOD_SUBSTITUTIONS {
    Class value() default METHOD_SUBSTITUTIONS.class;

    String hiddenClass() default "";

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
                    boolean conditional = substituteAnnotation.conditional();
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
                            ProgramError.unexpected("could not find unconditional substitutee constructor in " + substitutee + " substituted by " + substituteMethod);
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
                    MaxineVM.registerImageMethod(originalMethodActor); // TODO: loosen this requirement
                } else {
                    // Any other method in the substitutor class must be either inlined or static.
                    if (substituteMethod.getAnnotation(INLINE.class) == null && substituteMethod.getAnnotation(INTRINSIC.class) == null && !Modifier.isStatic(substituteMethod.getModifiers())) {
                        ProgramError.unexpected("method without @" + SUBSTITUTE.class.getSimpleName() + " annotation in " + substitutor + " must be static, have @" + INTRINSIC.class.getSimpleName() +
                                        "(UNSAFE_CAST) or @" + INLINE.class.getSimpleName() + " annotation: " + substituteMethod);
                    }
                }
            }
            ProgramError.check(substitutionFound, "no method with " + SUBSTITUTE.class.getSimpleName() + " annotation found in " + substitutor);
        }

        public static void processAnnotationInfo(METHOD_SUBSTITUTIONS annotation, ClassActor substitutor) {

            // These two checks make it impossible for method substitutions holders to have instance fields.
            // A substitute non-static method could never access such a field given that the receiver is
            // cast (via UNSAFE_CAST) to be an instance of the substitutee.
            ProgramError.check(substitutor.superClassActor.typeDescriptor == JavaTypeDescriptor.OBJECT, "method substitution class must directly subclass java.lang.Object");
            ProgramError.check(substitutor.localInstanceFieldActors().length == 0, "method substitution class cannot declare any dynamic fields");

            Class holder;
            if (annotation.value() != METHOD_SUBSTITUTIONS.class) {
                assert annotation.hiddenClass().isEmpty();
                holder = annotation.value();
            } else {
                assert !annotation.hiddenClass().isEmpty();
                holder = Classes.forName(annotation.hiddenClass(), false, substitutor.classLoader);
            }

            if (!annotation.innerClass().isEmpty()) {
                holder = Classes.getInnerClass(holder, annotation.innerClass());
            }
            register(holder, substitutor.toJava());
        }

        private static Method findMethod(Class declaringClass, String name, SignatureDescriptor signatureDescriptor) {
            for (Method javaMethod : declaringClass.getDeclaredMethods()) {
                if (javaMethod.getName().equals(name)) {
                    if (signatureDescriptor.parametersEqual(SignatureDescriptor.fromJava(javaMethod))) {
                        return javaMethod;
                    }
                }
            }
            return null;
        }

        private static Constructor findConstructor(Class declaringClass, SignatureDescriptor signatureDescriptor) {
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
