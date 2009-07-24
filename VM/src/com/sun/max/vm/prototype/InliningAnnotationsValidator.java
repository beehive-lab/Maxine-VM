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
package com.sun.max.vm.prototype;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * A utility class for validating a given method with respect to usage of the {@link INLINE} and {@link NEVER_INLINE} annotations.
 * This check is expensive and so should only be performed when {@linkplain MaxineVM#isPrototyping() prototyping}.
 *
 * @author Doug Simon
 */
public final class InliningAnnotationsValidator {
    private InliningAnnotationsValidator() {
    }

    /**
     * Validates a given method with respect to usage of the INLINE annotation.
     * This check is expensive and so should only be performed when
     * {@linkplain MaxineVM#isPrototyping() prototyping}.
     */
    public static void apply(ClassMethodActor classMethodActor) {
        if (!MaxineVM.isMaxineClass(classMethodActor.holder())) {
            return;
        }
        final Method javaMethod = classMethodActor.toJava();
        if (checked.contains(javaMethod)) {
            return;
        }
        checked.add(javaMethod);

        final INLINE annotation = javaMethod.getAnnotation(INLINE.class);

        if (javaMethod.isBridge()) {
            // Only the method called by the bridge (i.e. the target of the bridge) needs to be checked as there is
            // no way to apply an annotation to a bridge method
            assert annotation == null;
            return;
        }

        if (annotation != null) {
            check(!Modifier.isAbstract(javaMethod.getModifiers()), "Cannot apply INLINE annotation to abstract method: " + format(javaMethod));
            check(javaMethod.getAnnotation(NEVER_INLINE.class) == null, "Cannot apply INLINE and NEVER_INLINE to the same method: " + format(javaMethod));
        }

        if (Modifier.isStatic(javaMethod.getModifiers())) {
            check(annotation == null || !annotation.override(),
                            "Static method should only have INLINE annotation with the \"override\" element set to false: " + format(javaMethod));
            return;
        }

        Method overidingMethod = javaMethod;
        Method overriddenMethod = overriddenMethod(javaMethod);
        INLINE topOverriddenMethodAnnotation = null;
        Method topOverriddenMethod = null;
        while (overriddenMethod != null) {
            final INLINE overriddenMethodAnnotation = overriddenMethod.getAnnotation(INLINE.class);
            check((overriddenMethodAnnotation == null) == (annotation == null), "Overridden method must have INLINE annotation if overiding method does and vice versa: " + format(overidingMethod) + " overrides " + format(overriddenMethod));
            topOverriddenMethodAnnotation = overriddenMethodAnnotation;
            topOverriddenMethod = overriddenMethod;
            overidingMethod = overriddenMethod;
            overriddenMethod = overriddenMethod(overriddenMethod);
            if (overriddenMethodAnnotation != null && overriddenMethod != null) {
                check(!overriddenMethodAnnotation.override(),
                                "Overiding method must have INLINE annotation with the \"override\" element set to false: " + format(overidingMethod));
            }
        }

        if (topOverriddenMethodAnnotation != null) {
            check(topOverriddenMethodAnnotation.override(),
                            "Highest overridden method must have INLINE annotation with the \"override\" element set to true: " + format(overidingMethod) + " overrides " + format(topOverriddenMethod));
        } else {
            if (annotation != null) {
                check(Modifier.isPrivate(javaMethod.getModifiers()) || Modifier.isFinal(javaMethod.getModifiers()) || Modifier.isFinal(javaMethod.getDeclaringClass().getModifiers()) || annotation.override(),
                                "Method annotated with INLINE must be private or final or must have the \"override\" element set to true: " + format(javaMethod));
            }
        }
    }

    /**
     * Finds the method in the class hierarchy that is immediately overridden by a given method. This search
     * excludes the interface hierarchy. Method M1 immediately overrides method M2 if there is no other
     * method M3 in the class hierarchy such that M3 overrides M2 and M1 overrides M3.
     *
     * @return {@code null} if there is no method overridden by {@code javaMethod} in the class hierarchy
     */
    private static Method overriddenMethod(Method javaMethod) {
        final Class<?> superClass = javaMethod.getDeclaringClass().getSuperclass();
        if (superClass != null) {
            try {
                final Method superMethod = superClass.getMethod(javaMethod.getName(), javaMethod.getParameterTypes());
                if (superMethod.getDeclaringClass().isInterface()) {
                    return null;
                }
                return superMethod;
            } catch (NoSuchMethodException e) {
            }
        }
        return null;
    }

    /**
     * Formats a method for use in an error/warning message.
     */
    private static String format(Method javaMethod) {
        final StringBuilder sb = new StringBuilder(javaMethod.getDeclaringClass().getName()).append('.').append(javaMethod.getName()).append('(');
        final Class[] parameterTypes = javaMethod.getParameterTypes();
        for (int i = 0; i != parameterTypes.length; ++i) {
            sb.append(parameterTypes[i].getSimpleName());
            if (i != parameterTypes.length - 1) {
                sb.append(", ");
            }
        }
        return sb.append(')').toString();
    }

    private static void check(boolean condition, String errorMessage) {
        ProgramError.check(condition, errorMessage);
    }

    private static IdentityHashSet<Method> checked = new IdentityHashSet<Method>();
}
