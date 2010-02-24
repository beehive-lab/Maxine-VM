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

import com.sun.c1x.bytecode.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.type.*;

/**
 * Denotes a class (the "substitutor") that provides an alternative implementation
 * (a method annotated by {@link SUBSTITUTE}) for at least one method in another
 * class (the {@link #value "substitutee"}).
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
        private static final GrowableMapping<ClassMethodActor, ClassMethodActor> originalToSubstitute = HashMapping.createIdentityMapping();

        /**
         * The converse mapping.
         */
        private static final GrowableMapping<ClassMethodActor, ClassMethodActor> substituteToOriginal = HashMapping.createIdentityMapping();

        /**
         * @param substitutee a class that has one or more methods to be substituted
         * @param substitutor a class that provides substitute implementations for one or more methods in {@code substitutee}
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
                    final Method originalMethod = findMethod(substitutee, substituteName, SignatureDescriptor.fromJava(substituteMethod));
                    ProgramError.check(originalMethod != null, "could not find method in " + substitutee + " substituted by " + substituteMethod);
                    final ClassMethodActor originalMethodActor = ClassMethodActor.fromJava(originalMethod);
                    if (originalToSubstitute.put(originalMethodActor, ClassMethodActor.fromJava(substituteMethod)) != null) {
                        ProgramError.unexpected("a substitute has already been registered for " + originalMethod);
                    }
                    if (substituteToOriginal.put(ClassMethodActor.fromJava(substituteMethod), originalMethodActor) != null) {
                        ProgramError.unexpected("only one original method per substitute allowed - " + substituteMethod);
                    }
                    originalMethodActor.beUnsafe();
                    MaxineVM.registerImageMethod(originalMethodActor); // TODO: loosen this requirement
                } else {
                    // Any other method in the substitutor class must be either inlined or static.
                    if (substituteMethod.getAnnotation(INLINE.class) == null &&
                                    !Intrinsics.isUnsafeCast(substituteMethod) &&
                                    !Modifier.isStatic(substituteMethod.getModifiers())) {
                        ProgramError.unexpected(
                            "method without @" + SUBSTITUTE.class.getSimpleName() + " annotation in " + substitutor +
                            " must be static, have @" + INTRINSIC.class.getSimpleName() + "(UNSAFE_CAST) or @" + INLINE.class.getSimpleName() + " annotation: " + substituteMethod);
                    }
                }
            }
            ProgramError.check(substitutionFound, "no method with " + SUBSTITUTE.class.getSimpleName() + " annotation found in " + substitutor);
        }

        public static void processAnnotationInfo(AnnotationInfo annotationInfo, ClassActor substitutor) {
            assert annotationInfo.annotationTypeDescriptor().equals(JavaTypeDescriptor.forJavaClass(METHOD_SUBSTITUTIONS.class));

            // These two checks make it impossible for method substitutions holders to have instance fields.
            // A substitute non-static method could never access such a field given that the receiver is
            // cast (via UNSAFE_CAST) to be an instance of the substitutee.
            ProgramError.check(substitutor.superClassActor.typeDescriptor == JavaTypeDescriptor.OBJECT, "method substitution class must directly subclass java.lang.Object");
            ProgramError.check(substitutor.localInstanceFieldActors().length == 0, "method substitution class cannot declare any dynamic fields");

            Class holder = null;
            final AnnotationInfo.NameElementPair[] nameElementPairs = annotationInfo.nameElementPairs();
            final AnnotationInfo.NameElementPair outerClassNameElementPair = nameElementPairs[0];
            if (outerClassNameElementPair.name().equals("value")) {
                final AnnotationInfo.TypeElement typeElement = (AnnotationInfo.TypeElement) outerClassNameElementPair.element();
                holder = typeElement.typeDescriptor().resolveType(substitutor.classLoader);
            } else {
                assert outerClassNameElementPair.name().equals("hiddenClass");
                final AnnotationInfo.StringElement stringElement = (AnnotationInfo.StringElement) outerClassNameElementPair.element();
                holder = Classes.forName(stringElement.string(), false, substitutor.classLoader);
            }
            if (nameElementPairs.length > 1) {
                final AnnotationInfo.NameElementPair innerClassNameElementPair = nameElementPairs[1];
                assert innerClassNameElementPair.name().equals("innerClass");
                final AnnotationInfo.StringElement stringElement = (AnnotationInfo.StringElement) innerClassNameElementPair.element();
                if (!stringElement.string().isEmpty()) {
                    holder = Classes.getInnerClass(holder, stringElement.string());
                    ProgramError.check(holder != null, "method substitution inner class not found: " + holder + "." + stringElement.string());
                }
            }
            ProgramError.check(holder != null, "method substitution class not found");
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
