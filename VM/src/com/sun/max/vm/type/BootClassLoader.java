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
package com.sun.max.vm.type;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.*;

/**
 * The VM internal class loader that is commonly referred to as the bootstrap class
 * loader in JVM specification.
 *
 * @see http://java.sun.com/docs/books/jvms/second_edition/html/ConstantPool.doc.html#79383
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class BootClassLoader extends ClassLoader {

    /**
     * The singleton instance of this class.
     */
    public static final BootClassLoader BOOT_CLASS_LOADER = new BootClassLoader();

    private Classpath classpath;

    public Classpath classpath() {
        if (classpath == null) {
            classpath = Classpath.bootClassPath();
        }
        return classpath;
    }

    protected Class findClass(Classpath classpath, String name) throws ClassNotFoundException {
        final ClasspathFile classpathFile = classpath.readClassFile(name);
        if (classpathFile == null) {
            throw new ClassNotFoundException(name);
        }
        return ClassfileReader.defineClassActor(name, this, classpathFile.contents, null, classpathFile.classpathEntry, false).toJava();
    }

    public synchronized Class<?> findBootstrapClass(String name) throws ClassNotFoundException {
        final Class c = findLoadedClass(name);
        if (c != null) {
            return c;
        }
        return findClass(classpath(), name);
    }

    @Override
    public synchronized Class<?> findClass(String name) throws ClassNotFoundException {
        if (MaxineVM.isHosted()) {
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                ClasspathFile classfile = ClassfileReader.findGeneratedClassfile(name);
                if (classfile != null) {
                    final byte[] classfileBytes = classfile.contents;
                    return defineClass(name, classfileBytes, 0, classfileBytes.length);
                }
            }
        }
        return findClass(classpath(), name);
    }

    @ALIAS(declaringClass = ClassLoader.class, innerClass = "NativeLibrary", name = "<init>")
    private native void init(Class fromClass, String name);

    @ALIAS(declaringClass = ClassLoader.class, innerClass = "NativeLibrary")
    private long handle;

    @ALIAS(declaringClass = ClassLoader.class)
    private static Vector<Object> loadedLibraryNames;

    @ALIAS(declaringClass = ClassLoader.class)
    private Vector<Object> nativeLibraries;

    @INTRINSIC(UNSAFE_CAST)
    private static native BootClassLoader asThis(Object nativeLibrary);

    private Object createNativeLibrary(String path, Word handle) {
        final Object nativeLibrary = Heap.createTuple(JDK.java_lang_ClassLoader$NativeLibrary.classActor().dynamicHub());
        BootClassLoader thisNativeLibrary = asThis(nativeLibrary);
        thisNativeLibrary.init(BootClassLoader.class, path);
        thisNativeLibrary.handle = handle.asAddress().toLong();
        return nativeLibrary;
    }

    private void loadNativeLibrary(String libraryPath, String libraryName) {
        final String fileName = libraryPath + File.separator + System.mapLibraryName(libraryName);
        final Word handle = DynamicLinker.load(fileName);
        final Object nativeLibrary = createNativeLibrary(fileName, handle);

        loadedLibraryNames.addElement(fileName);
        nativeLibraries.add(nativeLibrary);
    }

    public void loadJavaAndZipNativeLibraries(String javaLibraryPath, String zipLibraryPath) {
        if (platform().operatingSystem == OperatingSystem.GUESTVM) {
            // no native libraries in GuestVM
            return;
        }

        loadNativeLibrary(javaLibraryPath, "java");
        loadNativeLibrary(zipLibraryPath, "zip");

        vmConfig().runScheme().runNativeInitializationMethods();
    }

    @HOSTED_ONLY
    private BootClassLoader() {
    }
}
