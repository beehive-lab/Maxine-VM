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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;

import java.security.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Implements substitutions necessary for {@link ClassLoader}.
 */
@METHOD_SUBSTITUTIONS(ClassLoader.class)
public final class JDK_java_lang_ClassLoader {

    private JDK_java_lang_ClassLoader() {
    }

    /**
     * Cast this {@code JDK_java_lang_ClassLoader} instance to a {@code ClassLoader} instance.
     * @return a view of this object as a class loader
     */
    @INTRINSIC(UNSAFE_CAST)
    private native ClassLoader thisClassLoader();

    /**
     * Registers native methods associated with this class using the JNI mechanisms. This implementation
     * has no native methods, thus this method does nothing.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    /**
     * Creates a class from a classfile represented as a byte array.
     *
     * @see java.lang.ClassLoader#defineClass0(String, byte[], int, int, ProtectionDomain)
     *
     * @param name the name of the class
     * @param bytes a representation of the classfile
     * @param offset offset into the array at which the classfile begins
     * @param length the length of the classfile
     * @param protectionDomain the protection domain in which to create the class
     * @return a new {@code Class} instance representing the class
     */
    @SUBSTITUTE
    private Class defineClass0(String name, byte[] bytes, int offset, int length, ProtectionDomain protectionDomain) {
        return ClassfileReader.defineClassActor(name, thisClassLoader(), bytes, offset, length, protectionDomain, null, false).toJava();
    }

    /**
     * Creates a class from a classfile represented as a byte array with the specified source.
     *
     * @see java.lang.ClassLoader#defineClass1(String, byte[], int, int, ProtectionDomain)
     *
     * @param name the name of the class
     * @param bytes a representation of the classfile
     * @param offset offset into the array at which the classfile begins
     * @param length the length of the classfile
     * @param protectionDomain the protection domain in which to create the class
     * @param source
     * @return a new {@code Class} instance representing the class
     */
    @SUBSTITUTE(optional = true)
    private Class defineClass1(String name, byte[] bytes, int offset, int length, ProtectionDomain protectionDomain, String source) {
        final ClassActor classActor = ClassfileReader.defineClassActor(name, thisClassLoader(), bytes, offset, length, protectionDomain, source, false);
        return classActor.toJava();
    }

    /**
     * Creates a class from a classfile represented as a byte array with the specified source.
     *
     * @see java.lang.ClassLoader#defineClass1(String, byte[], int, int, ProtectionDomain)
     *
     * @param name the name of the class
     * @param bytes a representation of the classfile
     * @param offset offset into the array at which the classfile begins
     * @param length the length of the classfile
     * @param protectionDomain the protection domain in which to create the class
     * @param source
     * @param verify if true, then verification is not performed for the class
     * @return a new {@code Class} instance representing the class
     */
    @SUBSTITUTE(optional = true)
    private Class defineClass1(String name, byte[] bytes, int offset, int length, ProtectionDomain protectionDomain, String source, boolean verify) {
        final ClassActor classActor = ClassfileReader.defineClassActor(name, thisClassLoader(), bytes, offset, length, protectionDomain, source, false);
        if (!verify) {
            classActor.doNotVerify();
        }
        return classActor.toJava();
    }

    /**
     * Creates a class from a classfile represented as a {@code ByteBuffer} with the specified source.
     *
     * @see java.lang.ClassLoader#defineClass2(String, java.nio.ByteBuffer, int, int, ProtectionDomain)
     *
     * @param name the name of the class
     * @param byteBuffer the buffer containing the bytes of the classfile
     * @param offset offset into the array at which the classfile begins
     * @param length the length of the classfile
     * @param protectionDomain the protection domain in which to create the class
     * @param source
     * @return a new {@code Class} instance representing the class
     */
    @SUBSTITUTE(optional = true)
    private Class defineClass2(String name, java.nio.ByteBuffer byteBuffer, int offset, int length, ProtectionDomain protectionDomain, String source) {
        return defineClass1(name, byteBuffer.array(), offset, length, protectionDomain, source);
    }

    /**
     * Creates a class from a classfile represented as a {@code ByteBuffer} with the specified source.
     *
     * @see java.lang.ClassLoader#defineClass2(String, java.nio.ByteBuffer, int, int, ProtectionDomain)
     *
     * @param name the name of the class
     * @param byteBuffer the buffer containing the bytes of the classfile
     * @param offset offset into the array at which the classfile begins
     * @param length the length of the classfile
     * @param protectionDomain the protection domain in which to create the class
     * @param source
     * @param verify if true, then verification is not performed for the class
     * @return a new {@code Class} instance representing the class
     */
    @SUBSTITUTE(optional = true)
    private Class defineClass2(String name, java.nio.ByteBuffer byteBuffer, int offset, int length, ProtectionDomain protectionDomain, String source, boolean verify) {
        return defineClass1(name, byteBuffer.array(), offset, length, protectionDomain, source, verify);
    }

