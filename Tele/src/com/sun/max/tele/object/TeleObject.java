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

import sun.misc.*;

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Canonical surrogate for a heap object in the tele VM.
 *
 * This class and its subclasses play the role of typed wrappers for References to heap objects in the tele VM,
 * encapsulating implementation details for working with those objects remotely.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleObject extends TeleVMHolder implements ObjectProvider {

    protected static final int COPY_TRACE_VALUE = 4;

    /**
     * Map: Reference to {@link Object}s in the target VM --> canonical local {@link TeleObject} that represents the
     * object in the target VM. Relies on References being canonical and GC-safe.
     */
    private static final GrowableMapping<Reference, TeleObject> _referenceToTeleObject = HashMapping.createIdentityMapping();

    /**
     * Map from OID to TeleObject.
     */
    private static final GrowableMapping<Long, TeleObject> _oidToTeleObject = HashMapping.createEqualityMapping();

    protected static final Unsafe _unsafe = (Unsafe) WithoutAccessCheck.getStaticField(Unsafe.class, "theUnsafe");

    // TODO (mlvdv)  TeleObject weak references
    /**
     * Factory method for canonical {@link TeleObject} surrogate for heap objects in the teleVM. Special subclasses are
     * created for Maxine implementation objects of special interest, and for other objects for which special treatment
     * is desired.
     *
     * Returns null for the distinguished zero {@link Reference}.
     *
     * Care is taken to avoid I/O with the tele VM during synchronized
     * access to the canonicalization map.  There is a small exception
     * to this for {@link TeleTargetMethod}.
     *
     * @param teleVM the tele VM in which the object resides
     * @param reference a Java object in the tele VM
     * @return canonical local surrogate for the object
     */
    public static TeleObject make(TeleVM teleVM, Reference reference) {
        assert reference != null;
        if (reference.isZero()) {
            return null;
        }
        TeleObject teleObject = null;
        synchronized (_referenceToTeleObject) {
            teleObject = _referenceToTeleObject.get(reference);
        }
        if (teleObject != null) {
            return teleObject;
        }
        // Keep all the tele VM traffic outside of synchronization.
        if (!teleVM.isValidOrigin(reference.toOrigin())) {
            return null;
        }

        final Reference hubReference = teleVM.wordToReference(teleVM.layoutScheme().generalLayout().readHubReferenceAsWord(reference));
        final Reference classActorReference = teleVM.fields().Hub_classActor.readReference(hubReference);
        final ClassActor classActor = teleVM.makeClassActor(classActorReference);

        // Must check for the static tuple case first; it doesn't follow the usual rules
        final Reference hubhubReference = teleVM.wordToReference(teleVM.layoutScheme().generalLayout().readHubReferenceAsWord(hubReference));
        final Reference hubClassActorReference = teleVM.fields().Hub_classActor.readReference(hubhubReference);
        final ClassActor hubClassActor = teleVM.makeClassActor(hubClassActorReference);
        final Class hubJavaClass = hubClassActor.toJava();  // the class of this object's hub
        if (StaticHub.class.isAssignableFrom(hubJavaClass)) {
            teleObject = new TeleStaticTuple(teleVM, reference);
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {
                    teleObject = new TeleStaticTuple(teleVM, reference);
                }
            }
        } else if (classActor.isArrayClassActor()) {
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {
                    teleObject = new TeleArrayObject(teleVM, reference);
                }
            }
        } else if (classActor.isHybridClassActor()) {
            final Class javaClass = classActor.toJava();
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {
                    if (DynamicHub.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleDynamicHub(teleVM, reference);
                    } else if (StaticHub.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleStaticHub(teleVM, reference);
                    } else {
                        Problem.error("invalid hybrid implementation type");
                    }
                }
            }
        } else if (classActor.isTupleClassActor()) {
            final Class javaClass = classActor.toJava(); // the class of this object
            synchronized (_referenceToTeleObject) {
                // Check map again, just in case there's a race
                teleObject = _referenceToTeleObject.get(reference);
                if (teleObject == null) {

                    // Some common Java classes
                    if (String.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleString(teleVM, reference);

                    } else if (Enum.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleEnum(teleVM, reference);

                    } else if (ClassLoader.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleClassLoader(teleVM, reference);

                        // Maxine Actors
                    } else if (FieldActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleFieldActor(teleVM, reference);

                    } else if (VirtualMethodActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleVirtualMethodActor(teleVM, reference);

                    } else if (StaticMethodActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleStaticMethodActor(teleVM, reference);

                    } else if (InterfaceMethodActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleInterfaceMethodActor(teleVM, reference);

                    } else if (InterfaceActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleInterfaceActor(teleVM, reference);

                    } else if (VmThread.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleVmThread(teleVM, reference);

                    } else if (PrimitiveClassActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TelePrimitiveClassActor(teleVM, reference);

                    } else if (ArrayClassActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleArrayClassActor(teleVM, reference);

                    } else if (ReferenceClassActor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleReferenceClassActor(teleVM, reference);

                        // Maxine code management
                    } else if (JitTargetMethod.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleJitTargetMethod(teleVM, reference);

                    } else if (OptimizedTargetMethod.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleOptimizedTargetMethod(teleVM, reference);

                    } else if (RuntimeStub.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleRuntimeStub(teleVM, reference);

                    } else if (CodeRegion.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleCodeRegion(teleVM, reference);

                    } else if (CodeManager.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleCodeManager(teleVM, reference);

                    } else if (RuntimeMemoryRegion.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleRuntimeMemoryRegion(teleVM, reference);

                     // Other Maxine support
                    } else if (Kind.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleKind(teleVM, reference);

                    } else if (ObjectReferenceValue.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleObjectReferenceValue(teleVM, reference);

                    } else if (Builtin.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleBuiltin(teleVM, reference);

                       // ConstantPool and PoolConstants
                    } else if (ConstantPool.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleConstantPool(teleVM, reference);

                    } else if (CodeAttribute.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleCodeAttribute(teleVM, reference);

                    } else if (Utf8Constant.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleUtf8Constant(teleVM, reference);

                    } else if (StringConstant.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleStringConstant(teleVM, reference);

                    } else if (ClassConstant.Resolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleClassConstant.Resolved(teleVM, reference);

                    } else if (ClassConstant.Unresolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleClassConstant.Unresolved(teleVM, reference);

                    } else if (FieldRefConstant.Resolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleFieldRefConstant.Resolved(teleVM, reference);

                    } else if (FieldRefConstant.Unresolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleFieldRefConstant.Unresolved(teleVM, reference);

                    } else if (FieldRefConstant.UnresolvedIndices.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleFieldRefConstant.UnresolvedIndices(teleVM, reference);

                    } else if (ClassMethodRefConstant.Resolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleClassMethodRefConstant.Resolved(teleVM, reference);

                    } else if (ClassMethodRefConstant.Unresolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleClassMethodRefConstant.Unresolved(teleVM, reference);

                    } else if (ClassMethodRefConstant.UnresolvedIndices.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleClassMethodRefConstant.UnresolvedIndices(teleVM, reference);

                    } else if (InterfaceMethodRefConstant.Resolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleInterfaceMethodRefConstant.Resolved(teleVM, reference);

                    } else if (InterfaceMethodRefConstant.Unresolved.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleInterfaceMethodRefConstant.Unresolved(teleVM, reference);

                    } else if (InterfaceMethodRefConstant.UnresolvedIndices.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleInterfaceMethodRefConstant.UnresolvedIndices(teleVM, reference);

                    } else if (PoolConstant.class.isAssignableFrom(javaClass)) {
                        // General PoolConstant, not handled specially
                        teleObject = new TelePoolConstant(teleVM, reference);

                        // java.lang.reflect objects
                    } else if (Class.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleClass(teleVM, reference);

                    } else if (Constructor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleConstructor(teleVM, reference);

                    } else if (Field.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleField(teleVM, reference);

                    } else if (Method.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleMethod(teleVM, reference);

                    } else if (TypeDescriptor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleTypeDescriptor(teleVM, reference);

                    } else if (SignatureDescriptor.class.isAssignableFrom(javaClass)) {
                        teleObject = new TeleSignatureDescriptor(teleVM, reference);

                        // all other object instances
                    } else {
                        teleObject = new TeleTupleObject(teleVM, reference);
                    }
                }
            }
        } else {
            Problem.error("invalid object implementation type");
        }

        _oidToTeleObject.put(teleObject.getOID(), teleObject);
        assert _oidToTeleObject.containsKey(teleObject.getOID());
        return teleObject;
    }

    public static TeleObject lookupObject(long id) {
        return _oidToTeleObject.get(id);
    }

    // The static factory method ensures synchronized TeleObjects creation
    protected TeleObject(TeleVM teleVM, Reference reference) {
        super(teleVM);
        _reference = (TeleReference) reference;
        _oid = _reference.makeOID();
        _referenceToTeleObject.put(reference, this);
    }

    private final TeleReference _reference;

    /**
     * @return canonical reference to this object in the teleVM
     */
    public TeleReference reference() {
        return _reference;
    }

    /**
     * @return current absolute location of the beginning of the object's memory allocation in the tele VM,
     * subject to relocation by GC.
     */
    public Pointer getCurrentCell() {
        return teleVM().referenceToCell(_reference);
    }

    /**
     * @return the current size of memory allocated for the object in the tele VM.
     */
    public Size getCurrentSize() {
        return classActorForType().dynamicTupleSize();
    }

    /**
     * @return current absolute location of the object's origin (not necessarily beginning of memory)
     *  in the tele VM, subject to relocation by GC
     */
    public Pointer getCurrentOrigin() {
        return _reference.toOrigin();
    }

    private final long _oid;

    /**
     * @return a number that uniquely identifies this object in the tele VM for the duration of the inspection
     */
    public long getOID() {
        return _oid;
    }

    @Override
    public String toString() {
        return getClass().toString() + "<" + _oid + ">";
    }

    private TeleHub _teleHub = null;

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
     * @return the local surrogate for the Hub of this object
     */
    public TeleHub getTeleHub() {
        if (_teleHub == null) {
            final Reference hubReference = teleVM().wordToReference(teleVM().layoutScheme().generalLayout().readHubReferenceAsWord(_reference));
            _teleHub = (TeleHub) make(teleVM(), hubReference);
        }
        return _teleHub;
    }

    /**
     * @return the "misc" word from the header of this object in the teleVM
     */
    public Word getMiscWord() {
        return teleVM().layoutScheme().generalLayout().readMisc(_reference);
    }

    /**
     * @return local {@link ClassActor}, equivalent to the one in the teleVM that describes the type
     * of this object in the tele VM.
     * Note that in the singular instance of {@link StaticTuple} this does not correspond to the actual type of the
     * object, which is an exceptional Maxine object that has no ordinary Java type; it returns in this case
     * the type of the class that the tuple helps implement.
     */
    public ClassActor classActorForType() {
        return getTeleHub().getTeleClassActor().classActor();
    }

    /**
     * return local surrogate for the{@link ClassMethodActor} associated with this object in the tele VM, either
     * because it is a {@link ClassMethodActor} or because it is a class closely associated with a method that refers to
     * a {@link ClassMethodActor}. Null otherwise.
     */
    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        return null;
    }

    /**
     * @param fieldActor local {@link FieldActor}, part of the {@link ClassActor} for the type of this object, that
     *            describes a field in this object in the tele VM
     * @return contents of the designated field in this object in the tele VM
     */
    public abstract Value readFieldValue(FieldActor fieldActor);

    /**
     * @return a shallow copy of the object in the teleVM, with any references in it nulled out
     */
    public abstract Object shallowCopy();

    /**
     * Filter for pruning the object graph copied during a {@linkplain TeleObject#deepCopy}.
     */
    protected static interface FieldIncludeChecker {

        /**
         * Determines if a given field is to be traversed and copied during a deep copy.
         *
         * @param level  the depth of the sub-graph currently being copied
         * @param fieldActor  the field to be queried
         */
        boolean include(int level, FieldActor fieldActor);
    }

    protected static final class DeepCopyContext {

        private int _level = 0;
        private final FieldIncludeChecker _fieldIncludeChecker;
        private final Map<TeleObject, Object> _teleObjectToObject = new HashMap<TeleObject, Object>();

        private static final FieldIncludeChecker _defaultIFieldIncludeChecker = new FieldIncludeChecker() {
            public boolean include(int level, FieldActor fieldActor) {
                return true;
            }
        };

        /**
         * Creates a context for a deep copy.
         */
        protected DeepCopyContext() {
            _fieldIncludeChecker = _defaultIFieldIncludeChecker;
        }

        /**
         * Creates a context for a deep copy in which a filter suppresses copying of specified fields.
         */
        protected DeepCopyContext(FieldIncludeChecker fieldIncludeChecker) {
            _fieldIncludeChecker = fieldIncludeChecker;
        }

        /**
         * @return the depth of the object graph currently being copied
         */
        protected int level() {
            return _level;
        }

        /**
         * Registers a newly copied object in the context to avoid duplication.
         */
        protected void register(TeleObject teleObject, Object newObject) {
            _teleObjectToObject.put(teleObject, newObject);
        }

        /**
         * @return whether the specified object field at this level of the object graph should be copied.
         */
        protected boolean include(int level, FieldActor fieldActor) {
            return _fieldIncludeChecker.include(level, fieldActor);
        }

    }

    /**
     * @return produces a deep copy of an object as part of
     * a larger deep copy in which this particular object may have
     * already been copied.
     */
    protected final Object makeDeepCopy(DeepCopyContext context) {
        Object newObject = context._teleObjectToObject.get(this);
        if (newObject == null) {
            context._level++;
            newObject = createDeepCopy(context);
            context.register(this, newObject);
            context._level--;
        }
        return newObject;
    }

    /**
     * @return creates a local deep copy of the object, using Maxine-specific shortcuts when
     * possible to produce a local equivalent without copying.
     * Implementations that copy recursively must call {@link TeleObject#makeDeepCopy(DeepCopyContext)},
     * and must register newly allocated objects before doing so.  This will result in redundant registrations
     * in those cases.
     */
    protected abstract Object createDeepCopy(DeepCopyContext context);

    /**
     * @return a best effort deep copy - with certain substitutions
     */
    public final Object deepCopy() {
        Trace.begin(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        final Object objectCopy = makeDeepCopy(new DeepCopyContext());
        Trace.end(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        return objectCopy;
    }

    /**
     * @return a best effort deep copy - with certain substitutions, and with
     * certain specified field omissions.
     */
    public final Object deepCopy(FieldIncludeChecker fieldIncludeChecker) {
        Trace.begin(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        final Object objectCopy = makeDeepCopy(new DeepCopyContext(fieldIncludeChecker));
        Trace.end(COPY_TRACE_VALUE, "Deep copying from VM: " + this);
        return objectCopy;
    }

    /**
     * Updates the field of an object or class from the tele VM.
     *
     * @param teleObject surrogate for a tuple in the tele VM. This will be a static tuple if the field is static.
     * @param tuple the local object to be updated in the host VM. This value is ignored if the field is static.
     * @param fieldActor the field to be copied/updated
     */
    protected static final void copyField(DeepCopyContext context, final TeleObject teleObject, final Object newTuple, final FieldActor fieldActor) throws TeleError {
        if (context.include(context.level(), fieldActor)) {
            if (!fieldActor.isInjected()) {
                final Field field = fieldActor.toJava();
                field.setAccessible(true);
                try {
                    final Value value = teleObject.readFieldValue(fieldActor);
                    final Object newJavaValue;
                    if (fieldActor.kind() == Kind.REFERENCE) {
                        final TeleObject teleFieldReferenceObject = TeleObject.make(teleObject.teleVM(), value.asReference());
                        if (teleFieldReferenceObject == null) {
                            newJavaValue = null;
                        } else {
                            newJavaValue = teleFieldReferenceObject.makeDeepCopy(context);
                        }
                    } else if (fieldActor.kind() == Kind.WORD) {
                        final Class<Class< ? extends Word>> type = null;
                        final Class< ? extends Word> wordType = StaticLoophole.cast(type, fieldActor.toJava().getType());
                        newJavaValue = value.asWord().as(wordType);
                    } else {
                        newJavaValue = value.asBoxedJavaValue();
                    }
                    field.set(newTuple, newJavaValue);
                } catch (IllegalAccessException illegalAccessException) {
                    throw new TeleError("could not access field: " + field, illegalAccessException);
                }
            }
        }
    }

    /**
     * Updates the static fields of a specified local class from the tele VM.
     */
    public static void copyStaticFields(TeleVM teleVM, Class javaClass) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final TeleClassActor teleClassActor = teleVM.teleClassRegistry().findTeleClassActorByClass(javaClass);
        final TeleStaticTuple teleStaticTuple = teleClassActor.getTeleStaticTuple();

        Trace.begin(COPY_TRACE_VALUE, "Copying static fields of " + javaClass + "from VM");
        try {
            for (FieldActor fieldActor : classActor.localStaticFieldActors()) {
                copyField(new DeepCopyContext(), teleStaticTuple, null, fieldActor);
            }
        } finally {
            Trace.end(COPY_TRACE_VALUE, "Copying static fields of " + javaClass + "from VM");
        }
    }



    public Reference getReference() {
        return this.reference();
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return teleVM().teleClassRegistry().findTeleClassActor(classActorForType());
    }
}
