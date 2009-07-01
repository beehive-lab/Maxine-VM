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
