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
package com.sun.max.vm.jdk;

import static com.sun.c1x.bytecode.Bytecodes.*;

import java.security.*;

import com.sun.c1x.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Implements substitutions necessary for {@link ClassLoader}.
 *
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
    @SUBSTITUTE(conditional = true)
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
    @SUBSTITUTE(conditional = true)
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
    @SUBSTITUTE(conditional = true)
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
    @SUBSTITUTE(conditional = true)
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
        MakeClassInitialized.makeClassInitialized(ClassActor.fromJava(javaClass));
    }

    /**
     * Finds a class necessary for bootstrapping (typically a JDK or internal VM class).
     * @see java.lang.ClassLoader#findBootstrapClass(String)
     * @param name the name of the class to find
     * @return the class which is found
     * @throws ClassNotFoundException if the class was not found
     */
    @SUBSTITUTE
    private Class findBootstrapClass(String name) throws ClassNotFoundException {
        return BootClassLoader.BOOT_CLASS_LOADER.findBootstrapClass(name);
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
        final ClassActor classActor =  ClassRegistry.get(thisClassLoader(), descriptor, false);
        if (classActor == null) {
            return null;
        }
        return classActor.toJava();
    }

    /**
     * java.lang.AssertionStatusDirectives is package private.
     * This is a parallel class with exactly the same fields:
     */
    private static final class Fake_AssertionStatusDirectives {
        String[] classes;
        boolean[] classEnabled;
        String[] packages;
        boolean[] packageEnabled;
        boolean deflt;
    }

    /**
     * Retrieves the assertion status directives for this class loader.
     * @return the assertion status directives object
     */
    @SUBSTITUTE
    private static Object retrieveDirectives() {
        final Fake_AssertionStatusDirectives assertionStatusDirectives = new Fake_AssertionStatusDirectives();
        //TODO: obtain proper values for these from command line arguments:
        assertionStatusDirectives.classes = new String[0];
        assertionStatusDirectives.classEnabled = new boolean[0];
        assertionStatusDirectives.packages = new String[0];
        assertionStatusDirectives.packageEnabled = new boolean[0];
        assertionStatusDirectives.deflt = false;

        // Replace the result object's class reference with the type the JDK expects:
        final ClassActor classActor = ClassRegistry.BOOT_CLASS_REGISTRY.get(JavaTypeDescriptor.getDescriptorForJavaString("java.lang.AssertionStatusDirectives"));
        assert classActor != null;
        Layout.writeHubReference(Reference.fromJava(assertionStatusDirectives), Reference.fromJava(classActor));
        return assertionStatusDirectives;
    }
}
