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
package test.com.sun.max.vm.type;

import static com.sun.max.vm.type.JavaTypeDescriptor.*;
import junit.framework.*;

import com.sun.max.vm.type.*;

/**
 * Tests for {@link TypeDescriptor}.
 *
 * @author Athul Acharya
 * @author Ben L. Titzer
 */

public class TypeDescriptorTest extends TestCase {

    public TypeDescriptorTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TypeDescriptorTest.class);
    }

    private TypeDescriptor arrayOf(TypeDescriptor descriptor, int dimensions) {
        return getArrayDescriptorForDescriptor(descriptor, dimensions);
    }

    public void test_arrayOf() {
        assertTrue(arrayOf(BYTE, 1).toString().equals("[B"));
        assertTrue(arrayOf(BYTE, 2).toString().equals("[[B"));
        assertTrue(arrayOf(STRING, 1).toString().equals("[Ljava/lang/String;"));
        //the following test shouldn't work, because this:
        //TypeDescriptor.VOID.arrayOf().arrayOf().toJava(void.class.getClassLoader())
        //causes an exception.
        assertTrue(arrayOf(VOID, 2).toString().equals("[[V"));
    }

    public void test_forNameSyntax() {
        assertTrue(forNameOk("java.lang.Object"));
        assertTrue(forNameOk("java.lang.String"));
        assertTrue(forNameOk("C.B.A"));
        assertTrue(forNameOk("A12.B23.C00"));
        assertTrue(forNameOk("[Ljava.lang.Object;"));
        assertTrue(forNameOk("[LA12.lang.Object;"));
        assertTrue(forNameOk("[Ljava.c34.Object;"));
        assertTrue(forNameOk("[I"));
        assertFalse(forNameOk("Ljava/lang/Object"));
        assertFalse(forNameOk("j/k"));
        assertFalse(forNameOk("j."));
        assertFalse(forNameOk("J;"));
        assertFalse(forNameOk("a.b.c.e."));
        assertFalse(forNameOk("7"));
        assertFalse(forNameOk("Jave/78;"));
        assertFalse(forNameOk("8"));

        assertFalse(forNameOk("int[]"));
        assertFalse(forNameOk("java.lang.Object[]"));
        assertFalse(forNameOk("[java.lang"));
        assertFalse(forNameOk("[java/lang"));
    }

    public void test_array255() {
        assertTrue(forNameOk("[[[[[[[[[[[[[[[[[I"));
        assertTrue(forNameOk(intArrayOfDimensions(200)));
        assertTrue(forNameOk(intArrayOfDimensions(250)));
        assertTrue(forNameOk(intArrayOfDimensions(253)));
        assertTrue(forNameOk(intArrayOfDimensions(254)));
        assertTrue(forNameOk(intArrayOfDimensions(255)));
        assertFalse(forNameOk(intArrayOfDimensions(256)));
    }

    public void test_parseArray() {
        assertTrue(parseTypeDescriptor("[[I").toString().equals("[[I"));
        assertTrue(JavaTypeDescriptor.getArrayDimensions(parseTypeDescriptor("[[I")) == 2);

        assertTrue(parseTypeDescriptor("[[[[Ljava/lang/Object;").toString().equals("[[[[Ljava/lang/Object;"));
        assertTrue(JavaTypeDescriptor.getArrayDimensions(parseTypeDescriptor("[[[[Ljava/lang/Object;")) == 4);

    }

    public void test_identity() {
        assertTrue(parseTypeDescriptor("[[[[B") == parseTypeDescriptor("[[[[B"));
        assertTrue(parseTypeDescriptor("[[[[B") == parseMangledArrayOrUnmangledClassName("[[[[B"));
        assertTrue(parseTypeDescriptor("Ljava/lang/Object;") == parseMangledArrayOrUnmangledClassName("java.lang.Object"));

        assertTrue(JavaTypeDescriptor.VOID == parseTypeDescriptor("V"));
        assertTrue(JavaTypeDescriptor.BYTE == parseTypeDescriptor("B"));
        assertTrue(JavaTypeDescriptor.BOOLEAN == parseTypeDescriptor("Z"));
        assertTrue(JavaTypeDescriptor.SHORT == parseTypeDescriptor("S"));
        assertTrue(JavaTypeDescriptor.CHAR == parseTypeDescriptor("C"));
        assertTrue(JavaTypeDescriptor.INT == parseTypeDescriptor("I"));
        assertTrue(JavaTypeDescriptor.FLOAT == parseTypeDescriptor("F"));
        assertTrue(JavaTypeDescriptor.LONG == parseTypeDescriptor("J"));
        assertTrue(JavaTypeDescriptor.DOUBLE == parseTypeDescriptor("D"));
    }

    public void test_primitiveClasses() {
        assertTrue(resolveToJavaClass(parseTypeDescriptor("V"), null) == void.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("B"), null) == byte.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("Z"), null) == boolean.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("S"), null) == short.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("C"), null) == char.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("I"), null) == int.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("F"), null) == float.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("J"), null) == long.class);
        assertTrue(resolveToJavaClass(parseTypeDescriptor("D"), null) == double.class);
    }

    String intArrayOfDimensions(int dims) {
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < dims; i++) {
            buffer.append('[');
        }
        buffer.append('I');
        return buffer.toString();
    }

    boolean forNameOk(String string) {
        try {
            return parseMangledArrayOrUnmangledClassName(string) != null;
        } catch (ClassFormatError e) {
            return false;
        }
    }

    //note: this test necessarily tests the 'L' case of TypeDescriptor.parse(),
    //and calls to TypeDescriptor.create() with Lslashified(...);
    public void test_forTupleType() throws ClassFormatError {
        assertTrue(getDescriptorForWellFormedTupleName("java.lang.String").toString().equals("Ljava/lang/String;"));
        assertTrue(getDescriptorForWellFormedTupleName("java.lang.Object").toString().equals("Ljava/lang/Object;"));
        assertTrue(getDescriptorForWellFormedTupleName("java.lang.Cloneable").toString().equals("Ljava/lang/Cloneable;"));
    }

    //note: this test necessarily also tests TypeDescriptor.isArrayTypeDescriptor()
    public void test_bottomElementType() {
        assertTrue(arrayOf(BYTE, 1).elementTypeDescriptor().toString().equals("B"));
        assertTrue(arrayOf(BYTE, 2).elementTypeDescriptor().toString().equals("B"));
        assertTrue(BYTE.elementTypeDescriptor().toString().equals("B"));
        assertTrue(arrayOf(STRING, 1).elementTypeDescriptor().toString().equals("Ljava/lang/String;"));
    }

    public void test_fromJava() {
        //fromJava(Class)
        assertTrue(forJavaClass(TypeDescriptor.class).toString().equals("Lcom/sun/max/vm/type/TypeDescriptor;"));
        assertTrue(forJavaClass(Integer.class).toString().equals("Ljava/lang/Integer;"));
        assertTrue(forJavaClass(int.class).toString().equals("I"));
        assertTrue(forJavaClass(int[].class).toString().equals("[I"));

        //fromJava(String)
        assertTrue(getDescriptorForJavaString(TypeDescriptor.class.getCanonicalName()).toString().equals("Lcom/sun/max/vm/type/TypeDescriptor;"));
        assertTrue(getDescriptorForJavaString(Integer.class.getCanonicalName()).toString().equals("Ljava/lang/Integer;"));
        assertTrue(getDescriptorForJavaString(int.class.getCanonicalName()).toString().equals("I"));
        assertTrue(getDescriptorForJavaString(int[].class.getCanonicalName()).toString().equals("[I"));
    }

    //note: this test necessarily also tests the rest of TypeDescriptor.parse()
    public void test_create() throws ClassFormatError {
        TypeDescriptor t;

        assertTrue(parseTypeDescriptor("I").toString().equals("I"));
        assertTrue(parseTypeDescriptor("[[B").toString().equals("[[B"));

        try {
            t = parseTypeDescriptor("IB");
            fail("\"IB\" is not a valid type descriptor string: got " + t);
        } catch (ClassFormatError e) { }

        try {
            t = parseTypeDescriptor("[B[I");
            fail("\"[B[I\" is not a valid type descriptor string: got " + t);
        } catch (ClassFormatError e) { }

        try {
            t = parseTypeDescriptor("235");
            fail("\"235\" is not a valid type descriptor string: got " + t);
        } catch (ClassFormatError e) { }

        try {
            t = parseTypeDescriptor("Ljava.lang.String");
            fail("\"Ljava.lang.String\" is not a valid type descriptor string: got " + t);
        } catch (ClassFormatError e) { }

        try {
            t = parseTypeDescriptor("Ljava.lang.String;;");
            fail("\"Ljava.lang.String;;\" is not a valid type descriptor string: got " + t);
        } catch (ClassFormatError e) { }

        try {
            t = parseTypeDescriptor("");
            fail("\"\" is not a valid type descriptor string: got " + t);
        } catch (ClassFormatError e) { }
    }

    public void test_toJavaString() {
        assertTrue(forJavaClass(TypeDescriptor.class).toJavaString().equals("com.sun.max.vm.type.TypeDescriptor"));
        assertTrue(forJavaClass(Integer.class).toJavaString().equals("java.lang.Integer"));
        assertTrue(forJavaClass(int.class).toJavaString().equals("int"));
        assertTrue(forJavaClass(int[].class).toJavaString().equals("int[]"));
    }

    public void test_toJava() {
        assertTrue(forJavaClass(TypeDescriptor.class).resolveType(TypeDescriptor.class.getClassLoader()) == TypeDescriptor.class);
        assertTrue(forJavaClass(Integer.class).resolveType(Integer.class.getClassLoader()) == Integer.class);
        assertTrue(forJavaClass(int.class).resolveType(int.class.getClassLoader()) == int.class);
        assertTrue(forJavaClass(int[].class).resolveType(int.class.getClassLoader()) == int[].class);
        assertTrue(VOID.resolveType(void.class.getClassLoader()) == void.class);
    }
}
