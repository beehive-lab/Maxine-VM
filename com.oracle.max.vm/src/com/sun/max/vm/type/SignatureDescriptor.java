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
package com.sun.max.vm.type;

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.type.JavaTypeDescriptor.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.collect.ChainedHashMapping.Entry;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;

/**
 * A string description of a method signature, see #4.3.3.
 */
public abstract class SignatureDescriptor extends Descriptor implements RiSignature {

    /**
     * The only concrete subclass of {@link TypeDescriptor}.
     * Using a subclass hides the details of storing TypeDescriptors in a {@link ChainedHashMapping}.
     */
    private static final class SignatureDescriptorEntry extends SignatureDescriptor implements ChainedHashMapping.Entry<String, SignatureDescriptorEntry> {

        SignatureDescriptorEntry(String value, TypeDescriptor[] typeDescriptors) {
            super(value, typeDescriptors);
        }

        public String key() {
            return toString();
        }

        private Entry<String, SignatureDescriptorEntry> next;

        public Entry<String, SignatureDescriptorEntry> next() {
            return next;
        }

        public void setNext(Entry<String, SignatureDescriptorEntry> next) {
            this.next = next;
        }

        public void setValue(SignatureDescriptorEntry value) {
            assert value == this;
        }

        public SignatureDescriptorEntry value() {
            return this;
        }
    }

    /**
     * Searching and adding entries to this map is only performed by
     * {@linkplain #createSignatureDescriptor(String, TypeDescriptor[]) one method} which is synchronized.
     */
    private static final Mapping<String, SignatureDescriptorEntry> canonicalSignatureDescriptors = new ChainingValueChainedHashMapping<String, SignatureDescriptorEntry>();

    static {
        // Ensures Kind is initialized
        Kind.fromJava(Object.class);
    }

    SignatureDescriptor(String value, TypeDescriptor[] typeDescriptors) {
        super(value);
        assert getClass() == SignatureDescriptorEntry.class;
        this.typeDescriptors = typeDescriptors;
        int n = 0;
        for (int i = 1; i != typeDescriptors.length; ++i) {
            n += typeDescriptors[i].toKind().stackSlots;
        }
        numberOfSlots = n;
    }

    /**
     * The return and parameter types of this signature. The return type is at index 0 followed by the parameter types starting at index 1.
     */
    private final TypeDescriptor[] typeDescriptors;

    public final int numberOfSlots;

    private static synchronized SignatureDescriptor createSignatureDescriptor(String value, TypeDescriptor[] typeDescriptors) {
        SignatureDescriptorEntry signatureDescriptorEntry = canonicalSignatureDescriptors.get(value);
        if (signatureDescriptorEntry == null) {
            final TypeDescriptor[] verifiedTypes;
            if (typeDescriptors == null) {
                verifiedTypes = parse(value, 0);
            } else {
                verifiedTypes = typeDescriptors;
            }
            assert verifiedTypes.length >= 1;

            signatureDescriptorEntry = new SignatureDescriptorEntry(value, verifiedTypes);
            canonicalSignatureDescriptors.put(value, signatureDescriptorEntry);
        }
        return signatureDescriptorEntry;
    }

    @HOSTED_ONLY
    public static int totalNumberOfDescriptors() {
        return canonicalSignatureDescriptors.length();
    }

    /**
     * Determines if a given signature contains any word types.
     *
     * @param signature the signature to test
     */
    public static boolean containsWord(SignatureDescriptor signature) {
        for (TypeDescriptor type : signature.typeDescriptors) {
            if (type.toKind().isWord) {
                return true;
            }
        }
        return false;
    }

    public static TypeDescriptor[] parse(String string, int startIndex) throws ClassFormatError {
        if ((startIndex > string.length() - 3) || string.charAt(startIndex) != '(') {
            throw classFormatError("Invalid method signature: " + string);
        }
        final List<TypeDescriptor> typeDescriptorList = new ArrayList<TypeDescriptor>();
        typeDescriptorList.add(JavaTypeDescriptor.VOID); // placeholder until the return type is parsed
        int i = startIndex + 1;
        while (string.charAt(i) != ')') {
            final TypeDescriptor descriptor = parseTypeDescriptor(string, i, true);
            typeDescriptorList.add(descriptor);
            i = i + descriptor.toString().length();
            if (i >= string.length()) {
                throw classFormatError("Invalid method signature: " + string);
            }
        }
        i++;
        final TypeDescriptor descriptor = parseTypeDescriptor(string, i, true);
        if (i + descriptor.toString().length() != string.length()) {
            throw classFormatError("Invalid method signature: " + string);
        }
        final TypeDescriptor[] typeDescriptors = typeDescriptorList.toArray(new TypeDescriptor[typeDescriptorList.size()]);
        // Plug in the return type
        typeDescriptors[0] = descriptor;
        return typeDescriptors;
    }

