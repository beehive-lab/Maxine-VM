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
package com.sun.c1x.util;

import static com.sun.c1x.ir.BlockBegin.BlockFlag.*;

import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ir.BlockBegin.*;
import com.sun.c1x.value.*;

/**
 * An {@link InstructionVisitor} for {@linkplain #printInstruction(Instruction) printing}
 * an {@link Instruction} as an expression or statement.
 *
 * @author Doug Simon
 */
public class InstructionPrinter implements InstructionVisitor {
    /**
     * The columns printed in a tabulated instruction
     * {@linkplain InstructionPrinter#printInstructionListing(Instruction) listing}.
     */
    public enum InstructionLineColumn {
        /**
         * The instruction's bytecode index.
         */
        BCI(2, "bci"),

        /**
         * The instruction's use count.
         */
        USE(7, "use"),

        /**
         * The instruction as {@linkplain InstructionPrinter#valueString(com.sun.c1x.ir.Instruction) value}.
         */
        VALUE(12, "tid"),

        /**
         * The instruction formatted as an expression or statement.
         */
        INSTRUCTION(19, "instr"),

        END(60, "");

        final int _position;
        final String _label;

        private InstructionLineColumn(int position, String label) {
            _position = position;
            _label = label;
        }

        /**
         * Prints this column's label to a given stream after padding the stream with '_' characters
         * until its {@linkplain C1XPrintStream#position() position} is equal to this column's position.
         * @param out the print stream
         */
        public void printLabel(C1XPrintStream out) {
            out.fillTo(_position, '_');
            out.print(_label);
        }

        /**
         * Prints space characters to a given stream until its {@linkplain C1XPrintStream#position() position}
         * is equal to this column's position.
         * @param out the print stream
         */
        public void advance(C1XPrintStream out) {
            out.fillTo(_position, ' ');
        }
    }

    private final C1XPrintStream _out;
    private final boolean _printPhis;

    public InstructionPrinter(C1XPrintStream out, boolean printPhis) {
        _out = out;
        _printPhis = printPhis;
    }

    public C1XPrintStream out() {
        return _out;
    }

    public static String binaryOperationAsString(Op2 i) {
        return valueString(i.x()) + ' ' + Bytecodes.operator(i.opcode()) + ' ' + valueString(i.y());
    }

    public static String arrayAccessAsString(AccessIndexed indexed) {
        return valueString(indexed) + '[' + valueString(indexed.index()) + ']';
    }

    public static String fieldAccessAsString(AccessField field) {
        return valueString(field.object()) + "._" + field.offset();
    }

    /**
     * Prints a given instruction as an expression or statement.
     *
     * @param instruction the instruction to print
     */
    public void printInstruction(Instruction instruction) {
        instruction.accept(this);
    }

    /**
     * Prints a header for the tabulated data printed by {@link #printInstructionListing(Instruction)}.
     */
    public void printInstructionListingHeader() {
        InstructionLineColumn.BCI.printLabel(_out);
        InstructionLineColumn.USE.printLabel(_out);
        InstructionLineColumn.VALUE.printLabel(_out);
        InstructionLineColumn.INSTRUCTION.printLabel(_out);
        InstructionLineColumn.END.printLabel(_out);
        _out.println();
    }

    /**
     * Prints an instruction listing on one line. The instruction listing is composed of the
     * columns specified by {@link InstructionLineColumn}.
     *
     * @param instruction the instruction to print
     */
    public void printInstructionListing(Instruction instruction) {
        if (instruction.isPinned()) {
            _out.print('.');
        }
        InstructionLineColumn.BCI.advance(_out);
        _out.print(instruction.bci());

        InstructionLineColumn.USE.advance(_out);
        _out.print("0");

        InstructionLineColumn.VALUE.advance(_out);
        _out.print(valueString(instruction));

        InstructionLineColumn.INSTRUCTION.advance(_out);
        printInstruction(instruction);

        _out.println();
    }

    public void visitArithmeticOp(ArithmeticOp arithOp) {
        _out.print(binaryOperationAsString(arithOp));
    }

