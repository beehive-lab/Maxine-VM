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
package com.sun.c1x.graph;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class CriticalEdgeFinder implements BlockClosure {

    private final IR ir;
    private Map<BlockBegin, Set<BlockBegin>> edges = new HashMap<BlockBegin, Set<BlockBegin>>();


    public CriticalEdgeFinder(IR ir) {
        this.ir = ir;
    }


    @Override
    public void apply(BlockBegin block) {
        if (block.numberOfSux() >= 2) {
            for (BlockBegin succ : block.end().successors()) {
                if (succ.numberOfPreds() >= 2) {
                    // TODO: (tw) probably we don't have to make it a critical edge if succ only contains the _same_ predecessor multiple times.
                    recordCriticalEdge(block, succ);
                }
            }
        }
    }

    private void recordCriticalEdge(BlockBegin block, BlockBegin succ) {
        if (!edges.containsKey(block)) {
            edges.put(block, new HashSet<BlockBegin>());
        }

        edges.get(block).add(succ);
    }

    public void splitCriticalEdges() {
        for (BlockBegin from : edges.keySet()) {
            for (BlockBegin to : edges.get(from)) {
                BlockBegin split =  ir.splitEdge(from, to);

                if (C1XOptions.PrintIR && C1XOptions.Verbose) {
                    TTY.println("Split edge between block %d and block %d, creating new block %d", from.blockID, to.blockID, split.blockID);
                }
            }
        }
    }

}
