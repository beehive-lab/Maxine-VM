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
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class LIRBranch extends LIRInstruction {

    private LIRCondition cond;
    private BasicType type;
    private Label label;
    private BlockBegin block; // if this is a branch to a block, this is the block
    private BlockBegin ublock; // if this is a float-branch, this is the unordered block
    private CodeStub stub; // if this is a branch to a stub, this is the stub

    public LIRBranch(LIRCondition cond, Label label) {
        super(LIROpcode.Branch, LIROperandFactory.illegalOperand, null);
        this.cond = cond;
        this.label = label;
    }

    public LIRBranch(LIRCondition cond, BasicType type, CodeStub stub) {
        this(cond, stub.entry());
        this.type = type;
        this.stub = stub;
    }

    public LIRBranch(LIRCondition cond, BasicType type, BlockBegin block) {
        this(cond, type, block, null);
    }

    public LIRBranch(LIRCondition cond, BasicType type, BlockBegin block, BlockBegin ublock) {
        super(LIROpcode.Branch, LIROperandFactory.illegalOperand, null);
        this.type = type;
        this.label = block.label();
        this.block = block;
        this.ublock = ublock;
    }

    LIRCondition cond() {
        return cond;
    }

    BasicType type() {
        return type;
    }

    Label label() {
        return label;
    }

    BlockBegin block() {
        return block;
    }

    BlockBegin ublock() {
        return ublock;
    }

    CodeStub stub() {
        return stub;
    }

    void changeBlock(BlockBegin b) {
        assert block != null : "must have old block";
        assert block.label() == label() : "must be equal";

        this.block = b;
        this.label = b.label();
    }

    void changeUblock(BlockBegin b) {
        assert ublock != null : "must have old block";
        this.ublock = b;
    }

    void negateCondition() {
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
            masm.emitStub(stub());
        }
    }

    @Override
    LIRBranch asBranch() {
        return this;
    }

    @Override
    public void printInstruction(LogStream out) {
        printCondition(out, cond());
        out.print(" ");
        if (block() != null) {
            out.printf("[B%d] ", block().blockID());
        } else if (stub() != null) {
            out.print("[");
            stub().printName(out);
            out.printf(": 0x%x]", stub());
            if (stub().info() != null) {
                out.printf(" [bci:%d]", stub().info().bci());
            }
        } else {
            out.printf("[label:0x%x] ", label().loc());
        }
        if (ublock() != null) {
            out.printf("unordered: [B%d] ", ublock().blockID());
        }
    }
}
