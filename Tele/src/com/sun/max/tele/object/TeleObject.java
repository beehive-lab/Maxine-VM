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
package com.sun.max.tele.object;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
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
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Canonical surrogate for a heap object in the VM.
 *
 * This class and its subclasses play the role of typed wrappers for References to heap objects in the VM, encapsulating
 * implementation details for working with those objects remotely. <br>
 * Each implementation is expected to either avoid caching any values read from the VM, or to override
 * {@link #updateCache()} and refresh the cache(s) when that method is called.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public abstract class TeleObject extends AbstractTeleVMHolder implements TeleVMCache, ObjectProvider, TeleObjectMemory {

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
     * @author Michael Van De Vanter
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

    private TeleReference reference;
    private final LayoutScheme layoutScheme;
    private final SpecificLayout specificLayout;
    private final long oid;
    private TeleHub teleHub = null;

    private Pointer lastValidPointer;

    /**
     * A "surrogate" object that encapsulates information about an object in the VM. <br>
     * This is not the same thing as a Proxy, although it can be used that way. Specific subclasses encapsulate design
     * information about the structure of the VM that enable useful access methods beyond simple field or element
     * access. Most important are the subclasses for {@link Actor}s, objects in the VM that encapsulate meta-information
     * about the language and object representation. <br>
     * The factory method {@link TeleObjectFactory#make(Reference)} ensures synchronized TeleObjects creation.
     *
     * @param vm the VM in which the object resides
     * @param reference the location of the object in the VM (whose absolute address can change via GC)
     * @param specificLayout information about the layout of information in the object
     */
    protected TeleObject(TeleVM vm, Reference reference, SpecificLayout specificLayout) {
        super(vm);
        this.reference = (TeleReference) reference;
        this.layoutScheme = Layout.layoutScheme();
        this.specificLayout = specificLayout;
        oid = this.reference.makeOID();
        lastValidPointer = Pointer.zero();

    }

    private TimedTrace tracer() {
        if (updateTracer == null) {
            updateTracer = new TimedTrace(UPDATE_TRACE_VALUE, tracePrefix() + " updating");
        }
        return updateTracer;
    }

    public final void updateCache(long epoch) {
        // Note that this method gets called automatically as part of instance creation
        // in {@link TeleObjectFactory#make(Reference)}

        // TODO (mlvdv) restore thread-lock assertion here for all updates??

        final StatsPrinter statsPrinter = new StatsPrinter();

        // Do some specialized tracing here, since there are subclasses that we
        // want to contribute to the tracing statistics, and since we want to
        // selectively trace certain subclasses.
        tracer().begin(getObjectUpdateTraceValue());

        if (epoch > lastUpdateEpoch) {
            updateObjectCache(statsPrinter);
            lastUpdateEpoch = epoch;
        } else {
            statsPrinter.addStat("Redundant update skipped");
            Trace.line(UPDATE_TRACE_VALUE, tracePrefix() + " redundant update epoch=" + epoch + ": " + this);
        }
        tracer().end(getObjectUpdateTraceValue(), statsPrinter);
        /*
         * if (reference.toOrigin().equals(Pointer.zero())) { live = false; }
         */
    }

    /**
     * Gets the level at which to present a trace of individual object updates, specified
     * here as the default.  Subclasses should override to lower the level for specific types,
     * since producing traces for every object update would generate an unworkably large amount
     * of output.
     *
     * @return the trace level that should bye used by this object during updates
     */
    protected int getObjectUpdateTraceValue() {
        return UPDATE_TRACE_VALUE;
    }

    /**
     * Internal call to subclasses to update their state, wrapped in the {@link TeleObject} class to provide timing and
     * update statistics reporting.
     *
     * @param statsPrinters list of objects that report statistics for updates performed on this object so far (with no
     *            newlines)
     */
    protected void updateObjectCache(StatsPrinter statsPrinter) {
    }

    public final TeleObjectMemory.State getTeleObjectMemoryState() {
        return reference.getTeleObjectMemoryState();
    }

    public final boolean isLive() {
        return reference.isLive();
    }

    public final boolean isObsolete() {
        return reference.isObsolete();
    }

    public final boolean isDead() {
        return reference.isDead();
    }

    public final TeleObject getForwardedTeleObject() {
        if (isObsolete()) {
            TeleReference forwardedTeleRef = reference.getForwardedTeleRef();
            TeleObject teleObject = heap().findObjectByOID(forwardedTeleRef.makeOID());
            if (teleObject == null) {
                reference = (TeleReference) forwardedTeleRef;
                return this;
            }
            return teleObject;
        }
        return this;
    }

    /**
     * @return canonical reference to this object in the VM
     */
    public final TeleReference reference() {
        return reference;
    }

    /**
     * @return to which of the Maxine heap object representations does this surrogate refer?
     */
    public abstract ObjectKind kind();

    /**
     * @return a number that uniquely identifies this object in the VM for the duration of the inspection
     */
    public final long getOID() {
        return oid;
    }

    /**
     * @return a short string describing the role played by this object if it is of special interest in the Maxine
     *         implementation, null if any other kind of object.
     */
    public String maxineRole() {
        return null;
    }

    /**
     * @return an extremely short, abbreviated version of the string {@link #maxineRole()}, describing the role played
     *         by this object in just a few characters.
     */
    public String maxineTerseRole() {
        return maxineRole();
    }

    /**
     * The class actor for this class is returned when the real class actor
     * of a {@link TeleObject} cannot be retrieved.
     */
    public static class InvalidObjectClass {}

    /**
     * @return local {@link ClassActor}, equivalent to the one in the VM that describes the type of this object in the
     *         VM. Note that in the singular instance of {@link StaticTuple} this does not correspond to the actual type
     *         of the object, which is an exceptional Maxine object that has no ordinary Java type; it returns in this
     *         case the type of the class that the tuple helps implement.
     */
    public ClassActor classActorForObjectType() { // TODO: fix class actor lookup
        try {
            TeleHub hub = getTeleHub();
            TeleClassActor teleClassActor = hub.getTeleClassActor();
            return teleClassActor.classActor();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return ClassActor.fromJava(InvalidObjectClass.class);
        }
    }

    /**
     * return local surrogate for the{@link ClassMethodActor} associated with this object in the VM, either because it
     * is a {@link ClassMethodActor} or because it is a class closely associated with a method that refers to a
     * {@link ClassMethodActor}. Null otherwise.
     */
    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        return null;
    }

    /**
     * The current "origin" of the object in VM memory, which may change through GC as long as the object remains live.
     * When the object is no longer live, the last live location is returned. <br>
     * Note that the origin is not necessarily beginning of the object's memory allocation, depending on the particular
     * object layout used.
     *
     * @return current absolute location of the object's origin, subject to change by GC
     * @see GeneralLayout
     *
     */
    public Pointer origin() {
        if (isObsolete() || isDead()) {
            return lastValidPointer;
        }
        Pointer pointer = reference.toOrigin();
        lastValidPointer = pointer;
        return pointer;
    }

    /**
     * @return the size of the memory occupied by this object in the VM, including header.
     */
    protected abstract int objectSize();

    /**
     * Gets the current area of memory in which the object is stored.
     *
     * @return current memory region occupied by this object in the VM, subject to relocation by GC.
     */
    public final TeleFixedMemoryRegion objectMemoryRegion() {
        if (isObsolete() || isDead()) {
            // Log.println("STATE DEAD: " + lastValidPointer + " " + specificLayout.originToCell(lastValidPointer));
            return new TeleFixedMemoryRegion(vm(), "", specificLayout.originToCell(lastValidPointer), objectSize());
        }
        return new TeleFixedMemoryRegion(vm(), "", specificLayout.originToCell(reference.toOrigin()), objectSize());
    }

    public final MaxMemoryRegion getForwardedMemoryRegion() {
        if (isObsolete()) {
            return new TeleFixedMemoryRegion(vm(), "", specificLayout.originToCell(reference.getForwardedTeleRef().toOrigin()), objectSize());
        }
        return null;
    }

    /**
     * The fields in the object's header.
     *
     * @return enumeration of the fields in the header of this object
     */
    public abstract HeaderField[] headerFields();

    /**
     * The type of a field in the obejct's header, calling out specially
     * the standard ones. Unknown ones are treated as words.
     *
     * @param headerField identifies a header field in the object layout
     * @return the type of the header field, Word if unknown.
     */
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

    /**
     * Offset from the object's origin of a field in the object's header.
     *
     * @param headerField identifies a header field in the object layout
     * @return the location of the header field relative to object origin
     */
    public final int headerOffset(HeaderField headerField) {
        if (headerField == HeaderField.LENGTH) {
            return layoutScheme.arrayLayout.getOffsetFromOrigin(headerField).toInt();
        } else {
            return layoutScheme.generalLayout.getOffsetFromOrigin(headerField).toInt();
        }
    }

    /**
     * Address of a field in the object's header.
     *
     * @param headerField identifies a header field in the object layout
     * @return the location of the header in VM memory
     */
    public Address headerAddress(HeaderField headerField) {
        return origin().plus(headerOffset(headerField));
    }

    /**
     * The memory region in which an object header field is stored, subject to change by GC relocation.
     *
     * @param headerField a field in the object's header
     * @return current memory region occupied by a header field in this object in the VM
     */
    public final TeleFixedMemoryRegion headerMemoryRegion(HeaderField headerField) {
        final Address address = headerAddress(headerField);
        final int nBytes = headerSize(headerField);
        return new TeleFixedMemoryRegion(vm(), "Current memory for header field " + headerField.name, address, nBytes);
    }

    /**
     * Gets the "hub" object pointed to in the object's header.
     *
     * @return the local surrogate for the Hub of this object
     */
    public TeleHub getTeleHub() {
        if (teleHub == null) {
            final Reference hubReference = vm().wordToReference(Layout.readHubReferenceAsWord(reference));
            teleHub = (TeleHub) heap().makeTeleObject(hubReference);
        }
        return teleHub;
    }

    /**
     * Gets the contents of the misc" word in the object's header.
     *
     * @return the "misc" word from the header of this object in the VM
     */
    public Word getMiscWord() {
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

    /**
     * The size in bytes of a field in the object.
     *
     * @param fieldActor descriptor for a field in this class
     * @return the memory size of the field
     */
    public abstract int fieldSize(FieldActor fieldActor);

    /**
     * The memory region in which field in the object is stored, subject to change by GC relocation.
     *
     * @param fieldActor a field in the object
     * @return current memory region occupied by the field in this object in the VM, subject to relocation by GC.
     */
    public final TeleFixedMemoryRegion fieldMemoryRegion(FieldActor fieldActor) {
        final Pointer start = origin().plus(fieldActor.offset());
        return new TeleFixedMemoryRegion(vm(), "", start, fieldSize(fieldActor));
    }

    /**
     * @param fieldActor local {@link FieldActor}, part of the {@link ClassActor} for the type of this object, that
     *            describes a field in this object in the VM
     * @return contents of the designated field in this object in the VM
     */
    public abstract Value readFieldValue(FieldActor fieldActor);

    /**
     * @return a shallow copy of the object in the teleVM, with any references in it nulled out
     */
    public abstract Object shallowCopy();

    protected static class DeepCopier {

        int level = 0;
        final Map<TeleObject, Object> teleObjectToObject = new HashMap<TeleObject, Object>();
        final Set<FieldActor> omittedFields = new HashSet<FieldActor>();

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
            };
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
            if (oldValue == null && newInstance && Trace.hasLevel(1)) {
                copiesPerType.get(object.getClass()).value++;
                totalCopies++;
                if ((numberOfCopies % 100) == 0) {
                    Trace.line(1, "Deep copied " + numberOfCopies + " objects [" + totalCopies + " in total]");
                }
            }
        }

        protected DeepCopier omit(FieldActor fieldActor) {
            omittedFields.add(fieldActor);
            return this;
        }

        /**
         * Gets the number of unique object copied by this copier.
         */
        public int numberOfCopies() {
            return teleObjectToObject.size();
        }

        /**
         * Updates the field of an object or class from the VM.
         *
         * @param teleObject surrogate for a tuple in the VM. This will be a static tuple if the field is static.
         * @param tuple the local object to be updated in the host VM. This value is ignored if the field is static.
         * @param fieldActor the field to be copied/updated
         */
        protected void copyField(TeleObject teleObject, Object newTuple, FieldActor fieldActor) {
            if (!omittedFields.isEmpty() && omittedFields.contains(fieldActor)) {
                return;
            }

            if (!fieldActor.isInjected()) {
                final Field field = fieldActor.toJava();
                field.setAccessible(true);
                try {
                    final Value value = teleObject.readFieldValue(fieldActor);
                    final Object newJavaValue;
                    if (fieldActor.kind.isReference) {
                        final TeleObject teleFieldReferenceObject = teleObject.heap().makeTeleObject(value.asReference());
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
     * Hook for subclasses to refine the extent of {@linkplain #deepCopy() deep copying}.
     */
    protected DeepCopier newDeepCopier() {
        return new DeepCopier();
    }

    /**
     * @return a best effort deep copy - with certain substitutions
     */
    public final Object deepCopy() {
        Object objectCopy = null;
        Trace.begin(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        long start = System.currentTimeMillis();
        if (vm().tryLock()) {
            try {
                DeepCopier copier = newDeepCopier();
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

    /**
     * Updates the static fields of a specified local class from the VM.
     */
    public static void copyStaticFields(TeleVM teleVM, Class javaClass) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final TeleClassActor teleClassActor = teleVM.classRegistry().findTeleClassActor(javaClass);
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

    public Reference getReference() {
        return this.reference();
    }

    public ReferenceTypeProvider getReferenceType() {
        return vm().classRegistry().findTeleClassActor(classActorForObjectType().typeDescriptor);
    }
}
