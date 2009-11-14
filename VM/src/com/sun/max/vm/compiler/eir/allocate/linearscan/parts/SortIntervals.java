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
package com.sun.max.vm.compiler.eir.allocate.linearscan.parts;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;

/**
 * Sort linear scan intervals in increasing starting order.
 *
 * @author Thomas Wuerthinger
 */
public class SortIntervals extends AlgorithmPart {

    public SortIntervals() {
        super(8);
    }

    @Override
    protected void doit() {
        final Interval[] arr = new Interval[generation().variables().length()];

        int z = 0;
        for (EirVariable variable : generation().variables()) {

            final Interval interval = variable.interval;
            final int start = interval.getFirstRangeStart();

            int prev = z - 1;
            while (prev > 0 && arr[prev].getFirstRangeStart() > start) {
                arr[prev + 1] = arr[prev];
                prev--;
            }

            arr[prev + 1] = interval;

            z++;
        }

        data().setSortedIntervals(new ArrayListSequence<Interval>(arr));
    }

    @Override
    protected boolean assertPostconditions() {

        for (EirVariable variable : generation().variables()) {
            assert Sequence.Static.containsIdentical(data().sortedIntervals(), variable.interval);
        }

        int prev = Integer.MIN_VALUE;
        for (Interval i : data().sortedIntervals()) {
            assert prev <= i.getFirstRangeStart();
            prev = i.getFirstRangeStart();
        }

        return super.assertPostconditions();
    }
}
