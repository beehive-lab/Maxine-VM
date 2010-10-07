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
package com.sun.max.tele.method;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Disassembler for machine code in the VM.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
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
    public static List<TargetCodeInstruction> decode(Platform platform, Address codeStart, byte[] code, byte[] encodedInlineDataDescriptors) {
        final Disassembler disassembler = createDisassembler(platform, codeStart, InlineDataDecoder.createFrom(encodedInlineDataDescriptors));
        final LoadLiteralParser literalParser = createLiteralParser(platform, disassembler, codeStart, code);
        return create(codeStart, code, disassembler, literalParser);
    }

    // Synchronize on class to avoid use of the disassembler before the initial call made during initialization.
    // This might be tidier if not all static.
    private static synchronized Disassembler createDisassembler(final Platform platform, Address startAddress, InlineDataDecoder inlineDataDecoder) {
        return Disassembler.createDisassembler(platform.instructionSet(), platform.wordWidth(), startAddress.toLong(), inlineDataDecoder);
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

    /**
     * On SPARC, literals are accessed using a base register that is set in the prologue of each method that uses literal values.
     * The base register is set using the "read pc" instruction, which can be at various position in the method prologue, depending on the size
     * of the sequence of instructions that build the method's frame. All literal addresses are computed relative to the pc of this instruction, so the
     * literal parser has first to locate that instruction. The prologue is also place differently depending on whether the method is JITed or optimized.
     *
     */
    private static class SPARCLoadLiteralParser extends  LoadLiteralParser {
        private static final int FRAME_ADAPTER_PROLOGUE = 8;

        /**
         * Minimal offset to the instruction that set the literal base from the optimized entry point.
         * Prologue is at least two instructions: a stack banging load and a save. The read pc instruction
         * is the last instruction of the prologue.
         */
        private static final int OPT_MIN_OFFSET_TO_RD_PC = 8;

        /**
         * Maximum size of a optimized code frame builder (up to 5 instructions for optimized code, up to 8 for jited code).
         */
        private static final int OPT_MAX_FRAME_BUILDER_SIZE = 20;

        /**
         * Maximum size of a jited code frame builder (up to 8 for jited code).
         */
        private static final int JIT_MAX_FRAME_BUILDER_SIZE = 32;

        private static final int JIT_MIN_OFFSET_TO_RD_PC = 12;

        private static final int SET_LITERAL_BASE_INSTRUCTION;

        private static final int NOP_TEMPLATE;

        private static final int FLUSHW_TEMPLATE;

        /**
         * Offset to the rdpc instruction that sets the literal base from the flushw instruction of the trapStub.
         */
        private static final int TRAP_STUB_RDPC_OFFSET = 232;

        private static final int DISP19_MASK = 0x7ffff;

        private static final Endianness ENDIANNESS =  Platform.platform().endianness();
        static {
            final Endianness endianness = platform().endianness();
            final SPARCAssembler asm =  SPARCAssembler.createAssembler(platform().wordWidth());
            final GPR literalBaseRegister = (GPR) vmConfig().targetABIsScheme().optimizedJavaABI.literalBaseRegister();

            asm.rd(StateRegister.PC, literalBaseRegister);
            int setLiteralBaseInstruction = 0;
            int nopTemplate = 0;
            int flushwTemplate = 0;
            try {
                setLiteralBaseInstruction = endianness.readInt(new ByteArrayInputStream(asm.toByteArray()));
                asm.reset();
                asm.nop();
                nopTemplate = endianness.readInt(new ByteArrayInputStream(asm.toByteArray()));
                asm.reset();
                asm.flushw();
                flushwTemplate = endianness.readInt(new ByteArrayInputStream(asm.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AssemblyException e) {
                e.printStackTrace();
            }
            SET_LITERAL_BASE_INSTRUCTION = setLiteralBaseInstruction;
            NOP_TEMPLATE = nopTemplate;
            FLUSHW_TEMPLATE = flushwTemplate;
        }

        private static boolean isTrapStub(byte [] code) throws IOException {
            if (code.length > TRAP_STUB_RDPC_OFFSET) {
                final ByteArrayInputStream in = new ByteArrayInputStream(code, 0, code.length);
                in.skip(8);
                if (ENDIANNESS.readInt(in) == FLUSHW_TEMPLATE) {
                    in.reset();
                    in.skip(TRAP_STUB_RDPC_OFFSET);
                    if (ENDIANNESS.readInt(in) == SET_LITERAL_BASE_INSTRUCTION) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static int searchLiteralBaseInstruction(int start, int end, byte [] code) {
            final int codeSize = ((end > code.length) ? code.length : end) - start;
            if (codeSize > 0) {
                try {
                    final ByteArrayInputStream in = new ByteArrayInputStream(code, start, codeSize);
                    int baseOffset = start;
                    int numInstructions = codeSize / 4;
                    while (numInstructions > 0) {
                        if (ENDIANNESS.readInt(in) == SET_LITERAL_BASE_INSTRUCTION) {
                            return baseOffset;
                        }
                        baseOffset += 4;
                        numInstructions--;
                    }
                    // not found. Might the trapStub.
                    if (isTrapStub(code)) {
                        return TRAP_STUB_RDPC_OFFSET;
                    }
                    // The code may be JITed.
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return -1;
        }

        private static int literalBaseFromCodeStart(byte [] code) {
            assert code.length % 4 == 0;
            try {
                final int firstInstruction = ENDIANNESS.readInt(new ByteArrayInputStream(code, 0, 4));
                if (firstInstruction != NOP_TEMPLATE) {
                    // First instruction is a branch to the effective JIT entry point.
                    // If the code was compiled by the JIT, it branches to the method entry prologue whose the
                    // fourth instruction is the rdpc.
                    // Note, this may raise a exception if the method doesn't have a JIT entry point. We ignore them and fall back to the
                    // optimized code search strategy that follows.
                    final int disp19 = firstInstruction & DISP19_MASK;
                    final int start = (disp19 << 2) + JIT_MIN_OFFSET_TO_RD_PC;
                    final int end = start + JIT_MAX_FRAME_BUILDER_SIZE;
                    final int offsetToReadPC = searchLiteralBaseInstruction(start, end, code);
                    if (offsetToReadPC > 0) {
                        return offsetToReadPC;
                    }
                }
            } catch (IOException e) {
                // This wasn't a JITed method. Let search the rdpc instruction in the optimized method prologue.
            }

            // Optimized method case
            final int start = OPT_MIN_OFFSET_TO_RD_PC;
            final int end = start + OPT_MAX_FRAME_BUILDER_SIZE + FRAME_ADAPTER_PROLOGUE;
            return searchLiteralBaseInstruction(start, end, code);
        }

        SPARCLoadLiteralParser(Disassembler disassembler, Address codeStart, byte [] code) {
            super(disassembler, codeStart.plus(literalBaseFromCodeStart(code)));
        }

        @Override
        boolean loadsLiteralData(DisassembledInstruction disassembledInstruction) {
            if (disassembledInstruction.arguments().size() == 3 &&
                            Utils.first(disassembledInstruction.arguments()) == GPR.L7 &&
                            disassembledInstruction.arguments().get(1) instanceof Immediate32Argument) {
                return true;
            }
            return false;
        }
        @Override
        Address literalAddress(DisassembledInstruction disassembledInstruction) {
            final Immediate32Argument immediateArgument = (Immediate32Argument) disassembledInstruction.arguments().get(1);
            return literalBase.plus(immediateArgument.value());
        }
    }

    private static LoadLiteralParser createLiteralParser(final Platform platform, Disassembler disassembler, Address codeStart, byte [] code) {
        //final ProcessorKind processorKind = teleVM.vmConfiguration().platform().processorKind();
        switch (platform.instructionSet()) {
            case ARM:
                TeleError.unimplemented();
                return null;
            case AMD64: {
                return new AMD64LoadLiteralParser(disassembler, codeStart);
            }
            case IA32:
                TeleError.unimplemented();
                return null;
            case PPC:
                TeleError.unimplemented();
                return null;
            case SPARC: {
                return new SPARCLoadLiteralParser(disassembler, codeStart, code);
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
                    objects.add((DisassembledObject) disassembler.scanOne(bufferedInputStream).get(0));
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
