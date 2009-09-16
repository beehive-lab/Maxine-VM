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

import java.util.*;

import com.sun.c1x.ir.*;


/**
 * The <code>Loop</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class Loop {

    BlockBegin loopHeader;
    BlockBegin loopEnd;
    List <BlockBegin> loopBody;       // all the blocks that form the loop, except the loopHeader
    List <BlockBegin> loopExitNodes;  // blocks that are target of an exit edge in loop

    public Loop(BlockBegin loopStart, BlockBegin loopEnd, List <BlockBegin> loopBody) {
        this.loopHeader = loopStart;
        this.loopEnd = loopEnd;
        this.loopBody = loopBody;
        assert !loopBody.contains(loopStart) : "Loop body must not contain the loop header";
        loopExitNodes = new ArrayList<BlockBegin>();
        findLoopExitNodes();
    }

    /**
     * Search all the exit nodes for the loop.
     * Exit node blocks include target blocks from edges coming from loop body,
     * and possibly, from loop header.
     */
    private void findLoopExitNodes() {
        exitNodesForBlock(loopHeader);

        for (BlockBegin block : loopBody) {
            exitNodesForBlock(block);
        }
    }

    public boolean contains(BlockBegin block) {
        if (block == loopHeader) {
            return true;
        }

        return loopBody.contains(block);
    }

    /**
     *
     */
    private void exitNodesForBlock(BlockBegin block) {
        for (int i = 0; i < block.numberOfSux(); i++) {
            BlockBegin successor = block.suxAt(i);
            if (!loopBody.contains(successor) && block != loopHeader && !loopExitNodes.contains(successor)) {
                loopExitNodes.add(successor);
            }
        }
    }

    public Loop(BlockBegin loopStart, BlockBegin loopEnd) {
        this.loopHeader = loopStart;
        this.loopEnd = loopEnd;
        this.loopBody = new ArrayList <BlockBegin>();
    }

    public void addBlock(BlockBegin block) {
        if (block != loopHeader) {
            loopBody.add(block);
        }
    }

    public void setLoopHeader(BlockBegin loopHeader) {
        this.loopHeader = loopHeader;
    }

    public List<BlockBegin> getLoopBlocks() {
        ArrayList<BlockBegin> loopBlocks = new ArrayList <BlockBegin>();
        loopBlocks.add(loopHeader);
        loopBlocks.addAll(loopBody);
        return loopBlocks;
    }

    @Override
    public String toString() {
        String output = "\nLoop Header: " + blockInfo(loopHeader) + "\nLoop End   : " + blockInfo(loopEnd);
        output += "\nBlocks:\n";
        for (BlockBegin block : loopBody) {
            output += blockInfo(block) + "\n";
        }
        return output;
    }

    private String blockInfo(BlockBegin block) {
        return "block #" + block.blockID + ", DFN:" + block.depthFirstNumber() + ", @" + block.bci();
    }
}
