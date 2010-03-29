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
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.stub.*;

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
    private BlockBegin ublock; // if this is a float-branch, this is the unordered block

    /**
     * Creates a new LIRBranch instruction.
     *
     * @param cond the branch condition
     * @param label target label
     *
     */
    public LIRBranch(Condition cond, Label label) {
        super(LIROpcode.Branch, LIROperand.IllegalLocation, null, false, null, 0, 0);
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
        super(LIROpcode.Branch, LIROperand.IllegalLocation, null, false, stub, 0, 0);
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
        super(LIROpcode.Branch, LIROperand.IllegalLocation, null, false, null, 0, 0);
        this.cond = cond;
        this.kind = kind;
        this.label = block.label();
        this.block = block;
        this.ublock = null;
    }

    public LIRBranch(Condition cond, CiKind kind, BlockBegin block, BlockBegin ublock) {
        super(LIROpcode.CondFloatBranch, LIROperand.IllegalLocation, null, false, null, 0, 0);
        this.cond = cond;
        this.kind = kind;
        this.label = block.label();
        this.block = block;
        this.ublock = ublock;
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

    public BlockBegin ublock() {
        return ublock;
    }

    public void changeBlock(BlockBegin b) {
        assert block != null : "must have old block";
        assert block.label() == label() : "must be equal";

        this.block = b;
        this.label = b.label();
    }

    public void changeUblock(BlockBegin b) {
        assert ublock != null : "must have old block";
        this.ublock = b;
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
    public void printInstruction(LogStream out) {
        printCondition(out, cond());
        out.print(" ");
        if (block() != null) {
            out.printf("[B%d] ", block().blockID);
        } else {
            if (stub != null) {
                out.print("[");
                stub.printName(out);
                out.printf(": %s]", stub.toString());
                if (stub.info != null) {
                    out.printf(" [bci:%d]", stub.info.bci);
                }
            } else {
                out.printf("[label:0x%x] ", label().position());
            }
        }
        if (ublock() != null) {
            out.printf("unordered: [B%d] ", ublock().blockID);
        }
    }

    public void substitute(BlockBegin oldBlock, BlockBegin newBlock) {
        if (block == oldBlock) {
            block = newBlock;
            LIRInstruction instr = newBlock.lir().instructionsList().get(0);
            assert instr instanceof LIRLabel : "first instruction of block must be label";
            label = ((LIRLabel) instr).label();
        }
        if (ublock == oldBlock) {
            ublock = newBlock;
        }
    }
}
