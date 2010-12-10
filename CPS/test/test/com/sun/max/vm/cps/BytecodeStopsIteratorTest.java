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
package test.com.sun.max.vm.cps;

import junit.framework.*;

import com.sun.max.ide.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.jit.*;

/**
 * Tests {@link BytecodeStopsIterator}.
 *
 * @author Doug Simon
 */
@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class BytecodeStopsIteratorTest extends MaxTestCase {

    public static Test suite() {
        return new TestSuite(BytecodeStopsIteratorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BytecodeStopsIteratorTest.suite());
    }

    static class Stop {
        final int index;
        public Stop(int index) {
            this.index = index;
        }
    }

    static class DRCStop extends Stop {

        public DRCStop(int index) {
            super(index);
        }
    }

    static class Entry {
        final int bcp;
        final Stop[] stops;

        public Entry(int bcp, Stop... stops) {
            this.bcp = bcp;
            this.stops = stops;
        }

        static int[] encode(Entry... entries) {
            int size = 0;
            for (Entry e : entries) {
                size += 1 + e.stops.length;
            }
            final int[] table = new int[size];
            int i = 0;
            for (Entry e : entries) {
                table[i++] = e.bcp | BytecodeStopsIterator.BCP_BIT;
                for (Stop s : e.stops) {
                    if (s instanceof DRCStop) {
                        table[i++] = s.index | BytecodeStopsIterator.DIRECT_RUNTIME_CALL_BIT;
                    } else {
                        table[i++] = s.index;
                    }
                }
            }
            return table;
        }
    }

    private void assertSame(BytecodeStopsIterator bsi, Entry[] entries) {
        int i = 0;
        for (int bcp = bsi.bytecodePosition(); bcp != -1; bcp = bsi.next()) {
            final Entry entry = entries[i++];
            assertEquals(bcp, entry.bcp);
            int j = 0;
            for (int stopIndex = bsi.nextStopIndex(true); stopIndex != -1; stopIndex = bsi.nextStopIndex(false)) {
                final Stop stop = entry.stops[j++];
                assertEquals(stop.index, stopIndex);
                assertEquals(stop instanceof DRCStop, bsi.isDirectRuntimeCall());
            }

            j = 0;
            for (int stopIndex = bsi.nextStopIndex(true); stopIndex != -1; stopIndex = bsi.nextStopIndex(false)) {
                final Stop stop = entry.stops[j++];
                assertEquals(stop.index, stopIndex);
                assertEquals(stop instanceof DRCStop, bsi.isDirectRuntimeCall());
            }

            assertEquals(entry.stops.length, j);
        }
        assertEquals(entries.length, i);
    }

    public void test_valid() {
        final Entry[] entries = {
            new Entry(0, new Stop(2), new DRCStop(0)),
            new Entry(5, new Stop(1))

        };

        final BytecodeStopsIterator bsi = new BytecodeStopsIterator(Entry.encode(entries));
        assertSame(bsi, entries);
        bsi.reset();
        assertSame(bsi, entries);
    }

    public void test_invalid() {
        try {
            new BytecodeStopsIterator(new int[] {0, 2, 0, 5, 1});
            fail("Expected assertion failure");
        } catch (AssertionError e) {
        }
    }
}
