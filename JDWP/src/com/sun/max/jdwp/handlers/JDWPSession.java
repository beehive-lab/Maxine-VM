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
package com.sun.max.jdwp.handlers;

import java.util.logging.*;

import com.sun.max.collect.*;
import com.sun.max.jdwp.constants.*;
import com.sun.max.jdwp.constants.Error;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.data.ID.*;
import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;

/**
 * This class is respondible for handling a JDWP session in terms of managing the JDWP object space.
 * It contains utility functions for looking up the identifier of an object or looking up an object based on the identifier.
 *
 * @author Thomas Wuerthinger
 *
 */
public class JDWPSession {

    private static final Logger LOGGER = Logger.getLogger(Handlers.class.getName());

    private VMAccess vm;

    private VariableMapping<ID, Provider> idToProvider;
    private VariableMapping<Provider, ID> providerToID;

    private VariableMapping<MethodProvider, ReferenceTypeProvider> methodToReferenceType;
    private VariableMapping<FieldProvider, ReferenceTypeProvider> fieldToReferenceType;
    private VariableMapping<FrameProvider, ThreadProvider> frameToThread;
    private long lastID;

    public JDWPSession(VMAccess vm) {
        assert vm != null : "Virtual machine abstraction must not be null";
        this.vm = vm;
        idToProvider = new ChainedHashMapping<ID, Provider>();

        final Class<HashIdentity<Provider>> type = null;
        providerToID = new ChainedHashMapping<Provider, ID>(HashIdentity.instance(type));

        final Class<HashIdentity<MethodProvider>> type2 = null;
        methodToReferenceType = new ChainedHashMapping<MethodProvider, ReferenceTypeProvider>(HashIdentity.instance(type2));

        final Class<HashIdentity<FieldProvider>> type3 = null;
        fieldToReferenceType = new ChainedHashMapping<FieldProvider, ReferenceTypeProvider>(HashIdentity.instance(type3));

        final Class<HashIdentity<FrameProvider>> type4 = null;
        frameToThread = new ChainedHashMapping<FrameProvider, ThreadProvider>(HashIdentity.instance(type4));
    }

    public static int getValueTypeTag(VMValue.Type type) {

        switch (type) {
            case BOOLEAN:
                return Tag.BOOLEAN;
            case BYTE:
                return Tag.BYTE;
            case CHAR:
                return Tag.CHAR;
            case DOUBLE:
                return Tag.DOUBLE;
            case FLOAT:
                return Tag.FLOAT;
            case INT:
                return Tag.INT;
            case LONG:
                return Tag.LONG;
            case PROVIDER:
                return Tag.OBJECT;
            case SHORT:
                return Tag.SHORT;
            case VOID:
                return Tag.VOID;
        }

        throw new IllegalArgumentException("Unknown virtual machine type: " + type);

    }

    public VMAccess vm() {
        return vm;
    }

    /**
     * All events should be hold back until {@link releaseEvents()} is called.
     * @throws JDWPException
     */
    public void holdEvents() throws JDWPException {
        // TODO: Add implementation.
        throw new JDWPNotImplementedException();
    }

    /**
     * All events that were hold back because of a call to {@link holdEvents()} should be set free.
     * @throws JDWPException
     */
    public void releaseEvents() throws JDWPException {
        // TODO: Add implementation.
        throw new JDWPNotImplementedException();
    }

    /**
     * Looks up an ID object of a Provider object or creates a new ID object if none is found.
     * @param provider the provider for which the ID is looked up
     * @param idKlass the class of the ID object
     * @return the ID object representing the given Provider object
     */
    private <Provider_Type extends Provider, ID_Type extends ID> ID_Type makeID(Provider_Type provider, Class<ID_Type> idKlass) {
        if (provider == null) {
            return ID.create(0, idKlass);
        }

        if (!providerToID.containsKey(provider)) {
            lastID++;
            final ID_Type newID = ID.create(lastID, idKlass);
            idToProvider.put(newID, provider);
            providerToID.put(provider, newID);
            assert providerToID.containsKey(provider);
            assert idToProvider.containsKey(newID);
            assert idToProvider.containsKey(ID.convert(newID, ID.class));
            LOGGER.fine("Created new ID " + lastID + " for provider " + provider);
            return newID;
        }
        final ID result = providerToID.get(provider);

        // TODO: Make a better check-system!
        /*if (!idKlass.isAssignableFrom(result.getClass())) {
            throw new IllegalArgumentException("The ID " + result + " found for provider " + provider + " is not assignable to type " + idKlass.getName() + " but of type " + result.getClass());
        }*/

        return ID.convert(result, idKlass);
        //return StaticLoophole.cast(idKlass, result);
    }

