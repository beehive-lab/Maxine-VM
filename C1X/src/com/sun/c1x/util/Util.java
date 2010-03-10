/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.util;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ir.Value;
import com.sun.c1x.ir.Instruction;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ri.*;

/**
 * The {@code Util} class contains a motley collection of utility methods used throughout the compiler.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class Util {

    public static final int K = 1024;
    public static final int M = 1024 * 1024;
    public static final int PRINTING_LINE_WIDTH = 40;
    public static final char SECTION_CHARACTER = '*';
    public static final char SUB_SECTION_CHARACTER = '=';
    public static final char SEPERATOR_CHARACTER = '-';

    public static RuntimeException unimplemented() {
        throw new Error("unimplemented");
    }

    public static RuntimeException unimplemented(String msg) {
        throw new Error("unimplemented:" + msg);
    }

    public static RuntimeException shouldNotReachHere() {
        throw new Error("should not reach here");
    }

    public static <T> boolean replaceInList(T a, T b, List<T> list) {
        final int max = list.size();
        for (int i = 0; i < max; i++) {
            if (list.get(i) == a) {
                list.set(i, b);
                return true;
            }
        }
        return false;
    }

    public static <T> boolean replaceAllInList(T a, T b, List<T> list) {
        final int max = list.size();
        for (int i = 0; i < max; i++) {
            if (list.get(i) == a) {
                list.set(i, b);
            }
        }
        return false;
    }

    /**
     * Checks whether the specified integer is a power of two.
     *
     * @param val the value to check
     * @return {@code true} if the value is a power of two; {@code false} otherwise
     */
    public static boolean isPowerOf2(int val) {
        return val != 0 && (val & val - 1) == 0;
    }

    /**
     * Checks whether the specified long is a power of two.
     *
     * @param val the value to check
     * @return {@code true} if the value is a power of two; {@code false} otherwise
     */
    public static boolean isPowerOf2(long val) {
        return val != 0 && (val & val - 1) == 0;
    }

    /**
     * Computes the log (base 2) of the specified integer, rounding down.
     * (E.g {@code log2(8) = 3}, {@code log2(21) = 4})
     *
     * @param val the value
     * @return the log base 2 of the value
     */
    public static int log2(int val) {
        assert val > 0 && isPowerOf2(val);
        return 31 - Integer.numberOfLeadingZeros(val);
    }

    /**
     * Computes the log (base 2) of the specified long, rounding down.
     * (E.g {@code log2(8) = 3}, {@code log2(21) = 4})
     *
     * @param val the value
     * @return the log base 2 of the value
     */
    public static int log2(long val) {
        assert val > 0 && isPowerOf2(val);
        return 63 - Long.numberOfLeadingZeros(val);
    }

    public static int align(int size, int align) {
        assert isPowerOf2(align);
        return (size + align - 1) & ~(align - 1);
    }

    /**
     * Gets a word with the nth bit set.
     * @param n the nth bit to set
     * @return an integer value with the nth bit set
     */
    public static int nthBit(int n) {
        return (n >= Integer.SIZE ? 0 : 1 << (n));
    }

    /**
     * Gets a word with the right-most n bits set.
     * @param n the number of rigth most bits to set
     * @return an integer value with the right-most n bits set
     */
    public static int rightNBits(int n) {
        return nthBit(n) - 1;
    }

    /**
     * Statically cast an object to an arbitrary Object type. Dynamically checked.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Class<T> type, Object object) {
        return (T) object;
    }

    /**
     * Statically cast an object to an arbitrary Object type. Dynamically checked.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object object) {
        return (T) object;
    }

    private static String internalNameToJava(String name) {
        switch (name.charAt(0)) {
            case 'L':
                return name.substring(1, name.length() - 1).replace('/', '.');
            case '[':
                return internalNameToJava(name.substring(1)) + "[]";
            default:
                if (name.length() != 1) {
                    throw new IllegalArgumentException("Illegal internal name: " + name);
                }
                return CiKind.fromPrimitiveOrVoidTypeChar(name.charAt(0)).javaName;
        }
    }

    /**
     * Converts a given type to its Java programming language name. The following are examples of strings returned by
     * this method:
     *
     * <pre>
     *     qualified == true:
     *         java.lang.Object
     *         int
     *         boolean[][]
     *     qualified == false:
     *         Object
     *         int
     *         boolean[][]
     * </pre>
     *
     * @param riType the type to be converted to a Java name
     * @param qualified specifies if the package prefix of the type should be included in the returned name
     * @return the Java name corresponding to {@code riType}
     */
    public static String toJavaName(RiType riType, boolean qualified) {
        CiKind kind = riType.kind();
        if (kind.isPrimitive() || kind == CiKind.Void) {
            return kind.javaName;
        }
        String string = internalNameToJava(riType.name());
        if (qualified) {
            return string;
        }
        final int lastDot = string.lastIndexOf('.');
        if (lastDot != -1) {
            string = string.substring(lastDot + 1);
        }
        return string;
    }

    /**
     * Converts a given type to its Java programming language name. The following are examples of strings returned by
     * this method:
     *
     * <pre>
     *      java.lang.Object
     *      int
     *      boolean[][]
     * </pre>
     *
     * @param riType the type to be converted to a Java name
     * @return the Java name corresponding to {@code riType}
     */
    public static String toJavaName(RiType riType) {
        return internalNameToJava(riType.name());
    }

    /**
     * Gets a string for a given method formatted according to a given format specification. A format specification is
     * composed of characters that are to be copied verbatim to the result and specifiers that denote an attribute of
     * the method that is to be copied to the result. A specifier is a single character preceded by a '%' character. The
     * accepted specifiers and the method attributes they denote are described below:
     *
     * <pre>
     *     Specifier | Description                                          | Example(s)
     *     ----------+------------------------------------------------------------------------------------------
     *     'R'       | Qualified return type                                | "int" "java.lang.String"
     *     'r'       | Unqualified return type                              | "int" "String"
     *     'H'       | Qualified holder                                     | "java.util.Map.Entry"
     *     'h'       | Unqualified holder                                   | "Entry"
     *     'n'       | Method name                                          | "add"
     *     'P'       | Qualified parameter types, separated by ', '         | "int, java.lang.String"
     *     'p'       | Unqualified parameter types, separated by ', '       | "int, String"
     *     'f'       | Indicator if method is unresolved, static or virtual | "unresolved" "static" "virtual"
     *     '%'       | A '%' character                                      | "%"
     * </pre>
     *
     * @param format a format specification
     * @param method the method to be formatted
     * @param kinds if {@code true} then the types in {@code method}'s signature are printed in the
     *            {@linkplain CiKind#jniName JNI} form of their {@linkplain CiKind kind}
     * @return the result of formatting this method according to {@code format}
     * @throws IllegalFormatException if an illegal specifier is encountered in {@code format}
     */
    public static String format(String format, RiMethod method, boolean kinds) throws IllegalFormatException {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        RiSignature sig = method.signatureType();
        while (index < format.length()) {
            final char ch = format.charAt(index++);
            if (ch == '%') {
                if (index >= format.length()) {
                    throw new UnknownFormatConversionException("An unquoted '%' character cannot terminate a method format specification");
                }
                final char specifier = format.charAt(index++);
                boolean qualified = false;
                switch (specifier) {
                    case 'R':
                        qualified = true;
                        // fall through
                    case 'r': {
                        sb.append(kinds ? sig.returnKind().jniName : toJavaName(sig.returnType(), qualified));
                        break;
                    }
                    case 'H':
                        qualified = true;
                        // fall through
                    case 'h': {
                        sb.append(toJavaName(method.holder(), qualified));
                        break;
                    }
                    case 'n': {
                        sb.append(method.name());
                        break;
                    }
                    case 'P':
                        qualified = true;
                        // fall through
                    case 'p': {
                        for (int i = 0; i < sig.argumentCount(false); i++) {
                            if (i != 0) {
                                sb.append(", ");
                            }
                            sb.append(kinds ? sig.argumentKindAt(i).jniName : toJavaName(sig.argumentTypeAt(i), qualified));
                        }
                        break;
                    }
                    case 'f': {
                        sb.append(!method.isLoaded() ? "unresolved" : method.isStatic() ? "static" : "virtual");
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
     * Gets a string for a given field formatted according to a given format specification. A format specification is
     * composed of characters that are to be copied verbatim to the result and specifiers that denote an attribute of
     * the field that is to be copied to the result. A specifier is a single character preceded by a '%' character. The
     * accepted specifiers and the field attributes they denote are described below:
     *
     * <pre>
     *     Specifier | Description                                          | Example(s)
     *     ----------+------------------------------------------------------------------------------------------
     *     'T'       | Qualified type                                       | "int" "java.lang.String"
     *     't'       | Unqualified type                                     | "int" "String"
     *     'H'       | Qualified holder                                     | "java.util.Map.Entry"
     *     'h'       | Unqualified holder                                   | "Entry"
     *     'n'       | Field name                                           | "age"
     *     'f'       | Indicator if field is unresolved, static or instance | "unresolved" "static" "instance"
     *     '%'       | A '%' character                                      | "%"
     * </pre>
     *
     * @param format a format specification
     * @param field the field to be formatted
     * @param kinds if {@code true} then {@code field}'s type is printed in the
     *            {@linkplain CiKind#jniName JNI} form of its {@linkplain CiKind kind}
     * @return the result of formatting this field according to {@code format}
     * @throws IllegalFormatException if an illegal specifier is encountered in {@code format}
     */
    public static String format(String format, RiField field, boolean kinds) throws IllegalFormatException {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        RiType type = field.type();
        while (index < format.length()) {
            final char ch = format.charAt(index++);
            if (ch == '%') {
                if (index >= format.length()) {
                    throw new UnknownFormatConversionException("An unquoted '%' character cannot terminate a field format specification");
                }
                final char specifier = format.charAt(index++);
                boolean qualified = false;
                switch (specifier) {
                    case 'T':
                        qualified = true;
                        // fall through
                    case 't': {
                        sb.append(kinds ? type.kind().jniName : toJavaName(type, qualified));
                        break;
                    }
                    case 'H':
                        qualified = true;
                        // fall through
                    case 'h': {
                        sb.append(toJavaName(field.holder(), qualified));
                        break;
                    }
                    case 'n': {
                        sb.append(field.name());
                        break;
                    }
                    case 'f': {
                        sb.append(!field.isLoaded() ? "unresolved" : field.isStatic() ? "static" : "instance");
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
     * Converts a Java source-language class name into the internal form.
     *
     * @param className the class name
     * @return the internal name form of the class name
     */
    public static String toInternalName(String className) {
        return "L" + className.replace('.', '/') + ";";
    }

    /**
     * Utility method to combine a base hash with the identity hash of one or more objects.
     *
     * @param hash the base hash
     * @param x the object to add to the hash
     * @return the combined hash
     */
    public static int hash1(int hash, Object x) {
        // always set at least one bit in case the hash wraps to zero
        return 0x10000000 | (hash + 7 * System.identityHashCode(x));
    }

    /**
     * Utility method to combine a base hash with the identity hash of one or more objects.
     *
     * @param hash the base hash
     * @param x the first object to add to the hash
     * @param y the second object to add to the hash
     * @return the combined hash
     */
    public static int hash2(int hash, Object x, Object y) {
        // always set at least one bit in case the hash wraps to zero
        return 0x20000000 | (hash + 7 * System.identityHashCode(x) + 11 * System.identityHashCode(y));
    }

    /**
     * Utility method to combine a base hash with the identity hash of one or more objects.
     *
     * @param hash the base hash
     * @param x the first object to add to the hash
     * @param y the second object to add to the hash
     * @param z the third object to add to the hash
     * @return the combined hash
     */
    public static int hash3(int hash, Object x, Object y, Object z) {
        // always set at least one bit in case the hash wraps to zero
        return 0x30000000 | (hash + 7 * System.identityHashCode(x) + 11 * System.identityHashCode(y) + 13 * System.identityHashCode(z));
    }

    /**
     * Utility method to combine a base hash with the identity hash of one or more objects.
     *
     * @param hash the base hash
     * @param x the first object to add to the hash
     * @param y the second object to add to the hash
     * @param z the third object to add to the hash
     * @param w the fourth object to add to the hash
     * @return the combined hash
     */
    public static int hash4(int hash, Object x, Object y, Object z, Object w) {
        // always set at least one bit in case the hash wraps to zero
        return 0x40000000 | (hash + 7 * System.identityHashCode(x) + 11 * System.identityHashCode(y) + 13 * System.identityHashCode(z) + 17 * System.identityHashCode(w));
    }

    static {
        assert log2(2) == 1;
        assert log2(4) == 2;
        assert log2(8) == 3;
        assert log2(16) == 4;
        assert log2(32) == 5;
        assert log2(0x40000000) == 30;

        assert log2(2L) == 1;
        assert log2(4L) == 2;
        assert log2(8L) == 3;
        assert log2(16L) == 4;
        assert log2(32L) == 5;
        assert log2(0x4000000000000000L) == 62;

        assert !isPowerOf2(3);
        assert !isPowerOf2(5);
        assert !isPowerOf2(7);
        assert !isPowerOf2(-1);

        assert isPowerOf2(2);
        assert isPowerOf2(4);
        assert isPowerOf2(8);
        assert isPowerOf2(16);
        assert isPowerOf2(32);
        assert isPowerOf2(64);
    }

    /**
     * Sets the element at a given position of a list and ensures that this position exists. If the list is current
     * shorter than the position, intermediate positions are filled with a given value.
     *
     * @param list the list to put the element into
     * @param pos the position at which to insert the element
     * @param x the element that should be inserted
     * @param filler the filler element that is used for the intermediate positions in case the list is shorter than pos
     */
    public static <T> void atPutGrow(List<T> list, int pos, T x, T filler) {
        if (list.size() < pos + 1) {
            while (list.size() < pos + 1) {
                list.add(filler);
            }
            assert list.size() == pos + 1;
        }

        assert list.size() >= pos + 1;
        list.set(pos, x);
    }

    public static void breakpoint() {
        // do nothing.
    }

    public static void guarantee(boolean b, String string) {
        if (!b) {
            throw new CiBailout(string);
        }
    }

    public static boolean is8bit(long l) {
        return l < 128 && l >= -128;
    }

    public static void warning(String string) {
    }

    public static int safeToInt(long l) {
        assert (int) l == l;
        return (int) l;
    }

    public static boolean traceLinearScan(int level, String string, Object... objects) {
        if (C1XOptions.TraceLinearScanLevel >= level) {
            TTY.println(String.format(string, objects));
        }
        return true;
    }

    public static int roundUp(int number, int mod) {
        return ((number + mod - 1) / mod) * mod;
    }

    public static void truncate(List<?> instructions, int length) {
        while (instructions.size() > length) {
            instructions.remove(instructions.size() - 1);
        }
    }

    public static void printBytes(String name, byte[] array, int bytesPerLine) {
        printBytes(name, array, array.length, bytesPerLine);
    }

    public static void printSection(String name, char sectionCharacter) {

        String header = " " + name + " ";
        int remainingCharacters = PRINTING_LINE_WIDTH - header.length();
        int leftPart = remainingCharacters / 2;
        int rightPart = remainingCharacters - leftPart;
        for (int i = 0; i < leftPart; i++) {
            TTY.print(sectionCharacter);
        }

        TTY.print(header);

        for (int i = 0; i < rightPart; i++) {
            TTY.print(sectionCharacter);
        }

        TTY.println();
    }

    public static void printBytes(String name, byte[] array, int length, int bytesPerLine) {
        assert bytesPerLine > 0;
        TTY.println("%s: %d bytes", name, length);
        for (int i = 0; i < length; i++) {
            TTY.print("%02x ", array[i]);
            if (i % bytesPerLine == bytesPerLine - 1) {
                TTY.println();
            }
        }

        if (length % bytesPerLine != bytesPerLine - 1) {
            TTY.println();
        }
    }

    public static CiKind[] signatureToKinds(RiSignature signature, boolean withReceiver) {
        int args = signature.argumentCount(false);
        CiKind[] result;
        int i = 0;
        if (withReceiver) {
            result = new CiKind[args + 1];
            result[0] = CiKind.Object;
            i = 1;
        } else {
            result = new CiKind[args];
        }
        for (int j = 0; j < args; j++) {
            result[i + j] = signature.argumentKindAt(j);
        }
        return result;
    }

    public static <T> T nonFatalUnimplemented(T val) {
        if (C1XOptions.FatalUnimplemented) {
            throw new Error("unimplemented");
        }
        return val;
    }

    public static int nonFatalUnimplemented(int val) {
        if (C1XOptions.FatalUnimplemented) {
            throw new Error("unimplemented");
        }
        return val;
    }

    public static boolean nonFatalUnimplemented(boolean val) {
        if (C1XOptions.FatalUnimplemented) {
            throw new Error("unimplemented");
        }
        return val;
    }

    public static boolean isShiftCount(int x) {
        return 0 <= x && x < 32;
    }

    public static void nonFatalUnimplemented() {
        if (C1XOptions.FatalUnimplemented) {
            throw new Error("unimplemented");
        }
    }

    public static boolean isByte(int x) {
        return 0 <= x && x < 0x100;
    }

    public static boolean is8bit(int x) {
        return -0x80 <= x && x < 0x80;
    }

    public static boolean is16bit(int x) {
        return -0x8000 <= x && x < 0x8000;
    }

    public static short safeToShort(int v) {
        assert is16bit(v);
        return (short) v;
    }

    /**
     * Utility method to check that two instructions have the same kind.
     * @param i the first instruction
     * @param other the second instruction
     * @return {@code true} if the instructions have the same kind
     */
    public static boolean equalKinds(Value i, Value other) {
        return i.kind == other.kind;
    }

    /**
     * Checks that two instructions are equivalent, optionally comparing constants.
     * @param x the first instruction
     * @param y the second instruction
     * @param compareConstants {@code true} if equivalent constants should be considered equivalent
     * @return {@code true} if the instructions are equivalent; {@code false} otherwise
     */
    public static boolean equivalent(Instruction x, Instruction y, boolean compareConstants) {
        if (x == y) {
            return true;
        }
        if (compareConstants && x != null && y != null) {
            if (x.isConstant() && x.asConstant().equivalent(y.asConstant())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a given instruction to a value string. The representation of an instruction as
     * a value is formed by concatenating the {@linkplain com.sun.c1x.ci.CiKind#typeChar character} denoting its
     * {@linkplain com.sun.c1x.ir.Instruction#kind kind} and its {@linkplain com.sun.c1x.ir.Instruction#id}. For example,
     * "i13".
     *
     * @param value the instruction to convert to a value string. If {@code value == null}, then "null" is returned.
     * @return the instruction representation as a string
     */
    public static String valueString(Value value) {
        return value == null ? "null" : "" + value.kind.typeChar + value.id();
    }

}
