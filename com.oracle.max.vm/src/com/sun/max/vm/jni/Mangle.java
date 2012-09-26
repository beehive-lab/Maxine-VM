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
package com.sun.max.vm.jni;

import java.lang.reflect.*;
import java.util.regex.*;

import com.sun.max.lang.*;
import com.sun.max.vm.type.*;

/**
 * A utility for mangling Java method name and signatures into C function names. Support is also provided for
 * demangling.
 *
 * @see "http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/design.html#wp615"
 */
public final class Mangle {

    private Mangle() {
    }

    // Mangling

    /**
     * Mangles a given string such that it can be represented as (part of) a valid C function name.
     */
    private static String mangle(String name) {
        final StringBuilder mangledName = new StringBuilder(100);
        final int length = name.length();
        for (int i = 0; i < length; i++) {
            final char ch = name.charAt(i);
            if (isAlphaNumeric(ch)) {
                mangledName.append(ch);
            } else if (ch == '_') {
                mangledName.append("_1");
            } else if (ch == '.') {
                mangledName.append("_");
            } else if (ch == ';') {
                mangledName.append("_2");
            } else if (ch == '[') {
                mangledName.append("_3");
            } else {
                mangledName.append(mangleChar(ch));
            }
        }

        return mangledName.toString();
    }

    /**
     * Mangles a Java method to a unique C function name in compliance with the JNI specification for resolving native
     * method names.
     *
     * @param method a Java method
     * @param withSignature if true, the method's signature is included in the mangled name
     * @return the mangled C function name for {@code method}
     */
    public static String mangleMethod(Method method, boolean withSignature) {
        return mangleMethod(JavaTypeDescriptor.forJavaClass(method.getDeclaringClass()), method.getName(), withSignature ? SignatureDescriptor.fromJava(method) : null, false);
    }

    /**
     * The delimiter in the string returned by {@link #mangleMethod(TypeDescriptor, String, SignatureDescriptor, boolean)} separating
     * the short mangled form from the suffix to be added to obtain the long mangled form.
     */
    public static final char LONG_NAME_DELIMITER = ' ';

    /**
     * Mangles a Java method to the symbol(s) to be used when binding it to a native function.
     * If {@code signature} is {@code null}, then a non-qualified symbol is returned.
     * Otherwise, a qualified symbol is returned. A qualified symbol has its non-qualified
     * prefix separated from its qualifying suffix by {@link #LONG_NAME_DELIMITER} if
     * {@code splitSuffix} is {@code true}.
     *
     * @param declaringClass a fully qualified class descriptor
     * @param name a Java method name (not checked here for validity)
     * @param signature if non-null, a method signature to include in the mangled name
     * @param splitSuffix determines if {@link #LONG_NAME_DELIMITER} should be used as described above
     * @return the symbol for the C function as described above
     */
    public static String mangleMethod(TypeDescriptor declaringClass, String name, SignatureDescriptor signature, boolean splitSuffix) {
        final StringBuilder result = new StringBuilder(100);
        final String declaringClassName = declaringClass.toJavaString();
        result.append("Java_").append(mangle(declaringClassName)).append('_').append(mangle(name));
        if (signature != null) {
            if (splitSuffix) {
                result.append(LONG_NAME_DELIMITER);
            }
            result.append("__");
            final String sig = signature.toString();
            final String parametersSignature = sig.substring(1, sig.lastIndexOf(')')).replace('/', '.').replace('$', '.');
            result.append(mangle(parametersSignature));
        }
        return result.toString();
    }

    private static String mangleChar(char ch) {
        final String s = Integer.toHexString(ch);
        assert s.length() <= 4;
        return "_0" + Strings.times('0', 4 - s.length()) + s;
    }

    private static boolean isAlphaNumeric(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }

    // Demangling

    private static final Pattern SIGNATURE_SEPARATOR = Pattern.compile("(.*)__([^0123].*)?");

    private static String demangle(String name) throws IllegalArgumentException {
        int position = 0;
        final StringBuilder sb = new StringBuilder();
        int nextUnderscore;
        while ((nextUnderscore = name.indexOf('_', position)) != -1) {
            if (nextUnderscore != position) {
                sb.append(name.substring(position, nextUnderscore));
            }
            final String tail = name.substring(nextUnderscore);
            if (tail.startsWith("_0")) {
                try {
                    if (tail.length() < 6) {
                        throw new NumberFormatException();
                    }
                    final String hexEncodedUnicodeChar = tail.substring(2, 6);
                    sb.append((char) Integer.parseInt(hexEncodedUnicodeChar, 16));
                    position = nextUnderscore + 6;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("bad Unicode character encoding at position " + nextUnderscore + ": " + name);
                }
            } else if (tail.startsWith("_1")) {
                sb.append('_');
                position = nextUnderscore + 2;
            } else if (tail.startsWith("_2")) {
                sb.append(';');
                position = nextUnderscore + 2;
            } else if (tail.startsWith("_3")) {
                sb.append('[');
                position = nextUnderscore + 2;
            } else {
                sb.append('.');
                position = nextUnderscore + 1;
            }
        }
        if (position < name.length()) {
            sb.append(name.substring(position));
        }
        return sb.toString();
    }

