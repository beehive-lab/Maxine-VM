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
package com.sun.c1x.debug;

import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.ci.CiMethod;
import static com.sun.c1x.debug.InstructionPrinter.InstructionLineColumn.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.ConstType;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * An {@link com.sun.c1x.ir.InstructionVisitor} for {@linkplain #printInstruction(Instruction) printing}
 * an {@link Instruction} as an expression or statement.
 *
 * @author Doug Simon
 */
public class InstructionPrinter extends InstructionVisitor {
    /**
     * Formats a given instruction as value is a {@linkplain com.sun.c1x.value.ValueStack frame state}. If the instruction is a phi defined at a given
     * block, its {@linkplain com.sun.c1x.ir.Phi#operand() operands} are appended to the returned string.
     *
     * @param index the index of the value in the frame state
     * @param value the frame state value
     * @param block if {@code value} is a phi, then its operands are formatted if {@code block} is its
     *            {@linkplain com.sun.c1x.ir.Phi#block() join point}
     * @return the instruction representation as a string
     */
    public static String stateString(int index, Instruction value, BlockBegin block) {
        StringBuilder sb = new StringBuilder(30);
        sb.append(String.format("%2d  %s", index, Instruction.valueString(value)));
        if (value instanceof Phi) {
            Phi phi = (Phi) value;
            // print phi operands
            if (phi.block() == block) {
                sb.append(" [");
                for (int j = 0; j < phi.operandCount(); j++) {
                    sb.append(' ');
                    Instruction operand = phi.operandAt(j);
                    if (operand != null) {
                        sb.append(Instruction.valueString(operand));
                    } else {
                        sb.append("NULL");
                    }
                }
                sb.append("] ");
            }
        }
        if (value != null && value != value.subst()) {
            sb.append("alias ").append(Instruction.valueString(value.subst()));
        }
        return sb.toString();
    }

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
         * The instruction as a {@linkplain Instruction#valueString(com.sun.c1x.ir.Instruction) value}.
         */
        VALUE(12, "tid"),

        /**
         * The instruction formatted as an expression or statement.
         */
        INSTRUCTION(19, "instr"),

        END(60, "");

        final int position;
        final String label;

        private InstructionLineColumn(int position, String label) {
            this.position = position;
            this.label = label;
        }

        /**
         * Prints this column's label to a given stream after padding the stream with '_' characters
         * until its {@linkplain LogStream#position() position} is equal to this column's position.
         * @param out the print stream
         */
        public void printLabel(LogStream out) {
            out.fillTo(position + out.indentation(), '_');
            out.print(label);
        }

        /**
         * Prints space characters to a given stream until its {@linkplain LogStream#position() position}
         * is equal to this column's position.
         * @param out the print stream
         */
        public void advance(LogStream out) {
            out.fillTo(position + out.indentation(), ' ');
        }
    }

    private final LogStream out;
    private final boolean printPhis;

    public InstructionPrinter(LogStream out, boolean printPhis) {
        this.out = out;
        this.printPhis = printPhis;
    }

    public LogStream out() {
        return out;
    }

    /**
     * Prints a given instruction as an expression or statement.
     *
     * @param instruction the instruction to print
     */
    public void printInstruction(Instruction instruction) {
        instruction.accept(this);
    }

    public void printBlock(BlockBegin block) {
        block.accept(this); // TODO: maybe we don't need to print out the whole block
    }

    /**
     * Prints a header for the tabulated data printed by {@link #printInstructionListing(Instruction)}.
     */
    public void printInstructionListingHeader() {
        BCI.printLabel(out);
        USE.printLabel(out);
        VALUE.printLabel(out);
        INSTRUCTION.printLabel(out);
        END.printLabel(out);
        out.println();
    }

    /**
     * Prints an instruction listing on one line. The instruction listing is composed of the
     * columns specified by {@link InstructionLineColumn}.
     *
     * @param instruction the instruction to print
     */
    public void printInstructionListing(Instruction instruction) {
        if (instruction.isPinned()) {
            out.print('.');
        }

        int indentation = out.indentation();
        out.fillTo(BCI.position + indentation, ' ').
             print(instruction.bci()).
             fillTo(USE.position + indentation, ' ').
             print("0").
             fillTo(VALUE.position + indentation, ' ').
             print(instruction).
             fillTo(INSTRUCTION.position + indentation, ' ');
        printInstruction(instruction);
        out.println();
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp arithOp) {
        out.print(arithOp.x()).
             print(' ').
             print(Bytecodes.operator(arithOp.opcode())).
             print(' ').
             print(arithOp.y());
    }

    @Override
    public void visitArrayLength(ArrayLength i) {
        out.print(i.array()).print(".length");
    }

    @Override
    public void visitBase(Base i) {
        out.print("std entry B").print(i.standardEntry().blockID);
        if (i.successors().size() > 1) {
            out.print(" osr entry B").print(i.osrEntry().blockID);
        }
    }

    @Override
    public void visitBlockBegin(BlockBegin block) {
        // print block id
        BlockEnd end = block.end();
        out.print("B").print(block.blockID).print(" ");

        // print flags
        StringBuilder sb = new StringBuilder(8);
        if (block.isStandardEntry()) {
            sb.append('S');
        }
        if (block.isOsrEntry()) {
            sb.append('O');
        }
        if (block.isExceptionEntry()) {
            sb.append('E');
        }
        if (block.isSubroutineEntry()) {
            sb.append('s');
        }
        if (block.isParserLoopHeader()) {
            sb.append("LH");
        }
        if (block.isBackwardBranchTarget()) {
            sb.append('b');
        }
        if (block.wasVisited()) {
            sb.append('V');
        }
        if (sb.length() != 0) {
            out.print('(').print(sb.toString()).print(')');
        }

        // print block bci range
        out.print('[').print(block.bci()).print(", ").print(end == null ? -1 : end.bci()).print(']');

        // print block successors
        if (end != null && end.successors().size() > 0) {
            out.print(" .");
            for (BlockBegin successor : end.successors()) {
                out.print(" B").print(successor.blockID);
            }
        }
        // print exception handlers
        if (!block.exceptionHandlers().isEmpty()) {
            out.print(" (xhandlers");
            for (BlockBegin handler : block.exceptionHandlerBlocks()) {
                out.print(" B").print(handler.blockID);
            }
            out.print(')');
        }

        // print dominator block
        if (block.dominator() != null) {
            out.print(" dom B").print(block.dominator().blockID);
        }

        // print predecessors
        if (!block.predecessors().isEmpty()) {
            out.print(" pred:");
            for (BlockBegin pred : block.predecessors()) {
                out.print(" B").print(pred.blockID);
            }
        }

        if (!printPhis) {
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
            out.println();
            out.println("Locals:");

            ValueStack state = block.state();
            do {
                int i = 0;
                while (i < state.localsSize()) {
                    Instruction value = state.localAt(i);
                    if (value != null) {
                        out.println(stateString(i, value, block));
                        // also ignore illegal HiWords
                        i += value.type().isIllegal() ? 1 : value.type().size();
                    } else {
                        i++;
                    }
                }
                out.println();
                state = state.scope().callerState();
            } while (state != null);
        }

        // print values on stack
        if (hasPhisOnStack) {
            out.println();
            out.println("Stack:");
            int i = 0;
            while (i < block.state().stackSize()) {
                Instruction value = block.state().stackAt(i);
                if (value != null) {
                    out.println(stateString(i, value, block));
                    i += value.type().size();
                } else {
                    i++;
                }
            }
        }
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

    @Override
    public void visitCheckCast(CheckCast checkcast) {
        out.print("checkcast(").
             print(checkcast.object()).
             print(") ").
             print(checkcast.targetClass().name());
    }

    @Override
    public void visitCompareOp(CompareOp compareOp) {
        out.print(compareOp.x()).
             print(' ').
             print(Bytecodes.operator(compareOp.opcode())).
             print(' ').
             print(compareOp.y());
    }

    @Override
    public void visitConstant(Constant constant) {
        ValueType type = constant.type();
        if (type == ConstType.NULL_OBJECT) {
            out.print("null");
        } else if (type.isPrimitive()) {
            out.print(type.asConstant().valueString());
        } else if (type.isObject()) {
            Object object = type.asConstant().asObject();
            if (object instanceof String) {
                out.print('"').print(object.toString()).print('"');
            } else {
                out.print("<object: ").print(object.getClass().getName()).print('@').print(System.identityHashCode(object)).print('>');
            }
        } else if (type.isJsr()) {
            out.print("bci:").print(type.asConstant().valueString());
        } else {
            out.print("???");
        }
    }

    @Override
    public void visitConvert(Convert convert) {
        out.print(Bytecodes.name(convert.opcode())).print('(').print(convert.value()).print(')');
    }

    @Override
    public void visitExceptionObject(ExceptionObject i) {
        out.print("incoming exception");
    }

    @Override
    public void visitGoto(Goto go2) {
        out.print("goto B").print(go2.defaultSuccessor().blockID);
        if (go2.isSafepoint()) {
            out.print(" (safepoint)");
        }
    }

    @Override
    public void visitIf(If i) {
        out.print("if ").
             print(i.x()).
             print(' ').
             print(i.condition().operator).
             print(' ').
             print(i.y()).
             print(" then B").
             print(i.successors().get(0).blockID).
             print(" else B").
             print(i.successors().get(1).blockID);
        if (i.isSafepoint()) {
            out.print(" (safepoint)");
        }
    }

    @Override
    public void visitIfInstanceOf(IfInstanceOf i) {
        out.print("<IfInstanceOf>");
    }

    @Override
    public void visitIfOp(IfOp i) {
        out.print(i.x()).
             print(' ').
             print(i.condition().operator).
             print(' ').
             print(i.y()).
             print(" ? ").
             print(i.trueValue()).
             print(" : ").
             print(i.falseValue());
    }

    @Override
    public void visitInstanceOf(InstanceOf i) {
        out.print("instanceof(").print(i.object()).print(") ").print(i.targetClass().name());
    }

    @Override
    public void visitIntrinsic(Intrinsic intrinsic) {
        out.print(intrinsic.intrinsic().simpleClassName()).print('.').print(intrinsic.intrinsic().name()).print('(');
        for (int i = 0; i < intrinsic.arguments().length; i++) {
          if (i > 0) {
              out.print(", ");
          }
          out.print(intrinsic.arguments()[i]);
        }
        out.print(')');
    }

    @Override
    public void visitInvoke(Invoke invoke) {
        int argStart = 0;
        if (invoke.hasReceiver()) {
            out.print(invoke.receiver()).print('.');
            argStart = 1;
          }

          out.print(Bytecodes.name(invoke.opcode())).print('(');
          Instruction[] arguments = invoke.arguments();
          for (int i = argStart; i < arguments.length; i++) {
              if (i > argStart) {
                  out.print(", ");
              }
              out.print(arguments[i]);
          }
          out.println(')');
          INSTRUCTION.advance(out);
          CiMethod target = invoke.target();
          out.print(target.holder().name()).print('.').print(target.name()).print(target.signatureType().asString());
    }

    @Override
    public void visitLoadField(LoadField i) {
        out.print(i.object()).
             print("._").
             print(i.offset()).
             print(" (").
             print(i.field().type().basicType().basicChar).
             print(")");
    }

    @Override
    public void visitLoadIndexed(LoadIndexed load) {
        out.print(load).print('[').print(load.index()).print("] (").print(load.type().tchar()).print(')');
    }

    @Override
    public void visitLocal(Local local) {
        out.print("local[index ").print(local.javaIndex()).print(']');
    }

    @Override
    public void visitLogicOp(LogicOp logicOp) {
        out.print(logicOp.x()).print(' ').print(Bytecodes.operator(logicOp.opcode())).print(' ').print(logicOp.y());
    }

    @Override
    public void visitLookupSwitch(LookupSwitch lswitch) {
        out.print("lookupswitch ");
        if (lswitch.isSafepoint()) {
            out.print("(safepoint) ");
        }
        out.println(lswitch.value());
        int l = lswitch.numberOfCases();
        for (int i = 0; i < l; i++) {
            INSTRUCTION.advance(out);
            out.printf("case %5d: B%d%n", lswitch.keyAt(i), lswitch.successors().get(i).blockID);
        }
        INSTRUCTION.advance(out);
        out.print("default   : B").print(lswitch.defaultSuccessor().blockID);

    }

    @Override
    public void visitMonitorEnter(MonitorEnter monitorenter) {
        out.print("enter monitor[").print(monitorenter.lockNumber()).print("](").print(monitorenter.object()).print(')');
    }

    @Override
    public void visitMonitorExit(MonitorExit monitorexit) {
        out.print("exit monitor[").print(monitorexit.lockNumber()).print("](").print(monitorexit.object()).print(')');
    }

    @Override
    public void visitNegateOp(NegateOp negate) {
        out.print('-').print(negate);
    }

    @Override
    public void visitNewInstance(NewInstance newInstance) {
        out.print("new instance ").print(newInstance.instanceClass().name());
    }

    @Override
    public void visitNewMultiArray(NewMultiArray newMultiArray) {
        out.print("new multi array [");
        final Instruction[] dimensions = newMultiArray.dimensions();
        for (int i = 0; i < dimensions.length; i++) {
          if (i > 0) {
              out.print(", ");
          }
          out.print(dimensions[i]);
        }
        out.print("] ").print(newMultiArray.elementType.name());
    }

    @Override
    public void visitNewObjectArray(NewObjectArray newObjectArray) {
        out.print("new object array [").print(newObjectArray.length()).print("] ").print(newObjectArray.elementClass().name());
    }

    @Override
    public void visitNewTypeArray(NewTypeArray newTypeArray) {
        out.print("new ").print(newTypeArray.elementType().name()).print(" array [").print(newTypeArray.length()).print(']');
    }

    @Override
    public void visitNullCheck(NullCheck i) {
        out.print("null_check(").print(i.object()).print(')');
        if (!i.canTrap()) {
          out.print(" (eliminated)");
        }
    }

    @Override
    public void visitOsrEntry(OsrEntry osrEntry) {
        out.print("osr entry");
    }

    @Override
    public void visitPhi(Phi phi) {
        out.print("phi function");
    }

    @Override
    public void visitProfileCall(ProfileCall profileCall) {
        final CiMethod method = profileCall.method();
        out.print("profile ").print(profileCall.object()).print(method.holder().name()).print('.').print(method.name());
        if (profileCall.knownHolder() != null) {
          out.print(", ").print(profileCall.knownHolder().name());
        }
        out.print(')');
    }

    @Override
    public void visitProfileCounter(ProfileCounter i) {
        // TODO: Recognize interpreter invocation counter specially
        out.print("counter [").print(i.mdo()).print(").print(").print(i.offset()).print("] += ").print(i.increment());
    }

    @Override
    public void visitReturn(Return ret) {
        if (ret.result() == null) {
            out.print("return");
        } else {
            out.print(ret.type().tchar()).print("return ").print(ret.result());
        }
    }

    @Override
    public void visitRoundFP(RoundFP i) {
        out.print("roundfp ").print(i.value());
    }

    @Override
    public void visitShiftOp(ShiftOp shiftOp) {
        out.print(shiftOp.x()).print(' ').print(Bytecodes.operator(shiftOp.opcode())).print(' ').print(shiftOp.y());
    }

    @Override
    public void visitStoreField(StoreField store) {
        out.print(store.object()).print("._").print(store.offset()).print(" := ").print(store.value()).print(" (").print(store.field().type().basicType().basicChar).print(')');
    }

    @Override
    public void visitStoreIndexed(StoreIndexed store) {
        out.print(store).print('[').print(store.index()).print("] := ").print(store.value()).print(" (").print(store.type().tchar()).print(')');
    }

    @Override
    public void visitTableSwitch(TableSwitch tswitch) {
        out.print("tableswitch ");
        if (tswitch.isSafepoint()) {
            out.print("(safepoint) ");
        }
        out.println(tswitch.value());
        int l = tswitch.numberOfCases();
        for (int i = 0; i < l; i++) {
            INSTRUCTION.advance(out);
            out.printf("case %5d: B%d%n", tswitch.lowKey() + i, tswitch.successors().get(i).blockID);
        }
        INSTRUCTION.advance(out);
        out.print("default   : B").print(tswitch.defaultSuccessor().blockID);
    }

    @Override
    public void visitThrow(Throw i) {
        out.print("throw ").print(i.exception());
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject unsafe) {
        out.print("UnsafeGetObject.(").print(unsafe.object()).print(", ").print(unsafe.offset()).print(')');
    }

    @Override
    public void visitUnsafeGetRaw(UnsafeGetRaw unsafe) {
        out.print("UnsafeGetRaw.(base ").print(unsafe.base());
        if (unsafe.hasIndex()) {
            out.print(", index ").print(unsafe.index()).print(", log2_scale ").print(unsafe.log2Scale());
        }
        out.print(')');
    }

    @Override
    public void visitUnsafePrefetchRead(UnsafePrefetchRead unsafe) {
        out.print("UnsafePrefetchRead.(").print(unsafe.object()).print(", ").print(unsafe.offset()).print(')');
    }

    @Override
    public void visitUnsafePrefetchWrite(UnsafePrefetchWrite unsafe) {
        out.print("UnsafePrefetchWrite.(").print(unsafe.object()).print(", ").print(unsafe.offset()).print(')');
    }

    @Override
    public void visitUnsafePutObject(UnsafePutObject unsafe) {
        out.print("UnsafePutObject.(").print(unsafe.object()).print(", ").print(unsafe.offset() +
                        ", value ").print(unsafe.value()).print(')');
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw unsafe) {
        out.print("UnsafePutRaw.(base ").print(unsafe.base());
        if (unsafe.hasIndex()) {
            out.print(", index ").print(unsafe.index()).print(", log2_scale ").print(unsafe.log2Scale());
        }
        out.print(", value ").print(unsafe.value()).print(')');
    }

}
