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
package com.sun.max.vm.hosted;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.type.*;

/**
 * Parent hosted class loader for {@link HostedBootClassLoader} and {@link HostedVMClassLoader}.
 */
public abstract class HostedClassLoader extends ClassLoader {
    /**
     * The default classpath for loading classes.
     */
    protected Classpath classpath;

    /**
     * A cache of loaded classes for fast lookup. This is equivalent to the map {@link ClassRegistry}
     * but is in terms of {@link Class}, which is what {@link ClassLoader#loadClass} uses.
     */
    protected Map<String, Class> definedClasses = new HashMap<String, Class>();

    protected HostedClassLoader() {
        super(null);
    }

    protected HostedClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Sets the classpath to be used for any subsequent loading of classes through this loader. This should
     * ideally only be called once per execution before any class loading is performed through this loader.
     *
     * @param classpath the classpath to use for this loader.
     */
    public void setClasspath(Classpath classpath) {
        ProgramWarning.check(this.classpath == null, "overriding hosted boot class loader's classpath: old value=\"" + this.classpath + "\", new value=\"" + classpath + "\"");
        this.classpath = classpath;
    }

    /**
     * Set the default classpath for the concrete subclass.
     * @param classpath
     */
    protected abstract Classpath getDefaultClasspath();

    /**
     * Gets the classpath of the hosted boot classloader.
     *
     * @return an object representing the classpath of this loader
     */
    public Classpath classpath() {
        if (classpath == null) {
            setClasspath(getDefaultClasspath());
        }
        return classpath;
    }

