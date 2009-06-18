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

import static com.sun.c1x.ir.Instruction.*;
import static com.sun.c1x.util.InstructionPrinter.InstructionLineColumn.*;

import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * An {@link InstructionVisitor} for {@linkplain #printInstruction(Instruction) printing}
 * an {@link Instruction} as an expression or statement.
 *
 * @author Doug Simon
 */
public class InstructionPrinter extends InstructionVisitor {
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

        final int _position;
        final String _label;

        private InstructionLineColumn(int position, String label) {
            _position = position;
            _label = label;
        }

        /**
         * Prints this column's label to a given stream after padding the stream with '_' characters
         * until its {@linkplain LogStream#position() position} is equal to this column's position.
         * @param out the print stream
         */
        public void printLabel(LogStream out) {
            out.fillTo(_position + out.indentation(), '_');
            out.print(_label);
        }

        /**
         * Prints space characters to a given stream until its {@linkplain LogStream#position() position}
         * is equal to this column's position.
         * @param out the print stream
         */
        public void advance(LogStream out) {
            out.fillTo(_position + out.indentation(), ' ');
        }
    }

    private final LogStream _out;
    private final boolean _printPhis;

    public InstructionPrinter(LogStream out, boolean printPhis) {
        _out = out;
        _printPhis = printPhis;
    }

    public LogStream out() {
        return _out;
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
        BCI.printLabel(_out);
        USE.printLabel(_out);
        VALUE.printLabel(_out);
        INSTRUCTION.printLabel(_out);
        END.printLabel(_out);
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

        int indentation = _out.indentation();
        _out.fillTo(BCI._position + indentation, ' ').
             print(instruction.bci()).
             fillTo(USE._position + indentation, ' ').
             print("0").
             fillTo(VALUE._position + indentation, ' ').
             print(instruction).
             fillTo(INSTRUCTION._position + indentation, ' ');
        printInstruction(instruction);
        _out.println();
    }

    @Override
    public void visitArithmeticOp(ArithmeticOp arithOp) {
        _out.print(arithOp.x()).
             print(' ').
             print(Bytecodes.operator(arithOp.opcode())).
             print(' ').
             print(arithOp.y());
    }

    @Override
    public void visitArrayLength(ArrayLength i) {
        _out.print(i.array()).print(".length");
    }

    @Override
    public void visitBase(Base i) {
        _out.print("std entry B").print(i.standardEntry().blockID());
        if (i.successors().size() > 1) {
          _out.print(" osr entry B").print(i.osrEntry().blockID());
        }
    }

    @Override
    public void visitBlockBegin(BlockBegin block) {
        // print block id
        BlockEnd end = block.end();
        _out.print("B").print(block.blockID()).print(" ");

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
            _out.print('(').print(sb.toString()).print(')');
        }

        // print block bci range
        _out.print('[').print(block.bci()).print(", ").print((end == null ? -1 : end.bci())).print(']');

        // print block successors
        if (end != null && end.successors().size() > 0) {
            _out.print(" .");
            for (BlockBegin successor : end.successors()) {
                _out.print(" B").print(successor.blockID());
            }
        }
        // print exception handlers
        if (!block.exceptionHandlers().isEmpty()) {
            _out.print(" (xhandlers");
            for (BlockBegin handler : block.exceptionHandlerBlocks()) {
                _out.print(" B").print(handler.blockID());
            }
            _out.print(')');
        }

        // print dominator block
        if (block.dominator() != null) {
            _out.print(" dom B").print(block.dominator().blockID());
        }

        // print predecessors
        if (!block.predecessors().isEmpty()) {
            _out.print(" pred:");
            for (BlockBegin pred : block.predecessors()) {
                _out.print(" B").print(pred.blockID());
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
                        _out.println(stateString(i, value, block));
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
                    _out.println(stateString(i, value, block));
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
        _out.print("checkcast(").
             print(checkcast.object()).
             print(") ").
             print(checkcast.targetClass().name());
    }

    @Override
    public void visitCompareOp(CompareOp compareOp) {
        _out.print(compareOp.x()).
             print(' ').
             print(Bytecodes.operator(compareOp.opcode())).
             print(' ').
             print(compareOp.y());
    }

    @Override
    public void visitConstant(Constant constant) {
        ValueType type = constant.type();
        if (type == ConstType.NULL_OBJECT) {
            _out.print("null");
        } else if (type.isPrimitive()) {
            _out.print(type.asConstant().valueString());
        } else if (type.isClass()) {
            ClassType k = (ClassType) type;
            if (k.ciType().isLoaded()) {
                _out.print("<unloaded> ");
            }
            _out.print("class ").print(k.ciType().name());
        } else if (type.isObject()) {
            Object object = type.asConstant().asObject();
            if (object instanceof String) {
                _out.print('"').print(object.toString()).print('"');
            } else {
                _out.print("<object: ").print(object.getClass().getName()).print('@').print(System.identityHashCode(object)).print('>');
            }
        } else if (type.isJsr()) {
            _out.print("bci:").print(type.asConstant().valueString());
        } else {
            _out.print("???");
        }
    }

    @Override
    public void visitConvert(Convert convert) {
        _out.print(Bytecodes.name(convert.opcode())).print('(').print(convert.value()).print(')');
    }

    @Override
    public void visitExceptionObject(ExceptionObject i) {
        _out.print("incoming exception");
    }

    @Override
    public void visitGoto(Goto go2) {
        _out.print("goto B").print(go2.defaultSuccessor().blockID());
        if (go2.isSafepoint()) {
            _out.print(" (safepoint)");
        }
    }

    @Override
    public void visitIf(If i) {
        _out.print("if ").
             print(i.x()).
             print(' ').
             print(i.condition()._operator).
             print(' ').
             print(i.y()).
             print(" then B").
             print(i.successors().get(0).blockID()).
             print(" else B").
             print(i.successors().get(1).blockID());
        if (i.isSafepoint()) {
            _out.print(" (safepoint)");
        }
    }

    @Override
    public void visitIfInstanceOf(IfInstanceOf i) {
        _out.print("<IfInstanceOf>");
    }

    @Override
    public void visitIfOp(IfOp i) {
        _out.print(i.x()).
             print(' ').
             print(i.condition()._operator).
             print(' ').
             print(i.y()).
             print(" ? ").
             print(i.trueValue()).
             print(" : ").
             print(i.falseValue());
    }

    @Override
    public void visitInstanceOf(InstanceOf i) {
        _out.print("instanceof(").print(i.object()).print(") ").print(i.targetClass().name());
    }

    @Override
    public void visitIntrinsic(Intrinsic intrinsic) {
        _out.print(intrinsic.intrinsic().simpleClassName()).print('.').print(intrinsic.intrinsic().name()).print('(');
        for (int i = 0; i < intrinsic.arguments().length; i++) {
          if (i > 0) {
              _out.print(", ");
          }
          _out.print(intrinsic.arguments()[i]);
        }
        _out.print(')');
    }

    @Override
    public void visitInvoke(Invoke invoke) {
        if (invoke.object() != null) {
            _out.print(invoke.object()).print('.');
          }

          _out.print(Bytecodes.name(invoke.opcode())).print('(');
          Instruction[] arguments = invoke.arguments();
          for (int i = 0; i < arguments.length; i++) {
              if (i > 0) {
                  _out.print(", ");
              }
              _out.print(arguments[i]);
          }
          _out.println(')');
          INSTRUCTION.advance(_out);
          CiMethod target = invoke.target();
          _out.print(target.holder().name()).print('.').print(target.name()).print(target.signatureType().asString());
    }

    @Override
    public void visitLoadField(LoadField i) {
        _out.print(i.object()).
             print("._").
             print(i.offset()).
             print(" (").
             print(i.field().type().basicType().basicChar).
             print(")");
    }

    @Override
    public void visitLoadIndexed(LoadIndexed load) {
        _out.print(load).print('[').print(load.index()).print("] (").print(load.type().tchar()).print(')');
    }

    @Override
    public void visitLocal(Local local) {
        _out.print("local[index ").print(local.javaIndex()).print(']');
    }

    @Override
    public void visitLogicOp(LogicOp logicOp) {
        _out.print(logicOp.x()).print(' ').print(Bytecodes.operator(logicOp.opcode())).print(' ').print(logicOp.y());
    }

    @Override
    public void visitLookupSwitch(LookupSwitch lswitch) {
        _out.print("lookupswitch ");
        if (lswitch.isSafepoint()) {
            _out.print("(safepoint) ");
        }
        _out.println(lswitch.value());
        int l = lswitch.numberOfCases();
        for (int i = 0; i < l; i++) {
            INSTRUCTION.advance(_out);
            _out.printf("case %5d: B%d%n", lswitch.keyAt(i), lswitch.successors().get(i).blockID());
        }
        INSTRUCTION.advance(_out);
        _out.print("default   : B").print(lswitch.defaultSuccessor().blockID());

    }

    @Override
    public void visitMonitorEnter(MonitorEnter monitorenter) {
        _out.print("enter monitor[").print(monitorenter.lockNumber()).print("](").print(monitorenter.object()).print(')');
    }

    @Override
    public void visitMonitorExit(MonitorExit monitorexit) {
        _out.print("exit monitor[").print(monitorexit.lockNumber()).print("](").print(monitorexit.object()).print(')');
    }

    @Override
    public void visitNegateOp(NegateOp negate) {
        _out.print('-').print(negate);
    }

    @Override
    public void visitNewInstance(NewInstance newInstance) {
        _out.print("new instance ").print(newInstance.instanceClass().name());
    }

    @Override
    public void visitNewMultiArray(NewMultiArray newMultiArray) {
        _out.print("new multi array [");
        final Instruction[] dimensions = newMultiArray.dimensions();
        for (int i = 0; i < dimensions.length; i++) {
          if (i > 0) {
              _out.print(", ");
          }
          _out.print(dimensions[i]);
        }
        _out.print("] ").print(newMultiArray.elementType().name());
    }

    @Override
    public void visitNewObjectArray(NewObjectArray newObjectArray) {
        _out.print("new object array [").print(newObjectArray.length()).print("] ").print(newObjectArray.elementClass().name());
    }

    @Override
    public void visitNewTypeArray(NewTypeArray newTypeArray) {
        _out.print("new ").print(newTypeArray.elementType().name()).print(" array [").print(newTypeArray.length()).print(']');
    }

    @Override
    public void visitNullCheck(NullCheck i) {
        _out.print("null_check(").print(i.object()).print(')');
        if (!i.canTrap()) {
          _out.print(" (eliminated)");
        }
    }

    @Override
    public void visitOsrEntry(OsrEntry osrEntry) {
        _out.print("osr entry");
    }

    @Override
    public void visitPhi(Phi phi) {
        _out.print("phi function");
    }

    @Override
    public void visitProfileCall(ProfileCall profileCall) {
        final CiMethod method = profileCall.method();
        _out.print("profile ").print(profileCall.object()).print(method.holder().name()).print('.').print(method.name());
        if (profileCall.knownHolder() != null) {
          _out.print(", ").print(profileCall.knownHolder().name());
        }
        _out.print(')');
    }

    @Override
    public void visitProfileCounter(ProfileCounter i) {
        // TODO: Recognize interpreter invocation counter specially
        _out.print("counter [").print(i.mdo()).print(").print(").print(i.offset()).print("] += ").print(i.increment());
    }

    @Override
    public void visitReturn(Return ret) {
        if (ret.result() == null) {
            _out.print("return");
        } else {
            _out.print(ret.type().tchar()).print("return ").print(ret.result());
        }
    }

    @Override
    public void visitRoundFP(RoundFP i) {
        _out.print("roundfp ").print(i.value());
    }

    @Override
    public void visitShiftOp(ShiftOp shiftOp) {
        _out.print(shiftOp.x()).print(' ').print(Bytecodes.operator(shiftOp.opcode())).print(' ').print(shiftOp.y());
    }

    @Override
    public void visitStoreField(StoreField store) {
        _out.print(store.object()).print("._").print(store.offset()).print(" := ").print(store.value()).print(" (").print(store.field().type().basicType().basicChar).print(')');
    }

    @Override
    public void visitStoreIndexed(StoreIndexed store) {
        _out.print(store).print('[').print(store.index()).print("] := ").print(store.value()).print(" (").print(store.type().tchar()).print(')');
    }

    @Override
    public void visitTableSwitch(TableSwitch tswitch) {
        _out.print("tableswitch ");
        if (tswitch.isSafepoint()) {
            _out.print("(safepoint) ");
        }
        _out.println(tswitch.value());
        int l = tswitch.numberOfCases();
        for (int i = 0; i < l; i++) {
            INSTRUCTION.advance(_out);
            _out.printf("case %5d: B%d%n", tswitch.lowKey() + i, tswitch.successors().get(i).blockID());
        }
        INSTRUCTION.advance(_out);
        _out.print("default   : B").print(tswitch.defaultSuccessor().blockID());
    }

    @Override
    public void visitThrow(Throw i) {
        _out.print("throw ").print(i.exception());
    }

    @Override
    public void visitUnsafeGetObject(UnsafeGetObject unsafe) {
        _out.print("UnsafeGetObject.(").print(unsafe.object()).print(", ").print(unsafe.offset()).print(')');
    }

    @Override
    public void visitUnsafeGetRaw(UnsafeGetRaw unsafe) {
        _out.print("UnsafeGetRaw.(base ").print(unsafe.base());
        if (unsafe.hasIndex()) {
            _out.print(", index ").print(unsafe.index()).print(", log2_scale ").print(unsafe.log2Scale());
        }
        _out.print(')');
    }

    @Override
    public void visitUnsafePrefetchRead(UnsafePrefetchRead unsafe) {
        _out.print("UnsafePrefetchRead.(").print(unsafe.object()).print(", ").print(unsafe.offset()).print(')');
    }

    @Override
    public void visitUnsafePrefetchWrite(UnsafePrefetchWrite unsafe) {
        _out.print("UnsafePrefetchWrite.(").print(unsafe.object()).print(", ").print(unsafe.offset()).print(')');
    }

    @Override
    public void visitUnsafePutObject(UnsafePutObject unsafe) {
        _out.print("UnsafePutObject.(").print(unsafe.object()).print(", ").print(unsafe.offset() +
                        ", value ").print(unsafe.value()).print(')');
    }

    @Override
    public void visitUnsafePutRaw(UnsafePutRaw unsafe) {
        _out.print("UnsafePutRaw.(base ").print(unsafe.base());
        if (unsafe.hasIndex()) {
            _out.print(", index ").print(unsafe.index()).print(", log2_scale ").print(unsafe.log2Scale());
        }
        _out.print(", value ").print(unsafe.value()).print(')');
    }

}
