/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.verifier;

import static com.sun.max.vm.bytecode.Bytecode.Flags.*;
import static com.sun.max.vm.verifier.InstructionHandle.Flag.*;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.verifier.TypeInferencingMethodVerifier.*;

/**
 * Inlines subroutines and removes dead code.
 * <p>
 * This implementation is partially derived from inlinejsr.c, a source file in the preverifier tool that is part of the standard
 * <a href="http://java.sun.com/javame/index.jsp">Java Platform, Micro Edition</a> distribution.
 *
 * @author Doug Simon
 * @author David Liu
 */
public class SubroutineInliner {

    private final TypeInferencingMethodVerifier _verifier;
    private final boolean _verbose;
    private final AppendableIndexedSequence<InstructionHandle> _instructionHandles;

    /**
     * Map from each original instruction position to the handles representing the copies of the instruction in the
     * rewritten method. Each original instruction that was in a subroutine may occur more than once in the rewritten method.
     */
    private final InstructionHandle[] _instructionMap;

    public SubroutineInliner(TypeInferencingMethodVerifier verifier, boolean verbose) {
        _verifier = verifier;
        _verbose = verbose;
        _instructionHandles = new ArrayListSequence<InstructionHandle>();
        _instructionMap = new InstructionHandle[verifier.codeAttribute().code().length];
    }

    public CodeAttribute rewriteCode() {
        rewriteOneSubroutine(SubroutineCall.TOP);
        final byte[] newCode = fixupCode();
        final Sequence<ExceptionHandlerEntry> exceptionHandlerTable = fixupExceptionHandlers(newCode);
        final LineNumberTable lineNumberTable = fixupLineNumberTable();
        final LocalVariableTable localVariableTable = fixupLocalVariableTable();

        final CodeAttribute oldCodeAttribute = _verifier.codeAttribute();
        final CodeAttribute newCodeAttribute = new CodeAttribute(
            oldCodeAttribute.constantPool(),
            newCode,
            (char) oldCodeAttribute.maxStack(),
            (char) oldCodeAttribute.maxLocals(),
            exceptionHandlerTable,
            lineNumberTable,
            localVariableTable,
            oldCodeAttribute.stackMapTable());
        return newCodeAttribute;
    }

    private void rewriteOneSubroutine(SubroutineCall subroutineCall) {
        int count = 0;
        final int depth = subroutineCall.depth();
        final int codeLength = _verifier.codeAttribute().code().length;

        InstructionHandle retHandle = null;
        InstructionHandle instructionHandle = null;
        final TypeState[] typeStateMap = _verifier.typeStateMap();

        int typeStatePosition = 0;
        while (typeStatePosition < codeLength) {
            final TypeState typeState = typeStateMap[typeStatePosition];
            if (typeState != null && typeState.visited()) {
                Instruction instruction = typeState.targetedInstruction();
                while (true) {
                    final int position = instruction.position();
                    if (subroutineCall.matches(typeState.subroutineFrame())) {
                        instructionHandle = new InstructionHandle(instruction, subroutineCall, _instructionMap[position]);
                        _instructionMap[position] = instructionHandle;
                        _instructionHandles.append(instructionHandle);
                        ++count;

                        if (count == 1 && depth > 0) {
                            // This is the first instruction included as part of the subroutine call.
                            final InstructionHandle callerHandle = subroutineCall.caller();
                            final Jsr caller = (Jsr) callerHandle.instruction();

                            if (instruction != caller.target.targetedInstruction()) {
                                // If it's not the target of the JSR that got us here the JSR will be converted into a goto.
                                callerHandle.setFlag(JSR_TARGETED_GOTO);
                            }
                        }

                        switch (instruction.opcode()) {
                            case JSR:
                            case JSR_W: {
                                final Jsr jsr = (Jsr) instruction;
                                if (jsr.ret() == null) {
                                    // The subroutine doesn't have a RET instruction so we turn the JSR into a goto.
                                    instructionHandle.setFlag(JSR_SIMPLE_GOTO);
                                } else {
                                    instructionHandle.setFlag(SKIP);
                                }
                                final SubroutineCall innerSubroutine = new SubroutineCall(jsr.target.subroutineFrame().subroutine(), subroutineCall, instructionHandle);
                                rewriteOneSubroutine(innerSubroutine);
                                break;
                            }
                            case RET: {
                                assert retHandle == null : "Multiple RET in one subroutine should have been caught during verification";
                                assert depth != 0 : "RET outside a subroutine should have been caught during verification";
                                retHandle = instructionHandle;
                                break;
                            }
                            case ASTORE_0:
                            case ASTORE_1:
                            case ASTORE_2:
                            case ASTORE_3:
                            case ASTORE: {
                                if (_verifier.isReturnPositionStore(instruction)) {
                                    instructionHandle.setFlag(SKIP);
                                }
                                break;
                            }
                            default:
                                // do nothing
                                break;
                        }
                    }
                    if (instruction.opcode().is(FALL_THROUGH_DELIMITER)) {
                        typeStatePosition = position + instruction.size();
                        break;
                    }
                    instruction = instruction.next();
                }
            } else {
                ++typeStatePosition;
            }
        }

        final int nextHandleIndex = _instructionHandles.length();
        if (depth > 0) {
            subroutineCall.setNextInstuctionHandleIndex(nextHandleIndex);
            if (retHandle != null) {
                // If the last instruction isn't a RET, convert it into a goto
                if (retHandle == instructionHandle) {
                    retHandle.setFlag(SKIP);
                } else {
                    retHandle.setFlag(RET_SIMPLE_GOTO);
                }
            }
        }
    }

