/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c1x.debug;

import java.util.*;

import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * The {@code IRChecker} class walks over the IR graph and checks
 * that each instruction has the appropriate type for its inputs and output,
 * as well as other structural properties of the IR graph.
 *
 * @author Marcelo Cintra
 * @author Ben L. Titzer
 */
public class IRChecker extends ValueVisitor {

    /**
     * The {@code IRCheckException} class is thrown when the IRChecker detects
     * a problem with the IR.
     *
     * @author Marcelo Cintra
     */
    public static class IRCheckException extends RuntimeException {
        public static final long serialVersionUID = 8974598793158772L;

        public IRCheckException(String msg) {
            super(msg);
        }
    }

    private final IR ir;
    private final String phase;
    private final HashMap<Integer, BlockBegin> idMap = new HashMap<Integer, BlockBegin>();
    private final BasicValueChecker basicChecker = new BasicValueChecker();

    /**
     * Creates a new IRChecker for the specified IR.
     * @param ir the IR to check
     * @param phase the phase of compilation when verification is being run
     */
    public IRChecker(IR ir, String phase) {
        this.ir = ir;
        this.phase = phase;
    }

    public void check() {
        ir.startBlock.iterateAnyOrder(new CheckBlock(), false);
        ir.startBlock.iterateAnyOrder(new CheckReachable(), true);
        ir.startBlock.iterateAnyOrder(new CheckValues(), false);
    }

    private class CheckBlock implements BlockClosure {
        public void apply(BlockBegin block) {
            // check every instruction in the block top to bottom
            BlockBegin b = idMap.get(block.blockID);
            if (b != null && b != block) {
                fail("Block id is not unique " + block + " and " + b);
            }
            idMap.put(block.blockID, block);
            Instruction instr = block;
            Instruction prev = block;
            while (instr != null) {
                if (instr instanceof BlockEnd) {
                    assertNull(instr.next(), "BlockEnd should not have next");
                }
                prev = instr;
                instr = instr.next();
            }
            if (!(prev instanceof BlockEnd)) {
                fail("Block should end with a BlockEnd " + block);
            }
            if (prev != block.end()) {
                fail("Block refers to wrong block end " + block);
            }
            checkBlockEnd(block.end());
        }
    }

    private class CheckReachable implements BlockClosure {
        public void apply(BlockBegin block) {
            // check that the block was reachable in the first pass
            if (idMap.get(block.blockID) != block) {
                fail("Block is not reachable from start block: " + block);
            }
            List<BlockBegin> linearScanOrder = ir.linearScanOrder();
            if (linearScanOrder != null) {
                if (!linearScanOrder.contains(block)) {
                    fail("Block not in linear scan list: " + block);
                }
            }
        }
    }

    private Instruction currentInstruction;
    private BlockBegin currentBlock;

    private class CheckValues implements BlockClosure {
        public void apply(BlockBegin block) {
            currentBlock = block;
            Instruction instr = block;
            while (instr != null) {
                currentInstruction = instr;
                basicChecker.apply(instr);
                instr.allValuesDo(basicChecker);
                instr.accept(IRChecker.this);
                instr = instr.next();
            }
        }
    }

    protected void unimplented(Value value) {
        fail("unimplemented: visiting value of type " + value.getClass().getSimpleName());
    }

    /**
     * Checks the types of incoming instructions and the type of this instruction
     * match the types expected by the opcode.
     * @param i the ArithmeticOp instruction to be verified
     */
    @Override
    public void visitArithmeticOp(ArithmeticOp i) {
        Value x = i.x();
        Value y = i.y();

        switch (i.opcode) {
            case Bytecodes.IADD:
            case Bytecodes.ISUB:
            case Bytecodes.IMUL:
            case Bytecodes.IDIV:
            case Bytecodes.IREM:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Int);
                assertKind(y, CiKind.Int);
                break;

            case Bytecodes.LADD:
            case Bytecodes.LSUB:
            case Bytecodes.LMUL:
            case Bytecodes.LDIV:
            case Bytecodes.LREM:
                assertKind(i, CiKind.Long);
                assertKind(x, CiKind.Long);
                assertKind(y, CiKind.Long);
                break;

            case Bytecodes.FADD:
            case Bytecodes.FSUB:
            case Bytecodes.FMUL:
            case Bytecodes.FDIV:
            case Bytecodes.FREM:
                assertKind(i, CiKind.Float);
                assertKind(x, CiKind.Float);
                assertKind(y, CiKind.Float);
                break;

            case Bytecodes.DADD:
            case Bytecodes.DSUB:
            case Bytecodes.DMUL:
            case Bytecodes.DDIV:
            case Bytecodes.DREM:
                assertKind(i, CiKind.Double);
                assertKind(x, CiKind.Double);
                assertKind(y, CiKind.Double);
                break;

            case Bytecodes.WREM:
            case Bytecodes.WDIV:
                assertKind(i, CiKind.Word);
                assertKind(x, CiKind.Word);
                assertKind(y, CiKind.Word);
                break;
            case Bytecodes.WDIVI:
                assertKind(i, CiKind.Word);
                assertKind(x, CiKind.Word);
                assertKind(y, CiKind.Int);
                break;
            case Bytecodes.WREMI:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Word);
                assertKind(y, CiKind.Int);
                break;

