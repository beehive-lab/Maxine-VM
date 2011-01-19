/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.c1x.debug.InstructionPrinter.InstructionLineColumn.*;

import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.Bytecodes.MemoryBarriers;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * A {@link ValueVisitor} for {@linkplain #printInstruction(Value) printing}
 * an {@link Instruction} as an expression or statement.
 *
 * @author Doug Simon
 */
public class InstructionPrinter extends ValueVisitor {

    /**
     * Formats a given instruction as a value in a {@linkplain FrameState frame state}. If the instruction is a phi defined at a given
     * block, its {@linkplain Phi#inputCount() inputs} are appended to the returned string.
     *
     * @param index the index of the value in the frame state
     * @param value the frame state value
     * @param block if {@code value} is a phi, then its inputs are formatted if {@code block} is its
     *            {@linkplain Phi#block() join point}
     * @return the instruction representation as a string
     */
    public static String stateString(int index, Value value, BlockBegin block) {
        StringBuilder sb = new StringBuilder(30);
        sb.append(String.format("%2d  %s", index, Util.valueString(value)));
        if (value instanceof Phi) {
            Phi phi = (Phi) value;
            // print phi operands
            if (phi.block() == block) {
                sb.append(" [");
                for (int j = 0; j < phi.inputCount(); j++) {
                    sb.append(' ');
                    Value operand = phi.inputAt(j);
                    if (operand != null) {
                        sb.append(Util.valueString(operand));
                    } else {
                        sb.append("NULL");
                    }
                }
                sb.append("] ");
            }
        }
        if (value != null && value.hasSubst()) {
            sb.append("alias ").append(Util.valueString(value.subst()));
        }
        return sb.toString();
    }

    /**
     * The columns printed in a tabulated instruction
     * {@linkplain InstructionPrinter#printInstructionListing(Value) listing}.
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
         * The instruction as a {@linkplain com.sun.c1x.util.Util#valueString(com.sun.c1x.ir.Value) value}.
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
            out.fillTo(position + out.indentationLevel(), '_');
            out.print(label);
        }

        /**
         * Prints space characters to a given stream until its {@linkplain LogStream#position() position}
         * is equal to this column's position.
         * @param out the print stream
         */
        public void advance(LogStream out) {
            out.fillTo(position + out.indentationLevel(), ' ');
        }
    }

    private final LogStream out;
    private final boolean printPhis;
    private final CiTarget target;

    public InstructionPrinter(LogStream out, boolean printPhis, CiTarget target) {
        this.out = out;
        this.printPhis = printPhis;
        this.target = target;
    }

    public LogStream out() {
        return out;
    }

    /**
     * Prints a given instruction as an expression or statement.
     *
     * @param instruction the instruction to print
     */
    public void printInstruction(Value instruction) {
        instruction.accept(this);
    }

    public void printBlock(BlockBegin block) {
        block.accept(this); // TODO: maybe we don't need to print out the whole block
    }

    /**
     * Prints a header for the tabulated data printed by {@link #printInstructionListing(Value)}.
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
    public void printInstructionListing(Value instruction) {
        if (instruction.isLive()) {
            out.print('.');
        }

        int indentation = out.indentationLevel();
        out.fillTo(BCI.position + indentation, ' ').
             print(instruction instanceof Instruction ? ((Instruction) instruction).bci() : 0).
             fillTo(USE.position + indentation, ' ').
             print("0").
             fillTo(VALUE.position + indentation, ' ').
             print(instruction).
             fillTo(INSTRUCTION.position + indentation, ' ');
        printInstruction(instruction);
        String flags = instruction.flagsToString();
        if (!flags.isEmpty()) {
            out.print("  [flags: " + flags + "]");
        }
        if (instruction instanceof StateSplit) {
            out.print("  [state: " + ((StateSplit) instruction).stateBefore() + "]");
        }
        out.println();
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp arithOp) {
        out.print(arithOp.x()).
             print(' ').
             print(Bytecodes.operator(arithOp.opcode)).
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

        if (end != null && end.stateAfter() != null) {
            FrameState state = block.stateBefore();

            int i = 0;
            while (!hasPhisOnStack && i < state.stackSize()) {
                Value value = state.stackAt(i);
                hasPhisOnStack = isPhiAtBlock(value, block);
                if (value != null && !value.isIllegal()) {
                    i += value.kind.sizeInSlots();
                } else {
                    i++;
                }
            }

            do {
                for (i = 0; !hasPhisInLocals && i < state.localsSize();) {
                    Value value = state.localAt(i);
                    hasPhisInLocals = isPhiAtBlock(value, block);
                    // also ignore illegal HiWords
                    if (value != null && !value.isIllegal()) {
                        i += value.kind.sizeInSlots();
                    } else {
                        i++;
                    }
                }
                state = state.callerState();
            } while (state != null);
        }

        // print values in locals
        if (hasPhisInLocals) {
            out.println();
            out.println("Locals:");

            FrameState state = block.stateBefore();
            do {
                int i = 0;
                while (i < state.localsSize()) {
                    Value value = state.localAt(i);
                    if (value != null) {
                        out.println(stateString(i, value, block));
                        // also ignore illegal HiWords
                        i += value.isIllegal() ? 1 : value.kind.sizeInSlots();
                    } else {
                        i++;
                    }
                }
                out.println();
                state = state.callerState();
            } while (state != null);
        }

        // print values on stack
        if (hasPhisOnStack) {
            out.println();
            out.println("Stack:");
            int i = 0;
            while (i < block.stateBefore().stackSize()) {
                Value value = block.stateBefore().stackAt(i);
                if (value != null) {
                    out.println(stateString(i, value, block));
                    i += value.kind.sizeInSlots();
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
    public static boolean isPhiAtBlock(Value value, BlockBegin block) {
        return value instanceof Phi && ((Phi) value).block() == block;
    }

    private String nameOf(RiType type) {
        return CiUtil.toJavaName(type);
    }

    @Override
    public void visitCheckCast(CheckCast checkcast) {
        out.print("checkcast(").
             print(checkcast.object()).
             print(",").
             print(checkcast.targetClassInstruction()).
             print(") ").
             print(nameOf(checkcast.targetClass()));
    }

    @Override
    public void visitCompareOp(CompareOp compareOp) {
        out.print(compareOp.x()).
             print(' ').
             print(Bytecodes.operator(compareOp.opcode)).
             print(' ').
             print(compareOp.y());
    }

    @Override
    public void visitUnsignedCompareOp(UnsignedCompareOp compareOp) {
        out.print(compareOp.x()).
             print(' ').
             print(Bytecodes.operator(compareOp.opcode)).
             print(' ').
             print(compareOp.y());
    }

    @Override
    public void visitConstant(Constant constant) {
        CiConstant value = constant.value;
        if (value == CiConstant.NULL_OBJECT) {
            out.print("null");
        } else if (value.kind.isPrimitive()) {
            out.print(constant.asConstant().valueString());
        } else if (value.kind.isObject()) {
            Object object = constant.asConstant().asObject();
            if (object == null) {
                out.print("null");
            } else if (object instanceof String) {
                out.print('"').print(object.toString()).print('"');
            } else {
                out.print("<object: ").print(value.kind.format(object)).print('>');
            }
        } else if (value.kind.isWord()) {
            out.print("0x").print(Long.toHexString(value.asLong()));
        } else if (value.kind.isJsr()) {
            out.print("bci:").print(constant.asConstant().valueString());
        } else {
            out.print("???");
        }
    }

    @Override
    public void visitConvert(Convert convert) {
        out.print(Bytecodes.nameOf(convert.opcode)).print('(').print(convert.value()).print(')');
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
        out.print("instanceof(").print(i.object()).print(") ").print(nameOf(i.targetClass()));
    }

    @Override
    public void visitIntrinsic(Intrinsic intrinsic) {
        out.print(intrinsic.intrinsic().className).print('.').print(intrinsic.intrinsic().name()).print('(');
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

        RiMethod target = invoke.target();
        out.print(target.name()).print('(');
        Value[] arguments = invoke.arguments();
        for (int i = argStart; i < arguments.length; i++) {
            if (i > argStart) {
                out.print(", ");
            }
            out.print(arguments[i]);
        }
        out.print(CiUtil.format(") [method: %H.%n(%p):%r]", target, false));
    }

    @Override
    public void visitLoadField(LoadField i) {
        out.print(i.object()).
             print(".").
             print(i.field().name()).
             print(" [field: ").
             print(CiUtil.format("%h.%n:%t", i.field(), false)).
             print("]");
    }

    @Override
    public void visitLoadIndexed(LoadIndexed load) {
        out.print(load.array()).print('[').print(load.index()).print("] (").print(load.kind.typeChar).print(')');
    }

    @Override
    public void visitLocal(Local local) {
        out.print("local[index ").print(local.javaIndex()).print(']');
    }

    @Override
    public void visitLogicOp(LogicOp logicOp) {
        out.print(logicOp.x()).print(' ').print(Bytecodes.operator(logicOp.opcode)).print(' ').print(logicOp.y());
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
    public void visitMemoryBarrier(MemoryBarrier i) {
        out.print(MemoryBarriers.barriersString(i.barriers));
    }

    @Override
    public void visitMonitorAddress(MonitorAddress i) {
        out.print("monitor_address (").print(i.monitor()).print(")");
    }

    @Override
    public void visitMonitorEnter(MonitorEnter monitorenter) {
        out.print("enter monitor[").print(monitorenter.lockNumber).print("](").print(monitorenter.object()).print(')');
    }

    @Override
    public void visitMonitorExit(MonitorExit monitorexit) {
        out.print("exit monitor[").print(monitorexit.lockNumber).print("](").print(monitorexit.object()).print(')');
    }

    @Override
    public void visitNegateOp(NegateOp negate) {
        out.print("- ").print(negate.x());
    }

    @Override
    public void visitSignificantBit(SignificantBitOp significantBit) {
        out.print(Bytecodes.nameOf(significantBit.op) + " [").print(significantBit).print("] ");
    }

    @Override
    public void visitNewInstance(NewInstance newInstance) {
        out.print("new instance ").print(nameOf(newInstance.instanceClass()));
    }

    @Override
    public void visitNewMultiArray(NewMultiArray newMultiArray) {
        out.print("new multi array [");
        final Value[] dimensions = newMultiArray.dimensions();
        for (int i = 0; i < dimensions.length; i++) {
          if (i > 0) {
              out.print(", ");
          }
          out.print(dimensions[i]);
        }
        out.print("] ").print(nameOf(newMultiArray.elementKind));
    }

    @Override
    public void visitNewObjectArray(NewObjectArray newObjectArray) {
        out.print("new object array [").print(newObjectArray.length()).print("] ").print(nameOf(newObjectArray.elementClass()));
    }

    @Override
    public void visitNewTypeArray(NewTypeArray newTypeArray) {
        out.print("new ").print(newTypeArray.elementKind().name()).print(" array [").print(newTypeArray.length()).print(']');
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
    public void visitReturn(Return ret) {
        if (ret.result() == null) {
            out.print("return");
        } else {
            out.print(ret.kind.typeChar).print("return ").print(ret.result());
        }
    }

    @Override
    public void visitShiftOp(ShiftOp shiftOp) {
        out.print(shiftOp.x()).print(' ').print(Bytecodes.operator(shiftOp.opcode)).print(' ').print(shiftOp.y());
    }

    @Override
    public void visitStoreField(StoreField store) {
        out.print(store.object()).
            print(".").
            print(store.field().name()).
            print(" := ").
            print(store.value()).
            print(" [type: ").print(CiUtil.format("%h.%n:%t", store.field(), false)).
            print(']');
    }

    @Override
    public void visitStoreIndexed(StoreIndexed store) {
        out.print(store.array()).print('[').print(store.index()).print("] := ").print(store.value()).print(" (").print(store.kind.typeChar).print(')');
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
    public void visitCompareAndSwap(CompareAndSwap i) {
        out.print(Bytecodes.nameOf(i.opcode)).print("(").print(i.pointer());
        out.print(" + ").print(i.offset());
        out.print(", ").print(i.expectedValue()).print(", ").print(i.newValue()).print(')');
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

    @Override
    public void visitInfopoint(Infopoint i) {
        out.print(Bytecodes.nameOf(i.opcode));
    }

    @Override
    public void visitLoadPointer(LoadPointer i) {
        out.print("*(").print(i.pointer());
        if (i.displacement() == null) {
            out.print(" + ").print(i.offset());
        } else {
            int scale = target.sizeInBytes(i.dataKind);
            out.print(" + ").print(i.displacement()).print(" + (").print(i.index()).print(" * " + scale + ")");
        }
        out.print(")");
    }

    @Override
    public void visitLoadRegister(LoadRegister i) {
        out.print(i.register.toString());
    }

    @Override
    public void visitNativeCall(NativeCall invoke) {
        out.print(invoke.nativeMethod.jniSymbol()).print('(');
        Value[] arguments = invoke.arguments;
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                out.print(", ");
            }
            out.print(arguments[i]);
        }
        out.print(')');
    }

    @Override
    public void visitResolveClass(ResolveClass i) {
        out.print("resolve[").print(nameOf(i.type)).print("-" + i.portion + "]");
    }

    @Override
    public void visitStorePointer(StorePointer i) {
        out.print("*(").print(i.pointer());
        if (i.displacement() == null) {
            out.print(" + ").print(i.offset());
        } else {
            int scale = target.sizeInBytes(i.dataKind);
            out.print(" + ").print(i.displacement()).print(" + (").print(i.index()).print(" * " + scale + ")");
        }
        out.print(") := ").print(i.value());
    }

    @Override
    public void visitStoreRegister(StoreRegister i) {
        out.print(i.register.toString()).print(" := ").print(i.value());
    }

    @Override
    public void visitStackAllocate(StackAllocate i) {
        out.print("alloca(").print(i.size()).print(")");
    }

    @Override
    public void visitUnsafeCast(UnsafeCast i) {
        out.print("unsafe_cast(").
        print(i.value()).
        print(") ").
        print(nameOf(i.toType));
    }

    @Override
    public void visitLoadStackAddress(AllocateStackVariable i) {
        out.print("&(").print(i.value()).print(")");
    }

    @Override
    public void visitPause(Pause i) {
        out.print("pause");
    }

    @Override
    public void visitBreakpointTrap(BreakpointTrap i) {
        out.print("breakpoint_trap");
    }

    @Override
    public void visitArrayCopy(ArrayCopy arrayCopy) {
        out.print("arrayCopy");
    }

    @Override
    public void visitBoundsCheck(BoundsCheck boundsCheck) {
        out.print("boundsCheck ").print(boundsCheck.index()).print(" ").print(boundsCheck.length());

    }
}