    private byte[] fixupCode() {
        int position = 0;
        for (InstructionHandle instructionHandle : _instructionHandles) {
            instructionHandle.setPosition(position);
            if (instructionHandle.flag() != SKIP) {
                final Instruction instruction = instructionHandle.instruction();
                switch (instruction.opcode()) {
                    case TABLESWITCH:
                    case LOOKUPSWITCH:
                        final int oldPadSize = 3 - instruction.position() % 4;
                        final int newPadSize = 3 - position % 4;
                        position += instruction.size() - oldPadSize + newPadSize;
                        break;
                    case RET:
                        // becomes a goto with a 16-bit offset
                        position += 3;
                        break;
                    default:
                        position += instruction.size();
                        break;
                }
            }
        }

        if (_verbose) {
            Log.println();
            final String methodSignature = _verifier.classMethodActor().format("%H.%n(%p)");
            Log.println("Rewriting " + methodSignature);
        }

        final int newCodeSize = position;
        // Create new code array
        try {
            final ByteArrayOutputStream newCodeStream = new ByteArrayOutputStream(newCodeSize);
            final DataOutputStream dataStream = new DataOutputStream(newCodeStream);
            for (InstructionHandle instructionHandle : _instructionHandles) {
                if (_verbose) {
                    Log.println(instructionHandle + "   // " + instructionHandle.flag());
                }
                if (instructionHandle.flag() != SKIP) {
                    final Instruction instruction = instructionHandle.instruction();
                    position = instructionHandle.position();
                    assert position == newCodeStream.size();
                    final SubroutineCall subroutine = instructionHandle.subroutineCall();

                    if (instruction instanceof Branch) {
                        final Branch branch = (Branch) instruction;
                        final Bytecode opcode = instruction.opcode();

                        if (instruction instanceof Jsr) {
                            final Jsr jsr = (Jsr) branch;
                            assert instructionHandle.flag() == JSR_SIMPLE_GOTO || instructionHandle.flag() == JSR_TARGETED_GOTO;
                            final SubroutineCall innerSubroutine = new SubroutineCall(jsr.target.subroutineFrame().subroutine(), subroutine, instructionHandle);

                            if (opcode == Bytecode.JSR_W) {
                                assert instruction.size() == 5;
                                Bytecode.GOTO_W.writeTo(dataStream);
                                dataStream.writeInt(calculateNewOffset(position, innerSubroutine, branch.target.position(), Ints.VALUE_RANGE));
                            } else {
                                assert instruction.size() == 3;
                                Bytecode.GOTO.writeTo(dataStream);
                                dataStream.writeShort(calculateNewOffset(position, innerSubroutine, branch.target.position(), Shorts.VALUE_RANGE));
                            }
                        } else {
                            opcode.writeTo(dataStream);
                            if (opcode == Bytecode.GOTO_W) {
                                assert instruction.size() == 5;
                                dataStream.writeInt(calculateNewOffset(position, subroutine, branch.target.position(), Ints.VALUE_RANGE));
                            } else {
                                assert instruction.size() == 3;
                                dataStream.writeShort(calculateNewOffset(position, subroutine, branch.target.position(), Shorts.VALUE_RANGE));
                            }
                        }
                    } else {
                        final Bytecode opcode = instruction.opcode();
                        switch (opcode) {
                            case RET: {
                                final Ret ret = (Ret) instruction;
                                SubroutineCall callingSuboutine = subroutine;
                                int extraFramesToPop = ret.numberOfFramesPopped() - 1;
                                while (extraFramesToPop > 0) {
                                    callingSuboutine = callingSuboutine.parent();
                                    --extraFramesToPop;
                                }

                                final InstructionHandle gotoTarget = _instructionHandles.get(callingSuboutine.nextInstuctionHandleIndex());
                                final int offset = gotoTarget.position() - position;
                                checkOffset(offset, Shorts.VALUE_RANGE);
                                Bytecode.GOTO.writeTo(dataStream);
                                dataStream.writeShort(offset);
                                break;
                            }
                            case TABLESWITCH:
                            case LOOKUPSWITCH: {
                                final Select select = (Select) instruction;
                                opcode.writeTo(dataStream);
                                final int padding = 3 - position % 4; // number of pad bytes

                                for (int i = 0; i < padding; i++) {
                                    dataStream.writeByte(0);
                                }

                                // Update default target
                                dataStream.writeInt(calculateNewOffset(position, subroutine, select.defaultTarget.position(), Ints.VALUE_RANGE));

                                if (opcode == Bytecode.TABLESWITCH) {
                                    final Tableswitch tableswitch = (Tableswitch) select;
                                    dataStream.writeInt(tableswitch.low);
                                    dataStream.writeInt(tableswitch.high);
                                    for (TypeState target : tableswitch.caseTargets) {
                                        dataStream.writeInt(calculateNewOffset(position, subroutine, target.position(), Ints.VALUE_RANGE));
                                    }
                                } else {
                                    final Lookupswitch lookupswitch = (Lookupswitch) select;
                                    final TypeState[] caseTargets = lookupswitch.caseTargets;
                                    final int[] matches = lookupswitch.matches;
                                    dataStream.writeInt(matches.length); // npairs
                                    for (int i = 0; i != matches.length; ++i) {
                                        final TypeState target = caseTargets[i];
                                        dataStream.writeInt(matches[i]);
                                        dataStream.writeInt(calculateNewOffset(position, subroutine, target.position(), Ints.VALUE_RANGE));
                                    }
                                }
                                break;
                            }
                            default:
                                instruction.writeTo(dataStream);
                                break;
                        }
                    }
                }
            }

            dataStream.close();
            return newCodeStream.toByteArray();
        } catch (IOException ioe) {
            throw ProgramError.unexpected(ioe);
        }
    }

