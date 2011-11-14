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
package com.sun.max.platform;

import static java.lang.Integer.*;
import static java.lang.System.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.runtime.*;

/**
 * Platform configuration information. This class maintains a current {@link #platform() platform} context
 * that is initialized via system properties and native methods (where the relevant system property is {@code null}).
 * The system properties used to initialize the initial platform context are:
 * <ul>
 * <li>{@link #PLATFORM_PROPERTY}</li>
 * <li>{@link #CPU_PROPERTY}</li>
 * <li>{@link #ISA_PROPERTY}</li>
 * <li>{@link #ENDIANNESS_PROPERTY}</li>
 * <li>{@link #WORD_WIDTH_PROPERTY}</li>
 * <li>{@link #OS_PROPERTY}</li>
 * <li>{@link #PAGE_SIZE_PROPERTY}</li>
 * <li>{@link #NUMBER_OF_SIGNALS_PROPERTY}</li>
 * </ul>
 */
public final class Platform {

    /**
     * The name of the system property whose value (if non-null) specifies the target platform.
     */
    public static final String PLATFORM_PROPERTY = "max.platform";

    /**
     * The name of the system property whose value (if non-null) specifies the target CPU.
     * Iff {@code null}, the value returned by {@link CPU#defaultForInstructionSet(ISA)}
     * is used.
     */
    public static final String CPU_PROPERTY = "max.cpu";

