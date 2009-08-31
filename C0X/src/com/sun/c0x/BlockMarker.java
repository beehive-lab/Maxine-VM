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
package com.sun.c0x;

import java.util.List;

import com.sun.c1x.bytecode.BytecodeLookupSwitch;
import com.sun.c1x.bytecode.BytecodeSwitch;
import com.sun.c1x.bytecode.BytecodeTableSwitch;
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.ri.RiExceptionHandler;
import com.sun.c1x.ri.RiMethod;
import com.sun.c1x.util.Bytes;

/**
 * The <code>BlockMarker</code> class computes the starts of all basic blocks by iterating over the bytecodes,
 * if this information is not available from the verifier.
 *
 * @author Ben L. Titzer
 */
public class BlockMarker {

    static final int BLOCK_START = 1;
    static final int BLOCK_BACKWARD_TARGET = 2;
    static final int BLOCK_EXCEPTION_ENTRY = 4;
    static final int BLOCK_SUBROUTINE_ENTRY = 8;
    static final int BLOCK_SUBROUTINE_RETURN = 16;
    static final int PREDECESSOR_SHIFT = 5;
    static final int PREDECESSOR_MASK = 0x1f;
    static final int ONE_PREDECESSOR = 1 << PREDECESSOR_SHIFT;

    final byte[] code;
    final byte[] blockMap;
    List<RiExceptionHandler> handlers;
    int numBlocks;
    int numBackwardEdges;

    /**
     * Creates a new BlockMap instance from the specified bytecode.
     * @param method the compiler interface method containing the code
     */
    public BlockMarker(RiMethod method) {
        byte[] code = method.code();
        this.handlers = method.exceptionHandlers();
        this.code = code;
        this.blockMap = new byte[code.length];
    }

    public void markBlocks() {
        if (handlers != null) {
            // mark all exception handler blocks
            for (RiExceptionHandler h : handlers) {
                setExceptionEntry(h.handlerBCI());
            }
        }
        iterateOverBytecodes();
    }

    void iterateOverBytecodes() {
        // iterate over the bytecodes top to bottom, discovering the starts of basic blocks
        int bci = 0;
        byte[] code = this.code;
        setEntry(0, BLOCK_START);
        while (bci < code.length) {
            int opcode = Bytes.beU1(code, bci);
            switch (opcode) {
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
                    bci = branch(bci, bci + Bytes.beS2(code, bci + 1), bci + 3);
                    break;
                }

                case Bytecodes.GOTO: {
                    bci = jump(bci, bci + Bytes.beS2(code, bci + 1), bci + 3);
                    break;
                }

                case Bytecodes.GOTO_W: {
                    bci = jump(bci, bci + Bytes.beS4(code, bci + 1), bci + 5);
                    break;
                }

                case Bytecodes.JSR: {
                    bci = setSubroutineEntry(bci + Bytes.beS2(code, bci + 1), bci + 3);
                    break;
                }

                case Bytecodes.JSR_W: {
                    bci = setSubroutineEntry(bci + Bytes.beS4(code, bci + 1), bci + 5);
                    break;
                }

                case Bytecodes.TABLESWITCH: {
                    BytecodeSwitch sw = new BytecodeTableSwitch(code, bci);
                    makeSwitchSuccessors(bci, sw);
                    setEntry(bci += sw.size(), BLOCK_START);
                    break;
                }

                case Bytecodes.LOOKUPSWITCH: {
                    BytecodeSwitch sw = new BytecodeLookupSwitch(code, bci);
                    makeSwitchSuccessors(bci, sw);
                    setEntry(bci += sw.size(), BLOCK_START);
                    break;
                }
                case Bytecodes.WIDE: {
                    bci += Bytecodes.length(code, bci);
                    break;
                }

                default: {
                    bci += Bytecodes.length(opcode); // all variable length instructions are handled above
                }
            }
        }
    }

    private void setEntry(int bci, int flags) {
        byte prev = blockMap[bci];
        if (prev == 0) {
            // this is the first time the block has been marked
            numBlocks++;
            blockMap[bci] = (byte) (flags | ONE_PREDECESSOR);
        } else {
            // block previously marked; add flag and increment predecessors
            int nflags = prev | flags;
            int pred = nflags >> PREDECESSOR_SHIFT;
            if (++pred == 0) {
                pred = 7;
            }
            blockMap[bci] = (byte) ((pred << PREDECESSOR_SHIFT) | (nflags & PREDECESSOR_MASK));
        }
    }

    private void setExceptionEntry(int bci) {
        setEntry(bci, BLOCK_EXCEPTION_ENTRY | BLOCK_START);
    }

    private int setSubroutineEntry(int bci, int nextBCI) {
        setEntry(bci, BLOCK_SUBROUTINE_ENTRY | BLOCK_START);
        setEntry(nextBCI, BLOCK_SUBROUTINE_RETURN | BLOCK_START);
        return nextBCI;
    }

    private void set(int fromBCI, int toBCI) {
        if (fromBCI >= toBCI) {
            numBackwardEdges++;
            setEntry(toBCI, BLOCK_BACKWARD_TARGET | BLOCK_START);
        } else {
            setEntry(toBCI, BLOCK_START);
        }
    }

    private void makeSwitchSuccessors(int bci, BytecodeSwitch tswitch) {
        int max = tswitch.numberOfCases();
        for (int i = 0; i < max; i++) {
            set(bci, tswitch.targetAt(i));
        }
        set(bci, tswitch.defaultTarget());
    }

    private int branch(int bci, int targetBCI, int nextBCI) {
        set(bci, targetBCI);
        setEntry(nextBCI, BLOCK_START);
        return nextBCI;
    }

    private int jump(int bci, int targetBCI, int nextBCI) {
        set(bci, targetBCI);
        return nextBCI;
    }

    static boolean isBlockStart(int bci, byte[] blockMap) {
        return blockMap[bci] != 0;
    }

    static boolean isBackwardBranchTarget(int bci, byte[] blockMap) {
        return (blockMap[bci] & BLOCK_BACKWARD_TARGET) != 0;
    }

    static boolean isExceptionEntry(int bci, byte[] blockMap) {
        return (blockMap[bci] & BLOCK_EXCEPTION_ENTRY) != 0;
    }

    static boolean isSubroutineEntry(int bci, byte[] blockMap) {
        return (blockMap[bci] & BLOCK_SUBROUTINE_ENTRY) != 0;
    }

    static boolean isSubroutineReturn(int bci, byte[] blockMap) {
        return (blockMap[bci] & BLOCK_SUBROUTINE_RETURN) != 0;
    }

    static int getPredecessorCount(int bci, byte[] blockMap) {
        return (blockMap[bci] & 0xff) >> PREDECESSOR_SHIFT;
    }
}