    public void visitArrayLength(ArrayLength i) {
        _out.print(valueString(i.array()) + ".length");
    }

    public void visitBase(Base i) {
        _out.print("std entry B" + i.standardEntry().blockID());
        if (i.successors().size() > 1) {
          _out.print(" osr entry B" + i.osrEntry().blockID());
        }
    }

    public void visitBlockBegin(BlockBegin block) {
        // print block id
        BlockEnd end = block.end();
        _out.print("B" + block.blockID() + " ");

        // print flags
        StringBuilder sb = new StringBuilder(8);
        if (block.checkBlockFlag(BlockFlag.StandardEntry)) {
            sb.append('S');
        }
        if (block.checkBlockFlag(OsrEntry)) {
            sb.append('O');
        }
        if (block.checkBlockFlag(ExceptionEntry)) {
            sb.append('E');
        }
        if (block.checkBlockFlag(SubroutineEntry)) {
            sb.append('s');
        }
        if (block.checkBlockFlag(ParserLoopHeader)) {
            sb.append("LH");
        }
        if (block.checkBlockFlag(BackwardBranchTarget)) {
            sb.append('b');
        }
        if (block.checkBlockFlag(WasVisited)) {
            sb.append('V');
        }
        if (sb.length() != 0) {
            _out.print('(' + sb.toString() + ')');
        }

        // print block bci range
        _out.print('[' + block.bci() + ", " + (end == null ? -1 : end.bci()) + ']');

        // print block successors
        if (end != null && end.successors().size() > 0) {
            _out.print(" .");
            for (BlockBegin successor : end.successors()) {
                _out.print(" B" + successor.blockID());
            }
        }
        // print exception handlers
        if (!block.exceptionHandlers().isEmpty()) {
            _out.print(" (xhandlers");
            for (BlockBegin handler : block.exceptionHandlerBlocks()) {
                _out.print(" B" + handler.blockID());
            }
            _out.print(')');
        }

        // print dominator block
        if (block.dominator() != null) {
            _out.print(" dom B" + block.dominator().blockID());
        }

        // print predecessors
        if (!block.predecessors().isEmpty()) {
            _out.print(" pred:");
            for (BlockBegin pred : block.predecessors()) {
                _out.print(" B" + pred.blockID());
            }
        }

        if (!_printPhis) {
            return;
        }

        // print phi functions
        boolean hasPhisInLocals = false;
        boolean hasPhisOnStack = false;

        if (end != null && end.state() != null) {
            ValueStack state = block.state();

            int i = 0;
            while (!hasPhisOnStack && i < state.stackSize()) {
                Instruction value = state.stackAt(i);
                hasPhisOnStack = isPhiAtBlock(value, block);
                i += value.type().size();
            }

            do {
                for (i = 0; !hasPhisInLocals && i < state.localsSize();) {
                    Instruction value = state.localAt(i);
                    hasPhisInLocals = isPhiAtBlock(value, block);
                    // also ignore illegal HiWords
                    if (value != null && !value.type().isIllegal()) {
                        i += value.type().size();
                    } else {
                        i++;
                    }
                }
                state = state.scope().callerState();
            } while (state != null);
        }

        // print values in locals
        if (hasPhisInLocals) {
            _out.println();
            _out.println("Locals:");

            ValueStack state = block.state();
            do {
                int i = 0;
                while (i < state.localsSize()) {
                    Instruction value = state.localAt(i);
                    if (value != null) {
                        _out.println(localVariableOrStackSlotAsString(i, value, block));
                        // also ignore illegal HiWords
                        i += value.type().isIllegal() ? 1 : value.type().size();
                    } else {
                        i++;
                    }
                }
                _out.println();
                state = state.scope().callerState();
            } while (state != null);
        }

        // print values on stack
        if (hasPhisOnStack) {
            _out.println();
            _out.println("Stack:");
            int i = 0;
            while (i < block.state().stackSize()) {
                Instruction value = block.state().stackAt(i);
                if (value != null) {
                    _out.println(localVariableOrStackSlotAsString(i, value, block));
                    i += value.type().size();
                } else {
                    i++;
                }
            }
        }
    }

