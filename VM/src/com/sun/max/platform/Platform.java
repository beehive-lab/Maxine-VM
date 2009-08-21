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

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.stack.sparc.*;

/**
 * Platform-dependent information for VM configuration.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class Platform {

    /**
     * Details about the processor of this platform.
     */
    public final ProcessorKind processorKind;

    /**
     * The operating system.
     */
    public final OperatingSystem operatingSystem;

    /**
     * The number of bytes in a virtual page.
     */
    public final int pageSize;


    /**
     * Stack bias added to the stack pointer to obtain the actual top of the stack frame.
     * <br>
     * Normally 0, but see SPARC/SOLARIS.
     */
    public final int stackBias;

    public Platform(ProcessorKind processorKind, OperatingSystem operatingSystem, int pageSize) {
        this.processorKind = processorKind;
        this.operatingSystem = operatingSystem;
        this.pageSize = pageSize;

        if (processorKind.processorModel == ProcessorModel.SPARCV9 && operatingSystem == OperatingSystem.SOLARIS) {
            this.stackBias = SPARCStackFrameLayout.STACK_BIAS;
        } else {
            this.stackBias = 0;
        }
    }

    public Platform(ProcessorModel processorModel, OperatingSystem operatingSystem, int pageSize) {
        this(new ProcessorKind(processorModel, processorModel.instructionSet, processorModel.defaultDataModel), operatingSystem, pageSize);
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

    public Endianness endianess() {
        return processorKind.dataModel.endianness;
    }

    public WordWidth wordWidth() {
        return processorKind.dataModel.wordWidth;
    }

    public InstructionSet instructionSet() {
        return processorKind.instructionSet;
    }

    public ProcessorModel processorModel() {
        return processorKind.processorModel;
    }

    public int cacheAlignment() {
        return processorKind.dataModel.cacheAlignment;
    }

    /**
     * A map from platform strings to correlated {@link Platform} objects.
     */
    public static final Map<String, Platform> Supported;
    static {
        Map<String, Platform> map = new TreeMap<String, Platform>();
        map.put("solaris-amd64", new Platform(ProcessorModel.AMD64, OperatingSystem.SOLARIS, Ints.K * 8));
        map.put("solaris-sparcv9", new Platform(ProcessorModel.SPARCV9, OperatingSystem.SOLARIS, Ints.K * 8));
        map.put("linux-amd64", new Platform(ProcessorModel.AMD64, OperatingSystem.LINUX, Ints.K * 8));
        map.put("darwin-amd64", new Platform(ProcessorModel.AMD64, OperatingSystem.DARWIN, Ints.K * 8));
        Supported = Collections.unmodifiableMap(map);
    }

    /**
     * Parses a platform string into a {@link Platform} object. The strings for which a non-null {@code Platform} object
     * is returned are any of the keys of {@link #Supported}. The string can include a suffix of a ':' followed by a
     * value parsable by {@link Longs#parseScaledValue(String)} specifying a page size.
     *
     * @param platformSpec a string describing a platform and an optional page size
     * @return the Platform object corresponding to {@code platformSpec} or null if none of the preset platforms is
     *         matched by {@code platformSpec}
     */
    public static Platform parse(String platformSpec) {
        int colonIndex = platformSpec.indexOf(':');
        String pageSizeString = null;
        if (colonIndex != -1) {
            pageSizeString = platformSpec.substring(colonIndex + 1);
            platformSpec = platformSpec.substring(0, colonIndex);
        }
        Platform platform = Supported.get(platformSpec);
        if (platform == null) {
            return null;
        }
        if (pageSizeString != null) {
            long pageSize = Longs.parseScaledValue(pageSizeString);
            assert pageSize == (int) pageSize;
            platform = new Platform(platform.processorKind, platform.operatingSystem, (int) pageSize);
        }
        return platform;
    }

    public static void main(String[] args) {
        printPlatformSpecificationHelp(System.out);
    }

    /**
     * Prints a help message detailing the format of a platform specification accepted by {@link #parse(String)}.
     *
     * @param out stream to which the help message is printed
     */
    public static void printPlatformSpecificationHelp(PrintStream out) {
        out.println("A platform specification has one of the following formats:");
        out.println();
        out.println("    <platform>");
        out.println("    <platform>:<page size>");
        out.println();
        out.println("For example:");
        out.println();
        out.println("    solaris-amd64");
        out.println("    solaris-amd64:8k");
        out.println();
        out.println("The supported platforms are:");
        out.println();
        for (Map.Entry<String, Platform> entry : Supported.entrySet()) {
            out.println("    " + entry.getKey());
        }
    }
}
