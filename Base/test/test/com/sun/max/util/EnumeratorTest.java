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
/*VCSID=d9fdf10c-6e11-4296-966c-35bc182bd1b0*/
package test.com.sun.max.util;

import com.sun.max.ide.*;
import com.sun.max.util.*;

/**
 * Tests for com.sun.max.util.Enumerator.
 *
 * @author Hiroshi Yamauchi
 */
public class EnumeratorTest extends MaxTestCase {

    public EnumeratorTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EnumeratorTest.class);
    }

    private static final class NonSuccessiveEnumerator<E extends Enum<E> & Enumerable<E>> extends Enumerator<E> {
        private NonSuccessiveEnumerator(Class<E> type) {
            super(type);
        }
    }
    private static enum NonSuccessiveEnum implements Enumerable<NonSuccessiveEnum> {
        E0(0), E100(100), E1000(1000);

        private final int _value;
        private NonSuccessiveEnum(int value) {
            _value = value;
        }
        public int value() {
            return _value;
        }
        public Enumerator<NonSuccessiveEnum> enumerator() {
            return new NonSuccessiveEnumerator<NonSuccessiveEnum>(NonSuccessiveEnum.class);
        }
    }

    public void test_value() {
        assertTrue(NonSuccessiveEnum.E0.ordinal() == 0);
        assertTrue(NonSuccessiveEnum.E100.ordinal() == 1);
        assertTrue(NonSuccessiveEnum.E1000.ordinal() == 2);
        assertTrue(NonSuccessiveEnum.E0.value() == 0);
        assertTrue(NonSuccessiveEnum.E100.value() == 100);
        assertTrue(NonSuccessiveEnum.E1000.value() == 1000);
    }

    public void test_enumerator() {
        final Enumerator<NonSuccessiveEnum> enumerator = NonSuccessiveEnum.E0.enumerator();
        assertTrue(enumerator.type() == NonSuccessiveEnum.class);
        int sum = 0;
        for (NonSuccessiveEnum e : enumerator) {
            sum += e.value();
        }
        assertTrue(sum == 1100);
        assertTrue(enumerator.fromValue(0) == NonSuccessiveEnum.E0);
        assertTrue(enumerator.fromValue(100) == NonSuccessiveEnum.E100);
        assertTrue(enumerator.fromValue(1000) == NonSuccessiveEnum.E1000);
        assertTrue(enumerator.fromValue(1) == null);
    }
}
