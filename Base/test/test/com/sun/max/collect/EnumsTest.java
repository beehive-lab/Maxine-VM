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
 * Tests for {@link Enums}.
 *
 * @author Michael Van De Vanter
 */
public class EnumsTest extends MaxTestCase {

    public EnumsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EnumsTest.class);
    }

    private static enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY,
        THURSDAY, FRIDAY, SATURDAY
    }

    private static enum EmptyEnum {
    }

    public void test_fullName() {
        assertEquals(Enums.fullName(Day.SUNDAY), "test.com.sun.max.collect.EnumsTest.Day.SUNDAY");
    }

    public void test_fromString() {
        assertEquals(Enums.fromString(Day.class, "THURSDAY"), Day.THURSDAY);
    }

    public void test_powerSequenceIndex() {
        assertEquals(Enums.powerSequenceIndex(Day.SUNDAY), 1);
        assertEquals(Enums.powerSequenceIndex(Day.SATURDAY), 64);
        final EnumSet<Day> weekends = java.util.EnumSet.of(Day.SATURDAY, Day.SUNDAY);
        assertEquals(Enums.powerSequenceIndex(weekends), 65);
    }

    public void test_constrain() {
        assertEquals(Enums.constrain(Day.SUNDAY, Day.MONDAY, Day.TUESDAY), Day.MONDAY);
        assertEquals(Enums.constrain(Day.MONDAY, Day.MONDAY, Day.TUESDAY), Day.MONDAY);
        assertEquals(Enums.constrain(Day.TUESDAY, Day.MONDAY, Day.TUESDAY), Day.TUESDAY);
        assertEquals(Enums.constrain(Day.WEDNESDAY, Day.MONDAY, Day.TUESDAY), Day.MONDAY);
    }

}

