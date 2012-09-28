/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Implementation of a special "<em>dark matter</em>" object type, used to fill free space that cannot be allocated, so that:
 * <ul>
 * <li>Memory management related operations that walk over memory regions comprising dark matter pay no extra cost for skipping dark matter. Examples
 * include sweep operations of  mark-sweep collector, and dirty card scanning in a generational GC with a card-table based remembered set. </li>
 * <li>The inspector can easily and unambiguously distinguish dark matter.</li>
 * </ul>
 * To achieve the above, each span of dark memory is formatted as a special type of object that can be neither allocated nor referenced directly
 * by application code.
 * Because dark matter can be of arbitrary size, it is formatted as an instance of the special scalar array type represented by the
 * {@linkplain DarkMatter#DARK_MATTER_ARRAY DARK_MATTER_ARRAY} array class actor.  This class:
 * <ul>
 * <li>has no symbolic definition;</li>
 * <li>is not registered in the class registry;</li>
 * <li>cannot be named in Java; and</li>
 * <li>cannot be instantiated via reflection.</li>
 * </ul>
 * This class is otherwise equivalent in layout to a long array.  As any array can have zero length, the minimum size for an instance is
 * three words.  The smallest pieces of heap space, which are two words wide, are formatted specially as instances of the class {@link SmallestDarkMatter}.
 */
public final class DarkMatter {

    @INSPECTED
    public static final String DARK_MATTER_CLASS_NAME = "dark matter []";

    @INSPECTED
    public static final ArrayClassActor<LongValue> DARK_MATTER_ARRAY =
        new ArrayClassActor<LongValue>(ClassRegistry.LONG, SymbolTable.makeSymbol(DARK_MATTER_CLASS_NAME));

    public static final DarkMatterLogger logger = new DarkMatterLogger();

    /**
     * Boot image generation initialization. The logger may not be initialized properly during boot image generation.
     * Calling this during heap scheme initialization forces its initialization.
     */
    @HOSTED_ONLY
    public static void initialize() {
        logger.checkOptions();
    }

    /**
     * Variable-less class used to format the smallest possible dark-matter (i.e., two-words space).
     */
    public static class SmallestDarkMatter {
        @FOLD
        static DynamicHub hub() {
            return ClassActor.fromJava(SmallestDarkMatter.class).dynamicHub();
        }

        @FOLD
        static Word hubOrigin() {
            return Reference.fromJava(hub()).toOrigin();
        }

        static void format(Address darkMatter) {
            final Pointer origin = Layout.cellToOrigin(darkMatter.asPointer());
            Layout.writeHubReference(origin, Reference.fromJava(hub()));
            Layout.writeMisc(origin, Word.zero());
            // FIXME: Tracing  here may lead to issue with GC if used when retiring TLABs during mutator allocation.
            if (logger.enabled()) {
                logger.logFormatSmall(darkMatter);
            }
        }

        private SmallestDarkMatter() {
        }
    }

    private DarkMatter() {
    }

    @FOLD
    public static Size minSize() {
        return SmallestDarkMatter.hub().tupleSize;
    }

    @FOLD
    private static DynamicHub hub() {
        return DARK_MATTER_ARRAY.dynamicHub();
    }

    @FOLD
    private static Size darkMatterHeaderSize() {
        return Layout.longArrayLayout().getArraySize(Kind.LONG, 0);
    }

    @FOLD
    private static Word hubOrigin() {
        return Reference.fromJava(hub()).toOrigin();
    }

    /**
     * Tells whether the value of a word equals the address of a DarkMatter hub.
     * @param hubWord a word value
     * @return true if the word value is the origin of a dark matter hub.
     */
    public static boolean isDarkMatterHub(Word hubWord) {
        return hubWord.equals(hubOrigin()) || hubWord.equals(SmallestDarkMatter.hubOrigin());
    }

    @NEVER_INLINE
    private static void reportInvalidDarkMatterRange(Address start, Size size) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.printRange(start, start.plus(size), false);
        Log.print(" (");
        Log.print(size);
        Log.print(")");
        Log.unlock(lockDisabledSafepoints);
        FatalError.unexpected("Not enough space to format Dark Matter");
    }

    private static void plantDarkMatter(Address start, Size size)  {
        final Pointer origin = Layout.cellToOrigin(start.asPointer());
        final int length = size.minus(darkMatterHeaderSize()).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
        Layout.writeHubReference(origin, Reference.fromJava(hub()));
        Layout.writeMisc(origin, Word.zero());
        if (MaxineVM.isDebug()) {
            origin.writeWord(Layout.arrayLayout().arrayLengthOffset(), Word.zero());
        }
        Layout.writeArrayLength(origin, length);
        if (MaxineVM.isDebug()) {
            Memory.setWords(start.plus(darkMatterHeaderSize()).asPointer(), length, Memory.zappedMarker());
        }
        // FIXME: Tracing  here may lead to issue with GC if used when retiring TLABs during mutator allocation.
        if (logger.enabled()) {
            logger.logFormat(start, start.plus(size));
        }
    }

    @FOLD
    private static Size maxDarkMatterSize() {
        return Size.G.shiftedLeft(Word.widthValue().log2numberOfBytes);
    }
    /**
     * Format a word-aligned heap region as dark matter. A {@linkplain FatalError} is raised if the region is less than two-words wide.
     * @param start address to the first word of the region
     * @param size size of the region
     */
    public static void format(Address start, Size size) {
        if (size.greaterThan(minSize())) {
            // Can't use DarkMatter array for formatting very large region (length encoded as. Need to slice it into smaller region.
            while (size.greaterThan(maxDarkMatterSize())) {
                FatalError.breakpoint();
                plantDarkMatter(start, maxDarkMatterSize());
                start = start.plus(maxDarkMatterSize());
                size = size.minus(maxDarkMatterSize());
            }
            plantDarkMatter(start, size);
        } else if (size.equals(minSize())) {
            SmallestDarkMatter.format(start);
        } else {
            reportInvalidDarkMatterRange(start, size);
        }
    }

    /**
      * Format a word-aligned heap region as dark matter. A {@linkplain FatalError} is raised if the region is less than two-words wide.
     * @param start address to the first word of the region
     * @param end  address to the end of the last word of the region
     */
    public static void format(Address start, Address end) {
        format(start, end.minus(start).asSize());
    }

    /*
     * Interface for logging heap resizing decisions made by the GenSSHeapSizingPolicy.
     * The interface uses long instead of Size to improve human-readability from the inspector's log views.
     */
    @HOSTED_ONLY
    @VMLoggerInterface(defaultConstructor = true)
    private interface DarkMatterLoggerInterface {
        void format(
                        @VMLogParam(name = "start") Address start,
                        @VMLogParam(name = "end") Address end);
        void formatSmall(
                        @VMLogParam(name = "start") Address start);
    }

    static final class DarkMatterLogger extends DarkMatterLoggerAuto {
        DarkMatterLogger() {
            super("DarkMatter", "Dark Matter Formation");
        }
        @Override
        protected void traceFormat(Address start, Address end) {
            Log.print("dark matter @ ");
            Log.printRange(start, end, true);
        }

        @Override
        protected void traceFormatSmall(Address start) {
            Log.print("small dark matter @ ");
            Log.println(start);
        }
    }

// START GENERATED CODE
    private static abstract class DarkMatterLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Format, FormatSmall;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected DarkMatterLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        protected DarkMatterLoggerAuto() {
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logFormat(Address start, Address end) {
            log(Operation.Format.ordinal(), start, end);
        }
        protected abstract void traceFormat(Address start, Address end);

        @INLINE
        public final void logFormatSmall(Address start) {
            log(Operation.FormatSmall.ordinal(), start);
        }
        protected abstract void traceFormatSmall(Address start);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Format
                    traceFormat(toAddress(r, 1), toAddress(r, 2));
                    break;
                }
                case 1: { //FormatSmall
                    traceFormatSmall(toAddress(r, 1));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
