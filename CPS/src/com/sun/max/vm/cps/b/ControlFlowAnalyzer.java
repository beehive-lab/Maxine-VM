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
package com.sun.max.vm.cps.b;

import java.util.*;

import com.sun.max.collect.*;
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
    private AppendableIndexedSequence<BirBlock> createBasicBlocks() {
        final AppendableIndexedSequence<BirBlock> blocks = new ArrayListSequence<BirBlock>();
        int start = -1;
        for (int i = 0; i < code().length; i++) {
            if (starts[i]) {
                assert start == -1;
                start = i;
            }
            assert start != -1;
            if (stops[i] && start != -1) {
                final BirBlock block = new BirBlock(new BytecodeBlock(code(), start, i));
                blocks.append(block);
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
    public IndexedSequence<BirBlock> run() {
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(this);
        bytecodeScanner.scan(new BytecodeBlock(code));
        final IndexedSequence<BirBlock> blocks = createBasicBlocks();
        connectJumpsAndDetermineSafepoints(blockMap);
        return blocks;
    }
}
