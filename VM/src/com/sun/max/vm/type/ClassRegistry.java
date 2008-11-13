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

    private static final EnumMap<Property, VariableMapping<Object, Object>> _propertyMapsPrototype = new EnumMap<Property, VariableMapping<Object, Object>>(Property.class);

    private ClassRegistry() {
        _propertyMaps = _propertyMapsPrototype.clone();
        for (Property property : Property.VALUES) {
            _propertyMaps.put(property, property.createMap());
        }
    }

    /**
     * This is only here to support ClassFileWriter.testLoadGeneratedClasses().
     */
    @PROTOTYPE_ONLY
    public static final Map<ClassLoader, ClassRegistry> _classLoaderToRegistryMap = new IdentityHashMap<ClassLoader, ClassRegistry>() {
        @Override
        public ClassRegistry put(ClassLoader key, ClassRegistry value) {
            if (value == null) {
                return super.put(key, new ClassRegistry());
            }
            return super.put(key, value);
        }
    };

    private static final ClassRegistry _vmClassRegistry = new ClassRegistry();

    public static ClassRegistry vmClassRegistry() {
        return _vmClassRegistry;
    }

    public static ClassRegistry makeRegistry(ClassLoader classLoader) {
        if (MaxineVM.isPrototyping()) {
            final ClassRegistry classRegistry = _classLoaderToRegistryMap.get(classLoader);
            if (classRegistry != null) {
                return classRegistry;
            }
            return _vmClassRegistry;
        }
        if (classLoader == null) {
            return _vmClassRegistry;
        }
        synchronized (classLoader) {
            final ClassRegistry result = ClassLoader_classRegistry.read(classLoader);
            if (result != null) {
                return result;
            }
            final ClassRegistry registry = new ClassRegistry();
            ClassLoader_classRegistry.writeObject(classLoader, registry);
            return registry;
        }
    }

    @INSPECTED
    private final GrowableMapping<TypeDescriptor, ClassActor> _typeDescriptorToClassActor = new ChainedHashMapping<TypeDescriptor, ClassActor>(5000);

    public int numberOfClassActors() {
        return _typeDescriptorToClassActor.length();
    }

    public Iterator<ClassActor> iterator() {
        return _typeDescriptorToClassActor.values().iterator();
    }

    public int length() {
        return numberOfClassActors();
    }

    private static TupleClassActor _javaLangObjectActor;
    private static TupleClassActor _javaLangClassActor;
    private static InterfaceActor _javaLangCloneableActor;
    private static InterfaceActor _javaIoSerializableActor;

    private void put(ClassActor classActor) {
        final TypeDescriptor typeDescriptor = classActor.typeDescriptor();
        final ClassActor existingClassActor = _typeDescriptorToClassActor.put(typeDescriptor, classActor);
        if (existingClassActor != null) {
            ProgramWarning.message("Cannot add class actor for " + classActor.name() + " to registry more than once");
            return;
        }
        if (MaxineVM.isPrototyping() && this == _vmClassRegistry) {
            if (_javaLangObjectActor == null && typeDescriptor.equals(JavaTypeDescriptor.OBJECT)) {
                _javaLangObjectActor = (TupleClassActor) classActor;
            }
            if (_javaLangClassActor == null && typeDescriptor.equals(JavaTypeDescriptor.CLASS)) {
                _javaLangClassActor = (TupleClassActor) classActor;
            }
            if (_javaLangCloneableActor == null && typeDescriptor.equals(JavaTypeDescriptor.CLONEABLE)) {
                _javaLangCloneableActor = (InterfaceActor) classActor;
            }
            if (_javaIoSerializableActor == null && typeDescriptor.equals(JavaTypeDescriptor.SERIALIZABLE)) {
                _javaIoSerializableActor = (InterfaceActor) classActor;
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
        final ClassActor classActor = _typeDescriptorToClassActor.get(typeDescriptor);
        if (false && (!MaxineVM.isPrototyping() && classActor == null)) {
            Log.print("unresolved: ");
            Log.println(typeDescriptor.string());
        }
        return classActor;
    }

    public static ClassActor get(ClassLoader classLoader, TypeDescriptor typeDescriptor) {
        return makeRegistry(classLoader).get(typeDescriptor);
    }

    public boolean contains(TypeDescriptor typeDescriptor) {
        return _typeDescriptorToClassActor.containsKey(typeDescriptor);
    }

    public static boolean contains(ClassLoader classLoader, TypeDescriptor typeDescriptor) {
        return makeRegistry(classLoader).contains(typeDescriptor);
    }

    public static TupleClassActor javaLangObjectActor() {
        assert _javaLangObjectActor != null;
        return _javaLangObjectActor;
    }

    public static TupleClassActor javaLangClassActor() {
        assert _javaLangClassActor != null;
        return _javaLangClassActor;
    }

    public static InterfaceActor javaLangCloneableActor() {
        assert _javaLangCloneableActor != null;
        return _javaLangCloneableActor;
    }

    public static InterfaceActor javaIoSerializeableActor() {
        assert _javaIoSerializableActor != null;
        return _javaIoSerializableActor;
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

        private final Class _keyType;
        private final Class _valueType;
        private final Object _defaultValue;
        private final boolean _isFinal;

        /**
         * Defines a property that can only be set once.
         *
         * @param keyType
         *                the type of objects to which the property applies
         * @param valueType
         *                the type of the property's values
         * @param defaultValue
         *                the default value of the property
         */
        private Property(Class keyType, Class valueType, Object defaultValue) {
            this(true, keyType, valueType, defaultValue);
        }

        /**
         * Defines a property.
         *
         * @param isFinal
         *                determines if the property can only be set once for a given object
         * @param keyType
         *                the type of objects to which the property applies
         * @param valueType
         *                the type of the property's values
         * @param defaultValue
         *                the default value of the property
         */
        private Property(boolean isFinal, Class keyType, Class valueType, Object defaultValue) {
            _keyType = keyType;
            _valueType = valueType;
            _defaultValue = defaultValue;
            _isFinal = isFinal;
        }

        VariableMapping<Object, Object> createMap() {
            return new ChainedHashMapping<Object, Object>();
        }

        /**
         * Sets the value of this property for a given key.
         *
         * @param mapping
         *                the mapping from keys to values for this property
         * @param object
         *                the object for which the value of this property is to be retrieved
         * @param value
         *                the value to be set
         */
        void set(VariableMapping<Object, Object> mapping, Object object, Object value) {
            assert _keyType.isInstance(object);
            if (value != null) {
                assert _valueType.isInstance(value);
                synchronized (mapping) {
                    final Object oldValue = mapping.put(object, value);
                    assert !_isFinal || oldValue == null;
                }
            } else {
                mapping.remove(object);
            }
        }

        /**
         * Gets the value of this property for a given key.
         *
         * @param mapping
         *                the mapping from keys to values for this property
         * @param object
         *                the object for which the value of this property is to be retrieved
         */
        Object get(VariableMapping<Object, Object> mapping, Object object) {
            assert _keyType.isInstance(object);
            synchronized (mapping) {
                final Object value = mapping.get(object);
                if (value != null) {
                    return value;
                }
            }
            return _defaultValue;
        }
    }

    private final Map<Property, VariableMapping<Object, Object>> _propertyMaps;

    /**
     * Sets the value of a given property for a given object.
     */
    public <Key_Type, Value_Type> void set(Property property, Key_Type object, Value_Type value) {
        property.set(_propertyMaps.get(property), object, value);
    }

    /**
     * Gets the value of a given property for a given object.
     */
    public <Key_Type, Value_Type> Value_Type get(Property property, Key_Type object) {
        final Class<Value_Type> type = null;
        return StaticLoophole.cast(type, property.get(_propertyMaps.get(property), object));
    }

    public void trace(int level) {
        if (!Trace.hasLevel(level)) {
            return;
        }
        final PrintStream out = Trace.stream();
        out.println("BEGIN ClassRegistry");
        for (ClassActor classActor : _typeDescriptorToClassActor.values()) {
            out.println("    " + classActor.name());
        }
        out.println("END ClassRegistry");
    }
}
