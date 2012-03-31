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
package com.sun.max.vm.hosted;

import static com.sun.max.vm.hosted.MethodFinder.PatternType.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * A utility for {@linkplain #match(String[]) finding} a set of methods
 * based on a class path and one or more patterns.
 * <p>
 * A pattern is a class name pattern followed by an optional method name
 * pattern separated by a ':' further followed by an optional signature:
 * <pre>
 *   &lt;class name&gt;[:&lt;method name&gt;[:&lt;signature&gt;]]
 * </pre>
 * For example, the list of patterns:
 * <pre>
 *      "Object:wait", "String", "Util:add:(int,float)"
 * </pre>
 * will match all methods in a class whose name contains {@code "Object"} where the
 * method name contains {@code "wait"}, all methods in a class whose name
 * contains {@code "String"} and all methods in any class whose name
 * contains {@code "Util"}, the method name contains {@code "add"} and the
 * method signature is {@code (int, float)}.
 * <p>
 * The type of matching performed for a given class/method name is determined
 * by the position of '^' in the pattern name as follows:
 * <pre>
 * Position of '^'   | Match algorithm
 *  ------------------+------------------
 *  start AND end     | Equality
 *  start             | Prefix
 *  end               | Suffix
 *  absent            | Substring
 * </pre>
 *
 * For example, {@code "^java.util:^toString^"} matches all methods named {@code "toString"} in
 * any class whose name starts with {@code "java.util"}.
 * <p>
 * The matching performed on a signature is always a substring test. Signatures can
 * specified either in Java source syntax (e.g. {@code "int,String"}) or JVM internal syntax
 * (e.g. {@code "IFLjava/lang/String;"}). The latter must always use fully qualified type
 * names where as the former must not.
 * <p>
 * Any pattern starting with {@code '!'} is an exclusion specification. Any class or method
 * whose name contains an exclusion string (the exclusion specification minus the
 * leading {@code '!'}) is excluded.
 */
public class MethodFinder {

    enum PatternType {
        EXACT("matching") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.equals(pattern);
            }
        },
        PREFIX("starting with") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.startsWith(pattern);
            }
        },
        SUFFIX("ending with") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.endsWith(pattern);
            }
        },
        CONTAINS("containing") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.contains(pattern);
            }
        };

        final String relationship;

        private PatternType(String relationship) {
            this.relationship = relationship;
        }

        abstract boolean matches(String input, String pattern);

        @Override
        public String toString() {
            return relationship;
        }
    }

    public static class PatternMatcher {
        public final String pattern;
        // 1: exact, 2: prefix, 3: suffix, 4: substring
        public final PatternType type;

        public PatternMatcher(String pattern) {
            if (pattern.startsWith("^") && pattern.endsWith("^") && pattern.length() != 1) {
                this.type = EXACT;
                this.pattern = pattern.substring(1, pattern.length() - 1);
            } else if (pattern.startsWith("^")) {
                this.type = PREFIX;
                this.pattern = pattern.length() == 1 ? "" : pattern.substring(1);
            } else if (pattern.endsWith("^")) {
                this.type = SUFFIX;
                this.pattern = pattern.substring(0, pattern.length() - 1);
            } else {
                this.type = CONTAINS;
                this.pattern = pattern;
            }
        }

        boolean matches(String input) {
            return type.matches(input, pattern);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PatternMatcher) {
                PatternMatcher pm = (PatternMatcher) obj;
                return pm.pattern.equals(pattern) && pm.type.equals(type);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return pattern.hashCode();
        }
    }

    public MethodFinder() {
    }

    /**
     * Gets the list of methods matching a given set of patterns.
     *
     * @param patterns a list of patterns used to filter the methods found on the class path
     * @param classpath
     * @param classLoader
     * @param nonFatalErrors list to which non-fatal errors are appended during the search. If {@code null}, then a
     *            {@linkplain ProgramWarning warning} is logged
     * @return
     */
    public List<MethodActor> find(String[] patterns, Classpath classpath, ClassLoader classLoader, List<Throwable> nonFatalErrors) {

        final List<MethodActor> methods = new ArrayList<MethodActor>();
        final Set<String> exclusions = new HashSet<String>();

        for (int i = 0; i != patterns.length; ++i) {
            final String argument = patterns[i];
            if (argument.startsWith("!")) {
                exclusions.add(argument.substring(1));
                patterns[i] = null;
            }
        }

        for (int i = 0; i != patterns.length; ++i) {
            final String argument = patterns[i];
            if (argument == null) {
                continue;
            }
            final int colonIndex = argument.indexOf(':');
            final PatternMatcher classNamePattern = new PatternMatcher(colonIndex == -1 ? argument : argument.substring(0, colonIndex));

            // search for matching classes on the class path
            final List<String> matchingClasses = new ArrayList<String>();

            if (classNamePattern.type == EXACT) {
                matchingClasses.add(classNamePattern.pattern);
            } else {
                new ClassSearch() {
                    @Override
                    protected boolean visitClass(String className) {
                        if (!className.endsWith("package-info")) {
                            if (classNamePattern.matches(className)) {
                                for (String exclusion : exclusions) {
                                    if (className.contains(exclusion)) {
                                        return true;
                                    }
                                }
                                matchingClasses.add(className);
                            }
                        }
                        return true;
                    }
                }.run(classpath);
            }

            // for all found classes, search for matching methods
            for (String className : matchingClasses) {
                try {
                    Class<?> javaClass = null;
                    try {
                        javaClass = Class.forName(className, false, classLoader);
                    } catch (NoClassDefFoundError noClassDefFoundError) {
                        throw new ClassNotFoundException(className, noClassDefFoundError);
                    }

                    final ClassActor classActor = getClassActor(javaClass);
                    if (classActor == null) {
                        continue;
                    }

                    if (colonIndex == -1) {
                        // Class only: select all methods in class
                        for (MethodActor actor : classActor.localStaticMethodActors()) {
                            addMethod(methods, actor, exclusions);
                        }
                        for (MethodActor methodActor : classActor.localVirtualMethodActors()) {
                            addMethod(methods, methodActor, exclusions);
                        }
                        for (MethodActor methodActor : classActor.localInterfaceMethodActors()) {
                            addMethod(methods, methodActor, exclusions);
                        }
                    } else {
                        // a method pattern was specified, find matching methods
                        final int secondColonIndex = argument.indexOf(':', colonIndex + 1);
                        final PatternMatcher methodNamePattern;
                        String signature;
                        if (secondColonIndex == -1) {
                            methodNamePattern = new PatternMatcher(argument.substring(colonIndex + 1));
                            signature = null;
                        } else {
                            methodNamePattern = new PatternMatcher(argument.substring(colonIndex + 1, secondColonIndex));
                            signature = argument.substring(secondColonIndex + 1);
                            // Normalize specified signature to have only a single space after any commas
                            signature = signature.replaceAll(", *", ", ");
                        }
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localStaticMethodActors(), exclusions);
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localVirtualMethodActors(), exclusions);
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localInterfaceMethodActors(), exclusions);
                    }
                } catch (ClassNotFoundException classNotFoundException) {
                    if (nonFatalErrors != null) {
                        nonFatalErrors.add(classNotFoundException);
                    } else {
                        ProgramWarning.message(classNotFoundException.toString() + (classNotFoundException.getCause() == null ? "" : " (cause: " + classNotFoundException.getCause() + ")"));
                    }
                }
            }
        }
        return methods;
    }

    /**
     * Adds a class to the list of classes that will be searched for matching methods.
     * Subclasses can override this to implement additional filtering.
     *
     * @param pattern the pattern that matched {@code className}
     * @param className the name of a class that will be processed
     * @param matchingClasses the list of classes that will be processed
     */
    protected void addClassToProcess(PatternMatcher pattern, String className, List<String> matchingClasses) {
        matchingClasses.add(className);
    }

    /**
     * Adds a method to a list of matching methods.
     * Subclasses can override this to implement additional filtering.
     *
     * @param method the method to add
     * @param methods the list to which {@code method} is added
     */
    protected void addMethod(MethodActor method, List<MethodActor> methods) {
        methods.add(method);
    }

    private void addMethod(List<MethodActor> methods, MethodActor methodActor, Set<String> exclusions) {
        for (String exclusion : exclusions) {
            if (methodActor.name.string.contains(exclusion)) {
                return;
            }
        }
        addMethod(methodActor, methods);
    }

    /**
     * Gets a {@link ClassActor} corresponding to a given {@link Class} instance.
     */
    protected ClassActor getClassActor(Class<?> javaClass) {
        return ClassActor.fromJava(javaClass);
    }

    private void addMatchingMethods(final List<MethodActor> methods, final ClassActor classActor, final PatternMatcher methodNamePattern, final String signature, MethodActor[] methodActors, Set<String> exclusions) {
        for (final MethodActor method : methodActors) {
            if (methodNamePattern.matches(method.name.toString())) {
                if (signature != null) {
                    final SignatureDescriptor methodSignature = method.descriptor();
                    if (methodSignature.string.contains(signature)) {
                        addMethod(methods, method, exclusions);
                    } else {
                        String javaSignature = methodSignature.toJavaString(false, true);
                        if (javaSignature.contains(signature)) {
                            addMethod(methods, method, exclusions);
                        }
                    }
                } else {
                    addMethod(methods, method, exclusions);
                }
            }
        }
    }

    @HOSTED_ONLY
    public static class Usage {
        public static void main(String[] args) {

        }
    }

    public static void main(String[] args) {
        System.out.print("Creating Java prototype... ");
        JavaPrototype.initialize();
        System.out.println("done");

        MethodFinder matcher = new MethodFinder();
        List<MethodActor> methods = matcher.find(args, Classpath.fromSystem(), MethodFinder.class.getClassLoader(), null);
        System.out.println("Matched " + methods.size() + " methods:");
        for (MethodActor method : methods) {
            System.out.println(method.format("  %H.%n(%p)"));
        }
    }
}
