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
package com.sun.c1x.lir;

import com.sun.c1x.asm.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.stub.*;
import com.sun.cri.ci.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class LIRBranch extends LIRInstruction {

    private Condition cond;
    private CiKind kind;
    private Label label;
    private BlockBegin block;  // if this is a branch to a block, this is the block

    /**
     * This is the unordered block for a float branch.
     */
    private BlockBegin unorderedBlock; //

    /**
     * Creates a new LIRBranch instruction.
     *
     * @param cond the branch condition
     * @param label target label
     *
     */
    public LIRBranch(Condition cond, Label label) {
        super(LIROpcode.Branch, CiValue.IllegalValue, null, false, null);
        this.cond = cond;
        this.label = label;
    }

    /**
     * Creates a new LIRBranch instruction.
     *
     * @param cond the branch condition
     * @param kind
     * @param stub
     *
     */
    public LIRBranch(Condition cond, CiKind kind, LocalStub stub) {
        super(LIROpcode.Branch, CiValue.IllegalValue, null, false, stub);
        this.cond = cond;
        this.label = stub.entry;
        this.kind = kind;
    }

    /**
     * Creates a new LIRBranch instruction.
     *
     * @param cond
     * @param kind
     * @param block
     *
     */
    public LIRBranch(Condition cond, CiKind kind, BlockBegin block) {
        super(LIROpcode.Branch, CiValue.IllegalValue, null, false, null);
        this.cond = cond;
        this.kind = kind;
        this.label = block.label();
        this.block = block;
        this.unorderedBlock = null;
    }

    public LIRBranch(Condition cond, CiKind kind, BlockBegin block, BlockBegin ublock) {
        super(LIROpcode.CondFloatBranch, CiValue.IllegalValue, null, false, null);
        this.cond = cond;
        this.kind = kind;
        this.label = block.label();
        this.block = block;
        this.unorderedBlock = ublock;
    }

    /**
     * @return the condition
     */
    public Condition cond() {
        return cond;
    }

    public Label label() {
        return label;
    }

    public BlockBegin block() {
        return block;
    }

    public BlockBegin unorderedBlock() {
        return unorderedBlock;
    }

    public void changeBlock(BlockBegin b) {
        assert block != null : "must have old block";
        assert block.label() == label() : "must be equal";

        this.block = b;
        this.label = b.label();
    }

    public void changeUblock(BlockBegin b) {
        assert unorderedBlock != null : "must have old block";
        this.unorderedBlock = b;
    }

    public void negateCondition() {
        cond = cond.negate();
    }

    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitBranch(this);
        if (stub != null) {
            masm.addCodeStub(stub);
        }
    }

    @Override
    public String operationString(OperandFormatter operandFmt) {
        StringBuilder buf = new StringBuilder(cond().operator).append(' ');
        if (block() != null) {
            buf.append("[B").append(block.blockID).append(']');
        } else {
            if (stub != null) {
                buf.append("[").append(stub.name()).append(": ").append(stub).append(']');
                if (stub.info != null) {
                    buf.append(" [bci:").append(stub.info.bci).append(']');
                }
            } else {
                buf.append("[label:0x").append(Integer.toHexString(label().position())).append(']');
            }
        }
        if (unorderedBlock() != null) {
            buf.append("unordered: [B").append(unorderedBlock().blockID).append(']');
        }
        return buf.toString();
    }

    public void substitute(BlockBegin oldBlock, BlockBegin newBlock) {
        if (block == oldBlock) {
            block = newBlock;
            LIRInstruction instr = newBlock.lir().instructionsList().get(0);
            assert instr instanceof LIRLabel : "first instruction of block must be label";
            label = ((LIRLabel) instr).label();
        }
        if (unorderedBlock == oldBlock) {
            unorderedBlock = newBlock;
        }
    }
}
