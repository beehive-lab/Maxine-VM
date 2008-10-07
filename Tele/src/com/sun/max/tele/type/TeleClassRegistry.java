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
/*VCSID=bcb748be-5d86-41d9-ae23-59ffb9e7955e*/
package com.sun.max.tele.type;

import java.util.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.value.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 * @author Athul Acharya
 * @author Michael Van De Vanter
 */
public class TeleClassRegistry extends TeleVMHolder {

    private static final Logger LOGGER = Logger.getLogger(TeleClassRegistry.class.getName());

    // TODO (mlvdv) rework this as a subclass of TeleObject

    private final Map<TypeDescriptor, Reference> _typeDescriptorToClassActorReference = new HashMap<TypeDescriptor, Reference>();

    private final Map<Integer, Reference> _idToClassActorReference = new HashMap<Integer, Reference>();

    /**
     * @param id  Class ID of a {@link ClassActor} in the target VM.
     * @return Surrogate for the {@link ClassActor} in the target VM, null if not known.
     */
    public TeleClassActor findTeleClassActorByID(int id) {
        final Reference classActorReference = _idToClassActorReference.get(id);
        if (classActorReference != null && !classActorReference.isZero()) {
            return (TeleClassActor) TeleObject.make(teleVM(), classActorReference);
        }
        return null;
    }

    private final Map<Integer, ClassActor> _idToClassActor = new HashMap<Integer, ClassActor>();

    /**
     * @param id Target id of a {@link ClassActor} in the target VM.
     * @return  Local {@link ClassActor} equivalent to the one in the target VM, null if not known.
     */
    private ClassActor findClassActorByID(int id) {
        ClassActor classActor = _idToClassActor.get(id);
        if (classActor == null) {
            final Reference classActorReference = _idToClassActorReference.get(id);
            if (classActorReference != null) {
                classActor = teleVM().makeClassActor(classActorReference);
                _idToClassActor.put(id, classActor);
            }
        }
        return classActor;
    }

    private static ThreadLocal<Boolean> _usingTeleClassIDs = new ThreadLocal<Boolean>() {
        @Override
        protected synchronized Boolean initialValue() {
            return false;
        }
    };

