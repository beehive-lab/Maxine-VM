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

import java.util.*;

import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;

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

    final byte[] code;
    final byte[] blockMap;
    List<CiExceptionHandler> handlers;
    int numBlocks;
    int numBackwardEdges;

    /**
     * Creates a new BlockMap instance from the specified bytecode.
     * @param method the compiler interface method containing the code
     */
    public BlockMarker(CiMethod method) {
        byte[] code = method.code();
        this.handlers = method.exceptionHandlers();
        this.code = code;
        this.blockMap = new byte[code.length];
    }

    public void markBlocks() {
        if (handlers != null) {
            // mark all exception handler blocks
            for (CiExceptionHandler h : handlers) {
                setExceptionEntry(h.handlerBCI());
            }
        }
        iterateOverBytecodes();
    }

    void iterateOverBytecodes() {
        // iterate over the bytecodes top to bottom, discovering the starts of basic blocks
        int bci = 0;
        byte[] code = this.code;
        setEntry(0);
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
                    succ2(bci, bci + 3, bci + Bytes.beS2(code, bci + 1));
                    bci += 3; // these are all 3 byte opcodes
                    break;
                }

                case Bytecodes.GOTO: {
                    succ1(bci, bci + Bytes.beS2(code, bci + 1));
                    bci += 3; // goto is 3 bytes
                    break;
                }

                case Bytecodes.GOTO_W: {
                    succ1(bci, bci + Bytes.beS4(code, bci + 1));
                    bci += 5; // goto_w is 5 bytes
                    break;
                }

                case Bytecodes.JSR: {
                    int target = bci + Bytes.beS2(code, bci + 1);
                    succ2(bci, bci + 3, target);
                    setSubroutineEntry(target);
                    bci += 3; // jsr is 3 bytes
                    break;
                }

                case Bytecodes.JSR_W: {
                    int target = bci + Bytes.beS4(code, bci + 1);
                    succ2(bci, bci + 5, target);
                    setSubroutineEntry(target);
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
                    bci += Bytecodes.length(opcode); // all variable length instructions are handled above
                }
            }
        }
    }

    void setEntry(int bci) {
        if (blockMap[bci] == 0) {
            numBlocks++;
        }
        blockMap[bci] |= BLOCK_START;
    }

    void setBackwardsBranchTarget(int bci) {
        if (blockMap[bci] == 0) {
            numBlocks++;
        }
        blockMap[bci] |= BLOCK_START | BLOCK_BACKWARD_TARGET;
    }

    void setExceptionEntry(int bci) {
        if (blockMap[bci] == 0) {
            numBlocks++;
        }
        blockMap[bci] |= BLOCK_START | BLOCK_EXCEPTION_ENTRY;
    }

    void setSubroutineEntry(int bci) {
        if (blockMap[bci] == 0) {
            numBlocks++;
        }
        blockMap[bci] |= BLOCK_START | BLOCK_SUBROUTINE_ENTRY;
    }

    void branch(int fromBCI, int toBCI) {
        if (fromBCI >= toBCI) {
            setBackwardsBranchTarget(toBCI);
        } else {
            setEntry(toBCI);
        }
    }

    private void makeSwitchSuccessors(int bci, BytecodeSwitch tswitch) {
        // make a list of all the successors of a switch
        int max = tswitch.numberOfCases();
        ArrayList<BlockBegin> list = new ArrayList<BlockBegin>(max + 1);
        for (int i = 0; i < max; i++) {
            branch(bci, tswitch.targetAt(i));
        }
        branch(bci, tswitch.defaultTarget());
    }

    void succ2(int bci, int s1, int s2) {
        branch(bci, s1);
        branch(bci, s2);
    }

    void succ1(int bci, int s1) {
        branch(bci, s1);
    }
}