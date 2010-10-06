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
package com.sun.max.tele.type;

import static com.sun.max.vm.VMConfiguration.*;

import java.util.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A registry of classes known to be loaded in the VM.
 * The registry is initialized with all classes pre-loaded into the boot image.
 * Dynamically loaded classes are discovered and added by
 * inspection during each {@link #updateCache()}.
 *
 * The registry is constructed using information obtained by low level
 * data reading, avoiding the overhead of creating a {@link TeleClassActor},
 * which overhead includes loading the class.
 *
 * @author Bernd Mathiske
 * @author Athul Acharya
 * @author Michael Van De Vanter
 */
public class TeleClassRegistry extends AbstractTeleVMHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private final TimedTrace updateTracer;

    // TODO (mlvdv)  Generalize to map either  (TypeDescriptor, ClassLoader) -> ClassActor Reference *or*  TypeDescriptor -> ClassActor Reference*
    private final Map<TypeDescriptor, Reference> typeDescriptorToClassActorReference = new HashMap<TypeDescriptor, Reference>();

    // ClassID of a {@link ClassActor} in the VM -> reference to the ClassActor
    private final Map<Integer, Reference> idToClassActorReference = new HashMap<Integer, Reference>();

    /**
     * ClassID Mapping.
     */
    private final Map<Integer, ClassActor> idToClassActor = new HashMap<Integer, ClassActor>();

    /**
     * The number of classes loaded in the VM that were discovered when the registry was created.
     */
    private final int initialClassCount;

    /**
     * The number of classes loaded in the VM, known so far, that have been loaded since the registry was created.
     */
    private int dynamicallyLoadedClassCount = 0;

    /**
     * A list of indices in the class registry table that need processing after the {@link TeleHeap} is fully initialized.
     */
    private List<Integer> attachFixupList = new ArrayList<Integer>();

    /**
     * Cache of of table reference to support processing the fix-up list.
     */
    private Reference tableReference;

    private final Object statsPrinter = new Object() {

        private int oldLoadedClassCount = 0;

        @Override
        public String toString() {
            final int newLoadedClassCount = initialClassCount + dynamicallyLoadedClassCount;
            final StringBuilder msg = new StringBuilder();
            msg.append(" #classes=(initial=").append(initialClassCount);
            msg.append(",dynamic=").append(dynamicallyLoadedClassCount);
            msg.append(",new=").append(newLoadedClassCount - oldLoadedClassCount);
            msg.append(")");
            oldLoadedClassCount = newLoadedClassCount;
            return msg.toString();
        }
    };

    /**
     * Create a registry that contains summary information about all classes known to have been
     * loaded into the VM, initialized at registry creation with classes pre-loaded
     * into the boot image and supplemented with dynamically loaded classes with each
     * call to {@link #updateCache()}.
     * @param vm
     */
    public TeleClassRegistry(TeleVM vm) {
        super(vm);
        assert vm().lockHeldByCurrentThread();
        final TimedTrace initTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        initTracer.begin();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");

        int count = 0;
        try {
            final Reference classRegistryReference = vm().bootClassRegistryReference();

            if (vm().getInterpreterUseLevel() > 0) {
                final TeleReferenceValue classRegistryReferenceValue = TeleReferenceValue.from(vm(), classRegistryReference);
                final int length = TeleInterpreter.execute(vm, ClassRegistry.class, "numberOfClassActors", SignatureDescriptor.fromJava(int.class),
                                classRegistryReferenceValue).asInt();

                for (int i = 0; i < length; i++) {
                    final Reference classActorReference = TeleInterpreter.execute(vm, ClassRegistry.class, "getClassActorByIndex",
                                    SignatureDescriptor.fromJava(ClassActor.class, int.class),
                                    classRegistryReferenceValue, IntValue.from(i)).asReference();
                    final String typeDescriptorString = (String) TeleInterpreter.execute(vm, ClassRegistry.class, "getTypeDescriptorStringByIndex",
                                    SignatureDescriptor.fromJava(String.class, int.class),
                                    classRegistryReferenceValue, IntValue.from(i)).unboxObject();
                    final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString);
                    final int id = TeleInterpreter.execute(vm, ClassRegistry.class, "getClassActorSerialByIndex",
                                    SignatureDescriptor.fromJava(int.class, int.class),
                                    classRegistryReferenceValue, IntValue.from(i)).asInt();

                    idToClassActorReference.put(id, classActorReference);
                    typeDescriptorToClassActorReference.put(typeDescriptor, classActorReference);
                    count++;
                }
            } else {
                final Reference typeDescriptorToClassActorReference = vm().teleFields().ClassRegistry_typeDescriptorToClassActor.readReference(classRegistryReference);
                tableReference = vm().teleFields().HashMap_table.readReference(typeDescriptorToClassActorReference);
                final int length = vmConfig().layoutScheme().arrayHeaderLayout.readLength(tableReference);
                for (int i = 0; i < length; i++) {
                    Reference entryReference = vm().readReference(tableReference, i);
                    while (!entryReference.isZero()) {
                        if (entryReference instanceof TemporaryTeleReference && TeleVM.isAttaching()) {
                            // this is likely to be a reference in the dynamic heap that we can't see because TeleHeap is not fully initialized yet.
                            // so we add it to a fix-up list and handle it later
                            attachFixupList.add(i);
                        } else {
                            final Reference classActorReference = vm().teleFields().HashMap$Entry_value.readReference(entryReference);
                            addToRegistry(classActorReference);
                            count++;
                        }
                        entryReference = vm().teleFields().HashMap$Entry_next.readReference(entryReference);
                    }
                }
            }
            ClassID.setMapping(classIDMapping);
        } catch (Throwable throwable) {
            ProgramError.unexpected("could not build inspector type registry", throwable);
        }
        initialClassCount = count;
        initTracer.end(statsPrinter);
    }

    public void updateCache() {
        // Adds information to the registry about any newly loaded classes in the VM.
        updateTracer.begin();
        assert vm().lockHeldByCurrentThread();
        final Reference teleClassInfoStaticTupleReference = vm().teleFields().InspectableClassInfo_classActorCount.staticTupleReference(vm());
        final Pointer loadedClassCountPointer = teleClassInfoStaticTupleReference.toOrigin().plus(vm().teleFields().InspectableClassInfo_classActorCount.fieldActor().offset());
        final int newLoadedClassCount = vm().dataAccess().readInt(loadedClassCountPointer);
        if (dynamicallyLoadedClassCount < newLoadedClassCount) {
            final Pointer loadedClassActorsPointer = teleClassInfoStaticTupleReference.toOrigin().plus(vm().teleFields().InspectableClassInfo_classActors.fieldActor().offset());
            final Reference loadedClassActorsArrayReference = vm().wordToReference(vm().dataAccess().readWord(loadedClassActorsPointer));
            while (dynamicallyLoadedClassCount < newLoadedClassCount) {
                final Reference classActorReference = vm().getElementValue(Kind.REFERENCE, loadedClassActorsArrayReference, dynamicallyLoadedClassCount).asReference();
                try {
                    addToRegistry(classActorReference);
                } catch (InvalidReferenceException e) {
                    e.printStackTrace();
                }
                dynamicallyLoadedClassCount++;
            }
        }
        updateTracer.end(statsPrinter);
    }


    public void processAttachFixupList() {
        final TimedTrace timedTrace = new TimedTrace(TRACE_VALUE, tracePrefix() + " adding entries from attach fixup list");
        timedTrace.begin();
        for (Integer i : attachFixupList) {
            Reference entryReference = vm().readReference(tableReference, i);
            final Reference classActorReference = vm().teleFields().HashMap$Entry_value.readReference(entryReference);
            addToRegistry(classActorReference);
        }
        timedTrace.end("added=" + attachFixupList.size());
    }

    /**
     * @param id  Class ID of a {@link ClassActor} in the VM.
     * @return surrogate for the {@link ClassActor} in the VM, null if not known.
     */
    public TeleClassActor findTeleClassActorByID(int id) {
        final Reference classActorReference = idToClassActorReference.get(id);
        if (classActorReference != null && !classActorReference.isZero()) {
            return (TeleClassActor) heap().makeTeleObject(classActorReference);
        }
        return null;
    }

    /**
     * @param typeDescriptor A local {@link TypeDescriptor}.
     * @return surrogate for the equivalent {@link ClassActor} in the VM, null if not known.
     */
    public TeleClassActor findTeleClassActorByType(TypeDescriptor typeDescriptor) {
        final Reference classActorReference = typeDescriptorToClassActorReference.get(typeDescriptor);
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) heap().makeTeleObject(classActorReference);
    }

    /**
     * @param javaClass   A local {@link Class} object.
     * @return surrogate for the equivalent {@link ClassActor} in the VM, null if not known.
     */
    public TeleClassActor findTeleClassActorByClass(Class javaClass) {
        final Reference classActorReference = typeDescriptorToClassActorReference.get(JavaTypeDescriptor.forJavaClass(javaClass));
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) heap().makeTeleObject(classActorReference);
    }

    /**
     * @return  {@link TypeDescriptor}s for all classes loaded in the VM.
     */
    public Set<TypeDescriptor> typeDescriptors() {
        return Collections.unmodifiableSet(typeDescriptorToClassActorReference.keySet());
    }

    /**
     * @return surrogates for all {@link ClassActor}s loaded in the VM.
     */
    public ReferenceTypeProvider[] teleClassActors() {
        final ReferenceTypeProvider[] result = new ReferenceTypeProvider[idToClassActorReference.size()];
        int index = 0;
        for (Reference classActorReference : idToClassActorReference.values()) {
            result[index++] = (TeleClassActor) heap().makeTeleObject(classActorReference);
        }
        return result;
    }

    /**
     * Adds an entry to the registry.
     *
     * @param classActorReference a {@link ClassActor} for a class loaded in the VM
     * @throws ClassFormatError
     */
    private void addToRegistry(final Reference classActorReference) throws ClassFormatError {
        final int id = vm().teleFields().ClassActor_id.readInt(classActorReference);
        idToClassActorReference.put(id, classActorReference);
        final Reference typeDescriptorReference = vm().teleFields().ClassActor_typeDescriptor.readReference(classActorReference);
        final Reference stringReference = vm().teleFields().Descriptor_string.readReference(typeDescriptorReference);
        final String typeDescriptorString = vm().getString(stringReference);
        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString);
        typeDescriptorToClassActorReference.put(typeDescriptor, classActorReference);
        Trace.line(TRACE_VALUE + 1, tracePrefix() + ": adding class (" + id + ", " + typeDescriptor.toJavaString() + ")");
    }

    /**
     * @param id Target id of a {@link ClassActor} in the target VM.
     * @return  Local {@link ClassActor} equivalent to the one in the target VM, null if not known.
     */
    private ClassActor findClassActorByID(int id) {
        ClassActor classActor = idToClassActor.get(id);
        if (classActor == null) {
            final Reference classActorReference = idToClassActorReference.get(id);
            if (classActorReference != null) {
                classActor = vm().makeClassActor(classActorReference);
                idToClassActor.put(id, classActor);
            }
        }
        return classActor;
    }

    private static ThreadLocal<Boolean> usingTeleClassIDs = new ThreadLocal<Boolean>() {
        @Override
        protected synchronized Boolean initialValue() {
            return false;
        }
    };

    private final ClassID.Mapping classIDMapping = new ClassID.Mapping() {
        public ClassActor idToClassActor(int id) {
            if (usingTeleClassIDs.get()) {
                return findClassActorByID(id);
            }
            return null;
        }
    };

    /**
     * While the runnable executes, class actor IDs refer to the tele VM, not the Inspector VM.
     * This can be employed to reuse code that manipulates MemberIDs and HolderIDs.
     * Such code would otherwise not work, because class actor IDs in the Inspector differ from those in the tele VM.
     */
    public static <Result_Type> Result_Type usingTeleClassIDs(Function<Result_Type> function) {
        final boolean oldValue = usingTeleClassIDs.get();
        usingTeleClassIDs.set(true);
        try {
            final Result_Type result = function.call();
            return result;
        } catch (Exception exception) {
            throw ProgramError.unexpected(exception);
        } finally {
            usingTeleClassIDs.set(oldValue);
        }
    }

}
