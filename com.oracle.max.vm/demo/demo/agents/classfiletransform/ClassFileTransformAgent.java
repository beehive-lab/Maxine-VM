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
package demo.agents.classfiletransform;

import java.lang.instrument.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * A test for the basic machinery of bytecode transformation on class loading.
 * In particular it checks for (incorrect) transformation of the same classloader/classname pair
 * by different threads.
 */
public class ClassFileTransformAgent implements ClassFileTransformer {

    private static boolean verbose;
    private static Pattern classPattern;
    private static ConcurrentMap<ClClass, ArrayList<Thread>> cclassMap = new ConcurrentHashMap<ClClass, ArrayList<Thread>>();

    /**
     * Uniquely identifies an attempted definition for a given class in a given classloader.
     */
    private static class ClClass {
        final ClassLoader classLoader;
        final String className;

        ClClass(ClassLoader classLoader, String className) {
            this.classLoader = classLoader;
            this.className = className;
        }

        @Override
        public boolean equals(Object other) {
            ClClass otherClClass = (ClClass) other;
            return classLoader == otherClClass.classLoader && className.equals(otherClClass.className);
        }

        @Override
        public int hashCode() {
            return classLoader.hashCode() ^ className.hashCode();
        }
    }
    public static void premain(String arg, Instrumentation instrumentation) {
        String pattern = ".*";
        if (arg == null) {
            System.out.println("arg is null");
        } else {
            String[] args = arg.split(",");
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("verbose")) {
                    verbose = true;
                } else {
                    pattern = args[i];
                }
            }
        }
        classPattern = Pattern.compile(pattern);
        instrumentation.addTransformer(new ClassFileTransformAgent());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class< ? > classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ArrayList<Thread> v = cclassMap.putIfAbsent(new ClClass(loader, className), currentThreadList());
        if (v != null) {
            v.add(Thread.currentThread());
            System.out.printf("class %s transformed by %s, also by %s%n", className, v.get(0), v.get(1));
        }
        if (classPattern.matcher(className).matches()) {
            if (verbose) {
                System.out.printf("%s ClassTransformAgent.transform: %s%n", Thread.currentThread(), className);
            }
        }
        return null;
    }

    private static ArrayList<Thread> currentThreadList() {
        ArrayList<Thread>  list = new ArrayList<Thread>();
        list.add(Thread.currentThread());
        return list;
    }
}
