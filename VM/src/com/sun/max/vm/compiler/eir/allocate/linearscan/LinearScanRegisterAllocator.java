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
package com.sun.max.vm.compiler.eir.allocate.linearscan;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.parts.*;

/**
 * Linear scan register allocator based on the register allocator described by Christian Wimmer in
 * his master thesis "Linear Scan Register Allocation for the Java HotSpot Client Compiler".
 *
 * @author Thomas Wuerthinger
 */
public abstract class LinearScanRegisterAllocator<EirRegister_Type extends EirRegister> extends EirAllocator<EirRegister_Type> {

    // Debugging flags, may be turned off to increase performance
    public static final boolean PHASE_TIMING = false;
    public static final boolean DETAILED_TIMING = false;
    public static final boolean DETAILED_COUNTING = true;
    public static final boolean DETAILED_ASSERTIONS = false;

    public static final String METRICS_PREFIX = "LSRA";

    /**
     * The different phases of the algorithm.
     */
    private final AlgorithmPart[] phases = new AlgorithmPart[]{

        /**
         * Phase 1: Allocate constants and split variables when a fixed register is required.
         */
        new Prologue(),

        /**
         * Phase 2: Detect loops and loop depth of each block.
         */
        new DetectLoops(),

        /**
         * Phase 3: Compute an optimized block order for the linear scan processing.
         */
        new ComputeBlockOrder(),

        /**
         * Phase 4: Give consecutive even numbers to the instructions based on the linear scan order.
         */
        new NumberInstructions(),

        /**
         * Phase 5: Iterative live set calculation.
         */
        new ComputeLiveSets(false),

        /**
         * Phase 6: Simulate a random execution pass and log results for verification after register allocation.
         */
        new VerifyAllocation(true),

        /**
         * Phase 7: Build intervals for all variables.
         */
        new BuildIntervals(),

        /**
         * Phase 8: Sort the intervals in ascending start order.
         */
        new SortIntervals(),

        /**
         * Phase 9: Walk the intervals and allocate a register for each intervals. If allocation of a register is not possible,
         * intervals are split.
         */
        new WalkIntervals(),

        /**
         * Phase 10: Resolve data flow by inserting moves at block borders. Possibly intermediate blocks are inserted as
         * semantically the moves need to be inserted at block edges.
         */
        new ResolveDataFlow(),

        /**
         * Phase 11: Update the live sets as they are needed by code generation.
         */
        new ComputeLiveSets(true),

        /**
         * Phase 12: Simulate the same random execution pass as in the previous VerifyAllocation run. Results must be the same
         * when register allocation is correct.
         */
        new VerifyAllocation(false),

        /**
         * Phase 13: Remove moves where source and destination location is the same.
         */
        new RemoveRedundantMoves()
    };


    /**
     * Constructor for the linear scan allocator.
     * @param methodGeneration the object on which the register allocation is performed
     */
    public LinearScanRegisterAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
    }

    /**
     * Converts a given register pool set to a register array.
     * @param registers the pool set that should be converted
     * @return an array with all registers in the pool set
     */
    private EirRegister[] convertToArray(PoolSet<EirRegister_Type> registers) {

        int maxOrdial = 0;
        for (EirRegister_Type r : registers) {
            maxOrdial = Math.max(maxOrdial, r.ordinal);
        }
        final EirRegister[] result = new EirRegister[maxOrdial + 1];
        for (EirRegister_Type r : registers) {
            result[r.ordinal] = r;
        }

        return result;
    }

    /**
     * Runs the register allocator, should only be called once.
     */
    @Override
    public void run() {

        final AlgorithmData data = new AlgorithmData(methodGeneration(), convertToArray(this.allocatableIntegerRegisters()), convertToArray(this.allocatableFloatingPointRegisters()));

        for (AlgorithmPart part : phases) {
            part.run(data);
        }
    }
}
