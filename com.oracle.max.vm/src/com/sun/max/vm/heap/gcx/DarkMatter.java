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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An helper class to fill free space that cannot be allocated with instances of distinguishable "dark matter" object types, so that:
 * <ul>
 * <li>Memory management related operations that walk over a memory regions comprising dark matter don't pay extra cost for skipping the dark matter (for example,
 * sweep operations of  mark-sweep collector, or dirty card scanning in a generational GC with a card-table based remembered set). </li>
 * <li> The inspector can easily and unambiguously distinguish dark matter.</li>
 * <ul>
 * To achieve the above, dark matter is formatted as special objects that cannot be allocated nor reference directly by application code.
 * Because dark matter can be of arbitrary size, it is formatted as an instance of the special dark matter scalar array type represented by the
 *  {@linkplain DarkMatter#DARK_MATTER_ARRAY} array class actor.
 * This type has no symbolic definition, cannot be named in Java nor instantiated via reflection. In particular, it isn't registered in the class registry.
 * It is otherwise equivalent in layout to an long array, and as any array can have a length of 0. Thus, the minimum size for a dark matter array is
 * three-words, and it cannot be used to format the smallest pieces of heap space which are only two-words wide. These are instead formatted as
 * instance of the class SmallestDarkMatter.
 */
public final class DarkMatter {
    @INSPECTED
    private static final ArrayClassActor<LongValue> DARK_MATTER_ARRAY =
        new ArrayClassActor<LongValue>(ClassRegistry.LONG, SymbolTable.makeSymbol("dark matter []"));

    /**
     * Variable-less class used to format the smallest possible dark-matter (i.e., two-words space).
     */
    private static class SmallestDarkMatter {

        @INTRINSIC(UNSAFE_CAST)
        private static native SmallestDarkMatter asSmallestDarkMatter(Object darkMatter);

        static SmallestDarkMatter toDarkMatter(Address cell) {
            return asSmallestDarkMatter(Reference.fromOrigin(Layout.cellToOrigin(cell.asPointer())).toJava());
        }

        @FOLD
        static DynamicHub hub() {
            return ClassActor.fromJava(SmallestDarkMatter.class).dynamicHub();
        }

        @FOLD
        static Word hubWord() {
            return Reference.fromJava(hub()).toOrigin();
        }

        static void format(Address darkMatter) {
            final Pointer origin = Layout.cellToOrigin(darkMatter.asPointer());
            Layout.writeHubReference(origin, Reference.fromJava(hub()));
            Layout.writeMisc(origin, Word.zero());
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
    private static Word hubWord() {
        return Reference.fromJava(hub()).toOrigin();
    }

    /**
     * Tells whether an address is the origin of a cell formatted as dark matter.
     * @param origin heap address
     * @return true if the address is the origin of a cell formatted as dark matter.
     */
    public static boolean isDarkMatterOrigin(Address origin) {
        final Word hubWord =  origin.asPointer().readWord(Layout.hubIndex());
        return hubWord.equals(hubWord()) && hubWord.equals(SmallestDarkMatter.hubWord());
    }

    /**
     * Size of a cell formatted as dark matter. Raises a {@linkplain FatalError} if the address is not the start of some dark matter.
     * @param address address of a cell formatted as a dark matter.
     * @return Size size of the dark matter cell
     */
    public static Size darkMatterSize(Address address) {
        final Word hubWord =  address.asPointer().readWord(Layout.hubIndex());
        if (hubWord.equals(hubWord())) {
            return Size.fromInt(Layout.readArrayLength(address.asPointer()));
        }
        FatalError.check(hubWord.equals(SmallestDarkMatter.hubWord()), "not dark matter origin");
        return minSize();
    }

    /**
     * Format a word-aligned heap region as dark matter. A {@linkplain FatalError} is raised if the region is less than two-words wide.
     * @param start address to the first word of the region
     * @param size size of the region
     */
    public static void format(Address start, Size size) {
        if (size.greaterThan(minSize())) {
            final Pointer origin = Layout.cellToOrigin(start.asPointer());
            final int length = size.minus(darkMatterHeaderSize()).unsignedShiftedRight(Word.widthValue().log2numberOfBytes).toInt();
            Layout.writeHubReference(origin, Reference.fromJava(hub()));
            Layout.writeMisc(origin, Word.zero());
            Layout.writeArrayLength(origin, length);
            if (MaxineVM.isDebug()) {
                Memory.setWords(start.plus(darkMatterHeaderSize()).asPointer(), length, Memory.zappedMarker());
            }
        } else if (size.equals(minSize())) {
            SmallestDarkMatter.format(start);
        } else {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("[");
            Log.print(start);
            Log.print(",");
            Log.print(start.plus(size));
            Log.print(" (");
            Log.print(size);
            Log.print(")");
            Log.unlock(lockDisabledSafepoints);
            FatalError.unexpected("invalid dark matter size");
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
}
