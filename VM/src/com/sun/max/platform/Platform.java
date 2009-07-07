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
package com.sun.max.platform;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;

/**
 * Platform-dependent information for VM configuration.
 *
 * @author Bernd Mathiske
 */
public final class Platform {

    public final ProcessorKind processorKind;
    public final OperatingSystem operatingSystem;
    public final int pageSize;

    public Platform(ProcessorKind processorKind, OperatingSystem operatingSystem, int pageSize) {
        this.processorKind = processorKind;
        this.operatingSystem = operatingSystem;
        this.pageSize = pageSize;
    }

    public static Platform host() {
        return MaxineVM.host().configuration().platform();
    }

    @FOLD
    public static Platform target() {
        return MaxineVM.target().configuration().platform();
    }

    @UNSAFE
    @FOLD
    public static Platform hostOrTarget() {
        return MaxineVM.hostOrTarget().configuration().platform();
    }

    public Platform constrainedByInstructionSet(InstructionSet instructionSet) {
        ProcessorKind processor = processorKind;
        if (processor.instructionSet != instructionSet) {
            processor = ProcessorKind.defaultForInstructionSet(instructionSet);
        }
        return new Platform(processor, operatingSystem, pageSize);
    }

    @Override
    public String toString() {
        return operatingSystem.toString().toLowerCase() + "-" + processorKind + ", page size=" + pageSize;
    }

    public void inspect(PlatformInspector inspector) {
        inspector.inspectCacheAlignment(processorKind.dataModel.cacheAlignment);
        inspector.inspectEndianness(processorKind.dataModel.endianness);
        inspector.inspectWordWidth(processorKind.dataModel.wordWidth);
        inspector.inspectInstructionSet(processorKind.instructionSet);
        inspector.inspectOperatingSystem(operatingSystem);
        inspector.inspectPageSize(pageSize);
        inspector.inspectProcessorModel(processorKind.processorModel);
    }

    /**
     * A platform inspector is used to inquire about or validate platform configuration details.
     */
    public static interface PlatformInspector {
        void inspectOperatingSystem(OperatingSystem os);
        void inspectInstructionSet(InstructionSet isa);
        void inspectPageSize(int pageSize);
        void inspectProcessorModel(ProcessorModel cpu);
        void inspectWordWidth(WordWidth wordWidth);
        void inspectEndianness(Endianness endianness);
        void inspectCacheAlignment(int alignment);
    }

    /**
     * A default platform inspector enables a subclass to only override the methods corresponding to
     * the configuration details that are of concern in a given context.
     */
    public static class PlatformInspectorAdapter implements PlatformInspector {
        public void inspectCacheAlignment(int alignment) {
        }

        public void inspectEndianness(Endianness endianness) {
        }

        public void inspectInstructionSet(InstructionSet isa) {
        }

        public void inspectOperatingSystem(OperatingSystem os) {
        }

        public void inspectPageSize(int pageSize) {
        }

        public void inspectProcessorModel(ProcessorModel cpu) {
        }

        public void inspectWordWidth(WordWidth wordWidth) {
        }
    }
}