    /**
     * The name of the system property whose value (if non-null) specifies the target ISA.
     * Iff {@code null}, the value returned by {@link #nativeGetISA()} is used.
     */
    public static final String ISA_PROPERTY = "max.isa";

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
     * Iff {@code null}, the value returned by {@link #nativeGetOS()} is used.
     */
    public static final String OS_PROPERTY = "max.os";

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

    public final CPU cpu;

    public final ISA isa;

    public final DataModel dataModel;

    /**
     * The operating system.
     */
    public final OS os;

    /**
     * The number of bytes in a virtual page.
     */
    public final int pageSize;

    /**
     * Stack bias added to the stack pointer to obtain the actual top of the stack frame.
     */
    public final int stackBias;

    public final CiTarget target;

    private CiTarget createTarget() {
        CiArchitecture arch = null;
        int stackAlignment = -1;
        if (isa == ISA.AMD64) {
            arch = new AMD64();
            if (os == OS.DARWIN) {
                // Darwin requires 16-byte stack frame alignment.
                stackAlignment = 16;
            } else if (os == OS.SOLARIS || os == OS.LINUX) {
                // Linux apparently also requires it for functions that pass floating point functions on the stack.
                // One such function in the Maxine code base is log_print_float() in log.c which passes a float
                // value to fprintf on the stack. However, gcc doesn't fix the alignment itself so we simply
                // adopt the global convention on Linux of 16-byte alignment for stacks. If this is a performance issue,
                // this can later be refined to only be for JNI stubs that pass a float or double to native code.

                // Solaris has the same issues.
                stackAlignment = 16;
            } else if (os == OS.MAXVE) {
                stackAlignment = 8;
            } else {
                throw FatalError.unexpected("Unimplemented stack alignment: " + os);
            }

        } else {
            return null;
        }

        assert arch.wordSize == dataModel.wordWidth.numberOfBytes;
        boolean isMP = true;
        int spillSlotSize = arch.wordSize;
        int cacheAlignment = dataModel.cacheAlignment;
        boolean inlineObjects = false;
        return new CiTarget(arch,
                        isMP,
                        spillSlotSize,
                        stackAlignment,
                        pageSize,
                        cacheAlignment,
                        inlineObjects,
                        false,
                        false);
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
     * this platform object if {@code this.os().name().equalsIgnoreCase("windows")}. A negative
     * filter can be specified by prefixing {@code '!'} to the filter value. That is,
     * {@code @PLATFORM(os = "!windows")} can be used to match any platform with a non-Windows
     * operating system.
     * <p>
     * An element value that does contain a regular expression
     * is used as a case-insensitive {@linkplain Pattern pattern} that is {@linkplain Pattern#matcher(CharSequence) matched}
     * against the corresponding platform component. For example {@code @PLATFORM(cpu = "(amd64|ia32)")} can
     * be used to match an x86 based platform. At with simple string matching, a {@code '!'} prefix can
     * be employed to specify a negative filter.
     *
     * @param filter a {@link PLATFORM} instance
     * @return {@code true} is this platform is accepted by {@code filter}
     */
    public boolean isAcceptedBy(PLATFORM filter) {
        if (filter != null) {
            if (!isAcceptedBy(os.name(), filter.os())) {
                return false;
            }
            if (!isAcceptedBy(cpu.name(), filter.cpu())) {
                return false;
            }
        }
        return true;
    }

    public Platform(CPU cpu, OS os, int pageSize) {
        this(cpu, cpu.isa, cpu.defaultDataModel, os, pageSize);
    }

    public Platform(CPU cpu, ISA isa, DataModel dataModel, OS os, int pageSize) {
        this.isa = isa;
        this.cpu = cpu;
        this.os = os;
        this.dataModel = dataModel;
        this.pageSize = pageSize;

        if (cpu == CPU.SPARCV9 && os == OS.SOLARIS) {
            this.stackBias = 2047;
        } else {
            this.stackBias = 0;
        }
        target = createTarget();
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
     * Gets the {@linkplain #target target} associated with the {@linkplain #platform() current} platform context.
     */
    @FOLD
    public static CiTarget target() {
        assert current.target != null;
        return current.target;
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

    public Platform constrainedByInstructionSet(ISA isa) {
        CPU cpu = this.cpu;
        DataModel dataModel = this.dataModel;
        if (this.isa != isa) {
            cpu = CPU.defaultForInstructionSet(isa);
            dataModel = cpu.defaultDataModel;
        }
        return new Platform(cpu, isa, dataModel, os, pageSize);
    }

    @Override
    public String toString() {
        return os.toString().toLowerCase() + "-" + cpu.toString().toLowerCase() + ", isa=" + isa + ", " + dataModel + ", page size=" + pageSize;
    }

    public Endianness endianness() {
        return dataModel.endianness;
    }

    public WordWidth wordWidth() {
        return dataModel.wordWidth;
    }

    public int cacheAlignment() {
        return dataModel.cacheAlignment;
    }

    /**
     * Determine the instruction set.
     *
     * @return a string representing the name of the instruction set on which this VM is running
     */
    public static String getInstructionSet() {
        Prototype.loadHostedLibrary();
        return nativeGetISA();
    }

    private static native String nativeGetISA();

    /**
     * Determine whether the underlying memory model is big-endian.
     *
     * @return {@code true} if this memory model is big-endian; {@code false} otherwise
     */
    private static boolean isBigEndian() {
        Prototype.loadHostedLibrary();
        return nativeIsBigEndian();
    }

    private static native boolean nativeIsBigEndian();

    /**
     * Gets the absolute path to the "jni.h" file against which the native code was compiled.
     */
    @HOSTED_ONLY
    public static String jniHeaderFilePath() {
        try {
            return nativeJniHeaderFilePath();
        } catch (UnsatisfiedLinkError e) {
            String javaHome = System.getenv("JAVA_HOME");
            FatalError.check(javaHome != null, "Environment variable JAVA_HOME not set");
            if (javaHome != null) {
                File jni = new File(javaHome, "include" + File.separatorChar + "jni.h");
                if (jni.exists()) {
                    return jni.getAbsolutePath();
                }
            }
            throw FatalError.unexpected("Could not find jni.h");
        }
    }

    public static native String nativeJniHeaderFilePath();

    /**
     * Gets the absolute path to the "jvmti.h" file against which the native code was compiled.
     * This lives in the same directory as jni.h
     */
    @HOSTED_ONLY
    public static String jvmtiHeaderFilePath() {
        final File jniFile = new File(jniHeaderFilePath());
        final File jvmtiFile = new File(jniFile.getParentFile(), "jvmti.h");
        ProgramError.check(jvmtiFile.exists(), "cannot locate jvmti.h");
        return jvmtiFile.getAbsolutePath();
    }

    /**
     * Gets the width of the native word in bits (typically 32 or 64 bits).
     *
     * @return the width of the native word in bits
     */
    public static int getWordWidth() {
        Prototype.loadHostedLibrary();
        return nativeGetWordWidth();
    }

    private static native int nativeGetWordWidth();

    /**
     * Determines the operating system on which this virtual machine is running.
     *
     * @return a string representing the name of the OS on which this VM is running
     */
    public static String getOS() {
        Prototype.loadHostedLibrary();
        return nativeGetOS();
    }

    private static native String nativeGetOS();

    /**
     * Gets the page size in bytes of the platform on which this VM is running.
     *
     * @return the page size in bytes
     */
    public static int getPageSize() {
        Prototype.loadHostedLibrary();
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
        Prototype.loadHostedLibrary();
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
                throw ProgramError.unexpected(baos.toString());
            }
            return platform;
        }


        String isaProperty = getProperty(ISA_PROPERTY);
        ISA isa = ISA.valueOf(isaProperty == null ? getInstructionSet() : isaProperty.toUpperCase());
        WordWidth word = WordWidth.fromInt(getInteger(WORD_WIDTH_PROPERTY) == null ? getWordWidth() : getInteger(WORD_WIDTH_PROPERTY));
        final Endianness endianness;
        final String endiannessProperty = getProperty(ENDIANNESS_PROPERTY);
        if (endiannessProperty != null) {
            endianness = Endianness.valueOf(endiannessProperty);
        } else {
            endianness = isBigEndian() ? Endianness.BIG : Endianness.LITTLE;
        }
        final String cpuName = getProperty(CPU_PROPERTY);
        final CPU cpu;
        if (cpuName == null) {
            cpu = CPU.defaultForInstructionSet(isa);
        } else {
            cpu = CPU.valueOf(cpuName);
            assert cpu.isa == isa;
        }
        final int cacheAlignment = cpu.defaultDataModel.cacheAlignment;
        final DataModel dataModel = new DataModel(word, endianness, cacheAlignment);

        String osName = getProperty(OS_PROPERTY) == null ? getOS() : getProperty(OS_PROPERTY);
        final OS os = OS.fromName(osName);
        final int pageSize = getInteger(PAGE_SIZE_PROPERTY) == null ? getPageSize() : getInteger(PAGE_SIZE_PROPERTY);

        return new Platform(cpu, isa, dataModel, os, pageSize);
    }

    /**
     * A map from platform strings to correlated {@link Platform} objects.
     */
    public static final Map<String, Platform> Supported;
    static {
        Map<String, Platform> map = new TreeMap<String, Platform>();
        map.put("solaris-amd64", new Platform(CPU.AMD64, OS.SOLARIS, Ints.K * 8));
        map.put("solaris-sparcv9", new Platform(CPU.SPARCV9, OS.SOLARIS, Ints.K * 8));
        map.put("linux-amd64", new Platform(CPU.AMD64, OS.LINUX, Ints.K * 8));
        map.put("darwin-amd64", new Platform(CPU.AMD64, OS.DARWIN, Ints.K * 8));
        map.put("maxve-amd64", new Platform(CPU.AMD64, OS.MAXVE, Ints.K * 8));
        Supported = Collections.unmodifiableMap(map);
    }

    /**
     * The global platform {@linkplain #platform() context}.
     */
    private static Platform current = Platform.createDefaultPlatform();

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
            platform = new Platform(platform.cpu, platform.isa, platform.dataModel, platform.os, (int) pageSize);
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
