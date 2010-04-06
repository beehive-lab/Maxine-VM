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

import static com.sun.c1x.ci.CiValue.*;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;

/**
 * Converts {@link Phi} instructions into moves.
 *
 * Resolves cycles:
 * <pre>
 *
 *  r1 := r2  becomes  temp := r1
 *  r2 := r1           r1 := r2
 *                     r2 := temp
 * </pre>
 *
 * and orders moves:
 *
 * <pre>
 *  r2 := r3  becomes  r1 := r2
 *  r1 := r2           r2 := r3
 * </pre>
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Doug Simon
 */
public class PhiResolver {

    /**
     * Tracks a data flow dependency between a source operand and any number of the destination operands.
     */
    static class Node {

        /**
         * A source operand whose value flows into the {@linkplain #destinations destination} operands.
         */
        final CiValue operand;

        /**
         * The operands whose values are defined by the {@linkplain #operand source} operand.
         */
        final ArrayList<Node> destinations;

        /**
         * Denotes if a move instruction has already been emitted to initialize the value of {@link #operand}.
         */
        boolean assigned;

        /**
         * Specifies if this operand been visited for the purpose of emitting a move instruction.
         */
        boolean visited;

        /**
         * Specifies if this is the initial definition in data flow path for a given value.
         */
        boolean startNode;

        Node(CiValue operand) {
            this.operand = operand;
            destinations = new ArrayList<Node>();
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(operand.toString());
            if (!destinations.isEmpty()) {
                buf.append(" ->");
                for (Node node : destinations) {
                    buf.append(' ').append(node.operand);
                }
            }
            return buf.toString();
        }
    }

    private final LIRGenerator gen;

    /**
     * The operand loop header phi for the operand currently being process in {@link #dispose()}.
     */
    private Node loop;

    private CiValue temp;

    private final ArrayList<Node> variableOperands = new ArrayList<Node>(3);
    private final ArrayList<Node> otherOperands = new ArrayList<Node>(3);

    /**
     * Maps operands to nodes.
     */
    private final HashMap<CiValue, Node> operandToNodeMap = new HashMap<CiValue, Node>();

    public PhiResolver(LIRGenerator gen) {
        this.gen = gen;
        temp = IllegalLocation;
    }

    public void dispose() {
        // resolve any cycles in moves from and to variables
        for (int i = variableOperands.size() - 1; i >= 0; i--) {
            Node node = variableOperands.get(i);
            if (!node.visited) {
                loop = null;
                move(null, node);
                node.startNode = true;
                assert temp.isIllegal() : "moveTempTo() call missing";
            }
        }

        // generate move for move from non variable to arbitrary destination
        for (int i = otherOperands.size() - 1; i >= 0; i--) {
            Node node = otherOperands.get(i);
            for (int j = node.destinations.size() - 1; j >= 0; j--) {
                emitMove(node.operand, node.destinations.get(j).operand);
            }
        }
    }

    public void move(CiValue src, CiValue dest) {
        assert dest.isVariable() : "destination must be virtual";
        // tty.print("move "); src.print(); tty.print(" to "); dest.print(); tty.cr();
        assert src.isLegal() : "source for phi move is illegal";
        assert dest.isLegal() : "destination for phi move is illegal";
        Node srcNode = sourceNode(src);
        Node destNode = destinationNode(dest);
        srcNode.destinations.add(destNode);
      }

    private Node createNode(CiValue operand, boolean source) {
        Node node;
        if (operand.isVariable()) {
            node = operandToNodeMap.get(operand);
            assert node == null || node.operand.equals(operand);
            if (node == null) {
                node = new Node(operand);
                operandToNodeMap.put(operand, node);
            }
            // Make sure that all variables show up in the list when
            // they are used as the source of a move.
            if (source) {
                if (!variableOperands.contains(node)) {
                    variableOperands.add(node);
                }
            }
        } else {
            assert source;
            node = new Node(operand);
            otherOperands.add(node);
        }
        return node;
    }

    private Node destinationNode(CiValue opr) {
        return createNode(opr, false);
    }

    private void emitMove(CiValue src, CiValue dest) {
        assert src.isLegal();
        assert dest.isLegal();
        gen.lir.move(src, dest);
    }

    // Traverse assignment graph in depth first order and generate moves in post order
    // ie. two assignments: b := c, a := b start with node c:
    // Call graph: move(NULL, c) -> move(c, b) -> move(b, a)
    // Generates moves in this order: move b to a and move c to b
    // ie. cycle a := b, b := a start with node a
    // Call graph: move(NULL, a) -> move(a, b) -> move(b, a)
    // Generates moves in this order: move b to temp, move a to b, move temp to a
    private void move(Node src, Node dest) {
        if (!dest.visited) {
            dest.visited = true;
            for (int i = dest.destinations.size() - 1; i >= 0; i--) {
                move(dest, dest.destinations.get(i));
            }
        } else if (!dest.startNode) {
            // cycle in graph detected
            assert loop == null : "only one loop valid!";
            loop = dest;
            moveToTemp(src.operand);
            return;
        } // else dest is a start node

        if (!dest.assigned) {
            if (loop == dest) {
                moveTempTo(dest.operand);
                dest.assigned = true;
            } else if (src != null) {
                emitMove(src.operand, dest.operand);
                dest.assigned = true;
            }
        }
    }

    private void moveTempTo(CiValue dest) {
        assert temp.isLegal();
        emitMove(temp, dest);
        temp = IllegalLocation;
    }

    private void moveToTemp(CiValue src) {
        assert temp.isIllegal();
        temp = gen.newVariable(src.kind);
        emitMove(src, temp);
    }

    private Node sourceNode(CiValue opr) {
        return createNode(opr, true);
    }
}
