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
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.type.*;

/**
 * Parent hosted class loader for {@link HostedBootClassLoader} and {@link HostedVMClassLoader}.
 */
public abstract class HostedClassLoader extends ClassLoader {
    /**
     * The default classpath for loading classes.
     */
    protected Classpath classpath;

    protected final Set<String> loadedPackages = new HashSet<String>();

    protected HostedClassLoader() {
    }

    protected HostedClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Sets the classpath to be used for any subsequent loading of classes through the hosted boot class loader. This should
     * ideally only be called once per execution before any class loading is performed through
     * {@link #HOSTED_BOOT_CLASS_LOADER}.
     *
     * @param classpath
     *                the classpath to use
     */
    public void setClasspath(Classpath classpath) {
        ProgramWarning.check(this.classpath == null, "overriding hosted boot class loader's classpath: old value=\"" + this.classpath + "\", new value=\"" + classpath + "\"");
        this.classpath = classpath;
    }

    /**
     * Set the default classpath for the concrete subclass.
     * @param classpath
     */
    protected abstract Classpath getDefaultClassPath();

    /**
     * Gets the classpath of the hosted boot classloader.
     *
     * @return an object representing the classpath of this loader
     */
    public Classpath classpath() {
        if (classpath == null) {
            setClasspath(getDefaultClassPath());
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
     * Make a class actor for the specified type descriptor. This method will attempt to load
     * the class specified if it has not already been loaded and construct the class actor.
     *
     * @param typeDescriptor a well-formed descriptor of a class name.
     * @return the class actor for the specified type descriptor
     * @throws ClassNotFoundException if the class specified by the type descriptor could not be found
     */
    public ClassActor makeClassActor(final TypeDescriptor typeDescriptor) throws ClassNotFoundException {
        try {
            final ClassActor classActor = ClassRegistry.get(HostedClassLoader.this, typeDescriptor, false);
            if (classActor != null) {
                return classActor;
            }
            if (JavaTypeDescriptor.isArray(typeDescriptor)) {
                final ClassActor componentClassActor = makeClassActor(typeDescriptor.componentTypeDescriptor());
                return ClassActorFactory.createArrayClassActor(componentClassActor);
            }
            final String name = typeDescriptor.toJavaString();

            if (isOmittedType(name)) {
                throw new OmittedClassError(name);
            }

            synchronized (this) {
                loadedPackages.add(Classes.getPackageName(name));
            }

            final ClasspathFile classpathFile = readClassFile(classpath(), name);
            return ClassfileReader.defineClassActor(name, this, classpathFile.contents, null, classpathFile.classpathEntry, false);
        } catch (Exception exception) {
            throw Utils.cast(ClassNotFoundException.class, exception);
        }
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
            return makeClassActor(typeDescriptor);
        } catch (ClassNotFoundException throwable) {
            throw ProgramError.unexpected("could not make class Actor: " + typeDescriptor, throwable);
        }
    }

    /**
     * Create a class actor with the specified name from the specified byte array.
     *
     * @param name the name of the class
     * @param classfileBytes a byte array containing the encoded version of the class
     */
    public ClassActor makeClassActor(final String name, byte[] classfileBytes) {
        defineClass(name, classfileBytes, 0, classfileBytes.length);
        return ClassfileReader.defineClassActor(name, this, classfileBytes, null, null, false);
    }

    /**
     * Attempts to find an array class for the specified element type descriptor, loading the
     * element type class if necessary.
     *
     * @param elementTypeDescriptor the well-formed name of the element type
     * @return the class for array type specified
     * @throws ClassNotFoundException if the element type could not be found
     */
    private Class<?> findArrayClass(final TypeDescriptor elementTypeDescriptor) throws ClassNotFoundException {
        ClassActor elementClassActor = ClassRegistry.get(HostedClassLoader.this, elementTypeDescriptor, false);
        if (elementClassActor == null) {
            // findClass expects a Java class "Binary name".
            final Class elementType = findClass(elementTypeDescriptor.toJavaString());
            elementClassActor = ClassActor.fromJava(elementType);
        }
        final ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(elementClassActor);
        return arrayClassActor.toJava();
    }

    /**
     * Finds a class with the specified name given the specified class path.
     *
     * @param classpath the path which to search for the specified class
     * @param name the name of the class as a string
     * @return a class with the specified name
     * @throws ClassNotFoundException if the class could not be found
     */
    protected Class findClass(Classpath classpath, String name) throws ClassNotFoundException {
        final ClasspathFile classpathFile = readClassFile(classpath, name);
        return defineClass(name, classpathFile.contents, 0, classpathFile.contents.length);
    }

    /**
     * Overrides the default implementation of {@link ClassLoader#findClass(String) ClassLoader.findClass()},
     * using the internal actor machinery to find and build the classes as necessary.
     *
     * @param name the name of the class as a string
     */
    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            // FIXME: The class loader interface (as specified by the JDK) does not allow one to pass a name of an array class!
            // Specifically, the JDK says: "Class objects for array classes are not created by class loaders, but are created automatically
            // as required by the Java runtime. The class loader for an array class, as returned by Class.getClassLoader() is the same as
            // the class loader for its element type; if the element type is a primitive type, then the array class has no class loader."
            // So the following is not exactly legal.
            if (name.endsWith("[]")) {
                return findArrayClass(JavaTypeDescriptor.getDescriptorForJavaString(name).elementTypeDescriptor());
            } else if (name.charAt(0) == '[') {
                // make sure the name is slashified first
                final String elementTypeName = name.substring(1).replace('.', '/');
                return findArrayClass(JavaTypeDescriptor.parseTypeDescriptor(elementTypeName));
            }
            final Class<?> javaType = findClass(classpath(), name);
            makeClassActor(JavaTypeDescriptor.forJavaClass(javaType));
            return javaType;
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw ProgramError.unexpected(e);
        }
    }

    /**
     * Support for checking when loading a class if it is supposed to be omitted from the image.
     * @param name
     * @return
     */
    protected boolean isOmittedType(String name) {
        return false;
    }

    /**
     * Loads the class with the specified name.
     * @see ClassLoader#loadClass(String, resolve)
     */
    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        try {
            final Class<?> javaType = super.loadClass(name, resolve);
            if (MaxineVM.isHostedOnly(javaType)) {
                throw new HostOnlyClassError(javaType.getName());
            }
            if (isOmittedType(name)) {
                throw new OmittedClassError(javaType.getName());
            }
            makeClassActor(JavaTypeDescriptor.forJavaClass(javaType));

            if (Proxy.isProxyClass(javaType)) {
                JDK_java_lang_reflect_Proxy.bootProxyClasses.add(javaType);
            }
            return javaType;
        } catch (Exception exception) {
            throw Utils.cast(ClassNotFoundException.class, exception);
        }
    }

}
