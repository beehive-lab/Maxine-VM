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
package com.sun.max.vm.actor.holder;

import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.type.ClassRegistry.Property.*;

import java.io.*;
import java.lang.annotation.*;
import java.security.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.reflection.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
 * Internal representation of anything that has an associated instance of 'java.lang.Class'.
 * Every such "Java class" has a specific super class (in the JLS sense) and
 * a mirror, i.e. an instance of java.lang.Class reflecting on it.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class ClassActor extends Actor {

    public static final Deferrable.Queue DEFERRABLE_QUEUE_1 = Deferrable.createDeferred();
    public static final Deferrable.Queue DEFERRABLE_QUEUE_2 = Deferrable.createDeferred();

    public static final char NO_MAJOR_VERSION = (char) -1;
    public static final char NO_MINOR_VERSION = (char) -1;
    public static final SpecificLayout NO_SPECIFIC_LAYOUT = null;
    public static final TupleClassActor NO_SUPER_CLASS_ACTOR = null;
    public static final ClassActor NO_COMPONENT_CLASS_ACTOR = null;
    public static final TypeDescriptor[] NO_INNER_CLASSES = null;
    public static final TypeDescriptor NO_OUTER_CLASS = null;
    public static final String NO_SOURCE_FILE_NAME = null;
    public static final EnclosingMethodInfo NO_ENCLOSING_METHOD_INFO = null;

    public static final InterfaceActor[] NO_INTERFACES = new InterfaceActor[0];
    public static final FieldActor[] NO_FIELDS = new FieldActor[0];
    public static final MethodActor[] NO_METHODS = new MethodActor[0];
    public static final StaticMethodActor[] NO_STATIC_METHODS = new StaticMethodActor[0];
    public static final VirtualMethodActor[] NO_VIRTUAL_METHODS = new VirtualMethodActor[0];
    public static final InterfaceMethodActor[] NO_INTERFACE_METHODS = new InterfaceMethodActor[0];
    public static final TypeDescriptor[] NO_TYPE_DESCRIPTORS = new TypeDescriptor[0];

    /**
     * Unique class actor identifier. Simplifies the implementation of type checking, interface dispatch, etc.
     */
    @INSPECTED
    public final int id;

    @INSPECTED
    public final ClassLoader classLoader;

    @INSPECTED
    public final TypeDescriptor typeDescriptor;

    @INSPECTED
    @CONSTANT_WHEN_NOT_ZERO
    private Class mirror;

    public final ClassActor superClassActor;

    public final char majorVersion;

    public final char minorVersion;

    private final InterfaceActor[] localInterfaceActors;

    @INSPECTED
    private final FieldActor[] localInstanceFieldActors;

    @CONSTANT
    @INSPECTED
    private Object staticTuple;

    @INSPECTED
    private final FieldActor[] localStaticFieldActors;

    private int[] arrayClassIDs;

    @INSPECTED
    private final ClassActor componentClassActor;

    /**
     * A lazily initialized value holding the type representing an 1-dimensional array of this type.
     */
    ArrayClassActor arrayClassActor;

    public Object[] signers;

    private ProtectionDomain protectionDomain;

    /**
     * An object representing the initialization state of this class. This value will either be one of the sentinel
     * objects representing a state (i.e. {@link #VERIFIED}, {@link #PREPARED}, {@link #INITIALIZED}) or be an object
     * whose type denotes the state ({@code Throwable == ERROR}, {@code Thread == INITIALIZING}) and whose value gives
     * further details about the state.
     */
    private Object initializationState;

    public final Kind kind;

    protected ClassActor(Kind kind,
                         final SpecificLayout specificLayout,
                         ClassLoader classLoader,
                         Utf8Constant name,
                         char majorVersion,
                         char minorVersion,
                         int flags,
                         TypeDescriptor typeDescriptor,
                         ClassActor superClassActor,
                         ClassActor componentClassActor,
                         InterfaceActor[] interfaceActors,
                         FieldActor[] fieldActors,
                         MethodActor[] methodActors,
                         Utf8Constant genericSignature,
                         byte[] runtimeVisibleAnnotationsBytes,
                         String sourceFileName,
                         TypeDescriptor[] innerClasses,
                         TypeDescriptor outerClass,
                         EnclosingMethodInfo enclosingMethodInfo) {
        super(name, flags);
        if (MaxineVM.isHosted()) {
            checkProhibited(name);
            if (MaxineVM.isMaxineClass(typeDescriptor)) {
                initializationState = INITIALIZED;
            } else {
                // TODO: At some point, it may be worth trying to put JDK classes into the image in the VERIFIED state
                // so that their class initializers are run at the 'right time' (i.e. according to the JVM spec).
                // This solves the issue of having to clear/re-initialize static fields at runtime whose values
                // depend on the runtime context, not the image build time context.
                // However, it also raises other issues such as what it means to have instances in existence for
                // classes that will be re-initialized. Also, all code in the boot image will need to have
                // the appropriate class initialization barriers (that would be required if the same code
                // was compiled at runtime).
                initializationState = INITIALIZED;
            }

        } else {
            initializationState = PREPARED;
        }

        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.kind = kind;
        this.componentClassActor = componentClassActor;
        this.id = elementClassActor().makeID(numberOfDimensions());
        ClassID.register(id, this);
        this.typeDescriptor = typeDescriptor;
        this.superClassActor = superClassActor;
        this.sourceFileName = sourceFileName;
        assert classLoader != null;
        this.classLoader = classLoader;

        FatalError.check(classLoader != null, "Class loader cannot be null for class actor " + name);

        final ClassRegistry classRegistry = classRegistry();
        classRegistry.set(ENCLOSING_METHOD_INFO, this, enclosingMethodInfo);
        classRegistry.set(GENERIC_SIGNATURE, this, genericSignature);
        classRegistry.set(RUNTIME_VISIBLE_ANNOTATION_BYTES, this, runtimeVisibleAnnotationsBytes);
        classRegistry.set(INNER_CLASSES, this, innerClasses);

        if (outerClass != null) {
            assert isInnerClass(flags);
            final String outerClassName = outerClass.toJavaString();
            final String thisClassName = typeDescriptor.toJavaString();

            if (!canDeriveOuterClassFromInnerClassName(outerClassName, thisClassName)) {
                classRegistry.set(OUTER_CLASS, this, outerClass);
            }
        } else {
            assert !isInnerClass(flags());
        }

        this.localInterfaceActors = interfaceActors;

        this.localStaticMethodActors = Arrays.filter(methodActors, StaticMethodActor.class, NO_STATIC_METHODS);
        this.localVirtualMethodActors = Arrays.filter(methodActors, VirtualMethodActor.class, NO_VIRTUAL_METHODS);
        this.localInterfaceMethodActors = Arrays.filter(methodActors, InterfaceMethodActor.class, NO_INTERFACE_METHODS);

        this.localStaticFieldActors = InjectedFieldActor.Static.injectFieldActors(true, Arrays.filter(fieldActors, Actor.staticPredicate, NO_FIELDS), typeDescriptor);
        this.localInstanceFieldActors = InjectedFieldActor.Static.injectFieldActors(false, Arrays.filter(fieldActors, Actor.dynamicPredicate, NO_FIELDS), typeDescriptor);

        this.clinit = findLocalStaticMethodActor(SymbolTable.CLINIT);

        assignHolderToLocalFieldActors();

        // A map for matching methods based on their name and descriptor (and not holder)
        final int maxVtableSize = computeMaxVTableSize(superClassActor, localVirtualMethodActors);
        final GrowableMapping<MethodActor, VirtualMethodActor> methodLookup = new ChainedHashMapping<MethodActor, VirtualMethodActor>(maxVtableSize) {
            @Override
            public boolean equivalent(MethodActor methodActor1, MethodActor methodActor2) {
                return methodActor1.matchesNameAndType(methodActor2.name, methodActor2.descriptor());
            }
            @Override
            public int hashCode(MethodActor methodActor) {
                return methodActor.name.hashCode() ^ methodActor.descriptor().hashCode();
            }
        };

        new Deferrable(DEFERRABLE_QUEUE_1) {
            public void run() {
                final Size staticTupleSize = Layout.tupleLayout().layoutFields(NO_SUPER_CLASS_ACTOR, localStaticFieldActors);
                final TupleReferenceMap staticReferenceMap = new TupleReferenceMap(localStaticFieldActors);
                final StaticHub sHub = new StaticHub(staticTupleSize, ClassActor.this, staticReferenceMap);
                ClassActor.this.staticHub = sHub.expand(staticReferenceMap, getRootClassActorId());
                ClassActor.this.staticTuple = StaticTuple.create(ClassActor.this);

                final IdentityHashSet<InterfaceActor> allInterfaceActors = getAllInterfaceActors();
                Sequence<VirtualMethodActor> virtualMethodActors = gatherVirtualMethodActors(allInterfaceActors, methodLookup);
                ClassActor.this.allVirtualMethodActors = Sequence.Static.toArray(virtualMethodActors, new VirtualMethodActor[virtualMethodActors.length()]);
                assignHolderToLocalMethodActors();
                if (isReferenceClassActor() || isInterfaceActor()) {
                    final Size dynamicTupleSize = layoutFields(specificLayout);
                    final BitSet superClassActorSerials = getSuperClassActorSerials();
                    TupleReferenceMap dynamicReferenceMap;
                    int vTableLength;

                    if (isReferenceClassActor()) {
                        dynamicReferenceMap = new TupleReferenceMap(ClassActor.this);
                        vTableLength = ClassActor.this.allVirtualMethodActors.length;
                    } else {
                        assert isInterfaceActor();
                        dynamicReferenceMap = TupleReferenceMap.EMPTY;
                        vTableLength = 0;
                    }

                    final DynamicHub dHub = new DynamicHub(dynamicTupleSize, specificLayout, ClassActor.this, superClassActorSerials, allInterfaceActors, vTableLength, dynamicReferenceMap);
                    ClassActor.this.iToV = new int[dHub.iTableLength];
                    ClassActor.this.dynamicHub = dHub.expand(superClassActorSerials, allInterfaceActors, methodLookup, iToV, dynamicReferenceMap);
                }
            }
        };

        if (isReferenceClassActor()) {
            new Deferrable(DEFERRABLE_QUEUE_2) {
                public void run() {
                    dynamicHub.initializeVTable(allVirtualMethodActors);
                    dynamicHub.initializeITable(getAllInterfaceActors(), methodLookup);
                }
            };
        }
    }

    private int getRootClassActorId() {
        ClassActor root = this;
        while (root.superClassActor != null) {
            root = root.superClassActor;
        }
        return root.id;
    }

    private static int computeMaxVTableSize(ClassActor superClassActor, VirtualMethodActor[] localVirtualMethodActors) {
        int maxVTableSize = localVirtualMethodActors.length;
        if (superClassActor != null && superClassActor.allVirtualMethodActors != null) {
            maxVTableSize += superClassActor.allVirtualMethodActors.length;
        }
        return maxVTableSize;
    }

    /**
     * Determines if the name of an inner class is derived from the name of the outer class by appending '$' and the
     * non-qualified name of the inner class (which does not contain any subsequent '$' characters).
     */
    private static boolean canDeriveOuterClassFromInnerClassName(String outerClassName, String innerClassName) {
        if (innerClassName.startsWith(outerClassName)) {
            final String suffix = innerClassName.substring(outerClassName.length());
            if (suffix.lastIndexOf('$') == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isPrimitiveClassActor() {
        return false;
    }

    @INLINE
    public final boolean isInterfaceActor() {
        return isInterface(flags());
    }

    @INLINE
    public final boolean isTemplate() {
        return isTemplate(flags());
    }

    public final boolean isGenerated() {
        return isGenerated(flags());
    }

    public final boolean isRemote() {
        return isRemote(flags());
    }

    @INLINE
    public final boolean isArrayClassActor() {
        return this instanceof ArrayClassActor;
    }

    @INLINE
    public final boolean isTupleClassActor() {
        return this instanceof TupleClassActor;
    }

    @INLINE
    public final boolean isHybridClassActor() {
        return this instanceof HybridClassActor;
    }

    @INLINE
    public final boolean isSpecialReference() {
        return isSpecialReference(flags());
    }

    @INLINE
    public final boolean hasFinalizer() {
        return hasFinalizer(flags());
    }

    /**
     * Gets the component type of the array type represented by this actor. If this actor does not represent an array,
     * then {@code null} is returned.
     * <p>
     * The definition of an array's component type is taken from <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/arrays.doc.html">Chapter 10</a> of the Java
     * Language specification:
     * <p>
     * <blockquote> An array object contains a number of variables. The number of variables may
     * be zero, in which case the array is said to be empty. The variables contained in an array have no names; instead
     * they are referenced by array access expressions that use nonnegative integer index values. These variables are
     * called the components of the array. If an array has n components, we say n is the length of the array; the
     * components of the array are referenced using integer indices from 0 to n - 1, inclusive.
     * <p>
     * All the components of an array have the same type, called the component type of the array. If the component type
     * of an array is T, then the type of the array itself is written T[]. </blockquote>
     *
     * @see #elementClassActor()
     */
    @INLINE
    public final ClassActor componentClassActor() {
        return componentClassActor;
    }

    /**
     * Gets the element type of the type represented by this actor.  If this actor does not represent an array,
     * then this object is returned. Otherwise, the zero dimensional type corresponding to this array type is returned.
     * The following diagram clarifies the difference between the {@linkplain #componentClassActor() component} and
     * {@linkplain #elementClassActor() element} types of a given type:
     * <pre>
     * Type         Component         Element
     * --------------------------------------
     * A[][][]      A[][]             A
     * A[][]        A[]               A
     * A[]          A                 A
     * A            null              A
     * </pre>
     *
     * @see #componentClassActor()
     */
    public final ClassActor elementClassActor() {
        if (componentClassActor == null) {
            return this;
        }
        return componentClassActor.elementClassActor();
    }

    /**
     * Gets the number of array dimensions represented by this class actor.
     *
     * @return the number of array dimensions represented by this class actor or {@code 0} if this class actor is not an
     *         array class actor
     */
    public final int numberOfDimensions() {
        if (componentClassActor == null) {
            return 0;
        }
        return componentClassActor().numberOfDimensions() + 1;
    }

    protected final synchronized int makeID(int numberOfDimensions) {
        if (numberOfDimensions <= 0) {
            return ClassID.create();
        }

        if (arrayClassIDs == null) {
            arrayClassIDs = new int[numberOfDimensions];
            for (int i = 0; i < numberOfDimensions; i++) {
                arrayClassIDs[i] = ClassID.create();
            }
        } else if (arrayClassIDs.length < numberOfDimensions) {
            final int[] a = new int[numberOfDimensions];
            Ints.copyAll(arrayClassIDs, a);
            for (int i = arrayClassIDs.length; i < a.length; i++) {
                a[i] = ClassID.create();
            }
            arrayClassIDs = a;
        }
        return arrayClassIDs[numberOfDimensions - 1];
    }

    @Override
    public Utf8Constant genericSignature() {
        return classRegistry().get(GENERIC_SIGNATURE, this);
    }

    @Override
    public byte[] runtimeVisibleAnnotationsBytes() {
        return classRegistry().get(RUNTIME_VISIBLE_ANNOTATION_BYTES, this);
    }

    public final InterfaceActor[] localInterfaceActors() {
        return localInterfaceActors;
    }

    @INLINE
    public final Object staticTuple() {
        return staticTuple;
    }

    public final FieldActor[] localStaticFieldActors() {
        return localStaticFieldActors;
    }

    public final FieldActor findLocalStaticFieldActor(Utf8Constant name) {
        for (FieldActor fieldActor : localStaticFieldActors) {
            if (fieldActor.name == name) {
                return fieldActor;
            }
        }
        return null;
    }

    public final FieldActor findLocalStaticFieldActor(String name) {
        for (FieldActor fieldActor : localStaticFieldActors) {
            if (fieldActor.name.toString().equals(name)) {
                return fieldActor;
            }
        }
        return null;
    }

    /**
     * Searches a given array of members for one matching a given name and signature.
     *
     * @param name the name of the member to find
     * @param descriptor the signature of the member to find. If this value is {@code null}, then the first member found
     *            based on {@code name} is returned.
     * @return the matching member or {@code null} in none is found
     */
    private static <MemberActor_Type extends MemberActor> MemberActor_Type findMemberActor(MemberActor_Type[] memberActors, Utf8Constant name, Descriptor descriptor) {
        for (MemberActor_Type memberActor : memberActors) {
            if (descriptor == null) {
                if (memberActor.name == name) {
                    return memberActor;
                }
            } else {
                if (memberActor.matchesNameAndType(name, descriptor)) {
                    return memberActor;
                }
            }
        }
        return null;

    }

    public final FieldActor findLocalStaticFieldActor(Utf8Constant name, TypeDescriptor descriptor) {
        return findMemberActor(localStaticFieldActors, name, descriptor);
    }

    public final FieldActor findLocalStaticFieldActor(int offset) {
        for (FieldActor fieldActor : localStaticFieldActors) {
            if (fieldActor.offset() == offset) {
                return fieldActor;
            }
        }
        return null;
    }

    public final FieldActor findStaticFieldActor(Utf8Constant name, TypeDescriptor descriptor) {
        ClassActor holder = this;
        do {
            final FieldActor result = holder.findLocalStaticFieldActor(name, descriptor);
            if (result != null) {
                return result;
            }
            for (InterfaceActor interfaceActor : holder.localInterfaceActors) {
                final FieldActor interfaceResult = interfaceActor.findStaticFieldActor(name, descriptor);
                if (interfaceResult != null) {
                    return interfaceResult;
                }
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    public final FieldActor findStaticFieldActor(int offset) {
        ClassActor holder = this;
        do {
            final FieldActor result = holder.findLocalStaticFieldActor(offset);
            if (result != null) {
                return result;
            }
            for (InterfaceActor interfaceActor : holder.localInterfaceActors) {
                final FieldActor interfaceResult = interfaceActor.findStaticFieldActor(offset);
                if (interfaceResult != null) {
                    return interfaceResult;
                }
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    public final FieldActor findStaticFieldActor(String name) {
        ClassActor holder = this;
        do {
            final FieldActor result = holder.findLocalStaticFieldActor(name);
            if (result != null) {
                return result;
            }
            for (InterfaceActor interfaceActor : holder.localInterfaceActors) {
                final FieldActor interfaceResult = interfaceActor.findStaticFieldActor(name);
                if (interfaceResult != null) {
                    return interfaceResult;
                }
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    public final FieldActor[] localInstanceFieldActors() {
        return localInstanceFieldActors;
    }

    public final FieldActor findLocalInstanceFieldActor(Utf8Constant name) {
        for (FieldActor fieldActor : localInstanceFieldActors()) {
            if (fieldActor.name == name) {
                return fieldActor;
            }
        }
        return null;
    }

    public final FieldActor findLocalInstanceFieldActor(String name) {
        for (FieldActor fieldActor : localInstanceFieldActors()) {
            if (fieldActor.name.toString().equals(name)) {
                return fieldActor;
            }
        }
        return null;
    }

    /**
     * Searches the {@linkplain #localInstanceFieldActors() instance} fields declared by this class actor for one whose
     * {@linkplain FieldActor#offset()} matches a given value.
     *
     * @param offset the offset to search by
     * @return the instance field declared by this class whose offset equals {@code offset} or {@code null} if not such
     *         field exists
     */
    public final FieldActor findLocalInstanceFieldActor(int offset) {
        for (FieldActor fieldActor : localInstanceFieldActors()) {
            if (fieldActor.offset() == offset) {
                return fieldActor;
            }
        }
        return null;
    }

    public final FieldActor findLocalInstanceFieldActor(Utf8Constant name, TypeDescriptor descriptor) {
        return findMemberActor(localInstanceFieldActors(), name, descriptor);
    }

    public final FieldActor findInstanceFieldActor(Utf8Constant name, TypeDescriptor descriptor) {
        ClassActor holder = this;
        do {
            final FieldActor result = holder.findLocalInstanceFieldActor(name, descriptor);
            if (result != null) {
                return result;
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    public final FieldActor findInstanceFieldActor(int offset) {
        ClassActor holder = this;
        do {
            final FieldActor result = holder.findLocalInstanceFieldActor(offset);
            if (result != null) {
                return result;
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    public final FieldActor findLocalFieldActor(Utf8Constant name, TypeDescriptor descriptor) {
        final FieldActor result = findLocalInstanceFieldActor(name, descriptor);
        if (result != null) {
            return result;
        }
        return findLocalStaticFieldActor(name, descriptor);
    }

    public FieldActor findFieldActor(Utf8Constant name) {
        FieldActor fieldActor;
        ClassActor holder = this;
        do {
            fieldActor = holder.findLocalInstanceFieldActor(name);
            if (fieldActor != null) {
                return fieldActor;
            }
            fieldActor = holder.findLocalStaticFieldActor(name);
            if (fieldActor != null) {
                return fieldActor;
            }
            for (InterfaceActor interfaceActor : holder.localInterfaceActors()) {
                fieldActor = interfaceActor.findFieldActor(name);
                if (fieldActor != null) {
                    return fieldActor;
                }
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    /**
     * Part of #5.4.3.2.
     */
    public FieldActor findFieldActor(Utf8Constant name, TypeDescriptor descriptor) {
        FieldActor fieldActor;
        ClassActor holder = this;
        do {
            fieldActor = holder.findLocalInstanceFieldActor(name, descriptor);
            if (fieldActor != null) {
                return fieldActor;
            }
            fieldActor = holder.findLocalStaticFieldActor(name, descriptor);
            if (fieldActor != null) {
                return fieldActor;
            }
            for (InterfaceActor interfaceActor : holder.localInterfaceActors()) {
                fieldActor = interfaceActor.findFieldActor(name, descriptor);
                if (fieldActor != null) {
                    return fieldActor;
                }
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    /**
     * The class initializer for this class or null if this class has not class initializer.
     */
    public final StaticMethodActor clinit;

    @INSPECTED
    private final StaticMethodActor[] localStaticMethodActors;

    public final StaticMethodActor[] localStaticMethodActors() {
        return localStaticMethodActors;
    }

    public final StaticMethodActor findLocalStaticMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        return findMemberActor(localStaticMethodActors, name, descriptor);
    }

    public final boolean forAllStaticMethodActors(Predicate<StaticMethodActor> predicate) {
        ClassActor classActor = this;
        do {
            for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
                if (!predicate.evaluate(staticMethodActor)) {
                    return false;
                }
            }
            classActor = classActor.superClassActor;
        } while (classActor != null);
        return true;
    }

    public final void forAllStaticMethodActors(Procedure<StaticMethodActor> procedure) {
        ClassActor classActor = this;
        do {
            for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
                procedure.run(staticMethodActor);
            }
            classActor = classActor.superClassActor;
        } while (classActor != null);
    }

    public final StaticMethodActor findStaticMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        ClassActor holder = this;
        do {
            final StaticMethodActor result = holder.findLocalStaticMethodActor(name, descriptor);
            if (result != null) {
                return result;
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    @INSPECTED
    private VirtualMethodActor[] localVirtualMethodActors;

    public final VirtualMethodActor[] localVirtualMethodActors() {
        return localVirtualMethodActors;
    }

    /**
     * Searches the {@linkplain #localVirtualMethodActors() virtual} methods in this class actor for one matching a
     * given name and signature.
     *
     * @param name the name of the method to find
     * @param descriptor the signature of the method to find. If this value is {@code null}, then the first matching
     *            method found based on {@code name} is returned.
     * @return the matching method or {@code null} in none is found
     */
    public final VirtualMethodActor findLocalVirtualMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        return findMemberActor(localVirtualMethodActors, name, descriptor);
    }

    public final VirtualMethodActor findLocalVirtualMethodActor(MethodActor declaredMethod) {
        return findLocalVirtualMethodActor(declaredMethod.name, declaredMethod.descriptor());
    }

    public final VirtualMethodActor findLocalVirtualMethodActor(Utf8Constant name) {
        for (VirtualMethodActor virtualMethodActor : localVirtualMethodActors) {
            if (virtualMethodActor.name.equals(name)) {
                return virtualMethodActor;
            }
        }
        return null;
    }

    public final VirtualMethodActor findLocalVirtualMethodActor(String name) {
        for (VirtualMethodActor virtualMethodActor : localVirtualMethodActors) {
            if (virtualMethodActor.name.toString().equals(name)) {
                return virtualMethodActor;
            }
        }
        return null;
    }

    @CONSTANT
    private VirtualMethodActor[] allVirtualMethodActors;

    public final VirtualMethodActor[] allVirtualMethodActors() {
        return allVirtualMethodActors;
    }

    public final boolean forAllVirtualMethodActors(Predicate<VirtualMethodActor> predicate) {
        for (VirtualMethodActor virtualMethodActor : allVirtualMethodActors) {
            if (!predicate.evaluate(virtualMethodActor)) {
                return false;
            }
        }
        return true;
    }

    public final void forAllVirtualMethodActors(Procedure<VirtualMethodActor> procedure) {
        for (VirtualMethodActor virtualMethodActor : allVirtualMethodActors) {
            procedure.run(virtualMethodActor);
        }
    }

    public final VirtualMethodActor findVirtualMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        return findMemberActor(allVirtualMethodActors, name, descriptor);
    }

    public final VirtualMethodActor findVirtualMethodActor(MethodActor declaredMethod) {
        return findVirtualMethodActor(declaredMethod.name, declaredMethod.descriptor());
    }

    public final VirtualMethodActor getVirtualMethodActorByVTableIndex(int vTableIndex) {
        return allVirtualMethodActors[vTableIndex - DynamicHub.vTableStartIndex()];
    }

    /**
     * @param iIndex the interface method index starting from the beginning of the iTable
     * @return VirtualMethodActor
     */
    public final VirtualMethodActor getVirtualMethodActorByIIndex(int iIndex) {
        return getVirtualMethodActorByVTableIndex(iToV[iIndex]);
    }

    /**
     * Searches the {@linkplain #localVirtualMethodActors() virtual} methods and {@linkplain #localStaticMethodActors()
     * static} methods actors (in that order) declared in this class actor for one matching a given name and signature.
     *
     * @param name the name of the method to find
     * @param descriptor the signature of the method to find. If this value is {@code null}, then the first method found
     *            based on {@code name} is returned.
     * @return the matching method or {@code null} in none is found
     */
    public final ClassMethodActor findLocalClassMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        final ClassMethodActor result = findLocalVirtualMethodActor(name, descriptor);
        if (result != null) {
            return result;
        }
        return findLocalStaticMethodActor(name, descriptor);
    }

    public final StaticMethodActor findLocalStaticMethodActor(Utf8Constant name) {
        // Old style for loop so this can be used in the primordial VM phase:
        for (int i = 0; i < localStaticMethodActors.length; i++) {
            final StaticMethodActor staticMethodActor = localStaticMethodActors[i];
            if (staticMethodActor.name.equals(name)) {
                return staticMethodActor;
            }
        }
        return null;
    }

    public final StaticMethodActor findLocalStaticMethodActor(String name) {
        return findLocalStaticMethodActor(SymbolTable.makeSymbol(name));
    }

    public final MethodActor findLocalClassMethodActor(MethodActor declaredMethod) {
        return findLocalClassMethodActor(declaredMethod.name, declaredMethod.descriptor());
    }

    public final boolean forAllClassMethodActors(Predicate<ClassMethodActor> predicate) {
        final Class<Predicate<VirtualMethodActor>> dynamicType = null;
        if (!forAllVirtualMethodActors(StaticLoophole.cast(dynamicType, predicate))) {
            return false;
        }
        final Class<Predicate<StaticMethodActor>> staticType = null;
        return forAllStaticMethodActors(StaticLoophole.cast(staticType, predicate));
    }

    public final void forAllClassMethodActors(Procedure<ClassMethodActor> procedure) {
        final Class<Procedure<VirtualMethodActor>> dynamicType = null;
        forAllVirtualMethodActors(StaticLoophole.cast(dynamicType, procedure));
        final Class<Procedure<StaticMethodActor>> staticType = null;
        forAllStaticMethodActors(StaticLoophole.cast(staticType, procedure));
    }

    /**
     * This variant is for JNI and reflection, but not for byte code resolution.
     */
    public final MethodActor findMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        MethodActor methodActor;
        ClassActor holder = this;
        do {
            methodActor = holder.findLocalVirtualMethodActor(name, descriptor);
            if (methodActor != null && !methodActor.isMiranda()) {
                return methodActor;
            }
            methodActor = holder.findLocalStaticMethodActor(name, descriptor);
            if (methodActor != null) {
                return methodActor;
            }
            methodActor = holder.findLocalInterfaceMethodActor(name, descriptor);
            if (methodActor != null) {
                return methodActor;
            }
            for (InterfaceActor interfaceActor : holder.localInterfaceActors()) {
                methodActor = interfaceActor.findMethodActor(name, descriptor);
                if (methodActor != null) {
                    return methodActor;
                }
            }
            holder = holder.superClassActor;
        } while (holder != null);
        return null;
    }

    public final ClassMethodActor findClassMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        ClassActor classActor = this;
        do {
            final ClassMethodActor methodActor = classActor.findLocalClassMethodActor(name, descriptor);
            if (methodActor != null) {
                return methodActor;
            }
            if (name.equals(SymbolTable.INIT) || name.equals(SymbolTable.CLINIT)) {
                return null;
            }
            classActor = classActor.superClassActor;
        } while (classActor != null);
        return null;
    }

    public final ClassMethodActor findClassMethodActor(MethodActor declaredMethod) {
        return findClassMethodActor(declaredMethod.name, declaredMethod.descriptor());
    }

    @INSPECTED
    private final InterfaceMethodActor[] localInterfaceMethodActors;

    public final InterfaceMethodActor[] localInterfaceMethodActors() {
        return localInterfaceMethodActors;
    }

    public final InterfaceMethodActor findLocalInterfaceMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        return findMemberActor(localInterfaceMethodActors, name, descriptor);
    }

    public final MethodActor findLocalMethodActor(Utf8Constant name, SignatureDescriptor descriptor) {
        final MethodActor result = findLocalClassMethodActor(name, descriptor);
        if (result != null) {
            return result;
        }
        return findLocalInterfaceMethodActor(name, descriptor);
    }

    private void assignHolderToMembers(MemberActor[]... memberActorArrays) {
        int index = 0;
        for (MemberActor[] memberActors : memberActorArrays) {
            for (MemberActor memberActor : memberActors) {
                memberActor.assignHolder(this, index);
                index++;
            }
        }

    }

    private void assignHolderToLocalFieldActors() {
        assignHolderToMembers(localInstanceFieldActors, localStaticFieldActors);
    }

    public FieldActor getLocalFieldActor(int memberIndex) {
        if (memberIndex < localInstanceFieldActors.length) {
            return localInstanceFieldActors[memberIndex];
        }
        final int index = memberIndex - localInstanceFieldActors.length;
        if (index < localStaticFieldActors.length) {
            return localStaticFieldActors[index];
        }
        return null;
    }

    private void assignHolderToLocalMethodActors() {
        assignHolderToMembers(localVirtualMethodActors, localStaticMethodActors, localInterfaceMethodActors);
    }

    public MethodActor getLocalMethodActor(int memberIndex) {
        if (memberIndex < localVirtualMethodActors.length) {
            return localVirtualMethodActors[memberIndex];
        }
        int index = memberIndex - localVirtualMethodActors.length;
        if (index < localStaticMethodActors.length) {
            return localStaticMethodActors[index];
        }
        index -= localStaticMethodActors.length;
        if (index < localInterfaceMethodActors.length) {
            return localInterfaceMethodActors[index];
        }
        return null;
    }

    @CONSTANT
    private int[] iToV;

    private Sequence<VirtualMethodActor> gatherVirtualMethodActors(IdentityHashSet<InterfaceActor> allInterfaceActors, GrowableMapping<MethodActor, VirtualMethodActor> lookup) {
        if (!isReferenceClassActor()) {
            return Sequence.Static.empty(VirtualMethodActor.class);
        }

        final VariableSequence<VirtualMethodActor> result = new ArrayListSequence<VirtualMethodActor>();
        int vTableIndex = Hub.vTableStartIndex();

        // Copy the super class' dynamic methods:
        if (superClassActor != null) {
            for (VirtualMethodActor virtualMethodActor : superClassActor.allVirtualMethodActors()) {
                result.append(virtualMethodActor);
                if (!virtualMethodActor.isInstanceInitializer() && !virtualMethodActor.isPrivate()) {
                    lookup.put(virtualMethodActor, virtualMethodActor);
                } else {
                    ++vTableIndex;
                }
            }
            vTableIndex += lookup.length();
        }

        // Enter this class' local dynamic methods, "overriding" existing entries in the lookup table:
        for (VirtualMethodActor virtualMethodActor : localVirtualMethodActors()) {
            if (!virtualMethodActor.isInstanceInitializer() && !virtualMethodActor.isPrivate()) {
                final VirtualMethodActor superMethod;
                superMethod = lookup.put(virtualMethodActor, virtualMethodActor);
                if (superMethod == null) {
                    result.append(virtualMethodActor);
                    virtualMethodActor.setVTableIndex(vTableIndex);
                    vTableIndex++;
                } else {
                    if (superMethod.isFinal() && superMethod.isAccessibleBy(this)) {
                        throw verifyError("Class " + name + " overrides final method: " + superMethod.format("%r %H.%n(%p)"));
                    }
                    result.set(superMethod.vTableIndex() - Hub.vTableStartIndex(), virtualMethodActor);
                    virtualMethodActor.setVTableIndex(superMethod.vTableIndex());
                }
            }
        }

        int memberIndex = localVirtualMethodActors().length;
        int numberOfLocalMirandaMethods = 0;
        // Add Miranda methods for interface methods which lack any implementing dynamic class actor:
        for (InterfaceActor interfaceActor : allInterfaceActors) {
            for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                if (lookup.get(interfaceMethodActor) == null) {
                    numberOfLocalMirandaMethods++;
                    final MirandaMethodActor mirandaMethodActor = new MirandaMethodActor(interfaceMethodActor);
                    mirandaMethodActor.assignHolder(this, memberIndex);
                    memberIndex++;
                    lookup.put(mirandaMethodActor, mirandaMethodActor);

                    result.append(mirandaMethodActor);
                    mirandaMethodActor.setVTableIndex(vTableIndex);
                    vTableIndex++;
                }
            }
        }

        // Update local virtual methods with local Miranda methods (if any)
        if (numberOfLocalMirandaMethods != 0) {
            final VirtualMethodActor[] newLocalVirtualMethodActors = new VirtualMethodActor[localVirtualMethodActors.length + numberOfLocalMirandaMethods];
            System.arraycopy(localVirtualMethodActors, 0, newLocalVirtualMethodActors, 0, localVirtualMethodActors.length);
            int resultIndex = result.length() - numberOfLocalMirandaMethods;
            memberIndex = localVirtualMethodActors().length;
            for (int i = 0; i != numberOfLocalMirandaMethods; ++i) {
                newLocalVirtualMethodActors[memberIndex++] = result.get(resultIndex++);
            }
            localVirtualMethodActors = newLocalVirtualMethodActors;
        }

        return result;
    }

    @HOSTED_ONLY
    private static String prohibitedPackagePrefix = null;

    @HOSTED_ONLY
    public static void prohibitPackagePrefix(MaxPackage prefix) {
        prohibitedPackagePrefix = (prefix == null) ? null : prefix.name();
    }

    @HOSTED_ONLY
    private void checkProhibited(Utf8Constant typeName) {
        if (MaxineVM.isHosted()) {
            if (prohibitedPackagePrefix != null && !isArrayClassActor() && !InvocationStubGenerator.isGeneratedStubClassName(typeName.toString())) {
                ProgramError.check(!typeName.toString().startsWith(prohibitedPackagePrefix), "attempt to load from prohibited package: " + typeName);
            }
        }
    }

    /**
     * Gets the type descriptor representing the class of which this class is a member. Note that local and anonymous
     * classes are <b>not</b> member classes and as such, this method will return null for such a class. It will also
     * return null if this is an actor for a top-level class.
     */
    public final TypeDescriptor outerClassDescriptor() {
        if (!isInnerClass(flags())) {
            return null;
        }
        TypeDescriptor outerClass = classRegistry().get(OUTER_CLASS, this);
        if (outerClass == null) {
            final String signature = javaSignature(true);
            final int lastDollarIndex = signature.lastIndexOf('$');
            assert lastDollarIndex != -1;
            final String outerClassSignature = signature.substring(0, lastDollarIndex);
            outerClass = JavaTypeDescriptor.getDescriptorForJavaString(outerClassSignature);
        }
        return outerClass;
    }

    /**
     * @see #outerClassDescriptor()
     */
    public final ClassActor outerClassActor() {
        final TypeDescriptor outerClassDescriptor = outerClassDescriptor();
        if (outerClassDescriptor == null) {
            return null;
        }
        return outerClassDescriptor.resolve(classLoader);
    }

    /**
     * Gets the for the descriptors for the types declared as immediate members of this class. This does not include any
     * local or anonymous classes declared within this class.
     *
     * @return null if there are no types declared as immediate members of this class
     */
    public final TypeDescriptor[] innerClassDescriptors() {
        return classRegistry().get(INNER_CLASSES, this);
    }

    /**
     * Gets the actors for the classes declared as immediate members of this class. This does not
     * include any local or anonymous classes declared within this class.
     */
    public final ClassActor[] innerClassActors() {
        final TypeDescriptor[] innerClassDescriptors = innerClassDescriptors();
        if (innerClassDescriptors == null) {
            return null;
        }
        final ClassActor[] innerClassActors = new ClassActor[innerClassDescriptors.length];
        for (int i = 0; i != innerClassDescriptors.length; ++i) {
            innerClassActors[i] = innerClassDescriptors[i].resolve(classLoader);
        }
        return innerClassActors;
    }

    /**
     * The value of the SourceFile class file attribute associated with this class actor.
     */
    public final String sourceFileName;

    /**
     * Gets a file path for the source file of this class. The file path returned is based on the package containing
     * this class and the value of the {@linkplain #sourceFileName SourceFile attribute} associated with this class
     * actor. If the latter is null, then the source file name is derived from the top level class associated with this
     * class.
     */
    public String sourceFilePath() {
        String sourceFile = this.sourceFileName;
        if (sourceFile == null) {
            Class topLevelClass = toJava();
            for (Class enclosingClass = topLevelClass.getEnclosingClass(); enclosingClass != null; enclosingClass = enclosingClass.getEnclosingClass()) {
                topLevelClass = enclosingClass;
            }
            sourceFile = topLevelClass.getSimpleName() + ".java";
        }
        final String packageName = packageName();
        if (packageName.isEmpty()) {
            return sourceFile;
        }
        return packageName.replace('.', File.separatorChar) + File.separatorChar + sourceFile;
    }

    /**
     * Gets the value of the EnclosingMethod class file attribute associated with this class actor.
     *
     * @return null if there is no EnclosingMethod attribute associated with this class actor
     */
    public final EnclosingMethodInfo enclosingMethodInfo() {
        return classRegistry().get(ENCLOSING_METHOD_INFO, this);
    }

    protected Size layoutFields(SpecificLayout specificLayout) {
        return Size.zero();
    }

    public boolean isReferenceClassActor() {
        return false;
    }

    @CONSTANT
    private DynamicHub dynamicHub;

    /**
     * The dynamic hub is used by dynamic-instance-related operations and by subtype checks.
     * @return the dynamic hub
     */
    public final DynamicHub dynamicHub() {
        return dynamicHub;
    }

    @CONSTANT
    private StaticHub staticHub;

    /**
     * The static hub is used by static-tuple-related operations.
     * @return the static hub
     */
    public final StaticHub staticHub() {
        return staticHub;
    }

    public Size dynamicTupleSize() {
        return dynamicHub.tupleSize;
    }

    public final ClassRegistry classRegistry() {
        return ClassRegistry.makeRegistry(classLoader);
    }

    /**
     * Gets the name of the package containing this class.
     *
     * @return "" if this class is in the unnamed package
     */
    public final String packageName() {
        return Classes.getPackageName(name.toString());
    }

    public final boolean isInSameRuntimePackageAs(ClassActor other) {
        return classLoader == other.classLoader && packageName().equals(other.packageName());
    }

    @Override
    public final boolean isAccessibleBy(ClassActor accessor) {
        if (isPublic() || isInSameRuntimePackageAs(accessor) || accessor.isGenerated()) {
            return true;
        }
        return false;
    }

    /**
     * Gets all the interfaces specified by this class and its super-classes and super-interfaces.
     * That is, the transitive closure of interfaces inherited or declared by this class actor.
     */
    public final IdentityHashSet<InterfaceActor> getAllInterfaceActors() {
        final IdentityHashSet<InterfaceActor> result = new IdentityHashSet<InterfaceActor>();
        if (isInterfaceActor()) {
            result.add((InterfaceActor) this);
        }
        for (InterfaceActor interfaceActor : localInterfaceActors) {
            result.add(interfaceActor);
            result.addAll(interfaceActor.getAllInterfaceActors());
        }
        if (superClassActor != null) {
            result.addAll(superClassActor.getAllInterfaceActors());
        }
        return result;
    }

    /**
     * Gets all the methods declared by this class actor.
     */
    public Sequence<MethodActor> getLocalMethodActors() {
        final AppendableSequence<MethodActor> result = new LinkSequence<MethodActor>();
        AppendableSequence.Static.appendAll(result, localVirtualMethodActors());
        AppendableSequence.Static.appendAll(result, localStaticMethodActors());
        AppendableSequence.Static.appendAll(result, localInterfaceMethodActors());
        return result;
    }

    public final boolean hasSuperClass(ClassActor superClass) {
        ClassActor subClassActor = this;
        do {
            if (subClassActor == superClass) {
                return true;
            }
            subClassActor = subClassActor.superClassActor;
        } while (subClassActor != null);
        return false;
    }

    public final boolean isAssignableFrom(ClassActor subClassActor) {
        if (this.equals(subClassActor)) {
            return true;
        } else if (this.isPrimitiveClassActor() || subClassActor.isPrimitiveClassActor()) {
            return false;
        }
        return subClassActor.dynamicHub().isSubClassHub(this);
    }

    @INLINE
    public final Class mirror() {
        if (mirror == null) {
            if (MaxineVM.isHosted()) {
                mirror = JavaPrototype.javaPrototype().toJava(this);
            } else {
                return noninlineCreateMirror();
            }
        }
        return mirror;
    }

    private Class noninlineCreateMirror() {
        // Non-blocking synchronization is used here to swap in the mirror reference.
        // This could lead to some extra Class objects being created that become garbage, but should be harmless.
        final Class newMirror = (Class) Heap.createTuple(ClassRegistry.CLASS.dynamicHub());
        TupleAccess.writeObject(newMirror, Class_classActor.offset(), this);
        final Reference oldValue = Reference.fromJava(this).compareAndSwapReference(ClassRegistry.ClassActor_mirror.offset(), null,  Reference.fromJava(newMirror));
        if (oldValue == null) {
            return newMirror;
        }
        return UnsafeCast.asClass(oldValue.toJava());
    }

    @HOSTED_ONLY
    public final void setMirror(Class javaClass) {
        if (mirror == null) {
            mirror = javaClass;
        } else {
            if (mirror != javaClass) {
                ProgramError.unexpected("setMirror called with different value, old=" + mirror + ", new=" + javaClass);
            }
        }
    }

    @Override
    public final String javaSignature(boolean qualified) {
        return typeDescriptor.toJavaString(qualified);
    }

    @Override
    public String qualifiedName() {
        return javaSignature(true);
    }

    @HOSTED_ONLY
    private static final Map<Class, ClassActor> classToClassActorMap = new HashMap<Class, ClassActor>();

    /**
     * Gets the class actor for a given Java class.
     * <p>
     * If this called during bootstrapping and the given Java class is annotated with {@link HOSTED_ONLY}, then null
     * is returned.
     */
    @INLINE
    public static ClassActor fromJava(final Class<?> javaClass) {
        if (MaxineVM.isHosted()) {
            return JavaPrototype.javaPrototype().toClassActor(javaClass);
        }
        return (ClassActor) TupleAccess.readObject(javaClass, Class_classActor.offset());
    }

    /**
     * The local {@link Class} object for the actor.
     */
    @INLINE
    public final Class<?> toJava() {
        return mirror();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return toJava().getAnnotation(annotationClass);
    }

    protected BitSet getSuperClassActorSerials() {
        final BitSet result = new BitSet();
        result.set(id);
        if (superClassActor != null) {
            result.or(superClassActor.getSuperClassActorSerials());
        }
        for (InterfaceActor interfaceActor : localInterfaceActors()) {
            result.or(interfaceActor.getSuperClassActorSerials());
        }
        if (MaxineVM.isHosted()) {
            if (kind == Kind.WORD) {
                result.clear(ClassRegistry.OBJECT.id);
            }
        }
        return result;
    }

    private void verify() {
        if (isGenerated() || !ClassVerifier.shouldBeVerified(classLoader, isRemote())) {
            // generated stubs do not necessarily pass the verifier, even if they work as intended
        } else {
            Verifier.verifierFor(this).verify();
        }
    }

    /**
     * Constant denoting that a class is initialized. This value is {@code null} so that
     * the {@linkplain ClassActor#isInitialized() initialization test} for a class
     * is as small and fast as possible. Most importantly, it must not require loading of a static variable.
     *
     * This means there is a short window in the beginning of the {@link ClassActor#ClassActor constructor} during which
     * the {@code ClassActor} instance being initialized may appear initialized. Care must be taken to ensure
     * {@link ClassActor#isInitialized()} is never called inside this window.
     */
    private static final Object INITIALIZED = null;

    /**
     * Constant denoting that a class is prepared.
     */
    private static final Object PREPARED = new Object();

    /**
     * Constant denoting that a class is verified.
     */
    private static final Object VERIFIED = new Object();

    /**
     * Determines if this class actor has a parameterless static method named "<clinit>".
     */
    public final boolean hasClassInitializer() {
        return clinit != null;
    }

    public void callInitializer() {
        if (clinit != null) {
            SpecialBuiltin.call(CompilationScheme.Static.compile(clinit, CallEntryPoint.OPTIMIZED_ENTRY_POINT));
        }
        initializationState = INITIALIZED;
    }

    private boolean tryInitialization() {
        while (true) {
            synchronized (this) {
                if (isInitialized()) {
                    return false;
                }
                Object initializationState = this.initializationState;
                if (isPrepared(initializationState)) {
                    verify();
                    this.initializationState = VERIFIED;
                } else if (isVerified(initializationState)) {
                    this.initializationState = Thread.currentThread();
                    if (VMOptions.verboseOption.verboseClass) {
                        Log.println("[Initializing " + name + "]");
                    }
                    return true;
                } else if (isInitializing(initializationState)) {
                    // INITIALIZING:
                    Thread initializingThread = (Thread) initializationState;
                    if (initializingThread == Thread.currentThread()) {
                        return false;
                    }
                    try {
                        wait();
                    } catch (InterruptedException interruptedException) {
                    }
                } else if (isError(initializationState)) {
                    throw (NoClassDefFoundError) new NoClassDefFoundError().initCause((Throwable) initializationState);
                }
            }
        }
    }

    private static boolean isError(Object initializationState) {
        return initializationState instanceof Throwable;
    }

    @INLINE
    private static boolean isInitialized(Object initializationState) {
        return initializationState == null;
    }

    private static boolean isInitializing(Object initializationState) {
        return initializationState instanceof Thread;
    }

    private static boolean isVerified(Object initializationState) {
        return initializationState == VERIFIED;
    }

    private static boolean isPrepared(Object initializationState) {
        return initializationState == PREPARED;
    }

    private synchronized void terminateInitialization(Object state) {
        this.initializationState = state;
        notifyAll();
    }

    /**
     * Determines if this the class represented by this class actor is initialized.
     *
     * This test must be fast and so it is not synchronized. This means that a return value of {@code false}
     * is conservative. That is, the caller should operate on the assumption that the class is not initialized,
     * even though it may well be by the time the caller takes some action predicted on the class being initialized.
     * However, a return value of {@code true} is a guarantee that the class is initialized.
     */
    @INLINE
    public final boolean isInitialized() {
        return isInitialized(initializationState);
    }

    /**
     * See #2.17.5.
     */
    public void makeInitialized() {
        if (tryInitialization()) {
            if (superClassActor != null) {
                try {
                    superClassActor.makeInitialized();
                } catch (Error error) {
                    terminateInitialization(error);
                    throw error;
                }
            }
            try {
                callInitializer();
                terminateInitialization(INITIALIZED);
                return;
            } catch (Exception exception) {
                terminateInitialization(exception);
                throw new ExceptionInInitializerError(exception);
            } catch (Error error) {
                terminateInitialization(error);
                throw error;
            }
        }
    }

    public ProtectionDomain protectionDomain() {
        return protectionDomain;
    }

    public void setProtectionDomain(ProtectionDomain protectionDomain) {
        this.protectionDomain = protectionDomain;
    }

    public ConstantPool constantPool() {
        return null;
    }

    public WordWidth wordWidth() {
        return dynamicHub.wordWidth();
    }

    @INLINE
    public final boolean isInstance(Object object) {
        final Hub hub = ObjectAccess.readHub(object);
        return hub.isSubClassHub(this);
    }

    @INLINE
    public final boolean isNullOrInstance(Object object) {
        if (object == null) {
            return true;
        }
        final Hub hub = ObjectAccess.readHub(object);
        return hub.isSubClassHub(this);
    }

    @INLINE
    public final boolean isNonNullInstance(Object object) {
        if (object == null) {
            return false;
        }
        final Hub hub = ObjectAccess.readHub(object);
        return hub.isSubClassHub(this);
    }

    @Override
    public String toString() {
        final String flags = flagsString();
        if (flags.isEmpty()) {
            return name.toString() + " [" + flags + "]";
        }
        return name.toString();
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeInt(id);
    }

    public static ClassActor read(DataInputStream stream) throws IOException {
        return ClassID.toClassActor(stream.readInt());
    }

    // Inspector support for generated stubs:
    @INSPECTED
    public byte[] classfile;
}
