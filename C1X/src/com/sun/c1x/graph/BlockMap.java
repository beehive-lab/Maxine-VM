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

import com.sun.c1x.ir.BlockBegin;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.util.*;
import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiExceptionHandler;
import com.sun.c1x.C1XIntrinsic;
import com.sun.c1x.C1XOptions;

import java.util.*;

/**
 * The <code>BlockMap</code> class builds a mapping between bytecodes and basic blocks
 * and builds a conservative control flow graph. Note that this class serves a similar role
 * to <code>BlockListBuilder</code>, but makes fewer assumptions about what the compiler
 * interface provides. It builds all basic blocks for the control flow graph without requiring
 * the compiler interface to provide a bitmap of the beginning of basic blocks. It makes
 * two linear passes; one over the bytecodes to build block starts and successor lists,
 * and one pass over the block map to build the CFG. Note that the CFG built by this class
 * is <i>not</i> connected to the actual <code>BlockBegin</code> instances; this class does,
 * however, compute and assign the reverse postorder number of the blocks.
 *
 * @author Ben L. Titzer
 */
public class BlockMap {

    private static final BlockBegin[] NONE = new BlockBegin[0];
    private static final List<BlockBegin> NONE_LIST = Util.uncheckedCast(Collections.EMPTY_LIST);

    /**
     * The <code>ExceptionMap</code> class is used internally to track exception handlers
     * while iterating over the bytecode and the control flow graph. Since methods with
     * exception handlers are much less frequent than those without, the common case
     * does not need to construct an exception map.
     */
    private class ExceptionMap {
        private final BitMap _canTrap;
        private final boolean _isObjectInit;
        private final List<CiExceptionHandler> _allHandlers;
        private final ArrayMap<HashSet<BlockBegin>> _handlerMap;

        ExceptionMap(CiMethod method, byte[] code) {
            _canTrap = new BitMap(code.length);
            _isObjectInit = C1XIntrinsic.getIntrinsic(method) == C1XIntrinsic.java_lang_Object$init && C1XOptions.RegisterFinalizersAtInit;
            _allHandlers = method.exceptionHandlers();
            _handlerMap = new ArrayMap<HashSet<BlockBegin>>(_firstBlock, code.length / 5);
        }

        void setCanTrap(int bci) {
            _canTrap.set(bci);
        }

        void addHandlers(BlockBegin block, int bci) {
            if (_canTrap.get(bci)) {
                // XXX: replace with faster algorithm (sort exception handlers by start and end)
                for (CiExceptionHandler h : _allHandlers) {
                    if (h.startBCI() <= bci && bci <= h.endBCI()) {
                        if (h.isCatchAll()) {
                            break;
                        }
                        addHandler(block, get(h.handlerBCI()));
                    }
                }
            }
        }

        Iterable<BlockBegin> getHandlers(BlockBegin block) {
            // lookup handlers for the basic block
            HashSet<BlockBegin> set = _handlerMap.get(block.blockID());
            return set == null ? NONE_LIST : set;
        }

        void setHandlerEntrypoints() {
            // start basic blocks at all exception handler blocks and mark them as exception entries
            for (CiExceptionHandler h : _allHandlers) {
                addEntrypoint(h.handlerBCI(), BlockBegin.BlockFlag.ExceptionEntry);
            }
        }

        void addHandler(BlockBegin block, BlockBegin handler) {
            // add a handler to a basic block, creating the set if necessary
            HashSet<BlockBegin> set = _handlerMap.get(block.blockID());
            if (set == null) {
                set = new HashSet<BlockBegin>();
                _handlerMap.put(block.blockID(), set);
            }
            set.add(handler);
        }
    }

    private final byte[] _code;
    private final BlockBegin[] _blockMap;
    private final BitMap _storesInLoops;
    private BlockBegin[][] _successorMap;
    private ArrayList<BlockBegin> _loopBlocks;
    private ExceptionMap _exceptionMap;
    private final int _firstBlock;
    private int _blockNum; // used for initial block ID (count up) and post-order number (count down)

