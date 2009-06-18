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
package test.com.sun.max.collect;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;

/**
 * Tests for {@link Iterables}.
 *
 * @author Hiroshi Yamauchi
 */
public class IterablesTest extends MaxTestCase {

    public IterablesTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IterablesTest.class);
    }

    public void test_from() {
        final Iterable<Integer> iterable = Iterables.from(0, 1, 2);
        int sum = 0;
        for (Integer i : iterable) {
            sum += i;
        }
        assertTrue(sum == 3);
    }

    private static final class StringEnumeration implements Enumeration<String> {
        private String[] strings;
        private int index;

        private StringEnumeration(String... strings) {
            this.strings = strings;
            this.index = 0;
        }

        public boolean hasMoreElements() {
            if (index < strings.length) {
                return true;
            }
            return false;
        }

        public String nextElement() {
            return strings[index++];
        }
    }

    public void test_fromEnumeration() {
        final StringEnumeration enumeration = new StringEnumeration(new String[]{"I", "am", "a", "cat"});
        final Iterable<String> iterable = Iterables.fromEnumeration(enumeration);
        String concat = "";
        for (String s : iterable) {
            concat += s;
        }
        assertTrue(concat.equals("Iamacat"));
    }

    public void test_empty() {
        final Iterable<String> iterable = Iterables.empty();
        int counter = 0;
        String concat = "";
        for (String s : iterable) {
            concat += s;
            counter++;
        }
        assertEquals(counter, 0);
    }

    public void test_flatten1() {
        final Integer[] lowNums = {0, 1, 2};
        final Iterable<Integer> low = Arrays.asList(lowNums);
        final Vector<Integer> high = new Vector<Integer>(2);
        high.add(3);
        high.add(4);

        final LinkedList <Iterable<Integer>> all = new LinkedList <Iterable<Integer>>();
        all.add(low);
        all.add(high);
        final Iterable<Integer> result = Iterables.flatten1(all);
        final Set<Integer> resultSet = new HashSet<Integer>();
        for (Integer theInt : result) {
            resultSet.add(theInt);
        }
        assertEquals(resultSet.size(), 5);
        assertTrue(resultSet.contains(lowNums[0]));
        assertTrue(resultSet.contains(lowNums[1]));
        assertTrue(resultSet.contains(lowNums[2]));
        assertTrue(resultSet.contains(high.get(0)));
        assertTrue(resultSet.contains(high.get(1)));
    }

}
