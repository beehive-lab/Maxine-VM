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
 * Tests for {@link Iterators}.
 *
 * @author Michael Van De Vanter
 */
public class IteratorsTest extends MaxTestCase {

    public IteratorsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IteratorsTest.class);
    }

    public void testEmpty() {
        final Iterator<Integer> noInts = Iterators.empty();
        assertFalse(noInts.hasNext());
    }

    public void testJoin() {
        final Collection<String> s1 = Arrays.asList("1", "2", "3", "4");
        final Collection<String> s2 = Arrays.asList("5", "6", "7", "8");

        final Collection<String> result = new ArrayList<String>(s1);
        result.addAll(s2);

        final Iterator<String> joinedIterator = Iterators.join(s1.iterator(), s2.iterator());
        for (final Iterator<String> i = result.iterator(); i.hasNext();) {
            assertTrue(joinedIterator.hasNext());
            assertEquals(i.next(), joinedIterator.next());
        }
        assertFalse(joinedIterator.hasNext());
    }
}
