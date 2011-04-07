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
package com.sun.max.vm.type;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.classfile.ErrorContext.*;

import java.io.*;
import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.TypeDescriptor.*;

/**
 * The {@code JavaTypeDescriptor} class collects together commonly used functionality associated
 * with Java type descriptors, as well as commonly used type descriptor instances.
 *
 * @author Ben L. Titzer
 */
public final class JavaTypeDescriptor {

    private JavaTypeDescriptor() {
    }

    /**
     * A type descriptor for void or a primitive type.
     *
     * @author Ben L. Titzer
     */
    public static final class AtomicTypeDescriptor extends TypeDescriptorEntry {
        public final Class javaClass;
        private Kind kind;

        @HOSTED_ONLY
        AtomicTypeDescriptor(String name, Class javaClass) {
            super(name);
            this.javaClass = javaClass;
        }

        @Override
        public boolean isResolvableWithoutClassLoading(ClassLoader classLoader) {
            return true;
        }

        @Override
        public Class resolveType(ClassLoader classLoader) {
            return javaClass;
        }

        @Override
        public String toJavaString() {
            if (isHosted() && kind == null) {
                return javaClass.getName();
            }
            return kind.name.toString();
        }

        @HOSTED_ONLY
        void setKind(Kind kind) {
            assert this.kind == null;
            this.kind = kind;
        }

        @Override
        public Kind toKind() {
            return kind;
        }
    }

    /**
     * A type descriptor for a {@linkplain Word#getSubclasses() class} in the {@link Word} hierarchy.
     *
     * @author Doug Simon
     */
    public static final class WordTypeDescriptor extends TypeDescriptorEntry {
        /**
         * The {@link Class} instance of a {@linkplain Boxed non-boxed} word type descriptor
         * or the {@link Class#getName() name} of the class implementing a boxed word type.
         * The latter only exist when in hosted mode.
         */
        public final Object javaClass;

        @HOSTED_ONLY
        WordTypeDescriptor(String name, Class javaClass) {
            super(name);
            this.javaClass = Boxed.class.isAssignableFrom(javaClass) ? javaClass.getName() : javaClass;
        }

        @Override
        public boolean isResolvableWithoutClassLoading(ClassLoader classLoader) {
            return javaClass instanceof Class;
        }

        @Override
        public Class resolveType(ClassLoader classLoader) {
            if (javaClass instanceof Class) {
                return (Class) javaClass;
            }
            throw new NoClassDefFoundError(javaClass.toString());
        }

        @Override
        public String toJavaString() {
            if (javaClass instanceof Class) {
                return ((Class) javaClass).getName();
            }
            return javaClass.toString();
        }

        @Override
        public Kind toKind() {
            return Kind.WORD;
        }
    }

    public static final AtomicTypeDescriptor VOID = new AtomicTypeDescriptor("V", void.class);
    public static final AtomicTypeDescriptor BYTE = new AtomicTypeDescriptor("B", byte.class);
    public static final AtomicTypeDescriptor BOOLEAN = new AtomicTypeDescriptor("Z", boolean.class);
    public static final AtomicTypeDescriptor SHORT = new AtomicTypeDescriptor("S", short.class);
    public static final AtomicTypeDescriptor CHAR = new AtomicTypeDescriptor("C", char.class);
    public static final AtomicTypeDescriptor INT = new AtomicTypeDescriptor("I", int.class);
    public static final AtomicTypeDescriptor FLOAT = new AtomicTypeDescriptor("F", float.class);
    public static final AtomicTypeDescriptor LONG = new AtomicTypeDescriptor("J", long.class);
    public static final AtomicTypeDescriptor DOUBLE = new AtomicTypeDescriptor("D", double.class);

    private static final AtomicTypeDescriptor[] ATOMIC_DESCRIPTORS = {VOID, BYTE, BOOLEAN, SHORT, CHAR, INT, FLOAT, LONG, DOUBLE};

