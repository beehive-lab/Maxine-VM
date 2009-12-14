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
 * Canonical surrogate for a  {@link ClassActor} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleClassActor extends TeleActor implements ReferenceTypeProvider {

    protected TeleClassActor(TeleVM teleVM, Reference classActorReference) {
        super(teleVM, classActorReference);
    }

    private ClassActor classActor;

    /**
     * @return Local {@link ClassActor} corresponding the {@link ClassActor} in the {@link TeleVM}.
     */
    public ClassActor classActor() {
        if (classActor == null) {
            // Requires loading the class; delay until needed.
            classActor = teleVM().makeClassActor(reference());
        }
        return classActor;
    }

    private boolean initialized = false;

    // Fields are final; cache them.
    private TeleClassLoader teleClassLoader;
    private int id;
    private TeleTypeDescriptor teleTypeDescriptor;

    private void initialize() {
        if (!initialized) {

            final Reference classLoaderReference = teleVM().teleFields().ClassActor_classLoader.readReference(reference());
            teleClassLoader = (TeleClassLoader) teleVM().makeTeleObject(classLoaderReference);

            id = teleVM().teleFields().ClassActor_id.readInt(reference());

            final Reference typeDescriptorReference = teleVM().teleFields().ClassActor_typeDescriptor.readReference(reference());
            teleTypeDescriptor = (TeleTypeDescriptor) teleVM().makeTeleObject(typeDescriptorReference);

            initialized = true;
        }
    }

    /**
     * @return surrogate for the {@link ClassLoader} in the {@link TeleVM} for this class in the VM.
     * There is no local counterpart for these; all local surrogate objects are created by one
     * ClassLoader.
     */
    public TeleClassLoader getTeleClassLoader() {
        initialize();
        return teleClassLoader;
    }

    /**
     * @return the serial ID of the {@link ClassActor} in the {@link TeleVM}.
     */
    public int getId() {
        initialize();
        return id;
    }

    /**
     * @return surrogate for the {@link TypeDescriptor} in the{@link TeleVM} for this class in the VM.
     */
    public TeleTypeDescriptor getTeleTypeDescriptor() {
        initialize();
        return teleTypeDescriptor;
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return classActor();
    }

    public TeleClass getTeleClass() {
        final Reference reference = teleVM().teleFields().ClassActor_mirror.readReference(reference());
        if (reference.isZero()) {
            // TODO: assert that this class is the object class!
            return null;
        }
        return (TeleClass) teleVM().makeTeleObject(reference);
    }

    /**
     * @return surrogate for the static tuple of the {@link ClassActor} in the {@link TeleVM}.
     */
    public TeleStaticTuple getTeleStaticTuple() {
        final Reference staticTupleReference = teleVM().teleFields().ClassActor_staticTuple.readReference(reference());
        return (TeleStaticTuple) teleVM().makeTeleObject(staticTupleReference);
    }

    /**
     * @return surrogate for the {@link ClassActor} in the {@link TeleVM} for the type of the components of
     * this array type, null if not an array type
     */
    public TeleClassActor getTeleComponentClassActor() {
        final Reference componentClassActorReference = teleVM().teleFields().ClassActor_componentClassActor.readReference(reference());
        return (TeleClassActor) teleVM().makeTeleObject(componentClassActorReference);
    }

    /**
     * @return Reference to the constants in the {@link TeleVM}'s constant pool for the class, null if an array type
     */
    public TeleConstantPool getTeleConstantPool() {
        if (classActor().isTupleClassActor()) {
            return (TeleConstantPool) teleVM().makeTeleObject(teleVM().teleFields().TupleClassActor_constantPool.readReference(reference()));
        } else if (classActor().isHybridClassActor()) {
            return (TeleConstantPool) teleVM().makeTeleObject(teleVM().teleFields().HybridClassActor_constantPool.readReference(reference()));
        }
        return null;
    }

    private AppendableSequence<TeleFieldActor> readTeleStaticFieldActors() {
        final Reference teleStaticFieldActorsArrayReference = teleVM().teleFields().ClassActor_localStaticFieldActors.readReference(reference());
        final TeleArrayObject teleStaticFieldActorsArray = (TeleArrayObject) teleVM().makeTeleObject(teleStaticFieldActorsArrayReference);
        final AppendableSequence<TeleFieldActor> localTeleStaticFieldActors = new LinkSequence<TeleFieldActor>();
        for (int index = 0; index < teleStaticFieldActorsArray.getLength(); index++) {
            final Reference instanceFieldActorReference = teleStaticFieldActorsArray.readElementValue(index).asReference();
            final TeleFieldActor teleStaticFieldActor = (TeleFieldActor) teleVM().makeTeleObject(instanceFieldActorReference);
            localTeleStaticFieldActors.append(teleStaticFieldActor);
        }
        return localTeleStaticFieldActors;
    }

    /**
     * @return surrogates for the local (not inherited) static {@link FieldActor}s associated with the class in the {@link TeleVM}.
     */
    public Sequence<TeleFieldActor> getTeleStaticFieldActors() {
        return readTeleStaticFieldActors();
    }

    private AppendableSequence<TeleFieldActor> readTeleInstanceFieldActors() {
        final Reference teleInstanceFieldActorsArrayReference = teleVM().teleFields().ClassActor_localInstanceFieldActors.readReference(reference());
        final TeleArrayObject teleInstanceFieldActorsArray = (TeleArrayObject) teleVM().makeTeleObject(teleInstanceFieldActorsArrayReference);
        final AppendableSequence<TeleFieldActor> localTeleInstanceFieldActors = new LinkSequence<TeleFieldActor>();
        for (int index = 0; index < teleInstanceFieldActorsArray.getLength(); index++) {
            final Reference instanceFieldActorReference = teleInstanceFieldActorsArray.readElementValue(index).asReference();
            final TeleFieldActor teleInstanceFieldActor = (TeleFieldActor) teleVM().makeTeleObject(instanceFieldActorReference);
            localTeleInstanceFieldActors.append(teleInstanceFieldActor);
        }
        return localTeleInstanceFieldActors;
    }

    /**
     * @return surrogates for the local (not inherited) instance {@link FieldActor}s associated with the class in the {@link TeleVM}.
     */
    public Sequence<TeleFieldActor> getTeleInstanceFieldActors() {
        return readTeleInstanceFieldActors();
    }

    /**
     * @return surrogates for the local (not inherited) {@link FieldActor}s associated with the class in the {@link TeleVM}: both static and instance fields.
     */
    public Sequence<TeleFieldActor> getTeleFieldActors() {
        final AppendableSequence<TeleFieldActor> teleFieldActors = new LinkSequence<TeleFieldActor>();
        AppendableSequence.Static.appendAll(teleFieldActors, readTeleStaticFieldActors());
        AppendableSequence.Static.appendAll(teleFieldActors, readTeleInstanceFieldActors());
        return teleFieldActors;
    }

    private AppendableSequence<TeleInterfaceMethodActor> readTeleInterfaceMethodActors() {
        final Reference teleInterfaceMethodActorsArrayReference = teleVM().teleFields().ClassActor_localInterfaceMethodActors.readReference(reference());
        final TeleArrayObject teleInterfaceMethodActorsArray = (TeleArrayObject) teleVM().makeTeleObject(teleInterfaceMethodActorsArrayReference);
        final AppendableSequence<TeleInterfaceMethodActor> localTeleInterfaceMethodActors = new LinkSequence<TeleInterfaceMethodActor>();
        for (int index = 0; index < teleInterfaceMethodActorsArray.getLength(); index++) {
            final Reference interfaceMethodActorReference = teleInterfaceMethodActorsArray.readElementValue(index).asReference();
            final TeleInterfaceMethodActor teleInterfaceMethodActor = (TeleInterfaceMethodActor) teleVM().makeTeleObject(interfaceMethodActorReference);
            localTeleInterfaceMethodActors.append(teleInterfaceMethodActor);
        }
        return localTeleInterfaceMethodActors;
    }

    /**
     * @return surrogates for the local (not inherited) {@link InterfaceMethodActor}s associated with the class in the {@link TeleVM}.
     */
    public Sequence<TeleInterfaceMethodActor> getTeleInterfaceMethodActors() {
        return readTeleInterfaceMethodActors();
    }

    private AppendableSequence<TeleStaticMethodActor> readTeleStaticMethodActors() {
        final Reference teleStaticMethodActorsArrayReference = teleVM().teleFields().ClassActor_localStaticMethodActors.readReference(reference());
        final TeleArrayObject teleStaticMethodActorsArray = (TeleArrayObject) teleVM().makeTeleObject(teleStaticMethodActorsArrayReference);
        final AppendableSequence<TeleStaticMethodActor> localTeleStaticMethodActors = new LinkSequence<TeleStaticMethodActor>();
        for (int index = 0; index < teleStaticMethodActorsArray.getLength(); index++) {
            final Reference staticMethodActorReference = teleStaticMethodActorsArray.readElementValue(index).asReference();
            final TeleStaticMethodActor teleStaticMethodActor = (TeleStaticMethodActor) teleVM().makeTeleObject(staticMethodActorReference);
            localTeleStaticMethodActors.append(teleStaticMethodActor);
        }
        return localTeleStaticMethodActors;
    }

    /**
     * @return surrogates for the local (not inherited) {@link StaticMethodActor}s associated with the class in the {@link TeleVM}.
     */
    public Sequence<TeleStaticMethodActor> getTeleStaticMethodActors() {
        return readTeleStaticMethodActors();
    }

    private AppendableSequence<TeleVirtualMethodActor> readTeleVirtualMethodActors() {
        final Reference teleVirtualMethodActorsArrayReference = teleVM().teleFields().ClassActor_localVirtualMethodActors.readReference(reference());
        final TeleArrayObject teleArray = (TeleArrayObject) teleVM().makeTeleObject(teleVirtualMethodActorsArrayReference);
        final AppendableSequence<TeleVirtualMethodActor> localTeleVirtualMethodActors = new LinkSequence<TeleVirtualMethodActor>();
        for (int index = 0; index < teleArray.getLength(); index++) {
            final Reference staticMethodActorReference = teleArray.readElementValue(index).asReference();
            final TeleVirtualMethodActor teleVirtualMethodActor = (TeleVirtualMethodActor) teleVM().makeTeleObject(staticMethodActorReference);
            localTeleVirtualMethodActors.append(teleVirtualMethodActor);
        }
        return localTeleVirtualMethodActors;
    }

    /**
     * @return surrogates for the local (not inherited) {@link VirtualMethodActor}s associated with the class in the {@link TeleVM}.
     */
    public Sequence<TeleVirtualMethodActor> getTeleVirtualMethodActors() {
        return readTeleVirtualMethodActors();
    }

    /**
     * @return surrogates for the local (not inherited) {@link ClassMethodActor}s associated with the class in the {@link TeleVM}: both static and virtual methods.
     */
    public Sequence<TeleClassMethodActor> getTeleClassMethodActors() {
        final AppendableSequence<TeleClassMethodActor> teleClassMethodActors = new LinkSequence<TeleClassMethodActor>();
        AppendableSequence.Static.appendAll(teleClassMethodActors, readTeleStaticMethodActors());
        AppendableSequence.Static.appendAll(teleClassMethodActors, readTeleVirtualMethodActors());
        return teleClassMethodActors;
    }

    /**
     * @return surrogates for all of the the local (not inherited) {@link MethodActor}s associated with the class in the {@link TeleVM}:  static, virtual, and interface methods.
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

    public ClassLoaderProvider classLoader() {
        return getTeleClassLoader();
    }

    public ClassObjectProvider classObject() {
        return getTeleClass();
    }

    private abstract class FakeFieldProvider implements FieldProvider {

        private static final String PREFIX = "X_";

        private final String name;
        private final VMValue.Type type;
        private final String signature;

        public FakeFieldProvider(String name, VMValue.Type type, String signature) {
            this.name = PREFIX + name;
            this.type = type;
            this.signature = signature;
        }

        public ReferenceTypeProvider getReferenceTypeHolder() {
            return TeleClassActor.this;
        }

        public VMValue getStaticValue() {
            assert false : "This field is not a static field!";
            return null;
        }

        public void setStaticValue(VMValue value) {
            assert false : "This field is not a static field!";
        }

        public void setValue(ObjectProvider object, VMValue value) {
            assert false : "This field is readonly!";
        }

        public int getFlags() {
            return FieldActor.ACC_FINAL;
        }

        public String getGenericSignature() {
            return getSignature();
        }

        public String getName() {
            // TODO Auto-generated method stub
            return name;
        }

        public String getSignature() {
            return signature;
        }

        public Type getType() {
            return type;
        }
    }

    private final FieldProvider fakeAddressField = new FakeFieldProvider("address", VMValue.Type.LONG, JavaTypeDescriptor.LONG.toString()) {
        public VMValue getValue(ObjectProvider object) {
            return teleVM().vmAccess().createLongValue(((TeleObject) object).getReference().toOrigin().asAddress().toLong());
        }
    };

    private final FieldProvider fakeHubField = new FakeFieldProvider("hub", VMValue.Type.PROVIDER, "Lcom/sun/max/vm/actor/holder/Hub;") {
        public VMValue getValue(ObjectProvider object) {
            return teleVM().maxineValueToJDWPValue(TeleReferenceValue.from(teleVM(), ((TeleObject) object).getTeleHub().getReference()));
        }
    };

    private final FieldProvider fakeMiscField = new FakeFieldProvider("misc", VMValue.Type.LONG, JavaTypeDescriptor.LONG.toString()) {
        public VMValue getValue(ObjectProvider object) {
            return teleVM().vmAccess().createLongValue(((TeleObject) object).getMiscWord().asAddress().toLong());
        }
    };

    public FieldProvider[] getFields() {
        final List<TeleFieldActor> list = Sequence.Static.toList(this.getTeleFieldActors());
        final List<FieldProvider> result = new ArrayList<FieldProvider>();
        result.addAll(list);
        if (this.classActor().superClassActor == null) {
            result.add(0, fakeAddressField);
            result.add(0, fakeMiscField);
            result.add(0, fakeHubField);
        }
        final FieldProvider[] resultArray = new FieldProvider[list.size()];
        return result.toArray(resultArray);
    }

    public InterfaceProvider[] getImplementedInterfaces() {
        final IdentityHashSet<InterfaceActor> interfaces = classActor().getAllInterfaceActors();
        final AppendableSequence<InterfaceProvider> result = new LinkSequence<InterfaceProvider>();
        for (InterfaceActor interfaceActor : interfaces) {
            final InterfaceProvider interfaceProvider = (TeleInterfaceActor) teleVM().findTeleClassActor(interfaceActor.typeDescriptor);
            if (interfaceProvider != this) {
                result.append(interfaceProvider);
            }
        }
        return Sequence.Static.toArray(result, InterfaceProvider.class);
    }

    public MethodProvider[] getMethods() {
        return Sequence.Static.toArray(this.getTeleMethodActors(), MethodProvider.class);
    }

    public ReferenceTypeProvider[] getNestedTypes() {
        final ClassActor[] actors = classActor().innerClassActors();
        final ReferenceTypeProvider[] result = new ReferenceTypeProvider[actors.length];
        for (int i = 0; i < actors.length; i++) {
            result[i] = teleVM().findTeleClassActor(actors[i].typeDescriptor);
        }
        return result;
    }

    public ObjectProvider[] getInstances() {
        // TODO: Implement this correctly.
        return null;
    }

    public String getName() {
        return classActor().name.toString();
    }

    public String getSignature() {
        return classActor().typeDescriptor.toString();
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
        return classActor().sourceFileName;
    }

    public int getFlags() {
        return classActor().flags() & Actor.JAVA_CLASS_FLAGS;
    }

    public Type getType() {
        return TeleVM.maxineKindToJDWPType(classActor().kind);
    }

    public int majorVersion() {
        return classActor().majorVersion;
    }

    public int minorVersion() {
        return classActor().minorVersion;
    }
}
