/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import java.io.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Disassembler for machine code in the VM.
 */
public final class TeleDisassembler {

    private static final int TRACE_VALUE = 1;

    private static String tracePrefix() {
        return "[TeleDisassembler: thread=" + Thread.currentThread().getName() + "] ";
    }

    private TeleDisassembler() {
    }

    /**
     * Causes the disassembler to load and initialize static state
     * on a separate thread, since it can be time consuming.
     *
     * @param platform the kind of disassembler to initialize.
     */
    public static void initialize(final Platform platform) {
        final Thread thread = new Thread("TeleDisassembler initializer") {
            @Override
            public void run() {
                Trace.begin(TRACE_VALUE, tracePrefix() + "initializing");
                final long startTimeMillis = System.currentTimeMillis();
                createDisassembler(platform, Address.zero(), null);
                Trace.end(TRACE_VALUE, tracePrefix() + "initializing", startTimeMillis);
            }
        };
        thread.start();
    }

    /**
     * Disassembles a segment of compiled code.
     *
     * @param processorKind the kind of processor for which the code was compiled.
     * @param codeStart start location of the code in VM memory.
     * @param code the compiled code as bytes
     * @param encodedInlineDataDescriptors
     *
     * @return the code disassembled into instructions.
     */
    public static List<TargetCodeInstruction> decode(Platform platform, Address codeStart, byte[] code, InlineDataDecoder inlineDataDecoder) {
        final Disassembler disassembler = createDisassembler(platform, codeStart, inlineDataDecoder);
        final LoadLiteralParser literalParser = createLiteralParser(platform, disassembler, codeStart, code);
        return create(codeStart, code, disassembler, literalParser);
    }

    // Synchronize on class to avoid use of the disassembler before the initial call made during initialization.
    // This might be tidier if not all static.
    private static synchronized Disassembler createDisassembler(final Platform platform, Address startAddress, InlineDataDecoder inlineDataDecoder) {
        return Disassembler.createDisassembler(platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder);
    }

    private abstract static class LoadLiteralParser {
        protected Disassembler disassembler;
        protected Address literalBase;
        LoadLiteralParser(Disassembler disassembler, Address literalBase) {
            this.disassembler = disassembler;
            this.literalBase = literalBase;
        }

        /**
         * Gets a boolean indicating whether the specified disassembled instruction is an instruction that loads a
         * literal value.
         *
         * @param disassembledInstruction the disassembled instruction
         * @return true if the instruction loads a literal value, false otherwise.
         */
        abstract boolean loadsLiteralData(DisassembledInstruction disassembledInstruction);

        /**
         * Gets the address of the literal value loaded by an instruction.
         *
         * @param codeStart start of the code that contains the instruction that loads the literal value
         * @param disassembledInstruction the disassembled form of the instruction
         * @return the address where the literal value is stored
         */
        abstract Address literalAddress(DisassembledInstruction disassembledInstruction);
    }

    private static class AMD64LoadLiteralParser extends LoadLiteralParser {
        AMD64LoadLiteralParser(Disassembler disassembler, Address codeStart) {
            super(disassembler, codeStart);
        }
        @Override
        boolean loadsLiteralData(DisassembledInstruction disassembledInstruction) {
            if (disassembledInstruction.arguments().size() == 2 &&
                            Utils.first(disassembledInstruction.arguments()) instanceof AMD64GeneralRegister64 &&
                            Utils.last(disassembledInstruction.template().operands()) instanceof X86OffsetParameter &&
                            ((X86Template) disassembledInstruction.template()).addressSizeAttribute() == WordWidth.BITS_64 &&
                            ((X86Template) disassembledInstruction.template()).rmCase() == X86TemplateContext.RMCase.SDWORD) {
                return true;
            }
            return false;
        }
        @Override
        Address literalAddress(DisassembledInstruction disassembledInstruction) {
            final ImmediateArgument immediateArgument = (ImmediateArgument) disassembledInstruction.arguments().get(1);
            return Address.fromLong(disassembledInstruction.addressForRelativeAddressing().plus(immediateArgument).asLong());
        }
    }

