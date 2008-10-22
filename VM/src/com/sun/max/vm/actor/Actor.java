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
package com.sun.max.vm.actor;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of Java entities. These "actors" provide runtime support for classes, fields, methods, etc.
 * They "act" for them by carrying out underlying actions of Java instructions.
 * <p>
 * Actors uses identity for {@linkplain #equals(Object) equality testing}. The hash code for
 * {@linkplain ClassActor#hashCode() classes} and {@linkplain MemberActor#hashCode()} are specialized to minimize
 * conflicts. In any case, actors are safe to use as keys in {@linkplain Map maps}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Actor {

    // Common Flags in #4.1 table 4.1, #4.5 table 4.4 and #4.6 table 4.5:
    public static final int ACC_PUBLIC =     0x00000001;
    public static final int ACC_PRIVATE =    0x00000002;
    public static final int ACC_PROTECTED =  0x00000004;
    public static final int ACC_STATIC =     0x00000008;
    public static final int ACC_FINAL =      0x00000010;
    public static final int ACC_SYNTHETIC =  0x00001000;
    public static final int ACC_ENUM =       0x00004000;

    // Made up extra flag corresponding to attributes in #4.7:
    public static final int DEPRECATED =     0x00008000;

    // Common flags referring to interfaces and classes:
    public static final int ACC_SUPER =      0x00000020;
    public static final int ACC_INTERFACE =  0x00000200;
    public static final int ACC_ABSTRACT =   0x00000400;
    public static final int ACC_ANNOTATION = 0x00002000;

    // VM-internal flags for classes:
    public static final int INNER_CLASS =    0x00100000;
    public static final int TEMPLATE =       0x00200000;
    public static final int GENERATED =      0x00400000; // does not come from a class file

    // Common flags referring to fields in #4.5, Table #4.4:
    public static final int ACC_VOLATILE =   0x00000040;
    public static final int ACC_TRANSIENT =  0x00000080;

    // VM-internal flags referring to fields:
    public static final int INJECTED =               0x00010000; // an additionally injected field that was not derived from a class file field
    public static final int CONSTANT =               0x00020000;
    public static final int CONSTANT_WHEN_NOT_ZERO = 0x00040000;

    // Common flags referring to methods in #4.6, Table 4.5:
    public static final int ACC_SYNCHRONIZED = 0x00000020;
    public static final int ACC_BRIDGE =       0x00000040;
    public static final int ACC_VARARGS =      0x00000080;
    public static final int ACC_NATIVE =       0x00000100;
    // see above            ACC_ABSTRACT =     0x00000400;
    public static final int ACC_STRICT =       0x00000800;

    // VM-internal flags for methods:
    public static final int WRAPPER =              0x00100000;
    // see above            TEMPLATE =             0x00200000;
    public static final int CLASS_INITIALIZER =    0x00400000;
    public static final int INSTANCE_INITIALIZER = 0x00800000;
    public static final int C_FUNCTION =           0x01000000;
    public static final int JNI_FUNCTION =         0x02000000;
    public static final int FOLD =                 0x04000000;
    public static final int BUILTIN =              0x08000000;
    public static final int SURROGATE =            0x10000000;
    public static final int UNSAFE =               0x20000000;
    public static final int INLINE =               0x40000000;
    public static final int NEVER_INLINE =         0x80000000;
    public static final int INLINE_AFTER_SNIPPETS_ARE_COMPILED =
                                                   0x00010000;
    public static final int NO_SAFEPOINTS =        0x00004000;
    public static final int SIGNAL_STUB =          0x00008000;
    public static final int SIGNAL_HANDLER =       0x00002000;

    /**
     * Mask of the flags defined for classes in Table 4.1 of the JVM specification.
     */
    public static final int JAVA_CLASS_FLAGS =
        ACC_PUBLIC |
        ACC_FINAL |
        ACC_SUPER |
        ACC_INTERFACE |
        ACC_ABSTRACT |
        ACC_ANNOTATION |
        ACC_ENUM |
        ACC_SYNTHETIC;

    /**
     * Mask of the flags defined for fields in Table 4.4 of the JVM specification.
     */
    public static final int JAVA_FIELD_FLAGS =
        ACC_PUBLIC |
        ACC_PRIVATE |
        ACC_PROTECTED |
        ACC_STATIC |
        ACC_FINAL |
        ACC_VOLATILE |
        ACC_TRANSIENT |
        ACC_ENUM |
        ACC_SYNTHETIC;

    /**
     * Mask of the flags defined for methods in Table 4.5 of the JVM specification.
     */
    public static final int JAVA_METHOD_FLAGS =
        ACC_PUBLIC |
        ACC_PRIVATE |
        ACC_PROTECTED |
        ACC_STATIC |
        ACC_FINAL |
        ACC_SYNCHRONIZED |
        ACC_BRIDGE |
        ACC_VARARGS |
        ACC_NATIVE |
        ACC_ABSTRACT |
        ACC_STRICT |
        ACC_SYNTHETIC;

    public static final Utf8Constant NO_GENERIC_SIGNATURE = null;
    public static final byte[] NO_RUNTIME_VISIBLE_ANNOTATION_BYTES = null;

    @INSPECTED
    @CONSTANT
    private int _flags;

    @INSPECTED
    private final Utf8Constant _name;

    protected Actor(Utf8Constant name, int flags) {
        _flags = flags;
        _name = name;
    }

    @Override
    public final boolean equals(Object other) {
        return this == other;
    }

    /**
     * Subclasses must define their own hash code, obeying the normal rules about the relationship between {@link #equals(Object)} and {@link #hashCode()}.
     */
    @Override
    public abstract int hashCode();

    @INLINE
    public final int flags() {
        return _flags;
    }

    @INLINE
    public final boolean isPublic() {
        return isPublic(flags());
    }

    @INLINE
    public final boolean isPrivate() {
        return isPrivate(flags());
    }

    @INLINE
    public final boolean isProtected() {
        return isProtected(flags());
    }

    @INLINE
    public final boolean isStatic() {
        return isStatic(flags());
    }

    @INLINE
    public final boolean isFinal() {
        return isFinal(flags());
    }

    @INLINE
    public final boolean isSynthetic() {
        return isSynthetic(flags());
    }

    @INLINE
    public final boolean isEnum() {
        return isEnum(flags());
    }

    @INLINE
    public final boolean isAbstract() {
        return isAbstract(flags());
    }

    @INLINE
    public final boolean isDeprecated() {
        return isDeprecated(flags());
    }

    @INLINE
    public static final boolean isPublic(int flags) {
        return (flags & ACC_PUBLIC) != 0;
    }

    @INLINE
    public static final boolean isPrivate(int flags) {
        return (flags & ACC_PRIVATE) != 0;
    }

    @INLINE
    public static final boolean isProtected(int flags) {
        return (flags & ACC_PROTECTED) != 0;
    }

    @INLINE
    public static final boolean isStatic(int flags) {
        return (flags & ACC_STATIC) != 0;
    }

    @INLINE
    public static final boolean isFinal(int flags) {
        return (flags & ACC_FINAL) != 0;
    }

    @INLINE
    public static final boolean isSynthetic(int flags) {
        return (flags & ACC_SYNTHETIC) != 0;
    }

    @INLINE
    public static final boolean isEnum(int flags) {
        return (flags & ACC_ENUM) != 0;
    }

    @INLINE
    public static final boolean isAbstract(int flags) {
        return (flags & ACC_ABSTRACT) != 0;
    }

    @INLINE
    public static final boolean isInterface(int flags) {
        return (flags & ACC_INTERFACE) != 0;
    }

    @INLINE
    public static final boolean isInnerClass(int flags) {
        return (flags & INNER_CLASS) != 0;
    }

    @INLINE
    public static final boolean isDeprecated(int flags) {
        return (flags & DEPRECATED) != 0;
    }

    @INLINE
    public static final boolean isSuper(int flags) {
        return (flags & ACC_SUPER) != 0;
    }

    @INLINE
    public static final boolean isAnnotation(int flags) {
        return (flags & ACC_ANNOTATION) != 0;
    }

    @INLINE
    public static final boolean isBridge(int flags) {
        return (flags & ACC_BRIDGE) != 0;
    }

    @INLINE
    public static final boolean isVarArgs(int flags) {
        return (flags & ACC_VARARGS) != 0;
    }

    @INLINE
    public static boolean isSynchronized(int flags) {
        return (flags & ACC_SYNCHRONIZED) != 0;
    }

    @INLINE
    public static boolean isNative(int flags) {
        return (flags & ACC_NATIVE) != 0;
    }

    @INLINE
    public static boolean isStrict(int flags) {
        return (flags & ACC_STRICT) != 0;
    }

    @INLINE
    public static boolean isClassInitializer(int flags) {
        return (flags & CLASS_INITIALIZER) != 0;
    }

    @INLINE
    public static boolean isInstanceInitializer(int flags) {
        return (flags & INSTANCE_INITIALIZER) != 0;
    }

    @INLINE
    public static boolean isInitializer(int flags) {
        return (flags & (CLASS_INITIALIZER | INSTANCE_INITIALIZER)) != 0;
    }

    @INLINE
    public static boolean isCFunction(int flags) {
        return (flags & C_FUNCTION) != 0;
    }

    @INLINE
    public static boolean isJniFunction(int flags) {
        return (flags & JNI_FUNCTION) != 0;
    }

    @INLINE
    public static boolean isSignalStub(int flags) {
        return (flags & SIGNAL_STUB) != 0;
    }

    @INLINE
    public static boolean isSignalHandler(int flags) {
        return (flags & SIGNAL_HANDLER) != 0;
    }

    @INLINE
    public static boolean isTemplate(int flags) {
        return (flags & TEMPLATE) != 0;
    }

    @INLINE
    public static boolean isGenerated(int flags) {
        return (flags & GENERATED) != 0;
    }

    @INLINE
    public static boolean isVolatile(int flags) {
        return (flags & ACC_VOLATILE) != 0;
    }

    @INLINE
    public static boolean isTransient(int flags) {
        return (flags & ACC_TRANSIENT) != 0;
    }

    @INLINE
    public static boolean isInjected(int flags) {
        return (flags & INJECTED) != 0;
    }

    @INLINE
    public static boolean isConstant(int flags) {
        return (flags & CONSTANT) != 0;
    }

    @INLINE
    public static boolean isConstantWhenNotZero(int flags) {
        return (flags & CONSTANT_WHEN_NOT_ZERO) != 0;
    }

    @INLINE
    public static boolean isBuiltin(int flags) {
        return (flags & BUILTIN) != 0;
    }

    @INLINE
    public static boolean isSurrogate(int flags) {
        return (flags & SURROGATE) != 0;
    }

    @INLINE
    public static boolean isWrapper(int flags) {
        return (flags & WRAPPER) != 0;
    }

    @INLINE
    public static boolean isUnsafe(int flags) {
        return (flags & UNSAFE) != 0;
    }

    @INLINE
    public void beUnsafe() {
        _flags |= UNSAFE;
    }

    @INLINE
    public static boolean isInline(int flags) {
        return (flags & INLINE) != 0;
    }

    @INLINE
    public static boolean isNeverInline(int flags) {
        return (flags & NEVER_INLINE) != 0;
    }

    @INLINE
    public static boolean isDeclaredFoldable(int flags) {
        return (flags & FOLD) != 0;
    }

    @INLINE
    public static boolean noSafepoints(int flags) {
        return (flags & NO_SAFEPOINTS) != 0;
    }

    public static Predicate<Actor> _staticPredicate = new Predicate<Actor>() {
        public boolean evaluate(Actor actor) {
            return actor.isStatic();
        }
    };

    public static Predicate<Actor> _dynamicPredicate = new Predicate<Actor>() {
        public boolean evaluate(Actor actor) {
            return !actor.isStatic();
        }
    };

    @INLINE
    public final Utf8Constant name() {
        return _name;
    }

    /**
     * Gets the name of this actor qualified by it's declaring class (if known).
     * @return
     */
    public abstract String qualifiedName();

    /**
     * Gets the value of the Signature class file attribute associated with this actor.
     *
     * @return null if there is no Signature attribute associated with this actor
     */
    public abstract Utf8Constant genericSignature();

    public final String genericSignatureString() {
        final Utf8Constant constant = genericSignature();
        if (constant == null) {
            return null;
        }
        return constant.toString();
    }

    /**
     * Gets the bytes of the RuntimeVisibleAnnotations class file attribute associated with this actor.
     *
     * @return null if there is no RuntimeVisibleAnnotations attribute associated with this actor
     */
    public abstract byte[] runtimeVisibleAnnotationsBytes();

    public String simpleName() {
        return javaSignature(false);
    }

    public String flagsString() {
        return flagsString(_flags);
    }

    public static String flagsString(int flags) {
        final StringBuilder sb = new StringBuilder();

        if (isPublic(flags)) {
            sb.append("public ");
        }
        if (isProtected(flags)) {
            sb.append("protected ");
        }
        if (isPrivate(flags)) {
            sb.append("private ");
        }

        /* Canonical order */
        if (isAbstract(flags)) {
            sb.append("abstract ");
        }
        if (isStatic(flags)) {
            sb.append("static ");
        }
        if (isFinal(flags)) {
            sb.append("final ");
        }
        if (isTransient(flags) || isVarArgs(flags)) {
            sb.append("transient/varargs ");
        }
        if (isVolatile(flags) || isBridge(flags)) {
            sb.append("volatile/bridge ");
        }
        if (isSynchronized(flags) || isSuper(flags)) {
            sb.append("synchronized/super ");
        }
        if (isNative(flags)) {
            sb.append("native ");
        }
        if (isStrict(flags)) {
            sb.append("strictfp ");
        }
        if (isInterface(flags)) {
            sb.append("interface ");
        }
        if (isSynthetic(flags)) {
            sb.append("synthetic ");
        }
        if (isEnum(flags)) {
            sb.append("enum ");
        }
        if (isDeprecated(flags)) {
            sb.append("deprecated ");
        }
        if (isAnnotation(flags)) {
            sb.append("annotation ");
        }

        // Implementation specific flags
        if (isInjected(flags)) {
            sb.append("injected ");
        }
        if (isConstant(flags)) {
            sb.append("constant ");
        }
        if (isConstantWhenNotZero(flags)) {
            sb.append("constantWhenNotZero ");
        }
        if (isInnerClass(flags)) {
            sb.append("innerClass ");
        }
        if (isTemplate(flags)) {
            sb.append("template ");
        }
        if (isGenerated(flags)) {
            sb.append("generated ");
        }
        if (isClassInitializer(flags)) {
            sb.append("<clinit> ");
        }
        if (isInstanceInitializer(flags)) {
            sb.append("<init> ");
        }
        if (isCFunction(flags)) {
            sb.append("c_function ");
        }
        if (isJniFunction(flags)) {
            sb.append("jni_function ");
        }
        if (isDeclaredFoldable(flags)) {
            sb.append("fold ");
        }
        if (isBuiltin(flags)) {
            sb.append("builtin ");
        }
        if (isSurrogate(flags)) {
            sb.append("surrogate ");
        }
        if (isWrapper(flags)) {
            sb.append("wrapper ");
        }

        if (sb.length() > 0) {
            /* trim trailing space */
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
        return "";
    }

    /**
     * @see AnnotatedElement#getAnnotation(Class)
     */
    public abstract <A extends Annotation> A getAnnotation(Class<A> annotationClass);

    public boolean isAnnotationPresent(Class< ? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * Gets the signature of this actor in pseudo Java source syntax. For example:
     *
     * <pre>
     * "java.lang.Class"
     * "java.lang.String getName()"
     * "java.io.PrintStream out"
     * "java.util.Collections$EmptyMap"
     * "java.lang.Package$1PackageInfoProxy"
     * "java.util.AbstractMap$1$1"
     * </pre>
     *
     * Or if {@code qualified == false}:
     *
     * <pre>
     * "Class"
     * "String getName()"
     * "PrintStream out"
     * "Collections$EmptyMap"
     * "Package$1PackageInfoProxy"
     * "AbstractMap$1$1"
     * </pre>
     *
     * Note that the signature is not correct Java syntax if it contains a type that is a nested class.
     *
     * @param qualified if true, then the types in the signature are qualified with their package names
     * @return the signature of this actor in pseudo Java source syntax
     */
    public abstract String javaSignature(boolean qualified);

    /**
     * Gets a string for this actor formatted according to a given format specification. A format specification is
     * composed of characters that are to be copied verbatim to the result and specifiers that denote an attribute of this
     * actor that is to be copied to the result. A specifier is a single character preceded by a '%' character. The
     * accepted specifiers and the actor attribute they denote are described below:
     *
     * <pre>
     *     Specifier | Description                                        | Example(s)
     *     ----------+------------------------------------------------------------------------------------------
     *     'T'       | Qualified return/field type                        | "int" "java.lang.String"
     *     't'       | Unqualified return/field type                      | "int" "String"
     *     'R'       | Qualified return/field type                        | "int" "java.lang.String"
     *     'r'       | Unqualified return/field type                      | "int" "String"
     *     'H'       | Qualified holder                                   | "java.util.Map.Entry"
     *     'h'       | Unqualified holder                                 | "Entry"
     *     'n'       | Method/field/class name                            | "add"
     *     'P'       | Qualified parameter types, separated by ', '       | "int, java.lang.String"
     *     'p'       | Unqualified parameter types, separated by ', '     | "int, String"
     *     'f'       | The flags as a {@linkplain #flagsString() string}  | "public static final" "transient"
     *     '%'       | A '%' character                                    | "%"
     * </pre>
     *
     * If a specifier is given for an actor attribute that is not applicable for this actor type, then
     * the specifier is ignored or causes an {@link IllegalArgumentException} depending on the value of {@code strict}.
     * For example, a "%T" in {@code format} is ignored if this object is a {@link ClassActor} instance and {@code strict == false}.
     *
     * @param format a format specification
     * @param strict specifies what action to take if actor attributes that don't apply to this actor are encountered in {@code format}
     * @return the result of formatting this method according to {@code format}
     * @throws IllegalArgumentException if an illegal specifier is encountered in {@code format}
     */
    public final String format(String format, boolean strict) {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        while (index < format.length()) {
            final char ch = format.charAt(index++);
            if (ch == '%') {
                if (index >= format.length()) {
                    throw new IllegalArgumentException("An unquoted '%' character cannot terminate a method format specification");
                }
                final char specifier = format.charAt(index++);
                boolean qualified = false;
                switch (specifier) {
                    case 'T':
                    case 'R':
                        qualified = true;
                        // fall through
                    case 't':
                    case 'r': {
                        if (this instanceof MethodActor) {
                            final MethodActor methodActor = (MethodActor) this;
                            sb.append(methodActor.descriptor().getResultDescriptor().toJavaString(qualified));
                        } else if (this instanceof FieldActor) {
                            final FieldActor fieldActor = (FieldActor) this;
                            sb.append(fieldActor.descriptor().toJavaString(qualified));
                        } else {
                            if (strict) {
                                throw new IllegalArgumentException("Cannot use %" + specifier + " in format specification for an instance of " + getClass().getSimpleName());
                            }
                        }
                        break;
                    }
                    case 'H':
                        qualified = true;
                        // fall through
                    case 'h': {
                        if (this instanceof MemberActor) {
                            final MemberActor memberActor = (MemberActor) this;
                            sb.append(memberActor.holder().typeDescriptor().toJavaString(qualified));
                        } else {
                            if (strict) {
                                throw new IllegalArgumentException("Cannot use %" + specifier + " in format specification for an instance of " + getClass().getSimpleName());
                            }
                        }
                        break;
                    }
                    case 'n': {
                        sb.append(name());
                        break;
                    }
                    case 'P':
                        qualified = true;
                        // fall through
                    case 'p': {
                        if (this instanceof MethodActor) {
                            final MethodActor methodActor = (MethodActor) this;
                            String separator = "";
                            for (TypeDescriptor parameterDescriptor : methodActor.descriptor().getParameterDescriptors()) {
                                sb.append(separator).append(parameterDescriptor.toJavaString(qualified));
                                separator = ", ";
                            }
                        } else {
                            if (strict) {
                                throw new IllegalArgumentException("Cannot use %" + specifier + " in format specification for an instance of " + getClass().getSimpleName());
                            }
                        }
                        break;
                    }
                    case 'f': {
                        sb.append(flagsString());
                        break;
                    }
                    case '%': {
                        sb.append('%');
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("Illegal specifier '" + specifier + "' in a method format specification");
                    }
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Calling this method is equivalent to calling {@link #format(String) format(format, true)}.
     *
     * @param format a format specification
     * @return the result of formatting this method according to {@code format}
     * @throws IllegalArgumentException if an illegal specifier is encountered in {@code format}
     */
    public final String format(String format) {
        return format(format, true);
    }

    @Override
    public abstract String toString();

    public abstract boolean isAccessibleBy(ClassActor accessor);

    public final void checkAccessBy(ClassActor accessor) {
        if (!isAccessibleBy(accessor)) {
            throw new IllegalAccessError(accessor.name() + " cannot access " + this);
        }
    }
}