    private void checkOffset(int offset, Range allowableOffsetRange) {
        if (!allowableOffsetRange.contains(offset)) {
            throw ErrorContext.classFormatError("Subroutine inlining expansion caused an offset to grow beyond what a branch instruction can encode");
        }
    }

    private int calculateNewOffset(int fromPosition, SubroutineCall fromSubroutine, int oldToPosition, Range allowableOffsetRange) {
        for (InstructionHandle target = _instructionMap[oldToPosition]; target != null; target = target.next()) {
            if (fromSubroutine.canGoto(target.subroutineCall())) {
                final int offset = target.position() - fromPosition;
                checkOffset(offset, allowableOffsetRange);
                return offset;
            }
        }
        throw ProgramError.unexpected("cannot find updated target position");
    }

    private Sequence<ExceptionHandlerEntry> fixupExceptionHandlers(byte[] newCode) {
        final CodeAttribute codeAttribute = _verifier.codeAttribute();
        final Sequence<ExceptionHandlerEntry> oldHandlers = codeAttribute.exceptionHandlerTable();
        if (oldHandlers.isEmpty()) {
            return oldHandlers;
        }
        final AppendableSequence<ExceptionHandlerEntry> newHandlers = new ArrayListSequence<ExceptionHandlerEntry>(oldHandlers.length() * 2);
        for (ExceptionHandlerEntry oldHandler : oldHandlers) {
            // For each instruction handle that maps to this handler, match it to all instructions that go to the handler.
            for (InstructionHandle handlerHandle = _instructionMap[oldHandler.handlerPosition()]; handlerHandle != null; handlerHandle = handlerHandle.next()) {
                // Find all instructions that go to this handler
                boolean lastMatch = false;
                ExceptionHandlerEntry currentHandler = null;
                for (InstructionHandle instructionHandle : _instructionHandles) {
                    final Instruction instruction = instructionHandle.instruction();
                    final int position = instruction.position();
                    if (instructionHandle.flag() != SKIP) {
                        final boolean match =
                            (position >= oldHandler.startPosition()) &&
                            (position < oldHandler.endPosition()) &&
                            instructionHandle.subroutineCall().canGoto(handlerHandle.subroutineCall());
                        if (match && !lastMatch) {
                            // start a new catch frame
                            currentHandler = new ExceptionHandlerEntry(instructionHandle.position(), oldHandler.endPosition(),
                                handlerHandle.position(), oldHandler.catchTypeIndex());
                            lastMatch = true;
                        } else if (lastMatch && !match) {
                            currentHandler = currentHandler.changeEndPosition(instructionHandle.position());
                            newHandlers.append(currentHandler);
                            lastMatch = false;
                        }
                    }
                }
                if (lastMatch) {
                    assert !Sequence.Static.containsIdentical(newHandlers, currentHandler);
                    // code end is still in the catch frame
                    currentHandler = currentHandler.changeEndPosition(newCode.length);
                    newHandlers.append(currentHandler);
                }
            }
        }
        return newHandlers;
    }

