/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.type;

import java.io.*;
import java.text.*;
import java.util.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A registry that identifies all classes known to be loaded in the VM.
 * <p>
 * The registry identifies each loaded class with a {@link TeleReference} that points at the {@link ClassActor} for the
 * class in the VM. The registry does <em>not</em> created any instances of {@link TeleClassActor} for them, however, in
 * order to avoid unnecessary overhead.
 * <p>
 * At the start of an session the registry is initialized with all classes loaded into the boot image. Dynamically
 * loaded classes are discovered and added by inspection during each {@link #updateCache()}.
 * <p>
 * The registry is necessarily constructed using information obtained by low level data reading and knowledge of the VM
 * implementation, because the registry is itself required before type-based access can be supported.
 */
public final class TeleClassRegistry extends AbstractTeleVMHolder implements MaxClassRegistry, TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private static final String entityName = "Class Registry";

    private final String entityDescription;

    private final TimedTrace updateTracer;

    private long lastUpdateEpoch = -1L;

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
     * Classes, possibly not loaded, available on the classpath.
     * Lazily initialized; can re re-initialized.
     * @see #updateLoadableTypeDescriptorsFromClasspath()
     */
    private Set<TypeDescriptor> typesOnClasspath;

    /**
     * A set of ConcurrentHashMap.HashEntry values in the class registry table that need processing after the {@link TeleHeap} is fully initialized.
     */
    private List<Reference> attachFixupList = new ArrayList<Reference>();

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
     * call to {@link #updateCache(long)}.
     *
     * @param vm
     * @param epoch
     */
    public TeleClassRegistry(TeleVM vm, long epoch) {
        super(vm);
        assert vm().lockHeldByCurrentThread();
        final TimedTrace initTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " creating");
        initTracer.begin();
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + " updating");
        this.entityDescription = "Class loading and management for the " + vm().entityName();
        this.lastUpdateEpoch = epoch;
        int count = 0;
        try {
            final Reference classRegistry = vm.bootClassRegistryReference();
            TeleFields f = vm().teleFields();
            final Reference typeDescriptorToClassActor = f.ClassRegistry_typeDescriptorToClassActor.readReference(classRegistry);
            Reference segments = f.ConcurrentHashMap_segments.readReference(typeDescriptorToClassActor);
            int segmentsLength = Layout.readArrayLength(segments);
            for (int i = 0; i < segmentsLength; i++) {
                Reference segment = vm().readReference(segments, i);
                if (!segment.isZero()) {
                    Reference table = f.ConcurrentHashMap$Segment_table.readReference(segment);
                    if (!table.isZero()) {
                        int tableLength = Layout.readArrayLength(table);
                        for (int j = 0; j != tableLength; j++) {
                            Reference entry = vm().readReference(table, j);
                            while (!entry.isZero()) {
                                if (entry instanceof TemporaryTeleReference && TeleVM.isAttaching()) {
                                    // this is likely to be a reference in the dynamic heap that we can't see because TeleHeap is not
                                    // fully initialized yet so we add it to a fix-up list and handle it later
                                    attachFixupList.add(entry);
                                } else {
                                    Reference classActor = f.ConcurrentHashMap$HashEntry_value.readReference(entry);
                                    if (!classActor.isZero()) {
                                        addToRegistry(classActor);
                                        count++;
                                    }
                                }
                                entry = f.ConcurrentHashMap$HashEntry_next.readReference(entry);
                            }
                        }
                    }
                }
            }
            ClassID.setMapping(classIDMapping);
        } catch (Throwable throwable) {
            TeleError.unexpected("could not build inspector type registry", throwable);
        }
        initialClassCount = count;
        initTracer.end(statsPrinter);
    }

    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
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
            lastUpdateEpoch = epoch;
            updateTracer.end(statsPrinter);
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch);
        }
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxClassRegistry> memoryRegion() {
        // The class registry has no VM memory allocated, other than for its constituent objects
        return null;
    }

    public boolean contains(Address address) {
        // Class registry not represented by a memory region
        return false;
    }

    public TeleObject representation() {
        // No single distinguished object in VM runtime represents the class registry; there's one per classloader.
        return null;
    }

    public Set<TypeDescriptor> typeDescriptors() {
        return Collections.unmodifiableSet(typeDescriptorToClassActorReference.keySet());
    }

    public Iterable<TypeDescriptor> loadableTypeDescriptors() {
        final SortedSet<TypeDescriptor> typeDescriptors = new TreeSet<TypeDescriptor>();
        for (TypeDescriptor typeDescriptor : typeDescriptors()) {
            typeDescriptors.add(typeDescriptor);
        }
        typeDescriptors.addAll(typesOnClasspath());
        return typeDescriptors;
    }

    public void updateLoadableTypeDescriptorsFromClasspath() {
        final Set<TypeDescriptor> typesOnClasspath = new TreeSet<TypeDescriptor>();
        Trace.begin(TRACE_VALUE, tracePrefix() + "searching classpath for class files");
        new ClassSearch() {
            @Override
            protected boolean visitClass(String className) {
                if (!className.endsWith("package-info")) {
                    final String typeDescriptorString = "L" + className.replace('.', '/') + ";";
                    typesOnClasspath.add(JavaTypeDescriptor.parseTypeDescriptor(typeDescriptorString));
                }
                return true;
            }
        }.run(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.classpath());
        Trace.end(TRACE_VALUE, tracePrefix() + "searching classpath for class files ["
                + typesOnClasspath.size() + " types found]");
        this.typesOnClasspath = typesOnClasspath;
    }

    public TeleClassActor findTeleClassActor(int id) {
        final Reference classActorReference = idToClassActorReference.get(id);
        if (classActorReference != null && !classActorReference.isZero()) {
            return (TeleClassActor) heap().makeTeleObject(classActorReference);
        }
        return null;
    }

    public TeleClassActor findTeleClassActor(TypeDescriptor typeDescriptor) {
        final Reference classActorReference = typeDescriptorToClassActorReference.get(typeDescriptor);
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) heap().makeTeleObject(classActorReference);
    }

    public TeleClassActor findTeleClassActor(Class javaClass) {
        final Reference classActorReference = typeDescriptorToClassActorReference.get(JavaTypeDescriptor.forJavaClass(javaClass));
        if (classActorReference == null) {
            // Class hasn't been loaded yet by the inspectee.
            return null;
        }
        return (TeleClassActor) heap().makeTeleObject(classActorReference);
    }

    public void printSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        final NumberFormat formatter = NumberFormat.getInstance();
        printStream.print(indentation + "Classes loaded: " + formatter.format(initialClassCount + dynamicallyLoadedClassCount) +
                        " (initial: " + formatter.format(initialClassCount) +
                        ", during session: " + formatter.format(dynamicallyLoadedClassCount) + ")\n");
    }

    public void processAttachFixupList() {
        if (attachFixupList != null) {
            final TimedTrace timedTrace = new TimedTrace(TRACE_VALUE, tracePrefix() + " adding entries from attach fixup list");
            timedTrace.begin();
            for (Reference entry : attachFixupList) {
                if (!entry.isZero()) {
                    Reference classActor = vm().teleFields().ConcurrentHashMap$HashEntry_value.readReference(entry);
                    if (!classActor.isZero()) {
                        addToRegistry(classActor);
                    }
                }
            }
            timedTrace.end("added=" + attachFixupList.size());
            attachFixupList = null;
        }
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
     * Gets the canonical local {@link ClassActor} corresponding to a
     * {@link ClassActor} in the VM, creating it if needed.
     * Creation is done by loading the class, either from the classpath if present, or
     * by copying the classfile from the VM.  In either case the class is loaded by the
     * {@link HostedBootClassLoader#HOSTED_BOOT_CLASS_LOADER}.
     *
     * @param classActorReference  a {@link ClassActor} in the VM.
     * @return Local, canonical, equivalent {@link ClassActor} created by loading the same class.
     * @throws InvalidReferenceException if the argument does not point to a valid heap object in the VM.
     * @throws NoClassDefFoundError if the classfile is not on the classpath and the copy from the VM fails.
     * @throws TeleError if a classfile copied from the VM is cannot be loaded
     */
    public ClassActor makeClassActor(Reference classActorReference) throws InvalidReferenceException {
        vm().checkReference(classActorReference);
        final Reference utf8ConstantReference = vm().teleFields().Actor_name.readReference(classActorReference);
        vm().checkReference(utf8ConstantReference);
        final Reference stringReference = vm().teleFields().Utf8Constant_string.readReference(utf8ConstantReference);
        final String name = vm().getString(stringReference);
        try {
            return makeClassActor(name);
        } catch (ClassNotFoundException classNotFoundException) {
            // Not loaded and not available on local classpath; load by copying classfile from the VM
            final Reference byteArrayReference = vm().teleFields().ClassActor_classfile.readReference(classActorReference);
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) heap().makeTeleObject(byteArrayReference);
            if (teleByteArrayObject == null) {
                throw new NoClassDefFoundError(String.format("Could not retrieve class file from VM for %s%nTry using '%s' VM option to access generated class files.",
                    name, ClassfileReader.saveClassDir));
            }
            final byte[] classfile = (byte[]) teleByteArrayObject.shallowCopy();
            try {
                return HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.makeClassActor(name, classfile);
            } catch (ClassFormatError classFormatError) {
                final String msg = "in " + tracePrefix() + " unable to load classfile copied from VM, error message follows:\n   " + classFormatError;
                TeleError.unexpected(msg, null);
                return null;
            }
        }
    }

    /**
     * Gets a canonical local {@link ClassActor} for the named class, creating one if needed by loading the class from
     * the classpath using the {@link HostedBootClassLoader#HOSTED_BOOT_CLASS_LOADER}.
     *
     * @param name the name of a class
     * @return Local {@link ClassActor} corresponding to the class, possibly created by loading it from classpath.
     * @throws ClassNotFoundException if not already loaded and unavailable on the classpath.
     */
    private ClassActor makeClassActor(String name) throws ClassNotFoundException {
        // The VM registry includes all ClassActors for classes loaded locally
        // using the prototype class loader
        HostedBootClassLoader classLoader = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER;
        synchronized (classLoader) {
            ClassActor classActor = ClassRegistry.BOOT_CLASS_REGISTRY.get(JavaTypeDescriptor.getDescriptorForJavaString(name));
            if (classActor == null) {
                // Try to load the class from the local classpath.
                if (name.endsWith("[]")) {
                    classActor = ClassActorFactory.createArrayClassActor(makeClassActor(name.substring(0, name.length() - 2)));
                } else {
                    classActor = classLoader.makeClassActor(
                                    JavaTypeDescriptor.getDescriptorForWellFormedTupleName(name));
                }
            }
            return classActor;
        }
    }

    /**
     * Gets a canonical local {@classActor} corresponding to the type of a heap object in the VM, creating one if
     * needed by loading the class using the {@link HostedBootClassLoader#HOSTED_BOOT_CLASS_LOADER} from either the
     * classpath, or if not found on the classpath, by copying the classfile from the VM.
     *
     * @param objectReference An {@link Object} in the VM heap.
     * @return Local {@link ClassActor} representing the type of the object.
     * @throws InvalidReferenceException
     */
    public ClassActor makeClassActorForTypeOf(Reference objectReference)  throws InvalidReferenceException {
        vm().checkReference(objectReference);
        final Reference hubReference = vm().wordToReference(Layout.readHubReferenceAsWord(objectReference));
        final Reference classActorReference = vm().teleFields().Hub_classActor.readReference(hubReference);
        return makeClassActor(classActorReference);
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
        Trace.line(TRACE_VALUE + 2, tracePrefix() + ": adding class (" + id + ", " + typeDescriptor.toJavaString() + ")");
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
                classActor = makeClassActor(classActorReference);
                idToClassActor.put(id, classActor);
            }
        }
        return classActor;
    }

    /**
     * @return classes, possibly loaded, not available on the classpath.
     */
    private Set<TypeDescriptor> typesOnClasspath() {
        if (typesOnClasspath == null) {
            // Delayed initialization, because this can take some time.
            updateLoadableTypeDescriptorsFromClasspath();
        }
        return typesOnClasspath;
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
            } else {
                FatalError.unexpected("Who is resolving class IDs in the context of the host?");
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
            throw TeleError.unexpected(exception);
        } finally {
            usingTeleClassIDs.set(oldValue);
        }
    }

}
