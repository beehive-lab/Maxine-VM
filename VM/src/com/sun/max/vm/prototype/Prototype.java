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
package com.sun.max.vm.prototype;

import java.io.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * This class encapsulates a number of host-VM related concepts such as the host VM configuration and simplifies loading
 * native libraries on the host VM.
 *
 * The term "host" is potentially quite misleading. In particular, it does NOT mean the system/vm that is executing this code
 * i.e, running the {@link BootImageGenerator} or the {@link Inspector}. The "host" is in fact the prototype of the VM that will
 * be generated (hence the class name {@code Prototype}, and would perhaps be more accurately described as "target",
 * but see {@link MaxineVM} for the actual definition of "target". This could certainly benefit from some renaming, in particular
 * there is no concise way to describe the VM that is executing this code, i.e. the host of the host.
 *
 * By default the properties of the generated VM are the same as the system executing this code, and these are discovered
 * by calling native methods in the "prototytpe" library. However, they can all be overridden by properties, e.g.,
 * {@value OPERATING_SYSTEM_PROPERTY} defined here. Note that, regardless of the properties of the "host" VM,
 * the "prototype" library must be loaded from somewhere that is compatible with the "host of the host".
  *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Prototype {

    /**
     * The name of the system property whose value (if non-null) will determine the host platform.
     */
    public static final String PLATFORM_PROPERTY = "max.platform";

    /**
     * The name of the system property whose value (if non-null) will override the processor model specified by the
     * native code when {@linkplain #createHostPlatform() initializing} the host platform.
     */
    public static final String PROCESSOR_MODEL_PROPERTY = "max.host.cpu";

    /**
     * The name of the system property whose value (if non-null) will override the instruction set specified by the
     * native code when {@linkplain #createHostPlatform() initializing} the host platform.
     */
    public static final String INSTRUCTION_SET_PROPERTY = "max.host.isa";

    /**
     * The name of the system property whose value (if non-null) will override the endianness specified by the native
     * code when {@linkplain #createHostPlatform() initializing} the host platform.
     */
    public static final String ENDIANNESS_PROPERTY = "max.host.endianness";

    /**
     * The name of the system property whose value (if non-null) will override the word width (in bits) specified by the
     * native code when {@linkplain #createHostPlatform() initializing} the host platform.
     */
    public static final String WORD_WIDTH_PROPERTY = "max.host.bits";

    /**
     * The name of the system property whose value (if non-null) will override the operating system specified by the
     * native code when {@linkplain #createHostPlatform() initializing} the host platform. This should be specified
     * in the enum style {@link OperatingSystem#name()} and not Hungarian style, i.e, {@link OperatingSystem#asClassName()}.
     * E.g. LINUX not Linux.
     */
    public static final String OPERATING_SYSTEM_PROPERTY = "max.host.os";

    /**
     * The name of the system property whose value (if non-null) will override the page size (in bytes) specified by the
     * native code when {@linkplain #createHostPlatform() initializing} the host platform.
     */
    public static final String PAGE_SIZE_PROPERTY = "max.host.page";

    private final VMConfiguration vmConfiguration;

    /**
     * Get the VM configuration for the host.
     *
     * @return the VM configuration corresponding to the host virtual machine
     */
    public VMConfiguration vmConfiguration() {
        return vmConfiguration;
    }

    /**
     * Create a prototype with the specified VM configuration.
     *
     * @param vmConfiguration the VM configuration for the prototype
     */
    protected Prototype(VMConfiguration vmConfiguration) {
        this.vmConfiguration = vmConfiguration;
    }

    /**
     * The default place where native libraries are placed by the make system for the "host of the host" system.
     */
    private static final String LIBRARY_BUILD_PATH = "Native/generated/" + OperatingSystem.current().asPackageName() + "/";

    /**
     * The name of the default prototype library.
     */
    public static final String PROTOTYPE_LIBRARY_NAME = "prototype";

    /**
     * A status variable indicating whether modifications to the underlying "java.library.path" have been made.
     */
    private static boolean isPathHacked;

    /**
     * Loads a native library in the prototype with the specified name. This method automatically finds the correct path
     * and adds into the JDK's internal path for looking up native libraries so that it is not necessary to specify it
     * as an environment variable when launching the java program.
     *
     * @param name the name of the library as a string, without any prefixes or suffixes such as "lib*", "*.so",
     *            "*.dll", etc
     */
    public static synchronized void loadLibrary(String name) {
        if (!isPathHacked) {
            final File projectPath = JavaProject.findVcsProjectDirectory();
            final File workspacePath = projectPath.getParentFile();
            final String[] usrPaths = (String[]) WithoutAccessCheck.getStaticField(ClassLoader.class, "usr_paths");
            final String libraryPath = new File(workspacePath, LIBRARY_BUILD_PATH).getPath() + File.pathSeparator + Utils.toString(usrPaths, File.pathSeparator);
            JDKInterceptor.setLibraryPath(libraryPath);
            isPathHacked = true;
        }
        System.loadLibrary(name);
    }

    private static boolean isPrototypeLoaded;

    private static void loadPrototypeLibrary() {
        if (!isPrototypeLoaded) {
            loadLibrary(PROTOTYPE_LIBRARY_NAME);
            isPrototypeLoaded = true;
        }
    }

    /**
     * Determine the processor model.
     *
     * @return a string representing the name of the processor model on which this VM is running
     */
    private static String getProcessorModel() {
        loadPrototypeLibrary();
        return nativeGetProcessorModel();
    }

    private static native String nativeGetProcessorModel();

    /**
     * Determine the instruction set.
     *
     * @return a string representing the name of the instruction set on which this VM is running
     */
    private static String getInstructionSet() {
        loadPrototypeLibrary();
        return nativeGetInstructionSet();
    }

    private static native String nativeGetInstructionSet();

    /**
     * Determine whether the underlying memory model is big-endian.
     *
     * @return {@code true} if this memory model is big-endian; {@code false} otherwise
     */
    private static boolean isBigEndian() {
        loadPrototypeLibrary();
        return nativeIsBigEndian();
    }

    private static native boolean nativeIsBigEndian();

    /**
     * Gets the width of the native word in bits (typically 32 or 64 bits).
     *
     * @return the width of the native word in bits
     */
    private static int getWordWidth() {
        loadPrototypeLibrary();
        return nativeGetWordWidth();
    }

    private static native int nativeGetWordWidth();

    /**
     * Determines the operating system on which this virtual machine is running.
     *
     * @return a string representing the name of the OS on which this VM is running
     */
    private static String getOperatingSystem() {
        loadPrototypeLibrary();
        return nativeGetOperatingSystem();
    }

    private static native String nativeGetOperatingSystem();

    /**
     * Gets the page size in bytes of the platform on which this VM is running.
     *
     * @return the page size in bytes
     */
    private static int getPageSize() {
        loadPrototypeLibrary();
        return nativeGetPageSize();
    }

    private static native int nativeGetPageSize();

    /**
     * Creates a {@code Platform} that describes the host VM's platform.
     *
     * @return a new platform object that describes the host's platform
     */
    public static Platform createHostPlatform() {
        String platformSpec = System.getProperty(PLATFORM_PROPERTY);
        if (platformSpec != null) {
            Platform platform = Platform.parse(platformSpec);
            if (platform == null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(baos);
                out.println("Invalid platform specification: " + platformSpec);
                Platform.printPlatformSpecificationHelp(out);
                ProgramError.unexpected(baos.toString());
            }
            return platform;
        }

        final InstructionSet instructionSet = InstructionSet.valueOf(System.getProperty(INSTRUCTION_SET_PROPERTY, getInstructionSet()));
        final WordWidth wordWidth = WordWidth.fromInt(Integer.getInteger(WORD_WIDTH_PROPERTY, getWordWidth()));
        final Endianness endianness;
        final String endiannessProperty = System.getProperty(ENDIANNESS_PROPERTY);
        if (endiannessProperty != null) {
            endianness = Endianness.valueOf(endiannessProperty);
        } else {
            endianness = isBigEndian() ? Endianness.BIG : Endianness.LITTLE;
        }
        final String processorModelName = System.getProperty(PROCESSOR_MODEL_PROPERTY, getProcessorModel());
        final ProcessorModel processorModel;
        if (processorModelName == null) {
            processorModel = ProcessorModel.defaultForInstructionSet(instructionSet);
        } else {
            processorModel = ProcessorModel.valueOf(processorModelName);
            assert processorModel.instructionSet == instructionSet;
        }
        final int cacheAlignment = processorModel.defaultDataModel.cacheAlignment;
        final DataModel dataModel = new DataModel(wordWidth, endianness, cacheAlignment);
        final ProcessorKind processorKind = new ProcessorKind(processorModel, instructionSet, dataModel);

        final OperatingSystem operatingSystem = OperatingSystem.fromName(System.getProperty(OPERATING_SYSTEM_PROPERTY, getOperatingSystem()));
        final int pageSize = Integer.getInteger(PAGE_SIZE_PROPERTY, getPageSize());

        return new Platform(processorKind, operatingSystem, pageSize);
    }

    /**
     * Creates a VM configuration that describes the host.
     *
     * @return a {@code VMConfiguration} for this host.
     */
    private static VMConfiguration createHostVMConfiguration() {
        final Platform platform = createHostPlatform();
        return VMConfigurations.createPrototype(BuildLevel.PRODUCT, platform);
    }

    /**
     * Initialize the host VM's configuration information, building necessary configuration objects and host/target
     * configurations.
     */
    public static void initializeHost() {
        if (MaxineVM.isHostInitialized()) {
            return;
        }
        final VMConfiguration configuration = createHostVMConfiguration();
        MaxineVM.initialize(configuration, configuration);
        configuration.loadAndInstantiateSchemes(false);
    }
}
