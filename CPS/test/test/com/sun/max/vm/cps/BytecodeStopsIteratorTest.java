/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps;

import junit.framework.*;

import com.sun.max.ide.*;
import com.sun.max.vm.cps.jit.*;

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
        for (int bcp = bsi.bci(); bcp != -1; bcp = bsi.next()) {
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