    /**
     * Resolves a class, initializing it if necessary.
     * @see java.lang.ClassLoader#resolveClass0(Class)
     * @param javaClass the class which to resolve and initialize
     */
    @SUBSTITUTE
    private void resolveClass0(Class javaClass) {
        Snippets.makeClassInitialized(ClassActor.fromJava(javaClass));
    }

    /**
     * Finds a class necessary for bootstrapping (typically a JDK or internal VM class).
     * @see java.lang.ClassLoader#findBootstrapClass(String)
     * @param name the name of the class to find
     * @return the class which is found
     * @throws ClassNotFoundException if the class was not found and the substituted method declares this checked exception
     */
    @SUBSTITUTE
    private Class findBootstrapClass(String name) throws ClassNotFoundException {
        Class<?> c = BootClassLoader.BOOT_CLASS_LOADER.findBootstrapClass(name);
        if (c == null) {
            // Earlier versions of ClassLoader.findBootstrapClass() throw CNFE instead of returning null
            TypeDescriptor[] checkedExceptions = ClassRegistry.ClassLoader_findBootstrapClass.checkedExceptions();
            if (checkedExceptions.length != 0) {
                assert checkedExceptions.length == 1;
                assert checkedExceptions[0] == JavaTypeDescriptor.CLASS_NOT_FOUND_EXCEPTION;
                throw new ClassNotFoundException(name);
            }
        }
        return c;
    }

    /**
     * Find a class that has already been loaded.
     * @param name the name of the class
     * @return a reference to the class, if it exists; null otherwise
     */
    @SUBSTITUTE
    private Class findLoadedClass0(String name) {
        TypeDescriptor descriptor;
        try {
            descriptor = JavaTypeDescriptor.parseMangledArrayOrUnmangledClassName(name);
        } catch (ClassFormatError e) {
            return null;
        }

        // This query is specific to this class loader and so parents should not be searched.
        // c.f. SystemDictionary::find(Symbol* class_name, Handle class_loader, Handle protection_domain, TRAPS) in systemDictionary.cpp
        final ClassActor classActor = ClassRegistry.get(thisClassLoader(), descriptor, false);
        if (classActor == null) {
            return null;
        }
        return classActor.toJava();
    }

    private static final String ASSERTION_STATUS_DIRECTIVES_CLASS_NAME = "java.lang.AssertionStatusDirectives";
    private static Class<?> AssertionStatusDirectivesClass;
    private static Object assertionStatusDirectives;

    static {
        try {
            AssertionStatusDirectivesClass = Class.forName(ASSERTION_STATUS_DIRECTIVES_CLASS_NAME);
        } catch (ClassNotFoundException ex) {
            throw ProgramError.unexpected("can't load " + ASSERTION_STATUS_DIRECTIVES_CLASS_NAME);
        }
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native AssertionStatusDirectivesAlias asASDA(Object object);

    private static class AssertionStatusDirectivesAlias {
        @ALIAS(declaringClassName = ASSERTION_STATUS_DIRECTIVES_CLASS_NAME, name = "<init>")
        private native void init();
        @ALIAS(declaringClassName = ASSERTION_STATUS_DIRECTIVES_CLASS_NAME)
        String[] packages;
        @ALIAS(declaringClassName = ASSERTION_STATUS_DIRECTIVES_CLASS_NAME)
        boolean[] packageEnabled;
        @ALIAS(declaringClassName = ASSERTION_STATUS_DIRECTIVES_CLASS_NAME)
        String[] classes;
        @ALIAS(declaringClassName = ASSERTION_STATUS_DIRECTIVES_CLASS_NAME)
        boolean[] classEnabled;
        @ALIAS(declaringClassName = ASSERTION_STATUS_DIRECTIVES_CLASS_NAME)
        boolean deflt;
    }



    /**
     * Retrieves the assertion status directives for this class loader.
     *
     * @return the assertion status directives object
     */
    @SUBSTITUTE
    private static Object retrieveDirectives() {
        if (assertionStatusDirectives == null) {
            assertionStatusDirectives = Heap.createTuple(ClassActor.fromJava(AssertionStatusDirectivesClass).dynamicHub());
            AssertionStatusDirectivesAlias thisAssertionStatusDirectives = asASDA(assertionStatusDirectives);
            thisAssertionStatusDirectives.classes = AssertionsVMOption.classes.toArray(new String[AssertionsVMOption.classes.size()]);
            thisAssertionStatusDirectives.packages = AssertionsVMOption.packages.toArray(new String[AssertionsVMOption.packages.size()]);
            thisAssertionStatusDirectives.classEnabled = new boolean[AssertionsVMOption.classEnabled.size()];
            thisAssertionStatusDirectives.classEnabled = toBooleanArray(AssertionsVMOption.classEnabled);
            thisAssertionStatusDirectives.packageEnabled = toBooleanArray(AssertionsVMOption.packageEnabled);
            thisAssertionStatusDirectives.deflt = AssertionsVMOption.deflt;

        }
        return assertionStatusDirectives;
    }

    private static boolean[] toBooleanArray(ArrayList<Boolean> list) {
        boolean[] result = new boolean[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