    /**
     * Formats a given instruction as local variable or stack slot value. If the instruction is a phi defined at a given
     * block, its {@linkplain Phi#operand() operands} are appended to the returned string.
     *
     * @param index the index of the local variable or stack slot
     * @param value the value of the local variable or stack slot
     * @param block if {@code value} is a phi, then its operands are formatted if {@code block} is its
     *            {@linkplain Phi#block() join point}
     * @return the instruction representation as a string
     */
    public static String localVariableOrStackSlotAsString(int index, Instruction value, BlockBegin block) {
        StringBuilder sb = new StringBuilder(30);
        sb.append(String.format("%2d  %s", index, valueString(value)));
        if (value instanceof Phi) {
            Phi phi = (Phi) value;
            // print phi operands
            if (phi.block() == block) {
                sb.append(" [");
                for (int j = 0; j < phi.operandCount(); j++) {
                    sb.append(' ');
                    Instruction operand = phi.operandAt(j);
                    if (operand != null) {
                        sb.append(valueString(operand));
                    } else {
                        sb.append("NULL");
                    }
                }
                sb.append("] ");
            }
        }
        if (value != value.subst()) {
            sb.append("alias ").append(valueString(value.subst()));
        }
        return sb.toString();
    }

    /**
     * Determines if a given instruction is a phi whose {@linkplain Phi#block() join block} is a given block.
     *
     * @param value the instruction to test
     * @param block the block that may be the join block of {@code value} if {@code value} is a phi
     * @return {@code true} if {@code value} is a phi and its join block is {@code block}
     */
    public static boolean isPhiAtBlock(Instruction value, BlockBegin block) {
        return value instanceof Phi && ((Phi) value).block() == block;
    }

    public void visitCheckCast(CheckCast checkcast) {
        _out.print("checkcast(" + valueString(checkcast.object()) + ") " + checkcast.targetClass().name());
    }

    public void visitCompareOp(CompareOp compareOp) {
        _out.print(binaryOperationAsString(compareOp));
    }

    public void visitConstant(Constant constant) {
        ValueType type = constant.type();
        if (type.isPrimitive()) {
            _out.print(type.asConstant().valueString());
        } else if (type.isObject()) {
            // TODO: complete ValueType hierarchy with InstanceType, ArrayType, etc...
            _out.print("<object: TODO>");
        } else if (type.isJsr()) {
            _out.print("bci:" + type.asConstant().valueString());
        } else {
            _out.print("???");
        }
    }

    public void visitConvert(Convert convert) {
        _out.print(Bytecodes.name(convert.opcode() + '(') + valueString(convert.value()) + ')');
    }

    public void visitExceptionObject(ExceptionObject i) {
        _out.print("incomeing exception");
    }

    public void visitGoto(Goto go2) {
        _out.print("goto B" + go2.defaultSuccessor().blockID());
        if (go2.isSafepoint()) {
            _out.print(" (safepoint)");
        }
    }

    public void visitIf(If i) {
        _out.print("if " + valueString(i.x()) + ' ' + i.condition().name() + ' ' + valueString(i.y()) +
                   " then B" + i.successors().get(0).blockID() + " else B" + i.successors().get(1).blockID());
        if (i.isSafepoint()) {
            _out.print(" (safepoint)");
        }
    }

    public void visitIfInstanceOf(IfInstanceOf i) {
        _out.print("<IfInstanceOf>");
    }

    public void visitIfOp(IfOp i) {
        _out.print(valueString(i.x()) + ' ' + i.condition().name() + ' ' + valueString(i.y()) +
                   " ? " + valueString(i.trueValue()) + " : " + valueString(i.falseValue()));
    }

    public void visitInstanceOf(InstanceOf i) {
        _out.print("instanceof(" + valueString(i.object()) + ") " + i.targetClass().name());
    }

    public void visitIntrinsic(Intrinsic intrinsic) {
        _out.print(intrinsic.intrinsic().simpleClassName() + '.' + intrinsic.intrinsic().name() + '(');
        for (int i = 0; i < intrinsic.arguments().length; i++) {
          if (i > 0) {
              _out.print(", ");
          }
          _out.print(valueString(intrinsic.arguments()[i]));
        }
        _out.print(')');
    }

