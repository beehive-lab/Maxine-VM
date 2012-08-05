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
package test.com.sun.max.unsafe;

import junit.framework.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

public class WordTest extends WordTestCase {

    public WordTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(WordTest.class.getSimpleName());
        suite.addTestSuite(WordTest.class);
        return suite;
    }
    public static void main(String[] args) {
        junit.textui.TestRunner.run(WordTest.class);
    }

    public void test_as() {
        Word word = offsetHigh;
        assertTrue(word.asAddress().equals(word));
        assertTrue(word.asSize().equals(word));
        assertTrue(word.asPointer().equals(word));

        word = address0;
        assertTrue(word.asOffset().equals(word));

        word = addressLow;
        assertTrue(word.asOffset().equals(word));
        assertTrue(word.asSize().equals(word));
        assertTrue(word.asPointer().equals(word));
    }

    public void test_width() {
        for (int i = 1; i <= 1024; i++) {
            final Address address = addressMax.unsignedShiftedRight(i);
            if (address.equals(0) || address.equals(addressMax)) {
                for (WordWidth width : WordWidth.VALUES) {
                    if (width.numberOfBits == i) {
                        assertTrue(Word.widthValue().equals(width));
                        return;
                    }
                }
                fail();
            }
        }
        fail();
    }

    public void test_equals() {
        assertFalse(address0.equals(addressLow));

        assertTrue(addressLow.equals(offsetLow));

        final Word negative = Offset.fromInt(-low);
        assertFalse(offsetLow.equals(negative));
    }

}