    private final ClassID.Mapping _classIDMapping = new ClassID.Mapping() {
        public ClassActor idToClassActor(int id) {
            if (_usingTeleClassIDs.get()) {
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
        final boolean oldValue = _usingTeleClassIDs.get();
        _usingTeleClassIDs.set(true);
        try {
            final Result_Type result = function.call();
            return result;
        } catch (Exception exception) {
            throw ProgramError.unexpected(exception);
        } finally {
            _usingTeleClassIDs.set(oldValue);
        }
    }

    public TeleClassRegistry(TeleVM teleVM) {
        super(teleVM);
        Trace.begin(1, "creating TeleClassRegistry");
        int count = 0;
        try {
            final Reference classRegistryReference = teleVM().bootClassRegistryReference();

            if (teleVM().interpreterUseLevel() > 0) {
                final TeleReferenceValue classRegistryReferenceValue = TeleReferenceValue.from(teleVM(), classRegistryReference);
                final int length = InspectorInterpreter.start(teleVM, ClassRegistry.class, "numberOfClassActors", SignatureDescriptor.fromJava(int.class),
                                                     classRegistryReferenceValue).asInt();

                for (int i = 0; i < length; i++) {
                    final Reference classActorReference = InspectorInterpreter.start(teleVM, ClassRegistry.class, "getClassActorByIndex",
                                                                            SignatureDescriptor.fromJava(ClassActor.class, int.class),
                                                                            classRegistryReferenceValue, IntValue.from(i)).asReference();
                    final String typeDescriptorString = (String) InspectorInterpreter.start(teleVM, ClassRegistry.class, "getTypeDescriptorStringByIndex",
                                                                                   SignatureDescriptor.fromJava(String.class, int.class),
                                                                                   classRegistryReferenceValue, IntValue.from(i)).unboxObject();
                    final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString);
                    final int id = InspectorInterpreter.start(teleVM, ClassRegistry.class, "getClassActorSerialByIndex",
                                                         SignatureDescriptor.fromJava(int.class, int.class),
                                                         classRegistryReferenceValue, IntValue.from(i)).asInt();

                    _idToClassActorReference.put(id, classActorReference);
                    _typeDescriptorToClassActorReference.put(typeDescriptor, classActorReference);
                    count++;
                }
            } else {
                final Reference typeDescriptorToClassActorReference = teleVM().fields().ClassRegistry_typeDescriptorToClassActor.readReference(classRegistryReference);
                final Reference tableReference = teleVM().fields().ChainedHashMapping_table.readReference(typeDescriptorToClassActorReference);
                final int length = teleVM().layoutScheme().arrayHeaderLayout().readLength(tableReference);
                for (int i = 0; i < length; i++) {
                    Reference entryReference = teleVM().getReference(tableReference, i);
                    while (!entryReference.isZero()) {
                        // Use low level field reading to defer the overhead of creating TeleObjects and loading additional classes.
                        final Reference classActorReference = teleVM().fields().ChainedHashMapping$DefaultEntry_value.readReference(entryReference);
                        final int id = teleVM().fields().ClassActor_id.readInt(classActorReference);
                        _idToClassActorReference.put(id, classActorReference);
                        final Reference typeDescriptorReference = teleVM().fields().ClassActor_typeDescriptor.readReference(classActorReference);
                        final Reference stringReference = teleVM().fields().Descriptor_string.readReference(typeDescriptorReference);
                        final String typeDescriptorString = teleVM().getString(stringReference);
                        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString);
                        _typeDescriptorToClassActorReference.put(typeDescriptor, classActorReference);
                        count++;
                        entryReference = teleVM().fields().ChainedHashMapping$DefaultEntry_next.readReference(entryReference);
                    }
                }
            }
            ClassID.setMapping(_classIDMapping);
        } catch (Throwable throwable) {
            throw new TeleError("could not build inspector type registry", throwable);
        }
        Trace.end(1, "creating TeleClassRegistry (" + count + " entries)");
    }

    /**
     * @param typeDescriptor A local {@link TypeDescriptor} object.
     * @return Surrogate for the equivalent {@link ClassActor} in the target VM.
     */
    public TeleClassActor findTeleClassActor(TypeDescriptor typeDescriptor) {
        final Reference classActorReference = _typeDescriptorToClassActorReference.get(typeDescriptor);
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) TeleObject.make(teleVM(), classActorReference);
    }

    /**
     * @param javaClass   A local {@link Class} object.
     * @return surrogate for the equivalent {@link ClassActor} in the target VM.
     */
    public TeleClassActor findTeleClassActor(Class javaClass) {
        final Reference classActorReference = _typeDescriptorToClassActorReference.get(JavaTypeDescriptor.forJavaClass(javaClass));
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) TeleObject.make(teleVM(), classActorReference);
    }

    /**
     * @return A local {@link TypeDescriptor} for each class known to the registry.
     */
    public Set<TypeDescriptor> typeDescriptors() {
        return Collections.unmodifiableSet(_typeDescriptorToClassActorReference.keySet());
    }

    public ReferenceTypeProvider findTeleClassActor(ClassActor classActor) {
        return findTeleClassActor(classActor.typeDescriptor());
    }

    public InterfaceProvider findTeleInterfaceActor(InterfaceActor interfaceActor) {
        return (TeleInterfaceActor) findTeleClassActor(interfaceActor.typeDescriptor());
    }

    public MethodProvider findTeleMethodActor(MethodActor methodActor) {
        final TeleClassActor teleClassActor = findTeleClassActor(methodActor.holder().typeDescriptor());
        if (teleClassActor != null) {
            for (TeleMethodActor teleMethodActor : teleClassActor.getTeleMethodActors()) {
                if (teleMethodActor.methodActor().equals(methodActor)) {
                    return teleMethodActor;
                }
            }
        }


        LOGGER.warning("Could not find tele method for method actor: " + methodActor.name().toString() + ", holder=" + methodActor.holder().name().toString());
        return null;
    }
}
