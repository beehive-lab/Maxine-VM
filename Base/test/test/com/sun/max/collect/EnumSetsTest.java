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
/*VCSID=520601e4-6bed-4ad5-a807-3f46c6c3d2ee*/
package test.com.sun.max.collect;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;

/**
 * Tests for {@link EnumSets}.
 *
 * @author Michael Van De Vanter
 */
public class EnumSetsTest extends MaxTestCase {

    public EnumSetsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EnumSetsTest.class);
    }

    private static enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY,
        THURSDAY, FRIDAY, SATURDAY
    }

    public void test_of() {
        final Day[] noDays = new Day[0];
        final EnumSet<Day> empty = EnumSets.of(Day.class, noDays);
        assertEquals(empty.size(), 0);
        assertFalse(empty.contains(new Object()));

        final Day[] weekendDays = {Day.SATURDAY, Day.SUNDAY};
        final EnumSet<Day> weekend = EnumSets.of(Day.class, weekendDays);
        assertEquals(weekend.size(), 2);
        assertTrue(weekend.contains(Day.SATURDAY));
        assertTrue(weekend.contains(Day.SUNDAY));
        assertFalse(weekend.contains(Day.MONDAY));
        assertFalse(weekend.contains(new Object()));
    }

    public void test_union() {
        final Day[] weekendDays = {Day.SATURDAY, Day.SUNDAY};
        final Day[] productiveDays = {Day.TUESDAY, Day.WEDNESDAY, Day.THURSDAY};
        final EnumSet<Day> weekend = EnumSets.of(Day.class, weekendDays);
        final EnumSet<Day> productive = EnumSets.of(Day.class, productiveDays);
        final Set<Day> useful = EnumSets.union(weekend, productive);
        assertEquals(useful.size(), 5);
        assertTrue(useful.contains(Day.SATURDAY));
        assertTrue(useful.contains(Day.TUESDAY));
        assertFalse(useful.contains(Day.MONDAY));
    }

}
