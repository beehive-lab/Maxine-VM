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
package test.com.sun.max.vm.jni;

import java.io.*;
import java.lang.reflect.*;

import junit.framework.*;
import test.com.sun.max.vm.*;

import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.type.*;

/**
 *
 * @author Doug Simon
 */
public class MangleTest extends MaxTestCase {

    public MangleTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MangleTest.class);
    }

    public static Test suite() {
        return new VmTestSetup(new TestCaseClassSet(AutoTest.class).toTestSuite());
    }

    private boolean mangleAndDemangle(String className, String name, String signature) {
        try {
            final String mangled = Mangle.mangleMethod(JavaTypeDescriptor.parseTypeDescriptor(className), name, signature == null ? null : SignatureDescriptor.create(signature), false);
            final Mangle.DemangledMethod demangled = Mangle.demangleMethod(mangled);
            return demangled.mangle().equals(mangled);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isOverloadedNative(Method method) {
        for (Method m : method.getDeclaringClass().getDeclaredMethods()) {
            if (Modifier.isNative(m.getModifiers()) && !m.equals(method) && m.getName().equals(method.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containedByTopLevelClass(Method method) {
        return method.getDeclaringClass().getEnclosingClass() == null;
    }

    private String mangleAndDemangle(Method method) {
        assert Modifier.isNative(method.getModifiers());
        final String mangled = Mangle.mangleMethod(method, isOverloadedNative(method));
        final Mangle.DemangledMethod demangled = Mangle.demangleMethod(mangled);
        assertEquals(demangled.mangle(), mangled);
        if (containedByTopLevelClass(method)) {
            final Method demangledMethod = demangled.toJava(method.getDeclaringClass().getClassLoader());
            if (demangledMethod == null) {
                ProgramWarning.message("cannot demangle " + mangled + " to an existing Method object");
            } else {
                assertTrue(method.equals(demangledMethod));
            }
        }
        return mangled;
    }

    /**
     * Gets the output from the standard javah tool for a given class.
     */
    private String javah(Class declaringClass) {
        if (declaringClass.isAnonymousClass()) {
            return null;
        }
        try {
            final File tempFile = File.createTempFile("MangleTest_", ".h");
            final String[] args = {"-o", tempFile.getAbsolutePath(), "-classpath", Classpath.fromSystem()/* JavaProject.getClassPath(true)*/.toString(), declaringClass.getName()};
            ToolChain.javah(args);
            final String headerFile = new String(Files.toChars(tempFile));
            tempFile.delete();
            return headerFile;
        } catch (IOException e) {
            e.printStackTrace();
            fail();
            return null;
        }
    }

    private void mangleAndDemangle(Class declaringClass) {
        String headerFile = null;
        boolean headerFileGenerated = false;
        try {
            for (Method method : declaringClass.getDeclaredMethods()) {
                if (Modifier.isNative(method.getModifiers()) && method.getAnnotation(C_FUNCTION.class) == null) {
                    if (!headerFileGenerated) {
                        headerFile = javah(declaringClass);
                        headerFileGenerated = true;
                    }
                    String mangled = mangleAndDemangle(method);
                    if (headerFile != null && !headerFile.contains(mangled)) {
                        if (containedByTopLevelClass(method)) {
                            fail("can't find '" + mangled + "' in javah generated file");
                        } else {
                            ProgramWarning.message("can't find '" + mangled + "' in javah generated for non top-level " + declaringClass);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError e) {
            ProgramWarning.message(e + " while trying to get the declared methods of " + declaringClass);
        }
    }

    public void test_mangle() {
        Trace.on(1);
        class LocalClass {
            native void simple(int arg1, String arg2, Exception arg3);
            native void underscore_(int arg1, String arg2, Exception arg3);
            public native void overload();
            native String overload(int arg1, String arg2);
            native String overload(String arg2);

            final Object localAnonymousClass = new Object() {
                native void simple(int arg1, String arg2, Exception arg3);
                native void underscore_(int arg1, String arg2, Exception arg3);
                public native void overload();
                native String overload(int arg1, String arg2);
                native String overload(String arg2);
            };

            class InnerClass {
                native void simple(int arg1, String arg2, Exception arg3);
                native void underscore_(int arg1, String arg2, Exception arg3);
                public native void overload();
                native String overload(int arg1, String arg2);
                native String overload(String arg2);
            }
        }

        final Object anonymousClass = new Object() {
            native void simple(int arg1, String arg2, Exception arg3);
            native void underscore_(int arg1, String arg2, Exception arg3);
            public native void overload();
            native String overload(int arg1, String arg2);
            native String overload(String arg2);
        };

        // Reflection based
        mangleAndDemangle(MangleTest.class);
        mangleAndDemangle(NestedClass.class);
        mangleAndDemangle(InnerClass.class);
        mangleAndDemangle(LocalClass.class);
        mangleAndDemangle(anonymousClass.getClass());
        mangleAndDemangle(new LocalClass().localAnonymousClass.getClass());
        mangleAndDemangle(LocalClass.InnerClass.class);

        mangleAndDemangle(MakeStackVariable.class);
    }

    public void test_max() {
        // Maxine classes
        new ClassSearch() {
            @Override
            protected boolean visitClass(boolean isArchiveEntry, String className) {
                if (isArchiveEntry || className.endsWith("package-info")) {
                    return true;
                }
                if (!className.startsWith("com.sun.max")) {
                    return true;
                }
                if (className.startsWith("com.sun.max.asm")) {
                    // Too many generated methods
                    return true;
                }
                if (className.startsWith("com.sun.max.vm.asm.")) {
                    // Too many generated methods
                    return true;
                }
                try {
                    final Class<?> clazz = Class.forName(className, false, MangleTest.class.getClassLoader());
                    mangleAndDemangle(clazz);
                } catch (ClassNotFoundException e) {
                    ProgramWarning.message(e.toString());
                }
                return true;
            }

        }.run(Classpath.fromSystem());

        // String based
        assertTrue(mangleAndDemangle("Ljava/lang/Class;", "forName", null));
    }

    public native void simple(int arg1, String arg2, Exception arg3);
    public native void underscore_(int arg1, String arg2, Exception arg3);
    public native void overload();
    public native String overload(int arg1, String arg2);
    public native String overload(String arg2);

    static native String staticOverload(int arg1, String arg2);
    static native String staticOverload(String arg2);

    private static final class NestedClass {
        native void simple(int arg1, String arg2, Exception arg3);
        native void underscore_(int arg1, String arg2, Exception arg3);
        public native void overload();
        native String overload(int arg1, String arg2);
        native String overload(String arg2);

        static native String staticOverload(int arg1, String arg2);
        static native String staticOverload(String arg2);
    }

    private class InnerClass {
        native void simple(int arg1, String arg2, Exception arg3);
        native void underscore_(int arg1, String arg2, Exception arg3);
        public native void overload();
        native String overload(int arg1, String arg2);
        native String overload(String arg2);
    }
}