    /**
     * Looks up a Provider object based on the ID and throws an exception if no Provider object is found.
     * @param errorCode the error code of the exception that should be thrown in case no Provider object is found
     * @param klass the klass of the returned Provider object
     * @param id identifier of the object to look for
     * @return the object that has the given identifier
     * @throws JDWPException this exception is thrown, when no provider object was found
     */
    private <Provider_Type extends Provider, ID_Type extends ID> Provider_Type lookup(int errorCode, Class<Provider_Type> klass, ID_Type id) throws JDWPException {

        if (id.value() == 0) {
            return null;
        }

        if (!idToProvider.containsKey(id)) {
            throw new JDWPException((short) Error.INVALID_OBJECT, "The id " + id + " is unknown!");
        }
        final Provider result = idToProvider.get(id);
        // TODO: Check if there can be a better assertion.
        // if (klass.isAssignableFrom(result.getClass())) {
        // throw new JDWPException(errorCode, "The object found at id " + id + " is not a valid instance of " +
        // klass.getName() + " but: " + result);
        // }
        return StaticLoophole.cast(klass, result);
    }

    public ClassObjectProvider getClassObject(ClassObjectID id) throws JDWPException {
        return lookup(Error.INVALID_OBJECT, ClassObjectProvider.class, id);
    }

    public ArrayProvider getArray(ArrayID id) throws JDWPException {
        return lookup(Error.INVALID_ARRAY, ArrayProvider.class, id);
    }

    public ArrayTypeProvider getArrayType(ArrayTypeID id) throws JDWPException {
        return lookup(Error.INVALID_CLASS, ArrayTypeProvider.class, id);
    }

    public ClassLoaderProvider getClassLoader(ClassLoaderID id) throws JDWPException {
        return lookup(Error.INVALID_CLASS_LOADER, ClassLoaderProvider.class, id);
    }

    public ClassProvider getClass(ClassID id) throws JDWPException {
        return lookup(Error.INVALID_CLASS, ClassProvider.class, id);
    }

    /**
     * Checks whether the field really belongs to the given reference type.
     * @param refType
     * @param f
     * @throws JDWPException
     */
    private void checkField(ReferenceTypeProvider referenceTypeProvider, FieldProvider fieldProvider) throws JDWPException {

        if (!this.fieldToReferenceType.containsKey(fieldProvider)) {
            fieldToReferenceType.put(fieldProvider, referenceTypeProvider);
        }

        // TODO: Check subclass / class relationship.
        // if (_fieldToReferenceType.get(fieldProvider) != referenceTypeProvider) {
        // throw new JDWPException("Field has wrong parent reference type!");
        // }
    }

    private void checkMethod(ReferenceTypeProvider refType, MethodProvider m) throws JDWPException {
        if (!this.methodToReferenceType.containsKey(m)) {
            methodToReferenceType.put(m, refType);
        }

        // TODO: Check subclass / class relationship.
        // if (_methodToReferenceType.get(m) != refType) {
        // throw new JDWPException("Method has wrong parent reference type! Cached type: " +
        // _methodToReferenceType.get(m) + ", actual type: " + refType);
        // }
    }

    private void checkFrame(ThreadProvider thread, FrameProvider frameProvider) throws JDWPException {
        if (!this.frameToThread.containsKey(frameProvider)) {
            frameToThread.put(frameProvider, thread);
        }

        if (this.frameToThread.get(frameProvider) != thread) {
            throw new JDWPException("Frame has wrong parent thread!");
        }
    }

    public ThreadProvider frameToThread(FrameProvider f) {
        if (f == null) {
            return null;
        }
        return f.getThread();
    }

    public ReferenceTypeProvider methodToReferenceType(MethodProvider m) {
        if (m == null) {
            return null;
        }
        return m.getReferenceTypeHolder(); // _methodToReferenceType.get(m);
    }

    public ReferenceTypeProvider fieldToReferenceType(FieldProvider f) {
        if (f == null) {
            return null;
        }
        return f.getReferenceTypeHolder(); // _fieldToReferenceType.get(f);
    }

    public FieldProvider getField(ReferenceTypeID id, FieldID fid) throws JDWPException {
        final ReferenceTypeProvider refType = getReferenceType(id);
        final FieldProvider f = lookup(Error.INVALID_FIELDID, FieldProvider.class, fid);
        checkField(refType, f);
        return f;
    }

    public ThreadProvider getThread(ThreadID id) throws JDWPException {
        return lookup(Error.INVALID_THREAD, ThreadProvider.class, id);
    }

    public MethodProvider getMethod(ReferenceTypeID id, MethodID mid) throws JDWPException {
        final ReferenceTypeProvider refType = getReferenceType(id);
        final MethodProvider m = lookup(Error.INVALID_METHODID, MethodProvider.class, mid);
        checkMethod(refType, m);
        return m;
    }

