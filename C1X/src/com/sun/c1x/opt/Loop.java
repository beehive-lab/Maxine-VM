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
 * This class represents a loop, including a header block, an end block, blocks making up the body,
 * and any blocks that are exitted to from the loop body.
 *
 * @author Marcelo Cintra
 */
public class Loop {

    class Edge {
        final BlockBegin source;
        final BlockBegin destination;

        public Edge(BlockBegin source, BlockBegin destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge)) {
                return false;
            }
            Edge other = (Edge) obj;
            return other.destination == destination && other.source == source;
        }
    }

    BlockBegin header;
    final BlockBegin end;
    final List<BlockBegin> body;   // all the blocks that form the loop, except the loopHeader
    final List<Edge> exitEdges;    // edges that exit the loop

    public Loop(BlockBegin loopStart, BlockBegin loopEnd, List <BlockBegin> loopBody) {
        this.header = loopStart;
        this.end = loopEnd;
        this.body = loopBody;
        body.remove(header);
        exitEdges = new ArrayList<Edge>();
        findLoopExitNodes();
    }

    /**
     * Search all the exit nodes for the loop.
     * Exit node blocks include target blocks from edges coming from loop body,
     * and possibly, from loop header.
     */
    private void findLoopExitNodes() {
        addExitNodesForBlock(header);

        for (BlockBegin block : body) {
            addExitNodesForBlock(block);
        }
    }

    public boolean contains(BlockBegin block) {
        // TODO: use a bitmap for performance reasons
        return block == header || body.contains(block);
    }

    public BlockBegin header() {
        return header;
    }

    private void addExitNodesForBlock(BlockBegin block) {
        for (int i = 0; i < block.numberOfSux(); i++) {
            BlockBegin successor = block.suxAt(i);

            if (!body.contains(successor) && successor != header) {
                Edge newEdge = new Edge(block, successor);
                if (!exitEdges.contains(newEdge)) {
                    exitEdges.add(newEdge);
                }
            }
        }
    }

    public int numberOfBlocks() {
        return body.size() + 1;
    }

    public void addBlock(BlockBegin block) {
        if (block != header && !body.contains(block)) {
            body.add(block);
            findLoopExitNodes();
        }
    }

    public void setLoopHeader(BlockBegin loopHeader) {
        this.header = loopHeader;
        findLoopExitNodes();
    }

    public List<BlockBegin> getLoopBlocks() {
        ArrayList<BlockBegin> loopBlocks = new ArrayList <BlockBegin>();
        loopBlocks.add(header);
        loopBlocks.addAll(body);
        return loopBlocks;
    }

    @Override
    public String toString() {
        String output = "\nLoop Header: " + blockInfo(header) + "\nLoop End: " + blockInfo(end);
        output += "\nLoopBody:\n";
        for (BlockBegin block : body) {
            output += blockInfo(block) + "\n";
        }
        output += "\nExitEdges:\n";
        for (Edge edge : exitEdges) {
            output += "B" + edge.source.blockID + " -> B" + edge.destination.blockID + "\n";
        }

        return output;
    }

    private String blockInfo(BlockBegin block) {
        return "block #" + block.blockID + ", DFN:" + block.depthFirstNumber() + ", @" + block.bci();
    }
}
