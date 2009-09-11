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
package com.sun.max.vm.jdk;

import java.lang.reflect.*;
import java.security.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;

/*
 * Provides substitutions for native methods in {@link java.lang.Class java.lang.Class}.
 * In particular, the mapping between a {@code Class} and the {@code ClassLoader}
 * that loaded it is not maintained at the Java level (HotSpot maintains this internally).
 * This implementation uses an injected field in the {@code Class} instance that
 * references the {@link ClassActor ClassActor}, which contains the Maxine representation
 * of a class.
 *
 * Note that instances of {@code java.lang.Class} are created lazily as needed from
 * the internal {@code ClassActor} representation.
 */
@METHOD_SUBSTITUTIONS(Class.class)
final class JDK_java_lang_Class {

    private JDK_java_lang_Class() {
    }

    @SUBSTITUTE
    private static void registerNatives() {
    }

    @UNSAFE_CAST
    private native Class thisClass();

    @INLINE
    private ClassActor thisClassActor() {
        return ClassActor.fromJava(thisClass());
    }

    @SUBSTITUTE
    private static Class forName0(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        // loader may be null if invoked from VM (@see JDK_sun_reflect_Reflection.getCallerClass)
        ClassLoader classLoader = loader;
        if (classLoader == null) {
            classLoader = VmClassLoader.VM_CLASS_LOADER;
        }
        if (name != null && name.charAt(0) == '[') {
            // treat arrays specially.
            TypeDescriptor descriptor;
            try {
                descriptor = JavaTypeDescriptor.parseMangledArrayOrUnmangledClassName(name);
            } catch (ClassFormatError e) {
                throw new ClassNotFoundException(name);
            }
            return descriptor.resolveType(classLoader);
        }
        return resolveComponent(name, initialize, classLoader);
    }

    private static Class resolveComponent(String name, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        final Class javaClass = classLoader.loadClass(name);
        if (initialize) {
            MakeClassInitialized.makeClassInitialized(ClassActor.fromJava(javaClass));
        }
        return javaClass;
    }

    /**
     * Checks that the specified object is an instance of this class.
     * @param object the object instance to check
     * @see java.lang.Class#isInstance(Object)
     * @return true if the specified object's actual type is a subclass of this class; false if the
     * object reference is null, or if the object's actual type is not a subclass of this class
     */
    @SUBSTITUTE
    public boolean isInstance(Object object) {
        return thisClassActor().isNonNullInstance(object);
    }

    /**
     * Check whether an assignment to a location (field, array element, local, etc) of this type
     * from a value of the specified type is legal.
     * @see java.lang.Class#isAssignableFrom(Class)
     * @param other the type of the value to assign to a location of this type
     * @return true if the assignment is legal
     */
    @SUBSTITUTE
    public boolean isAssignableFrom(Class other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return thisClassActor().isAssignableFrom(ClassActor.fromJava(other));
    }

    /**
     * Returns true if this class is an interface.
     * @see java.lang.Class#isInterface()
     * @return true if this class is an interface
     */
    @SUBSTITUTE
    public boolean isInterface() {
        return thisClassActor().isInterfaceActor();
    }

    /**
     * Returns true if this class is an array.
     * @see java.lang.Class#isArray()
     * @return true if this class is an array
     */
    @SUBSTITUTE
    public boolean isArray() {
        return thisClassActor().isArrayClassActor();
    }

    /**
     * Returns true if this class is a primitive.
     * @see java.lang.Class#isPrimitive()
     * @return true if this class is a primitive
     */
    @SUBSTITUTE
    public boolean isPrimitive() {
        return thisClassActor() instanceof PrimitiveClassActor;
    }

    /**
     * Gets the fully-qualified, dotted name of this class.
     * @see java.lang.Class#getName()
     * @return the name of this class in dotted notation
     */
    @SUBSTITUTE
    public String getName() {
        final ClassActor thisClassActor = thisClassActor();
        if (thisClassActor.isArrayClassActor()) {
            return Descriptor.dottified(thisClassActor.typeDescriptor.toString());
        }
        return thisClassActor.name.toString();
    }

    /**
     * Retrieves the class loader that loaded this class.
     * @see java.lang.Class#getClassLoader()
     * @return the class loader that loaded this class
     */
    @SUBSTITUTE
    ClassLoader getClassLoader0() {
        final ClassLoader classLoader = thisClassActor().classLoader;
        if (classLoader == VmClassLoader.VM_CLASS_LOADER) {
            return null;
        }
        return classLoader;
    }

    /**
     * Gets the superclass of this class.
     * @see java.lang.Class#getSuperClass()
     * @return the superclass of this class; null if this class is {@code java.lang.Object}
     */
    @SUBSTITUTE
    public Class getSuperclass() {
        final ClassActor thisClassActor = thisClassActor();
        if (thisClassActor instanceof InterfaceActor) {
            return null;
        }
        final ClassActor superClassActor = thisClassActor.superClassActor;
        return superClassActor == null ? null : superClassActor.mirror();
    }

