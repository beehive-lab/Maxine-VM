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
package test.com.sun.max.unsafe;

import java.util.*;

import com.sun.max.ide.*;
import com.sun.max.lang.*;

/**
 * Tests for com.sun.max.util.Objects.
 *
 * @author Hiroshi Yamauchi
 */
public class ObjectsTest extends MaxTestCase {

    public ObjectsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ObjectsTest.class);
    }

    public void test_clone() {
        final int[] original = new int[100];
        for (int i = 0; i < original.length; i++) {
            original[i] = i;
        }
        final int[] clone = Objects.clone(original);
        for (int i = 0; i < 100; i++) {
            assertTrue(clone[i] == original[i]);
        }
    }

    private static final class C {
        String _f1;
        Integer _f2;
    }

    public void test_getClasses() {
        final Class[] classes = Objects.getClasses("abc", 1, new HashMap());
        assertTrue(classes[0] == String.class);
        assertTrue(classes[1] == Integer.class);
        assertTrue(classes[2] == HashMap.class);
    }
}