    public void visitInvoke(Invoke invoke) {
        if (invoke.object() != null) {
            _out.print(valueString(invoke.object()) + '.');
          }

          _out.print(Bytecodes.name(invoke.opcode()) + '(');
          Instruction[] arguments = invoke.arguments();
          for (int i = 0; i < arguments.length; i++) {
              if (i > 0) {
                  _out.print(", ");
              }
              _out.print(valueString(arguments[i]));
          }
          _out.println(')');
          InstructionLineColumn.INSTRUCTION.advance(_out);
          CiMethod target = invoke.target();
          _out.print(target.holder().name() + '.' + target.name() + target.signatureType().asString());
    }

    public void visitLoadField(LoadField i) {
        _out.print(fieldAccessAsString(i) + " (" + i.field().type().basicType()._char + ")");
    }

    public void visitLoadIndexed(LoadIndexed load) {
        _out.print(arrayAccessAsString(load) + " (" + load.type().tchar() + ')');
    }

    public void visitLocal(Local local) {
        _out.print("local[index " + local.javaIndex() + ']');
    }

    public void visitLogicOp(LogicOp logicOp) {
        _out.print(binaryOperationAsString(logicOp));
    }

    public void visitLookupSwitch(LookupSwitch lswitch) {
        _out.print("lookupswitch ");
        if (lswitch.isSafepoint()) {
            _out.print("(safepoint) ");
        }
        _out.println(valueString(lswitch.value()));
        int l = lswitch.numberOfCases();
        for (int i = 0; i < l; i++) {
            InstructionLineColumn.INSTRUCTION.advance(_out);
            _out.printf("case %5d: B%d%n", lswitch.keyAt(i), lswitch.successors().get(i).blockID());
        }
        InstructionLineColumn.INSTRUCTION.advance(_out);
        _out.print("default   : B" + lswitch.defaultSuccessor().blockID());

    }

    public void visitMonitorEnter(MonitorEnter monitorenter) {
        _out.print("enter monitor[" + monitorenter.lockNumber() + "](" + valueString(monitorenter.object()) + ')');
    }

    public void visitMonitorExit(MonitorExit monitorexit) {
        _out.print("exit monitor[" + monitorexit.lockNumber() + "](" + valueString(monitorexit.object()) + ')');
    }

    public void visitNegateOp(NegateOp negate) {
        _out.print('-' + valueString(negate));
    }

    public void visitNewInstance(NewInstance newInstance) {
        _out.print("new instance " + newInstance.instanceClass().name());
    }

    public void visitNewMultiArray(NewMultiArray newMultiArray) {
        _out.print("new multi array [");
        final Instruction[] dimensions = newMultiArray.dimensions();
        for (int i = 0; i < dimensions.length; i++) {
          if (i > 0) {
              _out.print(", ");
          }
          _out.print(valueString(dimensions[i]));
        }
        _out.print("] " + newMultiArray.elementType().name());
    }

    public void visitNewObjectArray(NewObjectArray newObjectArray) {
        _out.print("new object array [" + newObjectArray.length() + "] " + newObjectArray.elementClass().name());
    }

    public void visitNewTypeArray(NewTypeArray newTypeArray) {
        _out.print("new " + newTypeArray.elementType().name() + " array [" + newTypeArray.length() + ']');
    }

    public void visitNullCheck(NullCheck i) {
        _out.print("null_check(" + valueString(i.object()) + ')');
        if (!i.canTrap()) {
          _out.print(" (eliminated)");
        }
    }

    public void visitOsrEntry(OsrEntry osrEntry) {
        _out.print("osr entry");
    }

    public void visitPhi(Phi phi) {
        _out.print("phi function");
    }

    public void visitProfileCall(ProfileCall profileCall) {
        final CiMethod method = profileCall.method();
        _out.print("profile " + valueString(profileCall.object()) + method.holder().name() + '.' + method.name());
        if (profileCall.knownHolder() != null) {
          _out.print(", " + profileCall.knownHolder().name());
        }
        _out.print(')');
    }

