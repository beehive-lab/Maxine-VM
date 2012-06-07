/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.ti.*;

/**
 * The VM internal class loader that is commonly referred to as the bootstrap class
 * loader in JVM specification.
 *
 * @see http://java.sun.com/docs/books/jvms/second_edition/html/ConstantPool.doc.html#79383
 *
 */
public final class BootClassLoader extends ClassLoader {

    /**
     * The singleton instance of this class.
     */
    public static final BootClassLoader BOOT_CLASS_LOADER = new BootClassLoader();

    private Classpath classpath;

    /**
     * Map from a package name (in "/" separated format) to the file system path from it was loaded.
     */
    private final HashMap<String, String> packages = new HashMap<String, String>();

    public Classpath classpath() {
        if (classpath == null) {
            String extraPath = null;
            if (!MaxineVM.isHosted()) {
                extraPath = VMTI.handler().bootclassPathExtension();
            }
            classpath = Classpath.bootClassPath(extraPath);
        }
        return classpath;
    }

    /**
     * Gets the names of all the packages defined by this loader. The names are in "/" separated form
     * including a trailing "/".
     */
    public String[] packageNames() {
        synchronized (packages) {
            return packages.keySet().toArray(new String[packages.size()]);
        }
    }

    /**
     * Gets the path of the class path entry from which a class in the named package was last loaded.
     */
    public String packageSource(String packageName) {
        synchronized (packages) {
            return packages.get(packageName);
        }
    }

    private Class resolveClassOrNull(Classpath classpath, String name) {
        final ClasspathFile classpathFile = classpath.readClassFile(name);
        if (classpathFile == null) {
            if (vmResolveOk.get()) {
                // must use class registry, to avoid recursion back here, as boot is parent of vm
                ClassActor ca = ClassRegistry.VM_CLASS_REGISTRY.get(JavaTypeDescriptor.getDescriptorForJavaString(name));
                if (ca != null) {
                    return ca.toJava();
                }
            }
            return null;
        }
        ClassActor classActor = ClassfileReader.defineClassActor(name, this, classpathFile.contents, null, classpathFile.classpathEntry, false);
        int cp = name.lastIndexOf('.');
        if (cp != -1) {
            String packageName = name.substring(0, cp + 1).replace('.', '/');
            synchronized (packages) {
                packages.put(packageName, classpathFile.classpathEntry.path());
            }
        }
        return classActor.toJava();
    }

    public synchronized Class<?> findBootstrapClass(String name) {
        final Class c = findLoadedClass(name);
        if (c != null) {
            return c;
        }
        return resolveClassOrNull(classpath(), name);
    }

    private static class VMResolveState extends ThreadLocal<Boolean> {
        @Override
        public Boolean initialValue() {
            return false;
        }
    }

    /**
     * When {@code true} {@link #resolveClassOrNull} will search {@link VMClassLoader} to handle special cases
     * where boot classes refer to VM classes, e.g. native method stubs.
     */
    private static final VMResolveState vmResolveOk = new VMResolveState();

    /**
     * Set the capability to resolve VM classes from a boot class.
     * @param state the new state
     * @return the previous state
     */
    public static boolean allowResolveVM(boolean state) {
        boolean v = vmResolveOk.get();
        vmResolveOk.set(state);
        return v;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
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
        Class c = resolveClassOrNull(classpath(), name);
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        return c;
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
    private static Method method(Class<?> declaringClass, String name, Class... parameterTypes) throws Exception {
        Method m = declaringClass.getDeclaredMethod(name, parameterTypes);
        m.setAccessible(true);
        return m;
    }

    @HOSTED_ONLY
    private BootClassLoader() {
        try {
            String[] names = (String[]) method(java.lang.Package.class, "getSystemPackages0").invoke(null);
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                String path = (String) method(java.lang.Package.class, "getSystemPackage0", String.class).invoke(null, name);
                packages.put(name, path);
            }
        } catch (Exception e) {
            ProgramWarning.message("Failed to prepopulate package name to package source map: " + e);
        }
    }
}
