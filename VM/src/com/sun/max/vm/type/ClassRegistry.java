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

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.object.TupleAccess;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.value.*;

/**
 * Each class loader is associated with a class registry and vice versa.
 * The class registry augments the class loader with a mapping from TypeDescriptor to class actor.
 *
 * Prototype/vm/system/null class loaders are "associated" with the static value '_vmClassRegistry'.
 *
 * @author Bernd Mathiske
 */
public final class ClassRegistry implements IterableWithLength<ClassActor> {

    private static final EnumMap<Property, VariableMapping<Object, Object>> propertyMapsPrototyp = new EnumMap<Property, VariableMapping<Object, Object>>(Property.class);

    private ClassRegistry() {
        propertyMaps = propertyMapsPrototyp.clone();
        for (Property property : Property.VALUES) {
            propertyMaps.put(property, property.createMap());
        }
    }

    /**
     * This is only here to support ClassFileWriter.testLoadGeneratedClasses().
     */
    @PROTOTYPE_ONLY
    public static final Map<ClassLoader, ClassRegistry> classLoaderToRegistryMap = new IdentityHashMap<ClassLoader, ClassRegistry>() {
        @Override
        public ClassRegistry put(ClassLoader key, ClassRegistry value) {
            if (value == null) {
                return super.put(key, new ClassRegistry());
            }
            return super.put(key, value);
        }
    };

    private static final ClassRegistry vmClassRegistry = new ClassRegistry();

    public static ClassRegistry vmClassRegistry() {
        return vmClassRegistry;
    }

    public static ClassRegistry makeRegistry(ClassLoader classLoader) {
        if (MaxineVM.isPrototyping()) {
            final ClassRegistry classRegistry = classLoaderToRegistryMap.get(classLoader);
            if (classRegistry != null) {
                return classRegistry;
            }
            return vmClassRegistry;
        }
        if (classLoader == null) {
            return vmClassRegistry;
        }
        synchronized (classLoader) {
            final ClassRegistry result = (ClassRegistry) TupleAccess.readObject(classLoader, ClassLoader_classRegistry.offset());
            if (result != null) {
                return result;
            }
            final ClassRegistry registry = new ClassRegistry();
            TupleAccess.writeObject(classLoader, ClassLoader_classRegistry.offset(), registry);
            return registry;
        }
    }

    @INSPECTED
    private final GrowableMapping<TypeDescriptor, ClassActor> typeDescriptorToClassActor = new ChainedHashMapping<TypeDescriptor, ClassActor>(5000);

    public int numberOfClassActors() {
        return typeDescriptorToClassActor.length();
    }

    public Iterator<ClassActor> iterator() {
        return typeDescriptorToClassActor.values().iterator();
    }

    public int length() {
        return numberOfClassActors();
    }

    private static TupleClassActor javaLangObjectActor;
    private static TupleClassActor javaLangClassActor;
    private static InterfaceActor javaLangCloneableActor;
    private static InterfaceActor javaIoSerializableActor;

    private void put(ClassActor classActor) {
        final TypeDescriptor typeDescriptor = classActor.typeDescriptor;
        final ClassActor existingClassActor = typeDescriptorToClassActor.put(typeDescriptor, classActor);
        if (existingClassActor != null) {
            ProgramWarning.message("Cannot add class actor for " + classActor.name + " to registry more than once");
            return;
        }
        if (MaxineVM.isPrototyping() && this == vmClassRegistry) {
            if (javaLangObjectActor == null && typeDescriptor.equals(JavaTypeDescriptor.OBJECT)) {
                javaLangObjectActor = (TupleClassActor) classActor;
            }
            if (javaLangClassActor == null && typeDescriptor.equals(JavaTypeDescriptor.CLASS)) {
                javaLangClassActor = (TupleClassActor) classActor;
            }
            if (javaLangCloneableActor == null && typeDescriptor.equals(JavaTypeDescriptor.CLONEABLE)) {
                javaLangCloneableActor = (InterfaceActor) classActor;
            }
            if (javaIoSerializableActor == null && typeDescriptor.equals(JavaTypeDescriptor.SERIALIZABLE)) {
                javaIoSerializableActor = (InterfaceActor) classActor;
            }
        }
    }

    /**
     * Registers a [ClassLoader, ClassActor] pair with the global type registry.
     * <p>
     * Only completely initialized ClassActors must be registered with the ClassRegistry.
     * This prevents another thread from getting hold of a partially initialized ClassActor.
     * As such, registration must not be performed from within the ClassActor constructor
     * or any ClassActor subclass constructor. Instead, it should be done at the call site
     * of the 'new' that is allocating a ClassActor instance, typically by enclosing the 'new'
     * expression with a call to this method.
     */
    public static <ClassActor_Type extends ClassActor> ClassActor_Type put(ClassLoader classLoader, ClassActor_Type classActor) {
        makeRegistry(classLoader).put(classActor);
        return classActor;
    }

    public ClassActor get(TypeDescriptor typeDescriptor) {
        final ClassActor classActor = typeDescriptorToClassActor.get(typeDescriptor);
        if (false && (!MaxineVM.isPrototyping() && classActor == null)) {
            Log.print("unresolved: ");
            Log.println(typeDescriptor.string);
        }
        return classActor;
    }

    public static ClassActor get(ClassLoader classLoader, TypeDescriptor typeDescriptor) {
        return makeRegistry(classLoader).get(typeDescriptor);
    }

    public boolean contains(TypeDescriptor typeDescriptor) {
        return typeDescriptorToClassActor.containsKey(typeDescriptor);
    }

    public static boolean contains(ClassLoader classLoader, TypeDescriptor typeDescriptor) {
        return makeRegistry(classLoader).contains(typeDescriptor);
    }

    public static TupleClassActor javaLangObjectActor() {
        assert javaLangObjectActor != null;
        return javaLangObjectActor;
    }

    public static TupleClassActor javaLangClassActor() {
        assert javaLangClassActor != null;
        return javaLangClassActor;
    }

    public static InterfaceActor javaLangCloneableActor() {
        assert javaLangCloneableActor != null;
        return javaLangCloneableActor;
    }

    public static InterfaceActor javaIoSerializeableActor() {
        assert javaIoSerializableActor != null;
        return javaIoSerializableActor;
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
                synchronized (mapping) {
                    final Object oldValue = mapping.put(object, value);
                    assert !isFinal || oldValue == null;
                }
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
            synchronized (mapping) {
                final Object value = mapping.get(object);
                if (value != null) {
                    return value;
                }
            }
            return defaultValue;
        }
    }

    private final Map<Property, VariableMapping<Object, Object>> propertyMaps;

    /**
     * Sets the value of a given property for a given object.
     */
    public <Key_Type, Value_Type> void set(Property property, Key_Type object, Value_Type value) {
        property.set(propertyMaps.get(property), object, value);
    }

    /**
     * Gets the value of a given property for a given object.
     */
    public <Key_Type, Value_Type> Value_Type get(Property property, Key_Type object) {
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
