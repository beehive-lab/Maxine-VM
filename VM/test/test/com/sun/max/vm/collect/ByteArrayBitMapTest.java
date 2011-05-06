/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.collect;

import java.util.*;

import com.sun.max.ide.*;
import com.sun.max.vm.collect.*;

/**
 * Tests for {@link ByteArrayBitMap}.
 *
 * @author Doug Simon
 */
public class ByteArrayBitMapTest extends MaxTestCase {

    public ByteArrayBitMapTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ByteArrayBitMapTest.class);
    }

    public void test_all() {
        ByteArrayBitMap bitMap = new ByteArrayBitMap(0);
        try {
            bitMap.isSet(0);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }

        bitMap.setBytes(new byte[2]);
        bitMap.setSize(2);

        for (int i = 0; i != bitMap.width(); ++i) {
            bitMap.set(i);
            assertTrue(bitMap.isSet(i));
            bitMap.clear(i);
            assertFalse(bitMap.isSet(i));
        }

        final int size = ByteArrayBitMap.computeBitMapSize(100);

        bitMap = new ByteArrayBitMap(new byte[size * 2], size, size);
        final int[] bitPositions = {1, 6, 18, 23, 45, 57, 84, 85, 93, 99};
        for (int bitPosition : bitPositions) {
            bitMap.set(bitPosition);
        }

        final String bitPositionsString = Arrays.toString(bitPositions);
        final String bitMapString = bitMap.toString();
        assertEquals(bitPositionsString, bitMapString);

        int bitPositionsIndex = 0;
        for (int bitPosition = bitMap.nextSetBit(0); bitPosition >= 0; bitPosition = bitMap.nextSetBit(bitPosition + 1)) {
            final int expected = bitPositions[bitPositionsIndex++];
            assertEquals(expected, bitPosition);
        }
        assertEquals(bitPositionsIndex, bitPositions.length);
    }

    public void test_cardinality() {
        final int width = 101;
        final ByteArrayBitMap bitMap = new ByteArrayBitMap(width);
        assertEquals(0, bitMap.cardinality());
        bitMap.set(0);
        assertEquals(1, bitMap.cardinality());

        bitMap.clear(0);
        int cardinality = 0;
        for (int i = 0; i < width; i += 3) {
            bitMap.set(i);
            cardinality++;
            assertEquals(cardinality, bitMap.cardinality());
        }
    }
}
