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
package test.com.sun.max.lang;

import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public class ArraysTest extends MaxTestCase {

    public ArraysTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ArraysTest.class);
    }

    private Object[] makeObjectArray(int nElements) {
        final Object[] array = new Object[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Integer(i);
        }
        return array;
    }

    private Integer[] makeIntegerArray(int nElements) {
        final Integer[] array = new Integer[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Integer(i);
        }
        return array;
    }

    private String[] makeStringArray(int nElements) {
        final String[] array = new String[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = "string: " + i;
        }
        return array;
    }

    public void test_fromElements() {
        final Object[] original = makeObjectArray(3);
        assertTrue(Arrays.equals(original, Arrays.fromElements(original[0], original[1], original[2])));
    }

    public void test_from() {
        final Object[] original = makeObjectArray(10);
        assertTrue(Arrays.equals(original, Arrays.from(Object.class, Arrays.iterable(original))));
        assertTrue(Arrays.equals(original, Arrays.from(Object.class, original)));
    }

    public void test_iterable() {
        final Object[] original = makeObjectArray(10);
        final Iterable<Object> iterable = Arrays.iterable(original);
        int nElements = 0;
        for (Object o : iterable) {
            nElements++;
            assertTrue(Arrays.contains(original, o));
        }
        assertEquals(original.length, nElements);
    }

    public void test_collection() {
        final Object[] original = makeObjectArray(10);
        final java.util.Collection<Object> coll = Arrays.collection(original);
        assertEquals(original.length, coll.size());
        for (int i = 0; i < original.length; i++) {
            assertTrue(coll.contains(original[i]));
        }
        for (Object o : coll) {
            assertTrue(Arrays.contains(original, o));
        }
    }

    public void test_equals() {
        Integer[] a = makeIntegerArray(0);
        Integer[] b = makeIntegerArray(0);
        assertTrue(Arrays.equals(a, b));

        a = makeIntegerArray(1);
        b = makeIntegerArray(1);
        assertTrue(Arrays.equals(a, b));
        b[0] = new Integer(7);
        assertFalse(Arrays.equals(a, b)); // different element value

        b = makeIntegerArray(2);
        assertFalse(Arrays.equals(a, b)); // different length

        a = makeIntegerArray(5);
        b = makeIntegerArray(5);
        assertTrue(Arrays.equals(a, b));
        a[1] = null;
        assertFalse(Arrays.equals(a, b)); // null value
        b[1] = null;
        assertTrue(Arrays.equals(a, b));

        Object[] x = makeObjectArray(5);
        Object[] y = makeObjectArray(5);
        assertTrue(Arrays.equals(x, y));
        x[3] = "Hello";
        assertFalse(Arrays.equals(x, y)); // totally different value

        x = makeObjectArray(10);
        y = makeIntegerArray(10);
        y[2] = null;                      // null value
        assertFalse(Arrays.equals(x, y));

        x = makeIntegerArray(0);
        y = makeStringArray(0);
        assertTrue(Arrays.equals(x, y));
    }

    private void check_subArray(int nElements) {
        final Integer[] original = makeIntegerArray(nElements);
        final Integer[] array = makeIntegerArray(nElements);

        try {
            Arrays.subArray(array, -1);
            fail();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            // OK
        }

        Integer[] result = Arrays.subArray(array, 0);
        assertTrue(Arrays.equals(array, original));
        assertTrue(Arrays.equals(result, array));

        for (int n = 1; n < array.length; n++) {
            result = Arrays.subArray(array, n);
            assertTrue(result.length == array.length - n);
            for (int i = n; i < array.length; i++) {
                if (array[i] == null) {
                    assertNull(result[i - n]);
                } else {
                    assertEquals(array[i], result[i - n]);
                }
            }
        }

        result = Arrays.subArray(array, array.length);
        assertTrue(result.length == 0);

        try {
            Arrays.subArray(array, array.length + 1);
            fail();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
    }

    public void test_subArray() {
        check_subArray(0);
        check_subArray(1);
        check_subArray(5);
    }

    private void check_clone(int nElements) {
        final Integer[] original = makeIntegerArray(nElements);
        final Integer[] array = makeIntegerArray(nElements);
        final Integer[] result = Arrays.copy(array, new Integer[array.length]);
        assertTrue(Arrays.equals(array, original));
        assertTrue(Arrays.equals(result, array));
    }

    public void test_clone() {
        check_clone(0);
        check_clone(1);
        check_clone(8);
    }

    private void check_copy(int nElements, int srcPos, int dstPos, int length) {
        final Object[] src = makeObjectArray(nElements);
        final Object[] orig = makeObjectArray(nElements);  // how dst looked before copy
        final Object[] dst = new Object[nElements];
        for (int i = 0; i < nElements; i++) {
            dst[i] = orig[i];
        }
        final Object[] res = Arrays.copy(src, srcPos, dst, dstPos, length);
        for (int i = 0; i < dstPos; i++) {
            assertSame(dst[i], res[i]);
            assertSame(dst[i], orig[i]);
        }
        for (int i = dstPos; i < dstPos + length; i++) {
            assertSame(dst[i], res[i]);
            assertNotSame(dst[i], orig[i]);
            assertSame(dst[i], src[srcPos - dstPos + i]);
        }
        for (int i = dstPos + length; i < nElements; i++) {
            assertSame(dst[i], res[i]);
            assertSame(dst[i], orig[i]);
        }
    }

    public void test_copy() {
        check_copy(0, 0, 0, 0);
        check_copy(1, 0, 0, 0);
        check_copy(1, 1, 1, 0);
        check_copy(1, 0, 0, 1);
        check_copy(10, 0, 0, 0);
        check_copy(10, 0, 0, 10);
        check_copy(10, 0, 0, 3);
        check_copy(10, 4, 0, 3);
        check_copy(10, 0, 4, 3);
        check_copy(10, 4, 4, 3);
        check_copy(10, 4, 7, 3);
    }

    private void check_extend(int nElements) {
        final Object[] original = makeObjectArray(nElements);
        final Object[] newArray = Arrays.extend(original, 100);
        assertEquals(newArray.length, Math.max(100, nElements));
        assertTrue(Arrays.equals(Arrays.subArray(newArray, 0, nElements), original));
    }
    public void test_extend() {
        check_extend(0);
        check_extend(10);
        check_extend(100);
        check_extend(1000);
    }

    public void test_prepend() {
        final String[] original = makeStringArray(2);
        // Prepend to an empty array
        assertTrue(Arrays.equals(original, Arrays.prepend(new String[0], original[0], original[1])));
        // Prepend to a non-empty array
        final String[] newArray = new String[1];
        newArray[0] = original[1];
        assertTrue(Arrays.equals(original, Arrays.prepend(newArray, original[0])));
    }


    public void test_append() {
        final String[] original = makeStringArray(2);
        // Append to an empty array
        assertTrue(Arrays.equals(original, Arrays.append(new String[0], original[0], original[1])));
        assertTrue(Arrays.equals(original, Arrays.append(String.class, new String[0], original[0], original[1])));
        // Append to a non-empty array
        final String[] newArray1 = new String[1];
        newArray1[0] = original[0];
        assertTrue(Arrays.equals(original, Arrays.append(newArray1, original[1])));

        final String[] newArray2 = new String[1];
        newArray2[0] = original[0];
        assertTrue(Arrays.equals(original, Arrays.append(String.class, newArray2, original[1])));
    }

    public void test_remove() {
        final Integer[] original = makeIntegerArray(10);
        for (int index = 0; index < original.length; index++) {
            final Integer[] result = Arrays.remove(Integer.class, original, index);
            for (int i = 0; i < index; i++) {
                assertTrue(result[i] == original[i]);
            }
            for (int i = index; i < result.length; i++) {
                assertTrue(result[i] == original[i + 1]);
            }
        }
    }

    public void test_contains() {
        final Integer[] original = makeIntegerArray(10);
        assertFalse(Arrays.contains(original, null));
        assertFalse(Arrays.contains(original, new Integer(1)));
        for (int i = 0; i < original.length; i++) {
            assertTrue(Arrays.contains(original, original[i]));
        }
    }

    public void test_find() {
        final Object[] original = makeObjectArray(10);
        assertEquals(Arrays.find(original, new Integer(0)), -1);
        assertEquals(Arrays.find(original, null), -1);
        for (int i = 0; i < original.length; i++) {
            assertEquals(Arrays.find(original, original[i]), i);
        }
    }

    public void test_countElement() {
        final Integer[] original = makeIntegerArray(3);
        assertEquals(Arrays.countElement(original, null), 0);
        assertEquals(Arrays.countElement(original, new Integer(10)), 0);
        assertEquals(Arrays.countElement(original, original[1]), 1);
        original[2] = original[1];
        assertEquals(Arrays.countElement(original, original[1]), 2);
    }

    private final Predicate<Integer> _nonNegPred = new Predicate<Integer>() {
        public boolean evaluate(Integer i) {
            return i >= 0;
        }
    };
    private final Predicate<Integer> _posPred = new Predicate<Integer>() {
        public boolean evaluate(Integer i) {
            return i > 0;
        }
    };

    public void test_filter() {
        final Integer[] none = new Integer[0];
        final Integer[] int2Array = makeIntegerArray(2);
        // filter by numeric predicates
        assertTrue(Arrays.equals(Arrays.filter(int2Array, _nonNegPred, none), int2Array));
        assertFalse(Arrays.equals(Arrays.filter(int2Array, _posPred, none), int2Array));
        // filter by class
        assertTrue(Arrays.equals(Arrays.filter(int2Array, Integer.class, none), int2Array));
        final Integer[] int3Array = makeIntegerArray(3);
        int3Array[2] = null;
        assertTrue(Arrays.equals(Arrays.filter(int3Array, Integer.class, none), int2Array));
    }

    public void test_verify() {
        final Integer[] intArray = makeIntegerArray(2);
        // verify by numeric predicates
        assertTrue(Arrays.verify(intArray, _nonNegPred));
        assertFalse(Arrays.verify(intArray, _posPred));
        // verify by class
        final Object[] array = makeObjectArray(3);
        assertTrue(Arrays.verify(array, Object.class));
        assertTrue(Arrays.verify(array, Integer.class));
        array[1] = new String("str");
        assertTrue(Arrays.verify(array, Object.class));
        assertFalse(Arrays.verify(array, Integer.class));
    }

    public void test_mergeEqualElements() {
        final Object[] original = new Object[3];
        original[0] = new String("str");
        original[1] = new String("str");
        original[2] = new String("str");
        assertNotSame(original[0], original[1]);
        assertNotSame(original[0], original[2]);
        assertNotSame(original[1], original[2]);
        Arrays.mergeEqualElements(original);
        assertSame(original[0], original[1]);
        assertSame(original[1], original[2]);
    }

    public void test_flatten() {
        // Is it idempotent?
        for (int nElements = 0; nElements < 4; nElements++) {
            final Object[] original = makeObjectArray(nElements);
            assertTrue(Arrays.equals(Arrays.flatten(original.clone()), original));
        }
        // Does it flatten?
        final Object[] flat = makeObjectArray(10);
        final Object[] nested = new Object[4];
        nested[0] = new Integer(0);
        final Object[] oneTwo = new Object[2];
        oneTwo[0] = new Integer(1);
        oneTwo[1] = new Integer(2);
        nested[1] = oneTwo;
        nested[2] = new Integer(3);
        final Object[] fourNine = new Object[4];
        fourNine[0] = new Integer(4);
        final Object[] fiveSeven = new Object[3];
        fiveSeven[0] = new Integer(5);
        fiveSeven[1] = new Integer(6);
        fiveSeven[2] = new Integer(7);
        fourNine[1] = fiveSeven;
        fourNine[2] = new Integer(8);
        fourNine[3] = new Integer(9);
        nested[3] = fourNine;
        assertTrue(Arrays.equals(Arrays.flatten(nested), flat));
    }

}