    private static void getInterfaces(ClassActor classActor, AppendableSequence<Class> javaInterfaces) {
        if (classActor == null) {
            return;
        }
        for (InterfaceActor interfaceActor : classActor.localInterfaceActors()) {
            javaInterfaces.append(interfaceActor.mirror());
        }
        for (InterfaceActor interfaceActor : classActor.localInterfaceActors()) {
            getInterfaces(interfaceActor, javaInterfaces);
        }
    }

    /**
     * Get the interfaces implemented by this class.
     * @see java.lang.Class#getInterfaces()
     * @return the interfaces implemented by this class
     */
    @SUBSTITUTE
    public Class[] getInterfaces() {
        final AppendableSequence<Class> javaInterfaces = new LinkSequence<Class>();
        getInterfaces(thisClassActor(), javaInterfaces);
        return Sequence.Static.toArray(javaInterfaces, Class.class);
    }

    /**
     * Get the component type of this class, if this class is an array.
     * @see java.lang.Class#getComponentType()
     * @return the component type of this class, if this class is an array, {@code null} otherwise
     */
    @SUBSTITUTE
    public Class getComponentType() {
        final ClassActor componentClassActor = thisClassActor().componentClassActor();
        if (componentClassActor == null) {
            return null;
        }
        return componentClassActor.mirror();
    }

    /**
     * Get the access modifiers of this class.
     * @see java.lang.Class#getModifiers()
     * @return a bitmask of the modifiers for this class
     */
    @SUBSTITUTE
    public int getModifiers() {
        return thisClassActor().flags();
    }

    /**
     * Get the signers for this class.
     * @see java.lang.Class#getSigners()
     * @return the signers for this class
     */
    @SUBSTITUTE
    public Object[] getSigners() {
        return thisClassActor().signers;
    }

    /**
     * Set the signers for this class.
     * @see java.lang.Class#setSigners()
     * @param signers the new signers for this class
     */
    @SUBSTITUTE
    void setSigners(Object[] signers) {
        final ClassActor classActor = thisClassActor();
        classActor.signers = signers;
    }

    @SUBSTITUTE
    private Object[] getEnclosingMethod0() {
        final EnclosingMethodInfo enclosingMethodInfo = thisClassActor().enclosingMethodInfo();
        if (enclosingMethodInfo != null) {
            return new Object[]{
                enclosingMethodInfo.holder().resolveType(thisClassActor().classLoader),
                enclosingMethodInfo.name(),
                enclosingMethodInfo.descriptor()
            };
        }
        return null;
    }

    /**
     * Note that this method returns null for local and anonmyous classes. The {@link Class#getEnclosingClass()} method
     * must be used to get the <b>nearest</b> enclosing class of a local or anonmyous class.
     * <p>
     * This comment from the {@link Class#getEnclosingClass()} helps explain:
     *
     * There are five kinds of classes (or interfaces):
     * a) Top level classes
     * b) Nested classes (static member classes)
     * c) Inner classes (non-static member classes)
     * d) Local classes (named classes declared within a method)
     * e) Anonymous classes
     *
     * @see Class#getDeclaringClass()
     */
    @SUBSTITUTE
    public Class getDeclaringClass() {
        final ClassActor outerClassActor = thisClassActor().outerClassActor();
        if (outerClassActor != null) {
            return outerClassActor.toJava();
        }
        return null;
    }

    /**
     * Get the protection domain for this class.
     * @see java.lang.Class#getProtectionDomain0()
     * @return
     */
    @SUBSTITUTE
    public ProtectionDomain getProtectionDomain0() {
        return thisClassActor().protectionDomain();
    }

    /**
     * Set the protection domain for this class.
     * @see java.lang.Class#setProtectionDomain0()
     * @param protectionDomain
     */
    @SUBSTITUTE
    void setProtectionDomain0(ProtectionDomain protectionDomain) {
        thisClassActor().setProtectionDomain(protectionDomain);
    }

    /**
     * Gets a primitive class by its name.
     * @see java.lang.Class#getPrimitiveClass()
     * @param name the name of the class
     * @return the class for the specified primitive, if it exists; null otherwise
     */
    @SUBSTITUTE
    static Class getPrimitiveClass(String name) {
        for (Kind kind : Kind.PRIMITIVE_JAVA_CLASSES) {
            if (kind.name.toString().equals(name)) {
                return kind.javaClass;
            }
        }
        return null;
    }

    /**
     * Gets the generic signature of this class.
     * @see java.lang.Class#getGenericSignature()
     * @return the generic signature of this class as a string
     */
    @SUBSTITUTE
    private String getGenericSignature() {
        return thisClassActor().genericSignatureString();
    }

    /**
     * Get the annotations of this class.
     * @see java.lang.Class#getRawAnnotations()
     * @return the raw annotations for this class
     */
    @SUBSTITUTE
    private byte[] getRawAnnotations() {
        return thisClassActor().runtimeVisibleAnnotationsBytes();
    }