    public static final TypeDescriptor WORD = getDescriptorForTupleType(Word.class);
    public static final TypeDescriptor REFERENCE = getDescriptorForTupleType(Reference.class);
    public static final TypeDescriptor OBJECT = getDescriptorForTupleType(Object.class);
    public static final TypeDescriptor CLASS = getDescriptorForTupleType(Class.class);
    public static final TypeDescriptor CLONEABLE = getDescriptorForTupleType(Cloneable.class);
    public static final TypeDescriptor SERIALIZABLE = getDescriptorForTupleType(Serializable.class);
    public static final TypeDescriptor THROWABLE = getDescriptorForTupleType(Throwable.class);
    public static final TypeDescriptor STRING = getDescriptorForTupleType(String.class);
    public static final TypeDescriptor ACCESSOR = getDescriptorForTupleType(Accessor.class);
    public static final TypeDescriptor DYNAMIC_HUB = getDescriptorForTupleType(DynamicHub.class);
    public static final TypeDescriptor JNI_HANDLE = getDescriptorForTupleType(JniHandle.class);
    public static final TypeDescriptor HYBRID = getDescriptorForTupleType(Hybrid.class);
    public static final TypeDescriptor INSTANTIATION_EXCEPTION = getDescriptorForTupleType(InstantiationException.class);
    public static final TypeDescriptor ILLEGAL_ARGUMENT_EXCEPTION = getDescriptorForTupleType(IllegalArgumentException.class);
    public static final TypeDescriptor INVOCATION_TARGET_EXCEPTION = getDescriptorForTupleType(InvocationTargetException.class);

    public static final TypeDescriptor THREAD = getDescriptorForTupleType(Thread.class);
    public static final TypeDescriptor CLASS_LOADER = getDescriptorForTupleType(ClassLoader.class);
    public static final TypeDescriptor METHOD = getDescriptorForTupleType(Method.class);
    public static final TypeDescriptor FIELD = getDescriptorForTupleType(Field.class);
    public static final TypeDescriptor CONSTRUCTOR = getDescriptorForTupleType(Constructor.class);

    /**
     * The {@code getTypeDescriptorForTupleType()} method returns a type descriptor for the specified class. The class
     * must not be either a primitive type or an array type.
     *
     * @param javaClass
     *                the Java class for which to get a type descriptor
     * @return a type descriptor for the specified class
     */
    public static TypeDescriptor getDescriptorForTupleType(Class javaClass) {
        assert !javaClass.isArray() && !javaClass.isPrimitive();
        return TypeDescriptor.makeTypeDescriptor(mangleClassName(javaClass.getName(), '/'));
    }

    /**
     * The {@code getDescriptorForWellFormedTupleName()} method returns a type descriptor for the specified class name,
     * which is not a primitive or an array. This method DOES NOT do checking of the well-formedness of the class name,
     * but assumes the package.Class format.
     *
     * @param javaName
     *                the name of the Java class
     * @return a descriptor for the Java class
     */
    public static TypeDescriptor getDescriptorForWellFormedTupleName(String javaName) {
        return TypeDescriptor.makeTypeDescriptor(mangleClassName(javaName, '/'));
    }

    /**
     * The {@code getArrayDescriptorForComponent()} method returns a type descriptor corresponding to an array of the
     * specified component class.
     *
     * @param componentClass
     *                the component class of the array
     * @return a type descriptor corresponding to an array
     */
    public static TypeDescriptor getArrayDescriptorForComponent(Class componentClass) {
        final TypeDescriptor descriptor = forJavaClass(componentClass);
        return getArrayDescriptorForDescriptor(descriptor, 1);
    }

    /**
     * The {@code getArrayDescriptorForDescriptor()} method gets the canonical type descriptor for the specified
     * component type descriptor with the specified number of dimensions. For example if the number of dimensions is 1,
     * then this method will return a descriptor for an array of the component type; if the number of dimensions is 2,
     * it will return a descriptor for an array of an array of the component type, etc.
     *
     * @param descriptor the type descriptor for the component type of the array
     * @param dimensions the number of array dimensions
     * @return the canonical type descriptor for the specified component type and dimensions
     */
    public static TypeDescriptor getArrayDescriptorForDescriptor(TypeDescriptor descriptor, int dimensions) {
        assert dimensions > 0;
        String componentString = descriptor.toString();
        if (getArrayDimensions(descriptor) + dimensions > 255) {
            throw classFormatError("Array type with more than 255 dimensions");
        }
        for (int i = 0; i != dimensions; ++i) {
            componentString = "[" + componentString;
        }
        return TypeDescriptor.makeTypeDescriptor(componentString);
    }

