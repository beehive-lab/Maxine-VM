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
        Word word = _offsetHigh;
        assertTrue(word.asAddress().equals(word));
        assertTrue(word.asSize().equals(word));
        assertTrue(word.asPointer().equals(word));

        word = _address0;
        assertTrue(word.asOffset().equals(word));

        word = _addressLow;
        assertTrue(word.asOffset().equals(word));
        assertTrue(word.asSize().equals(word));
        assertTrue(word.asPointer().equals(word));
    }

    public void test_width() {
        for (int i = 1; i <= 1024; i++) {
            final Address address = _addressMax.unsignedShiftedRight(i);
            if (address.equals(0) || address.equals(_addressMax)) {
                for (WordWidth width : WordWidth.values()) {
                    if (width.numberOfBits == i) {
                        assertTrue(Word.width().equals(width));
                        return;
                    }
                }
                fail();
            }
        }
        fail();
    }

    public void test_equals() {
        assertFalse(_address0.equals(_addressLow));

        assertTrue(_addressLow.equals(_offsetLow));

        final Word negative = Offset.fromInt(-_low);
        assertFalse(_offsetLow.equals(negative));
    }

}