    public void visitProfileCounter(ProfileCounter i) {
        // TODO: Recognize interpreter invocation counter specially
        _out.print("counter [" + valueString(i.mdo()) + " + " + i.offset() + "] += " + i.increment());
    }

    public void visitReturn(Return ret) {
        if (ret.result() == null) {
            _out.print("return");
        } else {
            _out.print(ret.type().tchar() + "return " + valueString(ret.result()));
        }
    }

    public void visitRoundFP(RoundFP i) {
        _out.print("roundfp " + valueString(i.value()));
    }

    public void visitShiftOp(ShiftOp shiftOp) {
        _out.print(binaryOperationAsString(shiftOp));
    }

    public void visitStoreField(StoreField store) {
        _out.print(fieldAccessAsString(store) + " := " + valueString(store.value()) + " (" + store.field().type().basicType()._char + ')');
    }

    public void visitStoreIndexed(StoreIndexed store) {
        _out.print(arrayAccessAsString(store) + " := " + valueString(store.value()) + " (" + store.type().tchar() + ')');
    }

    public void visitTableSwitch(TableSwitch tswitch) {
        _out.print("tableswitch ");

        // output()->print("tableswitch ");
       if (tswitch.isSafepoint()) {
           _out.print("(safepoint) ");
       }
       _out.println(valueString(tswitch.value()));
        int l = tswitch.numberOfCases();
        for (int i = 0; i < l; i++) {
            InstructionLineColumn.INSTRUCTION.advance(_out);
            _out.printf("case %5d: B%d%n", tswitch.lowKey() + i, tswitch.successors().get(i).blockID());
        }
        InstructionLineColumn.INSTRUCTION.advance(_out);
        _out.print("default   : B" + tswitch.defaultSuccessor().blockID());
    }

    public void visitThrow(Throw i) {
        _out.print("throw " + valueString(i.exception()));
    }

    public void visitUnsafeGetObject(UnsafeGetObject unsafe) {
        _out.print("UnsafeGetObject.(" + valueString(unsafe.object()) + ", " + unsafe.offset() + ')');
    }

    public void visitUnsafeGetRaw(UnsafeGetRaw unsafe) {
        _out.print("UnsafeGetRaw.(base " + valueString(unsafe.base()));
        if (unsafe.hasIndex()) {
            _out.print(", index " + unsafe.index() + ", log2_scale " + unsafe.log2Scale());
        }
        _out.print(')');
    }

    public void visitUnsafePrefetchRead(UnsafePrefetchRead unsafe) {
        _out.print("UnsafePrefetchRead.(" + valueString(unsafe.object()) + ", " + unsafe.offset() + ')');
    }

    public void visitUnsafePrefetchWrite(UnsafePrefetchWrite unsafe) {
        _out.print("UnsafePrefetchWrite.(" + valueString(unsafe.object()) + ", " + unsafe.offset() + ')');
    }

    public void visitUnsafePutObject(UnsafePutObject unsafe) {
        _out.print("UnsafePutObject.(" + valueString(unsafe.object()) + ", " + unsafe.offset() +
                        ", value " + valueString(unsafe.value()) + ')');
    }

    public void visitUnsafePutRaw(UnsafePutRaw unsafe) {
        _out.print("UnsafePutRaw.(base " + valueString(unsafe.base()));
        if (unsafe.hasIndex()) {
            _out.print(", index " + unsafe.index() + ", log2_scale " + unsafe.log2Scale());
        }
        _out.print(", value " + valueString(unsafe.value()) + ')');
    }

    /**
     * Converts a given instruction to a value string. The representation of an instruction as
     * a value is formed by concatenating the {@linkplain com.sun.c1x.value.ValueType#tchar() character} denoting its
     * {@linkplain com.sun.c1x.ir.Instruction#type() type} and its {@linkplain com.sun.c1x.ir.Instruction#id()}. For example,
     * "i13".
     *
     * @param value the instruction to convert to a value string. If {@code value == null}, then "null" is returned.
     */
    public static String valueString(Instruction value) {
        return value == null ? "null" : "" + value.type().tchar() + value.id();
    }

}
