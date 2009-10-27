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

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.collect.ChainedHashMapping.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.JavaTypeDescriptor.*;

/**
 * A string description of a Java runtime type, e.g. a field's type, see #4.3.2.
 *
 * All type descriptors are canonicalized at creation. This makes a type descriptor comparison equivalent to a pointer
 * equality test.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class TypeDescriptor extends Descriptor {

    /**
     * The only concrete subclass of {@link TypeDescriptor}.
     * Using a subclass hides the details of storing TypeDescriptors in a {@link ChainedHashMapping}.
     */
    static class TypeDescriptorEntry extends TypeDescriptor implements ChainedHashMapping.Entry<String, TypeDescriptorEntry> {
        TypeDescriptorEntry(String value) {
            super(value);
            ProgramError.check(value.length() > 0);
            assert !canonicalTypeDescriptors.containsKey(value);
            canonicalTypeDescriptors.put(value, this);
        }

        public String key() {
            return toString();
        }

        private Entry<String, TypeDescriptorEntry> next;

        public Entry<String, TypeDescriptorEntry> next() {
            return next;
        }

        public void setNext(Entry<String, TypeDescriptorEntry> next) {
            this.next = next;
        }

        public void setValue(TypeDescriptorEntry value) {
            assert value == this;
        }

        public TypeDescriptorEntry value() {
            return this;
        }
    }

    /**
     * Searching and adding entries to this map is only performed by
     * {@linkplain #makeTypeDescriptor(String) one method} which synchronizes on the map before using it.
     */
    private static final GrowableMapping<String, TypeDescriptorEntry> canonicalTypeDescriptors = new ChainingValueChainedHashMapping<String, TypeDescriptorEntry>();

    static {
        Classes.initialize(JavaTypeDescriptor.class);
        Classes.initialize(KindTypeDescriptor.class);
    }

    TypeDescriptor(String string) {
        super(string);
    }

    public static TypeDescriptor lookup(String string) {
        synchronized (canonicalTypeDescriptors) {
            return canonicalTypeDescriptors.get(string);
        }
    }

    static TypeDescriptor makeTypeDescriptor(String string) {
        synchronized (canonicalTypeDescriptors) {
            TypeDescriptorEntry typeDescriptorEntry = canonicalTypeDescriptors.get(string);
            if (typeDescriptorEntry == null) {
                // creating the type descriptor entry will add it to the canonical mapping.
                typeDescriptorEntry = new TypeDescriptorEntry(string);
            }
            return typeDescriptorEntry;
        }
    }

    public static int numberOfDescriptors() {
        return canonicalTypeDescriptors.length();
    }

    /**
     * Gets the {@linkplain ClassActor#elementClassActor() element type} denoted by this type descriptor.
     */
    public TypeDescriptor elementTypeDescriptor() {
        if (!JavaTypeDescriptor.isArray(this)) {
            return this;
        }
        return makeTypeDescriptor(toString().substring(JavaTypeDescriptor.getArrayDimensions(this)));
    }

    /**
     * Gets the {@linkplain ClassActor#componentClassActor() component type} denoted by this type descriptor.
     */
    public TypeDescriptor componentTypeDescriptor() {
        if (!JavaTypeDescriptor.isArray(this)) {
            return null;
        }
        return makeTypeDescriptor(toString().substring(1));
    }

    public Kind toKind() {
        assert !(this instanceof AtomicTypeDescriptor);
        if (KindTypeDescriptor.isWord(this)) {
            return Kind.WORD;
        }
        return Kind.REFERENCE;
    }

    private static String stringToJava(String string) {
        switch (string.charAt(0)) {
            case 'L':
                return dottified(string.substring(1, string.length() - 1));
            case '[':
                return stringToJava(string.substring(1)) + "[]";
            default:
                return KindTypeDescriptor.toKind(string).name.toString();
        }
    }

    /**
     * Gets a string representation of this descriptor that resembles a Java source language declaration.
     * For example:
     *
     * <pre>
     *     TypeDescriptor.create("Ljava/lang/String;").toJavaString() returns "java.lang.String"
     *     TypeDescriptor.create("Ljava/util/Map$Entry;").toJavaString() returns "java.util.Map$Entry"
     * </pre>
     *
     * @return a string representation of this descriptor that resembles a Java source language declaration
     */
    public String toJavaString() {
        return stringToJava(toString());
    }

    /**
     * Gets a string representation of this descriptor that resembles a Java source language declaration.
     * For example:
     *
     * <pre>
     *     TypeDescriptor.create("Ljava/lang/String;").toJavaString(true) returns "java.lang.String"
     *     TypeDescriptor.create("Ljava/lang/String;").toJavaString(false) returns "String"
     *     TypeDescriptor.create("Ljava/util/Map$Entry;").toJavaString(true) returns "java.util.Map$Entry"
     *     TypeDescriptor.create("Ljava/util/Map$Entry;").toJavaString(false) returns "Map$Entry"
     * </pre>
     *
     * @param qualified
     *                specifies if the returned value should be qualified
     *
     * @return a string representation of this descriptor that resembles a Java source language declaration
     */
    public String toJavaString(boolean qualified) {
        String string = toJavaString();
        if (qualified) {
            return string;
        }
        final int lastDot = string.lastIndexOf('.');
        if (lastDot != -1) {
            string = string.substring(lastDot + 1);
        }
        return string;
    }

    /**
     * Resolves this type descriptor to a class using a given class loader.
     *
     * @param classLoader the class loader used to resolve this type descriptor to a class
     * @return the resolved class
     */
    public Class resolveType(ClassLoader classLoader) {
        return JavaTypeDescriptor.resolveToJavaClass(this, classLoader);
    }

    /**
     * Determines if this constant can be resolved without causing class loading.
     *
     * @param holder the class that contains this type descriptor as a reference to another class
     */
    public boolean isResolvableWithoutClassLoading(final ClassActor holder, final ClassLoader classLoader) {
        TypeDescriptor typeDescriptor = this;
        if (MaxineVM.isHosted()) {
            // When running the compiler in a prototype environment (e.g. for JUnit testing), it's
            // desirable to minimize the startup time of the compiler. That is, we do not want to
            // eagerly load all the classes normally loaded in a JavaPrototype. However, these
            // classes must appear to be loaded when they are referenced by code being compiled.
            // As such, they are determined here to be resolvable without class loading which will
            // cause them to be loaded if necessary.
            while (JavaTypeDescriptor.isArray(typeDescriptor)) {
                typeDescriptor = typeDescriptor.componentTypeDescriptor();
            }
            if (ClassRegistry.vmClassRegistry().get(typeDescriptor) != null) {
                return true;
            }
            if (JavaTypeDescriptor.isPrimitive(typeDescriptor)) {
                return true;
            }

            final Class<?> javaClass;
            try {
                // Don't trigger class initialization
                javaClass = Classes.forName(typeDescriptor.toJavaString(), false, getClass().getClassLoader());
            } catch (NoClassDefFoundError e) {
                // couldn't find the class, obviously it cannot be resolvable
                return false;
            }
            if (javaClass.getPackage().getName().equals("java.lang")) {
                return true;
            }

            if (MaxineVM.isHostedOnly(javaClass)) {
                return false;
            }

            if (PrototypeClassLoader.isOmittedType(typeDescriptor)) {
                return false;
            }

            if (MaxineVM.target().configuration.isMaxineVMPackage(MaxPackage.fromClass(javaClass))) {
                return true;
            }

            return false;
        }
        return ClassRegistry.get(classLoader, typeDescriptor, true) != null;
    }

    public ClassActor resolve(final ClassLoader classLoader) {
        if (MaxineVM.isHosted()) {
            return resolveInPrototype(classLoader);
        }
        return ClassActor.fromJava(resolveType(classLoader));
    }

    @HOSTED_ONLY
    private ClassActor resolveInPrototype(final ClassLoader classLoader) {
        Classes.initialize(PrimitiveClassActor.class);
        return PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.mustMakeClassActor(this);
    }
}