    /**
     * Demangles a name that was mangled in an manner equivalent to {@link #mangleMethod(Method, boolean) mangle}.
     *
     * @param mangled a mangled C function name
     * @return an object containing the demangled components of a Java method from which {@code mangled} may have been
     *         produced
     * @throws IllegalArgumentException if {@code mangled} is not a valid mangled method name
     */
    public static DemangledMethod demangleMethod(String mangled) throws IllegalArgumentException {
        if (!mangled.startsWith("Java_")) {
            throw new IllegalArgumentException("JNI mangled name must start with \"Java_\"");
        }
        String s = mangled.substring("Java_".length());
        final String qualifiedName;
        final String parametersSignature;
        final int delim = s.indexOf(LONG_NAME_DELIMITER);
        if (delim != -1) {
            s = s.substring(0, delim) + s.substring(delim + 1);
        }
        final Matcher matcher = SIGNATURE_SEPARATOR.matcher(s);
        if (matcher.matches()) {
            qualifiedName = demangle(matcher.group(1));
            final String sigMatch = matcher.group(2);
            if (sigMatch != null) {
                parametersSignature = demangle(sigMatch).replace('.', '/');
            } else {
                parametersSignature = "";
            }
        } else {
            qualifiedName = demangle(s);
            parametersSignature = null;
        }

        final int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot == -1 || qualifiedName.length() < 3) {
            throw new IllegalArgumentException();
        }

        final String className = 'L' + qualifiedName.substring(0, lastDot).replace('.', '/') + ';';
        final String name = qualifiedName.substring(lastDot + 1);
        return DemangledMethod.create(className, name, parametersSignature);
    }

    /**
     * An object containing the result of decoding a valid mangled method name.
     */
    public static final class DemangledMethod {

        private final TypeDescriptor declaringClass;
        private final String name;
        private final String parametersSignature;

        public TypeDescriptor declaringClass() {
            return declaringClass;
        }

        public String name() {
            return name;
        }

        public String parametersSignature() {
            return parametersSignature;
        }

        private static SignatureDescriptor createDummySignature(String parametersSignature) {
            if (parametersSignature == null) {
                return null;
            }
            return SignatureDescriptor.create('(' + parametersSignature + ")V");
        }

        public static DemangledMethod create(String declaringClass, String name, String parametersSignature) throws IllegalArgumentException {
            try {
                createDummySignature(parametersSignature);
                return new DemangledMethod(JavaTypeDescriptor.parseTypeDescriptor(declaringClass), name, parametersSignature);
            } catch (ClassFormatError e) {
                throw new IllegalArgumentException(e);
            }
        }

        private DemangledMethod(TypeDescriptor declaringClass, String name, String parametersSignature) {
            this.declaringClass = declaringClass;
            this.name = name;
            this.parametersSignature = parametersSignature;
        }

        /**
         * This may return null if this object was created from a mangled name of a method in a non top-level class.
         * This is because the JNI specification is not precise when it comes to the mangling of native methods in non
         * top-level classes.
         *
         * @param classLoader
         * @return null if the demangled method does not correspond to a method locatable by {@code classLoader}
         */
        public Method toJava(ClassLoader classLoader) {
            final Class declaringClass = this.declaringClass.resolveType(classLoader);
            for (Method method : declaringClass.getDeclaredMethods()) {
                if (Modifier.isNative(method.getModifiers())) {
                    if (name.equals(method.getName())) {
                        if (parametersSignature == null) {
                            return method;
                        }
                        final String signature = SignatureDescriptor.fromJava(method).toString();
                        final String parametersSignature = signature.substring(1, signature.lastIndexOf(')'));
                        if (this.parametersSignature.equals(parametersSignature)) {
                            return method;
                        }
                        if (parametersSignature.replace('$', '/').equals(this.parametersSignature)) {
                            return method;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof DemangledMethod && other.toString().equals(this.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            final String result = declaringClass.toString() + ' ' + name;
            if (parametersSignature == null) {
                return result;
            }
            return result + parametersSignature;
        }

        public String mangle() {
            return Mangle.mangleMethod(declaringClass, name, createDummySignature(parametersSignature), false);
        }
    }
}