    /**
     * The {@code getArrayDimensions()} method returns the number of array dimensions for the specified type descriptor.
     * All non-array types have 0 array dimensions.
     *
     * @param descriptor the type descriptor for which to compute the number of dimensions
     * @return the number of dimensions in the specified type descriptor
     */
    public static int getArrayDimensions(TypeDescriptor descriptor) {
        final String s = descriptor.toString();
        int dimension = 0;
        while (s.charAt(dimension) == '[') {
            dimension++;
        }
        return dimension;
    }

    /**
     * The {@code resolveToJavaClass()} method resolves the given type descriptor to a Java class within the specified
     * class loader.
     *
     * @param descriptor the type descriptor to resolve
     * @param classLoader the classloader in which to resolve the type descriptor
     * @return a reference to the {@code java.lang.Class} instance representing the Java class if it exists
     * @throws NoClassDefFoundError if a class definition for this type descriptor could not be found in the specified
     *             class loader
     */
    public static Class resolveToJavaClass(TypeDescriptor descriptor, ClassLoader classLoader) {
        if (descriptor instanceof AtomicTypeDescriptor) {
            return ((AtomicTypeDescriptor) descriptor).javaClass;
        }

        final String string = descriptor.toString();
        int dimensions = getArrayDimensions(descriptor);
        if (dimensions > 0) {
            final TypeDescriptor elementDescriptor = TypeDescriptor.makeTypeDescriptor(string.substring(dimensions, string.length()));
            final Class elementClass = resolveToJavaClass(elementDescriptor, classLoader);
            if (MaxineVM.isHosted()) {
                // in bootstrapping mode, we must use java reflection.
                Class resultClass = elementClass;
                while (dimensions > 0) {
                    // Reflectively create an instance of the array here and get its class
                    // This seems to be the only reliable way of going from element type to array type at the Java level.
                    resultClass = Array.newInstance(resultClass, 0).getClass();
                    dimensions--;
                }
                return resultClass;
            }
            final ClassActor elementClassActor = ClassActor.fromJava(elementClass);
            return ArrayClassActor.forComponentClassActor(elementClassActor, dimensions).toJava();
        }
        // not an array. demangle the class name and use Classes.forName()
        final String javaClassName = demangleClassName(descriptor.toString());
        return Classes.forName(javaClassName, false, classLoader);
    }

    /**
     * The {@code resolveToJavaClasses()} resolves an array of type descriptors to an array of classes.
     *
     * @param typeDescriptors an array of the type descriptors to resolve
     * @param classLoader the classloader in which to resolve the type descriptors
     * @return a new array of classes corresponding to the resolved type descriptors
     */
    public static Class[] resolveToJavaClasses(TypeDescriptor[] typeDescriptors, ClassLoader classLoader) {
        final Class[] javaClasses = new Class[typeDescriptors.length];
        for (int i = 0; i != javaClasses.length; ++i) {
            javaClasses[i] = resolveToJavaClass(typeDescriptors[i], classLoader);
        }
        return javaClasses;
    }

    /**
     * The {@code demangleClassName()} method converts a string of the type descriptor form "Lpackage/ClassName;" to the
     * dottified "package.ClassName" form.
     *
     * @param className the class name in type descriptor form
     * @return a string representation of the class name in standard form
     */
    public static String demangleClassName(String className) {
        final int stringEnd = className.length() - 1;
        assert className.charAt(0) == 'L' && className.charAt(stringEnd) == ';';
        return className.substring(1, stringEnd).replace('/', '.');
    }

    /**
     * The {@code mangleClassName()} method converts a string of the standard form "package.ClassName" to the type
     * descriptor form "Lpackage/ClassName;".
     *
     * @param className the class name in type descriptor form
     * @param separator the new separator for packages, e.g. '/'
     * @return a string representation of the class name with the dots ('.') replaced by the separator and 'L' prepended
     *         and ';' appended
     */
    public static String mangleClassName(String className, char separator) {
        String newClassName = className;
        if (separator != '.') {
            newClassName = className.replace('.', separator);
        }
        return "L" + newClassName + ";";
    }