    public static synchronized SignatureDescriptor lookup(String string) throws ClassFormatError {
        return canonicalSignatureDescriptors.get(string);
    }

    public static SignatureDescriptor create(String string) throws ClassFormatError {
        return createSignatureDescriptor(string, null);
    }

    public static SignatureDescriptor create(Utf8Constant utf8Constant) throws ClassFormatError {
        return create(utf8Constant.toString());
    }

    public static SignatureDescriptor create(Class returnType, Class... parameterTypes) {
        final StringBuilder sb = new StringBuilder("(");
        final TypeDescriptor[] typeDescriptors = new TypeDescriptor[1 + parameterTypes.length];
        int i = 1;
        for (Class parameterType : parameterTypes) {
            final TypeDescriptor parameterTypeDescriptor = forJavaClass(parameterType);
            typeDescriptors[i++] = parameterTypeDescriptor;
            sb.append(parameterTypeDescriptor);
        }
        final TypeDescriptor returnTypeDescriptor = forJavaClass(returnType);
        typeDescriptors[0] = returnTypeDescriptor;
        sb.append(")").append(returnTypeDescriptor);
        return createSignatureDescriptor(sb.toString(), typeDescriptors);
    }

    public static Kind[] kindsFromJava(Class[] types) {
        final Kind[] kinds = new Kind[types.length];
        for (int i = 0; i < types.length; i++) {
            kinds[i] = forJavaClass(types[i]).toKind();
        }
        return kinds;
    }

    public Kind resultKind() {
        return resultDescriptor().toKind();
    }

    /**
     * Gets the type descriptor of the return type in this signature object.
     */
    public TypeDescriptor resultDescriptor() {
        return typeDescriptors[0];
    }

    /**
     * Gets the number of local variable slots used by the parameters in this signature.
     * Long and double parameters use two slots, all other parameters use one slot.
     */
    public int computeNumberOfSlots() {
        return numberOfSlots;
    }

    /**
     * Copies the kinds of the parameters in this signature object into a given array.
     *
     * @param dst the array into which the parameter kinds are to be copied. If {@code null}, then a new array of length
     *            {@code getNumberOfParameters() + dstOffset} is allocated and used as the destination of the copy instead.
     * @param dstOffset the offset in the returned array at which to start writing
     * @return the array into which the parameter kinds were copied
     */
    public Kind[] copyParameterKinds(Kind[] dst, int dstOffset) {
        final Kind[] result = dst == null ? new Kind[dstOffset + typeDescriptors.length - 1] : dst;
        int dstIndex = dstOffset;
        for (int i = 1; i != typeDescriptors.length; ++i) {
            result[dstIndex++] = typeDescriptors[i].toKind();
        }
        return result;
    }

    /**
     * Resolves the parameter types in this signature object to an array of classes.
     *
     * @param classLoader the class loader used to resolve each parameter type descriptor to a class
     * @return the resolved array of classes
     */
    public Class[] resolveParameterTypes(ClassLoader classLoader) {
        final Class[] parameterTypes = new Class[typeDescriptors.length - 1];
        for (int i = 0; i != parameterTypes.length; ++i) {
            parameterTypes[i] = typeDescriptors[i + 1].resolveType(classLoader);
        }
        return parameterTypes;
    }

    /**
     * Resolves the return type in this signature object to a class.
     *
     * @param classLoader the class loader used to resolve the return type descriptor to a class
     * @return the resolved return type as a class
     */
    public Class resolveReturnType(ClassLoader classLoader) {
        return typeDescriptors[0].resolveType(classLoader);
    }

