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
/*VCSID=aaec3e3f-3f5f-4db2-b862-db517014618c*/
package com.sun.max.asm.dis.amd64;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.dis.x86.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.amd64.*;
import com.sun.max.asm.x86.*;
import com.sun.max.collect.*;

/**
 * Disassembles "switch" constructs from the AMD64 compiler back-end.
 *
 * @see com.sun.max.vm.compiler.eir.amd64.graph.AMD64EirInstructions.SWITCH_I32
 * @author David Liu
 */
public class AMD64SwitchDisassembler implements X86DisassembledInstructionScanner<AMD64Template, AMD64DisassembledInstruction> {
    public AMD64SwitchDisassembler(AMD64Disassembler disassembler) {
        _disassembler = disassembler;
    }

    private AMD64Disassembler _disassembler;

    private enum ScanState {
        STARTED,
        JNB,
        LEA,
        MOV,
        ADD,
        JMP
    }

    private ScanState _scanState;
    private int _lowerBound;
    private int _numEntries;
    private AMD64GeneralRegister64 _switchBaseReg;
    private AMD64GeneralRegister64 _switchIndexReg;
    private AMD64GeneralRegister64 _switchOffsetReg;
    private int _switchTableOffset;
    private Map<Integer, Integer> _switchTableEntries = new HashMap<Integer, Integer>();
    private AMD64Template _switchLabelTemplate = new AMD64TemplateCreator().createSwitchLabelTemplate();

    public void disassemblyStarted() {
        _scanState = ScanState.STARTED;
        _lowerBound = 0;
        _numEntries = 0;
        _switchTableOffset = -1;
        _switchBaseReg = null;
        _switchIndexReg = null;
        _switchTableEntries.clear();
    }

    public AMD64DisassembledInstruction overrideDisassembly(int currentPosition, BufferedInputStream stream) throws IOException {
        if (_switchTableEntries.containsKey(currentPosition)) {
            final int switchKey = _switchTableEntries.get(currentPosition);
            final int numberOfBytesInOffset = 4;
            final byte[] labelBytes = new byte[numberOfBytesInOffset];
            for (int i = 0; i < labelBytes.length; i++) {
                labelBytes[i] = (byte) stream.read();
            }
            final int labelTargetOffset = (labelBytes[0] & 0xFF) | (labelBytes[1] << 8) | (labelBytes[2] << 16) | (labelBytes[3] << 24);
            final AppendableIndexedSequence<Argument> arguments = new ArrayListSequence<Argument>();
            arguments.append(new Immediate32Argument(switchKey));
            arguments.append(new Immediate32Argument(_switchTableOffset + labelTargetOffset - currentPosition - numberOfBytesInOffset));
            final AMD64DisassembledInstruction disassembledInstruction = _disassembler.createDisassembledInstruction(currentPosition, labelBytes, _switchLabelTemplate, arguments);
            return disassembledInstruction;
        }

        return null;
    }

    public void instructionDisassembled(AMD64DisassembledInstruction disassembledInstruction) {
        final AMD64Template template = disassembledInstruction.template();
        switch (_scanState) {
            case STARTED:
                if (template.externalName().equals("subq") && template.operands().toString().equals("<AMD64GeneralRegister64, X86ImmediateParameter>")) {
                    _switchIndexReg = (AMD64GeneralRegister64) disassembledInstruction.arguments().get(0);
                    _lowerBound = (int) disassembledInstruction.arguments().get(1).asLong();
                } else if (template.externalName().equals("cmpq") && template.operands().toString().equals("<AMD64GeneralRegister64, X86ImmediateParameter>")) {
                    if (_switchIndexReg != null && !_switchIndexReg.equals(disassembledInstruction.arguments().get(0))) {
                        _lowerBound = 0;
                    }
                    _switchIndexReg = (AMD64GeneralRegister64) disassembledInstruction.arguments().get(0);
                    _numEntries = (int) disassembledInstruction.arguments().get(1).asLong();
                    _scanState = ScanState.JNB;
                } else {
                    break;
                }
                return;
            case JNB:
                if (template.externalName().equals("jnb") && template.operands().toString().equals("<X86OffsetParameter>")) {
                    _scanState = ScanState.LEA;
                    return;
                }
                break;
            case LEA:
                if (template.externalName().equals("lea") && template.operands().toString().equals("<AMD64GeneralRegister64, X86OffsetParameter>")) {
                    _switchBaseReg = (AMD64GeneralRegister64) disassembledInstruction.arguments().first();
                    _switchTableOffset = disassembledInstruction.endPosition() + (int) disassembledInstruction.arguments().last().asLong();
                    _scanState = ScanState.MOV;
                    return;
                }
                break;
            case MOV:
                if (template.externalName().equals("movsxd") && template.operands().toString().equals("<AMD64GeneralRegister64, AMD64BaseRegister64, AMD64IndexRegister64, Scale>")) {
                    if (!AMD64BaseRegister64.from(_switchBaseReg).equals(disassembledInstruction.arguments().get(1)) ||
                         !AMD64IndexRegister64.from(_switchIndexReg).equals(disassembledInstruction.arguments().get(2)) ||
                         !Scale.SCALE_4.equals(disassembledInstruction.arguments().get(3))) {
                        break;
                    }
                    _switchOffsetReg = (AMD64GeneralRegister64) disassembledInstruction.arguments().get(0);
                    _scanState = ScanState.ADD;
                    return;
                }
                break;
            case ADD:
                if (template.externalName().equals("add") && template.operands().toString().equals("<AMD64GeneralRegister64, AMD64GeneralRegister64>")) {
                    if (!AMD64GeneralRegister64.from(_switchBaseReg).equals(disassembledInstruction.arguments().get(0)) || !AMD64GeneralRegister64.from(_switchOffsetReg).equals(disassembledInstruction.arguments().get(1))) {
                        break;
                    }
                    _scanState = ScanState.JMP;
                    return;
                }
                break;
            case JMP:
                if (template.externalName().equals("jmp") && template.operands().toString().equals("<AMD64GeneralRegister64>")) {
                    if (!_switchBaseReg.equals(disassembledInstruction.arguments().get(0))) {
                        break;
                    }
                    for (int switchKey = _lowerBound; switchKey < _lowerBound + _numEntries; switchKey++) {
                        _switchTableEntries.put(_switchTableOffset + (switchKey - _lowerBound) * 4, switchKey);
                    }
                }
                break;
        }

        _scanState = ScanState.STARTED;
        _lowerBound = 0;
        _switchIndexReg = null;
    }
}
