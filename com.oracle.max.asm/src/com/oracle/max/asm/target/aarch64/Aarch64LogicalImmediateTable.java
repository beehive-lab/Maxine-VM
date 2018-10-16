/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.asm.target.aarch64;

import java.util.*;

import com.oracle.max.asm.*;

public class Aarch64LogicalImmediateTable {
    private static final Immediate[] IMMEDIATE_TABLE = buildImmediateTable();

    private static final int ImmediateOffset = 10;
    private static final int ImmediateRotateOffset = 16;
    private static final int ImmediateSizeOffset = 22;

    /**
     * Specifies whether immediate can be represented in all cases (YES), as a 64bit instruction
     * (SIXTY_FOUR_BIT_ONLY) or not at all (NO).
     */
    enum Representable {
        YES, SIXTY_FOUR_BIT_ONLY, NO
    }

    /**
     * Tests whether an immediate can be encoded for logical instructions.
     *
     * @param is64bit   if true immediate is considered a 64-bit pattern. If false we may use a 64-bit instruction to
     *                  load the 32-bit pattern into a register.
     * @return          enum specifying whether immediate can be used for 32- and 64-bit logical instructions ({@code #Representable.YES}),
     *                  for 64-bit instructions only ({@code #Representable.SIXTY_FOUR_BIT_ONLY}) or not at all ({@code #Representable.NO}).
     */
    public static Representable isRepresentable(boolean is64bit, long immediate) {
        int pos = getLogicalImmTablePos(is64bit, immediate);
        if (pos < 0) {
            // if 32bit instruction we can try again as 64bit immediate which may succeed.
            // i.e. 0xffffffff fails as a 32bit immediate but works as 64bit one.
            if (!is64bit) {
                assert NumUtil.isUnsignedNbit(32, immediate);
                pos = getLogicalImmTablePos(true, immediate);
                return pos >= 0 ? Representable.SIXTY_FOUR_BIT_ONLY
                        : Representable.NO;
            }
            return Representable.NO;
        }
        Immediate imm = IMMEDIATE_TABLE[pos];
        return imm.only64bit() ? Representable.SIXTY_FOUR_BIT_ONLY
                : Representable.YES;
    }

    public static Representable isRepresentable(int immediate) {
        return isRepresentable(false, NumUtil.asZeroExtendedLong(immediate));
    }

    public static int getLogicalImmEncoding(boolean is64bit, long value) {
        int pos = getLogicalImmTablePos(is64bit, value);
        assert pos >= 0 : "Value cannot be represented as logical immediate";
        Immediate imm = IMMEDIATE_TABLE[pos];
        assert is64bit ||
                !imm.only64bit() : "Immediate can only be represented for 64bit, but 32bit instruction specified";
        return IMMEDIATE_TABLE[pos].encoding;
    }

    /**
     * @param is64bit if true also allow 64-bit only encodings to be returned.
     * @return If positive the return value is the position into the IMMEDIATE_TABLE for the given
     *         immediate, if negative the immediate cannot be encoded.
     */
    private static int getLogicalImmTablePos(boolean is64bit, long value) {
        if (!is64bit) {
            // 32bit instructions can only have 32bit immediates.
            if (!NumUtil.isUnsignedNbit(32, value)) {
                return -1;
            }
            // If we have a 32bit instruction (and therefore immediate) we have to duplicate it
            // across 64bit to find it in the table.
            value |= value << 32;
        }
        Immediate imm = new Immediate(value);
        int pos = Arrays.binarySearch(IMMEDIATE_TABLE, imm);
        if (pos < 0) {
            return -1;
        }
        if (!is64bit && IMMEDIATE_TABLE[pos].only64bit()) {
            return -1;
        }
        return pos;
    }

    /**
     * To quote 5.4.2: [..] an immediate is a 32 or 64 bit pattern viewed as a vector of identical
     * elements of size e = 2, 4, 8, 16, 32 or (in the case of bimm64) 64 bits. Each element
     * contains the same sub-pattern: a single run of 1 to e-1 non-zero bits, rotated by 0 to e-1
     * bits. It is encoded in the following: 10-16: rotation amount (6bit) starting from 1s in the
     * LSB (i.e. 0111->1011->1101->1110) 16-22: This stores a combination of the number of set bits
     * and the pattern size. The pattern size is encoded as follows (x is used to store the number
     * of 1 bits - 1) e pattern 2 1111xx 4 1110xx 8 110xxx 16 10xxxx 32 0xxxxx 64 xxxxxx 22: if set
     * we have an instruction with 64bit pattern?
     */
    private static final class Immediate implements Comparable<Immediate> {
        public final long imm;
        public final int encoding;

        Immediate(long imm, boolean is64, int s, int r) {
            this.imm = imm;
            this.encoding = computeEncoding(is64, s, r);
        }

        // Used to be able to binary search for an immediate in the table.
        Immediate(long imm) {
            this(imm, false, 0, 0);
        }

        /**
         * Returns true if this pattern is only representable as 64bit.
         */
        public boolean only64bit() {
            return (encoding & (1 << ImmediateSizeOffset)) != 0;
        }

        private static int computeEncoding(boolean is64, int s, int r) {
            int sf = is64 ? 1 : 0;
            return sf << ImmediateSizeOffset | r << ImmediateRotateOffset |
                    s << ImmediateOffset;
        }

        @Override
        public int compareTo(Immediate o) {
            return Long.compare(imm, o.imm);
        }
    }

    @SuppressWarnings("fallthrough")
    private static Immediate[] buildImmediateTable() {
        final int nrImmediates = 5334;
        final Immediate[] table = new Immediate[nrImmediates];
        int nrImms = 0;
        for (int logE = 1; logE <= 6; logE++) {
            int e = 1 << logE;
            long mask = NumUtil.getNbitNumberLong(e);
            for (int nrOnes = 1; nrOnes < e; nrOnes++) {
                long val = (1L << nrOnes) - 1;
                // r specifies how much we rotate the value
                for (int r = 0; r < e; r++) {
                    long immediate = (val >>> r | val << (e - r)) & mask;
                    // Duplicate pattern to fill whole 64bit range.
                    switch (logE) {
                        case 1:
                            immediate |= immediate << 2;
                            // fall through
                        case 2:
                            immediate |= immediate << 4;
                            // fall through
                        case 3:
                            immediate |= immediate << 8;
                            // fall through
                        case 4:
                            immediate |= immediate << 16;
                            // fall through
                        case 5:
                            immediate |= immediate << 32;
                    }
                    // 5 - logE can underflow to -1, but we shift this bogus result
                    // out of the masked area.
                    int sizeEncoding = (1 << (5 - logE)) - 1;
                    int s = ((sizeEncoding << (logE + 1)) & 0x3f) |
                            (nrOnes - 1);
                    table[nrImms++] = new Immediate(immediate, /* is64bit */
                            e == 64, s, r);
                }
            }
        }
        Arrays.sort(table);
        assert nrImms == nrImmediates : nrImms + " instead of " + nrImmediates +
                " in table.";
        assert checkDuplicates(table) : "Duplicate values in table.";
        return table;
    }

    private static boolean checkDuplicates(Immediate[] table) {
        for (int i = 0; i < table.length - 1; i++) {
            if (table[i].imm >= table[i + 1].imm) {
                return false;
            }
        }
        return true;
    }
}