    /**
     * Creates a new BlockMap instance from the specified bytecode.
     * @param method the compiler interface method containing the code
     * @param firstBlock the first block number to use
     */
    public BlockMap(CiMethod method, int firstBlock) {
        byte[] code = method.code();
        _code = code;
        _firstBlock = firstBlock;
        _blockNum = firstBlock;
        _blockMap = new BlockBegin[code.length];
        _successorMap = new BlockBegin[code.length][];
        _loopBlocks = new ArrayList<BlockBegin>();
        _storesInLoops = new BitMap(method.maxLocals());
        if (method.hasExceptionHandlers()) {
            _exceptionMap = new ExceptionMap(method, code);
        }
    }

    /**
     * Add an entrypoint to this BlockMap. The resulting block will be marked
     * with the specified block flags.
     * @param bci the bytecode index of the start of the block
     * @param entryFlag the entry flag to mark the block with
     */
    public void addEntrypoint(int bci, BlockBegin.BlockFlag entryFlag) {
        make(bci).setBlockFlag(entryFlag);
    }

    /**
     * Gets the block that begins at the specified bytecode index.
     * @param bci the bytecode index of the start of the block
     * @return the block starting at the specified index, if it exists; <code>null</code> otherwise
     */
    public BlockBegin get(int bci) {
        return _blockMap[bci];
    }

    BlockBegin make(int bci) {
        BlockBegin block = _blockMap[bci];
        if (block == null) {
            block = new BlockBegin(bci);
            block.setBlockID(_blockNum++);
            _blockMap[bci] = block;
        }
        return block;
    }

    /**
     * Gets a conservative approximation of the successors of a given block.
     * @param block the block for which to get the successors
     * @return an array of the successors of the specified block
     */
    public BlockBegin[] getSuccessors(BlockBegin block) {
        return _successorMap[block.bci()];
    }

    /**
     * Gets the exception handlers for a specified block. Note that this
     * set of exception handlers takes into account whether the block contains
     * bytecodes that can cause traps or not.
     * @param block the block for which to get the exception handlers
     * @return an array of the blocks which represent exception handlers; a zero-length
     * array of blocks if there are no handlers that cover any potentially trapping
     * instruction in the specified block
     */
    public Iterable<BlockBegin> getHandlers(BlockBegin block) {
        if (_exceptionMap == null) {
            return NONE_LIST;
        }
        return _exceptionMap.getHandlers(block);
    }

    /**
     * Builds the block map and conservative CFG and numbers blocks.
     * @param computeStoresInLoops <code>true</code> if the block map builder should
     * make a second pass over the bytecodes for blocks in loops
     * @return <code>true</code> if the block map was built successfully; <code>false</code> otherwise
     */
    public boolean build(boolean computeStoresInLoops) {
        if (_exceptionMap != null) {
            _exceptionMap.setHandlerEntrypoints();
        }
        iterateOverBytecodes();
        moveSuccessorLists();
        computeBlockNumbers();
        if (computeStoresInLoops) {
            // process any blocks in loops to compute their stores
            // (requires another pass, but produces fewer phi's and ultimately better code)
            processLoopBlocks();
        } else {
            // be conservative and assume all locals are potentially stored in loops
            // (does not require another pass, but produces more phi's and worse code)
            _storesInLoops.setAll();
        }
        return true; // XXX: what bailout conditions should the BlockMap check?
    }

    /**
     * Cleans up any internal state not necessary after the initial pass. Note that
     * this method discards the conservative CFG edges and only retains the block mapping
     * and stores in loops.
     */
    public void cleanup() {
        // discard internal state no longer needed
        _successorMap = null;
        _loopBlocks = null;
        _exceptionMap = null;
    }

    /**
     * Gets the number of blocks in this block map.
     * @return the number of blocks
     */
    public int numberOfBlocks() {
        return _blockNum - _firstBlock;
    }

