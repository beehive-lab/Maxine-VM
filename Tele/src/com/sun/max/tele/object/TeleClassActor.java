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
package com.sun.max.tele.object;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.jdwp.vm.proxy.VMValue.*;
import com.sun.max.tele.*;
import com.sun.max.tele.value.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;


/**
 * Canonical surrogate for a  {@link ClassActor} in the Target VM.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleClassActor extends TeleActor implements ReferenceTypeProvider {

    protected TeleClassActor(TeleVM teleVM, Reference classActorReference) {
        super(teleVM, classActorReference);
    }

    private ClassActor _classActor;

    /**
     * @return Local {@link ClassActor} corresponding the the target VM's {@link ClassActor}.
     */
    public ClassActor classActor() {
        if (_classActor == null) {
            // Requires loading the class; delay until needed.
            _classActor = teleVM().makeClassActor(reference());
        }
        return _classActor;
    }

    private boolean _initialized = false;

    // Fields are final; cache them.
    private TeleClassLoader _teleClassLoader;
    private int _id;
    private TeleTypeDescriptor _teleTypeDescriptor;

    private void initialize() {
        if (!_initialized) {

            final Reference classLoaderReference = teleVM().fields().ClassActor_classLoader.readReference(reference());
            _teleClassLoader = (TeleClassLoader) TeleObject.make(teleVM(), classLoaderReference);

            _id = teleVM().fields().ClassActor_id.readInt(reference());

            final Reference typeDescriptorReference = teleVM().fields().ClassActor_typeDescriptor.readReference(reference());
            _teleTypeDescriptor = (TeleTypeDescriptor) TeleObject.make(teleVM(), typeDescriptorReference);

            _initialized = true;
        }
    }

    /**
     * @return surrogate for the {@link ClassLoader} in the tele VM for this class in the tele VM.
     * There is no local counterpart for these; all local surrogate objects are created by one
     * ClassLoader
     */
    public TeleClassLoader getTeleClassLoader() {
        initialize();
        return _teleClassLoader;
    }

    /**
     * @return the serial ID of the {@link ClassActor} in the tele VM.
     */
    public int getId() {
        initialize();
        return _id;
    }

    /**
     * @return surrogate for the {@link TypeDescriptor} in the teleVM for this class in the tele VM.
     */
    public TeleTypeDescriptor getTeleTypeDescriptor() {
        initialize();
        return _teleTypeDescriptor;
    }

    @Override
    protected Object createDeepCopy(DeepCopyContext context) {
        // Translate into local equivalent
        return classActor();
    }

    public TeleClass getTeleClass() {
        final Reference reference = teleVM().fields().ClassActor_mirror.readReference(reference());
        if (reference.isZero()) {
            // TODO: assert that this class is the object class!
            return null;
        }
        return (TeleClass) TeleObject.make(teleVM(), reference);
    }

    /**
     * @return surrogate for the static tuple of the {@link ClassActor} in the target VM.
     */
    public TeleStaticTuple getTeleStaticTuple() {
        final Reference staticTupleReference = teleVM().fields().ClassActor_staticTuple.readReference(reference());
        return (TeleStaticTuple) TeleObject.make(teleVM(), staticTupleReference);
    }

    /**
     * @return surrogate for the {@link ClassActor} in the tele VM for the type of the components of
     * this array type, null if not an array type
     */
    public TeleClassActor getTeleComponentClassActor() {
        final Reference componentClassActorReference = teleVM().fields().ClassActor_componentClassActor.readReference(reference());
        return (TeleClassActor) TeleObject.make(teleVM(), componentClassActorReference);
    }

    /**
     * @return Reference to the constants in the target VM's constant pool for the class, null if an array type
     */
    public TeleConstantPool getTeleConstantPool() {
        if (classActor().isTupleClassActor()) {
            return (TeleConstantPool) TeleObject.make(teleVM(), teleVM().fields().TupleClassActor_constantPool.readReference(reference()));
        } else if (classActor().isHybridClassActor()) {
            return (TeleConstantPool) TeleObject.make(teleVM(), teleVM().fields().HybridClassActor_constantPool.readReference(reference()));
        }
        return null;
    }

    private AppendableSequence<TeleFieldActor> readTeleStaticFieldActors() {
        final Reference teleStaticFieldActorsArrayReference = teleVM().fields().ClassActor_localStaticFieldActors.readReference(reference());
        final TeleArrayObject teleStaticFieldActorsArray = (TeleArrayObject) TeleObject.make(teleVM(), teleStaticFieldActorsArrayReference);
        final AppendableSequence<TeleFieldActor> localTeleStaticFieldActors = new LinkSequence<TeleFieldActor>();
        for (int index = 0; index < teleStaticFieldActorsArray.getLength(); index++) {
            final Reference instanceFieldActorReference = teleStaticFieldActorsArray.readElementValue(index).asReference();
            final TeleFieldActor teleStaticFieldActor = (TeleFieldActor) TeleObject.make(teleVM(), instanceFieldActorReference);
            localTeleStaticFieldActors.append(teleStaticFieldActor);
        }
        return localTeleStaticFieldActors;
    }

    /**
     * @return surrogates for the local (not inherited) static {@link FieldActor}s associated with the class in the tele VM.
     */
    public Sequence<TeleFieldActor> getTeleStaticFieldActors() {
        return readTeleStaticFieldActors();
    }

    private AppendableSequence<TeleFieldActor> readTeleInstanceFieldActors() {
        final Reference teleInstanceFieldActorsArrayReference = teleVM().fields().ClassActor_localInstanceFieldActors.readReference(reference());
        final TeleArrayObject teleInstanceFieldActorsArray = (TeleArrayObject) TeleObject.make(teleVM(), teleInstanceFieldActorsArrayReference);
        final AppendableSequence<TeleFieldActor> localTeleInstanceFieldActors = new LinkSequence<TeleFieldActor>();
        for (int index = 0; index < teleInstanceFieldActorsArray.getLength(); index++) {
            final Reference instanceFieldActorReference = teleInstanceFieldActorsArray.readElementValue(index).asReference();
            final TeleFieldActor teleInstanceFieldActor = (TeleFieldActor) TeleObject.make(teleVM(), instanceFieldActorReference);
            localTeleInstanceFieldActors.append(teleInstanceFieldActor);
        }
        return localTeleInstanceFieldActors;
    }

    /**
     * @return surrogates for the local (not inherited) instance {@link FieldActor}s associated with the class in the tele VM.
     */
    public Sequence<TeleFieldActor> getTeleInstanceFieldActors() {
        return readTeleInstanceFieldActors();
    }

    /**
     * @return surrogates for the local (not inherited) {@link FieldActor}s associated with the class in the tele VM: both static and instance fields.
     */
    public Sequence<TeleFieldActor> getTeleFieldActors() {
        final AppendableSequence<TeleFieldActor> teleFieldActors = new LinkSequence<TeleFieldActor>();
        AppendableSequence.Static.appendAll(teleFieldActors, readTeleStaticFieldActors());
        AppendableSequence.Static.appendAll(teleFieldActors, readTeleInstanceFieldActors());
        return teleFieldActors;
    }

    private AppendableSequence<TeleInterfaceMethodActor> readTeleInterfaceMethodActors() {
        final Reference teleInterfaceMethodActorsArrayReference = teleVM().fields().ClassActor_localInterfaceMethodActors.readReference(reference());
        final TeleArrayObject teleInterfaceMethodActorsArray = (TeleArrayObject) TeleObject.make(teleVM(), teleInterfaceMethodActorsArrayReference);
        final AppendableSequence<TeleInterfaceMethodActor> localTeleInterfaceMethodActors = new LinkSequence<TeleInterfaceMethodActor>();
        for (int index = 0; index < teleInterfaceMethodActorsArray.getLength(); index++) {
            final Reference interfaceMethodActorReference = teleInterfaceMethodActorsArray.readElementValue(index).asReference();
            final TeleInterfaceMethodActor teleInterfaceMethodActor = (TeleInterfaceMethodActor) TeleObject.make(teleVM(), interfaceMethodActorReference);
            localTeleInterfaceMethodActors.append(teleInterfaceMethodActor);
        }
        return localTeleInterfaceMethodActors;
    }

    /**
     * @return surrogates for the local (not inherited) {@link InterfaceMethodActor}s associated with the class in the tele VM.
     */
    public Sequence<TeleInterfaceMethodActor> getTeleInterfaceMethodActors() {
        return readTeleInterfaceMethodActors();
    }

    private AppendableSequence<TeleStaticMethodActor> readTeleStaticMethodActors() {
        final Reference teleStaticMethodActorsArrayReference = teleVM().fields().ClassActor_localStaticMethodActors.readReference(reference());
        final TeleArrayObject teleStaticMethodActorsArray = (TeleArrayObject) TeleObject.make(teleVM(), teleStaticMethodActorsArrayReference);
        final AppendableSequence<TeleStaticMethodActor> localTeleStaticMethodActors = new LinkSequence<TeleStaticMethodActor>();
        for (int index = 0; index < teleStaticMethodActorsArray.getLength(); index++) {
            final Reference staticMethodActorReference = teleStaticMethodActorsArray.readElementValue(index).asReference();
            final TeleStaticMethodActor teleStaticMethodActor = (TeleStaticMethodActor) TeleObject.make(teleVM(), staticMethodActorReference);
            localTeleStaticMethodActors.append(teleStaticMethodActor);
        }
        return localTeleStaticMethodActors;
    }

    /**
     * @return surrogates for the local (not inherited) {@link StaticMethodActor}s associated with the class in the tele VM.
     */
    public Sequence<TeleStaticMethodActor> getTeleStaticMethodActors() {
        return readTeleStaticMethodActors();
    }

    private AppendableSequence<TeleVirtualMethodActor> readTeleVirtualMethodActors() {
        final Reference teleVirtualMethodActorsArrayReference = teleVM().fields().ClassActor_localVirtualMethodActors.readReference(reference());
        final TeleArrayObject teleArray = (TeleArrayObject) TeleObject.make(teleVM(), teleVirtualMethodActorsArrayReference);
        final AppendableSequence<TeleVirtualMethodActor> localTeleVirtualMethodActors = new LinkSequence<TeleVirtualMethodActor>();
        for (int index = 0; index < teleArray.getLength(); index++) {
            final Reference staticMethodActorReference = teleArray.readElementValue(index).asReference();
            final TeleVirtualMethodActor teleVirtualMethodActor = (TeleVirtualMethodActor) TeleObject.make(teleVM(), staticMethodActorReference);
            localTeleVirtualMethodActors.append(teleVirtualMethodActor);
        }
        return localTeleVirtualMethodActors;
    }

    /**
     * @return surrogates for the local (not inherited) {@link VirtualMethodActor}s associated with the class in the tele VM.
     */
    public Sequence<TeleVirtualMethodActor> getTeleVirtualMethodActors() {
        return readTeleVirtualMethodActors();
    }

    /**
     * @return surrogates for the local (not inherited) {@link ClassMethodActor}s associated with the class in the tele VM: both static and virtual methods.
     */
    public Sequence<TeleClassMethodActor> getTeleClassMethodActors() {
        final AppendableSequence<TeleClassMethodActor> teleClassMethodActors = new LinkSequence<TeleClassMethodActor>();
        AppendableSequence.Static.appendAll(teleClassMethodActors, readTeleStaticMethodActors());
        AppendableSequence.Static.appendAll(teleClassMethodActors, readTeleVirtualMethodActors());
        return teleClassMethodActors;
    }

    /**
     * @return surrogates for all of the the local (not inherited) {@link MethodActor}s associated with the class in the tele VM:  static, virtual, and interface methods.
     */
    public Sequence<TeleMethodActor> getTeleMethodActors() {
        final AppendableSequence<TeleMethodActor> teleMethodActors = new LinkSequence<TeleMethodActor>();
        AppendableSequence.Static.appendAll(teleMethodActors, readTeleStaticMethodActors());
        AppendableSequence.Static.appendAll(teleMethodActors, readTeleVirtualMethodActors());
        AppendableSequence.Static.appendAll(teleMethodActors, readTeleInterfaceMethodActors());
        return teleMethodActors;
    }

    @Override
    public String maxineTerseRole() {
        return "ClassActor";
    }

    @Override
    public ClassLoaderProvider classLoader() {
        return getTeleClassLoader();
    }

    @Override
    public ClassObjectProvider classObject() {
        return getTeleClass();
    }

    private abstract class FakeFieldProvider implements FieldProvider {

        private static final String PREFIX = "X_";

        private final String _name;
        private final VMValue.Type _type;
        private final String _signature;

        public FakeFieldProvider(String name, VMValue.Type type, String signature) {
            _name = PREFIX + name;
            _type = type;
            _signature = signature;
        }

        @Override
        public ReferenceTypeProvider getReferenceTypeHolder() {
            return TeleClassActor.this;
        }

        @Override
        public VMValue getStaticValue() {
            assert false : "This field is not a static field!";
            return null;
        }

        @Override
        public void setStaticValue(VMValue value) {
            assert false : "This field is not a static field!";
        }

        @Override
        public void setValue(ObjectProvider object, VMValue value) {
            assert false : "This field is readonly!";
        }

        @Override
        public int getFlags() {
            return FieldActor.ACC_FINAL;
        }

        @Override
        public String getGenericSignature() {
            return getSignature();
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return _name;
        }

        @Override
        public String getSignature() {
            return _signature;
        }

        @Override
        public Type getType() {
            return _type;
        }
    }

    private final FieldProvider _fakeAddressField = new FakeFieldProvider("address", VMValue.Type.LONG, JavaTypeDescriptor.LONG.toString()) {

        @Override
        public VMValue getValue(ObjectProvider object) {
            return teleVM().createLongValue(((TeleObject) object).getReference().toOrigin().asAddress().toLong());
        }
    };

    private final FieldProvider _fakeHubField = new FakeFieldProvider("hub", VMValue.Type.PROVIDER, "Lcom/sun/max/vm/actor/holder/Hub;") {
        @Override
        public VMValue getValue(ObjectProvider object) {
            return teleVM().convertToVirtualMachineValue(TeleReferenceValue.from(teleVM(), ((TeleObject) object).getTeleHub().getReference()));
        }
    };

    private final FieldProvider _fakeMiscField = new FakeFieldProvider("misc", VMValue.Type.LONG, JavaTypeDescriptor.LONG.toString()) {

        @Override
        public VMValue getValue(ObjectProvider object) {
            return teleVM().createLongValue(((TeleObject) object).getMiscWord().asAddress().toLong());
        }
    };

    @Override
    public FieldProvider[] getFields() {
        final List<TeleFieldActor> list = Sequence.Static.toList(this.getTeleFieldActors());
        final List<FieldProvider> result = new ArrayList<FieldProvider>();
        result.addAll(list);
        if (this.classActor().superClassActor() == null) {
            result.add(0, _fakeAddressField);
            result.add(0, _fakeMiscField);
            result.add(0, _fakeHubField);
        }
        final FieldProvider[] resultArray = new FieldProvider[list.size()];
        return result.toArray(resultArray);
    }

    @Override
    public InterfaceProvider[] getImplementedInterfaces() {
        final IdentityHashSet<InterfaceActor> interfaces = classActor().getAllInterfaceActors();
        final AppendableSequence<InterfaceProvider> result = new LinkSequence<InterfaceProvider>();
        for (InterfaceActor interfaceActor : interfaces) {
            final InterfaceProvider interfaceProvider = teleVM().teleClassRegistry().findTeleInterfaceActor(interfaceActor);
            if (interfaceProvider != this) {
                result.append(interfaceProvider);
            }
        }
        return Sequence.Static.toArray(result, InterfaceProvider.class);
    }

    @Override
    public MethodProvider[] getMethods() {
        return Sequence.Static.toArray(this.getTeleMethodActors(), MethodProvider.class);
    }

    @Override
    public ReferenceTypeProvider[] getNestedTypes() {
        final ClassActor[] actors = classActor().innerClassActors();
        final ReferenceTypeProvider[] result = new ReferenceTypeProvider[actors.length];
        for (int i = 0; i < actors.length; i++) {
            result[i] = teleVM().teleClassRegistry().findTeleClassActor(actors[i]);
        }
        return result;
    }

    @Override
    public ObjectProvider[] getInstances() {
        // TODO: Implement this correctly.
        return null;
    }

    public String getName() {
        return classActor().name().toString();
    }

    public String getSignature() {
        return classActor().typeDescriptor().toString();
    }

    public String getSignatureWithGeneric() {
        return classActor().genericSignatureString();
    }

    public int getStatus() {

        // TODO: Correct implementation.

        int status = 0;

        if (classActor().isInitialized()) {
            status |= ClassStatus.INITIALIZED;
        }

        if (classActor().isInitialized()) {
            status |= ClassStatus.PREPARED;
        }

        if (classActor().isInitialized()) {
            status |= ClassStatus.VERIFIED;
        }

        return status;
    }

    public String getSourceFileName() {
        return classActor().sourceFileName();
    }

    public int getFlags() {
        return classActor().flags() & Actor.JAVA_CLASS_FLAGS;
    }

    public Type getType() {
        return TeleVM.toVirtualMachineType(classActor().kind());
    }

    public int majorVersion() {
        return classActor().majorVersion();
    }

    public int minorVersion() {
        return classActor().minorVersion();
    }
}
