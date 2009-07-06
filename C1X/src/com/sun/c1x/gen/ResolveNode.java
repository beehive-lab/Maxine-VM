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
package com.sun.c1x.gen;

import java.util.*;

import com.sun.c1x.lir.*;


public class ResolveNode {

    private LIROperand operand;
    private List<ResolveNode> destinations;
    private boolean assigned;
    private boolean visited;
    private boolean startNode;

    public ResolveNode(LIROperand operand) {
        this.operand = operand;
        destinations = new ArrayList<ResolveNode>();
    }

    LIROperand operand() {
        return operand;
    }

    int noOfDestinations() {
        return destinations.size();
    }

    ResolveNode destinationAt(int index) { return destinations.get(index); }
    boolean assigned() { return assigned; }
    boolean visited() { return visited; }
    boolean startNode() { return startNode; }


    void append(ResolveNode dest) {
        destinations.add(dest);
    }

    void setAssigned() { assigned = true; }
    void setVisited() { visited = true; }
    void setStartNode() { startNode = true; }
}
