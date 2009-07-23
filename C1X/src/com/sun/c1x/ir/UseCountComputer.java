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
package com.sun.c1x.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class UseCountComputer {

    private static final int maxRecurseDepth = 20;

    private List<Instruction> worklist = new ArrayList<Instruction>();
    private int depth;
    private HashMap<Instruction, Integer> map = new HashMap<Instruction, Integer>();

    public UseCountComputer(HashMap<Instruction, Integer> map) {
        this.map = map;
    }

    private InstructionClosure updateUseCount = new InstructionClosure() {

        public Instruction apply(Instruction n) {
            // Local instructions and Phis for expression stack values at the
            // start of basic blocks are not added to the instruction list
            assert n.isAppended() || n instanceof Local || n instanceof Phi : "node was not appended to the graph: " + n;
            // use n's input if not visited before
            if (!n.isPinned() && !map.containsKey(n)) {
                // note:
                // a) if the instruction is pinned, it will be handled by computeUseCount
                // b) if the instruction has uses, it was touched before
                // => in both cases we don't need to update n's values
                usesDo(n);
            }
            // use n
            if (!map.containsKey(n)) {
                map.put(n, 1);
            } else {
                map.put(n, map.get(n) + 1);
            }

            return n;
        }
    };

    private void usesDo(Instruction n) {
        depth++;
        if (depth > maxRecurseDepth) {
            // don't allow the traversal to recurse too deeply
            worklist.add(n);
        } else {
            n.inputValuesDo(updateUseCount);
            // special handling for some instructions
            if (!(n instanceof BlockEnd)) {
                // note on BlockEnd:
                // must 'use' the stack only if the method doesn't
                // terminate, however, in those cases stack is empty
                n.stateValuesDo(updateUseCount);
            }
        }
        depth--;
    }

    public void visitBlock(BlockBegin b) {
        depth = 0;
        // process all pinned nodes as the roots of expression trees
        for (Instruction n = b; n != null; n = n.next()) {
            if (n.isPinned()) {
                usesDo(n);
            }
        }
        assert depth == 0 : "should have counted back down";

        // now process any unpinned nodes which recursed too deeply
        while (worklist.size() > 0) {
            Instruction t = worklist.remove(worklist.size() - 1);
            if (!t.isPinned()) {
                // compute the use count
                usesDo(t);

                // pin the instruction so that LIRGenerator doesn't recurse
                // too deeply during it's evaluation.
                t.pin();
            }
        }
        assert depth == 0 : "should have counted back down";
    }
}
