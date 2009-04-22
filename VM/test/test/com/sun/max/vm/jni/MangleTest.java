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
package test.com.sun.max.vm.jni;

import java.io.*;
import java.lang.reflect.*;

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

    private boolean mangleAndDemangle(String className, String name, String signature) {
        try {
            final String mangled = Mangle.mangleMethod(JavaTypeDescriptor.parseTypeDescriptor(className), name, signature == null ? null : SignatureDescriptor.create(signature));
            final Mangle.DemangledMethod demangled = Mangle.demangleMethod(mangled);
//System.out.printAddress("  mangled: " + mangled);
//System.out.printAddress("demangled: " + demangled);
//System.out.printAddress();
            return demangled.mangle().equals(mangled);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isOverloaded(Method method) {
        for (Method m : method.getDeclaringClass().getDeclaredMethods()) {
            if (!m.equals(method) && m.getName().equals(method.getName())) {
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
        final String mangled = Mangle.mangleMethod(method, isOverloaded(method));
        final Mangle.DemangledMethod demangled = Mangle.demangleMethod(mangled);
//      System.out.printAddress("  mangled: " + mangled);
//      System.out.printAddress("demangled: " + demangled);
//      System.out.printAddress();
        assertTrue(demangled.mangle().equals(mangled));
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
            final String[] args = {"-o", tempFile.getAbsolutePath(), "-classpath", JavaProject.getClassPath(true).toString(), declaringClass.getName()};
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
                    final String mangled = mangleAndDemangle(method);
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

            final Object _localAnonymousClass = new Object() {
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
        mangleAndDemangle(new LocalClass()._localAnonymousClass.getClass());
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
                if (!className.startsWith(new com.sun.max.Package().name())) {
                    return true;
                }
                if (className.startsWith(new com.sun.max.asm.Package().name())) {
                    // Too many generated methods
                    return true;
                }
                if (className.startsWith(new com.sun.max.vm.asm.Package().name())) {
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
