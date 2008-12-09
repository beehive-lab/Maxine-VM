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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.sun.max.asm.AssemblyException;
import com.sun.max.asm.InlineDataDecoder;
import com.sun.max.asm.amd64.AMD64GeneralRegister64;
import com.sun.max.asm.dis.DisassembledData;
import com.sun.max.asm.dis.DisassembledInstruction;
import com.sun.max.asm.dis.DisassembledLabel;
import com.sun.max.asm.dis.DisassembledObject;
import com.sun.max.asm.dis.Disassembler;
import com.sun.max.asm.dis.amd64.AMD64DisassembledInstruction;
import com.sun.max.asm.dis.amd64.AMD64Disassembler;
import com.sun.max.asm.dis.ia32.IA32Disassembler;
import com.sun.max.asm.dis.ppc.PPC32Disassembler;
import com.sun.max.asm.dis.ppc.PPC64Disassembler;
import com.sun.max.asm.dis.sparc.SPARC32Disassembler;
import com.sun.max.asm.dis.sparc.SPARC64Disassembler;
import com.sun.max.asm.dis.sparc.SPARCDisassembledInstruction;
import com.sun.max.asm.gen.Immediate32Argument;
import com.sun.max.asm.gen.ImmediateArgument;
import com.sun.max.asm.gen.cisc.amd64.AMD64Template;
import com.sun.max.asm.gen.cisc.x86.X86OffsetParameter;
import com.sun.max.asm.gen.cisc.x86.X86Template;
import com.sun.max.asm.gen.cisc.x86.X86TemplateContext;
import com.sun.max.asm.gen.risc.sparc.SPARCTemplate;
import com.sun.max.asm.sparc.GPR;
import com.sun.max.asm.sparc.StateRegister;
import com.sun.max.asm.sparc.complete.SPARCAssembler;
import com.sun.max.collect.AppendableIndexedSequence;
import com.sun.max.collect.ArrayListSequence;
import com.sun.max.collect.IndexedSequence;
import com.sun.max.lang.Endianness;
import com.sun.max.lang.StaticLoophole;
import com.sun.max.lang.WordWidth;
import com.sun.max.platform.ProcessorKind;
import com.sun.max.program.Problem;
import com.sun.max.program.ProgramError;
import com.sun.max.program.ProgramWarning;
import com.sun.max.program.Trace;
import com.sun.max.tele.TeleVM;
import com.sun.max.unsafe.Address;
import com.sun.max.vm.VMConfiguration;

