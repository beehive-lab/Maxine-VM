/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.b;

import java.util.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.cps.bir.*;

/**
 * Creates a control flow graph for a given class method.
 *
 * @author Bernd Mathiske
 */
public class ControlFlowAnalyzer extends ControlFlowAdapter {

    /**
     * A recognized control flow jump in the bytecode instruction stream.
     */
    private static final class Jump {
        public final int fromAddress;
        public final int toAddress;

        Jump(int fromAddress, int toAddress) {
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
        }
    }

    private final ClassMethodActor classMethodActor;
    private final byte[] code;
    private final BirBlock[] blockMap;
    private final boolean[] starts;
    private final boolean[] stops;
    private final boolean[] terminations;
    private final List<Jump> jumps;

    public ControlFlowAnalyzer(ClassMethodActor classMethodActor, byte[] code) {
        this.classMethodActor = classMethodActor;
        this.code = code;
        this.blockMap = new BirBlock[code.length];
        this.starts = new boolean[code.length];
        this.stops = new boolean[code.length];
        this.terminations = new boolean[code.length];
        this.jumps = new ArrayList<Jump>();
        start(0);
    }

    public BirBlock[] blockMap() {
        return blockMap;
    }

    public void start(int bytcodePosition) {
        starts[bytcodePosition] = true;
        if (bytcodePosition > 0) {
            stops[bytcodePosition - 1] = true;
        }
    }

    /**
     * Records the termination of a basic block.
     *
     * @param successorAddress
     *                the bytecode position of the byte one past the last byte of the basic block being terminated
     */
    public void terminate(int successorAddress) {
        terminations[successorAddress - 1] = true;
        stops[successorAddress - 1] = true;

        // Ensure that unreachable or pseudo-unreachable blocks (e.g. synthesized
        // exception dispatchers) will also have their 'starts' recognized. These
        // blocks do not have explicit control jumps to them.
        if (successorAddress < starts.length) {
            starts[successorAddress] = true;
        }
    }

    @Override
    public void terminate() {
        terminate(currentBytePosition());
    }

    @Override
    public void jump(int toAddress) {
        jump(currentBytePosition(), toAddress);
    }

    /**
     * Records a jump.
     *
     * @param successorAddress
     *                the bytecode position of the byte one past the last byte of the instruction that is the source of the jump
     * @param toAddress
     *                the target of the jump
     */
    private void jump(int successorAddress, int toAddress) {
        jumps.add(new Jump(successorAddress - 1, toAddress));
        terminate(successorAddress);
        start(toAddress);
    }

    @Override
    public void fallThrough(int toAddress) {
        jump(toAddress);
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        final int successorAddress = currentBytePosition() + (numberOfCases * 4);
        jump(successorAddress, currentOpcodePosition() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = bytecodeScanner();
            jump(successorAddress, currentOpcodePosition() + scanner.readSwitchOffset());
        }
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        final int successorAddress = currentBytePosition() + (numberOfCases * 8);
        jump(successorAddress, currentOpcodePosition() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = bytecodeScanner();
            scanner.readSwitchCase(); // ignore case value
            jump(successorAddress, currentOpcodePosition() + scanner.readSwitchOffset());
        }
    }

    /**
     * Create basic blocks as indicated by _starts and _stops,
     * note in the basic block map for each bytecode position, which basic block it belongs to.
     *
     * @return the basic blocks in the byte code stream, aggregated in bytecode position order in an array
     */
    private List<BirBlock> createBasicBlocks() {
        final List<BirBlock> blocks = new ArrayList<BirBlock>();
        int start = -1;
        for (int i = 0; i < code().length; i++) {
            if (starts[i]) {
                assert start == -1;
                start = i;
            }
            assert start != -1;
            if (stops[i] && start != -1) {
                final BirBlock block = new BirBlock(new BytecodeBlock(code(), start, i));
                blocks.add(block);
                Arrays.fill(blockMap, start, i + 1, block);
                start = -1;
            }
        }
        return blocks;
    }

    /**
     * @param blkMap a mapping from bytecode position to Block
     */
    private void connectJumpsAndDetermineSafepoints(BirBlock[] blkMap) {
        for (Jump jump : jumps) {
            final BirBlock fromBlock = blkMap[jump.fromAddress];
            final BirBlock toBlock = blkMap[jump.toAddress];
            fromBlock.addSuccessor(toBlock);
            toBlock.addPredecessor(fromBlock);
            if (jump.toAddress > 0 && !terminations[jump.toAddress - 1]) {
                final BirBlock fallingThrough = blkMap[jump.toAddress - 1];
                fallingThrough.addSuccessor(toBlock);
                toBlock.addPredecessor(fallingThrough);
            }

            if (jump.toAddress < jump.fromAddress && !classMethodActor.noSafepoints()) {
                // Backwards branch detected, provision a safepoint at its target:
                toBlock.haveSafepoint();
            }
        }
    }

    /**
     * @return an array of spatially ordered basic blocks, which are also interconnected by their successor/predecessor relationships
     */
    public List<BirBlock> run() {
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(this);
        bytecodeScanner.scan(new BytecodeBlock(code));
        final List<BirBlock> blocks = createBasicBlocks();
        connectJumpsAndDetermineSafepoints(blockMap);
        return blocks;
    }
}
