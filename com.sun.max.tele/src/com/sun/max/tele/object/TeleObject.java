/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A canonical surrogate for a heap object in the VM.
 *
 * This class and its subclasses play the role of typed wrappers for {@link RemoteReference}s that refer to heap objects in
 * the VM. These wrappers encapsulate implementation details needed for working remotely with VM objects. This is not a
 * general proxy mechanisms, but rather an encapsulation of the design knowledge about the VM that is needed make sense
 * of VM state for debugging and visualization purposes.
 * <p>
 * The type hierarchy of {@link TeleObject} classes is designed as a <em>projection</em> (in the mathematical sense)
 * of the representational type hierarchy in the VM. This hierarchy includes standard Java object types, the Maxine
 * non-Java extended types under {@link TeleHybridObject}, and the special tuple {@link TeleStaticTuple}, which has no
 * type at all, even in the Maxine extended type system.
 * <p>
 * There exist {@link TeleObject} subclasses designed to represent specific object types in the VM for those types that
 * represent significant information about the runtime state of the VM, for example {@link TeleDynamicHub}. In those
 * cases there are typically specialized methods for accessing and reasoning about that state information. Other
 * subclasses are intended to manage usefully a whole subset of the type hierarchy; for example a
 * {@link TeleArrayObject} is used to represent any kind of array in the VM heap and a {@link TeleTupleObject} is used
 * to represent ordinary objects that are not called out for special treatment.
 * <p>
 * Note that instances of this class are to be created <strong>only</strong> by a reflection-driven
 * {@linkplain TeleObjectFactory factory}. That declaratively specified factory creates an instance of the most specific
 * subtype of {@link TeleObject} that applies to a VM object.
 * <p>
 * Each subclass implementation is expected to either avoid caching any values read from the VM, or to override
 * {@link #updateCache(long)} and refresh the cache(s) when that method is called.
 * <p>
 * There is an ongoing danger of circularity in the creation of {@link TeleObject} instances for VM objects that have
 * two-way (or circular) references to other objects; this can lead to infinite regress and stack overflow. The general
 * strategy for avoiding this is to keep the constructors for concrete subclasses as simple as possible and to avoid
 * following {@link RemoteReference} fields in constructors if at all possible. A troublesome example of such relationships
 * involves {@link TeleClassMethodActor}s and {@link TeleTargetMethod}s.
 *
 * @see TeleObjectFactory
 * @see VmObjectAccess
 */
public abstract class TeleObject extends AbstractVmHolder implements TeleVMCache, ObjectProvider, MaxObject {

    /**
     * Identification for the three low-level Maxine heap objects implementations upon which all objects are
     * implemented.
     *
     * @see com.sun.max.vm.object.ObjectAccess
     */
    public enum ObjectKind {

        /**
         * A Maxine implementation object that represents a Java array.
         *
         * @see com.sun.max.vm.object.ArrayAccess
         */
        ARRAY,

        /**
         * A Maxine implementation object that represents a Java object instance: a collection of name/value pairs.
         *
         * @see com.sun.max.vm.object.TupleAccess
         */
        TUPLE,

        /**
         * A special Maxine implementation object used to implement {@link Hub}s. These represent special objects that
         * cannot be described in ordinary Java; they have both fields (as in an object instance) and a collection of
         * specialized arrays.
         *
         * @see com.sun.max.vm.object.Hybrid
         */
        HYBRID;
    }

    /**
     * A simple class for aggregating lazily printed statistics, represented as a sequence of objects to be converted to
     * comma separate strings when actually printed.
     *
     */
    protected final class StatsPrinter {

        private final List<Object> statsPrinters = new ArrayList<Object>(10);

        /**
         * Adds a statistic to be reported.
         *
         * @param obj an object whose string value will be printed
         * @return a new printer containing nothing to be printed.
         */
        public StatsPrinter addStat(Object obj) {
            statsPrinters.add(obj == null ? "<unitialized stats>" : obj);
            return this;
        }

        @Override
        public String toString() {
            final int size = statsPrinters.size();
            if (size == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder(50);
            for (int index = 0; index < size; index++) {
                sb.append(statsPrinters.get(index).toString());
                if (index < size - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }

    /**
     * Default tracing for individual object cache updates, which would generate a tremendous amount of trace output
     * during each update cycle. Consider overriding for specific subclasses.
     */
    private static final int UPDATE_TRACE_VALUE = 3;

    /**
     * Controls tracing for deep object copying.
     */
    protected static final int COPY_TRACE_VALUE = 2;

    private TimedTrace updateTracer = null;

    private long lastUpdateEpoch = -1L;

    private final RemoteReference reference;
    private final SpecificLayout specificLayout;
    private final long oid;
    /**
     * Cache for Hub.
     */
    private TeleHub teleHub = null;

    /**
     * Creates a "surrogate" object that encapsulates information about an object in the VM.
     * <p>
     * This is not the same thing as a Proxy, although it can be used that way. Specific subclasses encapsulate design
     * information about the structure of the VM that enable useful access methods beyond simple field or element
     * access. Most important are the subclasses for {@link Actor}s, objects in the VM that encapsulate meta-information
     * about the language and object representation.
     * <p>
     * The factory method {@link TeleObjectFactory#make(RemoteReference)} ensures synchronized TeleObjects creation, and
     * instances should <em>only</em> be created via that factory.
     * <p>
     * It is important to avoid in the constructor (and in the constructors of subclasses) the following of
     * {@link RemoteReference} fields, which leads to the creation of another instance of {@link TeleObject}. This can lead to
     * infinite regress in the presence of mutually referential objects.
     *
     * @param vm the VM in which the object resides
     * @param reference the location of the object in the VM (whose absolute address can change via GC)
     * @param specificLayout information about the layout of information in the object
     */
    protected TeleObject(TeleVM vm, RemoteReference reference, SpecificLayout specificLayout) {
        super(vm);
        assert reference != null;
        this.reference = reference;
        this.specificLayout = specificLayout;
        oid = this.reference.makeOID();
    }

    private TimedTrace tracer() {
        if (updateTracer == null) {
            updateTracer = new TimedTrace(UPDATE_TRACE_VALUE, tracePrefix() + " updating");
        }
        return updateTracer;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <strong>Note</strong>: this update gets called automatically as part of {@link TeleObject} instance creation.
     *
     * @see TeleObjectFactory#make(RemoteReference)
     */
    public final void updateCache(long epoch) {
        // TODO (mlvdv) restore thread-lock assertion here for all updates??

        final StatsPrinter statsPrinter = new StatsPrinter();
        if (status().isNotDead()) {
            // Do some specialized tracing here, since there are subclasses that we
            // want to contribute to the tracing statistics, and since we want to
            // selectively trace certain subclasses.
            if (epoch > lastUpdateEpoch) {
                tracer().begin(getObjectUpdateTraceValue(epoch));
                if (updateObjectCache(epoch, statsPrinter)) {
                    lastUpdateEpoch = epoch;
                }
                tracer().end(getObjectUpdateTraceValue(epoch), statsPrinter);
            } else {
                statsPrinter.addStat("Redundant update skipped");
                Trace.line(UPDATE_TRACE_VALUE, tracePrefix() + " redundant update epoch=" + epoch + ": " + this);
            }
        }
    }

    /**
     * Force an immediate update of any cached data if it has not yet been done during the current refresh cycle.
     * @see #updateCache(long)
     */
    public final void updateCacheIfNeeded() {
        final long currentEpoch = vm().teleProcess().epoch();
        if (currentEpoch > lastUpdateEpoch && status().isNotDead()) {
            Trace.line(UPDATE_TRACE_VALUE, tracePrefix() + "out of order update at " + reference.origin().to0xHexString());
            updateCache(currentEpoch);
        }
    }

    /**
     * Gets the level at which to present a trace of individual object updates, specified
     * here as the default.  Subclasses should override to lower the level for specific types,
     * since producing traces for every object update would generate an unreasonably large amount
     * of output.
     *
     * @param epoch the current process epoch at the time of the update
     * @return the trace level that should bye used by this object during updates
     */
    protected int getObjectUpdateTraceValue(long epoch) {
        return UPDATE_TRACE_VALUE;
    }

    /**
     * Internal call to subclasses to update their state, wrapped in order to provide timing and update statistics
     * reporting and for there to be a uniform bail-out if there is an update failure.
     * <p>
     * The intention is for updates to take place top-down, so every override should first call {@code super()} and then
     * halt the update if there is an updating failure.
     *
     * @param epoch the process epoch at the time of this update.
     * @param statsPrinter list of objects that report statistics for updates performed on this object so far (with no
     *            newlines)
     * @return whether the object's cache was successfully updated.
     */
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        return true;
    }

    public final ObjectStatus status() {
        return reference().status();
    }

    public final RemoteReference reference() {
        return reference;
    }

    public final long getOID() {
        return oid;
    }

    public String maxineRole() {
        return null;
    }

    public String maxineTerseRole() {
        return maxineRole();
    }

    public boolean hasTextualVisualization() {
        return false;
    }

    public String textualVisualization() {
        return null;
    }

    /**
     * The class actor for this class is returned when the real class actor
     * of a {@link TeleObject} cannot be retrieved.
     */
    public static class InvalidObjectClass {}

    public ClassActor classActorForObjectType() { // TODO: fix class actor lookup
        try {
            TeleHub hub = getTeleHub();
            if (hub != null) {
                TeleClassActor teleClassActor = hub.getTeleClassActor();
                return teleClassActor.classActor();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return ClassActor.fromJava(InvalidObjectClass.class);
    }

    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        return null;
    }

    public final Pointer origin() {
        return reference.toOrigin();
    }

    /**
     * Gets the size of the memory occupied by this object in the VM,
     * including header.
     */
    protected abstract int objectSize();

    public final TeleFixedMemoryRegion objectMemoryRegion() {
        if (objectSize() > 0) {
            return new TeleFixedMemoryRegion(vm(), "", specificLayout.originToCell(reference.toOrigin()), objectSize());
        }
        return null;
    }

    public final TypeDescriptor headerType(HeaderField headerField) {
        if (headerField == HeaderField.HUB) {
            return getTeleHub() == null ? null : JavaTypeDescriptor.forJavaClass(getTeleHub().hub().getClass());
        } else if (headerField == HeaderField.LENGTH) {
            return JavaTypeDescriptor.INT;
        }
        return JavaTypeDescriptor.WORD;
    }

    /**
     * The size in bytes of a field in the object's header.
     *
     * @param headerField identifies a header field in the object layout
     * @return the size of the header field
     */
    public final int headerSize(HeaderField headerField) {
        return headerType(headerField).toKind().width.numberOfBytes;
    }

    public final int headerOffset(HeaderField headerField) {
        if (headerField == HeaderField.LENGTH) {
            return objects().layoutScheme().arrayLayout.getOffsetFromOrigin(headerField).toInt();
        } else {
            return objects().layoutScheme().generalLayout.getOffsetFromOrigin(headerField).toInt();
        }
    }

    public Address headerAddress(HeaderField headerField) {
        return reference().origin().plus(headerOffset(headerField));
    }

    public final TeleFixedMemoryRegion headerMemoryRegion(HeaderField headerField) {
        final Address address = headerAddress(headerField);
        final int nBytes = headerSize(headerField);
        return new TeleFixedMemoryRegion(vm(), "Current memory for header field " + headerField.name, address, nBytes);
    }

    public final TeleHub getTeleHub() {
        if (teleHub == null) {
            // If this object is a forwarder then get the hub of the new copy
            teleHub = (TeleHub) objects().makeTeleObject(reference.followIfForwarded().readHubAsRemoteReference());
        }
        return teleHub;
    }

    public Word readMiscWord() {
        return Layout.readMisc(reference);
    }

    /**
     * Gets the fields for either a tuple or hybrid object, returns empty set for arrays. Returns static fields in the
     * special case of a {@link StaticTuple} object.
     */
    public Set<FieldActor> getFieldActors() {
        final Set<FieldActor> instanceFieldActors = new HashSet<FieldActor>();
        collectInstanceFieldActors(classActorForObjectType(), instanceFieldActors);
        return instanceFieldActors;
    }

    /**
     * Gathers all instance fields for a class, including inherited fields.
     *
     * @param classActor description of a class
     * @param instanceFieldActors the set to which collected {@link FieldActor}s will be added.
     */
    private void collectInstanceFieldActors(ClassActor classActor, Set<FieldActor> instanceFieldActors) {
        if (classActor != null) {
            for (FieldActor fieldActor : classActor.localInstanceFieldActors()) {
                instanceFieldActors.add(fieldActor);
            }
            collectInstanceFieldActors(classActor.superClassActor, instanceFieldActors);
        }
    }

    /**
     * Gets the current memory address of a field in the object, subject to relocation by GC.
     *
     * @param fieldActor descriptor for a field in this class
     * @return the current location in memory of the field in this object
     */
    public abstract Address fieldAddress(FieldActor fieldActor);

    public abstract int fieldSize(FieldActor fieldActor);

    public final TeleFixedMemoryRegion fieldMemoryRegion(FieldActor fieldActor) {
        final Pointer start = origin().plus(fieldActor.offset());
        return new TeleFixedMemoryRegion(vm(), "", start, fieldSize(fieldActor));
    }

    /**
     * @return a shallow copy of the object in the vm, with any references in it nulled out
     */
    public abstract Object shallowCopy();

    /**
     * A carrier for the context needed to produce a local object that is a deep
     * copy of a VM object.  Copying is truncated at any field annotated with {@link INSPECTED}
     * and for which {@link INSPECTED#deepCopied()} returns {@code false}.
     *
     */
    protected static class DeepCopier {

        int level = 0;
        final Map<TeleObject, Object> teleObjectToObject;

        DeepCopier() {
            teleObjectToObject = new HashMap<TeleObject, Object>();
        }

        DeepCopier(DeepCopier parent) {
            level = parent.level;
            teleObjectToObject = parent.teleObjectToObject;
        }

        /**
         * @return the depth of the object graph currently being copied
         */
        protected int level() {
            return level;
        }

        static int totalCopies;

        static class Count implements Comparable<Count> {

            Class type;
            int value;

            @Override
            public int compareTo(Count o) {
                return value - o.value;
            }
        }

        static HashMap<Class, Count> copiesPerType = new HashMap<Class, Count>() {

            @Override
            public Count get(Object key) {
                Count count = super.get(key);
                if (count == null) {
                    count = new Count();
                    count.type = (Class) key;
                    put((Class) key, count);
                }
                return count;
            }
        };

        static {
            if (Trace.hasLevel(1)) {
                Runtime.getRuntime().addShutdownHook(new Thread("CopiesPerTypePrinter") {

                    @Override
                    public void run() {
                        SortedSet<Count> set = new TreeSet<Count>(copiesPerType.values());
                        System.out.println("Objects deep copied from VM (by type):");
                        for (Count c : set) {
                            System.out.println("    " + c.value + "\t" + c.type.getSimpleName());
                        }
                    }
                });
            }
        }

        /**
         * Registers a newly copied object in the context to avoid duplication.
         *
         * @param newInstance specifies {@code object} was just instantiated (as opposed to being a local surrogate)
         */
        protected void register(TeleObject teleObject, Object object, boolean newInstance) {
            Object oldValue = teleObjectToObject.put(teleObject, object);
            int numberOfCopies = numberOfCopies();
            if (oldValue == null && newInstance && Trace.hasLevel(COPY_TRACE_VALUE)) {
                copiesPerType.get(object.getClass()).value++;
                totalCopies++;
                if ((numberOfCopies % 100) == 0) {
                    Trace.line(COPY_TRACE_VALUE, "Deep copied " + numberOfCopies + " objects [" + totalCopies + " in total]");
                }
            }
        }

        /**
         * Gets the number of unique object copied by this copier.
         */
        public int numberOfCopies() {
            return teleObjectToObject.size();
        }

        /**
         * Updates the field of an object or class from the VM.  The field will always be set to {@code null} if
         * the field is annotated with {@link INSPECTED} and {@link INSPECTED#deepCopied()} returns {@code false}.
         *
         * @param teleObject surrogate for a tuple in the VM. This will be a static tuple if the field is static.
         * @param newTuple the local object to be updated in the host VM. This value is ignored if the field is static.
         * @param fieldActor the field to be copied/updated
         */
        protected void copyField(TeleObject teleObject, Object newTuple, FieldActor fieldActor) {
            if (!fieldActor.isInjected()) {
                Field field = null;
                try {
                    field = fieldActor.toJava();
                    field.setAccessible(true);
                } catch (NoSuchFieldError ex) {
                    // this is a hidden to reflection field like Throwable.backtrace, such fields should be avoided explicitly
                    return;
                }
                try {
                    final Value value = teleObject.readFieldValue(fieldActor);
                    final Object newJavaValue;
                    if (fieldActor.kind.isReference) {
                        INSPECTED a = fieldActor.getAnnotation(INSPECTED.class);
                        if (a != null && !a.deepCopied()) {
                            return;
                        }
                        final TeleObject teleFieldReferenceObject = teleObject.objects().makeTeleObject((RemoteReference) value.asReference());
                        if (teleFieldReferenceObject == null) {
                            newJavaValue = null;
                        } else {
                            newJavaValue = makeDeepCopy(fieldActor, teleFieldReferenceObject);
                        }
                    } else if (fieldActor.kind.isWord) {
                        final Class<Class< ? extends Word>> type = null;
                        final Class< ? extends Word> wordType = Utils.cast(type, fieldActor.toJava().getType());
                        newJavaValue = value.asWord().as(wordType);
                    } else {
                        newJavaValue = value.asBoxedJavaValue();
                    }
                    field.set(newTuple, newJavaValue);
                } catch (IllegalAccessException illegalAccessException) {
                    TeleError.unexpected("could not access field: " + field, illegalAccessException);
                }
            }
        }

        protected Object makeDeepCopy(FieldActor fieldActor, TeleObject teleObject) {
            return teleObject.makeDeepCopy(this);
        }
    }

    /**
     * @return produces a deep copy of an object as part of a larger deep copy in which this particular object may have
     *         already been copied.
     */
    protected final Object makeDeepCopy(DeepCopier context) {
        Object newObject = context.teleObjectToObject.get(this);
        if (newObject == null) {
            context.level++;
            newObject = createDeepCopy(context);
            context.register(this, newObject, false);
            context.level--;
        }
        return newObject;
    }

    /**
     * @return creates a local deep copy of the object, using Maxine-specific shortcuts when possible to produce a local
     *         equivalent without copying. Implementations that copy recursively must call
     *         {@link TeleObject#makeDeepCopy(DeepCopier)}, and must register newly allocated objects before doing so.
     *         This will result in redundant registrations in those cases.
     */
    protected abstract Object createDeepCopy(DeepCopier context);

    /**
     * Checks to see if a deep copy of this object would be potentially problematic, and if so generates a warning message.
     *
     * @return a warning message about deep copying, null otherwise; default is no-warning.
     */
    protected String deepCopyWarning() {
        return null;
    }

    /**
     * Hook for subclasses to refine the extent of {@linkplain #deepCopy() deep copying}.
     */
    protected DeepCopier newDeepCopier() {
        return new DeepCopier();
    }

    /**
     * Creates a local copy of the remote VM object.  Deep copying is truncated at reference fields
     * marked with the {@link INSPECTED} annotation specifying the value {@code false} for {@link INSPECTED#deepCopied()}.
     *
     * @param copier copier context of {@code null} is none.
     * @return a best effort deep copy, truncated at reference fields for which {@link INSPECTED#deepCopied()} returns {@code false}.
     * @see INSPECTED#deepCopied()
     */
    public final Object deepCopy(DeepCopier copier) {
        Object objectCopy = null;
        Trace.begin(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        long start = System.currentTimeMillis();
        final String warningMessage = deepCopyWarning();
        if (warningMessage != null) {
            TeleWarning.message(warningMessage);
        }
        if (vm().tryLock()) {
            try {
                if (copier == null) {
                    copier = newDeepCopier();
                }
                objectCopy = makeDeepCopy(copier);
                Trace.end(COPY_TRACE_VALUE, "Deep copying from VM: " + this + " [" + copier.numberOfCopies() + " objects]", start);
            } finally {
                vm().unlock();
            }
        } else {
            TeleWarning.message("Deep copy failed (VM busy) for " + this);
        }
        return objectCopy;
    }

    public final Object deepCopy() {
        return deepCopy(null);
    }

    /**
     * Updates the static fields of a specified local class from the VM.
     */
    public static void copyStaticFields(MaxVM vm, Class javaClass) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final TeleClassActor teleClassActor = vm.classes().findTeleClassActor(javaClass);
        final TeleStaticTuple teleStaticTuple = teleClassActor.getTeleStaticTuple();

        final String classMessage = "Copying static fields of " + javaClass + " from VM";
        Trace.begin(COPY_TRACE_VALUE, classMessage);
        DeepCopier copier = new DeepCopier();
        try {
            for (FieldActor fieldActor : classActor.localStaticFieldActors()) {
                final String fieldMessage = fieldActor.format("Copying static field '%n' of type '%t' from VM");
                Trace.begin(COPY_TRACE_VALUE, fieldMessage);
                copier.copyField(teleStaticTuple, null, fieldActor);
                Trace.end(COPY_TRACE_VALUE, fieldMessage);
            }
        } finally {
            Trace.end(COPY_TRACE_VALUE, classMessage + " [" + copier.numberOfCopies() + " objects]");
        }
    }

    @Override
    public String toString() {
        return getClass().toString() + "<" + oid + ">";
    }

    public ReferenceTypeProvider getReferenceType() {
        return classes().findTeleClassActor(classActorForObjectType().typeDescriptor);
    }
}