/**
 * Disassembler for machine code in the target VM.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TeleDisassembler {
    
    private static final int TRACE_VALUE = 1;
        
    private static final String _tracePrefix = "[TeleDisassembler] ";

    private TeleDisassembler() {
    }

    public static void initialize(final TeleVM teleVM) {
        final Thread thread = new Thread() {
            @Override
            public void run() {
                Trace.begin(TRACE_VALUE, _tracePrefix + "initializing");
                createDisassembler(teleVM, Address.zero(), null);
                Trace.end(TRACE_VALUE, _tracePrefix + "initializing");
            }
        };
        thread.start();
    }

    public static IndexedSequence<TargetCodeInstruction> decode(TeleVM teleVM, Address codeStart, byte[] code, byte[] encodedInlineDataDescriptors) {
        final Disassembler disassembler = createDisassembler(teleVM, codeStart, InlineDataDecoder.createFrom(encodedInlineDataDescriptors));
		final LoadLiteralParser literalParser = createLiteralParser(teleVM, disassembler, codeStart, code);
		return create(codeStart, code, disassembler, literalParser);
    }

    private static Disassembler createDisassembler(final TeleVM teleVM, Address startAddress, InlineDataDecoder inlineDataDecoder) {
        final ProcessorKind processorKind = teleVM.vmConfiguration().platform().processorKind();
        switch (processorKind.instructionSet()) {
            case ARM:
                Problem.unimplemented();
                return null;
            case AMD64:
                return new AMD64Disassembler(startAddress.toLong(), inlineDataDecoder);
            case IA32:
                return new IA32Disassembler(startAddress.toInt(), inlineDataDecoder);
            case PPC:
                if (processorKind.dataModel().wordWidth() == WordWidth.BITS_64) {
                    return new PPC64Disassembler(startAddress.toLong(), inlineDataDecoder);
                }
                return new PPC32Disassembler(startAddress.toInt(), inlineDataDecoder);
            case SPARC:
                if (processorKind.dataModel().wordWidth() == WordWidth.BITS_64) {
                    return new SPARC64Disassembler(startAddress.toLong(), inlineDataDecoder);
                }
                return new SPARC32Disassembler(startAddress.toInt(), inlineDataDecoder);
        }
        ProgramError.unknownCase();
        return null;
    }

    abstract static class LoadLiteralParser {
        protected Disassembler _disassembler;
        protected Address _literalBase;
        LoadLiteralParser(Disassembler disassembler, Address literalBase) {
            _disassembler = disassembler;
            _literalBase = literalBase;
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

    static class AMD64LoadLiteralParser extends LoadLiteralParser {
        AMD64LoadLiteralParser(Disassembler<AMD64Template, AMD64DisassembledInstruction> disassembler, Address codeStart) {
            super(disassembler, codeStart);
        }
        @Override
        boolean loadsLiteralData(DisassembledInstruction disassembledInstruction) {
            if (disassembledInstruction.arguments().length() == 2 &&
                            disassembledInstruction.arguments().first() instanceof AMD64GeneralRegister64 &&
                            disassembledInstruction.template().operands().last() instanceof X86OffsetParameter &&
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
    static class SPARCLoadLiteralParser extends  LoadLiteralParser {
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

        private static final Endianness ENDIANNESS =  VMConfiguration.target().platform().processorKind().dataModel().endianness();
        static {
            final Endianness endianness = VMConfiguration.target().platform().processorKind().dataModel().endianness();
            final SPARCAssembler asm =  SPARCAssembler.createAssembler(VMConfiguration.target().platform().processorKind().dataModel().wordWidth());
            final GPR literalBaseRegister = (GPR) VMConfiguration.target().targetABIsScheme().optimizedJavaABI().literalBaseRegister();

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

        SPARCLoadLiteralParser(Disassembler<SPARCTemplate, SPARCDisassembledInstruction> disassembler, Address codeStart, byte [] code) {
            super(disassembler, codeStart.plus(literalBaseFromCodeStart(code)));
        }

        @Override
        boolean loadsLiteralData(DisassembledInstruction disassembledInstruction) {
            if (disassembledInstruction.arguments().length() == 3 &&
                            disassembledInstruction.arguments().first() == GPR.L7 &&
                            disassembledInstruction.arguments().get(1) instanceof Immediate32Argument) {
                return true;
            }
            return false;
        }
        @Override
        Address literalAddress(DisassembledInstruction disassembledInstruction) {
            final Immediate32Argument immediateArgument = (Immediate32Argument) disassembledInstruction.arguments().get(1);
            return _literalBase.plus(immediateArgument.value());
        }
    }

    private static LoadLiteralParser createLiteralParser(final TeleVM teleVM, Disassembler rawDisassembler, Address codeStart, byte [] code) {
        final ProcessorKind processorKind = teleVM.vmConfiguration().platform().processorKind();
        switch (processorKind.instructionSet()) {
            case ARM:
                Problem.unimplemented();
                return null;
            case AMD64: {
                final Class<Disassembler<AMD64Template, AMD64DisassembledInstruction>> type = null;
                final Disassembler<AMD64Template, AMD64DisassembledInstruction> disassembler = StaticLoophole.cast(type, rawDisassembler);
                return new AMD64LoadLiteralParser(disassembler, codeStart);
            }
            case IA32:
                Problem.unimplemented();
                return null;
            case PPC:
                Problem.unimplemented();
                return null;
            case SPARC: {
                final Class<Disassembler<SPARCTemplate, SPARCDisassembledInstruction>> type = null;
                final Disassembler<SPARCTemplate, SPARCDisassembledInstruction> disassembler = StaticLoophole.cast(type, rawDisassembler);
                return new SPARCLoadLiteralParser(disassembler, codeStart, code);
            }
        }
        ProgramError.unknownCase();
        return null;
    }

    private static IndexedSequence<TargetCodeInstruction> create(
                    Address codeStart,
                    byte[] code,
                    Disassembler disassembler,
                    LoadLiteralParser literalParser) {

        IndexedSequence<DisassembledObject> disassembledObjects;
        try {
        	final Class<IndexedSequence<DisassembledObject>> type = null;
            disassembledObjects = StaticLoophole.cast(type, disassembler.scan(new BufferedInputStream(new ByteArrayInputStream(code))));
        } catch (Throwable throwable) {
            ProgramWarning.message("Could not completely disassemble given code stream - trying partial disassembly instead [error: " + throwable + "]");
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(code));
            final AppendableIndexedSequence<DisassembledObject> objects = new ArrayListSequence<DisassembledObject>();
            try {
            	while (bufferedInputStream.available() > 0) {
            		objects.append((DisassembledObject) disassembler.scanOne(bufferedInputStream).first());
            	}
            } catch (Throwable t) {
                ProgramWarning.message("Only partially disassembled given code stream [error: " + t + "]");
            }
            disassembledObjects = objects;
        }

        final AppendableIndexedSequence<TargetCodeInstruction> targetCodeInstructions = new ArrayListSequence<TargetCodeInstruction>(disassembledObjects.length());

        for (DisassembledObject disassembledObject : disassembledObjects) {
        	final DisassembledLabel label = disassembler.addressMapper().labelAt(disassembledObject);
        	final TargetCodeInstruction targetCodeInstruction;
        	if (disassembledObject instanceof DisassembledInstruction) {
        		final DisassembledInstruction disassembledInstruction = (DisassembledInstruction) disassembledObject;

        		final String operandsText = disassembledInstruction.operandsToString(disassembler.addressMapper());
        		final Address targetAddress;
        		final Address literalSourceAddress;
        		if (disassembledInstruction.arguments().length() == 1 && disassembledInstruction.arguments().first() instanceof ImmediateArgument &&
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
    		targetCodeInstructions.append(targetCodeInstruction);
        }
        return targetCodeInstructions;
    }
}
