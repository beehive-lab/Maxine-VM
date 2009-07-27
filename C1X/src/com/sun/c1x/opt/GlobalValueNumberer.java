/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.opt;

import com.sun.c1x.C1XMetrics;
import com.sun.c1x.graph.IR;
import com.sun.c1x.ir.BlockBegin;
import com.sun.c1x.ir.Instruction;

import java.util.HashMap;
import java.util.List;

/**
 * Implements global value numbering based on dominators.
 *
 * @author Ben L. Titzer
 */
public class GlobalValueNumberer {

    final IR ir;
    final HashMap<BlockBegin, ValueMap> valueMaps;
    ValueMap currentMap;

    /**
     * Creates a new GlobalValueNumbering pass and performs it on the IR.
     *
     * @param ir the IR on which to perform global value numbering
     */
    public GlobalValueNumberer(IR ir) {
        this.ir = ir;
        List<BlockBegin> blocks = ir.linearScanOrder();
        valueMaps = new HashMap<BlockBegin, ValueMap>(blocks.size());
        optimize(blocks);
    }

    void optimize(List<BlockBegin> blocks) {
        int numBlocks = blocks.size();
        int substCount = 0;
        BlockBegin startBlock = blocks.get(0);
        assert startBlock == ir.startBlock && startBlock.numberOfPreds() == 0 && startBlock.dominator() == null : "start block incorrect";
//        assert startBlock.next() instanceof Base && startBlock.next().next() != null : "start block must not have instructions";

        // initial value map, with nesting 0
        valueMaps.put(startBlock, new ValueMap());

        for (int i = 1; i < numBlocks; i++) {
            // iterate through all the blocks
            BlockBegin block = blocks.get(i);

            int numPreds = block.numberOfPreds();
            assert numPreds > 0 : "block must have predecessors";

            BlockBegin dominator = block.dominator();
            assert dominator != null : "dominator must exist";
            assert valueMaps.get(dominator) != null : "value map of dominator must exist";

            // create new value map with increased nesting
            currentMap = new ValueMap(valueMaps.get(dominator));

            assert numPreds > 1 || dominator == block.predAt(0) : "dominator must be equal to predecessor";

            // visit all instructions of this block
            for (Instruction instr = block.next(); instr != null; instr = instr.next()) {
                assert !instr.hasSubst() : "substitution already set";

                // attempt value numbering
                Instruction f = currentMap.findInsert(instr);
                if (f != instr) {
                    C1XMetrics.GlobalValueNumberHits++;
                    assert !f.hasSubst() : "can't have a substitution";
                    instr.setSubst(f);
                    f.setFlag(Instruction.Flag.PinGlobalValueNumbering);
                    substCount++;
                }
            }

            // remember value map for successors
            valueMaps.put(block, currentMap);
        }

        if (substCount != 0) {
            new SubstitutionResolver(startBlock);
        }
    }
}
