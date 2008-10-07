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
/*VCSID=204a4ce5-d0c0-4cd4-af1c-b6187ed8d782*/
package com.sun.max.asm.gen.risc.field;

import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public enum SignDependentOperations {

    UNSIGNED {
        @Override
        public int minArgumentValue(BitRange bitRange) {
            return 0;
        }

        @Override
        public int maxArgumentValue(BitRange bitRange) {
            return bitRange.valueMask();
        }

        @Override
        public int assemble(int unsignedInt, BitRange bitRange) throws IndexOutOfBoundsException {
            return bitRange.assembleUnsignedInt(unsignedInt);
        }

        @Override
        public int extract(int instruction, BitRange bitRange) {
            return bitRange.extractUnsignedInt(instruction);
        }

        @Override
        public AppendableSequence<Integer> legalTestArgumentValues(int min, int max, int grain) {
            assert min == 0;
            assert grain > 0;
            assert max >= grain;
            final AppendableSequence<Integer> result = smallContiguousRange(min, max, grain);
            return result == null ? new ArrayListSequence<Integer>(new Integer[]{0, grain, max - grain, max }) : result;
        }
    },

    SIGNED {

        @Override
        public int minArgumentValue(BitRange bitRange) {
            return -1 << (bitRange.width() - 1);
        }

        @Override
        public int maxArgumentValue(BitRange bitRange) {
            return bitRange.valueMask() >>> 1;
        }

        @Override
        public int assemble(int signedInt, BitRange bitRange) throws IndexOutOfBoundsException {
            return bitRange.assembleSignedInt(signedInt);
        }

        @Override
        public int extract(int instruction, BitRange bitRange) {
            return bitRange.extractSignedInt(instruction);
        }

        @Override
        public AppendableSequence<Integer> legalTestArgumentValues(int min, int max, int grain) {
            assert min < 0;
            assert grain > 0;
            assert max >= grain;
            final AppendableSequence<Integer> result = smallContiguousRange(min, max, grain);
            return result == null ? new ArrayListSequence<Integer>(new Integer[]{min, min + grain, -grain, 0, grain, max - grain, max }) : null;
        }
    },

    SIGNED_OR_UNSIGNED {

        @Override
        public int minArgumentValue(BitRange bitRange) {
            return SIGNED.minArgumentValue(bitRange);
        }

        @Override
        public int maxArgumentValue(BitRange bitRange) {
            return UNSIGNED.maxArgumentValue(bitRange);
        }

        @Override
        public int assemble(int value, BitRange bitRange) throws IndexOutOfBoundsException {
            return (value >= 0) ? UNSIGNED.assemble(value, bitRange) : SIGNED.assemble(value, bitRange);
        }

        @Override
        public int extract(int instruction, BitRange bitRange) {
            return UNSIGNED.extract(instruction, bitRange);
        }

        @Override
        public AppendableSequence<Integer> legalTestArgumentValues(int min, int max, int grain) {
            assert min < 0;
            assert grain > 0;
            assert max >= grain;
            final AppendableSequence<Integer> result = smallContiguousRange(min, max, grain);
            return result == null ? new ArrayListSequence<Integer>(new Integer[]{
                // We only test positive arguments, since negative ones would be returned as positive by extract()
                // and that is correct, because there is no way to tell just by the instruction which sign was meant
                0, grain, max / 2, max - grain, max
            }) : null;
        }
    };

    /**
     * Creates a sequence of all the integers inclusive between a given min and max if the
     * sequence contains 32 or less items. Otherwise, this method returns null.
     */
    public static AppendableSequence<Integer> smallContiguousRange(int min, int max, int grain) {
        final long range = (((long) max - (long) min) + 1) / grain;
        if (range > 0 && range <= 32) {
            final AppendableSequence<Integer> result = new ArrayListSequence<Integer>((int) range);
            for (int i = min; i <= max; i += grain * 2) {
                result.append(i);
            }
            return result;
        }
        return null;
    }

    public abstract int minArgumentValue(BitRange bitRange);

    public abstract int maxArgumentValue(BitRange bitRange);

    /**
     * @return instruction
     * @throws IndexOutOfBoundsException
     */
    public abstract int assemble(int value, BitRange bitRange) throws IndexOutOfBoundsException;

    public abstract int extract(int instruction, BitRange bitRange);

    public abstract AppendableSequence<Integer> legalTestArgumentValues(int min, int max, int grain);
}
