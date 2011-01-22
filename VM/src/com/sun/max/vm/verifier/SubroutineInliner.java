/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.verifier;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.vm.verifier.InstructionHandle.Flag.*;

import java.io.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
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

    private final TypeInferencingMethodVerifier verifier;
    private final boolean verbose;
    private final List<InstructionHandle> instructionHandles;

    /**
     * Map from each original instruction position to the handles representing the copies of the instruction in the
     * rewritten method. Each original instruction that was in a subroutine may occur more than once in the rewritten method.
     */
    private final InstructionHandle[] instructionMap;

    public SubroutineInliner(TypeInferencingMethodVerifier verifier, boolean verbose) {
        this.verifier = verifier;
        this.verbose = verbose;
        this.instructionHandles = new ArrayList<InstructionHandle>();
        this.instructionMap = new InstructionHandle[verifier.codeAttribute().code().length];
    }

    public CodeAttribute rewriteCode() {
        rewriteOneSubroutine(SubroutineCall.TOP);
        final byte[] newCode = fixupCode();
        final ExceptionHandlerEntry[] exceptionHandlerTable = fixupExceptionHandlers(newCode);
        final LineNumberTable lineNumberTable = fixupLineNumberTable();
        final LocalVariableTable localVariableTable = fixupLocalVariableTable();

        final CodeAttribute oldCodeAttribute = verifier.codeAttribute();
        final CodeAttribute newCodeAttribute = new CodeAttribute(
                oldCodeAttribute.constantPool,
            newCode,
            (char) oldCodeAttribute.maxStack,
            (char) oldCodeAttribute.maxLocals,
            exceptionHandlerTable,
            lineNumberTable,
            localVariableTable,
            oldCodeAttribute.stackMapTable());
        return newCodeAttribute;
    }

    private void rewriteOneSubroutine(SubroutineCall subroutineCall) {
        int count = 0;
        final int depth = subroutineCall.depth;
        final int codeLength = verifier.codeAttribute().code().length;

        InstructionHandle retHandle = null;
        InstructionHandle instructionHandle = null;
        final TypeState[] typeStateMap = verifier.typeStateMap();

        int typeStatePosition = 0;
        while (typeStatePosition < codeLength) {
            final TypeState typeState = typeStateMap[typeStatePosition];
            if (typeState != null && typeState.visited()) {
                Instruction instruction = typeState.targetedInstruction();
                while (true) {
                    final int position = instruction.position();
                    if (subroutineCall.matches(typeState.subroutineFrame())) {
                        instructionHandle = new InstructionHandle(instruction, subroutineCall, instructionMap[position]);
                        instructionMap[position] = instructionHandle;
                        instructionHandles.add(instructionHandle);
                        ++count;

                        if (count == 1 && depth > 0) {
                            // This is the first instruction included as part of the subroutine call.
                            final InstructionHandle callerHandle = subroutineCall.caller;
                            final Jsr caller = (Jsr) callerHandle.instruction;

                            if (instruction != caller.target.targetedInstruction()) {
                                // If it's not the target of the JSR that got us here the JSR will be converted into a goto.
                                callerHandle.flag = JSR_TARGETED_GOTO;
                            }
                        }

                        switch (instruction.opcode) {
                            case JSR:
                            case JSR_W: {
                                final Jsr jsr = (Jsr) instruction;
                                if (jsr.ret() == null) {
                                    // The subroutine doesn't have a RET instruction so we turn the JSR into a goto.
                                    instructionHandle.flag = JSR_SIMPLE_GOTO;
                                } else {
                                    instructionHandle.flag = SKIP;
                                }
                                final SubroutineCall innerSubroutine = new SubroutineCall(jsr.target.subroutineFrame().subroutine, subroutineCall, instructionHandle);
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
                                if (verifier.isReturnPositionStore(instruction)) {
                                    instructionHandle.flag = SKIP;
                                }
                                break;
                            }
                            default:
                                // do nothing
                                break;
                        }
                    }
                    if (Bytecodes.isStop(instruction.opcode)) {
                        typeStatePosition = position + instruction.size();
                        break;
                    }
                    instruction = instruction.next();
                }
            } else {
                ++typeStatePosition;
            }
        }

        final int nextHandleIndex = instructionHandles.size();
        if (depth > 0) {
            subroutineCall.setNextInstuctionHandleIndex(nextHandleIndex);
            if (retHandle != null) {
                // If the last instruction isn't a RET, convert it into a goto
                if (retHandle == instructionHandle) {
                    retHandle.flag = SKIP;
                } else {
                    retHandle.flag = RET_SIMPLE_GOTO;
                }
            }
        }
    }

    private byte[] fixupCode() {
        int position = 0;
        for (InstructionHandle instructionHandle : instructionHandles) {
            instructionHandle.position = position;
            if (instructionHandle.flag != SKIP) {
                final Instruction instruction = instructionHandle.instruction;
                switch (instruction.opcode) {
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

        if (verbose) {
            Log.println();
            final String methodSignature = verifier.classMethodActor().format("%H.%n(%p)");
            Log.println("Rewriting " + methodSignature);
        }

        final int newCodeSize = position;
        // Create new code array
        try {
            final ByteArrayOutputStream newCodeStream = new ByteArrayOutputStream(newCodeSize);
            final DataOutputStream dataStream = new DataOutputStream(newCodeStream);
            for (InstructionHandle instructionHandle : instructionHandles) {
                if (verbose) {
                    Log.println(instructionHandle + "   // " + instructionHandle.flag);
                }
                if (instructionHandle.flag != SKIP) {
                    final Instruction instruction = instructionHandle.instruction;
                    position = instructionHandle.position;
                    assert position == newCodeStream.size();
                    final SubroutineCall subroutine = instructionHandle.subroutineCall;

                    if (instruction instanceof Branch) {
                        final Branch branch = (Branch) instruction;
                        final int opcode = instruction.opcode;

                        if (instruction instanceof Jsr) {
                            final Jsr jsr = (Jsr) branch;
                            assert instructionHandle.flag == JSR_SIMPLE_GOTO || instructionHandle.flag == JSR_TARGETED_GOTO;
                            final SubroutineCall innerSubroutine = new SubroutineCall(jsr.target.subroutineFrame().subroutine, subroutine, instructionHandle);

                            if (opcode == JSR_W) {
                                assert instruction.size() == 5;
                                dataStream.write(GOTO_W);
                                dataStream.writeInt(calculateNewOffset(position, innerSubroutine, branch.target.position(), Ints.VALUE_RANGE));
                            } else {
                                assert instruction.size() == 3;
                                dataStream.write(GOTO);
                                dataStream.writeShort(calculateNewOffset(position, innerSubroutine, branch.target.position(), Shorts.VALUE_RANGE));
                            }
                        } else {
                            dataStream.write(opcode);
                            if (opcode == Bytecodes.GOTO_W) {
                                assert instruction.size() == 5;
                                dataStream.writeInt(calculateNewOffset(position, subroutine, branch.target.position(), Ints.VALUE_RANGE));
                            } else {
                                assert instruction.size() == 3;
                                dataStream.writeShort(calculateNewOffset(position, subroutine, branch.target.position(), Shorts.VALUE_RANGE));
                            }
                        }
                    } else {
                        final int opcode = instruction.opcode;
                        switch (opcode) {
                            case RET: {
                                final Ret ret = (Ret) instruction;
                                SubroutineCall callingSuboutine = subroutine;
                                int extraFramesToPop = ret.numberOfFramesPopped() - 1;
                                while (extraFramesToPop > 0) {
                                    callingSuboutine = callingSuboutine.parent();
                                    --extraFramesToPop;
                                }

                                final InstructionHandle gotoTarget = instructionHandles.get(callingSuboutine.nextInstuctionHandleIndex());
                                final int offset = gotoTarget.position - position;
                                checkOffset(offset, Shorts.VALUE_RANGE);
                                dataStream.write(GOTO);
                                dataStream.writeShort(offset);
                                break;
                            }
                            case TABLESWITCH:
                            case LOOKUPSWITCH: {
                                final Select select = (Select) instruction;
                                dataStream.write(opcode);
                                final int padding = 3 - position % 4; // number of pad bytes

                                for (int i = 0; i < padding; i++) {
                                    dataStream.writeByte(0);
                                }

                                // Update default target
                                dataStream.writeInt(calculateNewOffset(position, subroutine, select.defaultTarget.position(), Ints.VALUE_RANGE));

                                if (opcode == Bytecodes.TABLESWITCH) {
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
            throw verifier.fatalVerifyError("IO error while fixing up code: " + ioe);
        }
    }

    private void checkOffset(int offset, Range allowableOffsetRange) {
        if (!allowableOffsetRange.contains(offset)) {
            verifier.verifyError("Subroutine inlining expansion caused an offset to grow beyond what a branch instruction can encode");
        }
    }

    /**
     * Computes the offset for a branch or goto instruction where either it or its target has been inlined
     * (and thus resides at a new position).
     *
     * @param fromPosition the (possibly new) position of a branch or goto instruction
     * @param fromSubroutine the subroutine in which the branch or goto instruction resides
     * @param oldToPosition the old target position prior to subroutine inlining
     * @param allowableOffsetRange the valid value range for the adjusted offset
     * @return the computed offset
     * @throws VerifyError if the new offset could not be computed
     */
    private int calculateNewOffset(int fromPosition, SubroutineCall fromSubroutine, int oldToPosition, Range allowableOffsetRange) {
        for (InstructionHandle target = instructionMap[oldToPosition]; target != null; target = target.next) {
            if (fromSubroutine.canGoto(target.subroutineCall)) {
                final int offset = target.position - fromPosition;
                checkOffset(offset, allowableOffsetRange);
                return offset;
            }
        }
        throw verifier.fatalVerifyError("Cannot find new position for instruction that used to be at " + oldToPosition);
    }

    private ExceptionHandlerEntry[] fixupExceptionHandlers(byte[] newCode) {
        final CodeAttribute codeAttribute = verifier.codeAttribute();
        final ExceptionHandlerEntry[] oldHandlers = codeAttribute.exceptionHandlerTable();
        if (oldHandlers.length == 0) {
            return oldHandlers;
        }

        SortedSet<ExceptionHandlerEntry> newHandlers = new TreeSet<ExceptionHandlerEntry>(new Comparator<ExceptionHandlerEntry>() {
            public int compare(ExceptionHandlerEntry o1, ExceptionHandlerEntry o2) {
                return o1.startPosition() - o2.startPosition();
            }
        });

        for (ExceptionHandlerEntry oldHandler : oldHandlers) {
            // For each instruction handle that maps to this handler, match it to all instructions that go to the handler.
            for (InstructionHandle handlerHandle = instructionMap[oldHandler.handlerPosition()]; handlerHandle != null; handlerHandle = handlerHandle.next) {
                // Find all instructions that go to this handler
                boolean lastMatch = false;
                ExceptionHandlerEntry currentHandler = null;
                for (InstructionHandle instructionHandle : instructionHandles) {
                    final Instruction instruction = instructionHandle.instruction;
                    final int position = instruction.position();
                    if (instructionHandle.flag != SKIP) {
                        final boolean match =
                            (position >= oldHandler.startPosition()) &&
                            (position < oldHandler.endPosition()) &&
                            instructionHandle.subroutineCall.canGoto(handlerHandle.subroutineCall);
                        if (match && !lastMatch) {
                            // start a new catch frame
                            currentHandler = new ExceptionHandlerEntry(instructionHandle.position, oldHandler.endPosition(),
                                handlerHandle.position, oldHandler.catchTypeIndex());
                            lastMatch = true;
                        } else if (lastMatch && !match) {
                            currentHandler = currentHandler.changeEndPosition(instructionHandle.position);
                            newHandlers.add(currentHandler);
                            lastMatch = false;
                        }
                    }
                }
                if (lastMatch) {
                    assert !newHandlers.contains(currentHandler);
                    // code end is still in the catch frame
                    currentHandler = currentHandler.changeEndPosition(newCode.length);
                    newHandlers.add(currentHandler);
                }
            }
        }

        return newHandlers.toArray(new ExceptionHandlerEntry[newHandlers.size()]);
    }

    private LineNumberTable fixupLineNumberTable() {
        final CodeAttribute codeAttribute = verifier.codeAttribute();
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
        final List<LineNumberTable.Entry> newEntries = new ArrayList<LineNumberTable.Entry>();
        for (InstructionHandle instructionHandle : instructionHandles) {
            if (instructionHandle.flag != SKIP) {
                final Instruction instruction = instructionHandle.instruction;
                final int nextLineNumber = oldPositionToLineNumberMap[instruction.position()];
                if (nextLineNumber != currentLineNumber) {
                    final LineNumberTable.Entry entry = new LineNumberTable.Entry((char) instructionHandle.position, (char) nextLineNumber);
                    newEntries.add(entry);
                    currentLineNumber = nextLineNumber;
                }
            }
        }

        return new LineNumberTable(newEntries.toArray(new LineNumberTable.Entry[newEntries.size()]));
    }

    private LocalVariableTable fixupLocalVariableTable() {
        final CodeAttribute codeAttribute = verifier.codeAttribute();
        final LocalVariableTable localVariableTable = codeAttribute.localVariableTable();
        if (localVariableTable.isEmpty()) {
            return LocalVariableTable.EMPTY;
        }
        final ArrayList<LocalVariableTable.Entry> newEntries = new ArrayList<LocalVariableTable.Entry>();
        final LocalVariableTable.Entry[] entries = localVariableTable.entries();
        for (LocalVariableTable.Entry entry : entries) {
            final int startPc = entry.startPosition();
            final int endPc = startPc + entry.length(); // inclusive
            InstructionHandle lastMatchedHandle = null;

            int lastMatchedStartPc = -1;
            for (InstructionHandle instructionHandle : instructionHandles) {
                if (instructionHandle.flag != SKIP) {
                    final Instruction instruction = instructionHandle.instruction;
                    final boolean matches = instruction.position() >= startPc && instruction.position() <= endPc;
                    if (lastMatchedHandle == null && matches) {
                        lastMatchedStartPc = instructionHandle.position;
                        lastMatchedHandle = instructionHandle;
                    } else if (lastMatchedHandle != null && !matches) {
                        final LocalVariableTable.Entry newEntry =
                            new LocalVariableTable.Entry((char) lastMatchedStartPc,
                                                         (char) (lastMatchedHandle.position - lastMatchedStartPc),
                                                         (char) entry.slot(),
                                                         (char) entry.nameIndex(),
                                                         (char) entry.descriptorIndex(),
                                                         (char) entry.signatureIndex());
                        newEntries.add(newEntry);
                        lastMatchedHandle = null;
                    }
                }
            }
            if (lastMatchedHandle != null) {
                final LocalVariableTable.Entry newEntry =
                    new LocalVariableTable.Entry((char) lastMatchedStartPc,
                                    (char) (lastMatchedHandle.position - lastMatchedStartPc),
                                    (char) entry.slot(),
                                    (char) entry.nameIndex(),
                                    (char) entry.descriptorIndex(),
                                    (char) entry.signatureIndex());
                newEntries.add(newEntry);
            }
        }

        return new LocalVariableTable(newEntries);
    }
}
