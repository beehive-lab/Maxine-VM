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
package com.sun.max.tele;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * An implementation of a value as seen by the JDWP server.
 *
 * @author Thomas Wuerthinger
 *
 */
final class VMValueImpl implements VMValue {

    public static final VMValue VOID_VALUE = new VMValueImpl();

    private static GrowableMapping<Object, ObjectProvider> objectProviderCache = new IdentityHashMapping<Object, ObjectProvider>();

    private Type type;
    private Object value;

    private VMValueImpl() {
        type = Type.VOID;
        value = null;
    }

    public boolean isVoid() {
        return type == null;
    }

    public Byte asByte() {
        return type == Type.BYTE ? (Byte) value : null;
    }

    public Character asChar() {
        return type == Type.CHAR ? (Character) value : null;
    }

    public Short asShort() {
        return type == Type.SHORT ? (Short) value : null;
    }

    public Integer asInt() {
        return type == Type.INT ? (Integer) value : null;
    }

    public Float asFloat() {
        return type == Type.FLOAT ? (Float) value : null;
    }

    public Double asDouble() {
        return type == Type.DOUBLE ? (Double) value : null;
    }

    public Long asLong() {
        return type == Type.LONG ? (Long) value : null;
    }

    public Boolean asBoolean() {
        return type == Type.BOOLEAN ? (Boolean) value : null;
    }

    public Provider asProvider() {
        return type == Type.PROVIDER ? (Provider) value : null;
    }

    public Object asJavaObject() {
        if (value instanceof FakeObjectProvider) {
            return ((FakeObjectProvider) value).innerObject();
        }
        return value;
    }

    /**
     * Creates a JDWP value object that encapsulates a Java object living on the JDWP server side.
     * @param object the object that should be encapsulated in a JDWP value object
     * @param vm represents the VM in which this value should live
     * @param expectedClass the expected class that the faked value should have
     * @return a JDWP value object rpresenting the Java object
     */
    public static VMValue fromJavaObject(Object object, VMAccess vm, Class expectedClass) {
        final VMValueImpl value = new VMValueImpl();
        value.value = object;

        if (object == null) {
            value.type = Type.PROVIDER;
        } else if (expectedClass == Byte.TYPE) {
            value.type = Type.BYTE;
        } else if (expectedClass == Character.TYPE) {
            value.type = Type.CHAR;
        } else if (expectedClass == Short.TYPE) {
            value.type = Type.SHORT;
        } else if (expectedClass == Integer.TYPE) {
            value.type = Type.INT;
        } else if (expectedClass == Float.TYPE) {
            value.type = Type.FLOAT;
        } else if (expectedClass == Double.TYPE) {
            value.type = Type.DOUBLE;
        } else if (expectedClass == Long.TYPE) {
            value.type = Type.LONG;
        } else if (expectedClass == Boolean.TYPE) {
            value.type = Type.BOOLEAN;
        } else if (object instanceof ObjectProvider) {
            value.type = Type.PROVIDER;
        } else {

            // No matching type found => get fake object provider
            value.type = Type.PROVIDER;
            value.value = findFakeObjectProvider(object, vm);
        }

        return value;
    }

    /**
     * Looks up a JDWP object provider that encapsulates a certain Java object. If no object provider is found, a new one is generated.
     * @param object the object for which an object provider instance should be looked up
     * @param vm the VM in which this object provider lives
     * @return an ObjectProvider object encapsulating the given object
     */
    private static ObjectProvider findFakeObjectProvider(Object object, VMAccess vm) {

        assert object != null;

        if (!objectProviderCache.containsKey(object)) {
            objectProviderCache.put(object, createFakeObjectProvider(object, vm));
        }

        return objectProviderCache.get(object);
    }

    /**
     * Creates a new object provider encapsulating a certain Java object.
     * @param object the object that should be encapsulated in an ObjectProvider object
     * @param vm the VM in which the object provider should live in
     * @return an ObjectProvider object encapsulating the given object
     */
    private static ObjectProvider createFakeObjectProvider(Object object, VMAccess vm) {

        assert object != null;
        final ReferenceTypeProvider type = vm.getReferenceType(object.getClass());

        assert type != null : "The reference type for class " + object.getClass() + " could not be found!";

        if (type instanceof ArrayTypeProvider) {
            final ArrayTypeProvider arrayType = (ArrayTypeProvider) type;
            return new FakeArrayProvider(object, arrayType, vm);
        }

        if (object instanceof String) {
            return new FakeStringProvider((String) object, type);
        }

        return new FakeObjectProvider(object, type);
    }

    @Override
    public String toString() {
        return "VMValue(" + type + "): " + value;
    }

    private static class FakeStringProvider extends FakeObjectProvider implements StringProvider {

        private String stringValue;

        public FakeStringProvider(String stringValue, ReferenceTypeProvider type) {
            super(stringValue, type);
            this.stringValue = stringValue;
        }

        public String stringValue() {
            return stringValue;
        }
    }

    private static class FakeObjectProvider implements ObjectProvider {

        private Object innerObject;
        private ReferenceTypeProvider type;

        public FakeObjectProvider(Object innerObject, ReferenceTypeProvider type) {
            assert innerObject != null : "The inner object must not be null, otherwise the object is a valid value for an object provider anyway!";
            this.innerObject = innerObject;
            this.type = type;
        }

        public ReferenceTypeProvider getReferenceType() {
            return type;
        }

        Object innerObject() {
            return innerObject;
        }
    }

    private static class FakeArrayProvider extends FakeObjectProvider implements ArrayProvider {

        private ArrayTypeProvider arrayType;
        private VMAccess vm;

        public FakeArrayProvider(Object innerObject, ArrayTypeProvider arrayType, VMAccess vm) {
            super(innerObject, arrayType);
            this.arrayType = arrayType;
            this.vm = vm;
        }

        public ArrayTypeProvider getArrayType() {
            return arrayType;
        }

        public VMValue getValue(int i) {
            final Class klass = innerObject().getClass().getComponentType();
            assert klass != null;

            // Create values for array elements lazily.
            return vm.createJavaObjectValue(Array.get(innerObject(), i), klass);
        }

        public int length() {
            return Array.getLength(innerObject());
        }

        public void setValue(int i, VMValue value) {
            final Class klass = innerObject().getClass().getComponentType();
            assert klass != null;
            Array.set(innerObject(), i, value.asJavaObject());
        }
    }
}
