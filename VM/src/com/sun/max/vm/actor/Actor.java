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
package com.sun.max.vm.actor;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of Java entities. These "actors" provide runtime support for classes, fields, methods, etc.
 * They "act" for them by carrying out underlying actions of Java instructions.
 * <p>
 * Actors uses identity for {@linkplain #equals(Object) equality testing} and use the system identity
 * hash codes.
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
    public static final int INNER_CLASS =       0x00100000;
    public static final int REFLECTION_STUB =   0x00400000;
    public static final int FINALIZER =         0x00800000;
    public static final int REMOTE =            0x02000000;

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
    public static final int NO_SAFEPOINTS =        0x00004000;
    public static final int VERIFIED =             0x00010000;
    public static final int TEMPLATE =             0x00200000;
    public static final int INITIALIZER =          0x00400000;
    public static final int C_FUNCTION =           0x01000000;
    public static final int VM_ENTRY_POINT =       0x02000000;
    public static final int FOLD =                 0x04000000;
    public static final int LOCAL_SUBSTITUTE =     0x10000000;
    public static final int UNSAFE =               0x20000000;
    public static final int INLINE =               0x40000000;
    public static final int NEVER_INLINE =         0x80000000;

    /**
     * Mask of flags that a substitutee should adopt from its {@linkplain SUBSTITUTE substitute}.
     * Adoption of flags is a union operation with the existing flags of the substitutee.
     */
    public static final int SUBSTITUTION_ADOPTED_FLAGS =
        UNSAFE |
        FOLD |
        INLINE |
        NEVER_INLINE;

    /**
     * Mask of flags used to determine if a given method is unsafe. Unsafe methods
     * cannot be compiled with the JIT compiler.
     */
    public static final int UNSAFE_FLAGS =
        ACC_NATIVE |
        TEMPLATE |
        UNSAFE |
        C_FUNCTION |
        VM_ENTRY_POINT |
        LOCAL_SUBSTITUTE |
        NO_SAFEPOINTS;

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
    private int flags;

    @INSPECTED
    public final Utf8Constant name;

    protected Actor(Utf8Constant name, int flags) {
        if ((flags & UNSAFE_FLAGS) != 0) {
            flags |= UNSAFE;
        }
        this.flags = flags;
        this.name = name;
    }

    @INLINE
    public final int flags() {
        return flags;
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
    public static boolean isPublic(int flags) {
        return (flags & ACC_PUBLIC) != 0;
    }

    @INLINE
    public static boolean isPrivate(int flags) {
        return (flags & ACC_PRIVATE) != 0;
    }

    @INLINE
    public static boolean isProtected(int flags) {
        return (flags & ACC_PROTECTED) != 0;
    }

    @INLINE
    public static boolean isStatic(int flags) {
        return (flags & ACC_STATIC) != 0;
    }

    @INLINE
    public static boolean isFinal(int flags) {
        return (flags & ACC_FINAL) != 0;
    }

    @INLINE
    public static boolean isSynthetic(int flags) {
        return (flags & ACC_SYNTHETIC) != 0;
    }

    @INLINE
    public static boolean isEnum(int flags) {
        return (flags & ACC_ENUM) != 0;
    }

    @INLINE
    public static boolean isAbstract(int flags) {
        return (flags & ACC_ABSTRACT) != 0;
    }

    @INLINE
    public static boolean isInterface(int flags) {
        return (flags & ACC_INTERFACE) != 0;
    }

    @INLINE
    public static boolean isInnerClass(int flags) {
        return (flags & INNER_CLASS) != 0;
    }

    @INLINE
    public static boolean isDeprecated(int flags) {
        return (flags & DEPRECATED) != 0;
    }

    @INLINE
    public static boolean isSuper(int flags) {
        return (flags & ACC_SUPER) != 0;
    }

    @INLINE
    public static boolean isAnnotation(int flags) {
        return (flags & ACC_ANNOTATION) != 0;
    }

    @INLINE
    public static boolean isBridge(int flags) {
        return (flags & ACC_BRIDGE) != 0;
    }

    @INLINE
    public static boolean isVarArgs(int flags) {
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
    public static boolean isVerified(int flags) {
        return (flags & VERIFIED) != 0;
    }

    @INLINE
    public static boolean isStrict(int flags) {
        return (flags & ACC_STRICT) != 0;
    }

    @INLINE
    public static boolean isClassInitializer(int flags) {
        return (flags & INITIALIZER) != 0 && (flags & ACC_STATIC) != 0;
    }

    @INLINE
    public static boolean isInstanceInitializer(int flags) {
        return (flags & INITIALIZER) != 0 && (flags & ACC_STATIC) == 0;
    }

    @INLINE
    public static boolean isInitializer(int flags) {
        return (flags & INITIALIZER) != 0;
    }

    @INLINE
    public static boolean isCFunction(int flags) {
        return (flags & C_FUNCTION) != 0;
    }

    @INLINE
    public static boolean isVmEntryPoint(int flags) {
        return (flags & VM_ENTRY_POINT) != 0;
    }

    @INLINE
    public static boolean isTemplate(int flags) {
        return (flags & TEMPLATE) != 0;
    }

    @INLINE
    public static boolean isReflectionStub(int flags) {
        return (flags & REFLECTION_STUB) != 0;
    }

    @INLINE
    public static boolean isRemote(int flags) {
        return (flags & REMOTE) != 0;
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
    public static boolean isLocalSubstitute(int flags) {
        return (flags & LOCAL_SUBSTITUTE) != 0;
    }

    @INLINE
    public static boolean isUnsafe(int flags) {
        return (flags & UNSAFE) != 0;
    }

    @HOSTED_ONLY
    public void setFlagsFromSubstitute(ClassMethodActor substitute) {
        final int adoptedFlagsMask = SUBSTITUTION_ADOPTED_FLAGS;
        flags |= adoptedFlagsMask & substitute.flags();
    }

    @INLINE
    public final void beUnsafe() {
        flags |= UNSAFE;
    }

    @INLINE
    public final void beVerified() {
        flags |= VERIFIED;
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

    @INLINE
    public static boolean hasFinalizer(int flags) {
        return (flags & FINALIZER) != 0;
    }

    /**
     * Gets the name of this actor qualified by it's declaring class (if known).
     * @return the qualified name of this actor
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
        return flagsString(flags);
    }

    public static String flagsString(int flags) {
        final StringBuilder sb = new StringBuilder();

        appendFlag(sb, isPublic(flags), "public ");
        appendFlag(sb, isProtected(flags), "protected ");
        appendFlag(sb, isPrivate(flags), "private ");

        /* Canonical order */
        appendFlag(sb, isAbstract(flags), "abstract ");
        appendFlag(sb, isStatic(flags), "static ");
        appendFlag(sb, isFinal(flags), "final ");
        appendFlag(sb, isTransient(flags) || isVarArgs(flags), "transient/varargs ");
        appendFlag(sb, isVolatile(flags) || isBridge(flags), "volatile/bridge ");
        appendFlag(sb, isSynchronized(flags) || isSuper(flags), "synchronized/super ");
        appendFlag(sb, isNative(flags), "native ");
        appendFlag(sb, isStrict(flags), "strictfp ");
        appendFlag(sb, isInterface(flags), "interface ");
        appendFlag(sb, isSynthetic(flags), "synthetic ");
        appendFlag(sb, isEnum(flags), "enum ");
        appendFlag(sb, isDeprecated(flags), "deprecated ");
        appendFlag(sb, isAnnotation(flags), "annotation ");

        // Implementation specific flags
        appendFlag(sb, isInjected(flags), "injected ");
        appendFlag(sb, isConstant(flags), "constant ");
        appendFlag(sb, isConstantWhenNotZero(flags), "constantWhenNotZero ");
        appendFlag(sb, isInnerClass(flags), "innerClass ");
        appendFlag(sb, isTemplate(flags), "template ");
        appendFlag(sb, isReflectionStub(flags), "reflection_stub ");
        appendFlag(sb, isVerified(flags), "verified ");
        appendFlag(sb, isInitializer(flags), "init ");
        appendFlag(sb, isCFunction(flags), "c_function ");
        appendFlag(sb, isVmEntryPoint(flags), "vm_entry ");
        appendFlag(sb, isDeclaredFoldable(flags), "fold ");
        appendFlag(sb, isUnsafe(flags), "unsafe ");
        appendFlag(sb, isLocalSubstitute(flags), "substitute ");

        if (sb.length() > 0) {
            /* trim trailing space */
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
        return "";
    }

    private static void appendFlag(StringBuilder sb, boolean flag, String string) {
        if (flag) {
            sb.append(string);
        }
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
     *     Specifier | Args         | Description                                        | Example(s)
     *     ----------+--------------+------------------------------------------------------------------------------------------
     *     'T'       |              | Qualified return/field type                        | "int" "java.lang.String"
     *     't'       |              | Unqualified return/field type                      | "int" "String"
     *     'R'       |              | Qualified return/field type                        | "int" "java.lang.String"
     *     'r'       |              | Unqualified return/field type                      | "int" "String"
     *     's'       | bci          | Source file name and line derived from bci         | "String.java:33" "Native Method" "Unknown Source"
     *     'H'       |              | Qualified holder                                   | "java.util.Map.Entry"
     *     'h'       |              | Unqualified holder                                 | "Entry"
     *     'n'       |              | Method/field/class name                            | "add"
     *     'P'       |              | Qualified parameter types, separated by ', '       | "int, java.lang.String"
     *     'p'       |              | Unqualified parameter types, separated by ', '     | "int, String"
     *     'f'       |              | The flags as a {@linkplain #flagsString() string}  | "public static final" "transient"
     *     '%'       |              | A '%' character                                    | "%"
     * </pre>
     *
     * If a specifier is given for an actor attribute that is not applicable for this actor type, then
     * the specifier is ignored or causes an {@link IllegalFormatException} depending on the value of {@code strict}.
     * For example, a "%T" in {@code format} is ignored if this object is a {@link ClassActor} instance and {@code strict == false}.
     * @param strict specifies what action to take if actor attributes that don't apply to this actor are encountered in {@code format}
     * @param format a format specification
     * @param args arguments referenced by the format specifiers in {@code format}.
     *        If there are more arguments than consumed by the format specifiers, the
     *        extra arguments are ignored.
     *
     * @return the result of formatting this method according to {@code format}
     * @throws IllegalFormatException if an illegal specifier is encountered in {@code format}
     */
    public final String format(boolean strict, String format, Object... args) throws IllegalFormatException {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        int argIndex = 0;
        while (index < format.length()) {
            final char ch = format.charAt(index++);
            if (ch == '%') {
                if (index >= format.length()) {
                    throw new UnknownFormatConversionException("An unquoted '%' character cannot terminate a method format specification");
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
                            sb.append(methodActor.descriptor().resultDescriptor().toJavaString(qualified));
                        } else if (this instanceof FieldActor) {
                            final FieldActor fieldActor = (FieldActor) this;
                            sb.append(fieldActor.descriptor().toJavaString(qualified));
                        } else {
                            if (strict) {
                                throw new IllegalFormatConversionException(specifier, getClass());
                            }
                        }
                        break;
                    }
                    case 's': {
                        final Object arg;
                        try {
                            arg = args[argIndex++];
                        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                            throw new MissingFormatArgumentException(String.valueOf(specifier));
                        }
                        final int bci;
                        try {
                            bci = (Integer) arg;
                        } catch (ClassCastException classCastException) {
                            throw new IllegalFormatConversionException(specifier, arg == null ? Object.class : arg.getClass());
                        }
                        if (this instanceof ClassMethodActor) {
                            final ClassMethodActor classMethodActor = (ClassMethodActor) this;
                            final String stackTraceLine = classMethodActor.toStackTraceElement(bci).toString();
                            sb.append(stackTraceLine.substring(stackTraceLine.indexOf('(') + 1, stackTraceLine.lastIndexOf(')')));
                        } else {
                            if (strict) {
                                throw new IllegalFormatConversionException(specifier, getClass());
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
                            ClassActor holder = memberActor.holder();
                            sb.append(holder == null ? "null" : holder.typeDescriptor.toJavaString(qualified));
                        } else {
                            if (strict) {
                                throw new IllegalFormatConversionException(specifier, getClass());
                            }
                        }
                        break;
                    }
                    case 'n': {
                        sb.append(name);
                        break;
                    }
                    case 'P':
                        qualified = true;
                        // fall through
                    case 'p': {
                        if (this instanceof MethodActor) {
                            final MethodActor methodActor = (MethodActor) this;
                            String separator = "";
                            final SignatureDescriptor signature = methodActor.descriptor();
                            for (int i = 0; i < signature.numberOfParameters(); i++) {
                                sb.append(separator).append(signature.parameterDescriptorAt(i).toJavaString(qualified));
                                separator = ", ";
                            }
                        } else {
                            if (strict) {
                                throw new IllegalFormatConversionException(specifier, getClass());
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
                        throw new UnknownFormatConversionException(String.valueOf(specifier));
                    }
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Calling this method is equivalent to calling {@link #format(String, Object...) format(format, true)}.
     *
     * @param format a format specification
     * @param args arguments referenced by the format specifiers in {@code format}.
     *        If there are more arguments than consumed by the format specifiers, the
     *        extra arguments are ignored.
     * @return the result of formatting this method according to {@code format}
     * @throws IllegalFormatException if an illegal specifier is encountered in {@code format}
     */
    public final String format(String format, Object... args) throws IllegalFormatException {
        return format(true, format, args);
    }

    @Override
    public abstract String toString();

    public abstract boolean isAccessibleBy(ClassActor accessor);

    public final void checkAccessBy(ClassActor accessor) {
        if (!isAccessibleBy(accessor)) {
            isAccessibleBy(accessor);
            throw new IllegalAccessError(accessor.name + " cannot access " + this);
        }
    }
}
