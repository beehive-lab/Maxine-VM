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
 * Tests for {@link SortedProperties}.
 *
 * @author Doug Simon
 */
public class SortedPropertiesTest extends MaxTestCase {

    public SortedPropertiesTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SortedPropertiesTest.class);
    }

    public void test() {
        final SortedMap<String, String> sortedMap = new TreeMap<String, String>();
        final Properties sortedProperties = new SortedProperties();
        final Properties systemProperties = System.getProperties();
        for (final Enumeration<?> e = systemProperties.propertyNames(); e.hasMoreElements();) {
            final String name = (String) e.nextElement();
            final String value = systemProperties.getProperty(name);
            sortedMap.put(name, value);
            sortedProperties.setProperty(name, value);
        }

        final Enumeration<Object> keys = sortedProperties.keys();
        for (String key : sortedMap.keySet()) {
            assertTrue(keys.hasMoreElements());
            assertEquals(key, keys.nextElement());
        }
        assertFalse(keys.hasMoreElements());
    }
}
