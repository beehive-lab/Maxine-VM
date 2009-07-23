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

import com.sun.c1x.asm.Label;
import com.sun.c1x.debug.LogStream;
import com.sun.c1x.ir.BlockBegin;
import com.sun.c1x.stub.CodeStub;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class LIRBranch extends LIRInstruction {

    private LIRCondition cond;
    private BasicType type;
    private Label label;
    private BlockBegin block;  // if this is a branch to a block, this is the block
    private BlockBegin ublock; // if this is a float-branch, this is the unordered block
    CodeStub stub;     // if this is a branch to a stub, this is the stub

    /**
     * Creates a new LIRBranch instruction.
     *
     * @param cond the branch condition
     * @param label target label
     *
     */
    public LIRBranch(LIRCondition cond, Label label) {
        super(LIROpcode.Branch, LIROperandFactory.IllegalOperand, null);
        this.cond = cond;
        this.label = label;
    }

    /**
     * Creates a new LIRBranch instruction.
     *
     * @param cond the branch condition
     * @param type
     * @param stub
     *
     */
    public LIRBranch(LIRCondition cond, BasicType type, CodeStub stub) {
        this(cond, stub.entry());
        this.type = type;
        this.stub = stub;
    }

    /**
     * Creates a new LIRBranch instruction.
     *
     * @param cond
     * @param type
     * @param block
     *
     */
    public LIRBranch(LIRCondition cond, BasicType type, BlockBegin block) {
        this(cond, type, block, null);
    }

    public LIRBranch(LIRCondition cond, BasicType type, BlockBegin block, BlockBegin ublock) {
        super(LIROpcode.Branch, LIROperandFactory.IllegalOperand, null);
        this.cond = cond;
        this.type = type;
        this.label = block.label();
        this.block = block;
        this.ublock = ublock;
    }

    /**
     * @return the condition
     */
    public LIRCondition cond() {
        return cond;
    }

    /**
     * @return the type of this condition
     */
    BasicType type() {
        return type;
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

    public CodeStub stub() {
        return stub;
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
        switch (this.cond) {
            case AboveEqual:
                cond = LIRCondition.NotEqual;
                break;
            case NotEqual:
                cond = LIRCondition.Equal;
                break;
            case Less:
                cond = LIRCondition.GreaterEqual;
                break;
            case LessEqual:
                cond = LIRCondition.Greater;
                break;
            case GreaterEqual:
                cond = LIRCondition.Less;
                break;
            case Greater:
                cond = LIRCondition.LessEqual;
                break;
            default:
                Util.shouldNotReachHere();
        }
    }

    @Override
    public void emitCode(LIRAssembler masm) {
        masm.emitBranch(this);
        if (stub() != null) {
            masm.emitCodeStub(stub());
        }
    }

    @Override
    public void printInstruction(LogStream out) {
        printCondition(out, cond());
        out.print(" ");
        if (block() != null) {
            out.printf("[B%d] ", block().blockID);
        } else if (stub() != null) {
            out.print("[");
            stub().printName(out);
            out.printf(": %s]", stub().toString());
            if (stub().info() != null) {
                out.printf(" [bci:%d]", stub().info().bci());
            }
        } else {
            out.printf("[label:0x%x] ", label().loc());
        }
        if (ublock() != null) {
            out.printf("unordered: [B%d] ", ublock().blockID);
        }
    }
}