    public ObjectProvider getObject(ObjectID id) throws JDWPException {
        return lookup(Error.INVALID_OBJECT, ObjectProvider.class, id);
    }

    public FrameProvider getFrame(ThreadID id, FrameID fid) throws JDWPException {
        final ThreadProvider thread = getThread(id);
        final FrameProvider f = lookup(Error.INVALID_FRAMEID, FrameProvider.class, fid);
        checkFrame(thread, f);
        return f;
    }

    public StringProvider getString(StringID id) throws JDWPException {
        return lookup(Error.INVALID_STRING, StringProvider.class, id);
    }

    public ThreadGroupProvider getThreadGroup(ThreadGroupID id) throws JDWPException {
        return lookup(Error.INVALID_THREAD_GROUP, ThreadGroupProvider.class, id);
    }

    public ReferenceTypeProvider getReferenceType(ReferenceTypeID id) throws JDWPException {
        return lookup(Error.INVALID_CLASS, ReferenceTypeProvider.class, id);
    }

    public ID.ArrayID toID(ArrayProvider p) {
        return makeID(p, ID.ArrayID.class);
    }

    public ID.FieldID toID(FieldProvider p) {
        return makeID(p, ID.FieldID.class);
    }

    public ID.FrameID toID(FrameProvider p) {
        return makeID(p, ID.FrameID.class);
    }

    public InterfaceID toID(InterfaceProvider p) {
        return makeID(p, ID.InterfaceID.class);
    }

    public ReferenceTypeID toID(ReferenceTypeProvider p) {
        if (p == null) {
            return ID.ReferenceTypeID.create(0, ID.ReferenceTypeID.class);
        } else if (p instanceof ClassProvider) {
            return makeID(p, ID.ClassID.class);
        } else if (p instanceof ArrayProvider) {
            return makeID(p, ID.ArrayTypeID.class);
        } else if (p instanceof InterfaceProvider) {
            return makeID(p, ID.InterfaceID.class);
        } else {
            assert false : "Unknown subclass of ReferenceTypeProvider " + p + "!";
            return null;
        }
    }

    public ClassID toID(ClassProvider p) {
        return makeID(p, ID.ClassID.class);
    }

    public ObjectID toID(ObjectProvider p) {
        return makeID(p, ID.ObjectID.class);
    }

    public ThreadGroupID toID(ThreadGroupProvider p) {
        return makeID(p, ID.ThreadGroupID.class);
    }

    public StringID toID(StringProvider p) {
        return makeID(p, ID.StringID.class);
    }

    public ThreadID toID(ThreadProvider p) {
        return makeID(p, ID.ThreadID.class);
    }

    public ClassObjectID toID(ClassObjectProvider p) {
        return makeID(p, ID.ClassObjectID.class);
    }

    public ClassLoaderID toID(ClassLoaderProvider p) {
        return makeID(p, ID.ClassLoaderID.class);
    }

    public FieldID toID(ReferenceTypeProvider refType, FieldProvider p) throws JDWPException {
        checkField(refType, p);
        return makeID(p, ID.FieldID.class);
    }

    public MethodID toID(ReferenceTypeProvider refType, MethodProvider p) throws JDWPException {
        checkMethod(refType, p);
        return makeID(p, ID.MethodID.class);
    }

    public MethodID toID(MethodProvider p) {
        return makeID(p, ID.MethodID.class);
    }

    public FrameID toID(ThreadProvider thread, FrameProvider p) throws JDWPException {
        checkFrame(thread, p);
        return makeID(p, ID.FrameID.class);
    }

    public JDWPLocation fromCodeLocation(CodeLocation location) {
        final ReferenceTypeProvider refType = methodToReferenceType(location.method());
        return new JDWPLocation(getTypeTag(refType), ID.convert(toID(refType), ID.ClassID.class), toID(location.method()), location.position());
    }

    /**
     * Returns the JDWP type tag based on the class type of a ReferenceTypeProvider object.
     * @param refType the reference type object whose JDWP type tag should be returned
     * @return the JDWP type tag
     */
    public byte getTypeTag(ReferenceTypeProvider refType) {

        if (refType == null) {
            return 0;
        }

        if (ArrayTypeProvider.class.isAssignableFrom(refType.getClass())) {
            return TypeTag.ARRAY;
        } else if (ClassProvider.class.isAssignableFrom(refType.getClass())) {
            return TypeTag.CLASS;
        } else if (InterfaceProvider.class.isAssignableFrom(refType.getClass())) {
            return TypeTag.INTERFACE;
        }
        assert false : "Must not reach here, no other type of Provider.ReferenceType objects allowed! Type: " + refType.getClass();
        return 0;
    }

