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

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.TupleAccess;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.value.*;

/**
 * The initial class loader that after bootstrapping appears to have created the VM.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class VmClassLoader extends ClassLoader {

    /**
     * This exists (solely) for the purpose of being able to reify generated classes while prototyping. These are needed
     * so that the actors for generated stubs can be created. This field is omitted when generating the boot image.
     *
     * Optionally the class is written to the file system to allow the Inspector to access it when debugging the generated VM.
     *
     * As a temporary measure it is also used at target run time to allow the inspector to grab the class file from the target
     * without depending on all the I/O machinery working.
     */
    @PROTOTYPE_ONLY
    private Map<String, byte[]> generatedClassfiles = new HashMap<String, byte[]>();

    private synchronized void storeRuntimeGeneratedClassFile(String name, byte[] classfile) {
        if (MaxineVM.isPrototyping()) {
            if (generatedClassfiles == null) {
                generatedClassfiles = new HashMap<String, byte[]>();
            } else if (generatedClassfiles.containsKey(name)) {
                ProgramWarning.message("class with same name generated twice: " + name);
            }
            generatedClassfiles.put(name, classfile);
        }
    }

    public void saveGeneratedClassfile(String name, byte[] classfile) {
        storeRuntimeGeneratedClassFile(name, classfile);

        if (MaxineVM.isPrototyping()) {
            final String path = System.getProperty("maxine.vmclassloader.saveclassdir");
            if (path != null) {
                final File classfileFile = new File(path + File.separator + name.replace(".", File.separator) + ".class");
                BufferedOutputStream bs = null;
                try {
                    final File classfileDirectory = classfileFile.getParentFile();
                    if (!(classfileDirectory.exists() && classfileDirectory.isDirectory())) {
                        if (classfileDirectory.mkdirs()) {
                            throw new IOException("Could not make directory " + classfileDirectory);
                        }
                    }
                    bs = new BufferedOutputStream(new FileOutputStream(classfileFile));
                    bs.write(classfile);
                } catch (IOException ex) {
                    ProgramWarning.message("saveGeneratedClassfile: " + classfileFile + ": " + ex.getMessage());
                } finally {
                    if (bs != null) {
                        try {
                            bs.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        }
    }

    @PROTOTYPE_ONLY
    public ClasspathFile findGeneratedClassfile(String name) {
        final byte[] classfileBytes = generatedClassfiles.get(name);
        if (classfileBytes != null) {
            return new ClasspathFile(classfileBytes, null);
        }
        return null;
    }

    public Map<String, byte[]> generatedClassfiles() {
        return Collections.unmodifiableMap(generatedClassfiles);
    }

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
        return ClassfileReader.defineClassActor(name, this, classpathFile.contents, null, classpathFile.classpathEntry).toJava();
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
        if (MaxineVM.isPrototyping()) {
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                final byte[] classfileBytes = generatedClassfiles.get(name);
                if (classfileBytes != null) {
                    return defineClass(name, classfileBytes, 0, classfileBytes.length);
                }
            }
        }
        return findClass(classpath(), name);
    }

    private Object createNativeLibrary(String path, Word handle) {
        try {
            final ClassActor classActor = JDK.java_lang_ClassLoader$NativeLibrary.classActor();
            final VirtualMethodActor constructor = ClassMethodActor.findVirtual(classActor, SymbolTable.INIT.toString());
            final Object nativeLibrary = constructor.invokeConstructor(ReferenceValue.from(VmClassLoader.class), ReferenceValue.from(path)).asObject();
            final FieldActor longFieldActor = FieldActor.findInstance(classActor.toJava(), "handle");
            TupleAccess.writeLong(nativeLibrary, longFieldActor.offset(), handle.asAddress().toLong());
            return nativeLibrary;
        } catch (Throwable throwable) {
            ProgramError.unexpected(throwable);
        }
        throw ProgramError.unexpected();
    }

    private void loadNativeLibrary(String libraryPath, String libraryName) {
        final String fileName = libraryPath + File.separator + System.mapLibraryName(libraryName);
        final Word handle = DynamicLinker.load(fileName);
        final Object nativeLibrary = createNativeLibrary(fileName, handle);

        final Class<Vector<Object>> type = null;
        final FieldActor l = FieldActor.findStatic(ClassLoader.class, "loadedLibraryNames");
        final Vector<Object> loadedLibraryNames = StaticLoophole.cast(type, TupleAccess.readObject(StaticTuple.fromJava(ClassLoader.class), l.offset()));
        loadedLibraryNames.addElement(fileName);

        final FieldActor n = FieldActor.findInstance(ClassLoader.class, "nativeLibraries");
        final Vector<Object> nativeLibraries = StaticLoophole.cast(type, TupleAccess.readObject(this, n.offset()));
        nativeLibraries.addElement(nativeLibrary);
    }

    public void loadJavaAndZipNativeLibraries(String javaLibraryPath, String zipLibraryPath) {
        if (VMConfiguration.hostOrTarget().platform().operatingSystem == OperatingSystem.GUESTVM) {
            // no native libraries in GuestVM
            return;
        }

        loadNativeLibrary(javaLibraryPath, "java");
        loadNativeLibrary(zipLibraryPath, "zip");

        VMConfiguration.hostOrTarget().runScheme().runNativeInitializationMethods();
    }

    @PROTOTYPE_ONLY
    private VmClassLoader() {
    }

    public static final VmClassLoader VM_CLASS_LOADER = new VmClassLoader();

}