    private LineNumberTable fixupLineNumberTable() {
        final CodeAttribute codeAttribute = _verifier.codeAttribute();
        final LineNumberTable lineNumberTable = codeAttribute.lineNumberTable();
        if (lineNumberTable.isEmpty()) {
            return LineNumberTable.EMPTY;
        }

        // Expand the original line number tables into a map that covers every instruction position in the original code.
        final int oldCodeLength = codeAttribute.code().length;
        final int[] oldPositionToLineNumberMap = new int[oldCodeLength];
        final LineNumberTable.Entry[] entries = lineNumberTable.entries();
        int i = 0;
        int endPc;
        int line;
        int pc;

        // Process all but the last line number table entry
        for (; i < entries.length - 1; i++) {
            line = entries[i].lineNumber();
            endPc = entries[i + 1].position();
            for (pc = entries[i].position(); pc < endPc; pc++) {
                oldPositionToLineNumberMap[pc] = line;
            }
        }

        // Process the last line number table entry
        line = entries[i].lineNumber();
        for (pc = entries[i].position(); pc < oldCodeLength; pc++) {
            oldPositionToLineNumberMap[pc] = line;
        }

        int currentLineNumber = -1;
        final AppendableSequence<LineNumberTable.Entry> newEntries = new ArrayListSequence<LineNumberTable.Entry>();
        for (InstructionHandle instructionHandle : _instructionHandles) {
            if (instructionHandle.flag() != SKIP) {
                final Instruction instruction = instructionHandle.instruction();
                final int nextLineNumber = oldPositionToLineNumberMap[instruction.position()];
                if (nextLineNumber != currentLineNumber) {
                    final LineNumberTable.Entry entry = new LineNumberTable.Entry((char) instructionHandle.position(), (char) nextLineNumber);
                    newEntries.append(entry);
                    currentLineNumber = nextLineNumber;
                }
            }
        }

        return new LineNumberTable(Sequence.Static.toArray(newEntries, LineNumberTable.Entry.class));
    }

    private LocalVariableTable fixupLocalVariableTable() {
        final CodeAttribute codeAttribute = _verifier.codeAttribute();
        final LocalVariableTable localVariableTable = codeAttribute.localVariableTable();
        if (localVariableTable.isEmpty()) {
            return LocalVariableTable.EMPTY;
        }
        final AppendableSequence<LocalVariableTable.Entry> newEntries = new ArrayListSequence<LocalVariableTable.Entry>();
        final LocalVariableTable.Entry[] entries = localVariableTable.entries();
        for (LocalVariableTable.Entry entry : entries) {
            final int startPc = entry.startPosition();
            final int endPc = startPc + entry.length(); // inclusive
            InstructionHandle lastMatchedHandle = null;

            int lastMatchedStartPc = -1;
            for (InstructionHandle instructionHandle : _instructionHandles) {
                if (instructionHandle.flag() != SKIP) {
                    final Instruction instruction = instructionHandle.instruction();
                    final boolean matches = instruction.position() >= startPc && instruction.position() <= endPc;
                    if (lastMatchedHandle == null && matches) {
                        lastMatchedStartPc = instructionHandle.position();
                        lastMatchedHandle = instructionHandle;
                    } else if (lastMatchedHandle != null && !matches) {
                        final LocalVariableTable.Entry newEntry =
                            new LocalVariableTable.Entry((char) lastMatchedStartPc,
                                                         (char) (lastMatchedHandle.position() - lastMatchedStartPc),
                                                         (char) entry.slot(),
                                                         (char) entry.nameIndex(),
                                                         (char) entry.descriptorIndex(),
                                                         (char) entry.signatureIndex());
                        newEntries.append(newEntry);
                        lastMatchedHandle = null;
                    }
                }
            }
            if (lastMatchedHandle != null) {
                final LocalVariableTable.Entry newEntry =
                    new LocalVariableTable.Entry((char) lastMatchedStartPc,
                                    (char) (lastMatchedHandle.position() - lastMatchedStartPc),
                                    (char) entry.slot(),
                                    (char) entry.nameIndex(),
                                    (char) entry.descriptorIndex(),
                                    (char) entry.signatureIndex());
                newEntries.append(newEntry);
            }
        }

        return new LocalVariableTable(Sequence.Static.toList(newEntries));
    }
}