    public CodeLocation toCodeLocation(JDWPLocation location) throws JDWPException {
        final MethodProvider curMethod = getMethod(location.getClassID(), location.getMethodID());
        final int curPosition = (int) location.getIndex();
        return vm().createCodeLocation(curMethod, curPosition, false);
    }

    /**
     * Creates a JDWPValue object based on a virtual machine value object.
     * @param v the value that should be converted to a JDWPValue object
     * @return the newly created JDWPValue object representing the given Maxine Value object
     * @throws JDWPException this exception is thrown, when there occurred an error during the conversion
     */
    public JDWPValue toJDWPValue(VMValue v) throws JDWPException {

        if (v.isVoid()) {
            return new JDWPValue();
        } else if (v.asBoolean() != null) {
            return new JDWPValue(v.asBoolean());
        } else if (v.asByte() != null) {
            return new JDWPValue(v.asByte());
        } else if (v.asChar() != null) {
            return new JDWPValue(v.asChar());
        } else if (v.asDouble() != null) {
            return new JDWPValue(v.asDouble());
        } else if (v.asFloat() != null) {
            return new JDWPValue(v.asFloat());
        } else if (v.asInt() != null) {
            return new JDWPValue(v.asInt());
        } else if (v.asLong() != null) {
            return new JDWPValue(v.asLong());
        } else if (v.asShort() != null) {
            return new JDWPValue(v.asShort());
        } else {
            final Provider p = v.asProvider();
            if (p == null) {
                return new JDWPValue(ID.create(0, ObjectID.class));
            }

            if (p instanceof FieldProvider) {
                return new JDWPValue(toID((FieldProvider) p));
            } else if (p instanceof FrameProvider) {
                throw new IllegalArgumentException("Cannot convert FrameProvider " + p + " + to JDWP value!");
            } else if (p instanceof StringProvider) {
                return new JDWPValue(toID((StringProvider) p));
            } else if (p instanceof ArrayProvider) {
                return new JDWPValue(toID((ArrayProvider) p));
            } else if (p instanceof ObjectProvider) {
                return new JDWPValue(toID((ObjectProvider) p));
            }
        }

        throw new IllegalArgumentException("Could not convert value " + v + " to a JDWP value!");
    }

    /**
     * Creates a Maxine Value object based on the given JDWPValue object.
     * @param value the value to be converted to a Maxine Value object
     * @return a newly created Maxine Value object representing the given parameter
     * @throws JDWPException this exception is thrown when an error occurred during the conversion
     */
    public VMValue toValue(JDWPValue value) throws JDWPException {

        switch (value.tag()) {
            case Tag.BOOLEAN:
                return vm().createBooleanValue(value.asBoolean());
            case Tag.BYTE:
                return vm().createByteValue(value.asByte());
            case Tag.CHAR:
                return vm().createCharValue(value.asCharacter());
            case Tag.DOUBLE:
                return vm().createDoubleValue(value.asByte());
            case Tag.FLOAT:
                return vm().createFloatValue(value.asFloat());
            case Tag.INT:
                return vm().createIntValue(value.asInteger());
            case Tag.LONG:
                return vm().createLongValue(value.asLong());
            case Tag.SHORT:
                return vm().createShortValue(value.asShort());
            case Tag.ARRAY:
                return vm().createObjectProviderValue(this.getArray(value.asArray()));
            case Tag.CLASS_LOADER:
                return vm().createObjectProviderValue(this.getClassLoader(value.asClassLoader()));
            case Tag.CLASS_OBJECT:
                return vm().createObjectProviderValue(this.getClassObject(value.asClassObject()));
            case Tag.OBJECT:
                return vm().createObjectProviderValue(this.getObject(value.asObject()));
            case Tag.STRING:
                return vm().createObjectProviderValue(this.getString(value.asString()));
            case Tag.THREAD:
                return vm().createObjectProviderValue(this.getThread(value.asThread()));
            case Tag.THREAD_GROUP:
                return vm().createObjectProviderValue(this.getThreadGroup(value.asThreadGroup()));
            case Tag.VOID:
                return vm().getVoidValue();
        }

        throw new IllegalArgumentException("Argument " + value + " could not be converted into a VirtualMachineValue object!");
    }

    /**
     * Retrieves the status of a thread.
     * @param thread the thread whose status should be queried
     * @return the status of the given thread
     */
    public int getThreadStatus(ThreadProvider thread) {
        // TODO: Correct implementation
        return ThreadStatus.RUNNING;
    }

    /**
     * Retrieves the suspend status of a thread.
     * @param thread the thread whose suspend status should be queried
     * @return the suspend status of the given thread
     */
    public int getSuspendStatus(ThreadProvider thread) {
        // TODO: Correct implementation
        return SuspendStatus.SUSPEND_STATUS_SUSPENDED;
    }
}
