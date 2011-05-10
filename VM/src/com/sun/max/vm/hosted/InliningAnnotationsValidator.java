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
package com.sun.max.vm.hosted;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * A utility class for validating a given method with respect to usage of the {@link INLINE} and {@link NEVER_INLINE} annotations.
 * This check is expensive and so should only be performed when {@linkplain MaxineVM#isHosted() bootstrapping}.
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public final class InliningAnnotationsValidator {
    private InliningAnnotationsValidator() {
    }

    /**
     * Validates a given method with respect to usage of the INLINE annotation.
     * This check is expensive and so should only be performed when
     * {@linkplain MaxineVM#isHosted() bootstrapping}.
     */
    public static void apply(ClassMethodActor classMethodActor) {
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

    private static Set<Method> checked = CiUtil.newIdentityHashSet();
}