    /**
     * Gets the bitmap that indicates which local variables are assigned in loops.
     * @return a bitmap which indicates the locals stored in loops
     */
    public BitMap getStoresInLoops() {
        return _storesInLoops;
    }

    void iterateOverBytecodes() {
        // iterate over the bytecodes top to bottom.
        // mark the entrypoints of basic blocks and build lists of successors for
        // all bytecodes that end basic blocks (i.e. goto, ifs, switches, throw, jsr, returns, ret)
        int bci = 0;
        ExceptionMap exceptionMap = _exceptionMap;
        byte[] code = _code;
        make(0);
        while (bci < code.length) {
            int opcode = Bytes.beU1(code, bci);
            switch (opcode) {
                case Bytecodes.ATHROW:  // fall through
                case Bytecodes.IRETURN: // fall through
                case Bytecodes.LRETURN: // fall through
                case Bytecodes.FRETURN: // fall through
                case Bytecodes.DRETURN: // fall through
                case Bytecodes.ARETURN: // fall through
                case Bytecodes.RETURN:
                    if (exceptionMap != null && exceptionMap._isObjectInit) exceptionMap.setCanTrap(bci);
                    _successorMap[bci] = NONE; // end of control flow
                    bci += 1; // these are all 1 byte opcodes
                    break;

                case Bytecodes.RET:
                    _successorMap[bci] = NONE; // end of control flow
                    bci += 2; // ret is 2 bytes
                    break;

                case Bytecodes.IFEQ:      // fall through
                case Bytecodes.IFNE:      // fall through
                case Bytecodes.IFLT:      // fall through
                case Bytecodes.IFGE:      // fall through
                case Bytecodes.IFGT:      // fall through
                case Bytecodes.IFLE:      // fall through
                case Bytecodes.IF_ICMPEQ: // fall through
                case Bytecodes.IF_ICMPNE: // fall through
                case Bytecodes.IF_ICMPLT: // fall through
                case Bytecodes.IF_ICMPGE: // fall through
                case Bytecodes.IF_ICMPGT: // fall through
                case Bytecodes.IF_ICMPLE: // fall through
                case Bytecodes.IF_ACMPEQ: // fall through
                case Bytecodes.IF_ACMPNE: // fall through
                case Bytecodes.IFNULL:    // fall through
                case Bytecodes.IFNONNULL: {
                    succ2(bci, bci + 3, Bytes.beS2(code, bci + 1));
                    bci += 3; // these are all 3 byte opcodes
                    break;
                }

                case Bytecodes.GOTO: {
                    succ1(bci, Bytes.beS2(code, bci + 1));
                    bci += 3; // goto is 3 bytes
                    break;
                }

                case Bytecodes.GOTO_W: {
                    succ1(bci, Bytes.beS4(code, bci + 1));
                    bci += 5; // goto_w is 5 bytes
                    break;
                }

                case Bytecodes.JSR: {
                    int target = Bytes.beS2(code, bci + 1);
                    succ2(bci, bci + 3, target); // make JSR's a successor or not?
                    addEntrypoint(target, BlockBegin.BlockFlag.SubroutineEntry);
                    bci += 3; // jsr is 3 bytes
                    break;
                }

                case Bytecodes.JSR_W: {
                    int target = Bytes.beS4(code, bci + 1);
                    succ2(bci, bci + 5, target);
                    addEntrypoint(target, BlockBegin.BlockFlag.SubroutineEntry);
                    bci += 5; // jsr_w is 5 bytes
                    break;
                }

                case Bytecodes.TABLESWITCH: {
                    BytecodeSwitch sw = new BytecodeTableSwitch(code, bci);
                    makeSwitchSuccessors(bci, sw);
                    bci += sw.size();
                    break;
                }

                case Bytecodes.LOOKUPSWITCH: {
                    BytecodeSwitch sw = new BytecodeLookupSwitch(code, bci);
                    makeSwitchSuccessors(bci, sw);
                    bci += sw.size();
                    break;
                }
                case Bytecodes.WIDE: {
                    bci += Bytecodes.length(code, bci);
                    break;
                }

                default: {
                    if (exceptionMap != null && Bytecodes.canTrap(opcode)) exceptionMap.setCanTrap(bci);
                    bci += Bytecodes.length(opcode); // all variable length instructions are handled above
                }
            }
        }
    }

