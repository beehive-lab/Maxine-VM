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
/*VCSID=f33a20d7-38be-4bb8-abd6-61acc92c4fc6*/
package com.sun.max.vm.prototype;

import java.io.*;

import com.sun.max.asm.*;
import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * This class encapsulates a number of host-VM related concepts such as the host VM configuration and simplifies loading
 * native libraries on the host VM.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Prototype {

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
     * native code when {@linkplain #createHostPlatform() initializing} the host platform.
     */
    public static final String OPERATING_SYSTEM_PROPERTY = "max.host.os";

    /**
     * The name of the system property whose value (if non-null) will override the page size (in bytes) specified by the
     * native code when {@linkplain #createHostPlatform() initializing} the host platform.
     */
    public static final String PAGE_SIZE_PROPERTY = "max.host.page";

    private final VMConfiguration _vmConfiguration;

    /**
     * Get the VM configuration for the host.
     *
     * @return the VM configuration corresponding to the host virtual machine
     */
    public VMConfiguration vmConfiguration() {
        return _vmConfiguration;
    }

    /**
     * Create a prototype with the specified VM configuration.
     *
     * @param vmConfiguration the VM configuration for the prototype
     */
    protected Prototype(VMConfiguration vmConfiguration) {
        _vmConfiguration = vmConfiguration;
    }

    /**
     * The default place where native libraries are placed by the make system.
     */
    public static final String LIBRARY_BUILD_PATH = "Native/generated/" + OperatingSystem.current().name().toLowerCase() + "/";

    /**
     * The name of the default prototype library.
     */
    public static final String DEFAULT_LIBRARY_NAME = "prototype";

    /**
     * A status variable indicating whether modifications to the underlying "java.library.path" have been made.
     */
    private static boolean _isPathHacked;

    /**
     * Loads a native library in the prototype with the specified name. This method automatically finds the correct path
     * and adds into the JDK's internal path for looking up native libraries so that it is not necessary to specify it
     * as an environment variable when launching the java program.
     *
     * @param name the name of the library as a string, without any prefixes or suffixes such as "lib*", "*.so",
     *            "*.dll", etc
     */
    public static synchronized void loadLibrary(String name) {
        if (!_isPathHacked) {
            final File projectPath = JavaProject.findVcsProjectDirectory();
            final File workspacePath = projectPath.getParentFile();
            final String[] usrPaths = (String[]) WithoutAccessCheck.getStaticField(ClassLoader.class, "usr_paths");
            final String libraryPath = new File(workspacePath, LIBRARY_BUILD_PATH).getPath() + File.pathSeparator + Arrays.toString(usrPaths, File.pathSeparator);
            HackJDK.setLibraryPath(libraryPath);
            _isPathHacked = true;
        }
        System.loadLibrary(name);
    }

    /**
     * A native method to determine the processor model.
     *
     * @return a string representing the name of the processor model on which this VM is running
     */
    private static native String nativeGetProcessorModel();

    /**
     * A native method to determine the instruction set.
     *
     * @return a string representing the name of the instruction set on which this VM is running
     */
    private static native String nativeGetInstructionSet();

    /**
     * A native method to determine whether the underlying memory model is big-endian.
     *
     * @return {@code true} if this memory model is big-endian; {@code false} otherwise
     */
    private static native boolean nativeIsBigEndian();

    /**
     * Gets the width of the native word in bits (typically 32 or 64 bits).
     *
     * @return the width of the native word in bits
     */
    private static native int nativeGetWordWidth();

    /**
     * Determines the operating system on which this virtual machine is running.
     *
     * @return a string representing the name of the OS on which this VM is running
     */
    private static native String nativeGetOperatingSystem();

    /**
     * Gets the page size in bytes of the platform on which this VM is running.
     *
     * @return the page size in bytes
     */
    private static native int nativeGetPageSize();

    /**
     * Creates a {@code Platform} that describes the host VM's platform.
     *
     * @return a new platform object that describes the host's platform
     */
    public static Platform createHostPlatform() {
        loadLibrary(DEFAULT_LIBRARY_NAME);

        final InstructionSet instructionSet = InstructionSet.valueOf(System.getProperty(INSTRUCTION_SET_PROPERTY, nativeGetInstructionSet()));
        final WordWidth wordWidth = WordWidth.fromInt(Integer.getInteger(WORD_WIDTH_PROPERTY, nativeGetWordWidth()));
        final Endianness endianness;
        final String endiannessProperty = System.getProperty(ENDIANNESS_PROPERTY);
        if (endiannessProperty != null) {
            endianness = Endianness.valueOf(endiannessProperty);
        } else {
            endianness = nativeIsBigEndian() ? Endianness.BIG : Endianness.LITTLE;
        }
        final String processorModelName = System.getProperty(PROCESSOR_MODEL_PROPERTY, nativeGetProcessorModel());
        final ProcessorModel processorModel;
        if (processorModelName == null) {
            processorModel = ProcessorModel.defaultForInstructionSet(instructionSet);
        } else {
            processorModel = ProcessorModel.valueOf(processorModelName);
            assert processorModel.instructionSet() == instructionSet;
        }
        final Alignment alignment = processorModel.defaultDataModel().alignment();
        final DataModel dataModel = new DataModel(wordWidth, endianness, alignment);
        final ProcessorKind processorKind = new ProcessorKind(processorModel, instructionSet, dataModel);

        final OperatingSystem operatingSystem = OperatingSystem.valueOf(System.getProperty(OPERATING_SYSTEM_PROPERTY, nativeGetOperatingSystem()));
        final int pageSize = Integer.getInteger(PAGE_SIZE_PROPERTY, nativeGetPageSize());

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
        configuration.loadAndInstantiateSchemes();
    }
}