    /**
     * This method gets a type descriptor for any Java class object, including arrays and primitives.
     *
     * @param javaClass the Java class for which to retrieve a type descriptor
     * @return a reference to a canonical type descriptor for the specified Java class
     */
    public static TypeDescriptor forJavaClass(Class javaClass) {
        if (VOID == null || MaxineVM.isHosted()) {
            if (javaClass.isArray()) {
                return getArrayDescriptorForComponent(javaClass.getComponentType());
            }
            if (javaClass.isPrimitive()) {
                final AtomicTypeDescriptor atom = findAtomicTypeDescriptor(javaClass);
                assert atom != null;
                return atom;
            }
            return getDescriptorForTupleType(javaClass);
        }
        return ClassActor.fromJava(javaClass).typeDescriptor;
    }

    /**
     * Gets a type descriptor for a Java type represented as a string. The format for this name is that same as would be
     * written in java source code: i.e. primitives correspond to the keywords "byte", "char", etc, and arrays are
     * written as "Type[]".
     *
     * @param javaName the name of the java type as a string
     * @return a canonical type descriptor for the specified java string
     * @throws ClassFormatError if {@code javaName} does not denote a valid class name in the Java source language
     */
    public static TypeDescriptor getDescriptorForJavaString(String javaName) throws ClassFormatError {
        try {
            if (javaName.endsWith("[]")) {
                final String component = javaName.substring(0, javaName.length() - 2);
                return getArrayDescriptorForDescriptor(getDescriptorForJavaString(component), 1);
            }
            final AtomicTypeDescriptor atom = findAtomicTypeDescriptor(javaName);
            if (atom != null) {
                return atom;
            }
            return parseTypeDescriptor(mangleClassName(javaName, '/'));
        } catch (ClassFormatError e) {
            throw classFormatError("Invalid Java type name \"" + javaName + "\"");
        }
    }

    /**
     * Parses Java class names in one of two formats. If the first character is a '[', then the class name is assumed to
     * be a mangled array name; otherwise the name is considered to be a fully qualified Java class name separated by
     * dots.
     *
     * @param name the name to parse as a type descriptor
     * @return a type descriptor for the specified type
     * @throws ClassFormatError if the type descriptor is not valid, or if the type descriptor portion that is valid is
     *             not the entire string
     */
    public static TypeDescriptor parseMangledArrayOrUnmangledClassName(String name) throws ClassFormatError {
        if (name.length() > 0 && name.charAt(0) == '[') {
            // parse an array using the full parsing method
            return parseTypeDescriptor(name, 0, false);
        }
        // parse a class name separated by '.' without L or ;
        final int endIndex = parseClassName(name, 0, 0, '.');
        if (endIndex == name.length()) {
            return TypeDescriptor.makeTypeDescriptor(mangleClassName(name, '/'));
        }
        throw classFormatError("invalid class name \"" + name + "\"");
    }

    /**
     * Parses a Java type descriptor from a string according to the formatting
     * rules of the JVM specification.
     *
     * @param string
     *                the string from which to create a Java type descriptor
     * @return a type descriptor for the specified type
     * @throws ClassFormatError
     *                 if the type descriptor is not valid, or if the type descriptor portion that is valid is not the
     *                 entire string
     */
    public static TypeDescriptor parseTypeDescriptor(String string) throws ClassFormatError {
        final TypeDescriptor descriptor = parseTypeDescriptor(string, 0, true);
        if (descriptor.toString().length() != string.length()) {
            // if the valid class name did not occupy the entire string
            throw classFormatError("invalid type descriptor \"" + string + "\"");
        }
        return descriptor;
    }

