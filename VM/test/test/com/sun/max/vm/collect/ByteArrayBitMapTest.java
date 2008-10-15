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
}
