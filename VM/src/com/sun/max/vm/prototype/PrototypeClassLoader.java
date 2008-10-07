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
/*VCSID=092ab4a8-e803-48ed-bcd3-0e59540849c9*/
package com.sun.max.vm.prototype;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The class loader used during prototyping to make {@linkplain ClassActor actors} for the classes to be put in the boot image.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class PrototypeClassLoader extends ClassLoader {

    /**
     * The default classpath for loading prototype classes.
     */
    private static Classpath _classpath;

    private static final Set<String> _omittedClasses = new HashSet<String>();
    private static final Set<String> _omittedPackages = new HashSet<String>();

    /**
     * Adds a class that must not be loaded into the VM class registry. Calling {@link #loadClass(String, boolean)} for
     * this class will return null.
     *
     * @param javaClass
     */
    public static void omitClass(Class javaClass) {
        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.getDescriptorForTupleType(javaClass);
        ProgramError.check(!ClassRegistry.vmClassRegistry().contains(typeDescriptor), "Cannot omit a class already in VM class registry: " + javaClass);
        _omittedClasses.add(javaClass.getName());
    }

    /**
     * Adds the name of package that must not correspond to any class loaded into the VM class registry.
     * Calling {@link #loadClass(String, boolean)} for a class in the named package will return null.
     *
     * @param packageName
     */
    public static void omitPackage(String packageName) {
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            ProgramError.check(!classActor.packageName().equals(packageName), "Cannot omit a package that contains a class already in VM class registry: " + classActor.name());
        }
        _omittedPackages.add(packageName);
    }

    /**
     * Determines if a given type descriptor denotes a class that must not be loaded in VM class registry.
     * The set of omitted classes is determined by any preceding calls to {@link #omitClass(Class)} and {@link #omitPackage(String)}.
     *
     * @param typeDescriptor the descriptor of a type to test
     * @return {@code true} if {@code typeDescriptor} denotes a class that must not be loaded in VM class registry
     */
    public static boolean isOmittedType(TypeDescriptor typeDescriptor) {
        final String className = typeDescriptor.toJavaString();
        return _omittedClasses.contains(className) || _omittedPackages.contains(Classes.getPackageName(className));
    }

    /**
     * Sets the classpath to be used for any subsequent loading of classes through the prototype class loader. This should
     * ideally only be called once per execution before any class loading is performed through
     * {@link #PROTOTYPE_CLASS_LOADER}.
     *
     * @param classpath
     *                the classpath to use
     */
    public static void setClasspath(Classpath classpath) {
        ProgramWarning.check(_classpath == null, "overriding prototype class loader's classpath: old value=\"" + _classpath + "\", new value=\"" + classpath + "\"");
        _classpath = classpath;
    }

    /**
     * Gets the classpath of this prototype classloader.
     *
     * @return an object representing the classpath of this loader
     */
    public Classpath classpath() {
        if (_classpath == null) {
            setClasspath(Classpath.fromSystem());
        }
        return _classpath;
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
            classpathFile = VmClassLoader.VM_CLASS_LOADER.findGeneratedClassfile(name);
        }
        if (classpathFile != null) {
            return classpathFile;
        }
        throw new ClassNotFoundException(name);
    }

    private PrototypeClassLoader() {
    }

    public static final PrototypeClassLoader PROTOTYPE_CLASS_LOADER = new PrototypeClassLoader();

    /**
     * Make a class actor for the specified type descriptor. This method will attempt to load
     * the class specified if it has not already been loaded and construct the class actor.
     *
     * @param typeDescriptor a well-formed descriptor of a class name.
     * @return the class actor for the specified type descriptor
     * @throws ClassNotFoundException if the class specified by the type descriptor could not be found
     */
    public ClassActor makeClassActor(final TypeDescriptor typeDescriptor) throws ClassNotFoundException {
        if (isOmittedType(typeDescriptor)) {
            FatalError.unexpected("attempt to load a class that should not be in the boot image: " + typeDescriptor);
        }
        try {
            return MaxineVM.usingTargetWithException(new Function<ClassActor>() {
                public ClassActor call() throws Exception {
                    final ClassActor classActor = ClassRegistry.get(PrototypeClassLoader.this, typeDescriptor);
                    if (classActor != null) {
                        return classActor;
                    }
                    if (JavaTypeDescriptor.isArray(typeDescriptor)) {
                        final ClassActor componentClassActor = makeClassActor(typeDescriptor.componentTypeDescriptor());
                        return ClassActorFactory.createArrayClassActor(componentClassActor);
                    }
                    final String name = typeDescriptor.toJavaString();
                    final ClasspathFile classpathFile = readClassFile(classpath(), name);
                    return ClassfileReader.defineClassActor(name, PrototypeClassLoader.this, classpathFile._contents, null, classpathFile._classpathEntry);
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(ClassNotFoundException.class, exception);
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
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not make class Actor: " + typeDescriptor, throwable);
        }
    }

    /**
     * Create a class actor with the specified name from the specified byte array.
     *
     * @param name the name of the class
     * @param classfileBytes a byte array containing the encoded version of the class
     */
    public synchronized ClassActor makeClassActor(final String name, byte[] classfileBytes) {
        defineClass(name, classfileBytes, 0, classfileBytes.length);
        return ClassfileReader.defineClassActor(name, this, classfileBytes, null, null);
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
        ClassActor elementClassActor = ClassRegistry.get(PrototypeClassLoader.this, elementTypeDescriptor);
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
        return defineClass(name, classpathFile._contents, 0, classpathFile._contents.length);
    }

    /**
     * Overrides the default implementation of {@link ClassLoader#findClass(String) ClassLoader.findClass()},
     * using the internal actor machinery to find and build the classes as necessary.
     *
     * @param name the name of the class as a string
     */
    @Override
    public synchronized Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            return MaxineVM.usingTargetWithException(new Function<Class>() {
                public Class call() throws ClassNotFoundException {
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
                    if (!MaxineVM.isPrototypeOnly(javaType)) {
                        makeClassActor(JavaTypeDescriptor.forJavaClass(javaType));
                    } else {
                        Trace.line(1, "Ignoring prototype only type: " + javaType);
                    }
                    makeClassActor(JavaTypeDescriptor.forJavaClass(javaType));
                    return javaType;
                }
            });
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw ProgramError.unexpected(e);
        }
    }

    /**
     * Loads the class with the specified name.
     * @see ClassLoader#loadClass(String, resolve)
     */
    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        try {
            return MaxineVM.usingTargetWithException(new Function<Class>() {
                public Class call() throws ClassNotFoundException {
                    final Class<?> javaType = PrototypeClassLoader.super.loadClass(name, resolve);
                    if (MaxineVM.isPrototypeOnly(javaType)) {
                        Trace.line(1, "Ignoring prototype only type: " + javaType);
                        return null;
                    }
                    if (isOmittedType(JavaTypeDescriptor.getDescriptorForTupleType(javaType))) {
                        Trace.line(1, "Ignoring explicitly omitted type: " + javaType);
                        return null;
                    }
                    makeClassActor(JavaTypeDescriptor.forJavaClass(javaType));
                    return javaType;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(ClassNotFoundException.class, exception);
        }
    }

}