    /**
     * Gets the contents of the class file corresponding to a given class, searching a given classpath.
     *
     * @param classpath the classpath to search
     * @param name the name of the class to open
     * @return the contents of the class file representation of the class named {@code name}
     * @throws ClassNotFoundException if the class file cannot be found
     */
    public static ClasspathFile readClassFile(Classpath classpath, String name) throws ClassNotFoundException {
        ClasspathFile classpathFile = classpath.readClassFile(name);
        if (classpathFile == null) {
            classpathFile = ClassfileReader.findGeneratedClassfile(name);
        }
        if (classpathFile != null) {
            return classpathFile;
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Make a class actor for the specified type descriptor and fail with a program error
     * if it cannot be done.
     *
     * @param typeDescriptor a well-formed descriptor of a class name
     * @return the class actor for the specified type descriptor
     */
    public ClassActor mustMakeClassActor(TypeDescriptor typeDescriptor) {
        try {
            if (!JavaTypeDescriptor.isPrimitive(typeDescriptor)) {
                // this gets it into the correct registry
                String javaName = typeDescriptor.toJavaString();
                loadClass(javaName);
            }
            return ClassRegistry.getInBootOrVM(typeDescriptor);
        } catch (ClassNotFoundException throwable) {
            throw ProgramError.unexpected("could not make class Actor: " + typeDescriptor, throwable);
        }
    }

    /**
     * Define the class actor for a class successfully loaded by {@link ClassLoader#loadClass}.
     * @param javaClass the {@code Class} that was loaded.
     * @return the associated {@link ClassAtor}
     * @throws ClassNotFoundException
     */
    private ClassActor defineLoadedClassActor(Class javaClass) throws ClassNotFoundException {
        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.forJavaClass(javaClass);
        final ClassActor classActor = ClassRegistry.get(this, typeDescriptor, false);
        // This check catches stub and array classes that are already defined in their unique way.
        // It is easier catch them here this way than in {@link #loadClass}.
        if (classActor != null) {
            return classActor;
        }
        final String name = typeDescriptor.toJavaString();
        final ClasspathFile classpathFile = readClassFile(classpath(), name);
        definedClasses.put(name, javaClass);
        return ClassfileReader.defineClassActor(name, this, classpathFile.contents, null, classpathFile.classpathEntry, false);
    }

    /**
     * Create a class actor with the specified name from the specified byte array.
     * Inspector use only.
     *
     * @param name the name of the class
     * @param classfileBytes a byte array containing the encoded version of the class
     */
    public ClassActor makeClassActor(final String name, byte[] classfileBytes) {
        defineClass(name, classfileBytes, 0, classfileBytes.length);
        return ClassfileReader.defineClassActor(name, this, classfileBytes, null, null, false);
    }

    /**
     * Array/Invocation stub classes require special treatment and are handled here for all subclasses.
     * Since the classloader for the array/stub depends on the component/target type, our subclass will actually
     * handle the loading of that.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.endsWith("[]")) {
            return findArrayClass(name, JavaTypeDescriptor.getDescriptorForJavaString(name).componentTypeDescriptor());
        } else if (name.charAt(0) == '[') {
            // make sure the name is slashified first
            final String componentTypeName = name.substring(1).replace('.', '/');
            return findArrayClass(name, JavaTypeDescriptor.parseTypeDescriptor(componentTypeName));
        } else if (isStubClass(name)) {
            return defineStubClass(name);
        } else {
            return super.findClass(name);
        }
    }

    protected final boolean isStubClass(String name) {
        return name.startsWith(InvocationStubGenerator.STUB_PACKAGE_PREFIX);
    }

    /**
     * Define a stub class in this loader, if it was placed into our registry by {@link InvocationStubGenerator}.
     * @param name
     * @return
     */
    protected Class<?> defineStubClass(String name) throws ClassNotFoundException {
        TypeDescriptor typeDescriptor = JavaTypeDescriptor.getDescriptorForJavaString(name);
        if (ClassRegistry.get(this, typeDescriptor, false) != null) {
            ClasspathFile classpathFile = ClassfileReader.findGeneratedClassfile(name);
            Class<?> stubClass = defineClass(name, classpathFile.contents, 0, classpathFile.contents.length);
            definedClasses.put(name, stubClass);
            return stubClass;
        } else {
            throw new ClassNotFoundException();
        }
    }

    /**
     * Attempts to find an array class for the specified element type descriptor, loading the
     * element type class if necessary.
     *
     * @param elementTypeDescriptor the well-formed name of the element type
     * @return the class for array type specified
     * @throws ClassNotFoundException if the element type could not be found
     */
    private Class<?> findArrayClass(final String name, final TypeDescriptor elementTypeDescriptor) throws ClassNotFoundException {
        ClassActor elementClassActor = ClassRegistry.get(this, elementTypeDescriptor, false);
        if (elementClassActor == null) {
            final Class elementType = loadClass(elementTypeDescriptor.toJavaString());
            elementClassActor = ClassActor.fromJava(elementType);
        }
        // Special case: Owing to HostBootClassLoader being able to access VM classes
        // it is possible that we arrive here with elementClassActor being a VM class.
        // We have to abort, otherwise the array will incorrectly end up in the boot class registry.
        // HostVMClassLoader will define the array after HostBootClassLoader fails.
        if (this == HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER && elementClassActor.classLoader == HostedVMClassLoader.HOSTED_VM_CLASS_LOADER) {
            throw new ClassNotFoundException();
        }
        final ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(elementClassActor);
        Class<?> arrayClass = arrayClassActor.toJava();
        definedClasses.put(name, arrayClass);
        return arrayClass;
    }


    /**
     * Loads the class with the specified name. Also creates the {@link ClassActor} and records proxy classes, unless
     * prevent by subclass checks.
     *
     * @see ClassLoader#loadClass(String, resolve)
     */
    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        Class javaType = definedClasses.get(name);
        if (javaType != null) {
            return javaType;
        }
        try {
            javaType = super.loadClass(name, resolve);
            if (extraLoadClassChecks(javaType)) {
                defineLoadedClassActor(javaType);

                if (Proxy.isProxyClass(javaType)) {
                    JDK_java_lang_reflect_Proxy.bootProxyClasses.add(javaType);
                }
            }
            return javaType;
        } catch (Exception exception) {
            throw Utils.cast(ClassNotFoundException.class, exception);
        }
    }

    /**
     * Hook for a subclass to add additional checks before/after loading.
     * @param javaType {@code null} if the class is yet to be loaded, otherwise result of {@link #loadClass}
     * @return {@code true} iff the class should be added to the associated {@link ClassRegistry}
     * @throws ClassNotFoundException if the subclass wants to reject the class
     */
    protected abstract boolean extraLoadClassChecks(Class<?> javaType) throws ClassNotFoundException;

}
