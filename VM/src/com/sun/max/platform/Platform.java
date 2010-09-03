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

import static java.lang.Integer.*;
import static java.lang.System.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.stack.sparc.*;

/**
 * Platform configuration information. This class maintains a current {@link #platform() platform} context
 * that is initialized via system properties and native methods (where the relevant system property is {@code null}).
 * The system properties used to initialize the initial platform context are:
 * <ul>
 * <li>{@link #PLATFORM_PROPERTY}</li>
 * <li>{@link #PROCESSOR_MODEL_PROPERTY}</li>
 * <li>{@link #INSTRUCTION_SET_PROPERTY}</li>
 * <li>{@link #ENDIANNESS_PROPERTY}</li>
 * <li>{@link #WORD_WIDTH_PROPERTY}</li>
 * <li>{@link #OPERATING_SYSTEM_PROPERTY}</li>
 * <li>{@link #PAGE_SIZE_PROPERTY}</li>
 * <li>{@link #NUMBER_OF_SIGNALS_PROPERTY}</li>
 * </ul>
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class Platform {

    /**
     * The name of the system property whose value (if non-null) specifies the target platform.
     */
    public static final String PLATFORM_PROPERTY = "max.platform";

    /**
     * The name of the system property whose value (if non-null) specifies the target CPU.
     * Iff {@code null}, the value returned by {@link ProcessorModel#defaultForInstructionSet(InstructionSet)}
     * is used.
     */
    public static final String PROCESSOR_MODEL_PROPERTY = "max.cpu";

    /**
     * The name of the system property whose value (if non-null) specifies the target ISA.
     * Iff {@code null}, the value returned by {@link #nativeGetInstructionSet()} is used.
     */
    public static final String INSTRUCTION_SET_PROPERTY = "max.isa";

    /**
     * The name of the system property whose value (if non-null) specifies the target endianness.
     * Iff {@code null}, the value returned by {@link #nativeIsBigEndian()} is used.
     */
    public static final String ENDIANNESS_PROPERTY = "max.endianness";

    /**
     * The name of the system property whose value (if non-null) specifies the target word size (in bits).
     * Iff {@code null}, the value returned by {@link #nativeGetWordWidth()} is used.
     */
    public static final String WORD_WIDTH_PROPERTY = "max.bits";

    /**
     * The name of the system property whose value (if non-null) specifies the target OS.
     * Iff {@code null}, the value returned by {@link #nativeGetOperatingSystem()} is used.
     */
    public static final String OPERATING_SYSTEM_PROPERTY = "max.os";

    /**
     * The name of the system property whose value (if non-null) specifies the target page size.
     * Iff {@code null}, the value returned by {@link #nativeGetPageSize()} is used.
     */
    public static final String PAGE_SIZE_PROPERTY = "max.page";

    /**
     * The name of the system property whose value (if non-null) specifies the number of signals on the target.
     * Iff {@code null}, the value returned by {@link #nativeNumberOfSignals()} is used.
     */
    public static final String NUMBER_OF_SIGNALS_PROPERTY = "max.nsig";

    /**
     * The global platform {@linkplain #platform() context}.
     */
    private static Platform current = Platform.createDefaultPlatform();

    /**
     * Details about the processor of this platform.
     */
    private final ProcessorKind processorKind;

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

    private static final Pattern NON_REGEX_TEST_PATTERN = Pattern.compile("\\w+");

    /**
     * Determines if a given string contains a {@linkplain Pattern regular expression}.
     */
    private static boolean isRegex(String input) {
        for (int i = 0; i < input.length(); ++i) {
            if (!Character.isLetterOrDigit(input.charAt(i)) && input.charAt(i) == '_') {
                return true;
            }
        }
        return false;
    }

    private static boolean isAcceptedBy(String input, String filter) {
        if (filter.isEmpty()) {
            return true;
        }

        boolean negate = filter.charAt(0) == '!';
        if (negate) {
            filter = filter.substring(1);
        }
        boolean result;
        if (!isRegex(filter)) {
            result = input.equalsIgnoreCase(filter);
        } else {
            Pattern pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
            result = pattern.matcher(input).matches();
        }
        return negate ? !result : result;
    }

    /**
     * Determines if this platform is accepted by a filter specified by a {@link PLATFORM} annotation.
     * Each element of {@linkplain PLATFORM} is matched according to the following algorithm.
     * <p>
     * An element value of "" always matches the corresponding platform component.
     * <p>
     * An element value that is not a {@linkplain Pattern regular expression}
     * is a simple string filter compared for {@linkplain String#equalsIgnoreCase(String) case-insensitive equality} against the
     * corresponding platform component. For example {@code @PLATFORM(os = "windows")} will match
     * this platform object if {@code this.operatingSystem().name().equalsIgnoreCase("windows")}. A negative
     * filter can be specified by prefixing {@code '!'} to the filter value. That is,
     * {@code @PLATFORM(os = "!windows")} can be used to match any platform with a non-Windows
     * operating system.
     * <p>
     * An element value that does contain a regular expression
     * is used as a case-insensitive {@linkplain Pattern pattern} that is {@linkplain Pattern#matcher(CharSequence) matched}
     * against the corresponding platform component. For example {@code @PLATFORM(cpu = "(amd64|ia32")} can
     * be used to match an x86 based platform. At with simple string matching, a {@code '!'} prefix can
     * be employed to specify a negative filter.
     *
     * @param filter a {@link PLATFORM} instance
     * @return {@code true} is this platform is accepted by {@code filter}
     */
    public boolean isAcceptedBy(PLATFORM filter) {
        if (filter != null) {
            if (!isAcceptedBy(operatingSystem.name(), filter.os())) {
                return false;
            }
            if (!isAcceptedBy(processorModel().name(), filter.cpu())) {
                return false;
            }
        }
        return true;
    }

    public Platform(ProcessorModel processorModel, OperatingSystem operatingSystem, int pageSize) {
        this(new ProcessorKind(processorModel, processorModel.instructionSet, processorModel.defaultDataModel), operatingSystem, pageSize);
    }

    /**
     * Gets the current platform context. The initial value is created by {@link Platform#createDefaultPlatform()}.
     * It can be subsequently modified by {@link #set(Platform)}.
     */
    @FOLD
    public static Platform platform() {
        return current;
    }

    /**
     * Changes the current platform context.
     *
     * @param vm the new platform context (must not be {@code null})
     * @return the previous platform context
     */
    public static Platform set(Platform platform) {
        Platform old = current;
        current = platform;
        return old;
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

    public Endianness endianness() {
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

    public DataModel dataModel() {
        return processorKind.dataModel;
    }

    public int cacheAlignment() {
        return processorKind.dataModel.cacheAlignment;
    }

    /**
     * Determine the instruction set.
     *
     * @return a string representing the name of the instruction set on which this VM is running
     */
    public static String getInstructionSet() {
        Prototype.loadPrototypeLibrary();
        return nativeGetInstructionSet();
    }

    private static native String nativeGetInstructionSet();

    /**
     * Determine whether the underlying memory model is big-endian.
     *
     * @return {@code true} if this memory model is big-endian; {@code false} otherwise
     */
    private static boolean isBigEndian() {
        Prototype.loadPrototypeLibrary();
        return nativeIsBigEndian();
    }

    private static native boolean nativeIsBigEndian();

    /**
     * Gets the width of the native word in bits (typically 32 or 64 bits).
     *
     * @return the width of the native word in bits
     */
    public static int getWordWidth() {
        Prototype.loadPrototypeLibrary();
        return nativeGetWordWidth();
    }

    private static native int nativeGetWordWidth();

    /**
     * Determines the operating system on which this virtual machine is running.
     *
     * @return a string representing the name of the OS on which this VM is running
     */
    public static String getOperatingSystem() {
        Prototype.loadPrototypeLibrary();
        return nativeGetOperatingSystem();
    }

    private static native String nativeGetOperatingSystem();

    /**
     * Gets the page size in bytes of the platform on which this VM is running.
     *
     * @return the page size in bytes
     */
    public static int getPageSize() {
        Prototype.loadPrototypeLibrary();
        return nativeGetPageSize();
    }

    private static native int nativeGetPageSize();

    /**
     * Gets the number of signals supported by the target that may be delivered to the VM.
     * The range of signal numbers that the VM expects to see is between 0 (inclusive) and
     * {@code numberOfSignals()} (exclusive).
     * This checks the property {@value NUMBER_OF_SIGNALS_PROPERTY} first and only calls
     * {@link #nativeNumberOfSignals()} if there is no value set for the property.
     */
    public static int numberOfSignals() {
        final String prop = System.getProperty(NUMBER_OF_SIGNALS_PROPERTY);
        if (prop != null) {
            return Integer.parseInt(prop);
        }
        Prototype.loadPrototypeLibrary();
        return nativeNumberOfSignals();
    }

    /**
     * Get the number of signals supported by the host platform, the assumption being that the generated VM
     * will be running on the same platform.
     * @return
     */
    private static native int nativeNumberOfSignals();

    /**
     * Creates a default {@code Platform} derived from system properties and native code in the
     * case where the relevant system properties are {@code null}.
     */
    public static Platform createDefaultPlatform() {
        String platformSpec = System.getProperty(PLATFORM_PROPERTY);
        if (platformSpec != null) {
            Platform platform = parse(platformSpec);
            if (platform == null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(baos);
                out.println("Invalid platform specification: " + platformSpec);
                printPlatformSpecificationHelp(out);
                ProgramError.unexpected(baos.toString());
            }
            return platform;
        }


        InstructionSet isa = InstructionSet.valueOf(getProperty(INSTRUCTION_SET_PROPERTY) == null ? getInstructionSet() : getProperty(INSTRUCTION_SET_PROPERTY));
        WordWidth word = WordWidth.fromInt(getInteger(WORD_WIDTH_PROPERTY) == null ? getWordWidth() : getInteger(WORD_WIDTH_PROPERTY));
        final Endianness endianness;
        final String endiannessProperty = getProperty(ENDIANNESS_PROPERTY);
        if (endiannessProperty != null) {
            endianness = Endianness.valueOf(endiannessProperty);
        } else {
            endianness = isBigEndian() ? Endianness.BIG : Endianness.LITTLE;
        }
        final String cpuName = getProperty(PROCESSOR_MODEL_PROPERTY);
        final ProcessorModel processorModel;
        if (cpuName == null) {
            processorModel = ProcessorModel.defaultForInstructionSet(isa);
        } else {
            processorModel = ProcessorModel.valueOf(cpuName);
            assert processorModel.instructionSet == isa;
        }
        final int cacheAlignment = processorModel.defaultDataModel.cacheAlignment;
        final DataModel dataModel = new DataModel(word, endianness, cacheAlignment);
        final ProcessorKind processorKind = new ProcessorKind(processorModel, isa, dataModel);

        String osName = getProperty(OPERATING_SYSTEM_PROPERTY) == null ? getOperatingSystem() : getProperty(OPERATING_SYSTEM_PROPERTY);
        final OperatingSystem os = OperatingSystem.fromName(osName);
        final int pageSize = getInteger(PAGE_SIZE_PROPERTY) == null ? getPageSize() : getInteger(PAGE_SIZE_PROPERTY);

        return new Platform(processorKind, os, pageSize);
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
