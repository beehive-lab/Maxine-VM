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

import static com.sun.max.asm.dis.Disassembler.*;

import java.io.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.BytecodeLocation;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Representation of a method compilation in the VM.
 * Much of the information is derived by delegation to
 * a surrogate for the corresponding instance of {@link TargetMethod}
 * in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleCompiledMethod extends TeleCompiledCode {

    /**
     * Description of a compiled method region allocated in a code cache.
     * <br>
     * The parent of this region is the {@link MaxCompiledCodeRegion} in which it is created.
     * <br>
     * This region has no children, unless we decide later to subdivide and model the parts separately.
     *
     * @author Michael Van De Vanter
     */
    private static final class CompiledMethodMemoryRegion extends TeleDelegatedMemoryRegion implements MaxEntityMemoryRegion<MaxCompiledCode> {

        private static final IndexedSequence<MaxEntityMemoryRegion<? extends MaxEntity>> EMPTY =
            new ArrayListSequence<MaxEntityMemoryRegion<? extends MaxEntity>>(0);

        private final TeleCompiledMethod owner;
        private final boolean isBootCode;
        private final MaxEntityMemoryRegion<? extends MaxEntity> parent;

        private CompiledMethodMemoryRegion(TeleVM teleVM, TeleCompiledMethod owner, TeleTargetMethod teleTargetMethod, TeleCompiledCodeRegion teleCompiledCodeRegion, boolean isBootCode) {
            super(teleVM, teleTargetMethod);
            ProgramError.check(teleCompiledCodeRegion != null);
            this.owner = owner;
            this.isBootCode = isBootCode;
            this.parent = teleCompiledCodeRegion.memoryRegion();
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            return parent;
        }

        public IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxCompiledCode owner() {
            return owner;
        }

        public boolean isBootRegion() {
            return isBootCode;
        }
    }

    /**
     * Adapter for bytecode scanning that only knows the constant pool
     * index argument of the last method invocation instruction scanned.
     */
    private static final class MethodRefIndexFinder extends BytecodeAdapter  {
        int methodRefIndex = -1;

        public MethodRefIndexFinder reset() {
            methodRefIndex = -1;
            return this;
        }

        @Override
        protected void invokestatic(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            methodRefIndex = index;
        }

        public int methodRefIndex() {
            return methodRefIndex;
        }
    };

    private InstructionMap instructionMap = null;

    private final TeleTargetMethod teleTargetMethod;

    /**
     * Which number is this in the sequence of compilations for the method.
     */
    private final int compilationIndex;

    private CodeLocation codeStartLocation = null;
    private final CompiledMethodMemoryRegion compiledMethodMemoryRegion;
    private IndexedSequence<MachineCodeLocation> instructionLocations;

    /**
     * Creates an object that describes a region of VM memory used to hold a single compiled method.
     *
     * @param teleVM the VM
     * @param teleTargetMethod surrogate for the method compilation in the VM
     * @param teleCompiledCodeRegion surrogate for the code cache's allocation region in which this is created
     * @param isBootCode is this code in the boot region?
     */
    public TeleCompiledMethod(TeleVM teleVM, TeleTargetMethod teleTargetMethod, TeleCompiledCodeRegion teleCompiledCodeRegion, boolean isBootCode) {
        super(teleVM);
        this.teleTargetMethod = teleTargetMethod;
        this.compiledMethodMemoryRegion = new CompiledMethodMemoryRegion(teleVM, this, teleTargetMethod, teleCompiledCodeRegion, isBootCode);
        this.instructionMap = new CompiledMethodInstructionMap(teleVM, teleTargetMethod);
        final TeleClassMethodActor teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
        this.compilationIndex = teleClassMethodActor == null ? 0 : teleClassMethodActor.compilationIndexOf(teleTargetMethod);
    }

    public String entityName() {
        final ClassMethodActor classMethodActor = classMethodActor();
        if (classMethodActor != null) {
            return "Code " + classMethodActor.simpleName();
        }
        return teleTargetMethod.getRegionName();
    }

    public String entityDescription() {
        final ClassMethodActor classMethodActor = teleTargetMethod.classMethodActor();
        final String description = teleTargetMethod.getClass().getSimpleName() + " for method ";
        if (classMethodActor != null) {
            return description + classMethodActor.simpleName();
        }
        return description + teleTargetMethod.classActorForObjectType().simpleName();
    }

    public MaxEntityMemoryRegion<MaxCompiledCode> memoryRegion() {
        return compiledMethodMemoryRegion;
    }

    public boolean contains(Address address) {
        return compiledMethodMemoryRegion.contains(address);
    }

    public InstructionMap instructionMap() {
        return instructionMap;
    }

    public Address getCodeStart() {
        return teleTargetMethod.getCodeStart();
    }

    public CodeLocation getCodeStartLocation() {
        final Address codeStart = getCodeStart();
        if (codeStartLocation == null && codeStart != null) {
            codeStartLocation = codeManager().createMachineCodeLocation(codeStart, "code start location in method");
        }
        return codeStartLocation;
    }

    public Address getCallEntryPoint() {
        return teleTargetMethod.callEntryPoint();
    }

    public CodeLocation getCallEntryLocation() {
        final Address callEntryPoint = getCallEntryPoint();
        if (callEntryPoint.isZero()) {
            return null;
        }
        return codeManager().createMachineCodeLocation(callEntryPoint, "Method entry");
    }

    public int compilationIndex() {
        return compilationIndex;
    }

    public TeleClassMethodActor getTeleClassMethodActor() {
        return teleTargetMethod.getTeleClassMethodActor();
    }

    public ClassMethodActor classMethodActor() {
        final TeleClassMethodActor teleClassMethodActor = getTeleClassMethodActor();
        if (teleClassMethodActor != null) {
            return teleClassMethodActor.classMethodActor();
        }
        return null;
    }

    public ClassActor classActorForObjectType() {
        return teleTargetMethod.classActorForObjectType();
    }

    public String targetLocationToString(TargetLocation targetLocation) {
        final String framePointerRegisterName =
            TeleIntegerRegisters.symbolizer(vm().vmConfiguration()).fromValue(teleTargetMethod.getAbi().framePointer().value()).toString();

        switch (targetLocation.tag()) {
            case INTEGER_REGISTER: {
                final TargetLocation.IntegerRegister integerRegister = (TargetLocation.IntegerRegister) targetLocation;
                return TeleIntegerRegisters.symbolizer(vm().vmConfiguration()).fromValue(integerRegister.index()).toString();
            }
            case FLOATING_POINT_REGISTER: {
                final TargetLocation.FloatingPointRegister floatingPointRegister = (TargetLocation.FloatingPointRegister) targetLocation;
                return TeleFloatingPointRegisters.symbolizer(vm().vmConfiguration()).fromValue(floatingPointRegister.index()).toString();
            }
            case LOCAL_STACK_SLOT: {
                final TargetLocation.LocalStackSlot localStackSlot = (TargetLocation.LocalStackSlot) targetLocation;
                return framePointerRegisterName + "[" + (localStackSlot.index() * vm().wordSize().toInt()) + "]";
            }
            default: {
                return targetLocation.toString();
            }
        }
    }

    public TeleTargetMethod teleTargetMethod() {
        return teleTargetMethod;
    }

    public byte[] getCode() {
        return teleTargetMethod.getCode();
    }
    /**
     * Gets the name of the source variable corresponding to a stack slot, if any.
     *
     * @param slot a stack slot
     * @return the Java source name for the frame slot, null if not available.
     */
    public String sourceVariableName(MaxStackFrame.Compiled javaStackFrame, int slot) {
        return teleTargetMethod.sourceVariableName(javaStackFrame, slot);
    }

    public void writeSummary(PrintStream printStream) {
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(printStream));
        writer.println("target method: " + classMethodActor().format("%H.%n(%p)"));
        writer.println("compilation: " + compilationIndex);
        teleTargetMethod.disassemble(writer);
        writer.flush();
        final ProcessorKind processorKind = vm().vmConfiguration().platform().processorKind;
        final InlineDataDecoder inlineDataDecoder = InlineDataDecoder.createFrom(teleTargetMethod().encodedInlineDataDescriptors());
        final Address startAddress = getCodeStart();
        final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
            @Override
            protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                final String string = super.disassembledObjectString(disassembler, disassembledObject);
                if (string.startsWith("call ")) {
                    final BytecodeLocation bytecodeLocation = null; //_teleTargetMethod.getBytecodeLocationFor(startAddress.plus(disassembledObject.startPosition()));
                    if (bytecodeLocation != null) {
                        final MethodRefConstant methodRef = bytecodeLocation.getCalleeMethodRef();
                        if (methodRef != null) {
                            final ConstantPool pool = bytecodeLocation.classMethodActor.codeAttribute().constantPool;
                            return string + " [" + methodRef.holder(pool).toJavaString(false) + "." + methodRef.name(pool) + methodRef.signature(pool).toJavaString(false, false) + "]";
                        }
                    }
                }
                return string;
            }
        };
        disassemble(printStream, getCode(), processorKind.instructionSet, processorKind.dataModel.wordWidth, startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
    }

}