    /**
     * Parses a Java type descriptor from a string according to the formatting rules of the Java language.
     *
     * @param string the string from which to create a Java type descriptor
     * @param startIndex the index within the string from which to start parsing
     * @param slashes TODO
     * @return a type descriptor for the specified type
     * @throws ClassFormatError if the type descriptor is not valid
     */
    public static TypeDescriptor parseTypeDescriptor(String string, int startIndex, boolean slashes) throws ClassFormatError {
        if (startIndex >= string.length()) {
            throw classFormatError("invalid type descriptor");
        }
        switch (string.charAt(startIndex)) {
            case 'Z':
                return BOOLEAN;
            case 'B':
                return BYTE;
            case 'C':
                return CHAR;
            case 'D':
                return DOUBLE;
            case 'F':
                return FLOAT;
            case 'I':
                return INT;
            case 'J':
                return LONG;
            case 'S':
                return SHORT;
            case 'V':
                return VOID;
            case 'L': {
                if (slashes) {
                    // parse a slashified Java class name
                    final int endIndex = parseClassName(string, startIndex, startIndex + 1, '/');
                    if (endIndex > startIndex + 1 && endIndex < string.length() && string.charAt(endIndex) == ';') {
                        return TypeDescriptor.makeTypeDescriptor(string.substring(startIndex, endIndex + 1));
                    }
                } else {
                    // parse a dottified Java class name and convert to slashes
                    final int endIndex = parseClassName(string, startIndex, startIndex + 1, '.');
                    if (endIndex > startIndex + 1 && endIndex < string.length() && string.charAt(endIndex) == ';') {
                        final String substring = string.substring(startIndex, endIndex + 1);
                        return TypeDescriptor.makeTypeDescriptor(substring.replace('.', '/'));
                    }
                }
                throw typeDescriptorError("invalid Java name", string, startIndex);
            }
            case '[': {
                // compute the number of dimensions
                int index = startIndex;
                while (index < string.length() && string.charAt(index) == '[') {
                    index++;
                }
                final int dimensions = index - startIndex;
                if (dimensions > 255) {
                    throw typeDescriptorError("array with more than 255 dimensions", string, startIndex);
                }
                final TypeDescriptor component = parseTypeDescriptor(string, index, slashes);
                return getArrayDescriptorForDescriptor(component, dimensions);
            }
        }
        throw typeDescriptorError("invalid type descriptor", string, startIndex);
    }

    private static int parseClassName(String string, int startIndex, int index, final char separator) throws ClassFormatError {
        int position = index;
        final int length = string.length();
        while (position < length) {
            final char nextch = string.charAt(position);
            if (nextch == '.' || nextch == '/') {
                if (separator != nextch) {
                    return position;
                }
            } else if (nextch == ';' || nextch == '[') {
                return position;
            }
            position++;
        }
        return position;
    }

    static ClassFormatError typeDescriptorError(String message, String string, int index) {
        return classFormatError(message + " \" " + string.substring(0, index) + "\" -> \"" + string.substring(index) + "\"");
    }

    /**
     * This method checks whether the specified class is a subclass of the specified type descriptor. This check is
     * necessary in some cases where the type of the superclass is not yet resolved.
     *
     * @param target the type descriptor of the target superclass
     * @param sourceClass the class actor of the (potential) subclass
     * @return true if the specified class actor is a subclass of the specified type descriptor; false otherwise
     */
    public static boolean isSubclass(TypeDescriptor target, ClassActor sourceClass) {
        ClassActor superClassActor = sourceClass;
        while (superClassActor != null) {
            if (superClassActor.typeDescriptor == target) {
                return true;
            }
            superClassActor = superClassActor.superClassActor;
        }
        return false;
    }

    /**
     * Determines if a given source type descriptor is assignable to the type described by the {@code target}
     * descriptor. This test only applies if both this type and {@code source} describe non-interface types.
     *
     * @param target the type descriptor target
     * @param source the type to test
     * @param sourceSuperClass the super class actor of {@code from}
     */
    public static boolean isAssignableFrom(TypeDescriptor target, TypeDescriptor source, ClassActor sourceSuperClass) {
        return source == target || isSubclass(target, sourceSuperClass);
    }

    public static boolean isArray(TypeDescriptor descriptor) {
        return descriptor.toString().charAt(0) == '[';
    }

    public static boolean isPrimitive(TypeDescriptor descriptor) {
        return descriptor instanceof AtomicTypeDescriptor;
    }

    private static AtomicTypeDescriptor findAtomicTypeDescriptor(String name) {
        for (AtomicTypeDescriptor atom : ATOMIC_DESCRIPTORS) {
            if (name.equals(atom.javaClass.getName())) {
                return atom;
            }
        }
        return null;
    }

    private static AtomicTypeDescriptor findAtomicTypeDescriptor(Class javaClass) {
        for (AtomicTypeDescriptor atom : ATOMIC_DESCRIPTORS) {
            if (atom.javaClass == javaClass) {
                return atom;
            }
        }
        return null;
    }
}