    private static LoadLiteralParser createLiteralParser(final Platform platform, Disassembler disassembler, Address codeStart, byte [] code) {
        //final ProcessorKind processorKind = teleVM.vmConfiguration().platform().processorKind();
        switch (platform.isa) {
            case AMD64: {
                return new AMD64LoadLiteralParser(disassembler, codeStart);
            }
            case ARM:
            case PPC:
            case IA32:
            case SPARC: {
                throw TeleError.unimplemented();
            }
        }
        TeleError.unknownCase();
        return null;
    }

    private static List<TargetCodeInstruction> create(
                    Address codeStart,
                    byte[] code,
                    Disassembler disassembler,
                    LoadLiteralParser literalParser) {

        List<DisassembledObject> disassembledObjects;
        try {
            final Class<List<DisassembledObject>> type = null;
            disassembledObjects = Utils.cast(type, disassembler.scan(new BufferedInputStream(new ByteArrayInputStream(code))));
        } catch (Throwable throwable) {
            TeleWarning.message("Could not completely disassemble given code stream - trying partial disassembly instead", throwable);
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(code));
            final List<DisassembledObject> objects = new ArrayList<DisassembledObject>();
            try {
                while (bufferedInputStream.available() > 0) {
                    objects.add(disassembler.scanOne(bufferedInputStream).get(0));
                }
            } catch (Throwable t) {
                TeleWarning.message("Only partially disassembled given code stream", t);
            }
            disassembledObjects = objects;
        }

        final List<TargetCodeInstruction> targetCodeInstructions = new ArrayList<TargetCodeInstruction>(disassembledObjects.size());

        for (DisassembledObject disassembledObject : disassembledObjects) {
            final DisassembledLabel label = disassembler.addressMapper().labelAt(disassembledObject);
            final TargetCodeInstruction targetCodeInstruction;
            if (disassembledObject instanceof DisassembledInstruction) {
                final DisassembledInstruction disassembledInstruction = (DisassembledInstruction) disassembledObject;

                final String operandsText = disassembledInstruction.operandsToString(disassembler.addressMapper());
                final Address targetAddress;
                final Address literalSourceAddress;
                if (disassembledInstruction.arguments().size() == 1 && Utils.first(disassembledInstruction.arguments()) instanceof ImmediateArgument &&
                                (operandsText.contains("+") || operandsText.contains("-"))) {
                    targetAddress = Address.fromLong(disassembledInstruction.targetAddress().asLong());
                    literalSourceAddress = null;
                } else if (literalParser.loadsLiteralData(disassembledInstruction)) {
                    literalSourceAddress = literalParser.literalAddress(disassembledInstruction);
                    targetAddress = null;
                } else {
                    targetAddress = null;
                    literalSourceAddress = null;
                }
                targetCodeInstruction = new TargetCodeInstruction(
                                disassembledInstruction.mnemonic(),
                                codeStart.plus(disassembledInstruction.startPosition()),
                                disassembledInstruction.startPosition(),
                                label == null ? null : label.name(),
                                                disassembledInstruction.bytes(),
                                                operandsText,
                                                targetAddress,
                                                literalSourceAddress);
            } else {
                final DisassembledData disassembledData = (DisassembledData) disassembledObject;
                final String operandsText = disassembledData.operandsToString(disassembler.addressMapper());
                final ImmediateArgument dataTargetAddress = disassembledData.targetAddress();
                final Address targetAddress;
                if (dataTargetAddress != null) {
                    targetAddress = Address.fromLong(dataTargetAddress.asLong());
                } else {
                    targetAddress = null;
                }

                targetCodeInstruction = new TargetCodeInstruction(disassembledData.mnemonic(),
                                codeStart.plus(disassembledObject.startPosition()),
                                disassembledObject.startPosition(),
                                label == null ? null : label.name(),
                                                disassembledObject.bytes(),
                                                operandsText,
                                                targetAddress,
                                                null);
            }
            targetCodeInstructions.add(targetCodeInstruction);
        }
        return targetCodeInstructions;
    }
}
