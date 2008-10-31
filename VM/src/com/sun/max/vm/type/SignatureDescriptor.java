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
package com.sun.max.vm.type;

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.type.JavaTypeDescriptor.*;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.collect.ChainedHashMapping.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * A string description of a method signature, see #4.3.3.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class SignatureDescriptor extends Descriptor {

    /**
     * The only concrete subclass of {@link TypeDescriptor}.
     * Using a subclass hides the details of storing TypeDescriptors in a {@link ChainedHashMapping}.
     */
    private static final class SignatureDescriptorEntry extends SignatureDescriptor implements ChainedHashMapping.Entry<String, SignatureDescriptorEntry> {

        private SignatureDescriptorEntry(String value, TypeDescriptor[] typeDescriptors) {
            super(value, typeDescriptors);
        }

        public String key() {
            return toString();
        }

        private Entry<String, SignatureDescriptorEntry> _next;

        public Entry<String, SignatureDescriptorEntry> next() {
            return _next;
        }

        public void setNext(Entry<String, SignatureDescriptorEntry> next) {
            _next = next;
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
    private static final GrowableMapping<String, SignatureDescriptorEntry> _canonicalSignatureDescriptors = new ChainingValueChainedHashMapping<String, SignatureDescriptorEntry>();

    private SignatureDescriptor(String value, TypeDescriptor[] typeDescriptors) {
        super(value);
        assert getClass() == SignatureDescriptorEntry.class;
        _typeDescriptors = typeDescriptors;
    }

    /**
     * The return and parameter types of this signature. The return type is at index 0 followed by the parameter types starting at index 1.
     */
    private final TypeDescriptor[] _typeDescriptors;

    /**
     * A client implements this interface to {@linkplain SignatureDescriptor#visitParameterDescriptors process} the
     * individual type descriptors in a signature.
     */
    public static interface ParameterVisitor {
        /**
         * Processes a type descriptor that describes a parameter in a signature.
         *
         * @param parameter the type descriptor describing a parameter
         */
        void visit(TypeDescriptor parameter);
    }

    /**
     * Iterates over all the parameter types described by this signature.
     *
     * @param visitor the action to be performed for each parameter type traversed
     * @param inReverse if true, then the iteration is from the last parameter to the first otherwise it's from the
     *            first parameter to the last
     */
    public final void visitParameterDescriptors(ParameterVisitor visitor, boolean inReverse) {
        if (inReverse) {
            for (int i = _typeDescriptors.length - 1; i >= 1; --i) {
                visitor.visit(_typeDescriptors[i]);
            }
        } else {
            for (int i = 1; i < _typeDescriptors.length; ++i) {
                visitor.visit(_typeDescriptors[i]);
            }
        }
    }

    private static synchronized SignatureDescriptor createSignatureDescriptor(String value, TypeDescriptor[] typeDescriptors) {
        SignatureDescriptorEntry signatureDescriptorEntry = _canonicalSignatureDescriptors.get(value);
        if (signatureDescriptorEntry == null) {
            final TypeDescriptor[] verifiedTypes;
            if (typeDescriptors == null) {
                verifiedTypes = parse(value, 0);
            } else {
                verifiedTypes = typeDescriptors;
            }
            assert verifiedTypes.length >= 1;

            signatureDescriptorEntry = new SignatureDescriptorEntry(value, verifiedTypes);
            _canonicalSignatureDescriptors.put(value, signatureDescriptorEntry);
        }
        return signatureDescriptorEntry;
    }

    public static int numberOfDescriptors() {
        return _canonicalSignatureDescriptors.length();
    }

    public static TypeDescriptor[] parse(String string, int startIndex) throws ClassFormatError {
        if ((startIndex > string.length() - 3) || string.charAt(startIndex) != '(') {
            throw classFormatError("Invalid method signature: " + string);
        }
        final List<TypeDescriptor> typeDescriptorList = new ArrayList<TypeDescriptor>();
        typeDescriptorList.add(JavaTypeDescriptor.VOID); // placeholder until the return type is parsed
        int i = startIndex + 1;
        while (string.charAt(i) != ')') {
            final TypeDescriptor descriptor = parseTypeDescriptor(string, i);
            typeDescriptorList.add(descriptor);
            i = i + descriptor.toString().length();
            if (i >= string.length()) {
                throw classFormatError("Invalid method signature: " + string);
            }
        }
        i++;
        final TypeDescriptor descriptor = parseTypeDescriptor(string, i);
        if (i + descriptor.toString().length() != string.length()) {
            throw classFormatError("Invalid method signature: " + string);
        }
        final TypeDescriptor[] typeDescriptors = typeDescriptorList.toArray(new TypeDescriptor[typeDescriptorList.size()]);
        // Plug in the return type
        typeDescriptors[0] = descriptor;
        return typeDescriptors;
    }

    public static synchronized SignatureDescriptor lookup(String string) throws ClassFormatError {
        return _canonicalSignatureDescriptors.get(string);
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

    public Kind getResultKind() {
        return getResultDescriptor().toKind();
    }

    public TypeDescriptor getResultDescriptor() {
        return _typeDescriptors[0];
    }

    public TypeDescriptor[] getParameterDescriptors() {
        final TypeDescriptor[] parameterDescriptors = new TypeDescriptor[_typeDescriptors.length - 1];
        System.arraycopy(_typeDescriptors, 1, parameterDescriptors, 0, parameterDescriptors.length);
        return parameterDescriptors;
    }

    public TypeDescriptor[] getParameterDescriptorsIncludingReceiver(TypeDescriptor receiverDescriptor) {
        final TypeDescriptor[] parameterDescriptors = new TypeDescriptor[_typeDescriptors.length];
        parameterDescriptors[0] = receiverDescriptor;
        for (int i = 1; i != parameterDescriptors.length; ++i) {
            parameterDescriptors[i] = _typeDescriptors[i];
        }
        return parameterDescriptors;
    }

    /**
     * Gets the number of local variable slots used by the parameters in this signature.
     * Long and double parameters use two slot, all other parameters use one slot.
     */
    public int getNumberOfLocals() {
        int n = 0;
        for (int i = 1; i != _typeDescriptors.length; ++i) {
            n += _typeDescriptors[i].toKind().isCategory1() ? 1 : 2;
        }
        return n;
    }

    public Kind[] getParameterKinds() {
        final Kind[] parameterKinds = new Kind[_typeDescriptors.length - 1];
        for (int i = 0; i != parameterKinds.length; ++i) {
            parameterKinds[i] = _typeDescriptors[i + 1].toKind();
        }
        return parameterKinds;
    }

    public Kind[] getParameterKindsIncludingReceiver(Kind receiverKind) {
        final Kind[] parameterKinds = new Kind[_typeDescriptors.length];
        parameterKinds[0] = receiverKind;
        for (int i = 1; i != parameterKinds.length; ++i) {
            parameterKinds[i] = _typeDescriptors[i].toKind();
        }
        return parameterKinds;
    }

    public Class[] getParameterTypes(ClassLoader classLoader) {
        final Class[] parameterTypes = new Class[_typeDescriptors.length - 1];
        for (int i = 0; i != parameterTypes.length; ++i) {
            parameterTypes[i] = _typeDescriptors[i + 1].toJava(classLoader);
        }
        return parameterTypes;
    }

    public boolean parametersEqual(SignatureDescriptor other) {
        if (_typeDescriptors.length == other._typeDescriptors.length) {
            for (int i = 1; i != _typeDescriptors.length; ++i) {
                if (_typeDescriptors[i] != other._typeDescriptors[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int getNumberOfParameters() {
        return _typeDescriptors.length - 1;
    }

    public boolean hasNoParameters() {
        return _typeDescriptors.length == 1;
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
        boolean isFirst = true;
        for (TypeDescriptor parameterDescriptor : descriptor.getParameterDescriptors()) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(parameterDescriptor.toJavaString(qualified));
            isFirst = false;
        }
        sb.append(')');
        if (appendReturnType) {
            sb.append(' ').append(descriptor.getResultDescriptor().toJavaString(qualified));
        }
        return sb.toString();
    }
}