            default:
                fail("Arithmetic operation instruction has an illegal opcode: " + Bytecodes.nameOf(i.opcode));
        }
    }

    /**
     * Checks the types of incoming instructions and the type of this instruction
     * match the types expected by the opcode.
     * @param i the logic instruction to be verified
     */
    @Override
    public void visitLogicOp(LogicOp i) {
        Value x = i.x();
        Value y = i.y();

        switch (i.opcode) {
            case Bytecodes.IAND:
                assertKind(i, CiKind.Int);
                assertKind(y, CiKind.Int);
                if (!x.kind.isWord()) {
                    assertKind(x, CiKind.Int);
                } else {
                    // Result of strength reduction on Bytecodes.WREMI
                }
                break;
            case Bytecodes.IOR:
            case Bytecodes.IXOR:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Int);
                assertKind(y, CiKind.Int);
                break;
            case Bytecodes.LAND:
                assertKind(y, CiKind.Long);
                if (!x.kind.isWord()) {
                    assertKind(x, CiKind.Long);
                    assertKind(i, CiKind.Long);
                } else {
                    // Result of strength reduction on Bytecodes.WREM
                    assertKind(i, CiKind.Word);
                }
                break;
            case Bytecodes.LOR:
            case Bytecodes.LXOR:
                assertKind(i, CiKind.Long);
                assertKind(x, CiKind.Long);
                assertKind(y, CiKind.Long);
                break;
            default:
                fail("Logic operation instruction has an illegal opcode");
        }
    }

    /**
     * Checks the types of the incoming instruction and the type of this instruction
     * match the types expected by the opcode.
     * @param i the NegateOp instruction to be verified
     */
    @Override
    public void visitNegateOp(NegateOp i) {
        assertKind(i, i.x().kind);
    }

    /**
     * Checks the types of incoming instructions and the type of this instruction
     * match the types expected by the opcode.
     * @param i the CompareOp instruction to be verified
     */
    @Override
    public void visitCompareOp(CompareOp i) {
        Value x = i.x();
        Value y = i.y();

        switch (i.opcode) {
            case Bytecodes.LCMP:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Long);
                assertKind(y, CiKind.Long);
                break;
            case Bytecodes.FCMPG:
            case Bytecodes.FCMPL:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Float);
                assertKind(y, CiKind.Float);
                break;
            case Bytecodes.DCMPG:
            case Bytecodes.DCMPL:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Double);
                assertKind(y, CiKind.Double);
                break;
            default:
                fail("Illegal CompareOp opcode: " + Bytecodes.nameOf(i.opcode));
        }
    }

    @Override
    public void visitCompareAndSwap(CompareAndSwap i) {
        assertKind(i.pointer(), CiKind.Word);
        assertNull(i.displacement(), "displacement should be null");
        assertKind(i.newValue(), i.kind);
        assertKind(i.expectedValue(), i.kind);
        checkPointerOpOffsetOrIndex(i.offset());
        switch (i.opcode) {
            case Bytecodes.PCMPSWP_INT:
                assertKind(i, CiKind.Int);
                break;
            case Bytecodes.PCMPSWP_INT_I:
                assertKind(i, CiKind.Int);
                break;
            case Bytecodes.PCMPSWP_REFERENCE:
                assertKind(i, CiKind.Object);
                break;
            case Bytecodes.PCMPSWP_REFERENCE_I:
                assertKind(i, CiKind.Object);
                break;
            case Bytecodes.PCMPSWP_WORD:
                assertKind(i, CiKind.Word);
                break;
            case Bytecodes.PCMPSWP_WORD_I:
                assertKind(i, CiKind.Word);
                break;
            default:
                fail("Illegal CompareAndSwap opcode: " + Bytecodes.nameOf(i.opcode));
        }
    }

    /**
     * Typechecks a IfOp instruction.
     * @param i the IfOp instruction to be verified
     */
    @Override
    public void visitIfOp(IfOp i) {
        if (i.opcode != Bytecodes.ILLEGAL) {
            fail("Opcode for IfOp instruction must be ILLEGAL");
        }

        assertLegal(i);
        if (i.x().kind != i.y().kind) {
            fail("Operands to IfOp do not have the same kind");
        }
        assertKind(i, i.trueValue().kind.meet(i.falseValue().kind));
    }

    /**
     * Checks the types of incoming instructions and the type of this instruction
     * match the types expected by the opcode.
     * @param i the ShiftOp instruction to be verified
     */
    @Override
    public void visitShiftOp(ShiftOp i) {
        switch (i.opcode) {
            case Bytecodes.ISHL:
            case Bytecodes.ISHR:
                assertKind(i, CiKind.Int);
                assertKind(i.x(), CiKind.Int);
                assertKind(i.y(), CiKind.Int);
                break;
            case Bytecodes.IUSHR:
                if (i.kind.isWord()) {
                    // Result of strength reduction on Bytecodes.WDIVI
                    assertKind(i.x(), CiKind.Word);
                    assertKind(i.y(), CiKind.Int);
                } else {
                    assertKind(i, CiKind.Int);
                    assertKind(i.x(), CiKind.Int);
                    assertKind(i.y(), CiKind.Int);
                }
                break;

            case Bytecodes.LSHL:
            case Bytecodes.LSHR:
                assertKind(i, CiKind.Long);
                assertKind(i.x(), CiKind.Long);
                assertKind(i.y(), CiKind.Int);
                break;
            case Bytecodes.LUSHR:
                if (i.kind.isWord()) {
                    // Result of strength reduction on Bytecodes.WDIV
                    assertKind(i.x(), CiKind.Word);
                    assertKind(i.y(), CiKind.Int);
                } else {
                    assertKind(i, CiKind.Long);
                    assertKind(i.x(), CiKind.Long);
                    assertKind(i.y(), CiKind.Int);
                }
                break;
            default:
                fail("Illegal ShiftOp opcode");
        }
    }

    /**
     * Checks the types of incoming instruction and the type of this instruction
     * match the types expected by the opcode.
     * @param i the convert instruction to be verified
     */
    @Override
    public void visitConvert(Convert i) {
        switch (i.opcode) {
            case Bytecodes.I2L:
                assertKind(i, CiKind.Long);
                assertKind(i.value(), CiKind.Int);
                break;
            case Bytecodes.I2F:
                assertKind(i, CiKind.Float);
                assertKind(i.value(), CiKind.Int);
                break;
            case Bytecodes.I2D:
                assertKind(i, CiKind.Double);
                assertKind(i.value(), CiKind.Int);
                break;
            case Bytecodes.I2B:
            case Bytecodes.I2C:
            case Bytecodes.I2S:
                assertKind(i, CiKind.Int);
                assertKind(i.value(), CiKind.Int);
                break;

            case Bytecodes.L2I:
                assertKind(i, CiKind.Int);
                assertKind(i.value(), CiKind.Long);
                break;
            case Bytecodes.L2F:
                assertKind(i, CiKind.Float);
                assertKind(i.value(), CiKind.Long);
                break;
            case Bytecodes.L2D:
                assertKind(i, CiKind.Double);
                assertKind(i.value(), CiKind.Long);
                break;

            case Bytecodes.F2I:
                assertKind(i, CiKind.Int);
                assertKind(i.value(), CiKind.Float);
                break;
            case Bytecodes.F2L:
                assertKind(i, CiKind.Long);
                assertKind(i.value(), CiKind.Float);
                break;
            case Bytecodes.F2D:
                assertKind(i, CiKind.Double);
                assertKind(i.value(), CiKind.Float);
                break;

            case Bytecodes.D2I:
                assertKind(i, CiKind.Int);
                assertKind(i.value(), CiKind.Double);
                break;
            case Bytecodes.D2L:
                assertKind(i, CiKind.Long);
                assertKind(i.value(), CiKind.Double);
                break;
            case Bytecodes.D2F:
                assertKind(i, CiKind.Float);
                assertKind(i.value(), CiKind.Double);
                break;
            case Bytecodes.MOV_F2I:
                assertKind(i, CiKind.Int);
                assertKind(i.value(), CiKind.Float);
                break;
            case Bytecodes.MOV_I2F:
                assertKind(i, CiKind.Float);
                assertKind(i.value(), CiKind.Int);
                break;
            case Bytecodes.MOV_D2L:
                assertKind(i, CiKind.Long);
                assertKind(i.value(), CiKind.Double);
                break;
            case Bytecodes.MOV_L2D:
                assertKind(i, CiKind.Double);
                assertKind(i.value(), CiKind.Long);
                break;
            default:
                fail("invalid opcode in Convert: " + Bytecodes.nameOf(i.opcode));
        }
    }

    /**
     * Checks that the incoming instruction is an object, and this instruction is an object.
     * @param i the null check instruction to be verified
     */
    @Override
    public void visitNullCheck(NullCheck i) {
        if (i.object() == null) {
            fail("There is no instruction producing the object to check against null");
        }
        assertKind(i, CiKind.Object);
        assertKind(i.object(), CiKind.Object);
    }

    /**
     * Checks the incoming object instruction, if any, is of type object and that
     * this instruction has the same type as the field's type.
     * @param i the LoadField instruction to be verified
     */
    @Override
    public void visitLoadField(LoadField i) {
        assertKind(i, i.field().kind().stackKind());
        Value object = i.object();
        if (object != null) {
            assertKind(object, CiKind.Object);
            assertInstanceType(object.declaredType());
            assertInstanceType(object.exactType());
        } else if (!i.isStatic()) {
            fail("LoadField of instance field should not have null object");
        }
    }

    /**
     * Typechecks a StoreField instruction.
     * @param i the StoreField instruction to be verified
     */
    @Override
    public void visitStoreField(StoreField i) {
        assertKind(i.value(), i.field().kind().stackKind());
        Value object = i.object();
        if (object != null) {
            assertKind(object, CiKind.Object);
            assertInstanceType(object.declaredType());
            assertInstanceType(object.exactType());
        } else if (!i.isStatic()) {
            fail("StoreField of instance field should not have null object");
        }
    }

    /**
     * Typechecks a LoadIndexed instruction.
     * @param i the LoadIndexed instruction to be verified
     */
    @Override
    public void visitLoadIndexed(LoadIndexed i) {
        assertKind(i.array(), CiKind.Object);
        assertKind(i.index(), CiKind.Int);
        assertKind(i, i.elementKind().stackKind());
        assertArrayType(i.array().exactType());
        assertArrayType(i.array().declaredType());
    }

    /**
     * Typechecks a StoreIndexed instruction.
     * @param i the StoreIndexed instruction to be verified
     */
    @Override
    public void visitStoreIndexed(StoreIndexed i) {
        assertKind(i.array(), CiKind.Object);
        assertKind(i.index(), CiKind.Int);
        assertKind(i.value(), i.elementKind().stackKind());
        assertArrayType(i.array().exactType());
        assertArrayType(i.array().declaredType());
    }

    /**
     * Typechecks an ArrayLength instruction.
     * @param i the ArrayLength instruction to be verified
     */
    @Override
    public void visitArrayLength(ArrayLength i) {
        assertKind(i.array(), CiKind.Object);
        assertArrayType(i.array().exactType());
        assertArrayType(i.array().declaredType());
        assertKind(i, CiKind.Int);
    }

    /**
     * Typechecks a Constant instruction.
     * @param i the constant to be verified
     */
    @Override
    public void visitConstant(Constant i) {
        // do nothing.
    }

    /**
     * Typechecks an Exception Object instruction.
     * @param i the ExceptionObject instruction to be verified
     */
    @Override
    public void visitExceptionObject(ExceptionObject i) {
        assertKind(i, CiKind.Object);
    }

    /**
    * Typechecks a Local instruction.
    * @param i the Local instruction to be verified
    */
    @Override
    public void visitLocal(Local i) {
        if (i.javaIndex() < 0) {
            fail("Java index of Local instruction must be greater then or equal to zero");
        }
        assertLegal(i);
    }

    /**
    * Typechecks an OsrEntry instruction.
    * @param i the OsrEntry instruction to be verified
    */
    @Override
    public void visitOsrEntry(OsrEntry i) {
        // TODO: this type should probably be CiKind.Word in the future
        assertKind(i, CiKind.Jsr);
    }

    /**
     * Checks if the type of each operand in a Phi instruction is equivalent to
     * the type of the destination variable (or operand stack) slot.
     * @param i the arithmetic operation to be verified
     */
    @Override
    public void visitPhi(Phi i) {
        if (!i.isIllegal()) {
            checkPhi(i);
        }
    }

    private void checkPhi(Phi i) {
        BlockBegin block = i.block();
        if (idMap.get(block.blockID) != block) {
            fail("Phi refers to unreachable block " + i + " " + block);
        }
        // if the phi instruction corresponds to a local variable, checks if the local index is valid
        if (i.isLocal() && (i.localIndex() >= block.stateBefore().scope().method.maxLocals() || i.localIndex() < 0)) {
            fail("Phi refers to an invalid local variable");
        }
        for (int j = 0; j < i.inputCount(); j++) {
            assertKind(i.inputAt(j), i.kind);
        }
    }

    /**
     * Typechecks a MonitorEnter instruction.
     * @param i the MonitorEnter instruction to be verified
     */
    @Override
    public void visitMonitorEnter(MonitorEnter i) {
        assertKind(i, CiKind.Illegal);
        assertKind(i.object(), CiKind.Object);
    }

    /**
     * Typechecks a MonitorExit instruction.
     * @param i the MonitorExit instruction to be verified
     */
    @Override
    public void visitMonitorExit(MonitorExit i) {
        assertKind(i, CiKind.Illegal);
        assertKind(i.object(), CiKind.Object);
    }

    /**
     * Typechecks a BlockBegin instruction.
     * @param i the BlockBegin instruction to be verified
     */
    @Override
    public void visitBlockBegin(BlockBegin i) {
        BlockBegin b = idMap.get(i.blockID);
        if (b != null && b != i) {
            fail("Block id is not unique " + i + " and " + b);
        }
        idMap.put(i.blockID, i);
        assertNonNull(i.stateBefore(), "Block must have initial state");
        assertKind(i, CiKind.Illegal);
        if (i.depthFirstNumber() < -1) {
            fail("Block has an invalid depth first number");
        }
        assertNonNull(i.end(), "BlockBegin does not have BlockEnd");
        if (i.end().begin() != i) {
            fail("BlockEnd does not refer back to this BlockBegin");
        }
        // check that each predecessor has this block in its successor list
        List<BlockBegin> preds = i.predecessors();
        assertNonNull(preds, "Predecessor list does not exist");
        for (BlockBegin pred : preds) {
            List<BlockBegin> succ = i.isExceptionEntry() ? pred.exceptionHandlerBlocks() : pred.end().successors();
            if (!succ.contains(i)) {
                fail("Predecessor block does not have this block in its successor list");
            }
        }
        checkBlockEnd(i.end());
    }

    /**
     * Typechecks a Base instruction.
     * @param i the Base instruction to be verified
     */
    @Override
    public void visitBase(Base i) {
        assertKind(i, CiKind.Illegal);
        if (i.isSafepoint()) {
            fail("Value Base is not a safepoint instruction ");
        }
    }

    /**
     * Typechecks a Goto instruction.
     * @param i the Goto instruction to be verified
     */
    @Override
    public void visitGoto(Goto i) {
        assertKind(i, CiKind.Illegal);
        if (i.successors().size() != 1) {
            fail("Goto instruction must have one successor");
        }
        assertNonNull(i.stateAfter(), "Goto must have state after");
    }

    /**
     * Typechecks an If instruction.
     * @param i the If instruction to be verified
     */
    @Override
    public void visitIf(If i) {
        assertKind(i, CiKind.Illegal);
        if (!Util.archKindsEqual(i.x(), i.y())) {
            fail("Operands of If instruction must have same type");
        }
        if (i.successors().size() != 2) {
            fail("If instruction must have 2 successors");
        }
        assertNonNull(i.stateAfter(), "If must have state after");
    }

    void checkBlockEnd(BlockEnd i) {
        assertNonNull(i.begin(), "BlockEnd has no BlockBegin");
        // check that each successor has this block in its predecessor list
        List<BlockBegin> succs = i.successors();
        assertNonNull(succs, "BlockEnd successor list does not exist");
        for (BlockBegin succ : succs) {
            if (!succ.predecessors().contains(i.begin())) {
                fail("Successor block does not have this block in its predecessor list");
            }
        }
        if (i.next() != null) {
            fail("BlockEnd must not have next");
        }
    }

    /**
     * Typechecks an InstanceOf instruction.
     * @param i the InstanceOf instruction to be verified
     */
    @Override
    public void visitInstanceOf(InstanceOf i) {
        assertKind(i, CiKind.Int);
        assertKind(i.object(), CiKind.Object);
        assertNonNull(i.targetClass(), "targetClass in InsatanceOf instruction must be non null");
        assertNotPrimitive(i.targetClass());
    }

    /**
     * Typechecks a a Return instruction.
     * @param i the Return instruction to be verified
     */
    @Override
    public void visitReturn(Return i) {
        final Value result = i.result();

        CiKind retType = ir.compilation.method.signature().returnKind();
        if (result == null) {
            assertKind(i, CiKind.Void);
            if (retType != CiKind.Void) {
                fail("Must return value from non-void method");
            }
        } else {
            if (retType == CiKind.Void) {
                fail("Must not return value from void method");
            }
            if (i.kind == CiKind.Void) {
                fail("Return instruction must not be of type void if method returns a value");
            }
            assertKind(result, retType.stackKind());
            if (i.kind != retType.stackKind()) {
                fail("Return value type does not match the method's return type");
            }
        }
    }

    /**
     * Typechecks a LookupSwitch instruction.
     * @param i the LookupSwitch instruction to be verified
     */
    @Override
    public void visitLookupSwitch(LookupSwitch i) {
        assertKind(i, CiKind.Illegal);
        assertKind(i.value(), CiKind.Int);

        if (i.numberOfCases() > 1) {
            int min = i.keyAt(0);
            for (int j = 1; j < i.numberOfCases(); j++) {
                if (min >= i.keyAt(j)) {
                    fail("LookupSwitch keys must be ordered");
                }
                min = i.keyAt(j);
            }
        }
        if (i.numberOfCases() != i.keysLength()) {
            fail("Lookupswitch keys[] length does not match number of cases");
        }
    }

    /**
     * Typechecks a TableSwitch instruction.
     * @param i the TableSwitch instruction to be verified
     */
    @Override
    public void visitTableSwitch(TableSwitch i) {
        assertKind(i, CiKind.Illegal);
        assertKind(i.value(), CiKind.Int);
    }

    /**
     * Typechecks a Throw instruction.
     * @param i the Throw instruction to be verified
     */
    @Override
    public void visitThrow(Throw i) {
        assertKind(i, CiKind.Illegal);
        assertKind(i.exception(), CiKind.Object);
        assertInstanceType(i.exception().declaredType());
        assertInstanceType(i.exception().exactType());
    }

    /**
     * Typechecks an Instrinsic instruction.
     * @param i the Instrinsic instruction
     */
    @Override
    public void visitIntrinsic(Intrinsic i) {
        if (i.isIllegal()) {
            fail("Result type of Intrinsic instruction must not be Illegal");
        }
        // TODO: use the signature from the C1XIntrinsic to check arguments
    }

    /**
     * Typechecks an Invoke instruction.
     * @param i the Invoke instruction
     */
    @Override
    public void visitInvoke(Invoke i) {
        assertNonNull(i.target(), "Target of invoke cannot be null");
        assertNonNull(i.stateBefore(), "Invoke must have FrameState");
        RiSignature signatureType = i.target().signature();
        assertKind(i, signatureType.returnKind().stackKind());
        Value[] args = i.arguments();
        if (i.isStatic()) {
            // typecheck a static call (i.e. there should be no receiver)
            if (i.opcode() != Bytecodes.INVOKESTATIC) {
                fail("Static Invoke must use InvokeStatic bytecode");
            }
            int argSize = signatureType.argumentSlots(false);
            if (argSize != args.length) {
                fail("Size of Arguments does not match invoke signature");
            }
            typeCheckArguments(null, args, signatureType);
        } else {
            // typecheck a non-static call (i.e. there should be a receiver)
            assertNonNull(i.receiver(), "Receiver object should not be null");
            if (i.opcode() != Bytecodes.INVOKEVIRTUAL && i.opcode() != Bytecodes.INVOKESPECIAL && i.opcode() != Bytecodes.INVOKEINTERFACE) {
                fail("Nonstatic Invoke must use proper invoke bytecode");
            }
            typeCheckArguments(i.target().holder().kind(), args, signatureType);
        }
    }

    private void typeCheckArguments(CiKind receiverKind, Value[] args, RiSignature signatureType) {
        int argSize = signatureType.argumentSlots(receiverKind != null);
        if (argSize != args.length) {
            fail("Size of arguments does not match invoke signature");
        }
        int k = 0; // loops over signature positions
        int j = 0; // loops over argument positions
        for (; j < argSize; j++) {
            if (j == 0 && receiverKind != null) {
                assertKind(args[j], receiverKind);
            } else {
                CiKind kind = signatureType.argumentKindAt(k);
                assertKind(args[j], kind.stackKind());
                if (kind.sizeInSlots() == 2) {
                    assertNull(args[j + 1], "Second slot of a double operand must be null");
                    j = j + 1;
                }
                k = k + 1;
            }
        }
    }

    /**
     *  Typechecks the NewMultiArray instruction.
     *
     *  @param i the NewMultiArray instruction to check
     */
    @Override
    public void visitNewMultiArray(NewMultiArray i) {
        final Value[] dimensions = i.dimensions();
        assertKind(i, CiKind.Object);

        if (dimensions.length <= 1) {
            fail("Value NewMultiArray must have more than 1 dimension");
        }

        for (Value dim : dimensions) {
            assertKind(dim, CiKind.Int);
        }
    }

    /**
     * Typechecks the NewObjectArray instruction.
     * @param i the NewObjectArray instruction to be checked
     */
    @Override
    public void visitNewObjectArray(NewObjectArray i) {
        assertKind(i, CiKind.Object);
        assertKind(i.length(), CiKind.Int);
        assertNotPrimitive(i.elementClass());
    }

    /**
     *  Typechecks the NewTypeArray instruction.
     *  @param i the NewTypeArray instruction to check
     */
    @Override
    public void visitNewTypeArray(NewTypeArray i) {
        assertKind(i, CiKind.Object);
        assertKind(i.length(), CiKind.Int);
        assertPrimitive(i.elementKind());
    }

    /**
     * Typechecks the NewTypeArray instruction.
     * @param i the NewInstance instruction to check
     */
    @Override
    public void visitNewInstance(NewInstance i) {
        assertKind(i, CiKind.Object);
        assertInstanceType(i.instanceClass());
    }

    /**
     * Typechecks the CheckCast instruction.
     * @param i the CheckCast instruction to check
     */
    @Override
    public void visitCheckCast(CheckCast i) {
        assertKind(i, CiKind.Object);
        assertKind(i.object(), CiKind.Object);
        if (i.targetClass().kind() != CiKind.Object) {
            fail("Target class must be of type Object in a CheckCast instruction");
        }
    }

    /**
     * Typechecks the UnsafeGetObject instruction.
     * @param i the UnsafeGetObject instruction to check
     */
    @Override
    public void visitUnsafeGetObject(UnsafeGetObject i) {
        assertKind(i.object(), CiKind.Object);
    }

    /**
     * Typechecks the UnsafePrefetchRead instruction.
     * @param i the UnsafePrefetchRead instruction to check
     */
    @Override
    public void visitUnsafePrefetchRead(UnsafePrefetchRead i) {
        assertKind(i.object(), CiKind.Object);
    }

    /**
     * Typechecks the UnsafePrefetchWrite instruction.
     * @param i the UnsafePrefetchWrite instruction to check
     */
    @Override
    public void visitUnsafePrefetchWrite(UnsafePrefetchWrite i) {
        assertKind(i.object(), CiKind.Object);
    }

    /**
     * Typechecks the UnsafePutObject instruction.
     * @param i the UnsafePutObject instruction to check
     */
    @Override
    public void visitUnsafePutObject(UnsafePutObject i) {
        assertKind(i.object(), CiKind.Object);
        assertKind(i.offset(), CiKind.Int);
    }

    /**
     * Typechecks the UnsafeGetRaw instruction.
     * @param i the UnsafeGetRaw instruction to check
     */
    @Override
    public void visitUnsafeGetRaw(UnsafeGetRaw i) {
        if (i.base() != null) {
            assertKind(i.base(), CiKind.Long);
        }
        if (i.index() != null) {
            assertKind(i.index(), CiKind.Int);
        }
    }

    /**
     * Typechecks the UnsafePutRaw instruction.
     * @param i the UnsafePutRaw instruction to check
     */
    @Override
    public void visitUnsafePutRaw(UnsafePutRaw i) {
        if (i.base() != null) {
            assertKind(i.base(), CiKind.Long);
        }
        if (i.index() != null) {
            assertKind(i.index(), CiKind.Int);
        }
    }

    @Override
    public void visitIfInstanceOf(IfInstanceOf i) {
        unimplented(i);
    }

    @Override
    public void visitInfopoint(Infopoint i) {
    }

    private void checkPointerOpOffsetOrIndex(Value value) {
        if (!value.kind.isInt() && !Util.archKindsEqual(value.kind, CiKind.Word)) {
            fail("Type mismatch: " + value.kind + " should be of type " + CiKind.Int + " or " + CiKind.Word);
        }
    }

    @Override
    public void visitLoadPointer(LoadPointer i) {
        assertKind(i.pointer(), CiKind.Word);
        if (i.displacement() == null) {
            checkPointerOpOffsetOrIndex(i.offset());
        } else {
            checkPointerOpOffsetOrIndex(i.index());
            assertKind(i.displacement(), CiKind.Int);
        }
    }

    @Override
    public void visitLoadRegister(LoadRegister i) {
        assertNonNull(i.register, "Register must not be null");
    }

    @Override
    public void visitLoadStackAddress(AllocateStackVariable i) {
        assertNonNull(i.value(), "Value must not be null");
    }

    @Override
    public void visitNativeCall(NativeCall i) {
        assertNonNull(i.address(), "Address of native call cannot be null");
        assertNonNull(i.stateBefore(), "Invoke must have FrameState");
        RiSignature signatureType = i.signature;
        assertKind(i, signatureType.returnKind().stackKind());
        Value[] args = i.arguments;
        int argSize = signatureType.argumentSlots(false);
        if (argSize != args.length) {
            fail("Size of Arguments does not match invoke signature");
        }
        typeCheckArguments(null, args, signatureType);
    }

    @Override
    public void visitPause(Pause i) {
    }

    @Override
    public void visitBreakpointTrap(BreakpointTrap i) {
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
    }

    @Override
    public void visitSignificantBit(SignificantBitOp i) {
        assertNonNull(i.value(), "Value must not be null");
    }

    @Override
    public void visitStackAllocate(StackAllocate i) {
        if (!i.size().isConstant()) {
            fail("Size operand of StackAllocate instruction must be constant");
        }
    }

    @Override
    public void visitStorePointer(StorePointer i) {
        assertKind(i.pointer(), CiKind.Word);
        if (i.displacement() == null) {
            checkPointerOpOffsetOrIndex(i.offset());
        } else {
            checkPointerOpOffsetOrIndex(i.index());
            assertKind(i.displacement(), CiKind.Int);
        }
    }

    @Override
    public void visitArrayCopy(ArrayCopy arrayCopy) {
        throw Util.unimplemented();
    }

    @Override
    public void visitBoundsCheck(BoundsCheck boundsCheck) {
        throw Util.unimplemented();
    }

    @Override
    public void visitStoreRegister(StoreRegister i) {
    }

    @Override
    public void visitUnsafeCast(UnsafeCast i) {
    }

    @Override
    public void visitUnsignedCompareOp(UnsignedCompareOp i) {
        Value x = i.x();
        Value y = i.y();

        switch (i.opcode) {
            case Bytecodes.UCMP:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Int);
                assertKind(y, CiKind.Int);
                break;
            case Bytecodes.UWCMP:
                assertKind(i, CiKind.Int);
                assertKind(x, CiKind.Word);
                assertKind(y, CiKind.Word);
                break;
            default:
                fail("Illegal UnsignedCompareOp opcode: " + Bytecodes.nameOf(i.opcode));
        }
    }

    @Override
    public void visitMonitorAddress(MonitorAddress i) {
    }

    @Override
    public void visitMemoryBarrier(MemoryBarrier i) {
    }

    private void assertKind(Value i, CiKind kind) {
        assertNonNull(i, "Value should not be null");
        if (!Util.archKindsEqual(i.kind, kind)) {
            fail("Type mismatch: " + i + " should be of type " + kind);
        }
    }

    private void assertLegal(Value i) {
        assertNonNull(i, "Value should not be null");
        if (i.kind == CiKind.Illegal) {
            fail("Type mismatch: " + i + " should not be illegal");
        }
    }

    private void assertInstanceType(RiType riType) {
        if (riType != null && riType.isResolved()) {
            if (riType.isArrayClass() || riType.isInterface() || riType.kind().isPrimitive()) {
                fail("RiType " + riType + " must be an instance class");
            }
        }
    }

    private void assertArrayType(RiType riType) {
        if (riType != null && riType.isResolved()) {
            if (!riType.isArrayClass()) {
                fail("RiType " + riType + " must be an array class");
            }
        }
    }

    private void assertNotPrimitive(RiType riType) {
        if (riType != null && riType.isResolved()) {
            if (riType.kind().isPrimitive()) {
                fail("RiType " + riType + " must not be a primitive");
            }
        }
    }

    private void assertPrimitive(CiKind kind) {
        if (!kind.isPrimitive()) {
            fail("RiType " + kind + " must be a primitive");
        }
    }

    private void assertNonNull(Object object, String msg) {
        if (object == null) {
            fail(msg);
        }
    }

    private void assertNull(Object object, String msg) {
        if (object != null) {
            fail(msg);
        }
    }

    private void fail(String msg) {
        String location = "";
        try {
            location = "B" + currentBlock.blockID + ", bci " + currentInstruction.bci() + ": ";
        } catch (Throwable e) {
        }
        throw new IRCheckException(location + "IR error " + phase + ": " + msg);
    }

    private class BasicValueChecker implements ValueClosure {

        public Value apply(Value i) {
            if (i.hasSubst()) {
                fail("instruction has unresolved substitution " + i + " -> " + i.subst());
            }
            if (!i.isDeadPhi()) {
                // legal instructions must have legal instructions as inputs
                LegalValueChecker legalChecker = new LegalValueChecker(i);
                i.inputValuesDo(legalChecker);
                if (i instanceof Phi) {
                    // phis are special, once again
                    checkPhi((Phi) i);
                }
            }
            if (i instanceof BlockEnd) {
                checkBlockEnd((BlockEnd) i);
            }
            return i;
        }
    }

    private class LegalValueChecker implements ValueClosure {
        private final Value i;

        public LegalValueChecker(Value i) {
            this.i = i;
        }

        public Value apply(Value x) {
            assertNonNull(x, "must have input value");
            if (x.isIllegal()) {
                fail("Value has illegal input value " + i + " <- " + x);
            }
            return x;
        }
    }
}