    private void makeSwitchSuccessors(int bci, BytecodeSwitch tswitch) {
        // make a list of all the successors of a switch
        int max = tswitch.numberOfCases();
        ArrayList<BlockBegin> list = new ArrayList<BlockBegin>(max + 1);
        for (int i = 0; i < max; i++) {
            list.add(make(bci + tswitch.offsetAt(i)));
        }
        list.add(make(bci + tswitch.defaultOffset()));
        _successorMap[bci] = list.toArray(new BlockBegin[list.size()]);
    }

    void moveSuccessorLists() {
        // move successor lists from the block-ending bytecodes that created them
        // to the basic blocks which they end.
        // also handle fall-through cases from backwards branches into the middle of a block
        // add exception handlers to basic blocks
        BlockBegin current = get(0);
        ExceptionMap exceptionMap = _exceptionMap;
        for (int bci = 0; bci < _blockMap.length; bci++) {
            BlockBegin next = _blockMap[bci];
            if (next != null && next != current) {
                if (current != null) {
                    // add fall through successor to current block
                    _successorMap[current.bci()] = new BlockBegin[] { next };
                }
                current = next;
            }
            if (exceptionMap != null) {
                exceptionMap.addHandlers(current, bci);
            }
            BlockBegin[] succ = _successorMap[bci];
            if (succ != null && current != null) {
                // move this successor list to current block
                _successorMap[bci] = null;
                _successorMap[current.bci()] = succ;
                current = null;
            }
        }
        assert current == null : "fell off end of code, should end with successor list";
    }

    void computeBlockNumbers() {
        // compute the block number for all blocks
        int blockNum = _blockNum;
        int numBlocks = blockNum - _firstBlock;
        numberBlock(get(0), new BitMap(numBlocks), new BitMap(numBlocks));
        _blockNum = blockNum; // _blockNum is used to compute the number of blocks later
    }

    boolean numberBlock(BlockBegin block, BitMap visited, BitMap active) {
        // number a block with its reverse post-order traversal number
        int blockIndex = block.blockID() - _firstBlock;

        if (visited.get(blockIndex)) {
            if (active.get(blockIndex)) {
                // reached block via backward branch
                block.setLoopHeader(true);
                _loopBlocks.add(block);
                return true;
            }
            // return whether the block is already a loop header
            return block.isLoopHeader();
        }

        visited.set(blockIndex);
        active.set(blockIndex);

        boolean inLoop = false;
        for (BlockBegin succ : getSuccessors(block)) {
            // recursively process successors
            inLoop |= numberBlock(succ, visited, active);
        }
        if (_exceptionMap != null) {
            for (BlockBegin succ : _exceptionMap.getHandlers(block)) {
                // process exception handler blocks
                inLoop |= numberBlock(succ, visited, active);
            }
        }
        // clear active bit after successors are processed
        active.clear(blockIndex);
        block.setDepthFirstNumber(_blockNum--);
        if (inLoop) _loopBlocks.add(block);

        return inLoop;
    }

    void processLoopBlocks() {
        for (BlockBegin block : _loopBlocks) {
            // process all the stores in this block
            int bci = block.bci();
            byte[] code = _code;
            while (true) {
                // iterate over the bytecodes in this block
                int opcode = code[bci] & 0xff;
                if (opcode == Bytecodes.WIDE) {
                    bci += processWideStore(code[bci + 1] & 0xff, code, bci);
                } else if (Bytecodes.isStore(opcode)) {
                    bci += processStore(opcode, code, bci);
                } else {
                    bci += Bytecodes.length(code, bci);
                }
                if (_blockMap[bci] != null) {
                    // stop when we reach the next block
                    break;
                }
            }
        }
    }

