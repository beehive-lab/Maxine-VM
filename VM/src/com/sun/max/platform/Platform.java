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
/*VCSID=3559b1bc-f0d4-4bca-9329-bbeafad36c50*/
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

    private final ProcessorKind _processorKind;
    private final OperatingSystem _operatingSystem;
    private final int _pageSize;

    public Platform(ProcessorKind processorKind, OperatingSystem operatingSystem, int pageSize) {
        _processorKind = processorKind;
        _operatingSystem = operatingSystem;
        _pageSize = pageSize;
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

    @INLINE
    public ProcessorKind processorKind() {
        return _processorKind;
    }

    @INLINE
    public OperatingSystem operatingSystem() {
        return _operatingSystem;
    }

    @INLINE
    public int pageSize() {
        return _pageSize;
    }

    public Platform constrainedByInstructionSet(InstructionSet instructionSet) {
        ProcessorKind processorKind = processorKind();
        if (processorKind.instructionSet() != instructionSet) {
            processorKind = ProcessorKind.defaultForInstructionSet(instructionSet);
        }
        return new Platform(processorKind, operatingSystem(), pageSize());
    }

    @Override
    public String toString() {
        return _operatingSystem.toString().toLowerCase() + "-" + _processorKind + ", page size=" + _pageSize;
    }

    public void inspect(PlatformInspector inspector) {
        inspector.inspectAlignment(_processorKind.dataModel().alignment());
        inspector.inspectEndianness(_processorKind.dataModel().endianness());
        inspector.inspectWordWidth(_processorKind.dataModel().wordWidth());
        inspector.inspectInstructionSet(_processorKind.instructionSet());
        inspector.inspectOperatingSystem(_operatingSystem);
        inspector.inspectPageSize(_pageSize);
        inspector.inspectProcessorModel(_processorKind.processorModel());
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
        void inspectAlignment(Alignment alignment);
    }

    /**
     * A default platform inspector enables a subclass to only override the methods corresponding to
     * the configuration details that are of concern in a given context.
     */
    public static class PlatformInspectorAdapter implements PlatformInspector {
        public void inspectAlignment(Alignment alignment) {
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
