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

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.CodeLocation.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
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

        private TeleCompiledMethod owner;
        private final boolean isBootCode;
        final MaxEntityMemoryRegion<? extends MaxEntity> parent;

        private CompiledMethodMemoryRegion(TeleVM teleVM, TeleCompiledMethod owner, TeleTargetMethod teleTargetMethod, TeleCompiledCodeRegion teleCompiledCodeRegion, boolean isBootCode) {
            super(teleVM, teleTargetMethod);
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


    private final TeleTargetMethod teleTargetMethod;
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
    }

    public String entityName() {
        final ClassMethodActor classMethodActor = teleTargetMethod.classMethodActor();
        if (classMethodActor != null) {
            return "Code " + classMethodActor.simpleName();
        }
        return "Unknown compiled code";
    }

    public String entityDescription() {
        final ClassMethodActor classMethodActor = teleTargetMethod.classMethodActor();
        final String description = teleTargetMethod.getClass().getSimpleName() + " for method ";
        if (classMethodActor != null) {
            return description + classMethodActor.simpleName();
        }
        return description + "Unknown compiled code";
    }

    public MaxEntityMemoryRegion<MaxCompiledCode> memoryRegion() {
        return compiledMethodMemoryRegion;
    }

    public boolean contains(Address address) {
        return compiledMethodMemoryRegion.contains(address);
    }

    public Address getCodeStart() {
        return teleTargetMethod.getCodeStart();
    }

    public Address callEntryPoint() {
        return teleTargetMethod.callEntryPoint();
    }

    public IndexedSequence<TargetCodeInstruction> getInstructions() {
        return teleTargetMethod.getInstructions();
    }

    public IndexedSequence<MachineCodeLocation> getInstructionLocations() {
        if (instructionLocations == null) {
            getInstructions();
            final int length = getInstructions().length();
            final CodeManager codeManager = codeManager();
            final VariableSequence<MachineCodeLocation> locations = new VectorSequence<MachineCodeLocation>(length);
            for (int i = 0; i < length; i++) {
                locations.append(codeManager.createMachineCodeLocation(getInstructions().get(i).address, "native target code instruction"));
            }
            instructionLocations = locations;
        }
        return instructionLocations;
    }

    public CodeLocation entryLocation() {
        return teleTargetMethod.entryLocation();
    }

    public Sequence<MaxCodeLocation> labelLocations() {
        final AppendableSequence<MaxCodeLocation> locations = new ArrayListSequence<MaxCodeLocation>();
        for (TargetCodeInstruction targetCodeInstruction : getInstructions()) {
            if (targetCodeInstruction.label != null) {
                final String description = "Label " + targetCodeInstruction.label.toString() + " in " + entityName();
                locations.append(codeManager().createMachineCodeLocation(targetCodeInstruction.address, description));
            }
        }
        return locations;
    }

    public TeleClassMethodActor getTeleClassMethodActor() {
        return teleTargetMethod.getTeleClassMethodActor();
    }

    public StopPositions getStopPositions() {
        return teleTargetMethod.getStopPositions();
    }

    public int getJavaStopIndex(Address address) {
        final StopPositions stopPositions = getStopPositions();
        if (stopPositions != null) {
            final int targetCodePosition = address.minus(getCodeStart()).toInt();
            for (int i = 0; i < stopPositions.length(); i++) {
                if (stopPositions.get(i) == targetCodePosition) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public TeleTargetMethod teleTargetMethod() {
        return teleTargetMethod;
    }

    public MethodProvider getMethodProvider() {
        return teleTargetMethod.getMethodProvider();
    }

    public MachineCodeInstructionArray getTargetCodeInstructions() {
        return teleTargetMethod.getTargetCodeInstructions();
    }

}