    void processStoresInBlock(BlockBegin block) {
        int bci = block.bci();
        byte[] code = _code;
        do {
            // iterate over the bytecodes in this block
            int opcode = code[bci] & 0xff;
            if (opcode == Bytecodes.WIDE) {
                bci += processWideStore(code[bci + 1], code, bci);
            } else if (Bytecodes.isStore(opcode)) {
                bci += processStore(opcode, code, bci);
            } else {
                bci += Bytecodes.length(code, bci);
            }
        } while (_blockMap[bci] != null); // stop when we reach the next block

    }

    int processWideStore(int opcode, byte[] code, int bci) {
        switch (opcode) {
            case Bytecodes.IINC:     storeOne(Bytes.beU2(code, bci + 2)); return 5;
            case Bytecodes.ISTORE:   storeOne(Bytes.beU2(code, bci + 2)); return 3;
            case Bytecodes.LSTORE:   storeTwo(Bytes.beU2(code, bci + 2)); return 3;
            case Bytecodes.FSTORE:   storeOne(Bytes.beU2(code, bci + 2)); return 3;
            case Bytecodes.DSTORE:   storeTwo(Bytes.beU2(code, bci + 2)); return 3;
            case Bytecodes.ASTORE:   storeOne(Bytes.beU2(code, bci + 2)); return 3;
        }
        return Bytecodes.length(code, bci);
    }

    int processStore(int opcode, byte[] code, int bci) {
        switch (opcode) {
            case Bytecodes.IINC:     storeOne(code[bci + 1] & 0xff); return 3;
            case Bytecodes.ISTORE:   storeOne(code[bci + 1] & 0xff); return 2;
            case Bytecodes.LSTORE:   storeTwo(code[bci + 1] & 0xff); return 2;
            case Bytecodes.FSTORE:   storeOne(code[bci + 1] & 0xff); return 2;
            case Bytecodes.DSTORE:   storeTwo(code[bci + 1] & 0xff); return 2;
            case Bytecodes.ASTORE:   storeOne(code[bci + 1] & 0xff); return 2;
            case Bytecodes.ISTORE_0: storeOne(0); return 1;
            case Bytecodes.ISTORE_1: storeOne(1); return 1;
            case Bytecodes.ISTORE_2: storeOne(2); return 1;
            case Bytecodes.ISTORE_3: storeOne(3); return 1;
            case Bytecodes.LSTORE_0: storeTwo(0); return 1;
            case Bytecodes.LSTORE_1: storeTwo(1); return 1;
            case Bytecodes.LSTORE_2: storeTwo(2); return 1;
            case Bytecodes.LSTORE_3: storeTwo(3); return 1;
            case Bytecodes.FSTORE_0: storeOne(0); return 1;
            case Bytecodes.FSTORE_1: storeOne(1); return 1;
            case Bytecodes.FSTORE_2: storeOne(2); return 1;
            case Bytecodes.FSTORE_3: storeOne(3); return 1;
            case Bytecodes.DSTORE_0: storeTwo(0); return 1;
            case Bytecodes.DSTORE_1: storeTwo(1); return 1;
            case Bytecodes.DSTORE_2: storeTwo(2); return 1;
            case Bytecodes.DSTORE_3: storeTwo(3); return 1;
            case Bytecodes.ASTORE_0: storeOne(0); return 1;
            case Bytecodes.ASTORE_1: storeOne(1); return 1;
            case Bytecodes.ASTORE_2: storeOne(2); return 1;
            case Bytecodes.ASTORE_3: storeOne(3); return 1;
        }
        throw Util.shouldNotReachHere();
    }

    void storeOne(int local) {
        _storesInLoops.set(local);
    }

    void storeTwo(int local) {
        _storesInLoops.set(local);
        _storesInLoops.set(local + 1);
    }

    void succ2(int bci, int s1, int s2) {
        _successorMap[bci] = new BlockBegin[] { make(s1), make(s2) };
    }

    void succ1(int bci, int s1) {
        _successorMap[bci] = new BlockBegin[] { make(s1) };
    }
}
