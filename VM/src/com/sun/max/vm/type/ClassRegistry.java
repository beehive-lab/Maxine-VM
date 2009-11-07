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

import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.prototype.HostedBootClassLoader.*;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.value.*;

/**
 * Each class loader is associated with a class registry and vice versa.
 * The class registry augments the class loader with a mapping from
 * {@linkplain TypeDescriptor type descriptors} to {@linkplain ClassActor class actors}.
 *
 * The {@linkplain BootClassLoader#BOOT_CLASS_LOADER boot class loader} is associated the
 * {@linkplain #BOOT_CLASS_REGISTRY boot class registry}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class ClassRegistry implements IterableWithLength<ClassActor> {

    private static final EnumMap<Property, VariableMapping<Object, Object>> propertyMapsPrototype = new EnumMap<Property, VariableMapping<Object, Object>>(Property.class);

    /**
     * This is only here to support ClassFileWriter.testLoadGeneratedClasses().
     */
    @HOSTED_ONLY
    public static ClassLoader testClassLoader;
    private static ClassRegistry testClassRegistry;

    private final Map<Property, VariableMapping<Object, Object>> propertyMaps;

    @INSPECTED
    private final GrowableMapping<TypeDescriptor, ClassActor> typeDescriptorToClassActor = new ChainedHashMapping<TypeDescriptor, ClassActor>(5000);

    public final ClassLoader classLoader;

    private ClassRegistry(ClassLoader classLoader) {
        propertyMaps = propertyMapsPrototype.clone();
        for (Property property : Property.VALUES) {
            propertyMaps.put(property, property.createMap());
        }
        this.classLoader = classLoader;
    }

    /**
     * The class registry associated with the boot class loader.
     */
    public static final ClassRegistry BOOT_CLASS_REGISTRY = new ClassRegistry(HOSTED_BOOT_CLASS_LOADER);

    public static final TupleClassActor OBJECT = createClassActor(Object.class);
    public static final TupleClassActor CLASS = createClassActor(Class.class);
    public static final TupleClassActor THROWABLE = createClassActor(Throwable.class);
    public static final InterfaceActor CLONEABLE = createClassActor(Cloneable.class);
    public static final InterfaceActor SERIALIZABLE = createClassActor(Serializable.class);

    public static final PrimitiveClassActor<VoidValue> VOID = ClassRegistry.createPrimitiveClassActor(Kind.VOID);

    public static final PrimitiveClassActor<ByteValue> BYTE = ClassRegistry.createPrimitiveClassActor(Kind.BYTE);
    public static final PrimitiveClassActor<BooleanValue> BOOLEAN = ClassRegistry.createPrimitiveClassActor(Kind.BOOLEAN);
    public static final PrimitiveClassActor<ShortValue> SHORT = ClassRegistry.createPrimitiveClassActor(Kind.SHORT);
    public static final PrimitiveClassActor<CharValue> CHAR = ClassRegistry.createPrimitiveClassActor(Kind.CHAR);
    public static final PrimitiveClassActor<IntValue> INT = ClassRegistry.createPrimitiveClassActor(Kind.INT);
    public static final PrimitiveClassActor<FloatValue> FLOAT = ClassRegistry.createPrimitiveClassActor(Kind.FLOAT);
    public static final PrimitiveClassActor<LongValue> LONG = ClassRegistry.createPrimitiveClassActor(Kind.LONG);
    public static final PrimitiveClassActor<DoubleValue> DOUBLE = ClassRegistry.createPrimitiveClassActor(Kind.DOUBLE);

    public static final ArrayClassActor<ByteValue> BYTE_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(BYTE);
    public static final ArrayClassActor<BooleanValue> BOOLEAN_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(BOOLEAN);
    public static final ArrayClassActor<ShortValue> SHORT_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(SHORT);
    public static final ArrayClassActor<CharValue> CHAR_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(CHAR);
    public static final ArrayClassActor<IntValue> INT_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(INT);
    public static final ArrayClassActor<FloatValue> FLOAT_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(FLOAT);
    public static final ArrayClassActor<LongValue> LONG_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(LONG);
    public static final ArrayClassActor<DoubleValue> DOUBLE_ARRAY = ClassRegistry.createPrimitiveArrayClassActor(DOUBLE);

    /**
     * Creates a ClassActor for a primitive type.
     */
    @HOSTED_ONLY
    private static <Value_Type extends Value<Value_Type>> PrimitiveClassActor<Value_Type> createPrimitiveClassActor(Kind<Value_Type> kind) {
        return put(new PrimitiveClassActor<Value_Type>(kind));
    }

    /**
     * Creates an ArrayClassActor for a primitive array type.
     */
    @HOSTED_ONLY
    private static <Value_Type extends Value<Value_Type>> ArrayClassActor<Value_Type> createPrimitiveArrayClassActor(PrimitiveClassActor<Value_Type> componentClassActor) {
        return put(new ArrayClassActor<Value_Type>(componentClassActor));
    }

    /**
     * Creates a ClassActor for a tuple or interface type.
     */
    @HOSTED_ONLY
    private static <ClassActor_Type extends ClassActor> ClassActor_Type createClassActor(Class javaClass) {
        TypeDescriptor typeDescriptor = JavaTypeDescriptor.forJavaClass(javaClass);
        ClassActor classActor = BOOT_CLASS_REGISTRY.get(typeDescriptor);
        if (classActor == null) {
            final String name = typeDescriptor.toJavaString();
            Classpath classpath = HOSTED_BOOT_CLASS_LOADER.classpath();
            final ClasspathFile classpathFile = classpath.readClassFile(name);
            classActor = ClassfileReader.defineClassActor(name, HOSTED_BOOT_CLASS_LOADER, classpathFile.contents, null, classpathFile.classpathEntry, false);
        }
        Class<ClassActor_Type> type = null;
        return StaticLoophole.cast(type, classActor);
    }

    /**
     * Gets the registry for a given class loader, creating it first if necessary.
     *
     * @param classLoader the class loader for which the associated registry is requested
     * @return the class registry associated with {@code classLoader}
     */
    public static ClassRegistry makeRegistry(ClassLoader classLoader) {
        if (MaxineVM.isHosted()) {
            if (classLoader == testClassLoader) {
                if (testClassRegistry == null) {
                    testClassRegistry = new ClassRegistry(classLoader);
                }
                return testClassRegistry;
            }
            return BOOT_CLASS_REGISTRY;
        }
        if (classLoader == null) {
            return BOOT_CLASS_REGISTRY;
        }

        final ClassRegistry result = (ClassRegistry) TupleAccess.readObject(classLoader, ClassLoader_classRegistry.offset());
        if (result != null) {
            return result;
        }

        // Non-blocking synchronization is used here to swap in a new ClassRegistry reference.
        // This could lead to some extra ClassRegistry objects being created that become garbage, but should be harmless.
        final ClassRegistry newRegistry = new ClassRegistry(classLoader);
        final Reference oldRegistry = Reference.fromJava(classLoader).compareAndSwapReference(ClassLoader_classRegistry.offset(), null,  Reference.fromJava(newRegistry));
        if (oldRegistry == null) {
            return newRegistry;
        }
        return UnsafeCast.asClassRegistry(oldRegistry.toJava());
    }

    public int numberOfClassActors() {
        return typeDescriptorToClassActor.length();
    }

    /**
     * This iterator is not thread-safe. It must not be used while any other thread could possibly
     * {@linkplain #put(ClassActor) add} another class to the registry.
     */
    public Iterator<ClassActor> iterator() {
        return typeDescriptorToClassActor.values().iterator();
    }

    public int length() {
        return numberOfClassActors();
    }

    /**
     * Adds a class to this registry.
     *
     * The caller must only add classes that are known to not already be in this registry.
     * The best way to guarantee this is have this call be in a block that synchronizes on
     * {@link #classLoader} which first {@linkplain #get(TypeDescriptor) tests} whether
     * {@code classActor} is already in this registry.
     *
     * @param classActor the class actor to add. This class actor must not already be in this registry.
     */
    private void put0(ClassActor classActor) {
        final TypeDescriptor typeDescriptor = classActor.typeDescriptor;
        final ClassActor existingClassActor = typeDescriptorToClassActor.put(typeDescriptor, classActor);

        if (existingClassActor != null) {
            throw new NoClassDefFoundError("Cannot redefine " + classActor.name);
        }
    }

    /**
     * Adds a class to the registry associated with its class loader.
     *
     * The caller must only add classes that are known to not already be in the registry.
     * The best way to guarantee this is have this call be in a block that synchronizes on
     * {@link #classLoader} which first {@linkplain #get(TypeDescriptor) tests} whether
     * {@code classActor} is already in the registry.
     *
     * Only completely initialized ClassActors must be registered with the ClassRegistry.
     * This prevents another thread from getting hold of a partially initialized ClassActor.
     * As such, registration must not be performed from within the ClassActor constructor
     * or any ClassActor subclass constructor. Instead, it should be done at the call site
     * of the 'new' that is allocating a ClassActor instance, typically by enclosing the 'new'
     * expression with a call to this method.
     *
     * @param classActor the class actor to add. This class actor must not already be in the registry.
     */
    public static <ClassActor_Type extends ClassActor> ClassActor_Type put(ClassActor_Type classActor) {
        makeRegistry(classActor.classLoader).put0(classActor);
        return classActor;
    }

    /**
     * Looks up a class actor in this registry.
     *
     * @param typeDescriptor the type descriptor of the class actor to look up
     * @return the class actor corresponding to {@code typeDescriptor} in this registry or {@code null} if there is no
     *         entry in this registry corresponding to {@code typeDescriptor}
     */
    public ClassActor get(TypeDescriptor typeDescriptor) {
        return typeDescriptorToClassActor.get(typeDescriptor);
    }

    /**
     * Searches for a given type in a registry associated with a given class loader.
     *
     * @param classLoader the class loader to start searching in
     * @param typeDescriptor the type to look for
     * @param searchParents specifies if the {@linkplain ClassLoader#getParent() parents} of {@code classLoader} should
     *            be searched if the type is not in the registry of {@code classLoader}
     * @return the resolved actor corresponding to {@code typeDescriptor} or {@code null} if not found
     */
    public static ClassActor get(ClassLoader classLoader, TypeDescriptor typeDescriptor, boolean searchParents) {
        ClassRegistry registry = makeRegistry(classLoader);
        ClassActor classActor = registry.get(typeDescriptor);
        if (classActor != null) {
            return classActor;
        }

        if (!searchParents) {
            return null;
        }

        ClassLoader parent = classLoader.getParent();
        if (parent == null) {
            if (classLoader != BootClassLoader.BOOT_CLASS_LOADER) {
                // Every class loader should ultimately delegate to the boot class loader
                parent = BootClassLoader.BOOT_CLASS_LOADER;
            } else {
                return null;
            }
        }
        return get(parent, typeDescriptor, true);
    }

    public static TupleClassActor javaLangObjectActor() {
        return OBJECT;
    }

    public static TupleClassActor javaLangClassActor() {
        return CLASS;
    }

    public static TupleClassActor javaLangThrowableActor() {
        return THROWABLE;
    }

    public static InterfaceActor javaLangCloneableActor() {
        return CLONEABLE;
    }

    public static InterfaceActor javaIoSerializeableActor() {
        return SERIALIZABLE;
    }

    /**
     * An enumeration of the properties that can be associated with actors. These are properties for which only a small
     * percentage of actors will have a non-default value. As such, using maps to store the property values results in
     * a space saving.
     * <a>
     * One trade off of using maps for properties (as opposed to fields) is that access is slower and must be synchronized.
     *
     * @author Doug Simon
     */
    public enum Property {
        GENERIC_SIGNATURE(Actor.class, Utf8Constant.class, Actor.NO_GENERIC_SIGNATURE),
        RUNTIME_VISIBLE_ANNOTATION_BYTES(Actor.class, byte[].class, Actor.NO_RUNTIME_VISIBLE_ANNOTATION_BYTES),
        ENCLOSING_METHOD_INFO(ClassActor.class, EnclosingMethodInfo.class, null),
        INNER_CLASSES(ClassActor.class, TypeDescriptor[].class, null),
        OUTER_CLASS(ClassActor.class, TypeDescriptor.class, null),
        CHECKED_EXCEPTIONS(MethodActor.class, TypeDescriptor[].class, MethodActor.NO_CHECKED_EXCEPTIONS),
        CONSTANT_VALUE(FieldActor.class, Value.class, null),
        ANNOTATION_DEFAULT_BYTES(MethodActor.class, byte[].class, MethodActor.NO_ANNOTATION_DEFAULT_BYTES),
        INVOCATION_STUB(false, MethodActor.class, GeneratedStub.class, null),
        RUNTIME_VISIBLE_PARAMETER_ANNOTATION_BYTES(MethodActor.class, byte[].class, MethodActor.NO_RUNTIME_VISIBLE_PARAMETER_ANNOTATION_BYTES);

        public static final IndexedSequence<Property> VALUES = new ArraySequence<Property>(values());

        private final Class keyType;
        private final Class valueType;
        private final Object defaultValue;
        private final boolean isFinal;

        /**
         * Defines a property.
         *
         * @param isFinal determines if the property can only be set once for a given object
         * @param keyType the type of objects to which the property applies
         * @param valueType the type of the property's values
         * @param defaultValue the default value of the property
         */
        private Property(boolean isFinal, Class keyType, Class valueType, Object defaultValue) {
            this.keyType = keyType;
            this.valueType = valueType;
            this.defaultValue = defaultValue;
            this.isFinal = isFinal;
        }

        /**
         * Defines a property that can only be set once.
         *
         * @param keyType the type of objects to which the property applies
         * @param valueType the type of the property's values
         * @param defaultValue the default value of the property
         */
        private Property(Class keyType, Class valueType, Object defaultValue) {
            this(true, keyType, valueType, defaultValue);
        }

        VariableMapping<Object, Object> createMap() {
            return new ChainedHashMapping<Object, Object>();
        }

        /**
         * Sets the value of this property for a given key.
         *
         * @param mapping the mapping from keys to values for this property
         * @param object the object for which the value of this property is to be retrieved
         * @param value the value to be set
         */
        void set(VariableMapping<Object, Object> mapping, Object object, Object value) {
            assert keyType.isInstance(object);
            if (value != null) {
                assert valueType.isInstance(value);
                final Object oldValue = mapping.put(object, value);
                assert !isFinal || oldValue == null;
            } else {
                mapping.remove(object);
            }
        }

        /**
         * Gets the value of this property for a given key.
         *
         * @param mapping the mapping from keys to values for this property
         * @param object the object for which the value of this property is to be retrieved
         */
        Object get(VariableMapping<Object, Object> mapping, Object object) {
            assert keyType.isInstance(object);
            final Object value = mapping.get(object);
            if (value != null) {
                return value;
            }
            return defaultValue;
        }
    }

    /**
     * Sets the value of a given property for a given object.
     */
    public synchronized <Key_Type, Value_Type> void set(Property property, Key_Type object, Value_Type value) {
        property.set(propertyMaps.get(property), object, value);
    }

    /**
     * Gets the value of a given property for a given object.
     */
    public synchronized <Key_Type, Value_Type> Value_Type get(Property property, Key_Type object) {
        final Class<Value_Type> type = null;
        return StaticLoophole.cast(type, property.get(propertyMaps.get(property), object));
    }

    public void trace(int level) {
        if (!Trace.hasLevel(level)) {
            return;
        }
        final PrintStream out = Trace.stream();
        out.println("BEGIN ClassRegistry");
        for (ClassActor classActor : typeDescriptorToClassActor.values()) {
            out.println("    " + classActor.name);
        }
        out.println("END ClassRegistry");
    }
}
