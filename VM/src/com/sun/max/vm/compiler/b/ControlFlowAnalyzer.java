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
package com.sun.max.vm.compiler.b;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.bir.*;

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
        public final int _fromAddress;
        public final int _toAddress;

        Jump(int fromAddress, int toAddress) {
            _fromAddress = fromAddress;
            _toAddress = toAddress;
        }
    }

    private final byte[] _code;
    private final BirBlock[] _blockMap;
    private final boolean[] _starts;
    private final boolean[] _stops;
    private final boolean[] _terminations;
    private final List<Jump> _jumps;

    public ControlFlowAnalyzer(byte[] code) {
        _code = code;
        _blockMap = new BirBlock[code.length];
        _starts = new boolean[code.length];
        _stops = new boolean[code.length];
        _terminations = new boolean[code.length];
        _jumps = new ArrayList<Jump>();
        start(0);
    }

    public BirBlock[] blockMap() {
        return _blockMap;
    }

    public void start(int bytcodePosition) {
        _starts[bytcodePosition] = true;
        if (bytcodePosition > 0) {
            _stops[bytcodePosition - 1] = true;
        }
    }

    /**
     * Records the termination of a basic block.
     * 
     * @param successorAddress
     *                the bytecode position of the byte one past the last byte of the basic block being terminated
     */
    public void terminate(int successorAddress) {
        _terminations[successorAddress - 1] = true;
        _stops[successorAddress - 1] = true;

        // Ensure that unreachable or pseudo-unreachable blocks (e.g. synthesized
        // exception dispatchers) will also have their 'starts' recognized. These
        // blocks do not have explicit control jumps to them.
        if (successorAddress < _starts.length) {
            _starts[successorAddress] = true;
        }
    }

    @Override
    public void terminate() {
        terminate(currentByteAddress());
    }

    @Override
    public void jump(int toAddress) {
        jump(currentByteAddress(), toAddress);
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
        _jumps.add(new Jump(successorAddress - 1, toAddress));
        terminate(successorAddress);
        start(toAddress);
    }

    @Override
    public void fallThrough(int toAddress) {
        jump(toAddress);
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        final int successorAddress = currentByteAddress() + (numberOfCases * 4);
        jump(successorAddress, currentOpcodePosition() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = getBytecodeScanner();
            jump(successorAddress, currentOpcodePosition() + scanner.readSwitchOffset());
        }
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        final int successorAddress = currentByteAddress() + (numberOfCases * 8);
        jump(successorAddress, currentOpcodePosition() + defaultOffset);
        for (int i = 0; i < numberOfCases; i++) {
            final BytecodeScanner scanner = getBytecodeScanner();
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
            if (_starts[i]) {
                assert start == -1;
                start = i;
            }
            assert start != -1;
            if (_stops[i] && start != -1) {
                final BirBlock block = new BirBlock(new BytecodeBlock(code(), start, i));
                blocks.append(block);
                Arrays.fill(_blockMap, start, i + 1, block);
                start = -1;
            }
        }
        return blocks;
    }

    /**
     * @param blockMap a mapping from bytecode position to Block
     */
    private void connectJumpsAndDetermineSafepoints(BirBlock[] blockMap) {
        for (Jump jump : _jumps) {
            final BirBlock fromBlock = blockMap[jump._fromAddress];
            final BirBlock toBlock = blockMap[jump._toAddress];
            fromBlock.addSuccessor(toBlock);
            toBlock.addPredecessor(fromBlock);
            if (jump._toAddress > 0 && !_terminations[jump._toAddress - 1]) {
                final BirBlock fallingThrough = blockMap[jump._toAddress - 1];
                fallingThrough.addSuccessor(toBlock);
                toBlock.addPredecessor(fallingThrough);
            }

            if (jump._toAddress < jump._fromAddress) {
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
        bytecodeScanner.scan(new BytecodeBlock(_code));
        final IndexedSequence<BirBlock> blocks = createBasicBlocks();
        connectJumpsAndDetermineSafepoints(_blockMap);
        return blocks;
    }
}
