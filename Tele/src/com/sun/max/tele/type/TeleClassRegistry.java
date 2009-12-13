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

import java.util.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A registry of classes known to be loaded in the {@link TeleVM}.
 * The registry is initialized with all classes pre-loaded into the boot image.
 * Dynamically loaded classes are discovered and added by
 * inspection during each {@link #refresh()}.
 *
 * The registry is constructed using information obtained by low level
 * data reading, avoiding the overhead of creating a {@link TeleClassActor},
 * which overhead includes loading the class.
 *
 * @author Bernd Mathiske
 * @author Athul Acharya
 * @author Michael Van De Vanter
 */
public class TeleClassRegistry extends AbstractTeleVMHolder {

    private static final int TRACE_VALUE = 2;

    // TODO (mlvdv)  Generalize to map either  (TypeDescriptor, ClassLoader) -> ClassActor Reference *or*  TypeDescriptor -> ClassActor Reference*
    private final Map<TypeDescriptor, Reference> typeDescriptorToClassActorReference = new HashMap<TypeDescriptor, Reference>();

    // ClassID of a {@link ClassActor} in the {@link TeleVM} -> reference to the ClassActor
    private final Map<Integer, Reference> idToClassActorReference = new HashMap<Integer, Reference>();

    /**
     * Adds an entry to the registry.
     *
     * @param classActorReference a {@link ClassActor} for a class loaded in the {@link TeleVM}
     * @throws ClassFormatError
     */
    private void addToRegistry(final Reference classActorReference) throws ClassFormatError {
        final int id = teleVM().teleFields().ClassActor_id.readInt(classActorReference);
        idToClassActorReference.put(id, classActorReference);
        final Reference typeDescriptorReference = teleVM().teleFields().ClassActor_typeDescriptor.readReference(classActorReference);
        final Reference stringReference = teleVM().teleFields().Descriptor_string.readReference(typeDescriptorReference);
        final String typeDescriptorString = teleVM().getString(stringReference);
        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString);
        typeDescriptorToClassActorReference.put(typeDescriptor, classActorReference);
        Trace.line(TRACE_VALUE, tracePrefix() + ": adding class (" + id + ", " + typeDescriptor.toJavaString() + ")");
    }

    /**
     * The number of classes loaded in the boot image for the {@link TeleVM}.
     */
    private final int preLoadedClassCount;

    /**
     * The number of classes dynamically loaded in the {@link TeleVM} that are known so far.
     */
    private int dynamicallyLoadedClassCount = 0;

    /**
     * Create a registry that contains summary information about all classes known to have been
     * loaded into the {@link TeleVM}, initialized at registry creation with classes pre-loaded
     * into the boot image and supplemented with dynamically loaded classes with each
     * call to {@link #refresh()}.
     * @param teleVM
     */
    public TeleClassRegistry(TeleVM teleVM) {
        super(teleVM);
        Trace.begin(1, tracePrefix() + " initializing");
        final long startTimeMillis = System.currentTimeMillis();
        int count = 0;
        try {
            final Reference classRegistryReference = teleVM().bootClassRegistryReference();

            if (teleVM().getInterpreterUseLevel() > 0) {
                final TeleReferenceValue classRegistryReferenceValue = TeleReferenceValue.from(teleVM(), classRegistryReference);
                final int length = TeleInterpreter.execute(teleVM, ClassRegistry.class, "numberOfClassActors", SignatureDescriptor.fromJava(int.class),
                                classRegistryReferenceValue).asInt();

                for (int i = 0; i < length; i++) {
                    final Reference classActorReference = TeleInterpreter.execute(teleVM, ClassRegistry.class, "getClassActorByIndex",
                                    SignatureDescriptor.fromJava(ClassActor.class, int.class),
                                    classRegistryReferenceValue, IntValue.from(i)).asReference();
                    final String typeDescriptorString = (String) TeleInterpreter.execute(teleVM, ClassRegistry.class, "getTypeDescriptorStringByIndex",
                                    SignatureDescriptor.fromJava(String.class, int.class),
                                    classRegistryReferenceValue, IntValue.from(i)).unboxObject();
                    final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString);
                    final int id = TeleInterpreter.execute(teleVM, ClassRegistry.class, "getClassActorSerialByIndex",
                                    SignatureDescriptor.fromJava(int.class, int.class),
                                    classRegistryReferenceValue, IntValue.from(i)).asInt();

                    idToClassActorReference.put(id, classActorReference);
                    typeDescriptorToClassActorReference.put(typeDescriptor, classActorReference);
                    count++;
                }
            } else {
                final Reference typeDescriptorToClassActorReference = teleVM().teleFields().ClassRegistry_typeDescriptorToClassActor.readReference(classRegistryReference);
                final Reference tableReference = teleVM().teleFields().ChainedHashMapping_table.readReference(typeDescriptorToClassActorReference);
                final int length = teleVM().layoutScheme().arrayHeaderLayout.readLength(tableReference);
                for (int i = 0; i < length; i++) {
                    Reference entryReference = teleVM().readReference(tableReference, i);
                    while (!entryReference.isZero()) {
                        final Reference classActorReference = teleVM().teleFields().ChainedHashMapping$DefaultEntry_value.readReference(entryReference);
                        addToRegistry(classActorReference);
                        count++;
                        entryReference = teleVM().teleFields().ChainedHashMapping$DefaultEntry_next.readReference(entryReference);
                    }
                }
            }
            ClassID.setMapping(classIDMapping);
        } catch (Throwable throwable) {
            ProgramError.unexpected("could not build inspector type registry", throwable);
        }
        preLoadedClassCount = count;
        Trace.end(1, tracePrefix() + " initializing (" + preLoadedClassCount + " pre-loaded entries)", startTimeMillis);
    }

    /**
     * Adds information to the registry about any newly loaded classes in the {@link TeleVM}.
     */
    public void refresh(long processEpoch) {
        Trace.begin(TRACE_VALUE, tracePrefix() + "refreshing");
        final long startTimeMillis = System.currentTimeMillis();
        final Reference teleClassInfoStaticTupleReference = teleVM().teleFields().InspectableClassInfo_classActorCount.staticTupleReference(teleVM());
        final Pointer loadedClassCountPointer = teleClassInfoStaticTupleReference.toOrigin().plus(teleVM().teleFields().InspectableClassInfo_classActorCount.fieldActor().offset());
        final int remoteLoadedClassCount = teleVM().dataAccess().readInt(loadedClassCountPointer);
        final Pointer loadedClassActorsPointer = teleClassInfoStaticTupleReference.toOrigin().plus(teleVM().teleFields().InspectableClassInfo_classActors.fieldActor().offset());
        final Reference loadedClassActorsArrayReference = teleVM().wordToReference(teleVM().dataAccess().readWord(loadedClassActorsPointer));
        int index = dynamicallyLoadedClassCount;
        while (index < remoteLoadedClassCount) {
            final Reference classActorReference = teleVM().getElementValue(Kind.REFERENCE, loadedClassActorsArrayReference, index).asReference();
            try {
                addToRegistry(classActorReference);
            } catch (InvalidReferenceException e) {
                e.printStackTrace();
            }
            index++;
        }
        dynamicallyLoadedClassCount = remoteLoadedClassCount;
        Trace.end(TRACE_VALUE, tracePrefix() + "refreshing:  static=" + preLoadedClassCount + ", dynamic=" + remoteLoadedClassCount + ", new=" + (remoteLoadedClassCount - dynamicallyLoadedClassCount), startTimeMillis);
    }

    /**
     * @param id  Class ID of a {@link ClassActor} in the {@link TeleVM}.
     * @return surrogate for the {@link ClassActor} in the {@link TeleVM}, null if not known.
     */
    public TeleClassActor findTeleClassActorByID(int id) {
        final Reference classActorReference = idToClassActorReference.get(id);
        if (classActorReference != null && !classActorReference.isZero()) {
            return (TeleClassActor) teleVM().makeTeleObject(classActorReference);
        }
        return null;
    }

    /**
     * @param typeDescriptor A local {@link TypeDescriptor}.
     * @return surrogate for the equivalent {@link ClassActor} in the {@link TeleVM}, null if not known.
     */
    public TeleClassActor findTeleClassActorByType(TypeDescriptor typeDescriptor) {
        final Reference classActorReference = typeDescriptorToClassActorReference.get(typeDescriptor);
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) teleVM().makeTeleObject(classActorReference);
    }

    /**
     * @param javaClass   A local {@link Class} object.
     * @return surrogate for the equivalent {@link ClassActor} in the {@link TeleVM}, null if not known.
     */
    public TeleClassActor findTeleClassActorByClass(Class javaClass) {
        final Reference classActorReference = typeDescriptorToClassActorReference.get(JavaTypeDescriptor.forJavaClass(javaClass));
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) teleVM().makeTeleObject(classActorReference);
    }

    /**
     * @return  {@link TypeDescriptor}s for all classes loaded in the {@link TeleVM}.
     */
    public Set<TypeDescriptor> typeDescriptors() {
        return Collections.unmodifiableSet(typeDescriptorToClassActorReference.keySet());
    }

    /**
     * @return surrogates for all {@link ClassActor}s loaded in the {@link TeleVM}.
     */
    public ReferenceTypeProvider[] teleClassActors() {
        final ReferenceTypeProvider[] result = new ReferenceTypeProvider[idToClassActorReference.size()];
        int index = 0;
        for (Reference classActorReference : idToClassActorReference.values()) {
            result[index++] = (TeleClassActor) teleVM().makeTeleObject(classActorReference);
        }
        return result;
    }

    /**
     * ClassID Mapping.
     */
    private final Map<Integer, ClassActor> idToClassActor = new HashMap<Integer, ClassActor>();

    /**
     * @param id Target id of a {@link ClassActor} in the target VM.
     * @return  Local {@link ClassActor} equivalent to the one in the target VM, null if not known.
     */
    private ClassActor findClassActorByID(int id) {
        ClassActor classActor = idToClassActor.get(id);
        if (classActor == null) {
            final Reference classActorReference = idToClassActorReference.get(id);
            if (classActorReference != null) {
                classActor = teleVM().makeClassActor(classActorReference);
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
