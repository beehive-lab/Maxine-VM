/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.dis.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Utility to enhance the disassembler output for T1X cmpiled code.
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public class T1XDisassemblyPrinter extends DisassemblyPrinter {

    /**
     * Map from machine code positions to bytecode disassembly for the bytecode instruction
     * whose translation starts at the code position.
     */
    private final String[] posToBCode;

    private final HashMap<Integer, String> stopInfo = new HashMap<Integer, String>();
    private final HashMap<Integer, CiDebugInfo> stopDebugInfo = new HashMap<Integer, CiDebugInfo>();
    private String currentStopInfo;


    public T1XDisassemblyPrinter(T1XTargetMethod method) {
        super(false);

        if (method.stopPositions() != null) {
            StopPositions stopPositions = new StopPositions(method.stopPositions());
            Object[] directCallees = method.directCallees();

            int frameRefMapSize = method.frameRefMapSize;
            int totalRefMapSize = method.totalRefMapSize();
            int regRefMapSize = T1XTargetMethod.regRefMapSize();

            for (int stopIndex = 0; stopIndex < stopPositions.length(); ++stopIndex) {
                int pos = stopPositions.get(stopIndex);

                CiBitMap frameRefMap = new CiBitMap(method.referenceMaps(), stopIndex * totalRefMapSize, frameRefMapSize);
                CiBitMap regRefMap = new CiBitMap(method.referenceMaps(), (stopIndex * totalRefMapSize) + frameRefMapSize, regRefMapSize);

                CiDebugInfo debugInfo = new CiDebugInfo(null, regRefMap, frameRefMap);
                stopDebugInfo.put(pos, debugInfo);


                if (stopIndex < method.numberOfDirectCalls()) {
                    Object callee = directCallees[stopIndex];
                    stopInfo.put(pos, String.valueOf(callee));
                } else if (stopIndex < method.numberOfDirectCalls() + method.numberOfIndirectCalls()) {
                    CiCodePos codePos = method.getBytecodeFrames(stopIndex);
                    if (codePos != null) {
                        byte[] code = method.codeAttribute.code();
                        int bci = codePos.bci;
                        byte opcode = code[bci];
                        if (opcode == INVOKEINTERFACE || opcode == INVOKESPECIAL || opcode == INVOKESTATIC || opcode == INVOKEVIRTUAL) {
                            int cpi = Bytes.beU2(code, bci + 1);
                            RiMethod callee = vm().runtime.getConstantPool(codePos.method).lookupMethod(cpi, opcode);
                            stopInfo.put(pos, String.valueOf(callee));
                        }
                    }
                } else {
                    stopInfo.put(pos, "safepoint");
                }
            }
        }

        posToBCode = new String[method.code().length];
        int[] bciToPos = method.bciToPos;
        BytecodeStream s = new BytecodeStream(method.codeAttribute.code());
        for (int bci = 0; bci < bciToPos.length; ++bci) {
            int pos = bciToPos[bci];
            if (pos != 0) {
                StringBuilder sb = new StringBuilder(MIN_OPCODE_LINE_LENGTH);
                if (bci != bciToPos.length - 1) {
                    sb.append(";; -------------------- ");
                    s.setBCI(bci);
                    sb.append(bci).append(": ").append(Bytecodes.nameOf(s.currentBC()));
                    for (int i = bci + 1; i < s.nextBCI(); ++i) {
                        sb.append(' ').append(s.readUByte(i));
                    }
                    sb.append(" --------------------");
                } else {
                    sb.append(";; -------------------- <epilogue> --------------------");
                }
                while (sb.length() < MIN_OPCODE_LINE_LENGTH) {
                    sb.append('-');
                }
                posToBCode[pos] = sb.toString();
            }
        }
    }

    private static final int MIN_OPCODE_LINE_LENGTH = 100;

    @Override
    protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
        String string = super.disassembledObjectString(disassembler, disassembledObject);
        if (currentStopInfo != null) {
            String extra = currentStopInfo;
            if (extra != null) {
                string += " " + extra;
            }
        }
        return string;
    }

    @Override
    protected void printDisassembledObject(Disassembler disassembler, PrintStream stream, int nOffsetChars, int nLabelChars, DisassembledObject disassembledObject) {
        int pos = disassembledObject.startPosition();
        currentStopInfo = stopInfo.get(pos);
        if (pos < posToBCode.length && posToBCode[pos] != null) {
            stream.println(posToBCode[pos]);
        }
        CiDebugInfo info = stopDebugInfo.get(pos);
        if (info != null) {
            CiTarget target = target();
            CiArchitecture arch = target.arch;
            if (info.hasRegisterRefMap()) {
                stream.print(";;   reg-ref-map:");
                CiBitMap bm = info.registerRefMap;
                for (int reg = bm.nextSetBit(0); reg >= 0; reg = bm.nextSetBit(reg + 1)) {
                    stream.print(" " + arch.registers[reg]);
                }
                stream.println(" " + bm);
            }
            if (info.hasStackRefMap()) {
                stream.print(";; frame-ref-map:");
                CiBitMap bm = info.frameRefMap;
                for (int i = bm.nextSetBit(0); i >= 0; i = bm.nextSetBit(i + 1)) {
                    stream.print(" +" + i * JVMSFrameLayout.JVMS_SLOT_SIZE);
                }
                stream.println(" " + bm);
            }
        }
        super.printDisassembledObject(disassembler, stream, nOffsetChars, nLabelChars, disassembledObject);
    }
}