    public boolean parametersEqual(SignatureDescriptor other) {
        if (typeDescriptors.length == other.typeDescriptors.length) {
            for (int i = 1; i != typeDescriptors.length; ++i) {
                if (typeDescriptors[i] != other.typeDescriptors[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the number of parameters denoted by this signature.
     *
     * To iterate over the parameters of a signature object {@code sig}, use the following loop:
     * <pre>
     *    for (int i = 0; i <= sig.numberOfParameters(); i++) {
     *        final TypeDescriptor parameter = sig.parameterDescriptorAt(i);
     *        // operate on 'parameter'
     *    }
     * </pre>
     */
    @INLINE
    public final int numberOfParameters() {
        return typeDescriptors.length - 1;
    }

    /**
     * Gets the parameter within this signature at a given index.
     *
     * @param index the index of the parameter to return
     *
     * @see #numberOfParameters()
     */
    @INLINE
    public final TypeDescriptor parameterDescriptorAt(int index) {
        return typeDescriptors[index + 1];
    }

    public static SignatureDescriptor fromJava(Method method) {
        return fromJava(method.getReturnType(), method.getParameterTypes());
    }

    public static SignatureDescriptor fromJava(Constructor constructor) {
        return fromJava(Void.TYPE, constructor.getParameterTypes());
    }

    public static SignatureDescriptor fromJava(Class returnType, Class... parameterTypes) {
        return create(returnType, parameterTypes);
    }

    /**
     * Gets a string representation of this descriptor that resembles a Java source language declaration.
     * For example:
     *
     * <pre>
     *     SignatureDescriptor.create("()Ljava/lang/String;").toJavaString(true, true) returns "() java.lang.String"
     *     SignatureDescriptor.create("(IZLjava/util/Map;)V").toJavaString(true, true) returns "(int, boolean, java.util.Map) void"
     *     SignatureDescriptor.create("()Ljava/lang/String;").toJavaString(true, false) returns "()"
     *     SignatureDescriptor.create("(IZLjava/util/Map;)V").toJavaString(true, false) returns "(int, boolean, java.util.Map)"
     *     SignatureDescriptor.create("()Ljava/lang/String;").toJavaString(false, false) returns "()"
     *     SignatureDescriptor.create("(IZLjava/util/Map;)V").toJavaString(false, false) returns "(int, boolean, Map)"
     *     SignatureDescriptor.create("()Ljava/lang/String;").toJavaString(false, true) returns "() String"
     *     SignatureDescriptor.create("(IZLjava/util/Map;)V").toJavaString(false, true) returns "(int, boolean, Map) void"
     * </pre>
     *
     * @param qualified
     *                specifies if the types in the returned value should be qualified
     * @param appendReturnType
     *                specifies if the return type should be appended to the returned value (padded by a space character)
     *
     * @return a string representation of this descriptor that resembles a Java source language declaration
     */
    public String toJavaString(boolean qualified, boolean appendReturnType) {
        final SignatureDescriptor descriptor = this;
        final StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < descriptor.numberOfParameters(); i++) {
            final TypeDescriptor parameterDescriptor = descriptor.parameterDescriptorAt(i);
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(parameterDescriptor.toJavaString(qualified));
        }
        sb.append(')');
        if (appendReturnType) {
            sb.append(' ').append(descriptor.resultDescriptor().toJavaString(qualified));
        }
        return sb.toString();
    }

    public static final SignatureDescriptor VOID = create("()V");

    public int argumentCount(boolean receiver) {
        return numberOfParameters() + (receiver ? 1 : 0);
    }

    public RiType argumentTypeAt(int index, RiType accessingClass) {
        return UnresolvedType.toRiType(parameterDescriptorAt(index), accessingClass);
    }

    @Override
    public CiKind argumentKindAt(int index, boolean architecture) {
        return WordUtil.ciKind(parameterDescriptorAt(index).toKind(), architecture);
    }

    public final RiType returnType(RiType accessingClass) {
        return UnresolvedType.toRiType(resultDescriptor(), accessingClass);
    }

    @Override
    public final CiKind returnKind(boolean architecture) {
        return WordUtil.ciKind(resultDescriptor().toKind(), architecture);
    }

    public final String asString() {
        return toString();
    }

    public final int argumentSlots(boolean withReceiver) {
        return numberOfSlots + (withReceiver ? 1 : 0);
    }

}