    /**
     * Get the constant pool of this class.
     * @see java.lang.Class#getConstantPool()
     * @return the constant pool for this class
     */
    @SUBSTITUTE
    sun.reflect.ConstantPool getConstantPool() {
        final ConstantPool constantPool = thisClassActor().constantPool();
        if (constantPool == null) {
            return null;
        }
        return new ConstantPoolAdapter(constantPool);
    }

    /**
     * Gets the declared fields of this class.
     * @see java.lang.Class#getDeclaredFields0()
     * @param publicOnly true if this method should only return public fields
     * @return an array of the declared fields of this class
     */
    @SUBSTITUTE
    private Field[] getDeclaredFields0(boolean publicOnly) {
        final ClassActor classActor = thisClassActor();
        final AppendableSequence<Field> result = new LinkSequence<Field>();
        for (FieldActor fieldActor : classActor.localInstanceFieldActors()) {
            if (!fieldActor.isHiddenToReflection() && (!publicOnly || fieldActor.isPublic())) {
                result.append(fieldActor.toJava());
            }
        }
        for (FieldActor fieldActor : classActor.localStaticFieldActors()) {
            if (!fieldActor.isHiddenToReflection() && (!publicOnly || fieldActor.isPublic())) {
                result.append(fieldActor.toJava());
            }
        }
        return Sequence.Static.toArray(result, Field.class);
    }

    /**
     * Get the declared methods of this class.
     * @see java.lang.Class#getDeclaredMethods0()
     * @param publicOnly true if this method should return only those methods that are declared public
     * @return an array of the declared methods of this class
     */
    @SUBSTITUTE
    private Method[] getDeclaredMethods0(boolean publicOnly) {
        final ClassActor classActor = thisClassActor();
        final AppendableSequence<Method> result = new LinkSequence<Method>();
        for (MethodActor methodActor : classActor.localVirtualMethodActors()) {
            if (!methodActor.isHiddenToReflection() && (!publicOnly || methodActor.isPublic())) {
                result.append(methodActor.toJava());
            }
        }
        for (MethodActor methodActor : classActor.localStaticMethodActors()) {
            if (!methodActor.isHiddenToReflection() && (!publicOnly || methodActor.isPublic())) {
                result.append(methodActor.toJava());
            }
        }
        for (MethodActor methodActor : classActor.localInterfaceMethodActors()) {
            if (!methodActor.isHiddenToReflection() && (!publicOnly || methodActor.isPublic())) {
                result.append(methodActor.toJava());
            }
        }
        return Sequence.Static.toArray(result, Method.class);
    }

    /**
     * Get the declared constructors of this class.
     * @see java.lang.Class#getDeclaredConstructors0()
     * @param publicOnly true if only the public constructors should be returned
     * @return an array representing the declared constructors of this class
     */
    @SUBSTITUTE
    private Constructor[] getDeclaredConstructors0(boolean publicOnly) {
        final ClassActor classActor = thisClassActor();
        final AppendableSequence<Constructor> result = new LinkSequence<Constructor>();
        for (MethodActor methodActor : classActor.localVirtualMethodActors()) {
            if (methodActor.isInstanceInitializer() && (!publicOnly || methodActor.isPublic())) {
                result.append(methodActor.toJavaConstructor());
            }
        }
        return Sequence.Static.toArray(result, Constructor.class);
    }

    /**
     * Gets the classes that are declared within this class as specified by the {@link InnerClassesAttribute}
     * present in the classfile of this class. Note that the returned classes do not include local or
     * anonymous classes. This is not exactly clear from the specification of {@link Class#getDeclaredClasses()}
     * but is implied by http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4191731.
     */
    @SUBSTITUTE
    private Class[] getDeclaredClasses0() {
        final ClassActor classActor = thisClassActor();
        final ClassActor[] innerClassActors = classActor.innerClassActors();
        if (innerClassActors != null) {
            final Class[] declaredClasses = new Class[innerClassActors.length];
            for (int i = 0; i != declaredClasses.length; ++i) {
                declaredClasses[i] = innerClassActors[i].toJava();
            }
            return declaredClasses;
        }
        return null;
    }

    private static final Utf8Constant ASSERTIONS_DISABLED = SymbolTable.makeSymbol("$assertionsDisabled");

    /**
     * Gets the desired assertion status for the specified class.
     * @see java.lang.Class#desiredAssertionStatus0()
     * @param javaClass the class for which to get the assertion status
     * @return the desired assertion status of the specified class
     */
    @SUBSTITUTE
    private static boolean desiredAssertionStatus0(Class javaClass) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        final FieldActor fieldActor = classActor.findLocalStaticFieldActor(ASSERTIONS_DISABLED, JavaTypeDescriptor.BOOLEAN);
        if (fieldActor != null) {
            final FieldActor booleanFieldActor = fieldActor;
            return TupleAccess.readBoolean(classActor.staticTuple(), booleanFieldActor.offset());
        }
        return false;
    }
}
