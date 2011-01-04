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
        if (platform().os == OS.MAXVE) {
            // no native libraries in Maxine VE
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
