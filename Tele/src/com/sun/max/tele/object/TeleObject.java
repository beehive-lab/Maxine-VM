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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Canonical surrogate for a heap object in the {@link TeleVM}.
 *
 * This class and its subclasses play the role of typed wrappers for References to heap objects in the {@link TeleVM},
 * encapsulating implementation details for working with those objects remotely.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleObject extends AbstractTeleVMHolder implements ObjectProvider {

    /**
     * Identification for the three low-level Maxine heap objects implementations upon which all objects are implemented.
     * @see com.sun.max.vm.object.ObjectAccess
     */
    public enum ObjectKind {

        /**
         * A Maxine implementation object that represents a Java array.
         * @see com.sun.max.vm.object.ArrayAccess
         */
        ARRAY,

        /**
         * A Maxine implementation object that represents a Java object instance:  a collection of name/value pairs.
         * @see com.sun.max.vm.object.TupleAccess
         */
        TUPLE,

        /**
         * A special Maxine implementation object used to implement {@link Hub}s.
         * These represent special objects that cannot be described in ordinary Java;
         * they have both fields (as in an object instance) and a collection of
         * specialized arrays.
         * @see com.sun.max.vm.object.Hybrid
         */
        HYBRID;
    }

    /**
     * Controls tracing for deep object copying.
     */
    protected static final int COPY_TRACE_VALUE = 2;

    private final TeleReference reference;
    private final LayoutScheme layoutScheme;
    private final SpecificLayout specificLayout;
    private final long oid;
    private TeleHub teleHub = null;

    private boolean live;
    private Pointer lastValidPointer;

    /**
     * A "surrogate" object that encapsulates information about an object in the VM.
     * <br>
     * This is not the same thing as a Proxy, although it can be used that way.  Specific subclasses
     * encapsulate design information about the structure of the VM that enable useful access
     * methods beyond simple field or element access.  Most important are the subclasses for
     * {@link Actor}s, objects in the VM that encapsulate meta-information about the language and
     * object representation.
     * <br>
     * The factory method {@link TeleObjectFactory#make(Reference)} ensures synchronized TeleObjects creation.
     *
     * @param teleVM the VM in which the object resides
     * @param reference the location of the object in the VM (whose absolute address can change via GC)
     * @param specificLayout information about the layout of information in the object
     */
    protected TeleObject(TeleVM teleVM, Reference reference, SpecificLayout specificLayout) {
        super(teleVM);
        this.reference = (TeleReference) reference;
        this.layoutScheme = teleVM.vmConfiguration().layoutScheme();
        this.specificLayout = specificLayout;
        oid = this.reference.makeOID();
        live = true;
        lastValidPointer = Pointer.zero();
    }

    /**
     * The status of the object in the VM.  A new object is by definition "live", but
     * once it has been freed by the VM's garbage collector, it is no longer a legitimate
     * object.
     * <br>
     * Once an object is dead the memory most recently allocated to it might be
     * reused by the VM.  For debugging purposes, however, a {@link TeleObject}
     * continues to refer to the abandoned memory as if it were an object.
     *
     * @return whether the object in the VM is still live
     */
    public boolean isLive() {
        getCurrentOrigin();
        return live;
    }

    protected void refresh(long processEpoch) {
        /*if (reference.toOrigin().equals(Pointer.zero())) {
            live = false;
        }*/
    }

    /**
     * @return canonical reference to this object in the {@link TeleVM}
     */
    public TeleReference reference() {
        return reference;
    }

    /**
     * @return to which of the Maxine heap object representations does this surrogate refer?
     */
    public abstract ObjectKind getObjectKind();

    /**
     * @return a number that uniquely identifies this object in the {@link TeleVM} for the duration of the inspection
     */
    public long getOID() {
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
     * @return local {@link ClassActor}, equivalent to the one in the teleVM that describes the type
     * of this object in the {@link TeleVM}.
     * Note that in the singular instance of {@link StaticTuple} this does not correspond to the actual type of the
     * object, which is an exceptional Maxine object that has no ordinary Java type; it returns in this case
     * the type of the class that the tuple helps implement.
     */
    public ClassActor classActorForType() {
        return getTeleHub().getTeleClassActor().classActor();
    }

    /**
     * return local surrogate for the{@link ClassMethodActor} associated with this object in the {@link TeleVM}, either
     * because it is a {@link ClassMethodActor} or because it is a class closely associated with a method that refers to
     * a {@link ClassMethodActor}. Null otherwise.
     */
    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        return null;
    }

    /**
     * @return current memory region occupied by this object in the VM, subject to relocation by GC.
     */
    public final MemoryRegion getCurrentMemoryRegion() {
        return new FixedMemoryRegion(specificLayout.originToCell(reference.toOrigin()), objectSize(), "");
    }

    /**
     * @return the size of the memory occupied by this object in the VM, including header.
     */
    protected abstract Size objectSize();

    /**
     * The current location of the object in VM memory, which
     * may change through GC as long as the object remains live.
     * When the object is no longer live, the last live location is
     * returned.
     * <br>
     * Note that the origin is not necessarily beginning of the object's
     * memory allocation, depending on the particular object layout
     * used.
     * @return current absolute location of the object's origin
     * @see GeneralLayout
     *
     */
    public Pointer getCurrentOrigin() {
        Pointer pointer = reference.toOrigin();
        if (pointer.equals(Pointer.zero())) {
            live = false;
            return lastValidPointer;
        }
        lastValidPointer = pointer;
        return pointer;
    }

    /**
     * @return enumeration of the fields in the header of this object
     */
    public abstract HeaderField[] getHeaderFields();

    /**
     * @param headerField
     * @return current memory region occupied by a header field in this object in the VM, subject to change by GC
     */
    public final MemoryRegion getCurrentMemoryRegion(Layout.HeaderField headerField) {
        final Pointer start = getCurrentOrigin().plus(getHeaderOffset(headerField));
        final Size size = Size.fromInt(getHeaderType(headerField).toKind().width.numberOfBytes);
        return new FixedMemoryRegion(start, size, "");
    }

    /**
     * @param headerField identifies a header field in the object layout
     * @return the type of the header field
     */
    public final TypeDescriptor getHeaderType(Layout.HeaderField headerField) {
        if (headerField == HeaderField.HUB) {
            return getTeleHub() == null ? null : JavaTypeDescriptor.forJavaClass(getTeleHub().hub().getClass());
        } else if (headerField == HeaderField.LENGTH) {
            return JavaTypeDescriptor.INT;
        }
        return JavaTypeDescriptor.WORD;
    }

    /**
     * @param headerField identifies a header field in the object layout
     * @return the location of the header field relative to object origin
     */
    public final Offset getHeaderOffset(Layout.HeaderField headerField) {
        if (headerField != HeaderField.LENGTH) {
            return layoutScheme.generalLayout.getOffsetFromOrigin(headerField);
        } else {
            return layoutScheme.arrayHeaderLayout.getOffsetFromOrigin(headerField);
        }
    }

    /**
     * @return the local surrogate for the Hub of this object
     */
    public TeleHub getTeleHub() {
        if (teleHub == null) {
            final Reference hubReference = teleVM().wordToReference(teleVM().layoutScheme().generalLayout.readHubReferenceAsWord(reference));
            teleHub = (TeleHub) teleVM().makeTeleObject(hubReference);
        }
        return teleHub;
    }

    /**
     * @return the "misc" word from the header of this object in the teleVM
     */
    public Word getMiscWord() {
        return teleVM().layoutScheme().generalLayout.readMisc(reference);
    }

    /**
     *  Gets the fields for either a tuple or hybrid object, returns empty set for arrays.
     *  Returns static fields in the special case of a {@link StaticTuple} object.
     */
    public Set<FieldActor> getFieldActors() {
        final Set<FieldActor> instanceFieldActors = new HashSet<FieldActor>();
        collectInstanceFieldActors(classActorForType(), instanceFieldActors);
        return instanceFieldActors;
    }

    /**
     * @param fieldActor
     * @return current memory region occupied by a field in this object in the VM
     */
    public final MemoryRegion getCurrentMemoryRegion(FieldActor fieldActor) {
        final Pointer start = getCurrentOrigin().plus(fieldActor.offset());
        final Size size = getFieldSize(fieldActor);
        return new FixedMemoryRegion(start, size, "");
    }

    /**
     * Gathers all instance fields for a class, including inherited fields.
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
     * Gets the current memory address of a field in the object.
     *
     * @param fieldActor descriptor for a field in this class
     * @return the current location in memory of the field in this object
     */
    public abstract Address getFieldAddress(FieldActor fieldActor);

    /**
     * @param fieldActor descriptor for a field in this class
     * @return the memory size of the field
     */
    protected abstract Size getFieldSize(FieldActor fieldActor);

    /**
     * @param fieldActor local {@link FieldActor}, part of the {@link ClassActor} for the type of this object, that
     *            describes a field in this object in the {@link TeleVM}
     * @return contents of the designated field in this object in the {@link TeleVM}
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

        /**
         * Registers a newly copied object in the context to avoid duplication.
         */
        protected void register(TeleObject teleObject, Object newObject) {
            Object oldValue = teleObjectToObject.put(teleObject, newObject);
            int numberOfCopies = numberOfCopies();
            if (oldValue == null) {
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
         * Updates the field of an object or class from the {@link TeleVM}.
         *
         * @param teleObject surrogate for a tuple in the {@link TeleVM}. This will be a static tuple if the field is static.
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
                    if (fieldActor.kind == Kind.REFERENCE) {
                        final TeleObject teleFieldReferenceObject = teleObject.teleVM().makeTeleObject(value.asReference());
                        if (teleFieldReferenceObject == null) {
                            newJavaValue = null;
                        } else {
                            newJavaValue = makeDeepCopy(fieldActor, teleFieldReferenceObject);
                        }
                    } else if (fieldActor.kind == Kind.WORD) {
                        final Class<Class< ? extends Word>> type = null;
                        final Class< ? extends Word> wordType = StaticLoophole.cast(type, fieldActor.toJava().getType());
                        newJavaValue = value.asWord().as(wordType);
                    } else {
                        newJavaValue = value.asBoxedJavaValue();
                    }
                    field.set(newTuple, newJavaValue);
                } catch (IllegalAccessException illegalAccessException) {
                    ProgramError.unexpected("could not access field: " + field, illegalAccessException);
                }
            }
        }

        protected Object makeDeepCopy(FieldActor fieldActor, TeleObject teleObject) {
            return teleObject.makeDeepCopy(this);
        }
    }

    /**
     * @return produces a deep copy of an object as part of
     * a larger deep copy in which this particular object may have
     * already been copied.
     */
    protected final Object makeDeepCopy(DeepCopier context) {
        Object newObject = context.teleObjectToObject.get(this);
        if (newObject == null) {
            context.level++;
            newObject = createDeepCopy(context);
            context.register(this, newObject);
            context.level--;
        }
        return newObject;
    }

    /**
     * @return creates a local deep copy of the object, using Maxine-specific shortcuts when
     * possible to produce a local equivalent without copying.
     * Implementations that copy recursively must call {@link TeleObject#makeDeepCopy(DeepCopier)},
     * and must register newly allocated objects before doing so.  This will result in redundant registrations
     * in those cases.
     */
    protected abstract Object createDeepCopy(DeepCopier context);

    /**
     * @return a best effort deep copy - with certain substitutions
     */
    public final Object deepCopy() {
        Trace.begin(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        long start = System.currentTimeMillis();
        DeepCopier copier = new DeepCopier();
        final Object objectCopy = makeDeepCopy(copier);
        Trace.end(COPY_TRACE_VALUE, "Deep copying from VM: " + this + " [" + copier.numberOfCopies() + " objects]", start);
        return objectCopy;
    }

    /**
     * @return a best effort deep copy - with certain substitutions, and with
     * certain specified field omissions.
     */
    public final Object deepCopy(DeepCopier copier) {
        Trace.begin(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        long start = System.currentTimeMillis();
        final Object objectCopy = makeDeepCopy(copier);
        Trace.end(COPY_TRACE_VALUE, "Deep copying from VM: " + this + " [" + copier.numberOfCopies() + " objects]", start);
        return objectCopy;
    }

    /**
     * Updates the static fields of a specified local class from the {@link TeleVM}.
     */
    public static void copyStaticFields(TeleVM teleVM, Class javaClass) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final TeleClassActor teleClassActor = teleVM.findTeleClassActor(javaClass);
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
        return teleVM().findTeleClassActor(classActorForType().typeDescriptor);
    }
}
